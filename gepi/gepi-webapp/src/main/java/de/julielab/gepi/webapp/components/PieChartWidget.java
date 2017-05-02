package de.julielab.gepi.webapp.components;

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
		
	
	void onDrawChart() {
		javaScriptSupport.require("gepi/gcharts/piechart").with( super.getSingleArgsCount() );
	}
	
	
}
