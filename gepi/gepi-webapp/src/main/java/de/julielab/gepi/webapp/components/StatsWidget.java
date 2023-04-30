package de.julielab.gepi.webapp.components;

import com.google.common.collect.Multimap;
import de.julielab.gepi.core.retrieval.data.EsAggregatedResult;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;
import de.julielab.gepi.core.retrieval.data.InputMode;
import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.gepi.webapp.data.InputMapping;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
            return getEsAggregatedResult().get().getEventFrequencies().stream().map(Pair::getLeft).filter(e -> e.getArity() == 2).count();
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

    public List<Triple<String, String, Integer>> getTopInteractions() {
        try {
            int n = 10;
            final Future<EsAggregatedResult> esAggregatedResult = getEsAggregatedResult();
            final List<Pair<Event, Integer>> eventFrequencies = esAggregatedResult.get().getEventFrequencies();
            final Iterator<Pair<Event, Integer>> keyIt = eventFrequencies.stream().iterator();
            int i = 0;
            List<Triple<String, String, Integer>> topInteractions = new ArrayList<>(n);
            while (keyIt.hasNext() && i < n) {
                Pair<Event, Integer> p = keyIt.next();
                final Triple<String, String, Integer> triple = new ImmutableTriple(p.getLeft().getFirstArgument().getTopHomologyPreferredName(), p.getLeft().getSecondArgument().getTopHomologyPreferredName(), p.getRight());
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
