package de.julielab.gepi.core.retrieval.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventRetrievalResult {
	private final static Logger log = LoggerFactory.getLogger(EventRetrievalResult.class);
	public enum EventResultType {OUTSIDE, BIPARTITE, FULLTEXT_FILTERED}
	private List<Event> eventList;
	private EventResultType resultType;

	public List<Event> getEventList() {
		log.warn("Returning {} events from {}", eventList.size(), this);
		return Collections.unmodifiableList(eventList);
	}

	public void setEvents(Stream<Event> events) {
		eventList = events.collect(Collectors.toList());
		log.warn("Got {} events", eventList.size());
	}

	public EventResultType getResultType() {
		return resultType;
	}

	public void setResultType(EventResultType resultType) {
		this.resultType = resultType;
	}
}
