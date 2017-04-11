package de.julielab.gepi.webapp.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.retrieval.data.Event;

public class PieChartWidget extends GepiWidget {
	
	@Property
	private Event eventRow;
	
	@Inject
    private JavaScriptSupport javaScriptSupport;
	
	@Property
	private JSONArray eventsJSON;
	

	void setupRender() {
		super.setupRender();
	}
		
	void afterRender() throws InterruptedException, ExecutionException {
		if (persistResult != null && persistResult.isDone())
			javaScriptSupport.require("gepi/gcharts/piechart").with(getPieData());
    }
	
	/**
	 * Builds JSONArray that google charts understands for a pie chart.
	 * @return JSONArray - array of tuples (array)
	 */
	private JSONArray getPieData() {
		eventsJSON = new JSONArray();
		
		Map<String, Integer> evtPartnerCount = aggregateIDoccurrences();
		
		evtPartnerCount.forEach( (k, v) -> {
			JSONArray tmp = new JSONArray();
			tmp.put(k); 
			tmp.put(v);
			eventsJSON.put(tmp);
		});
		
		return eventsJSON;	
	}
	
	/**
	 * Gathers all atids and provides count of each occurrence.
	 * @return Map<String, Integer> String: Gene top homology ID, Integer: count
	 */
	private Map<String, Integer> aggregateIDoccurrences() {
		Map<String, Integer> pieData = null;
		List<String> atids = new ArrayList<String>();
		
		try {
			// get all atids in one list
			persistResult.get().getEventList().forEach(e -> 
				atids.addAll(e.getFirstAtidArguments()) );
		
			// get the counts of elements for each element
			pieData =  CollectionUtils.getCardinalityMap(atids);

		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pieData;
	}
	
	/**
	 * demo function for testing purposes. remove once code is ready.
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private void printEvtToStringOut() throws InterruptedException, ExecutionException {
		Iterator<Event> it = persistResult.get().getEventList().iterator();
		while ( it.hasNext() ) {
			Event tmpEvt = it.next();
			System.out.println(tmpEvt.getAllTokensToString());
			System.out.println(tmpEvt.getAllArguments().toString() );
			System.out.println(tmpEvt.getFirstAtidArguments().toString() );
		}
		
	}
	
}
