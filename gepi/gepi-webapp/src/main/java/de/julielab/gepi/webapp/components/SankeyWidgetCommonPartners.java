package de.julielab.gepi.webapp.components;

import java.util.concurrent.ExecutionException;

import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.services.IChartsDataManager;

public class SankeyWidgetCommonPartners extends GepiWidget {
	
	@Inject
    private JavaScriptSupport javaScriptSupport;
	
	@Inject
	private IChartsDataManager gChartMnger;
	
	@Property
	private JSONArray eventsJSON;
	
	void setupRender() {
		super.setupRender();
	}
	
	void onDrawChart() {
		try {
			javaScriptSupport.require("gepi/charts/sankeychart").with( "sankeychartcommonpartners",
					gChartMnger.getPairsWithCommonTarget(persistEsResult.get().getEventList()) );
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
		
}
