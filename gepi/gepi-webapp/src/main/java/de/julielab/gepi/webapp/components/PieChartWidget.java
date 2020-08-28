package de.julielab.gepi.webapp.components;

import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.services.IGePiDataService;

public class PieChartWidget extends GepiWidget {
	
	@Property
	private Event eventRow;
	
	@Inject
    private JavaScriptSupport javaScriptSupport;
	
	@Inject
	private IGePiDataService gChartMnger;
		
	@Property
	private JSONArray eventsJSON;
	


	void onDrawChart() {

	}
	
	
}
