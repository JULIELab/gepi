package de.julielab.gepi.core.retrieval.services;

import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_ALLARGUMENTS;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_ALLEVENTTYPES;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_LIKELIHOOD;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_MAINEVENTTYPE;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_NUMDISTINCTARGUMENTS;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_EVENT_SENTENCE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tapestry5.annotations.Log;

import de.julielab.elastic.query.components.data.ISearchServerDocument;
import de.julielab.elastic.query.services.ISearchServerResponse;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;

public class EventResponseProcessingService implements IEventResponseProcessingService {

	@Log
	@Override
	public EventRetrievalResult getEventRetrievalResult(ISearchServerResponse response) {
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
			Optional<List<Object>> allArguments = eventDocument.getFieldValues(FIELD_EVENT_ALLARGUMENTS);
			Optional<String> mainEventType = eventDocument.get(FIELD_EVENT_MAINEVENTTYPE);
			Optional<List<Object>> allEventTypes = eventDocument.getFieldValues(FIELD_EVENT_ALLEVENTTYPES);
			Optional<Integer> likelihood = eventDocument.get(FIELD_EVENT_LIKELIHOOD);
			Optional<String> sentence = eventDocument.get(FIELD_EVENT_SENTENCE);
			Optional<Integer> numArguments = eventDocument.get(FIELD_EVENT_NUMDISTINCTARGUMENTS);

			Map<String, List<String>> highlights = eventDocument.getHighlights();

			Event event = new Event();
			event.setAllArgumentTokens(allArguments.orElse(Collections.emptyList()).stream().map(o -> (String)o).collect(Collectors.toList()));
			event.setAllEventTypes(allEventTypes.orElse(Collections.emptyList()).stream().map(o -> (String)o).collect(Collectors.toList()));
			event.setLikelihood(likelihood.get());
			event.setMainEventType(mainEventType.get());
			event.setHighlightedSentence(highlights.getOrDefault(FIELD_EVENT_SENTENCE, Collections.emptyList()).stream().findFirst().orElse(null));
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
