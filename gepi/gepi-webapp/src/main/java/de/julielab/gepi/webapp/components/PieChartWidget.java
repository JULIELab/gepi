package de.julielab.gepi.webapp.components;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;

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
			System.out.println(tmpEvt.getTopHomologyArgs().toString() );
		}
		
	}
	
}
