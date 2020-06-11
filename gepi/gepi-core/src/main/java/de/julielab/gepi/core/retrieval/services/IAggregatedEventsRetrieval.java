package de.julielab.gepi.core.retrieval.services;

import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Stream;

/**
 * Retrieves pre-aggregated event statistics instead of all individual events as {@link IEventRetrievalService} does.
 */
public interface IAggregatedEventsRetrieval {
    /**
     * A-Search.
     * @param idStream1
     * @param eventTypes
     * @return
     */
    AggregatedEventsRetrievalResult getEvents(Future<Stream<String>> idStream1, List<String> eventTypes);

    /**
     * A-B-Search.
     * @param idStream1
     * @param idStream2
     * @param eventTypes
     * @return
     */
    AggregatedEventsRetrievalResult getEvents(Future<Stream<String>> idStream1, Future<Stream<String>> idStream2, List<String> eventTypes);
}