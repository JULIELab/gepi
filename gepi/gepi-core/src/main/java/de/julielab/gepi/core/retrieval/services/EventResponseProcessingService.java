package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.components.data.ISearchServerDocument;
import de.julielab.elastic.query.services.IElasticServerResponse;
import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import org.apache.tapestry5.annotations.Log;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Stream;

import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.*;

public class EventResponseProcessingService implements IEventResponseProcessingService {

	@Inject
	private IEventPostProcessingService eventPPService;

	private Logger log;

	public EventResponseProcessingService(Logger log) {
		this.log = log;
	}

	@Log
	@Override
	public EventRetrievalResult getEventRetrievalResult(IElasticServerResponse response) {
		if (response.getQueryErrorMessage() != null) {
			log.error("Error while querying ElasticSearch: {}", response.getQueryErrorMessage());
			throw new IllegalStateException(
					"The ElasticSearch server is down or was not queried correctly: There was no response.");
			// return new EventRetrievalResult();
		}
		// Gets, first, from all document hits their inner (event) hits and,
		// second, converts all inner event hits into instances of the Event
		// class and hence returns all events.
		Stream<Event> eventStream = resultDocuments2Events(response.getDocumentResults());
		EventRetrievalResult eventRetrievalResult = new EventRetrievalResult();
		eventRetrievalResult.setEvents(eventStream);
		log.trace("Size of the event retrieval result (number of events): {}", eventRetrievalResult.getEventList().size());
		// postprocess eventPreferred names first with given neo4j information
//		eventPPService.setPreferredNameFromConceptId(eventRetrievalResult.getEventList());
//		eventPPService.setArgumentGeneIds(eventRetrievalResult.getEventList());
		return eventRetrievalResult;
	}

	private Stream<Event> resultDocuments2Events(Stream<ISearchServerDocument> documents) {
		return documents.map(eventDocument -> {
            Optional<String> pmid = eventDocument.getFieldValue(FIELD_PMID);
            Optional<String> pmcid = eventDocument.getFieldValue(FIELD_PMCID);
			List<Object> conceptIds = eventDocument.getFieldValues(FIELD_EVENT_ARG_CONCEPT_IDS)
					.orElse(Collections.emptyList());
			List<Object> geneIds = eventDocument.getFieldValues(FIELD_EVENT_ARG_GENE_IDS)
					.orElse(Collections.emptyList());
			List<Object> topHomologyIds = eventDocument.getFieldValues(FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS)
					.orElse(Collections.emptyList());
            List<Object> argPrefNames = eventDocument.getFieldValues(FIELD_EVENT_ARG_PREFERRED_NAME)
                    .orElse(Collections.emptyList());
            List<Object> argHomologyPrefNames = eventDocument.getFieldValues(FIELD_EVENT_ARG_HOMOLOGY_PREFERRED_NAME)
                    .orElse(Collections.emptyList());
			List<Object> texts = eventDocument.getFieldValues(FIELD_EVENT_ARG_TEXT).orElse(Collections.emptyList());
			Optional<String> mainEventType = eventDocument.get(FIELD_EVENT_MAINEVENTTYPE);
			Optional<Integer> likelihood = eventDocument.get(FIELD_EVENT_LIKELIHOOD);
			Optional<String> sentence = eventDocument.get(FIELD_EVENT_SENTENCE);
			String eventId = eventDocument.getId();

			Map<String, List<String>> highlights = eventDocument.getHighlights();

			// In the past we could have multiple arguments per event. This has been broken down on the index
			// side and is always 2.
			int numArguments = 2;
			List<Argument> arguments = new ArrayList<>();
			for (int i = 0; i < numArguments; ++i) {
				String conceptId = i < conceptIds.size() ? (String) conceptIds.get(i) : null;
				String geneId = i < geneIds.size() ? (String) geneIds.get(i) : null;
				String topHomologyId = i < topHomologyIds.size() ? (String) topHomologyIds.get(i) : null;
				String text = i < texts.size() ? (String) texts.get(i) : null;

				if (conceptId != null) {

					// assert conceptId != null : "No concept ID received from event document with
					// ID " + eventDocument.getId() + ":\n" + eventDocument;
					assert geneId != null;
					assert topHomologyId != null;
					assert text != null;

					arguments.add(new Argument(geneId, conceptId, topHomologyId, text));
				} else {
					log.warn(
							"Came over event document where the concept Id of an argument missing. Document is skipped. This must be fixed in the index. The document was {}",
							eventDocument);
					return null;
				}
			}

			Event event = new Event();
			pmid.ifPresent(event::setPmid);
			pmcid.ifPresent(event::setPmcid);
			event.setEventId(eventId);
			//event.setDocumentType(documentType);
			event.setArguments(arguments);
			if (likelihood.isPresent())
				event.setLikelihood(likelihood.get());
			event.setMainEventType(mainEventType.get());
			event.setHighlightedSentence(highlights.getOrDefault(FIELD_EVENT_SENTENCE, Collections.emptyList()).stream()
					.findFirst().orElse(null));
			if (sentence.isPresent())
				event.setSentence(sentence.get());
			for (int i = 0; i < event.getNumArguments(); i++) {
                event.getArgument(i).setPreferredName((String) argPrefNames.get(i));
                event.getArgument(i).setTopHomologyPreferredName((String) argHomologyPrefNames.get(i));
            }

			return event;
		}).filter(Objects::nonNull);
	}


}
