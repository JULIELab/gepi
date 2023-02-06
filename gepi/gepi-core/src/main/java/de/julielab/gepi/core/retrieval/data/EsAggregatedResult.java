package de.julielab.gepi.core.retrieval.data;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class EsAggregatedResult {
    private Map<String, Long> aSymbolFrequencies = new HashMap<>();
    private Map<String, Long> bSymbolFrequencies = new HashMap<>();
    private Map<Event, Long> eventFrequencies = new HashMap<>();

    /**
     * <p>
     * Adds an event and its frequency in the result.
     * </p>
     * <p>
     * It is expected that the arguments in <tt>eventPair</tt> are already sorted in a way that the first argument
     * belongs to the A-list, if applicable.
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
    public void addArgumentPair(List<String> eventPair, long count) {
        final Argument arg1 = new AggregatedArgument(eventPair.get(0));
        final Argument arg2 = new AggregatedArgument(eventPair.get(1));
        final Event event = new Event();
        event.setArguments(List.of(arg1, arg2));
        eventFrequencies.put(event, count);
        // add the count to the arguments
        aSymbolFrequencies.compute(arg1.getTopHomologyPreferredName(), (k, v) -> v != null ? v + count : count);
        bSymbolFrequencies.compute(arg2.getTopHomologyPreferredName(), (k, v) -> v != null ? v + count : count);
    }
}
