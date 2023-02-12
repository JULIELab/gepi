package de.julielab.gepi.webapp.components;

import com.google.common.collect.Multimap;
import de.julielab.gepi.core.retrieval.data.EsAggregatedResult;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;
import de.julielab.gepi.core.retrieval.data.InputMode;
import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.gepi.webapp.data.InputMapping;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class StatsWidget extends GepiWidget {

    @Inject
    private Logger log;
    @Inject
    private IGePiDataService dataService;
    @Property
    private String viewMode;
    @Property
    private Triple<String, String, Integer> topInteractionsLoopItem;
    // used for A- and B items
    @Property
    private InputMapping inputMappingLoopItem;
    @InjectComponent
    @Property
    private InputListMappingTable aMapping;

    public int getNumberUniqueASymbols() {
        try {
            return getEsAggregatedResult().get().getASymbolFrequencies().size();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
    }

    public int getNumberUniqueBSymbols() {
        try {
            return getEsAggregatedResult().get().getBSymbolFrequencies().size();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
    }

    public long getNumberUniqueABPairs() {
        try {
            return getEsAggregatedResult().get().getEventFrequencies().keySet().stream().filter(e -> e.getArity() == 2).count();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
    }

    public long getNumEvents() {
        try {
            return getEsAggregatedResult().get().getTotalNumEvents();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
    }

//    public List<Triple<String, String, Integer>> getTopInteractions() {
//        int n = 10;
//        try {
//            Map<Pair<String, String>, Integer> cardinalityMap = getUnrolledResult4charts().get()
//                    .getEventList().stream()
//                    .filter(e -> !EventRetrievalService.FIELD_VALUE_MOCK_ARGUMENT.equals(e.getFirstArgument().getTopHomologyPreferredName()) && !EventRetrievalService.FIELD_VALUE_MOCK_ARGUMENT.equals(e.getSecondArgument().getTopHomologyPreferredName()))
//                    .map(e -> new ImmutablePair<>(e.getFirstArgument().getTopHomologyPreferredName(), e.getSecondArgument().getTopHomologyPreferredName()))
//                    .collect(Collectors.toMap(Function.identity(), x -> 1, Integer::sum));
//            List<Triple<String, String, Integer>> topInteractions = new ArrayList<>(cardinalityMap.size());
//            for (Pair<String, String> symbolPair : cardinalityMap.keySet()) {
//                Integer count = cardinalityMap.get(symbolPair);
//                topInteractions.add(new ImmutableTriple<>(symbolPair.getLeft(), symbolPair.getRight(), count));
//            }
//            topInteractions.sort(Comparator.<Triple<String, String, Integer>>comparingInt(Triple::getRight).reversed());
//            return topInteractions.subList(0, Math.min(n, topInteractions.size()));
//        } catch (InterruptedException | ExecutionException e) {
//            return Collections.emptyList();
//        }
//    }

    public List<Triple<String, String, Integer>> getTopInteractions() {
        try {
            int n = 10;
            final Future<EsAggregatedResult> esAggregatedResult = getEsAggregatedResult();
            final Map<Event, Integer> eventFrequencies = esAggregatedResult.get().getEventFrequencies();
            final Iterator<Event> keyIt = eventFrequencies.keySet().iterator();
            int i = 0;
            List<Triple<String, String, Integer>> topInteractions = new ArrayList<>(n);
            while (keyIt.hasNext() && i < n) {
                Event e = keyIt.next();
                final Triple<String, String, Integer> triple = new ImmutableTriple(e.getFirstArgument().getTopHomologyPreferredName(), e.getSecondArgument().getTopHomologyPreferredName(), eventFrequencies.get(e));
                topInteractions.add(triple);
                ++i;
            }
            return topInteractions;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Could not obtain top interactions", e);
            return Collections.emptyList();
        }
    }

    public int getInputSize(String list, String type) {
        try {
            final IdConversionResult idConversionResult = list.equalsIgnoreCase("a") ? requestData.getListAGePiIds().get() : requestData.getListBGePiIds().get();
            if (type.equalsIgnoreCase("converted")) {
                final Multimap<String, String> convertedItems = idConversionResult.getConvertedItems();
                return convertedItems.keySet().size();
            }
            return (int) idConversionResult.getUnconvertedItems().count();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean isAList() {
        return requestData.getInputMode().contains(InputMode.A) || isBList();
    }

    public boolean isBList() {
        return requestData.getInputMode().contains(InputMode.AB);
    }

}
