package de.julielab.gepi.webapp.components;

import com.google.common.collect.Multimap;
import de.julielab.gepi.core.retrieval.data.GepiConceptInfo;
import de.julielab.gepi.core.retrieval.data.GepiRequestData;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;
import de.julielab.gepi.core.services.IGeneIdService;
import de.julielab.gepi.webapp.data.InputMapping;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class InputListMappingTable {
    @Inject
    private Logger log;
    @Property
    private InputMapping inputMappingLoopItem;

    @Inject
    private IGeneIdService geneIdService;

    @Parameter
    private GepiRequestData requestData;

    /**
     * 'a' or 'b' for list A or list B, respectively.
     */
    @Parameter(defaultPrefix = "literal")
    private String list;

    @Parameter
    private int maxTableSize;
    /**
     * <p>Used to set a CSS class to mapping items for which no database entry was found: text-danger (from Bootstrap).</p>
     *
     * @return An empty string if the current mapping loop item indicates that the mapping was successful, <code>text-danger</code> otherwise.
     */
    public String getMappingFoundClass() {
        return inputMappingLoopItem.targetFound() ? "" : "text-danger";
    }

    /**
     *
     * @return
     */
    public boolean hasOverflowItems() {
        try {
            return maxTableSize < getConversionResult().getSourceIds().size();
        } catch (InterruptedException| ExecutionException e) {
            log.error("Could not obtain conversion result for list {}", list, e);
        }
        return true;
    }

    public List<InputMapping> getInputMapping() {
        try {
            final IdConversionResult conversionResult = getConversionResult();
            final Multimap<String, String> convertedItems = conversionResult.getConvertedItems();
            List<InputMapping> ret = new ArrayList<>();
            // they values are always database concept or aggregate IDs
            log.debug("Obtaining GeneInfo for {} items.", convertedItems.values().size());
            long time = System.currentTimeMillis();
            final Map<String, GepiConceptInfo> geneInfo = geneIdService.getGeneInfo(convertedItems.values());
            time = System.currentTimeMillis() - time;
            log.debug("Obtaining GeneInfo took {}s", time/1000);
            for (String inputId : (Iterable<String>) () -> convertedItems.keySet().stream().sorted(String.CASE_INSENSITIVE_ORDER).iterator()) {
                if (maxTableSize >= 0 && ret.size() == maxTableSize)
                    break;
                // If the input was gene names, it may well happen that it is not mapped to a single gene node
                // in our Neo4j database but a lot of them, despite the fact that we map to ortholog aggregates.
                // The reason is that a lot of genes that probably should be contained in gene_orthologs are not yet
                // in there. So we have the aggregate with all the main species and a long tail of Neo4j nodes that
                // belong to some organism that was not yet accounted for in gene_orthologs. This is a technical detail
                // and the user should not be burdened with it. So, find the best representative, which should be
                // the aggregate, if we have one.
                final Map<String, List<GepiConceptInfo>> infoByNames = convertedItems.get(inputId).stream().map(geneInfo::get).collect(Collectors.groupingBy(GepiConceptInfo::getSymbol));
                String majoritySymbol = null;
                int maxSymbolCount = -1;
                for (String symbol : infoByNames.keySet()) {
                    final Integer count = infoByNames.get(symbol).size();
                    if (maxSymbolCount < count) {
                        maxSymbolCount = count;
                        majoritySymbol = symbol;
                    }
                }
                // get one representative of the majority symbol
                GepiConceptInfo mappedRepresentative = infoByNames.get(majoritySymbol).get(0);
                for (String mappedId : convertedItems.get(inputId)) {
                    final GepiConceptInfo info = geneInfo.get(mappedId);
                    // Use the aggregate, if we have one.
                    if (info.isAggregate()) {
                        mappedRepresentative = info;
                        break;
                    }
                }
                ret.add(new InputMapping(inputId, mappedRepresentative));
            }
            final int maxSizeLeft = maxTableSize >= 0 ? maxTableSize - ret.size() : Integer.MAX_VALUE;
            conversionResult.getUnconvertedItems().sorted().limit(maxSizeLeft).map(i -> new InputMapping(i, null)).forEach(ret::add);
            return ret;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private IdConversionResult getConversionResult() throws InterruptedException, ExecutionException {
        return Optional.ofNullable(list.equalsIgnoreCase("a") ? requestData.getListAGePiIds() : requestData.getListBGePiIds()).orElse(CompletableFuture.completedFuture(IdConversionResult.of())).get();
    }
}
