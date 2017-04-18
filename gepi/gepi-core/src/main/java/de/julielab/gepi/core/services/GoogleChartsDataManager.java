package de.julielab.gepi.core.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.tapestry5.json.JSONArray;

import de.julielab.gepi.core.retrieval.data.Event;

public class GoogleChartsDataManager implements IGoogleChartsDataManager {
	
	private Map<String, Integer> singleArgCount;
	private Map<String, Integer> pairedArgCount;	
	private JSONArray eventsJSON;
	

	@Override
	public void setSingleArgCount(List<Event> evtList) {
		List<String> atids = new ArrayList<String>();
			
			// get all atids in one list
			evtList.forEach(e -> 
				atids.addAll(e.getTopHomologyArgs()) );
		
			// get the counts of how often event arguments appear
			singleArgCount = CollectionUtils.getCardinalityMap(atids);
			
			// sort entries
			singleArgCount = singleArgCount.entrySet()
					.stream()
					.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
							(e1, e2) -> e1,
							LinkedHashMap::new));
			
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
	public void setBothArgsCount(List<Event> evtList) {
		Hashtable<String, String> atids = new Hashtable<String, String>(); 
		
		// get all atid atid pairs in one list
		evtList.forEach(e -> {
			if (e.getTopHomologyArgs().size() == 1)
				atids.put(e.getTopHomologyArgs().get(0), null);
			else {
				String a = e.getTopHomologyArgs().get(0);
				String b = e.getTopHomologyArgs().get(1);
				if (a.compareTo(b) <= 0)
					atids.put(a, b);
				else
					atids.put(b, a);
			}
		});
		
		System.out.println(atids);
		
		// get the count for how often pairs appear
		
		// sort the entries
		
		// put to json
	}

	@Override
	public JSONArray getBothArgsCount() {
		// TODO Auto-generated method stub
		return null;
	}

}
