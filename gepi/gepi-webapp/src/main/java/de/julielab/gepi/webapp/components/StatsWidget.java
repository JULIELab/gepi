package de.julielab.gepi.webapp.components;

import com.google.common.collect.Multimap;
import de.julielab.gepi.core.retrieval.data.*;
import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.gepi.core.services.IGeneIdService;
import de.julielab.gepi.webapp.data.InputMapping;
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
//    @Inject
//    private IGeneIdService geneIdService;

    @Property
    private String viewMode;
    @Property
    private Triple<String, String, Integer> topInteractionsLoopItem;
    // used for A- and B items
    @Property
    private InputMapping inputMappingLoopItem;

    public int getNumberUniqueASymbols() {
        try {
            return (int) getPagedEsResult().get().getEventList().stream().map(Event::getFirstArgument).map(Argument::getPreferredName).distinct().count();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
    }

    public int getNumberUniqueBSymbols() {
        try {
            return (int) getPagedEsResult().get().getEventList().stream().filter(e -> e.getArity() > 1).map(Event::getSecondArgument).map(Argument::getPreferredName).distinct().count();
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
            return getUnrolledResult4charts().get().getEventList().size();
        } catch (InterruptedException | ExecutionException e) {
            return 0;
        }
    }

    public List<Triple<String, String, Integer>> getTopInteractions() {
        int n = 10;
        try {
            Map<Pair<String, String>, Integer> cardinalityMap = getUnrolledResult4charts().get()
                    .getEventList().stream()
                    .map(e -> new ImmutablePair<>(e.getFirstArgument().getTopHomologyPreferredName(), e.getSecondArgument().getTopHomologyPreferredName()))
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
