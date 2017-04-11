package de.julielab.gepi.core.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.tapestry5.json.JSONArray;

import de.julielab.gepi.core.retrieval.data.Event;

public class GoogleChartsDataManager implements IGoogleChartsDataManager {
	
	private Map<String, Integer> singleArgCount;
	private JSONArray eventsJSON;
	
//	public GoogleChartsDataManager() {
//		this.singleArgCount = null;
//		this.eventsJSON = null;
//	}
	
	@Override
	public void setSingleArgCount(List<Event> evtList) {
		List<String> atids = new ArrayList<String>();
		
			// get all atids in one list
			evtList.forEach(e -> 
				atids.addAll(e.getFirstAtidArguments()) );
		
			// get the counts of elements for each element
			singleArgCount = CollectionUtils.getCardinalityMap(atids);
			
			// put to json format
			eventsJSON = new JSONArray();
			
			this.singleArgCount.forEach( (k, v) -> {
				JSONArray tmp = new JSONArray();
				tmp.put(k); 
				tmp.put(v);
				eventsJSON.put(tmp);
			});
	}

	@Override
	public JSONArray getSingleArgCount() {		
		return eventsJSON;	
	}

	@Override
	public void setBothArgsCount() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public JSONArray getBothArgsCount() {
		// TODO Auto-generated method stub
		return null;
	}

}
