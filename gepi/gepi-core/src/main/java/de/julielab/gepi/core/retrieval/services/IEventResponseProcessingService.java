package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.services.ISearchServerResponse;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;

/**
 * Converts the event query response received from ElasticSearch into event
 * result objects usable withing GePi.
 * 
 * @author faessler
 *
 */
public interface IEventResponseProcessingService {

	EventRetrievalResult getEventRetrievalResult(ISearchServerResponse response);

}
