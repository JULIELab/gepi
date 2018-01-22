package de.julielab.gepi.core.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tapestry5.json.JSONArray;
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
			if (i++ > 10)
				it.remove();
		}

		// put to json
		pairedArgCountJson = new JSONArray();
		this.pairedArgCount.forEach((k, v) -> {
			JSONArray tmp = new JSONArray();
			tmp.put(k.getLeft().getPreferredName());
			tmp.put(k.getRight().getPreferredName());
			tmp.put(v);
			pairedArgCountJson.put(tmp);
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
	public JSONArray getPairesWithCommonTarget(List<Event> evtList) {
		List<Pair<Argument, Argument>> atids = new ArrayList<>();

		// get all atid atid pairs in one list
		evtList.forEach(e -> {
			if (e.getNumArguments() == 2) {
				atids.add(new ImmutablePair<Argument, Argument>(e.getFirstArgument(), e.getSecondArgument()));
			}
		});

		Map<Argument, Set<Argument>> sourcesByTargets = atids.stream().collect(
				Collectors.groupingBy(p -> p.getRight(), Collectors.mapping(p -> p.getLeft(), Collectors.toSet())));
		// sort the entries
		LinkedHashMap<Argument, Set<Argument>> sortedSourcesByTargets = sourcesByTargets.entrySet().stream()
				.sorted((e1, e2) -> e2.getValue().size() -e1.getValue().size())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		int maxNumSources = 15;
		int maxNumTargets = 15;

		Set<Argument> includedSources = new HashSet<>();
		int numTargets = 0;
		Map<Argument, Set<Argument>> limitedSortedSourcesByTargets = new HashMap<>();
		for (Iterator<Entry<Argument, Set<Argument>>> it = sortedSourcesByTargets.entrySet().iterator(); it
				.hasNext();) {
			Entry<Argument, Set<Argument>> entry = it.next();
			++numTargets;
			includedSources.addAll(entry.getValue());
			limitedSortedSourcesByTargets.put(entry.getKey(), entry.getValue());
			if (includedSources.size() >= maxNumSources || numTargets >= maxNumTargets)
				break;
		}

		// get the count for how often pairs appear
		pairedArgCount = CollectionUtils.getCardinalityMap(atids);

		// sort the entries
		pairedArgCount = pairedArgCount.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		for (Iterator<Entry<Pair<Argument, Argument>, Integer>> it = pairedArgCount.entrySet().iterator(); it
				.hasNext();) {
			Entry<Pair<Argument, Argument>, Integer> entry = it.next();
			Argument source = entry.getKey().getLeft();
			Argument target = entry.getKey().getRight();
			Set<Argument> sources = limitedSortedSourcesByTargets.get(target);
			if (sources == null || !sources.contains(source))
				it.remove();

		}

		// put to json
		pairedArgCountJson = new JSONArray();

		this.pairedArgCount.forEach((k, v) -> {
			JSONArray tmp = new JSONArray();
			tmp.put(k.getLeft().getPreferredName());
			tmp.put(k.getRight().getPreferredName());
			tmp.put(v);
			pairedArgCountJson.put(tmp);
		});

		return pairedArgCountJson;
	}

}
