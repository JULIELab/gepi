package de.julielab.gepi.core.retrieval.data;

import de.julielab.gepi.core.retrieval.services.EventRetrievalService;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class EsAggregatedResult {
    private Map<String, Integer> aSymbolFrequencies = new HashMap<>();
    private Map<String, Integer> bSymbolFrequencies = new HashMap<>();
    private Map<Event, Integer> eventFrequencies = new LinkedHashMap<>();
    private long totalNumEvents;

    /**
     * <p>
     * Adds an event and its frequency in the result.
     * </p>
     * <p>
     * It is expected that the arguments in <tt>eventPair</tt> are already sorted in a way that the first argument
     * belongs to the A-list, if applicable. Also, the input is expected to be sorted descending by frequency. The order
     * is used to quickly obtain top-N events in the StatsWidget.
     * </p>
     * The given <tt>count</tt> must be the total count of the given event. If another event with the same argument
     * strings is passed, there will be two events with the same arguments and their own respective counts in this result.
     * The count of the arguments, on the other hand, is accumulated across events.
     * In other words, the passed events must be unique, the arguments not.
     * <p>
     * <p>Hint: The indexing code in <tt>de.julielab.gepi.indexing.RelationFieldValueGenerator</tt> sorts the
     * arguments alphabetically so you don't need to worry about the same argument combination appearing twice with
     * switched argument positions. This should not happen by design.</p>
     *
     * </p>
     *
     * @param eventPair A unique argument pair.
     * @param count     The total count of this pair.
     */
    public void addArgumentPair(List<String> eventPair, int count) {
        final Argument arg1 = new AggregatedArgument(eventPair.get(0));
        final Argument arg2 = eventPair.size() > 1 ? new AggregatedArgument(eventPair.get(1)) : null;
        final Event event = new Event();
        event.setArity(eventPair.get(1).equals(EventRetrievalService.FIELD_VALUE_MOCK_ARGUMENT) ? 1 : 2);
        event.setArguments(List.of(arg1, arg2));
        eventFrequencies.put(event, count);
        // add the count to the arguments
        aSymbolFrequencies.compute(arg1.getTopHomologyPreferredName(), (k, v) -> v != null ? v + count : count);
        bSymbolFrequencies.compute(arg2.getTopHomologyPreferredName(), (k, v) -> v != null ? v + count : count);
    }

    public void setTotalNumEvents(long totalNumEvents) {
        this.totalNumEvents = totalNumEvents;
    }

    public long getTotalNumEvents() {
        return totalNumEvents;
    }
}
