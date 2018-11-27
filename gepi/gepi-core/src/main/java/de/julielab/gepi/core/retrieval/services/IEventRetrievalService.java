package de.julielab.gepi.core.retrieval.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.services.IGeneIdService.IdType;

public interface IEventRetrievalService {
	/**
	 * Retrieves events between two lists of genes. The IDs may be of any
	 * accepted type (see {@link IdType}) and will be converted automatically.
	 * 
	 * @param idStream1
	 * @param idStream2
	 * @return Events between the two streams of IDs.
	 */
	CompletableFuture<EventRetrievalResult> getBipartiteEvents(Future<Stream<String>> idStream1, Future<Stream<String>> idStream2, List<String> eventTypes, String sentenceFilter);
	CompletableFuture<EventRetrievalResult> getBipartiteEvents(Stream<String> idStream1, Stream<String> idStream2, List<String> eventTypes, String sentenceFilter);

	/**
	 * Retrieves events between the input genes and any genes not on the list.
	 * The IDs may be of any accepted type (see {@link IdType}) and will be
	 * converted automatically.
	 * 
	 * @param idStream
	 * @return Events between genes identified by the input stream and other
	 *         genes.
	 */
	CompletableFuture<EventRetrievalResult> getOutsideEvents(Future<Stream<String>> idStream, List<String> eventTypes, String sentenceFilter);

	CompletableFuture<EventRetrievalResult> getOutsideEvents(Stream<String> idStream, List<String> eventTypes, String sentenceFilter);
}
