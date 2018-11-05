package de.julielab.gepi.core.services;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Argument.ComparisonMode;
import de.julielab.gepi.core.retrieval.data.Event;

public class GoogleChartsDataManager implements IGoogleChartsDataManager {

    private static final Logger log = LoggerFactory.getLogger(GoogleChartsDataManager.class);
    private Map<Argument, Integer> singleArgCount;
    private Map<Pair<Argument, Argument>, Integer> pairedArgCount;
    private JSONArray singleArgCountJson;
    private JSONArray pairedArgCountJson;

    /**
     * sets json formated input list for google charts that accept one entry
     * name + number (here target event gene + count of occurrences)
     * singleArgCountJson is array of arrays with [<gene name><count>]
     */
    @SuppressWarnings("unchecked")
    @Override
    public JSONArray getTargetArgCount(List<Event> evtList) {
        List<Argument> arguments = new ArrayList<>();
        // get those arguments that were not part of the input
        evtList.forEach(e -> {
            for (int i = 1; i < e.getNumArguments(); ++i) {
                Argument a = e.getArgument(i);
                a.setComparisonMode(ComparisonMode.TOP_HOMOLOGY);
                arguments.add(a);
            }
        });

        // get the counts of how often event arguments appear
        singleArgCount = CollectionUtils.getCardinalityMap(arguments);

        // sort entries
        singleArgCount = singleArgCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // put to json format
        singleArgCountJson = new JSONArray();

        this.singleArgCount.forEach((k, v) -> {
            JSONArray tmp = new JSONArray();
            tmp.put(k.getPreferredName());
            tmp.put(v);
            singleArgCountJson.put(tmp);
        });
        return singleArgCountJson;
    }

    /**
     * sets json formated input list for google charts that accepts an entry
     * pair + number (here gene pair (from + to) + count of occurrences)
     * singleArgCountJson is array of arrays with [<gene name 1><gene name
     * 2><count>]
     */
    @SuppressWarnings("unchecked")
    @Override
    public JSONArray getPairedArgsCount(List<Event> evtList) {
        List<Pair<Argument, Argument>> atids = new ArrayList<>();

        log.trace("Number of events for pair counting: {}", evtList.size());

        // get all atid atid pairs in one list
        evtList.forEach(e -> {
            if (e.getNumArguments() == 2) {
                atids.add(new ImmutablePair<Argument, Argument>(e.getFirstArgument(), e.getSecondArgument()));
            }
        });

        // get the count for how often pairs appear
        pairedArgCount = CollectionUtils.getCardinalityMap(atids);

        // sort the entries
        pairedArgCount = pairedArgCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        int i = 0;
        for (Iterator<Entry<Pair<Argument, Argument>, Integer>> it = pairedArgCount.entrySet().iterator(); it
                .hasNext();) {
            @SuppressWarnings("unused")
            Entry<Pair<Argument, Argument>, Integer> entry = it.next();
            //if (i++ > 10)
            //	it.remove();
        }

        // put to json
        pairedArgCountJson = new JSONArray();
        this.pairedArgCount.forEach((k, v) -> {
            JSONObject o = new JSONObject();
            o.put("source", k.getLeft().getPreferredName());
            o.put("target", k.getRight().getPreferredName());
            o.put("weight", v);
            o.put("color", "grey");

            pairedArgCountJson.put(o);
        });

        return pairedArgCountJson;
    }

    /**
     * sets json formated input list for google charts that accepts an entry
     * pair + number (here gene pair (from + to) + count of occurrences)
     * singleArgCountJson is array of arrays with [<gene name 1><gene name
     * 2><count>]
     */
    @SuppressWarnings("unchecked")
    @Override
    public JSONArray getPairsWithCommonTarget(List<Event> evtList) {
        // First, we build a structure that counts how often each target gene (genes on the - possibly implicit - B list)
        // interacts how often with which source (A list) gene. We build a map whose keys are the targets and the values
        // are maps counting for each source gene the number of interactions with the target.
        final Map<Argument, Map<Event, Integer>> target2EventCardinalities = evtList.stream().collect(Collectors.groupingBy(e -> e.getArgument(1),
                Collectors.collectingAndThen(Collectors.toList(), CollectionUtils::getCardinalityMap)));
        System.out.println(target2EventCardinalities);
        // The general harmonic mean formula for x1,...,xn: n / ( 1/x1 + ... + 1/xn)
        Function<Map<Event,Integer>, Double> harmonicMean = eventCounts -> eventCounts.size() / (eventCounts.values().stream().map(c -> 1d/c).reduce(1d, (sum, c) -> sum + c));
        final LinkedHashMap<Argument, Map<Event, Integer>> orderedMap = target2EventCardinalities.entrySet().stream().sorted((e1,e2) -> (int)Math.signum(harmonicMean.apply(e2.getValue()) - harmonicMean.apply(e1.getValue()))).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (k, v) -> k, LinkedHashMap::new));

        pairedArgCountJson = new JSONArray();
        // At the moment, we cannot send all the data to the client, for much data it would just break the Sankey Diagram.
        // In the map below we memorize how often we already did output an edge from a source gene to a target gene.
        // Then we set a limit on the number of source gene occurrences: We will only show n many target genes for
        // each source gene. This will possibly exclude weaker connections to other source genes, but we can't help it right now.
        Map<Argument, Integer> sourceOccurrenceCount = new HashMap<>();
        orderedMap.values().stream().forEachOrdered(map -> map.forEach((k, v) -> {

            JSONObject o = new JSONObject();
            o.put("source", k.getArgument(0).getPreferredName());
            o.put("target", k.getArgument(1).getPreferredName());
            o.put("weight", v);
            o.put("color", "grey");
            //o.put("type", k.getMainEventType());

            pairedArgCountJson.put(o);
        }));

        // TODO assemble List of relevant nodes


        return pairedArgCountJson;
    }

}
