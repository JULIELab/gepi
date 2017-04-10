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

	private JSONArray eventsJSON, tmp;

	void setupRender() {
		super.setupRender();
	}
		
	void afterRender() {
		javaScriptSupport.require("gepi/gcharts/piechart").with(this.getGePiData());
    }
	
	private JSONArray getGePiData() {
		eventsJSON = new JSONArray();

		tmp = new JSONArray();
		tmp.put("Sleep"); tmp.put(7);
		eventsJSON.put( tmp );
		
		tmp = new JSONArray();
		tmp.put("Work"); tmp.put(11);		
		eventsJSON.put( tmp );
		
		return eventsJSON;
	}
	
}
