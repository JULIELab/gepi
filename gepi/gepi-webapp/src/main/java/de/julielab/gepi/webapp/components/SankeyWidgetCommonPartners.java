package de.julielab.gepi.webapp.components;

import java.util.concurrent.ExecutionException;

import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.services.IGePiDataService;

public class SankeyWidgetCommonPartners extends GepiWidget {
	
	@Inject
    private JavaScriptSupport javaScriptSupport;
	
	@Inject
	private IGePiDataService gChartMnger;
	
	@Property
	private JSONArray eventsJSON;
	

	void onDrawChart() {
		try {
			javaScriptSupport.require("gepi/charts/sankeychart").with( "sankeychartcommonpartners",
					gChartMnger.getPairsWithCommonTarget(getEsResult().get().getEventList()) );
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
		
}
