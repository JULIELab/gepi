package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.components.ISearchServerComponent;
import de.julielab.gepi.core.GepiCoreSymbolConstants;
import de.julielab.gepi.core.retrieval.data.*;
import de.julielab.gepi.core.services.GePiCoreTestModule;
import de.julielab.gepi.core.services.IGeneIdService;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.apache.tapestry5.ioc.services.SymbolSource;
import org.assertj.core.groups.Tuple;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses interaction data in JSON format that is imported into a small ElasticSearch docker instance. The data
 * comes from the <code>gepi-test-data</code> project. The PMIDs and GeneIds that the test data consist of are
 * to be found there under the <code>src/main/resources</code> directory.
 * <p>
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
        Future<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withIncludeUnary(true).withListAGePiIds(IdConversionResult.of("10243")));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(5);

        final List<String> eventTypes = outsideEvents.get().getEventList().stream().map(Event::getMainEventType).collect(Collectors.toList());
        final List<Argument> arguments = outsideEvents.get().getEventList().stream().map(Event::getArguments).flatMap(List::stream).collect(Collectors.toList());

        final List<String> conceptId = arguments.stream().map(Argument::getConceptId).collect(Collectors.toList());

        assertThat(eventTypes).containsExactlyInAnyOrder("Binding", "Binding", "Gene_expression", "Binding", "Binding");
        assertThat(conceptId).containsExactlyInAnyOrder("tid16199850",
                "none",
                "tid16199850",
                "none",
                "tid16199850",
                "none",
                "tid16199850",
                "tid16168587",
                "tid16199850",
                "none");
    }

    @Test
    public void testOpenSearchOnlyBinary() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withIncludeUnary(false).withListAGePiIds(IdConversionResult.of("10243")));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(1);

        final List<String> eventTypes = outsideEvents.get().getEventList().stream().map(Event::getMainEventType).collect(Collectors.toList());
        final List<Argument> arguments = outsideEvents.get().getEventList().stream().map(Event::getArguments).flatMap(List::stream).collect(Collectors.toList());

        final List<String> conceptId = arguments.stream().map(Argument::getConceptId).collect(Collectors.toList());

        assertThat(eventTypes).containsExactlyInAnyOrder("Binding");
        // of course, these values change every time we update the database.
        assertThat(conceptId).containsExactlyInAnyOrder("tid16199850", "tid16168587");
    }

    @Test
    public void testOpenSearchWithTaxFilter() throws Exception {
        // We should just find all documents with gene ID 10243
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withIncludeUnary(true).withListAGePiIds(IdConversionResult.of("atid2324")));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(5);

        // we should still receive the same events when we specify the taxId present in the events
        Future<EventRetrievalResult> outsideEvents2 = eventRetrievalService.openSearch(new GepiRequestData().withIncludeUnary(true).withListAGePiIds(IdConversionResult.of("atid2324")).withTaxId("9606"));
        assertThat(outsideEvents2.get().getEventList().size()).isEqualTo(5);

        // just use some other taxId that the genes do not long belong to
        Future<EventRetrievalResult> outsideEvents3 = eventRetrievalService.openSearch(new GepiRequestData().withIncludeUnary(true).withListAGePiIds(IdConversionResult.of("atid2324")).withTaxId("10090"));
        assertThat(outsideEvents3.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testClosedSearchEventsWithTaxFilter() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withListBGePiIds(IdConversionResult.of("2743")));
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(1);

        // The event has human genes, so we shouldn't find anything was another tax ID.
        bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withListBGePiIds(IdConversionResult.of("2743")).withTaxId("10110"));
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testFulltextSearchWithTaxFilter() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.getFulltextFilteredEvents(new GepiRequestData().withSentenceFilterString("NMDA receptor").withIncludeUnary(true), 0, 5, false);
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(5);

        // The event has human genes, so we shouldn't find anything was another tax ID.
        bipartiteEventsEvents = eventRetrievalService.getFulltextFilteredEvents(new GepiRequestData().withSentenceFilterString("NMDA receptor").withTaxId("4711"), 0, 5, false);
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(0);
    }


    @Test
    public void testOpenSearchWithEventTypeFilter1() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withIncludeUnary(true).withListAGePiIds(IdConversionResult.of("10243")).withEventTypes("Binding"));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(4);
    }

    @Test
    public void testOpenSearchWithEventTypeFilter2() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("3930")).withEventTypes("Negative_regulation"));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testOpenSearchWithSentenceFilter1() throws Exception {
        // 10051776_FE6_0.0_0.0.json
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withIncludeUnary(true).withListAGePiIds(IdConversionResult.of("10243")).withEventTypes("Gene_expression").withSentenceFilterString("\"NMDA receptor\""));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(1);
        assertThat(outsideEvents.get().getEventList()).extracting(Event::getEventId).containsExactly("10051776_FE6_0.0_0.0");
    }

    @Test
    public void testOpenSearchWithSentenceFilter2() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("3930")).withEventTypes("Positive_regulation").withSentenceFilterString("stress"));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testOpenSearchWithSentenceParagraphFilter() throws Exception {
        // this test refers to test file 10022233_FE6_0_1.json
        // both filter keywords, for sentence and paragraph, are present for the same event items
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("1268")).withFilterFieldsConnectionOperator("AND").withSentenceFilterString("pertussis toxin").withParagraphFilterString("immune modulation"));
        assertThat(outsideEvents.get().getEventList()).isNotEmpty();
        assertThat(outsideEvents.get().getEventList().get(0).getPmid()).isEqualTo("10022233");
    }

    @Test
    public void testOpenSearchWithSentenceParagraphFilter2() throws Exception {
        // This test refers to test files 10022233_FE6_0.0_1.0.json and 10022381_FE16_0.0_0.0.json.
        // Others are also hit but are not in focus of this test.
        // the 'pertussis toxin' phrase is present in the sentence of the former, the 'Heparinized blood' in the
        // paragraph of the latter.
        // Through the 'or' operator, we should get both
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> outsideEvents = eventRetrievalService.openSearch(new GepiRequestData().withIncludeUnary(true).withListAGePiIds(IdConversionResult.of("1268", "3458")).withFilterFieldsConnectionOperator("OR").withSentenceFilterString("\"pertussis toxin\"").withParagraphFilterString("\"Heparinized blood\""));
        assertThat(outsideEvents.get().getEventList()).extracting(Event::getEventId).contains("10022233_FE6_0.0_1.0", "10022381_FE16_0.0_0.0");
    }

    @Test
    public void testClosedSearchEvents() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withListBGePiIds(IdConversionResult.of("2743")));
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(1);

        final List<String> eventTypes = bipartiteEventsEvents.get().getEventList().stream().map(Event::getMainEventType).collect(Collectors.toList());
        final List<Argument> arguments = bipartiteEventsEvents.get().getEventList().stream().map(Event::getArguments).flatMap(List::stream).collect(Collectors.toList());

        final List<String> argumentPrefNames = arguments.stream().map(Argument::getPreferredName).collect(Collectors.toList());

        assertThat(eventTypes).containsExactlyInAnyOrder("Binding");
        assertThat(argumentPrefNames).containsExactlyInAnyOrder("GPHN", "GLRB");
    }

    @Test
    public void testClosedSearchEventsWithEventTypeFilter1() throws Exception {
        // 10029158_FE15_0.0_1.0.json
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("1440")).withListBGePiIds(IdConversionResult.of("3553")).withEventTypes("Localization"));
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(1);
    }

    @Test
    public void testClosedSearchEventsWithEventTypeFilter2() throws Exception {
        // 10051776_FE5_0.0_1.0.json
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withListBGePiIds(IdConversionResult.of("2743")).withEventTypes("Binding"));
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(1);
    }

    @Test
    public void testClosedSearchEventsWithEventTypeFilter3() throws Exception {
        // We should NOT find something because the event type filter doesn't fit.
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withListBGePiIds(IdConversionResult.of("2743")).withEventTypes("Phosphorylation"));
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testClosedSearchEventsWithSentenceFilter1() throws Exception {
        // 10051776_FE5_0.0_1.0.json
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withListBGePiIds(IdConversionResult.of("2743")).withEventTypes("Binding").withSentenceFilterString("sequence"));
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(1);
    }

    @Test
    public void testClosedSearchEventsWithSentenceFilter2() throws Exception {
        // there should be 0 hits because the 'stress' keyword is not contained in the found event sentence
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withListBGePiIds(IdConversionResult.of("8870")).withEventTypes("Binding").withSentenceFilterString("stress"));
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testClosedSearchWithSentenceParagraphFilter() throws Exception {
        // This tests uses 10049519_FE10_0.0_1.0 and 10051468_FE0_0.0_1.0.json
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("3569", "54315")).withListBGePiIds(IdConversionResult.of("7124", "21926")).withFilterFieldsConnectionOperator("OR").withSentenceFilterString("\"neutrophil infiltration\"").withParagraphFilterString("\"regenerating mice\"").withIncludeUnary(true));
        final List<Event> eventList = bipartiteEventsEvents.get().getEventList();
        assertThat(eventList).extracting(Event::getEventId).containsExactlyInAnyOrder("10049519_FE10_0.0_1.0", "10051468_FE0_0.0_1.0");
    }

    @Test
    public void getTotalNumberOfEvents() {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        long totalNumberOfEvents = eventRetrievalService.getTotalNumberOfEvents();
        // the number of JSON files in the test index directory
        assertThat(totalNumberOfEvents).isEqualTo(320);
    }

    @Test
    public void testOpenSearchEventWithSectionFilter() throws Exception {
        // First, establish the baseline: For gene ID 3458 we should find 8 events without filters
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> outsideEventsWithoutRestriction = eventRetrievalService.openSearch(new GepiRequestData().withIncludeUnary(true).withListAGePiIds(IdConversionResult.of("3458")));
        assertThat(outsideEventsWithoutRestriction.get().getEventList().size()).isEqualTo(8);

        // Filtering for "cytokine" on the headings should reduce the number of hits to 5
        Future<EventRetrievalResult> outsideEventsWithSectionFilter = eventRetrievalService.openSearch(new GepiRequestData().withIncludeUnary(true).withListAGePiIds(IdConversionResult.of("3458")).withSectionNameFilterString("cytokine"));
        assertThat(outsideEventsWithSectionFilter.get().getEventList().size()).isEqualTo(5);
    }

    @Test
    public void testHighlighting() throws Exception {
        // This tests uses 10049519_FE11_0_1.json and 10051468_FE0_0_1.json
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        Future<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.closedSearch(new GepiRequestData().withIncludeUnary(true).withListAGePiIds(IdConversionResult.of("7124")).withListBGePiIds(IdConversionResult.of("3569", "7351")).withFilterFieldsConnectionOperator("OR").withSentenceFilterString("\"neutrophil infiltration\"").withParagraphFilterString("\"regenerating mice\""));
        final List<Event> eventList = bipartiteEventsEvents.get().getEventList();
    }

    @Test
    public void testAggregationsOpenSearch() throws Exception {
        final IGeneIdService geneIdServiceMock = Mockito.mock(IGeneIdService.class);
        Mockito.when(geneIdServiceMock.getGeneInfo(Set.of("3458"))).thenReturn(Map.of("3458", GepiConceptInfo.builder().symbol("IFNG").build()));
        final EventRetrievalService eventRetrievalService = new EventRetrievalService(
                registry.getService(SymbolSource.class).valueForSymbol(GepiCoreSymbolConstants.INDEX_DOCUMENTS),
                LoggerFactory.getLogger(EventRetrievalService.class),
                registry.getService(IEventResponseProcessingService.class),
                geneIdServiceMock,
                registry.getService(ISearchServerComponent.class));
        Future<EsAggregatedResult> openAggregationResult = eventRetrievalService.openAggregatedSearch(new GepiRequestData().withIncludeUnary(true).withListAGePiIds(IdConversionResult.of("3458")));
        // As seen in other tests above, an open search for 3458 results in 8 hits - all of which need to hit 3458, IFNG. So:
        assertThat(openAggregationResult.get().getASymbolFrequencies().get("IFNG")).isEqualTo(8);
        // 1 event is with LBR, another with CPQ. The rest are unary events
        assertThat(openAggregationResult.get().getBSymbolFrequencies().get("LBR")).isEqualTo(1);
        assertThat(openAggregationResult.get().getBSymbolFrequencies().get("CPQ")).isEqualTo(1);
        assertThat(openAggregationResult.get().getEventFrequencies())
                .extractingFromEntries(e -> e.getKey().getFirstArgument().getTopHomologyPreferredName(), e -> e.getKey().getSecondArgument().getTopHomologyPreferredName(), e -> e.getValue())
                .containsExactly(Tuple.tuple("IFNG", "LBR", 1), Tuple.tuple("IFNG", "CPQ", 1), Tuple.tuple("IFNG", "none", 6));
    }

    @Test
    public void testAggregationsClosedSearch() throws Exception {
        final IGeneIdService geneIdServiceMock = Mockito.mock(IGeneIdService.class);
        Mockito.when(geneIdServiceMock.getGeneInfo(Set.of("10243"))).thenReturn(Map.of("10243", GepiConceptInfo.builder().symbol("GPHN").build()));
        final EventRetrievalService eventRetrievalService = new EventRetrievalService(
                registry.getService(SymbolSource.class).valueForSymbol(GepiCoreSymbolConstants.INDEX_DOCUMENTS),
                LoggerFactory.getLogger(EventRetrievalService.class),
                registry.getService(IEventResponseProcessingService.class),
                geneIdServiceMock,
                registry.getService(ISearchServerComponent.class));
        Future<EsAggregatedResult> closedAggregationResult = eventRetrievalService.closedAggregatedSearch(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withListBGePiIds(IdConversionResult.of("2743")));
        assertThat(closedAggregationResult.get().getASymbolFrequencies().get("GPHN")).isEqualTo(1);
        assertThat(closedAggregationResult.get().getBSymbolFrequencies().get("GLRB")).isEqualTo(1);
        assertThat(closedAggregationResult.get().getEventFrequencies())
                .extractingFromEntries(e -> e.getKey().getFirstArgument().getTopHomologyPreferredName(), e -> e.getKey().getSecondArgument().getTopHomologyPreferredName(), e -> e.getValue())
                .containsExactly(Tuple.tuple("GPHN", "GLRB", 1));
    }

    @Test
    public void testAggregationsFulltextSearch() throws Exception {
        // We don't need to mock a call to the service because no call should be done.
        // The fulltext search does not make a difference between A and B since no A and B IDs are given.
        final IGeneIdService geneIdServiceMock = Mockito.mock(IGeneIdService.class);
        final EventRetrievalService eventRetrievalService = new EventRetrievalService(
                registry.getService(SymbolSource.class).valueForSymbol(GepiCoreSymbolConstants.INDEX_DOCUMENTS),
                LoggerFactory.getLogger(EventRetrievalService.class),
                registry.getService(IEventResponseProcessingService.class),
                geneIdServiceMock,
                registry.getService(ISearchServerComponent.class));
        Future<EsAggregatedResult> closedAggregationResult = eventRetrievalService.fulltextAggregatedSearch(new GepiRequestData().withSentenceFilterString("NMDA receptor").withIncludeUnary(true));
        // just check a few arguments. There are too many to check them all.
        assertThat(closedAggregationResult.get().getASymbolFrequencies().get("GHR")).isEqualTo(4);
        assertThat(closedAggregationResult.get().getASymbolFrequencies().get("GPHN")).isEqualTo(3);
        assertThat(closedAggregationResult.get().getASymbolFrequencies().get("PKC")).isEqualTo(2);
        assertThat(closedAggregationResult.get().getASymbolFrequencies().get("TACR1")).isEqualTo(2);
        assertThat(closedAggregationResult.get().getASymbolFrequencies().get("GRIA1")).isEqualTo(1);
        assertThat(closedAggregationResult.get().getASymbolFrequencies().get("GRIA2")).isEqualTo(1);
        assertThat(closedAggregationResult.get().getASymbolFrequencies().get("GRIA3")).isEqualTo(1);
        assertThat(closedAggregationResult.get().getASymbolFrequencies().get("BCL2")).isEqualTo(1);
        assertThat(closedAggregationResult.get().getBSymbolFrequencies().get("IL1B")).isEqualTo(1);
        assertThat(closedAggregationResult.get().getEventFrequencies())
                .extractingFromEntries(e -> e.getKey().getFirstArgument().getTopHomologyPreferredName(), e -> e.getKey().getSecondArgument().getTopHomologyPreferredName(), e -> e.getValue())
                .contains(Tuple.tuple("GPHN", "none", 3), Tuple.tuple("PGR", "none", 1), Tuple.tuple("TACR1", "none", 2), Tuple.tuple("G protein-coupled receptors", "none", 1), Tuple.tuple("CSF3", "IL1B", 1));
    }


}
