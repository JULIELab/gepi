package de.julielab.gepi.webapp.pages;

import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.InjectPage;
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
@Import(stylesheet = { "context:css-pages/index.css" })
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
	private EventRetrievalResult result;
	
	@Property
	private Event eventItem;

	public Zone getOutputZone() {
		return outputZone;
	}

	public Zone getInputZone() {
		return inputZone;
	}

	// Handle call with an unwanted context
	Object onActivate(EventContext eventContext) {
		return eventContext.getCount() > 0 ? new HttpError(404, "Resource not found") : null;
	}

	void afterRender() {
		
	}
	
}
