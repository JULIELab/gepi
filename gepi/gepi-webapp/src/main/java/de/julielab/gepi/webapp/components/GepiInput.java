package de.julielab.gepi.webapp.components;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.julielab.gepi.core.retrieval.data.*;
import de.julielab.gepi.core.retrieval.services.IAggregatedEventsRetrievalService;
import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.gepi.core.retrieval.data.GepiRequestData;
import de.julielab.gepi.webapp.base.TabPersistentField;
import de.julielab.gepi.webapp.data.EventTypes;
import de.julielab.gepi.webapp.data.GepiQueryParameters;
import org.apache.tapestry5.*;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.commons.Messages;
import org.apache.tapestry5.commons.services.TypeCoercer;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.corelib.components.TextArea;
import org.apache.tapestry5.corelib.components.TextField;
import org.apache.tapestry5.http.services.Request;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ApplicationStateManager;
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

    @Inject
    private ApplicationStateManager asm;

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

    @InjectComponent
    private TextField sectionnameFilter;

    @Property
    @Persist(TabPersistentField.TAB)
    private String listATextAreaValue;

    @Property
    @Persist(TabPersistentField.TAB)
    private String listBTextAreaValue;

    @Property
    @Persist(TabPersistentField.TAB)
    private String taxId;

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

    @Property
    @Persist(TabPersistentField.TAB)
    private CompletableFuture<EventRetrievalResult> persistEsResult;

    @Parameter
    private CompletableFuture<AggregatedEventsRetrievalResult> neo4jResult;

    @Property
    @Persist(TabPersistentField.TAB)
    private CompletableFuture<AggregatedEventsRetrievalResult> persistNeo4jResult;

    @Inject
    private TypeCoercer typeCoercer;

    @Inject
    private Messages messages;

    @Property
    private List<EventTypes> selectedEventTypes = new ArrayList<>(EnumSet.allOf(EventTypes.class));

    @Property
    @Parameter
    private String sentenceFilterString;

    @Property
    @Parameter
    private String paragraphFilterString;

    @Property
    @Persist(TabPersistentField.TAB)
    private String filterFieldsConnectionOperator;

    @Property
    @Persist(TabPersistentField.TAB)
    private Integer eventLikelihood;

    @Property
    private String sectionNameFilterString;

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
    /**
     * TODO This is here to allow for paging requests. It overlaps with {@link #data} which should be sorted out at some point.
     */
    @Parameter
    private GepiRequestData requestData;

