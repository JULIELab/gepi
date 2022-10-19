package de.julielab.gepi.core.retrieval.services;

import de.julielab.gepi.core.retrieval.data.*;
import de.julielab.gepi.core.services.GePiCoreTestModule;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses interaction data in JSON format that is imported into a small ElasticSearch docker instance. The data
 * comes from the <code>gepi-test-data</code> project. The PMIDs and GeneIds that the test data consist of are
 * to be found there under the <code>src/main/resources</code> directory.
 * 
 * See {@link TestcontainersElasticSearch#populateEsTestInstance(GenericContainer)}
 */
public class EventRetrievalServiceIntegrationTest {

    // in case we need to disable X-shield: https://stackoverflow.com/a/51172136/1314955
    public static GenericContainer es = TestcontainersElasticSearch.getEsTestContainer();
    private static Registry registry;

    @BeforeClass
    public static void setup() throws Exception {
        es.start();
        TestcontainersElasticSearch.populateEsTestInstance(es);

        registry = RegistryBuilder.buildAndStartupRegistry(GePiCoreTestModule.class);
    }



    @AfterClass
    public static void shutdown() {
        es.stop();
    }

    @Test
    public void testOpenSearch() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(3);

        final List<String> eventTypes = outsideEvents.get().getEventList().stream().map(Event::getMainEventType).collect(Collectors.toList());
        final List<Argument> arguments = outsideEvents.get().getEventList().stream().map(Event::getArguments).flatMap(List::stream).collect(Collectors.toList());

        final List<String> geneids = arguments.stream().map(Argument::getGeneId).collect(Collectors.toList());

        assertThat(eventTypes).containsExactlyInAnyOrder("Positive_regulation", "Positive_regulation", "Binding");
        assertThat(geneids).containsExactlyInAnyOrder("10243", "10243", "10243", "2617", "26036", "8870");
    }

    @Test
    public void testOpenSearchWithEventTypeFilter1() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withEventTypes("Positive_regulation"));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(2);
    }

    @Test
    public void testOpenSearchWithEventTypeFilter2() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("3930")).withEventTypes("Negative_regulation"));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testOpenSearchWithSentenceFilter1() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withEventTypes("Positive_regulation").withSentenceFilterString("essential"));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(2);
    }

    @Test
    public void testOpenSearchWithSentenceFilter2() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("3930")).withEventTypes("Positive_regulation").withSentenceFilterString( "stress"));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testOpenSearchWithSentenceParagraphFilter() throws Exception {
        // this test refers to test file 10022233_FE6_0_1.json
        // both filter keywords, for sentence and paragraph, are present for the same event items
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("108")).withFilterFieldsConnectionOperator("AND").withSentenceFilterString("pertussis toxin").withParagraphFilterString("immune modulation"));
        assertThat(outsideEvents.get().getEventList()).isNotEmpty();
        assertThat(outsideEvents.get().getEventList().get(0).getPmid()).isEqualTo("10022233");
    }

    @Test
    public void testOpenSearchWithSentenceParagraphFilter2() throws Exception {
        // This test refers to test files 10022233_FE6_0_1.json and 10022381_FE16_0_1.json.
        // 10022233_FE7_0_1 is also a result because it's the same sentence and first argument as FE6.
        // the 'pertussis toxin' phrase is present in the sentence of the former, the 'Heparinized blood' in the
        // paragraph of the latter.
        // Through the 'or' operator, we should get both
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("108", "3458")).withFilterFieldsConnectionOperator("AND").withSentenceFilterString("\"pertussis toxin\"").withParagraphFilterString("\"Heparinized blood\""));
        assertThat(outsideEvents.get().getEventList()).extracting(Event::getEventId).containsExactlyInAnyOrder("10022233_FE6_0_1", "10022233_FE7_0_1", "10022381_FE16_0_1");
    }

    @Test
    public void testClosedSearchEvents() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withListBGePiIds( IdConversionResult.of("8870")));
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(1);

        final List<String> eventTypes = bipartiteEventsEvents.get().getEventList().stream().map(Event::getMainEventType).collect(Collectors.toList());
        final List<Argument> arguments = bipartiteEventsEvents.get().getEventList().stream().map(Event::getArguments).flatMap(List::stream).collect(Collectors.toList());

        final List<String> geneids = arguments.stream().map(Argument::getGeneId).collect(Collectors.toList());
        final List<String> argumentPrefNames = arguments.stream().map(Argument::getPreferredName).collect(Collectors.toList());

        assertThat(eventTypes).containsExactlyInAnyOrder("Binding");
        assertThat(geneids).containsExactlyInAnyOrder("10243", "8870");
        assertThat(argumentPrefNames).containsExactlyInAnyOrder("GPHN", "IER3");
    }

    @Test
    public void testClosedSearchEventsWithEventTypeFilter1() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withListBGePiIds(IdConversionResult.of("8870")).withEventTypes("Binding"));
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(1);
    }

    @Test
    public void testClosedSearchEventsWithEventTypeFilter2() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withListBGePiIds(IdConversionResult.of("8870")).withEventTypes("Negative_regulation"));
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testClosedSearchEventsWithSentenceFilter1() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withListBGePiIds(IdConversionResult.of("8870")).withEventTypes("Binding").withSentenceFilterString("sequence"));
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(1);
    }

    @Test
    public void testClosedSearchEventsWithSentenceFilter2() throws Exception {
        // there should be 0 hits because the 'stress' keyword is not contained in the found event sentence
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withListBGePiIds(IdConversionResult.of("8870")).withEventTypes("Binding").withSentenceFilterString("stress"));
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testClosedSearchWithSentenceParagraphFilter() throws Exception {
        // This tests uses 10049519_FE11_0_1.json and 10051468_FE0_0_1.json
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("7124")).withListBGePiIds(IdConversionResult.of("3569","7351")).withFilterFieldsConnectionOperator("OR").withSentenceFilterString("\"neutrophil infiltration\"").withParagraphFilterString("\"regenerating mice\""));
        final List<Event> eventList = bipartiteEventsEvents.get().getEventList();
        assertThat(eventList).extracting(Event::getEventId).containsExactlyInAnyOrder("10049519_FE11_0_1", "10051468_FE0_0_1");
    }

    @Test
    public void getTotalNumberOfEvents() {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        long totalNumberOfEvents = eventRetrievalService.getTotalNumberOfEvents();
        // the number of JSON files in the test index directory
        assertThat(totalNumberOfEvents).isEqualTo(57);
    }

    @Test
    public void testOpenSearchEventWithSectionFilter() throws Exception {
        // First, establish the baseline: For gene ID 3458 we should find 9 events without filters
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEventsWithoutRestriction = eventRetrievalService.openSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("3458")));
        assertThat(outsideEventsWithoutRestriction.get().getEventList().size()).isEqualTo(9);

        // Filtering for "cytokine" on the headings should reduce the number of hits to 1
        CompletableFuture<EventRetrievalResult> outsideEventsWithSectionFilter = eventRetrievalService.openSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("3458")).withSectionNameFilterString("cytokine"));
        assertThat(outsideEventsWithSectionFilter.get().getEventList().size()).isEqualTo(1);
    }
}
