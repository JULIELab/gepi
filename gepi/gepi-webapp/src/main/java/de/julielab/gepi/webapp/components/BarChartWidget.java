package de.julielab.gepi.webapp.components;

import java.util.concurrent.ExecutionException;

import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.services.IGoogleChartsDataManager;

public class BarChartWidget extends GepiWidget {
	
	@Inject
    private JavaScriptSupport javaScriptSupport;

	@Inject
	private IGoogleChartsDataManager gChartMnger;
	
	@Property
	private JSONArray eventsJSON;
	
	void setupRender() {
		super.setupRender();
	}
	
	void afterRender() throws InterruptedException, ExecutionException {
		if (persistResult != null && persistResult.isDone())
			javaScriptSupport.require("gepi/gcharts/barchart").with( super.getSingleArgsCount() );
    }
	

}
