package de.julielab.gepi.core.retrieval.data;

import de.julielab.gepi.core.retrieval.services.EventRetrievalService;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
public class EsAggregatedResult {
    private Map<String, Integer> aSymbolFrequencies = new HashMap<>();
    private Map<String, Integer> bSymbolFrequencies = new HashMap<>();
    private List<Pair<Event, Integer>> eventFrequencies = new ArrayList<>();
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
        eventFrequencies.add(ImmutablePair.of(event, count));
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

    public void normalizeGeneSymbols() {
        aSymbolFrequencies = normalizeSymbolsInArgumentPosition(aSymbolFrequencies, Event::getFirstArgument);
        bSymbolFrequencies = normalizeSymbolsInArgumentPosition(bSymbolFrequencies, Event::getSecondArgument);
        eventFrequencies = accumulateEventAfterNormalizingArguments();
    }

    private List<Pair<Event, Integer>> accumulateEventAfterNormalizingArguments() {
        final Map<Pair<Argument, Argument>, List<Pair<Event, Integer>>> normalizedPairGroups = eventFrequencies.stream().collect(Collectors.groupingBy(p -> Pair.of(p.getLeft().getFirstArgument(), p.getLeft().getSecondArgument())));
        List<Pair<Event, Integer>> normalizedEventFrequencies = new ArrayList<>(normalizedPairGroups.size());
        for (Pair<Argument, Argument> p : normalizedPairGroups.keySet()) {
            final List<Pair<Event, Integer>> countsForNormalizedEqualEvents = normalizedPairGroups.get(p);
            final int normalizedEventsSum = countsForNormalizedEqualEvents.stream().mapToInt(Pair::getRight).sum();
            // Just get any one of the event group as the new representative for the count - they are equal anyway.
            final Optional<Event> anyNormalizedEvent = countsForNormalizedEqualEvents.stream().map(Pair::getLeft).findAny();
            normalizedEventFrequencies.add(Pair.of(anyNormalizedEvent.get(), normalizedEventsSum));
        }
        Collections.sort(normalizedEventFrequencies, Comparator.<Pair<Event, Integer>, Integer>comparing(Pair::getRight).reversed());
        return normalizedEventFrequencies;
    }

    private Map<String, Integer> normalizeSymbolsInArgumentPosition(Map<String, Integer> symbolFrequencies, Function<Event, Argument> argumentSelector) {
        Map<String, Integer> normalizedFrequencies = new HashMap<>(symbolFrequencies.size());
        Map<String, String> normalized2maxFreqSymbol = new HashMap<>(symbolFrequencies.size());
        final Map<String, List<String>> normalized2varied = symbolFrequencies.keySet().stream().collect(Collectors.groupingBy(GeneSymbolNormalization::normalize));
        for (String normalizedSymbol : normalized2varied.keySet()) {
            final List<String> variedSymbols = normalized2varied.get(normalizedSymbol);
            String maxFreqSymbol = null;
            int maxFreq = 0;
            int symbolFreqSum = 0;
            for (String symbol : variedSymbols) {
                int freq = symbolFrequencies.get(symbol);
                symbolFreqSum += freq;
                if (freq > maxFreq) {
                    maxFreq = freq;
                    maxFreqSymbol = symbol;
                }
            }
            normalizedFrequencies.put(maxFreqSymbol, symbolFreqSum);
            normalized2maxFreqSymbol.put(normalizedSymbol, maxFreqSymbol);
        }
        // Set the majority vote symbol in the event arguments
        for (Pair<Event, Integer>  p : eventFrequencies) {
            final Argument argument = argumentSelector.apply(p.getLeft());
            // We work with the top homology name/symbol for aggregations
            final String maxFreqSymbol = normalized2maxFreqSymbol.get(GeneSymbolNormalization.normalize(argument.getTopHomologyPreferredName()));
            argument.setTopHomologyPreferredName(maxFreqSymbol);
        }
        return normalizedFrequencies;
    }
}
