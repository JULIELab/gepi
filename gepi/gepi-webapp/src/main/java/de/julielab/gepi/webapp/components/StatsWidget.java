package de.julielab.gepi.webapp.components;

import com.google.common.collect.Multimap;
import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.GepiGeneInfo;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;
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
    @Inject
    private IGeneIdService geneIdService;

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


    public List<InputMapping> getAInputMapping(int n) {
        try {
            final IdConversionResult conversionResult = requestData.getListAGePiIds().get();
            final Multimap<String, String> convertedItems = conversionResult.getConvertedItems();
            List<InputMapping> ret = new ArrayList<>();
            final Map<String, GepiGeneInfo> geneInfo = geneIdService.getGeneInfo(convertedItems.values());
            for (String inputId : (Iterable<String>) () -> convertedItems.keySet().stream().sorted().iterator()) {
                if (n >= 0 && ret.size() == n)
                    break;
                // If the input was gene names, it may well happen that it is not mapped to a single gene node
                // in our Neo4j database but a lot of them, despite the fact that we map to ortholog aggregates.
                // The reason is that a lot of genes that probably should be contained in gene_orthologs are not yet
                // in there. So we have the aggregate with all the main species and a long tail of Neo4j nodes that
                // belong to some organism that was not yet accounted for in gene_orthologs. This is a technical detail
                // and the user should not be burdened with it. So, find the best representative, which should be
                // the aggregate, if we have one.
                GepiGeneInfo mappedRepresentative = null;
                for (String mappedId : convertedItems.get(inputId)) {
                    final GepiGeneInfo info = geneInfo.get(mappedId);
                    // Use the first item as representative, it is as good as any - except if we have an aggregate.
                    if (mappedRepresentative == null)
                        mappedRepresentative = info;
                    // Use the aggregate, if we have one.
                    if (info.isAggregate()) {
                        mappedRepresentative = info;
                        break;
                    }
                }
                ret.add(new InputMapping(inputId, mappedRepresentative));
            }
            final int maxSizeLeft = n >= 0 ? n - ret.size() : Integer.MAX_VALUE;
            conversionResult.getUnconvertedItems().sorted().limit(maxSizeLeft).map(i -> new InputMapping(i, null)).forEach(ret::add);
            return ret;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * <p>Used to set a CSS class to mapping items for which no database entry was found: text-danger (from Bootstrap).</p>
     *
     * @return An empty string if the current mapping loop item indicates that the mapping was successful, <code>text-danger</code> otherwise.
     */
    public String getMappingFoundClass() {
        return inputMappingLoopItem.targetFound() ? "" : "text-danger";
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

}
