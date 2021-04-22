package de.julielab.gepi.webapp.components;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.julielab.gepi.core.retrieval.data.*;
import de.julielab.gepi.core.retrieval.services.IAggregatedEventsRetrievalService;
import de.julielab.gepi.core.services.IGePiDataService;
import org.apache.tapestry5.*;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.corelib.components.TextArea;
import org.apache.tapestry5.corelib.components.TextField;
import org.apache.tapestry5.internal.OptionModelImpl;
import org.apache.tapestry5.internal.SelectModelImpl;
import org.apache.tapestry5.internal.services.StringValueEncoder;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.TypeCoercer;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

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

    @InjectComponent
    private TextField dataSessionIdField;

    @InjectComponent
    private TextField sentenceFilter;

    @InjectComponent
    private TextField paragraphFilter;

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

    @Inject
    private IGePiDataService dataService;

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
    private String sentenceFilterString;

    @Property
    private String paragraphFilterString;

    @Property
    private String filterFieldsConnectionOperator;

    /**
     * This is not an ID for the servlet session but to the current data state.
     */
    @Parameter
    @Property
    private long dataSessionId;

    @Parameter
    private EnumSet<InputMode> inputMode;

    @Persist(PersistenceConstants.FLASH)
    private boolean newSearch;
    /**
     * Do not access this field. It is only here to store the data in the session. Data access should happen through
     * {@link de.julielab.gepi.core.services.GePiDataService}. There, the data is cached with weak keys and values.
     * The idea is that the data is evicted when the session ends.
     */
    @Persist
    private GePiData data;
    /**
     * This is an emergency exit against being locked in an error during development.
     */
    @ActivationRequestParameter
    private boolean reset;

    void onActivate(EventContext eventContext) {
        if (reset) {
            log.debug("Reset is active, setting data to null.");
            data = null;
        }
    }

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
        log.warn("{}", inputMode);
    }

    void onValidateFromInputForm() {
        // Note, this method is triggered even if server-side validation has
        // already found error(s).
        boolean noIdsGiven = listATextAreaValue == null || listATextAreaValue.isEmpty();
        boolean noSentenceFilterGiven = sentenceFilterString == null || sentenceFilterString.isBlank();
        boolean noParagraphFilterGiven = paragraphFilterString == null || paragraphFilterString.isBlank();
        if (noIdsGiven && noSentenceFilterGiven && noParagraphFilterGiven) {
            String msg = "Either lists of gene IDs or names must be given or a filter query to restrict the returned events.";
            inputForm.recordError(lista, msg);
            inputForm.recordError(sentenceFilter, msg + "1");
            inputForm.recordError(paragraphFilter, msg + "2");
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
        boolean isSentenceFilterPresent = sentenceFilterString != null && !sentenceFilterString.isBlank();
        boolean isParagraphFilterPresent = paragraphFilterString != null && !paragraphFilterString.isBlank();
        System.out.println("dev settings: " + selectedDevSettings);
        Future<IdConversionResult> listAGePiIds = convertToAggregateIds(listATextAreaValue, "listA");
        Future<IdConversionResult> listBGePiIds = convertToAggregateIds(listBTextAreaValue, "listB");
//        if ((filterString != null && !filterString.isBlank()) || selectedDevSettings.contains("Always use ES")) {
        fetchEventsFromElasticSearch(selectedEventTypeNames, isAListPresent, isABSearchRequest, listAGePiIds, listBGePiIds);
//        } else {
        fetchEventsFromNeo4j(selectedEventTypeNames, isAListPresent, isABSearchRequest);
//        }

        if (isABSearchRequest) {
            inputMode = EnumSet.of(InputMode.AB);
        } else if (isAListPresent){
            inputMode = EnumSet.of(InputMode.A);
        }
        if (isSentenceFilterPresent || isParagraphFilterPresent) {
            if (inputMode != null)
                inputMode.add(InputMode.FULLTEXT_QUERY);
            else
                inputMode = EnumSet.of(InputMode.FULLTEXT_QUERY);
        }

        data = new GePiData(neo4jResult, esResult, listAGePiIds, listBGePiIds);
        log.debug("Setting newly retrieved data for dataSessionId: {}", dataSessionId);
        dataService.putData(dataSessionId, data);
        Index indexPage = (Index) resources.getContainer();
        ajaxResponseRenderer.addRender(indexPage.getInputZone()).addRender(indexPage.getOutputZone());
        log.debug("Ajax rendering commands sent, entering the output display mode");
    }

    private void fetchEventsFromNeo4j(List<String> selectedEventTypeNames, boolean isAListPresent, boolean isABSearchRequest) {
        if (listATextAreaValue != null && !listATextAreaValue.isBlank()) {
            CompletableFuture<Stream<String>> aListIds = CompletableFuture.completedFuture(Stream.of(listATextAreaValue.split("\n")));
            if (isABSearchRequest) {
                neo4jResult = aggregatedEventsRetrievalService.getEvents(aListIds, CompletableFuture.completedFuture(Stream.of(listBTextAreaValue.split("\n"))), selectedEventTypeNames);
            } else if (isAListPresent) {
                neo4jResult = aggregatedEventsRetrievalService.getEvents(aListIds, selectedEventTypeNames);
            }
            persistNeo4jResult = neo4jResult;
        }
    }

    private void fetchEventsFromElasticSearch(List<String> selectedEventTypeNames, boolean isAListPresent, boolean isABSearchRequest, Future<IdConversionResult> listAGePiIds, Future<IdConversionResult> listBGePiIds) {
        if (isABSearchRequest) {
            log.debug("Calling EventRetrievalService for AB search");
            esResult = eventRetrievalService.getBipartiteEvents(
                    listAGePiIds,
                    listBGePiIds, selectedEventTypeNames, sentenceFilterString, paragraphFilterString);
        } else if (isAListPresent) {
            log.debug("Calling EventRetrievalService for A search");
            esResult = eventRetrievalService.getOutsideEvents(listAGePiIds, selectedEventTypeNames, sentenceFilterString, paragraphFilterString);
        } else {
            // No IDs were entered
            log.debug("Calling EventRetrievalService for scope filtered events");
            esResult = eventRetrievalService.getFulltextFilteredEvents(selectedEventTypeNames, sentenceFilterString, paragraphFilterString, filterFieldsConnectionOperator);
        }

        persistEsResult = esResult;
    }

    void onFailure() {
        if (request.isXHR()) {
            Index indexPage = (Index) resources.getContainer();
            ajaxResponseRenderer.addRender(indexPage.getInputZone());
        }
    }

    void afterRender() {
        javaScriptSupport.require("gepi/components/gepiinput").invoke("initialize").with(resultPresent());
        log.debug("Result present: " + resultPresent());
        if (resultPresent() && newSearch) {
            log.debug("Sending JS call to show the output widgets.");
            javaScriptSupport.require("gepi/components/gepiinput").invoke("showOutput");
        }
    }

    private Future<IdConversionResult> convertToAggregateIds(String input, String listName) {
        if (input != null) {
            List<String> inputList = Stream.of(input.split("\n")).map(String::trim).collect(Collectors.toList());
            log.debug("Got {} input IDs from {}", inputList.size(), listName);
            IGeneIdService.IdType idType = geneIdService.determineIdType(inputList.stream());
            log.debug("Identified input IDs of {} as: {}", listName, idType);
            return geneIdService.convert(inputList.stream(), idType, IGeneIdService.IdType.GEPI_AGGREGATE);
        }
        return null;
    }

    private boolean resultPresent() {
        return dataService.getData(dataSessionId) != GePiData.EMPTY;
    }

    private enum EventTypes {Regulation, Positive_regulation, Negative_regulation, Binding, Localization, Phosphorylation}


}
