package de.julielab.gepi.webapp.components;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONArray;

import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.services.IGoogleChartsDataManager;

public class GepiWidget {

	@Parameter(defaultPrefix = BindingConstants.LITERAL, name = "class")
	@Property
	private String classes;

	@Inject
	private IGoogleChartsDataManager gChartMnger;
	
	@Parameter
	@Property
	private CompletableFuture<EventRetrievalResult> result;
	
	@Property
	@Persist
	protected CompletableFuture<EventRetrievalResult> persistResult;

	void setupRender() {
		persistResult = result;
		if (persistResult != null && persistResult.isDone()) {
			// TODO: input event information handling
			
		}
	}
	
	/**
	 * Builds JSONArray that google charts requires for a pie and bar chart.
	 * @return JSONArray - array of tuples (array)
	 */
	protected JSONArray getSingleArgsCount() {
		try {
			gChartMnger.setSingleArgCount(persistResult.get().getEventList());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return gChartMnger.getSingleArgCount();
	}
	
	protected JSONArray getBothArgsCount() {
		try {
			gChartMnger.setBothArgsCount(persistResult.get().getEventList());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return gChartMnger.getBothArgsCount();
	}
	
	
}
