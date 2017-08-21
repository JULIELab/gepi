package de.julielab.gepi.core.retrieval.services;

import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_ARG_CONCEPT_IDS;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_ARG_GENE_IDS;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_ARG_PREFERRED_NAME;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_ARG_TEXT;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_LIKELIHOOD;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_MAINEVENTTYPE;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_NUMARGUMENTS;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_NUMDISTINCTARGUMENTS;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_SENTENCE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.tapestry5.annotations.Log;
import org.slf4j.Logger;

import de.julielab.elastic.query.components.data.ISearchServerDocument;
import de.julielab.elastic.query.services.ISearchServerResponse;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.Argument;

public class EventResponseProcessingService implements IEventResponseProcessingService {

	private Logger log;

	public EventResponseProcessingService(Logger log) {
		this.log = log;
	}

	@Log
	@Override
	public EventRetrievalResult getEventRetrievalResult(ISearchServerResponse response) {
		if (response.getQueryError() != null) {
			log.error("Error while querying ElasticSearch: {}", response.getQueryErrorMessage());
			throw new IllegalStateException(
					"The ElasticSearch server is down or was not queried correctly: There was no response.");
			// return new EventRetrievalResult();
		}
		// Gets, first, from all document hits their inner (event) hits and,
		// second, converts all inner event hits into instances of the Event
		// class and hence returns all events.
		Stream<Event> eventStream = resultDocuments2Events(getEventDocuments(response));
		EventRetrievalResult eventRetrievalResult = new EventRetrievalResult();
		eventRetrievalResult.setEvents(eventStream);
		return eventRetrievalResult;
	}

	private Stream<Event> resultDocuments2Events(Stream<ISearchServerDocument> documents) {
		return documents.map(eventDocument -> {
			List<Object> conceptIds = eventDocument.getFieldValues(FIELD_EVENT_ARG_CONCEPT_IDS)
					.orElse(Collections.emptyList());
			List<Object> geneIds = eventDocument.getFieldValues(FIELD_EVENT_ARG_GENE_IDS)
					.orElse(Collections.emptyList());
			List<Object> topHomologyIds = eventDocument.getFieldValues(FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS)
					.orElse(Collections.emptyList());
			List<Object> texts = eventDocument.getFieldValues(FIELD_EVENT_ARG_TEXT).orElse(Collections.emptyList());
			List<Object> preferredNames = eventDocument.getFieldValues(FIELD_EVENT_ARG_PREFERRED_NAME)
					.orElse(Collections.emptyList());
			Optional<String> mainEventType = eventDocument.get(FIELD_EVENT_MAINEVENTTYPE);
			Optional<Integer> likelihood = eventDocument.get(FIELD_EVENT_LIKELIHOOD);
			Optional<String> sentence = eventDocument.get(FIELD_EVENT_SENTENCE);
			Optional<Integer> numDistinctArguments = eventDocument.get(FIELD_EVENT_NUMDISTINCTARGUMENTS);
			String documentId = eventDocument.getId();
			String documentType = eventDocument.getIndexType();

			Map<String, List<String>> highlights = eventDocument.getHighlights();

			int numArguments = (int) eventDocument.get(FIELD_EVENT_NUMARGUMENTS).get();
			List<Argument> arguments = new ArrayList<>();
			for (int i = 0; i < numArguments; ++i) {
				String conceptId = i < conceptIds.size() ? (String) conceptIds.get(i) : null;
				String geneId = i < geneIds.size() ? (String) geneIds.get(i) : null;
				String topHomologyId = i < topHomologyIds.size() ? (String) topHomologyIds.get(i) : null;
				String text = i < texts.size() ? (String) texts.get(i) : null;
				String preferredName = i < preferredNames.size() ? (String) preferredNames.get(i) : null;

				arguments.add(new Argument(geneId, conceptId, topHomologyId, preferredName, text));
			}

			Event event = new Event();
			event.setDocumentId(documentId);
			event.setDocumentType(documentType);
			event.setArguments(arguments);
			event.setNumDistinctArguments(numDistinctArguments.get());
			if (likelihood.isPresent())
				event.setLikelihood(likelihood.get());
			event.setMainEventType(mainEventType.get());
			event.setHighlightedSentence(highlights.getOrDefault(FIELD_EVENT_SENTENCE, Collections.emptyList()).stream()
					.findFirst().orElse(null));
			if (sentence.isPresent())
				event.setSentence(sentence.get());

			return event;
		});
	}

	private Stream<ISearchServerDocument> getEventDocuments(ISearchServerResponse response) {
		return response.getDocumentResults().flatMap(document -> {
			return document.getInnerHits().getOrDefault(EventRetrievalService.FIELD_EVENTS, Collections.emptyList())
					.stream();
		});
	}
}
