package de.julielab.gepi.webapp.components;

import java.util.concurrent.ExecutionException;

import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.services.IGePiDataService;

public class BarChartWidget extends GepiWidget {
	
	@Inject
    private JavaScriptSupport javaScriptSupport;

	@Inject
	private IGePiDataService gChartMnger;
	
	@Property
	private JSONArray eventsJSON;
	

	void onDrawChart() {
//		try {
//			javaScriptSupport.require("gepi/charts/barchart").with(
//					gChartMnger.getArgumentCount(getEsResult().get().getEventList(), ) );
//		} catch (InterruptedException | ExecutionException e) {
//			 TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	

}
