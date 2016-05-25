package de.julielab.pages;

import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.corelib.components.TextArea;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.HttpError;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.slf4j.Logger;

/**
 * Start page of application gepi-webapp.
 */
public class Index {
	@Inject
	private Logger logger;

	@Inject
	private AjaxResponseRenderer ajaxResponseRenderer;

	@Property
	@Inject
	@Symbol(SymbolConstants.TAPESTRY_VERSION)
	private String tapestryVersion;

	@InjectPage
	private About about;

	@InjectComponent
	private Zone resultZone;

	@Inject
	private Request request;

	@Environmental
	private JavaScriptSupport javaScriptSupport;

	@InjectComponent
	private Form inputForm;
	
	@InjectComponent
	private TextArea lista;
	
	@Property
	@Persist
	private int counter;

	@Property
	private String listATextAreaValue;
	
	@Property
	private String listBTextAreaValue;

	// Handle call with an unwanted context
	Object onActivate(EventContext eventContext) {
		return eventContext.getCount() > 0 ? new HttpError(404, "Resource not found") : null;
	}

	Object onActionFromLearnMore() {
		about.setLearn("LearnMore");

		return about;
	}

	void onValidateFromInputForm() {

		// Note, this method is triggered even if server-side validation has
		// already found error(s).

		if (listATextAreaValue == null || listATextAreaValue.isEmpty())
		 inputForm.recordError(lista, "List A must not be empty.");

	}

	void onSuccess() {
		++counter;
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(resultZone);
		}
	}

	void onFailure() {
		--counter;
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(resultZone);
		}
	}

	void afterRender() {
		javaScriptSupport.require("gepi/pages/index").invoke("initialize");
	}

}
