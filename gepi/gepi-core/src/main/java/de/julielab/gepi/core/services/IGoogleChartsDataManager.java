package de.julielab.gepi.core.services;

import java.util.List;
import java.util.Map;

import org.apache.tapestry5.json.JSONArray;

import de.julielab.gepi.core.retrieval.data.Event;

public interface IGoogleChartsDataManager {
	/**
	 * Counts all top homology atids over all events.
	 * Required for pie Chart and bar chart.
	 * @param e
	 */
//	void setSingleArgCount(List<Event> e);
	
	/**
	 * input structure for pie chart and bar chart
	 * @return JSONArray - json array of tuples (itself realised as an json array)
	 */
	JSONArray getTargetArgCount(List<Event> e);

	/**
	 * Counts over all events how many events have the same number of arguments.
	 * @param e
	 */
//	void setBothArgsCount(List<Event> e);
	
	/**
	 * input structure required for sankey graph
	 * @return JSONArray - array of triplets ([<from, <to>, count])
	 */	
	JSONArray getPairedArgsCount(List<Event> e);
}
