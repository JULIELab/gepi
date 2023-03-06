package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.components.data.SearchServerRequest;
import de.julielab.gepi.core.retrieval.data.EsAggregatedResult;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.GepiRequestData;
import de.julielab.gepi.core.services.IdType;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface IEventRetrievalService {
	Future<EventRetrievalResult> getEvents(GepiRequestData requestData, boolean forCharts);

    Future<EventRetrievalResult> getEvents(GepiRequestData requestData, int from, int numRows, boolean forCharts);

    /**
	 * Retrieves events between two lists of genes. The IDs may be of any
	 * accepted type (see {@link IdType}) and will be converted automatically.
	 * 
	 *
     * @param requestData
     * @param from
     * @param numRows
     * @param forCharts
     * @return Events between the two streams of IDs.
	 */
	Future<EventRetrievalResult> closedSearch(GepiRequestData requestData, int from, int numRows, boolean forCharts);
	Future<EventRetrievalResult> closedSearch(GepiRequestData gepiRequestData);

	/**
	 * Retrieves events between the input genes and any genes not on the list.
	 * The IDs may be of any accepted type (see {@link IdType}) and will be
	 * converted automatically.
	 * 
	 *
	 * @param requestData
	 * @return Events between genes identified by the input stream and other
	 *         genes.
	 */
	Future<EventRetrievalResult> openSearch(GepiRequestData requestData);

	Future<EventRetrievalResult> openSearch(GepiRequestData gepiRequestData, int from, int numRows, boolean forCharts);

	SearchServerRequest getOpenSearchRequest(GepiRequestData requestData) throws ExecutionException, InterruptedException;

	SearchServerRequest getOpenSearchRequest(GepiRequestData requestData, int from, int numRows, boolean forCharts) throws ExecutionException, InterruptedException;

	Future<EventRetrievalResult> getFulltextFilteredEvents(GepiRequestData requestData, int from, int numRows, boolean forCharts);

    long getTotalNumberOfEvents();

    Future<EsAggregatedResult> getAggregatedEvents(GepiRequestData requestData);
}