//    void onActivate(EventContext eventContext) {
//        if (reset) {
//            log.debug("Reset is active, setting data to null.");
//            data = null;
//        }
//    }

    public void reset() {
        listATextAreaValue = "";
        listBTextAreaValue = "";
        taxId = "";
        selectedEventTypes = new ArrayList<>(EnumSet.allOf(EventTypes.class));
        eventLikelihood = 1;
        filterFieldsConnectionOperator = "AND";
        sentenceFilterString = "";
        paragraphFilterString = "";
        sectionNameFilterString = "";
    }

    public ValueEncoder getEventTypeEncoder() {
        return new EnumValueEncoder(typeCoercer, EventTypes.class);
    }

    public SelectModel getEventTypeModel() {
        return new EnumSelectModel(EventTypes.class, messages);
    }

    void setupRender() {
        log.warn("{}", inputMode);
        if (filterFieldsConnectionOperator == null)
            filterFieldsConnectionOperator = "AND";
        if (eventLikelihood == null)
            eventLikelihood = 1;
    }

    void onValidateFromInputForm() {
        // Note, this method is triggered even if server-side validation has
        // already found error(s).
        boolean noIdsGiven = listATextAreaValue == null || listATextAreaValue.isEmpty();
        boolean noSentenceFilterGiven = sentenceFilterString == null || sentenceFilterString.isBlank();
        boolean noParagraphFilterGiven = paragraphFilterString == null || paragraphFilterString.isBlank();
        boolean noSectionHeadingFilterGiven = sectionNameFilterString == null || sectionNameFilterString.isBlank();
        if (noIdsGiven && noSentenceFilterGiven && noParagraphFilterGiven && noSectionHeadingFilterGiven) {
            inputForm.recordError("Input is required to start a search for molecular interactions. There are different ways to start a search. Each possibility presented below or any combination of them can start a search.");
            inputForm.recordError(lista, "Enter a list of NCBI Gene IDs or NCBI Gene symbols into List A to search for interactions that include the provided genes.");
            inputForm.recordError(sentenceFilter, "Enter keywords to filter the interactions for words appearing in the same sentence.");
            inputForm.recordError(paragraphFilter, "Enter keywords to filter the interactions for words appearing in the same paragraph or abstract.");
            inputForm.recordError(sectionnameFilter, "Enter keywords to filter the interactions for words appearing in the section or abstract where the interaction appears in.");
            return;
        }
    }

    void onSuccessFromInputForm() {
        executeSearch();
    }

    public void executeSearch(GepiQueryParameters queryParameters, long dataSessionId) {
        this.selectedEventTypes = queryParameters.getSelectedEventTypes();
        this.listATextAreaValue = queryParameters.getListATextAreaValue();
        this.listBTextAreaValue = queryParameters.getListBTextAreaValue();
        this.taxId = queryParameters.getTaxId();
        this.eventLikelihood = queryParameters.getEventLikelihood();
        this.sentenceFilterString = queryParameters.getSentenceFilterString();
        this.paragraphFilterString = queryParameters.getParagraphFilterString();
        this.filterFieldsConnectionOperator = queryParameters.getFilterFieldsConnectionOperator();
        this.sectionNameFilterString = queryParameters.getSectionNameFilterString();
        this.dataSessionId = dataSessionId;
        executeSearch();
    }

    public void executeSearch() {
        log.debug("Setting newsearch to true");
        newSearch = true;
        List<String> selectedEventTypeNames = selectedEventTypes.stream().flatMap(e -> e == EventTypes.Regulation ? Stream.of(EventTypes.Regulation, EventTypes.Positive_regulation, EventTypes.Negative_regulation) : Stream.of(e)).map(EventTypes::name).collect(Collectors.toList());
        if (selectedEventTypeNames.isEmpty())
            selectedEventTypeNames = EnumSet.allOf(EventTypes.class).stream().map(Enum::name).distinct().collect(Collectors.toList());
        boolean isAListPresent = listATextAreaValue != null && listATextAreaValue.trim().length() > 0;
        boolean isABSearchRequest = listATextAreaValue != null && listATextAreaValue.trim().length() > 0 && listBTextAreaValue != null
                && listBTextAreaValue.trim().length() > 0;
        boolean isSentenceFilterPresent = sentenceFilterString != null && !sentenceFilterString.isBlank();
        boolean isParagraphFilterPresent = paragraphFilterString != null && !paragraphFilterString.isBlank();
        Future<IdConversionResult> listAGePiIds = convertToAggregateIds(listATextAreaValue, "listA");
        Future<IdConversionResult> listBGePiIds = convertToAggregateIds(listBTextAreaValue, "listB");
        if (isABSearchRequest) {
            inputMode = EnumSet.of(InputMode.AB);
        } else if (isAListPresent) {
            inputMode = EnumSet.of(InputMode.A);
        }
        log.info("InputMode {}", inputMode);
        log.info("Filter fields connector: {}", filterFieldsConnectionOperator);
        if (isSentenceFilterPresent || isParagraphFilterPresent) {
            if (inputMode != null)
                inputMode.add(InputMode.FULLTEXT_QUERY);
            else
                inputMode = EnumSet.of(InputMode.FULLTEXT_QUERY);
        }
        requestData = new GepiRequestData(selectedEventTypeNames, eventLikelihood, listAGePiIds, listBGePiIds, taxId.split("\\s*,\\s*"), sentenceFilterString, paragraphFilterString, filterFieldsConnectionOperator, sectionNameFilterString, inputMode, dataSessionId);
        log.debug("Fetching events from ElasticSearch");
//        if ((filterString != null && !filterString.isBlank())) {
        Future<EventRetrievalResult> pagedEsResult = eventRetrievalService.getEvents(requestData, 0, TableResultWidget.ROWS_PER_PAGE, false);
        Future<EventRetrievalResult> unrolledResult4Charts = eventRetrievalService.getEvents(requestData, true);
//        } else {
//        fetchEventsFromNeo4j(selectedEventTypeNames, isAListPresent, isABSearchRequest);
//        }
        final String[] aLines = listATextAreaValue != null ? listATextAreaValue.split("\n") : new String[0];
        final String[] bLines = listBTextAreaValue != null ? listBTextAreaValue.split("\n") : new String[0];
        log.info("[{}] A, first elements: {}", dataSessionId, Arrays.asList(aLines).subList(0, Math.min(5, aLines.length)));
        log.info("[{}] B, first elements: {}", dataSessionId, Arrays.asList(bLines).subList(0, Math.min(5, bLines.length)));
        log.info("[{}] taxIds: {}", dataSessionId, taxId);
        log.info("[{}] sentence filter: {}", dataSessionId, sentenceFilterString);
        log.info("[{}] paragraph filter: {}", dataSessionId, paragraphFilterString);
        log.info("[{}] section filter: {}", dataSessionId, sectionNameFilterString);

        data = new GePiData(neo4jResult, unrolledResult4Charts, pagedEsResult, listAGePiIds, listBGePiIds);
        log.debug("Setting newly retrieved data for dataSessionId: {}", dataSessionId);
        dataService.putData(dataSessionId, data);
        Index indexPage = (Index) resources.getContainer();
        ajaxResponseRenderer.addRender(indexPage.getInputZone()).addRender(indexPage.getOutputZone());
        log.trace("Ajax rendering commands sent, entering the output display mode");
        log.debug("Query input and sending process finished, results can be retrieved and displayed");
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
            List<String> inputList = Stream.of(input.split("[\n,]")).map(String::trim).collect(Collectors.toList());
            log.debug("Got {} input IDs from {}", inputList.size(), listName);
            IGeneIdService.IdType toIdType = IGeneIdService.IdType.GEPI_AGGREGATE;
            return geneIdService.convert(inputList.stream(),  toIdType);
        }
        return null;
    }

    private boolean resultPresent() {
        return dataService.getData(dataSessionId) != GePiData.EMPTY;
    }

    public String getFulltextFilterTooltip() {
        return "The document context is stored with the interactions and can be used for filter purposes. The supported query syntax is described in the ElasticSearch <a target=\"_blank\" class=\"link-secondary\" href=\"https://www.elastic.co/guide/en/elasticsearch/reference/7.17/query-dsl-simple-query-string-query.html\">documentation</a>.";
    }
}
