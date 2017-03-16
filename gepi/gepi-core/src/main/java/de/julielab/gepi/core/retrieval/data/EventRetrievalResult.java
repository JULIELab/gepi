package de.julielab.gepi.core.retrieval.data;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventRetrievalResult {
	private Stream<Event> events;

	public Stream<Event> getEvents() {
		return events;
	}
	
	public List<Event> getEventList(){
		return events.collect(Collectors.toList());
	}

	public void setEvents(Stream<Event> events) {
		this.events = events;
	}
}
