package de.julielab.gepi.core.services;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Argument.ComparisonMode;
import de.julielab.gepi.core.retrieval.data.Event;

public class ChartsDataManager implements IChartsDataManager {

    private static final Logger log = LoggerFactory.getLogger(ChartsDataManager.class);

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
        Map<Argument, Integer> singleArgCount = CollectionUtils.getCardinalityMap(arguments);

        // sort entries
        singleArgCount = singleArgCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // put to json format
        JSONArray singleArgCountJson = new JSONArray();

        singleArgCount.forEach((k, v) -> {
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
    public JSONObject getPairedArgsCount(List<Event> evtList) {

        log.trace("Number of events for pair counting: {}", evtList.size());


        // get the count for how often pairs appear
        Map<Event, Integer> pairedArgCount = CollectionUtils.getCardinalityMap(evtList);

        // put to json
        JSONArray nodes = new JSONArray();
        JSONArray links = new JSONArray();
        Set<String> nodeIdAlreadySeen = new HashSet<>();
        pairedArgCount.entrySet().stream().limit(10).forEach(e -> {
            final Event k = e.getKey();
            final Integer v = e.getValue();
            JSONObject link = new JSONObject();
            link.put("source", k.getFirstArgument().getTopHomologyId());
            link.put("target", k.getSecondArgument().getTopHomologyId());
            link.put("frequency", v);
            link.put("type", k.getMainEventType());

            links.put(link);

            if (nodeIdAlreadySeen.add(k.getFirstArgument().getTopHomologyId())) {
                nodes.put(getJsonObjectForArgument(k.getFirstArgument()));
            }
            if (nodeIdAlreadySeen.add(k.getSecondArgument().getTopHomologyId())) {
                nodes.put(getJsonObjectForArgument(k.getSecondArgument()));
            }
        });

        JSONObject nodesNLinks = new JSONObject();
        nodesNLinks.put("nodes", nodes);
        nodesNLinks.put("links", links);
        return nodesNLinks;
    }

    /**
     * sets json formated input list for google charts that accepts an entry
     * pair + number (here gene pair (from + to) + count of occurrences)
     * singleArgCountJson is array of arrays with [<gene name 1><gene name
     * 2><count>]
     */
    @SuppressWarnings("unchecked")
    @Override
    public JSONObject getPairsWithCommonTarget(List<Event> evtList) {
        // First, we build a structure that counts how often each target gene (genes on the - possibly implicit - B list)
        // interacts how often with which source (A list) gene. We build a map whose keys are the targets and the values
        // are maps counting for each source gene the number of interactions with the target.
        final Map<Argument, Map<Event, Integer>> target2EventCardinalities = evtList.stream().collect(Collectors.groupingBy(e -> e.getArgument(1),
                Collectors.collectingAndThen(Collectors.toList(), CollectionUtils::getCardinalityMap)));
        System.out.println(target2EventCardinalities);
        // The general harmonic mean formula for x1,...,xn: n / ( 1/x1 + ... + 1/xn)
        Function<Map<Event,Integer>, Double> harmonicMean = eventCounts -> eventCounts.size() / (eventCounts.values().stream().map(c -> 1d/c).reduce(1d, (sum, c) -> sum + c));
        final LinkedHashMap<Argument, Map<Event, Integer>> orderedMap = target2EventCardinalities.entrySet().stream().sorted((e1,e2) -> (int)Math.signum(harmonicMean.apply(e2.getValue()) - harmonicMean.apply(e1.getValue()))).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (k, v) -> k, LinkedHashMap::new));

        // put to json
        JSONArray nodes = new JSONArray();
        JSONArray links = new JSONArray();
        Set<String> nodeIdAlreadySeen = new HashSet<>();
        orderedMap.values().stream().forEachOrdered(map -> map.forEach((k, v) -> {
            JSONObject link = new JSONObject();
            link.put("source", k.getFirstArgument().getTopHomologyId());
            link.put("target", k.getSecondArgument().getTopHomologyId());
            link.put("frequency", v);
            link.put("type", k.getMainEventType());

            links.put(link);

            if (nodeIdAlreadySeen.add(k.getFirstArgument().getTopHomologyId())) {
                nodes.put(getJsonObjectForArgument(k.getFirstArgument()));
            }
            if (nodeIdAlreadySeen.add(k.getSecondArgument().getTopHomologyId())) {
                nodes.put(getJsonObjectForArgument(k.getSecondArgument()));
            }
        }));

        JSONObject nodesNLinks = new JSONObject();
        nodesNLinks.put("nodes", nodes);
        nodesNLinks.put("links", links);
        return nodesNLinks;
    }

    @Override
    public JSONArray convertToJson(List<Event> eventList) {
        JSONArray array = new JSONArray();
        for (Event event : eventList) {
            final Argument firstArgument = event.getFirstArgument();
            JSONObject source = getJsonObjectForArgument(firstArgument);

            final Argument secondArgument = event.getSecondArgument();
            JSONObject target = getJsonObjectForArgument(secondArgument);


            JSONObject o = new JSONObject();
            o.put("source", source);
            o.put("target", target);
            o.put("type", event.getMainEventType());

            array.put(o);
        }
        return array;
    }

    private JSONObject getJsonObjectForArgument(Argument argument) {
        JSONObject source = new JSONObject();
        source.put("id", argument.getTopHomologyId());
        source.put("name", argument.getPreferredName());
        return source;
    }

}
