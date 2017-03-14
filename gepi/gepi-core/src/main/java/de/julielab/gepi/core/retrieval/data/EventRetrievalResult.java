package de.julielab.gepi.core.retrieval.data;

import java.util.stream.Stream;

public class EventRetrievalResult {
	private Stream<Event> events;

	public Stream<Event> getEvents() {
		return events;
	}

	public void setEvents(Stream<Event> events) {
		this.events = events;
	}
}
