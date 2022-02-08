package de.julielab.gepi.core.retrieval.data;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventRetrievalResult {

    private long numTotalRows;
    private List<Event> eventList;
    private EventResultType resultType;

    public long getNumTotalRows() {
        return numTotalRows;
    }

    public void setNumTotalRows(long numTotalRows) {
        this.numTotalRows = numTotalRows;
    }

    public List<Event> getEventList() {
        return Collections.unmodifiableList(eventList);
    }

    public void setEvents(Stream<Event> events) {
        eventList = events.collect(Collectors.toList());
    }

    public void setEvents(List<Event> eventList) {
        this.eventList = eventList;
    }

    public EventResultType getResultType() {
        return resultType;
    }

    public void setResultType(EventResultType resultType) {
        this.resultType = resultType;
    }

    public enum EventResultType {OUTSIDE, BIPARTITE, FULLTEXT_FILTERED}
}
