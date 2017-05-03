package de.julielab.gepi.core.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.tapestry5.json.JSONArray;

import de.julielab.gepi.core.retrieval.data.Event;

public class GoogleChartsDataManager implements IGoogleChartsDataManager {

	private Map<String, Integer> singleArgCount;
	private Map<String, Integer> pairedArgCount;
	private JSONArray singleArgCountJson;
	private JSONArray pairedArgCountJson;
	private static final String GENE_PAIR_DELIMITER = "__$__";

	/**
	 * sets json formated input list for google charts that accept one entry
	 * name + number (here target event gene + count of occurrences)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setSingleArgCount(List<Event> evtList) {
		List<String> atids = new ArrayList<String>();
		// get all atids in one list
		evtList.forEach(e -> e.getArguments().forEach(a -> atids.add(a.getTopHomologyId())));

		// get the counts of how often event arguments appear
		singleArgCount = CollectionUtils.getCardinalityMap(atids);

		// sort entries
		singleArgCount = singleArgCount.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		// put to json format
		singleArgCountJson = new JSONArray();

		this.singleArgCount.forEach((k, v) -> {
			JSONArray tmp = new JSONArray();
			tmp.put(k);
			tmp.put(v);
			singleArgCountJson.put(tmp);
		});
	}

	/**
	 * singleArgCountJson is array of arrays with [<gene name><count>]
	 */
	@Override
	public JSONArray getSingleArgCount() {
		return singleArgCountJson;
	}

	/**
	 * sets json formated input list for google charts that accepts an entry
	 * pair + number (here gene pair (from + to) + count of occurrences)
	 */
	// <gene 1>,<gene 2>, <count>
	@SuppressWarnings("unchecked")
	@Override
	public void setBothArgsCount(List<Event> evtList) {

		List<String> atids = new ArrayList<String>();

		// get all atid atid pairs in one list
		evtList.forEach(e -> {
			System.out.println(e);
			if (e.getNumArguments() == 2) {
				String a = e.getArgument(0).getTopHomologyId();
				String b = e.getArgument(1).getTopHomologyId();
				if (Integer.parseInt(a.substring(4)) < Integer.parseInt(b.substring(4)))
					atids.add(a + GENE_PAIR_DELIMITER + b);
				else
					atids.add(b + GENE_PAIR_DELIMITER + a);
			}
		});

		// get the count for how often pairs appear
		pairedArgCount = CollectionUtils.getCardinalityMap(atids);

		// sort the entries
		pairedArgCount = pairedArgCount.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		System.out.println("counts: " + pairedArgCount);

		// put to json
		pairedArgCountJson = new JSONArray();

		this.pairedArgCount.forEach((k, v) -> {
			String[] tmpPair;
			tmpPair = k.split(Pattern.quote(GENE_PAIR_DELIMITER));
			JSONArray tmp = new JSONArray();
			tmp.put(tmpPair[0]);
			tmp.put(tmpPair[1]);
			tmp.put(v);
			pairedArgCountJson.put(tmp);
		});
	}

	/**
	 * singleArgCountJson is array of arrays with [<gene name 1><gene name
	 * 2><count>]
	 */
	@Override
	public JSONArray getBothArgsCount() {
		System.out.println("json: " + this.pairedArgCountJson);
		return pairedArgCountJson;
	}

}
