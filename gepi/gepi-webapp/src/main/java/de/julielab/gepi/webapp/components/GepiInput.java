package de.julielab.gepi.webapp.components;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.SelectModel;
import org.apache.tapestry5.ValueEncoder;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.corelib.components.TextArea;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.InjectService;
import org.apache.tapestry5.ioc.services.TypeCoercer;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.services.IEventRetrievalService;
import de.julielab.gepi.core.services.IGeneIdService;
import de.julielab.gepi.webapp.pages.Index;
import org.apache.tapestry5.util.EnumSelectModel;
import org.apache.tapestry5.util.EnumValueEncoder;

@Import(stylesheet = { "context:css-components/gepiinput.css" })
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

	@Inject
	private IGeneIdService geneIdService;

	@Parameter
	private CompletableFuture<EventRetrievalResult> result;

	@Inject
	private TypeCoercer typeCoercer;

	@Inject
	private Messages messages;

	@Property
	private List<EventTypes> selectedEventTypes;

	@Property
    private String filterString;

	private enum EventTypes {Regulation, Positive_regulation, Negative_regulation, Binding, Localization, Phosphorylation}

	public ValueEncoder getEventTypeEncoder() {
		return new EnumValueEncoder(typeCoercer, EventTypes.class);
	}

	public SelectModel getEventTypeModel() {
		return new EnumSelectModel(EventTypes.class, messages);
	}

	void setupRender() {
		//listATextAreaValue = "2475\n196";
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
        final List<String> selectedEventTypeNames = selectedEventTypes.stream().flatMap(e -> e == EventTypes.Regulation ? Stream.of(EventTypes.Positive_regulation, EventTypes.Negative_regulation) : Stream.of(e)).map(EventTypes::name).collect(Collectors.toList());
		if (listATextAreaValue != null && listATextAreaValue.trim().length() > 0 && listBTextAreaValue != null
				&& listBTextAreaValue.trim().length() > 0)
			result = eventRetrievalService.getBipartiteEvents(
					Stream.of(geneIdService.convertInput2Atid(listATextAreaValue)),
					Stream.of(geneIdService.convertInput2Atid(listBTextAreaValue)), selectedEventTypeNames, filterString);
		else if (listATextAreaValue != null && listATextAreaValue.trim().length() > 0)
			result = eventRetrievalService.getOutsideEvents(Stream.of(geneIdService.convertInput2Atid(listATextAreaValue)), selectedEventTypeNames, filterString);

		Index indexPage = (Index) resources.getContainer();
		ajaxResponseRenderer.addRender(indexPage.getInputZone()).addRender(indexPage.getOutputZone());
	}

	void onFailure() {
		if (request.isXHR()) {
			Index indexPage = (Index) resources.getContainer();
			ajaxResponseRenderer.addRender(indexPage.getInputZone());
		}
	}

	void afterRender() {
		javaScriptSupport.require("gepi/components/gepiinput").invoke("initialize");
		javaScriptSupport.require("gepi/base").invoke("setuptooltips");
		// The following JavaScript call always causes the inputcol to disappear
		// behind the left border of the viewport. This also happens when the
		// page is reloaded with a non-null result. But then, the index page is
		// hiding the inputcol by default, thus noone sees the shift.
		// Also, the outputcol is shown immediately by means of the index page
		// if the result already exists and is finished loading.
		if (result != null)
			javaScriptSupport.require("gepi/components/gepiinput").invoke("showOutput");
	}

}
