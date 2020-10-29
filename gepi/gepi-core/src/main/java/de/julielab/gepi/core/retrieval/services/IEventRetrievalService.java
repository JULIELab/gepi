package de.julielab.gepi.core.retrieval.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;
import de.julielab.gepi.core.services.IGeneIdService.IdType;

public interface IEventRetrievalService {
	/**
	 * Retrieves events between two lists of genes. The IDs may be of any
	 * accepted type (see {@link IdType}) and will be converted automatically.
	 * 
	 * @param listAIds
	 * @param listBIds
	 * @param paragraphFilterString
     * @return Events between the two streams of IDs.
	 */
	CompletableFuture<EventRetrievalResult> getBipartiteEvents(Future<IdConversionResult> listAIds, Future<IdConversionResult> listBIds, List<String> eventTypes, String sentenceFilter, String paragraphFilterString);
	CompletableFuture<EventRetrievalResult> getBipartiteEvents(IdConversionResult idStream1, IdConversionResult idStream2, List<String> eventTypes, String sentenceFilter, String paragraphFilter);

	/**
	 * Retrieves events between the input genes and any genes not on the list.
	 * The IDs may be of any accepted type (see {@link IdType}) and will be
	 * converted automatically.
	 * 
	 * @param listAIds
	 * @param paragraphFilter
	 * @return Events between genes identified by the input stream and other
	 *         genes.
	 */
	CompletableFuture<EventRetrievalResult> getOutsideEvents(Future<IdConversionResult> listAIds, List<String> eventTypes, String sentenceFilter, String paragraphFilter);

	CompletableFuture<EventRetrievalResult> getOutsideEvents(IdConversionResult idStream, List<String> eventTypes, String sentenceFilter, String paragraphFilter);

	CompletableFuture<EventRetrievalResult> getFulltextFilteredEvents(List<String> eventTypes, String sentenceFilter, String paragraphFilter);
}
