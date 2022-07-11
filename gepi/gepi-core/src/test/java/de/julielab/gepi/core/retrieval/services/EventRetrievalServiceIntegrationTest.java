package de.julielab.gepi.core.retrieval.services;

import de.julielab.gepi.core.retrieval.data.*;
import de.julielab.gepi.core.services.GePiCoreTestModule;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

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
    public void testGetOutsideEvents() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.getOutsideEvents(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(3);

        final List<String> eventTypes = outsideEvents.get().getEventList().stream().map(Event::getMainEventType).collect(Collectors.toList());
        final List<Argument> arguments = outsideEvents.get().getEventList().stream().map(Event::getArguments).flatMap(List::stream).collect(Collectors.toList());

        final List<String> geneids = arguments.stream().map(Argument::getGeneId).collect(Collectors.toList());

        assertThat(eventTypes).containsExactlyInAnyOrder("Positive_regulation", "Positive_regulation", "Binding");
        assertThat(geneids).containsExactlyInAnyOrder("10243", "10243", "10243", "2617", "26036", "8870");
    }

    @Test
    public void testGetOutsideEventsWithEventTypeFilter1() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.getOutsideEvents(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withEventTypes("Positive_regulation"));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(2);
    }

    @Test
    public void testGetOutsideEventsWithEventTypeFilter2() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.getOutsideEvents(new GepiRequestData().withListAGePiIds(IdConversionResult.of("3930")).withEventTypes("Negative_regulation"));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testGetOutsideEventsWithSentenceFilter1() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.getOutsideEvents(new GepiRequestData().withListAGePiIds(IdConversionResult.of("10243")).withEventTypes("Positive_regulation").withSentenceFilterString("essential"));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(2);
    }

    @Test
    public void testGetOutsideEventsWithSentenceFilter2() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.getOutsideEvents(new GepiRequestData().withListAGePiIds(IdConversionResult.of("3930")).withEventTypes("Positive_regulation").withSentenceFilterString( "stress"));
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testGetBipartiteEvents() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.getBipartiteEvents(IdConversionResult.of("10243"), IdConversionResult.of("8870"), Collections.emptyList(), null, null, null);
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
    public void testGetBipartiteEventsWithEventTypeFilter1() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.getBipartiteEvents(IdConversionResult.of("10243"), IdConversionResult.of("8870"), Arrays.asList("Binding"), null, null, null);
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(1);
    }

    @Test
    public void testGetBipartiteEventsWithEventTypeFilter2() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.getBipartiteEvents(IdConversionResult.of("10243"), IdConversionResult.of("8870"), Arrays.asList("Negative_regulation"), null, null, null);
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testGetBipartiteEventsWithSentenceFilter1() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.getBipartiteEvents(IdConversionResult.of("10243"), IdConversionResult.of("8870"), Arrays.asList("Binding"), "sequence", null, null);
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(1);
    }

    @Test
    public void testGetBipartiteEventsWithSentenceFilter2() throws Exception {
        // there should be 0 hits because the 'stress' keyword is not contained in the found event sentence
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.getBipartiteEvents(IdConversionResult.of("10243"), IdConversionResult.of("8870"), Arrays.asList("Binding"), "stress", null, null);
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void getTotalNumberOfEvents() {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        long totalNumberOfEvents = eventRetrievalService.getTotalNumberOfEvents();
        // the number of JSON files in the test index directory
        assertThat(totalNumberOfEvents).isEqualTo(57);
    }

    @Test
    public void testGetOutsideEventWithSectionFilter() throws Exception {
        // First, establish the baseline: For gene ID 3458 we should find 9 events without filters
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEventsWithoutRestriction = eventRetrievalService.getOutsideEvents(new GepiRequestData().withListAGePiIds(IdConversionResult.of("3458")));
        assertThat(outsideEventsWithoutRestriction.get().getEventList().size()).isEqualTo(9);

        // Filtering for "cytokine" on the headings should reduce the number of hits to 1
        CompletableFuture<EventRetrievalResult> outsideEventsWithSectionFilter = eventRetrievalService.getOutsideEvents(new GepiRequestData().withListAGePiIds(IdConversionResult.of("3458")).withSectionNameFilterString("cytokine"));
        assertThat(outsideEventsWithSectionFilter.get().getEventList().size()).isEqualTo(1);
    }
}
