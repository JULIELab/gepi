package de.julielab.gepi.core.retrieval.data;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventRetrievalResult {
	private Stream<Event> events;
	private List<Event> eventList;

	public Stream<Event> getEvents() {
		return events;
	}
	
	public List<Event> getEventList(){
		if (eventList == null)
			eventList = events.collect(Collectors.toList());
		return eventList;
	}

	public void setEvents(Stream<Event> events) {
		this.events = events;
	}
}
