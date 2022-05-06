package de.julielab.gepi.webapp.components;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.services.IGePiDataService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.neo4j.driver.util.Immutable;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StatsWidget extends GepiWidget{

    @Inject
    private IGePiDataService dataService;

    @Property
    private String viewMode;

    int getNumberUniqueASymbols() {
        try {
            return (int) getEsResult().get().getEventList().stream().map(Event::getFirstArgument).map(Argument::getPreferredName).distinct().count();
        } catch (InterruptedException| ExecutionException e) {
            return 0;
        }
    }

    int getNumberUniqueBSymbols() {
        try {
            return (int) getEsResult().get().getEventList().stream().map(Event::getSecondArgument).map(Argument::getPreferredName).distinct().count();
        } catch (InterruptedException| ExecutionException e) {
            return 0;
        }
    }

    int getNumEvents() {
        try {
            return getEsResult().get().getEventList().size();
        } catch (InterruptedException| ExecutionException e) {
            return 0;
        }
    }

    List<Triple<String, String, Integer>> getTopInteractions(int n) {
        try {
            Map<ImmutablePair<String, String>, Integer> cardinalityMap = getEsResult().get()
                    .getEventList().stream()
                    .map(e -> new ImmutablePair<>(e.getFirstArgument().getPreferredName(), e.getSecondArgument().getPreferredName()))
                    .collect(Collectors.toMap(Function.identity(), x -> 1, Integer::sum));
            List<Triple<String, String, Integer>> topInteractions = new ArrayList<>(cardinalityMap.size());
            for (Pair<String, String> symbolPair : cardinalityMap.keySet()) {
                Integer count = cardinalityMap.get(symbolPair);
                topInteractions.add(new ImmutableTriple<>(symbolPair.getLeft(), symbolPair.getRight(), count));
            }
            topInteractions.sort(Comparator.<Triple<String, String, Integer>>comparingInt(Triple::getRight).reversed());
            return topInteractions.subList(0, Math.min(n, topInteractions.size()));
        } catch (InterruptedException| ExecutionException e) {
            return Collections.emptyList();
        }
    }

    // TODO get num events per event type

}
