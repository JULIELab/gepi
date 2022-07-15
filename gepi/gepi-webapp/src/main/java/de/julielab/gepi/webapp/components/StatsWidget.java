package de.julielab.gepi.webapp.components;

import com.google.common.collect.Multimap;
import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;
import de.julielab.gepi.core.services.IGePiDataService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StatsWidget extends GepiWidget {

    @Inject
    private IGePiDataService dataService;

    @Property
    private String viewMode;
    @Property
    private Triple<String, String, Integer> topInteractionsLoopItem;
    // used for A- and B items
    @Property
    private Pair<String, String> inputMappingLoopItem;

    public int getNumberUniqueASymbols() {
        try {
            return (int) getPagedEsResult().get().getEventList().stream().map(Event::getFirstArgument).map(Argument::getPreferredName).distinct().count();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
    }

    public int getNumberUniqueBSymbols() {
        try {
            return (int) getPagedEsResult().get().getEventList().stream().map(Event::getSecondArgument).map(Argument::getPreferredName).distinct().count();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
    }

    public long getNumberUniqueABPairs() {
        // TODO unique on what level? The current default is the preferred name. But it really depends, it could also be on gene ID level which would be much more
        try {
            return getPagedEsResult().get().getEventList().stream().map(e -> e.getFirstArgument().getPreferredName() + "-" + e.getSecondArgument().getPreferredName()).distinct().count();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
    }

    public int getNumEvents() {
        try {
            return getPagedEsResult().get().getEventList().size();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
    }

    public List<Triple<String, String, Integer>> getTopInteractions() {
        int n = 10;
        try {
            Map<Pair<String, String>, Integer> cardinalityMap = getPagedEsResult().get()
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
        } catch (InterruptedException | ExecutionException e) {
            return Collections.emptyList();
        }
    }


    public List<Pair<String, String>> getAInputMapping() {
        try {
            final IdConversionResult conversionResult = requestData.getListAGePiIds().get();
            final Multimap<String, String> convertedItems = conversionResult.getConvertedItems();
            List<Pair<String, String>> ret = new ArrayList<>();
            for (String inputId : convertedItems.keySet()) {
                for (String mappedId : convertedItems.get(inputId)) {
                    ret.add(new ImmutablePair<>(inputId, mappedId));
                }
            }
            return ret;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    // TODO get num events per event type

}
