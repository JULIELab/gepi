package de.julielab.gepi.webapp.components;

import de.julielab.gepi.core.services.IGoogleChartsDataManager;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import java.util.concurrent.ExecutionException;

public class CircleWidget extends GepiWidget {
	
	@Inject
    private JavaScriptSupport javaScriptSupport;
	
	@Inject
	private IGoogleChartsDataManager gChartMnger;
	
	@Property
	private JSONArray eventsJSON;

	@Parameter(defaultPrefix = BindingConstants.LITERAL)
	@Property
	private String elementId;


	void setupRender() {
		super.setupRender();
	}
	
	void onDrawChart() {
		try {
			javaScriptSupport.require("gepi/gcharts/circlechart").with(elementId,
					gChartMnger.getPairedArgsCount(persistResult.get().getEventList()));
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
		
}
