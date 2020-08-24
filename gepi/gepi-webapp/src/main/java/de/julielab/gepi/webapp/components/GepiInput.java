package de.julielab.gepi.webapp.components;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;
import de.julielab.gepi.core.retrieval.services.IAggregatedEventsRetrievalService;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.SelectModel;
import org.apache.tapestry5.ValueEncoder;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.corelib.components.TextArea;
import org.apache.tapestry5.internal.OptionModelImpl;
import org.apache.tapestry5.internal.SelectModelImpl;
import org.apache.tapestry5.internal.services.StringValueEncoder;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
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
    private IAggregatedEventsRetrievalService aggregatedEventsRetrievalService;

    @Inject
    private IGeneIdService geneIdService;

    @Parameter
    private CompletableFuture<EventRetrievalResult> esResult;

    @Property
    @Persist
    private CompletableFuture<EventRetrievalResult> persistEsResult;

    @Parameter
    private CompletableFuture<AggregatedEventsRetrievalResult> neo4jResult;

    @Property
    @Persist
    private CompletableFuture<AggregatedEventsRetrievalResult> persistNeo4jResult;

    @Inject
    private TypeCoercer typeCoercer;

    @Inject
    private Messages messages;

    @Property
    private List<EventTypes> selectedEventTypes = new ArrayList<>(EnumSet.allOf(EventTypes.class));

    @Property
    private List<String> selectedDevSettings;

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

    public ValueEncoder getDevSettingsEncoder() {
        return new StringValueEncoder();
    }

    public SelectModel getDevSettingsModel() {
        return new SelectModelImpl(new OptionModelImpl("Always use ES"));
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
        log.debug("Setting newsearch to true");
        newSearch = true;
        List<String> selectedEventTypeNames = selectedEventTypes.stream().flatMap(e -> e == EventTypes.Regulation ? Stream.of(EventTypes.Positive_regulation, EventTypes.Negative_regulation) : Stream.of(e)).map(EventTypes::name).collect(Collectors.toList());
        if (selectedEventTypeNames.isEmpty())
            selectedEventTypeNames = EnumSet.allOf(EventTypes.class).stream().map(Enum::name).distinct().collect(Collectors.toList());
        boolean isAListPresent = listATextAreaValue != null && listATextAreaValue.trim().length() > 0;
        boolean isABSearchRequest = listATextAreaValue != null && listATextAreaValue.trim().length() > 0 && listBTextAreaValue != null
                && listBTextAreaValue.trim().length() > 0;
        System.out.println("dev settings: " + selectedDevSettings);
        Future<IdConversionResult> listAGePiIds = convertToAggregateIds(listATextAreaValue, "listA");
        Future<IdConversionResult> listBGePiIds = convertToAggregateIds(listATextAreaValue, "listB");
        if ((filterString != null && !filterString.isBlank()) || selectedDevSettings.contains("Always use ES")) {
            if (isABSearchRequest) {
                esResult = eventRetrievalService.getBipartiteEvents(
                        listAGePiIds,
                        listBGePiIds, selectedEventTypeNames, filterString);
            }
            else {
                if (isAListPresent) {
                    log.debug("Calling EventRetrievalService for outside events");
                    esResult = eventRetrievalService.getOutsideEvents(listAGePiIds, selectedEventTypeNames, filterString);
                    if (resultPresent())
                        log.debug("Retrieved the response future. It is " + (esResult.isDone() ? "" : "not ") + "(ES)" + (neo4jResult != null && neo4jResult.isDone() ? "" : "not ") + "(Neo4j) finished.");
                    else log.debug("After retrieving the result");
                }
            }
            persistEsResult = esResult;
        } else {
            CompletableFuture<Stream<String>> aListIds = CompletableFuture.completedFuture(Stream.of(listATextAreaValue.split("\n")));
            if (isABSearchRequest) {
                neo4jResult = aggregatedEventsRetrievalService.getEvents(aListIds, CompletableFuture.completedFuture(Stream.of(listBTextAreaValue.split("\n"))), selectedEventTypeNames);
            } else if (isAListPresent) {
                neo4jResult = aggregatedEventsRetrievalService.getEvents(aListIds, selectedEventTypeNames);
            }
            persistNeo4jResult = neo4jResult;
        }

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
        javaScriptSupport.require("gepi/components/gepiinput").invoke("initialize").with(resultPresent());
        if (resultPresent() && newSearch) {
            log.debug("Sending JS call to show the output widgets.");
            javaScriptSupport.require("gepi/components/gepiinput").invoke("showOutput");
        }
    }

    private Future<IdConversionResult> convertToAggregateIds(String input, String listName) {
        List<String> inputList = Stream.of(input.split("\n")).map(String::trim).collect(Collectors.toList());
        IGeneIdService.IdType idType = geneIdService.determineIdType(inputList.stream());
        log.debug("Identified input IDs of {} as: ", listName, idType);
        return geneIdService.convert(inputList.stream(), idType, IGeneIdService.IdType.GEPI_AGGREGATE);
    }

    private boolean resultPresent() {
        return esResult != null || neo4jResult != null;
    }

    private enum EventTypes {Regulation, Positive_regulation, Negative_regulation, Binding, Localization, Phosphorylation}

}
