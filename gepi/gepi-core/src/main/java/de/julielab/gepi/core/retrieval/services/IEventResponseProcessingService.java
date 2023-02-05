package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.components.data.aggregation.TermsAggregation;
import de.julielab.elastic.query.services.IElasticServerResponse;
import de.julielab.gepi.core.retrieval.data.EsAggregatedResult;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;

import java.util.Set;

/**
 * Converts the event query response received from ElasticSearch into event
 * result objects usable withing GePi.
 * 
 * @author faessler
 *
 */
public interface IEventResponseProcessingService {

	EventRetrievalResult getEventRetrievalResult(IElasticServerResponse response);

    EsAggregatedResult getEventRetrievalAggregatedResult(IElasticServerResponse singleSearchServerResponse, TermsAggregation eventCountRequest, Set<String> aListIdsAsSet);
}
