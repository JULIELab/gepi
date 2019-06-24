package de.julielab.gepi.webapp.components;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.julielab.gepi.core.services.IChartsDataManager;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.SelectModel;
import org.apache.tapestry5.ValueEncoder;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.corelib.components.TextArea;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.TypeCoercer;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.ajax.JavaScriptCallback;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.services.IEventRetrievalService;
import de.julielab.gepi.core.services.IGeneIdService;
import de.julielab.gepi.webapp.pages.Index;
import org.apache.tapestry5.util.EnumSelectModel;
import org.apache.tapestry5.util.EnumValueEncoder;
import org.slf4j.Logger;

@Import(stylesheet = {"context:css-components/gepiinput.css"})
public class GepiInput {

    @Inject
    private Logger log;

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
    @Persist
    private String listATextAreaValue;

    @Property
    @Persist
    private String listBTextAreaValue;

    @Inject
    private ComponentResources resources;

    @Inject
    private IEventRetrievalService eventRetrievalService;

    @Inject
    private IGeneIdService geneIdService;

    @Parameter
    private CompletableFuture<EventRetrievalResult> result;

    @Property
    @Persist
    private CompletableFuture<EventRetrievalResult> persistResult;

    @Inject
    private TypeCoercer typeCoercer;

    @Inject
    private Messages messages;

    @Property
    private List<EventTypes> selectedEventTypes;

    @Property
    private String filterString;

    @Persist(PersistenceConstants.FLASH)
    private boolean newSearch;


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
        newSearch = true;
       log.debug("Setting newsearch to true");
        final List<String> selectedEventTypeNames = selectedEventTypes.stream().flatMap(e -> e == EventTypes.Regulation ? Stream.of(EventTypes.Positive_regulation, EventTypes.Negative_regulation) : Stream.of(e)).map(EventTypes::name).collect(Collectors.toList());
        if (listATextAreaValue != null && listATextAreaValue.trim().length() > 0 && listBTextAreaValue != null
                && listBTextAreaValue.trim().length() > 0)
            result = eventRetrievalService.getBipartiteEvents(
                    geneIdService.convertInput2Atid(listATextAreaValue),
                    geneIdService.convertInput2Atid(listBTextAreaValue), selectedEventTypeNames, filterString);
        else if (listATextAreaValue != null && listATextAreaValue.trim().length() > 0) {
            log.debug("Calling EventRetrievalService for outside events");
            result = eventRetrievalService.getOutsideEvents(geneIdService.convertInput2Atid(listATextAreaValue), selectedEventTypeNames, filterString);
            if (result != null)
                log.debug("Retrieved the response future. It is " + (result.isDone() ? "" : "not ") + "finished.");
            else log.debug("After retrieving the result");
        }
        persistResult = result;

        Index indexPage = (Index) resources.getContainer();
        ajaxResponseRenderer.addRender(indexPage.getInputZone()).addRender(indexPage.getOutputZone());
        log.debug("Ajax rendering commands sent, entering the output display mode");
    }

    void onFailure() {
        if (request.isXHR()) {
            Index indexPage = (Index) resources.getContainer();
            ajaxResponseRenderer.addRender(indexPage.getInputZone());
        }
    }

    void afterRender() {
        javaScriptSupport.require("gepi/components/gepiinput").invoke("initialize").with(result != null);
        if (result != null && newSearch) {
            log.debug("Sending JS call to show the output widgets.");
            javaScriptSupport.require("gepi/components/gepiinput").invoke("showOutput");
        }
    }

    private enum EventTypes {Regulation, Positive_regulation, Negative_regulation, Binding, Localization, Phosphorylation}

}
