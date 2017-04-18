package de.julielab.gepi.webapp.pages;

import java.util.concurrent.CompletableFuture;

import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.ActivationRequestParameter;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.HttpError;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.slf4j.Logger;

import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;

/**
 * Start page of application gepi-webapp.
 */
@Import(stylesheet = { "context:css-pages/index.less" })
public class Index {
	@Inject
	private Logger logger;

	@Environmental
	private JavaScriptSupport javaScriptSupport;

	@Property
	@Inject
	@Symbol(SymbolConstants.TAPESTRY_VERSION)
	private String tapestryVersion;

	@InjectPage
	private About about;

	@InjectComponent
	private Zone outputZone;

	@InjectComponent
	private Zone inputZone;

	@Property
	@Persist
	private CompletableFuture<EventRetrievalResult> result;

	@Property
	private Event eventItem;
	
	/**
	 * This is an emergency exit against being locked in an error during development.
	 */
	@ActivationRequestParameter
	private boolean reset;
	
	public Zone getOutputZone() {
		return outputZone;
	}

	public Zone getInputZone() {
		return inputZone;
	}

	// Handle call with an unwanted context
	Object onActivate(EventContext eventContext) {
		if (reset)
			result = null;
		return eventContext.getCount() > 0 ? new HttpError(404, "Resource not found") : null;
	}
	
	void afterRender() {
		javaScriptSupport.require("gepi/pages/index").invoke("loadGoogleCharts");
	}
	
	/**
	 * 
	 * @return The class "in", causing the outputcol to show immediately, or the empty string which will hide the outputcol initially.
	 */
	public String getShowOutputClass() {
		if (result != null && result.isDone())
			return "in";
		return "";
	}
	
	public String getShowInputClass() {
		if (result == null)
			return "in";
		return "";
	}
	
	public Object onReset() {
		result = null;
		return this;
	}

}
