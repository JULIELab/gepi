package de.julielab.gepi.core.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tapestry5.json.JSONArray;

import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Argument.ComparisonMode;

public class GoogleChartsDataManager implements IGoogleChartsDataManager {

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
			};
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
