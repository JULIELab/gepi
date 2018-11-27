package de.julielab.gepi.core.services;

import java.util.List;

import org.apache.tapestry5.json.JSONArray;

import de.julielab.gepi.core.retrieval.data.Event;
import org.apache.tapestry5.json.JSONObject;

public interface IChartsDataManager {
	/**
	 * input structure for pie chart and bar chart
	 * @return JSONArray - json array of tuples (itself realised as an json array)
	 */
	JSONArray getTargetArgCount(List<Event> e);

	/**
	 * input structure required for sankey graph
	 * @return JSONArray - array of triplets ([<from, <to>, count])
	 */	
	JSONObject getPairedArgsCount(List<Event> e);

    JSONObject getPairsWithCommonTarget(List<Event> evtList);

    JSONArray convertToJson(List<Event> eventList);
}
