package de.julielab.gepi.webapp.components;

import java.util.stream.Stream;

import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Log;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.corelib.components.TextArea;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.services.IEventRetrievalService;
import de.julielab.gepi.webapp.pages.Index;

public class GepiInput {

	@Inject
	private AjaxResponseRenderer ajaxResponseRenderer;

	@Inject
	private Request request;

	@Environmental
	private JavaScriptSupport javaScriptSupport;

	@InjectComponent
	private Form inputForm;

	@InjectComponent
	private TextArea lista;

	@InjectComponent
	private TextArea listb;

	@Property
	private String listATextAreaValue;

	@Property
	private String listBTextAreaValue;

	@Inject
	private ComponentResources resources;

	@Inject
	private IEventRetrievalService eventRetrievalService;

	@Parameter
	private EventRetrievalResult result;
	
	void setupRender() {
		listATextAreaValue = "5327";
	}

	void onValidateFromInputForm() {
		// Note, this method is triggered even if server-side validation has
		// already found error(s).

		if (listATextAreaValue == null || listATextAreaValue.isEmpty()) {
			inputForm.recordError(lista, "List A must not be empty.");
			return;
		}
	}

	void onSuccessFromInputForm() {
		result = eventRetrievalService
				.getOutsideEvents(Stream.of(listATextAreaValue.split("\n")));

		Index indexPage = (Index) resources.getContainer();
		ajaxResponseRenderer.addRender(indexPage.getInputZone());
	}

	void onFailure() {
		if (request.isXHR()) {
			Index indexPage = (Index) resources.getContainer();
			ajaxResponseRenderer.addRender(indexPage.getInputZone());
		}
	}

	@Log
	void afterRender() {
		javaScriptSupport.require("gepi/components/gepiinput").invoke("initialize");
		if (result != null)
			javaScriptSupport.require("gepi/components/gepiinput").invoke("showOutput");
	}

}
