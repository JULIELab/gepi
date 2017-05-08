package de.julielab.gepi.core.services;

import java.util.List;
import java.util.Map;

import org.apache.tapestry5.json.JSONArray;

import de.julielab.gepi.core.retrieval.data.Event;

public interface IGoogleChartsDataManager {
	/**
	 * input structure for pie chart and bar chart
	 * @return JSONArray - json array of tuples (itself realised as an json array)
	 */
	JSONArray getTargetArgCount(List<Event> e);

	/**
	 * input structure required for sankey graph
	 * @return JSONArray - array of triplets ([<from, <to>, count])
	 */	
	JSONArray getPairedArgsCount(List<Event> e);

	JSONArray getPairesWithCommonTarget(List<Event> evtList);
}
