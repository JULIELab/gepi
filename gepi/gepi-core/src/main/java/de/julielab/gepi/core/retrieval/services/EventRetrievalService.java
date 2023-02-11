package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.components.ISearchServerComponent;
import de.julielab.elastic.query.components.data.ElasticSearchCarrier;
import de.julielab.elastic.query.components.data.ElasticServerResponse;
import de.julielab.elastic.query.components.data.HighlightCommand;
import de.julielab.elastic.query.components.data.SearchServerRequest;
import de.julielab.elastic.query.components.data.SortCommand.SortOrder;
import de.julielab.elastic.query.components.data.aggregation.TermsAggregation;
import de.julielab.elastic.query.components.data.query.*;
import de.julielab.gepi.core.GepiCoreSymbolConstants;
import de.julielab.gepi.core.retrieval.data.*;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult.EventResultType;
import de.julielab.gepi.core.services.IGeneIdService;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Gets any IDs, converts them to GePi IDs (or just queries the index?!) and
 * returns the found relations
 *
 * @author faessler
 */
public class EventRetrievalService implements IEventRetrievalService {

    public static final String FIELD_PMID = "pmid";

    public static final String FIELD_PMCID = "pmcid";

    public static final String FIELD_EVENT_MAINEVENTTYPE = "maineventtype";

    public static final String FIELD_EVENT_ALL_EVENTTYPES = "alleventtypes";

    public static final String FIELD_EVENT_ARGUMENTSEARCH = "arguments";

    public static final String FIELD_EVENT_ARG_GENE_IDS = "argumentgeneids";

    public static final String FIELD_EVENT_TAX_IDS = "argumenttaxids";

    public static final String FIELD_EVENT_ARG_CONCEPT_IDS = "argumentconceptids";

    public static final String FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS = "argumenttophomoids";

    public static final String FIELD_EVENT_ARG_TEXT = "argumentcoveredtext";

    public static final String FIELD_EVENT_ARG_PREFERRED_NAME = "argumentprefnames";

    public static final String FIELD_EVENT_ARG_HOMOLOGY_PREFERRED_NAME = "argumenthomoprefnames";


    public static final String FIELD_EVENT_ARGUMENT1SEARCH = "argument1";

    public static final String FIELD_EVENT_ARG1_GENE_ID = "argument1geneid";

    public static final String FIELD_EVENT_ARG1_CONCEPT_ID = "argument1conceptid";

    public static final String FIELD_EVENT_ARG1_TOP_HOMOLOGY_ID = "argument1tophomoid";

    public static final String FIELD_EVENT_ARG1_MATCH_TYPE = "argument1matchtype";

    public static final String FIELD_EVENT_ARG1_TEXT = "argument1coveredtext";

    public static final String FIELD_EVENT_ARG1_PREFERRED_NAME = "argument1prefname";

    public static final String FIELD_EVENT_ARG1_HOMOLOGY_PREFERRED_NAME = "argument1homoprefname";

    public static final String FIELD_EVENT_ARGUMENT2SEARCH = "argument2";

    public static final String FIELD_EVENT_ARG2_GENE_ID = "argument2geneid";

    public static final String FIELD_EVENT_ARG2_CONCEPT_ID = "argument2conceptid";

    public static final String FIELD_EVENT_ARG2_TOP_HOMOLOGY_ID = "argument2tophomoid";

    public static final String FIELD_EVENT_ARG2_MATCH_TYPE = "argument2matchtype";

    public static final String FIELD_EVENT_ARG2_TEXT = "argument2coveredtext";

    public static final String FIELD_EVENT_ARG2_PREFERRED_NAME = "argument2prefname";

    public static final String FIELD_EVENT_ARG2_HOMOLOGY_PREFERRED_NAME = "argument2homoprefname";

    public static final String FIELD_EVENT_SENTENCE_TEXT = "sentence.text";

    public static final String FIELD_EVENT_PARAGRAPH_TEXT = "paragraph.text";

    public static final String FIELD_EVENT_SENTENCE_TEXT_FILTER = "sentence.text";

    public static final String FIELD_EVENT_PARAGRAPH_TEXT_FILTER = "paragraph.text";

    public static final String FIELD_EVENT_SENTENCE_TEXT_ARGUMENTS = "sentence.text_arguments";

    public static final String FIELD_EVENT_PARAGRAPH_TEXT_ARGUMENTS = "paragraph.text_arguments";

    public static final String FIELD_EVENT_SENTENCE_TEXT_TRIGGER = "sentence.text_trigger";

    public static final String FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_1 = "sentence.text_likelihood_1";

    public static final String FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_2 = "sentence.text_likelihood_2";

    public static final String FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_3 = "sentence.text_likelihood_3";

    public static final String FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_4 = "sentence.text_likelihood_4";

    public static final String FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_5 = "sentence.text_likelihood_5";

    public static final String FIELD_EVENT_PARAGRAPH_TEXT_TRIGGER = "paragraph.text_trigger";

    public static final String FIELD_PARAGRAPH_HEADINGS = "paragraph.headings";

    public static final String FIELD_EVENT_LIKELIHOOD = "likelihood";

    public static final String FIELD_NUM_ARGUMENTS = "numarguments";

    public static final String FIELD_AGGREGATION_VALUE = "aggregationvalue";

    /**
     * The values in the field have the form symbol1---symbol2
     */
    public static final String AGGREGATION_VALUE_DELIMITER = "---";


    public static final String FIELD_VALUE_MOCK_ARGUMENT = "none";

    public static final int DEFAULT_PAGE_SIZE = 10;

    public static final String FIELD_GENE_MAPPING_SOURCE = "genemappingsource";
    public static final List<String> FIELDS_FOR_TABLE = Arrays.asList(
            FIELD_PMID,
            FIELD_PMCID,
            FIELD_EVENT_LIKELIHOOD,
            FIELD_EVENT_SENTENCE_TEXT,
//            FIELD_EVENT_PARAGRAPH_TEXT,
            FIELD_EVENT_MAINEVENTTYPE,
            FIELD_EVENT_ALL_EVENTTYPES,
            FIELD_EVENT_ARG_GENE_IDS,
            FIELD_EVENT_ARG_CONCEPT_IDS,
            FIELD_EVENT_ARG_PREFERRED_NAME,
            FIELD_EVENT_ARG_HOMOLOGY_PREFERRED_NAME,
            FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS,
            FIELD_EVENT_ARG_TEXT,
            FIELD_NUM_ARGUMENTS
//            ,
//            FIELD_GENE_MAPPING_SOURCE
    );
    public static final List<String> FIELDS_FOR_CHARTS = Arrays.asList(
//            FIELD_EVENT_ARG_GENE_IDS,
            FIELD_EVENT_ARG_CONCEPT_IDS,
            FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS,
            FIELD_EVENT_ARG_PREFERRED_NAME,
            FIELD_EVENT_ARG_HOMOLOGY_PREFERRED_NAME,
            FIELD_NUM_ARGUMENTS
    );
    private static final int SCROLL_SIZE = 2000;
    private Logger log;
    private IGeneIdService geneIdService;
    private ISearchServerComponent searchServerComponent;
    private String documentIndex;
    private IEventResponseProcessingService eventResponseProcessingService;

    public EventRetrievalService(@Symbol(GepiCoreSymbolConstants.INDEX_DOCUMENTS) String documentIndex, Logger log,
                                 IEventResponseProcessingService eventResponseProcessingService, IGeneIdService geneIdService,
                                 ISearchServerComponent searchServerComponent) {
        this.documentIndex = documentIndex;
        this.log = log;
        this.eventResponseProcessingService = eventResponseProcessingService;
        this.geneIdService = geneIdService;
        this.searchServerComponent = searchServerComponent;
    }

    @Override
    public Future<EventRetrievalResult> getEvents(GepiRequestData requestData, boolean forCharts) {
        return getEvents(requestData, 0, DEFAULT_PAGE_SIZE, forCharts);
    }

    /**
     * If <tt>numRows</tt> is set to 0, only argument pairs are retrieved for the charts.
     *
     * @param requestData
     * @param from
     * @param numRows
     * @param forCharts
     * @return
     */
    @Override
    public Future<EventRetrievalResult> getEvents(GepiRequestData requestData, int from, int numRows, boolean forCharts) {
        Future<EventRetrievalResult> esResult;
        EnumSet<InputMode> inputMode = requestData.getInputMode();
        if (inputMode.contains(InputMode.AB)) {
            log.debug("Calling EventRetrievalService for AB search for rows from {}, number of rows {}, forCharts: {}", from, numRows, forCharts);
            esResult = closedSearch(requestData, from, numRows, forCharts);
        } else if (inputMode.contains(InputMode.A)) {
            log.debug("Calling EventRetrievalService for A search for rows from {}, number of rows {}, forCharts: {}", from, numRows, forCharts);
            esResult = openSearch(requestData, from, numRows, forCharts);
        } else {
            // No IDs were entered
            log.debug("Calling EventRetrievalService for scope filtered events for rows from {}, number of rows {}, forCharts: {}", from, numRows, forCharts);
            esResult = getFulltextFilteredEvents(requestData, from, numRows, forCharts);
        }
        return esResult;
    }

    @Override
    public Future<EventRetrievalResult> closedSearch(GepiRequestData requestData, int from, int numRows, boolean forCharts) {
        return CompletableFuture.supplyAsync(() -> {
            try {

                log.debug("Retrieving closed events for {} A IDs and {} B IDs", requestData.getListAGePiIds().get().getConvertedItems().size(), requestData.getListBGePiIds().get().getConvertedItems().size());
                if (log.isDebugEnabled())
                    log.debug("Some A target IDs are: {}", requestData.getListAGePiIds().get().getTargetIds().stream().limit(10).collect(Collectors.joining(", ")));
                if (log.isDebugEnabled())
                    log.debug("Some B target IDs are: {}", requestData.getListBGePiIds().get().getTargetIds().stream().limit(10).collect(Collectors.joining(", ")));

                SearchServerRequest serverRqst = getClosedSearchRequest(requestData, from, numRows, forCharts);

                ElasticSearchCarrier<ElasticServerResponse> carrier = new ElasticSearchCarrier<>("ClosedSearch");
                carrier.addSearchServerRequest(serverRqst);
                long time = System.currentTimeMillis();
                log.debug("Sent closed search server request");
                searchServerComponent.process(carrier);
                if (log.isDebugEnabled())
                    log.debug("Server answered after {} seconds. Reading results.", (System.currentTimeMillis() - time) / 1000);

                EventRetrievalResult eventResult = eventResponseProcessingService
                        .getEventRetrievalResult(carrier.getSingleSearchServerResponse());
                eventResult.setStartRow(from);
                eventResult.setEndRow(from + numRows - 1);
                time = System.currentTimeMillis() - time;
                log.debug("Retrieved {} events for closed search from ElasticSearch in {} seconds with forCharts={}", eventResult.getEventList().size(), time / 1000, forCharts);
                eventResult.setResultType(EventResultType.BIPARTITE);
                reorderBipartiteEventResultArguments(requestData.getAListIdsAsSet(), requestData.getBListIdsAsSet(), eventResult);
                return eventResult;
            } catch (InterruptedException | ExecutionException e) {
                log.error("Could not retrieve the IDs for the query", e);
            }
            return null;
        });
    }

    private SearchServerRequest getClosedSearchRequest(GepiRequestData requestData, int from, int numRows, boolean forCharts) throws ExecutionException, InterruptedException {
        BoolQuery eventQuery = EventQueries.getClosedQuery(requestData, requestData.getAListIdsAsSet(), requestData.getBListIdsAsSet());

        boolean downloadAll = forCharts || numRows == Integer.MAX_VALUE;

        SearchServerRequest serverRqst = new SearchServerRequest();
        serverRqst.query = eventQuery;
        serverRqst.index = documentIndex;
        serverRqst.start = from;
        serverRqst.rows = numRows;
        configureDeepPaging(serverRqst, downloadAll, forCharts, requestData.getEventRetrievalLimitForAggregations());
        if (!downloadAll && numRows > 0) {
            addHighlighting(serverRqst);
        }
        return serverRqst;
    }


    @Override
    public Future<EventRetrievalResult> closedSearch(GepiRequestData requestData) {
        return closedSearch(requestData, 0, DEFAULT_PAGE_SIZE, false);
    }

    /**
     * Reorders the arguments of the events to make the first argument correspond to
     * the A ID list and the second argument to the B ID list.
     *
     * @param idSetA      The set of list A query IDs.
     * @param idSetB      The set of list B query IDs.
     * @param eventResult The event result as returned by the
     *                    {@link EventResponseProcessingService}.
     */
    private void reorderBipartiteEventResultArguments(Set<String> idSetA, Set<String> idSetB,
                                                      EventRetrievalResult eventResult) {
        // reorder all arguments such that the first argument corresponds to
        // an input ID from list A and the second argument corresponds to an
        // ID from list B

        for (Event e : eventResult.getEventList()) {
            Argument firstArg = e.getFirstArgument();
            Argument secondArg = e.getSecondArgument();
            if (!(idSetA.contains(firstArg.getGeneId()) || idSetA.contains(firstArg.getTopHomologyId())))
                e.swapArguments();
            else if (!(idSetB.contains(secondArg.getGeneId()) || idSetB.contains(secondArg.getTopHomologyId())))
                e.swapArguments();
        }
    }

    @Override
    public Future<EventRetrievalResult> openSearch(GepiRequestData requestData) {
        return openSearch(requestData, 0, DEFAULT_PAGE_SIZE, false);
    }

    @Override
    public Future<EventRetrievalResult> openSearch(GepiRequestData gepiRequestData, int from, int numRows, boolean forCharts) {
        assert gepiRequestData.getListAGePiIds() != null : "No A-list IDs set.";
        log.debug("Returning async result");
        return CompletableFuture.supplyAsync(() -> {
            try {
                Set<String> idSet = gepiRequestData.getAListIdsAsSet();

                log.debug("Retrieving outside events for {} A IDs", idSet.size());
                if (log.isDebugEnabled())
                    log.debug("Some A target IDs are: {}", gepiRequestData.getListAGePiIds().get().getTargetIds().stream().limit(10).collect(Collectors.joining(", ")));
                SearchServerRequest serverCmd = getOpenSearchRequest(gepiRequestData, from, numRows, forCharts);

                ElasticSearchCarrier<ElasticServerResponse> carrier = new ElasticSearchCarrier("OpenSearch");
                carrier.addSearchServerRequest(serverCmd);
                long time = System.currentTimeMillis();
                log.debug("Sent open search server request");
                searchServerComponent.process(carrier);
                if (log.isDebugEnabled())
                    log.debug("Server answered after {} seconds. Reading results.", (System.currentTimeMillis() - time) / 1000);


                EventRetrievalResult eventResult = eventResponseProcessingService
                        .getEventRetrievalResult(carrier.getSingleSearchServerResponse());
                eventResult.setStartRow(from);
                eventResult.setEndRow(from + numRows - 1);
                time = System.currentTimeMillis() - time;
                log.info("Retrieved {} events for open search from ElasticSearch in {} seconds with forCharts={}", eventResult.getEventList().size(), time / 1000, forCharts);
                eventResult.setResultType(EventResultType.OUTSIDE);
                reorderOutsideEventResultsArguments(idSet, eventResult);
                log.debug("After reordering, the event list has {} elements", eventResult.getEventList().size());
                return eventResult;
            } catch (InterruptedException | ExecutionException e) {
                log.error("Could not retrieve the IDs for the query", e);
                throw new RuntimeException(e);
            } catch (Error error) {
                error.printStackTrace();
                throw error;
            }
        });
    }

    @Override
    public SearchServerRequest getOpenSearchRequest(GepiRequestData requestData) throws ExecutionException, InterruptedException {
        return getOpenSearchRequest(requestData, 0, DEFAULT_PAGE_SIZE, false);
    }

    @Override
    public SearchServerRequest getOpenSearchRequest(GepiRequestData requestData, int from, int numRows, boolean forCharts) throws ExecutionException, InterruptedException {
        BoolQuery eventQuery = EventQueries.getOpenQuery(requestData);

        boolean downloadAll = forCharts || numRows == Integer.MAX_VALUE;

        SearchServerRequest serverRqst = new SearchServerRequest();
        serverRqst.query = eventQuery;
        serverRqst.index = documentIndex;
        serverRqst.start = from;
        serverRqst.rows = numRows;
        configureDeepPaging(serverRqst, downloadAll, forCharts, requestData.getEventRetrievalLimitForAggregations());
        if (!downloadAll && numRows > 0) {
            addHighlighting(serverRqst);
        }
        return serverRqst;
    }

    private void configureDeepPaging(SearchServerRequest serverRqst, boolean downloadAll, boolean forCharts, int interactionRetrievalLimit) {
        if (downloadAll)
            serverRqst.rows = forCharts ? Math.min(SCROLL_SIZE, interactionRetrievalLimit) : SCROLL_SIZE;
        serverRqst.fieldsToReturn = forCharts ? FIELDS_FOR_CHARTS : FIELDS_FOR_TABLE;
        serverRqst.downloadCompleteResults = downloadAll && (!forCharts || interactionRetrievalLimit > 0);
        serverRqst.downloadCompleteResultsMethod = "searchAfter";
        serverRqst.downloadCompleteResultMethodKeepAlive = "5m";
        if (downloadAll) {
            if (forCharts && interactionRetrievalLimit < Integer.MAX_VALUE)
                serverRqst.downloadCompleteResultsLimit = interactionRetrievalLimit;
            serverRqst.addSortCommand("_shard_doc", SortOrder.ASCENDING);
        }
    }

    private void addHighlighting(SearchServerRequest serverRqst) {
        serverRqst.addHighlightCmd(getHighlightCommand("xargumentx", "hl-argument", FIELD_EVENT_SENTENCE_TEXT_ARGUMENTS));
        serverRqst.addHighlightCmd(getHighlightCommand("xtriggerx", "hl-trigger", FIELD_EVENT_SENTENCE_TEXT_TRIGGER));
        serverRqst.addHighlightCmd(getHighlightCommand("xlike1x", "hl-like1", FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_1));
        serverRqst.addHighlightCmd(getHighlightCommand("xlike2x", "hl-like2", FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_2));
        serverRqst.addHighlightCmd(getHighlightCommand("xlike3x", "hl-like3", FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_3));
        serverRqst.addHighlightCmd(getHighlightCommand("xlike4x", "hl-like4", FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_4));
        serverRqst.addHighlightCmd(getHighlightCommand("xlike5x", "hl-like5", FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_5));
        serverRqst.addHighlightCmd(getHighlightCommand(null, "hl-filter", FIELD_EVENT_SENTENCE_TEXT, FIELD_EVENT_PARAGRAPH_TEXT));
    }

    /**
     * <p>Created highlight commands required for GePI searches.</p>
     * <p>When <code>hlTerm</code> is not null, it used in a <code>TermQuery</code> that is specified as a highlight query. This is used to highlight only special terms like event argument and event trigger words. The respective placeholder terms - <code>xargumentx</code> and <code>xtriggerx</code> - have been added in the <code>RelationDocumentGenerator</code> in the indexing code. If <code>hlTerm</code> is null, the actual query terms are highlighted.</p>
     *
     * @param hlTerm
     * @param hlClass
     * @return
     */
    private HighlightCommand getHighlightCommand(String hlTerm, String hlClass, String... hlFields) {
        HighlightCommand hlc = new HighlightCommand();
        for (String hlField : hlFields)
            hlc.addField(hlField, 1, 0);
        hlc.fields.forEach(f -> {
            f.pre = "<mark class=\"" + hlClass + "\">";
            f.post = "</mark>";
            if (hlTerm != null) {
                SearchServerQuery q;
                if (hlTerm.contains("*")) {
                    final WildcardQuery wcq = new WildcardQuery();
                    wcq.field = f.field;
                    wcq.query = hlTerm;
                    q = wcq;
                } else {
                    TermQuery tq = new TermQuery();
                    tq.field = f.field;
                    tq.term = hlTerm;
                    q = tq;
                }
                f.highlightQuery = q;
            }
            if (f.field.contains("paragraph"))
                f.fragsize = 80;
        });
        return hlc;
    }


    @Override
    public CompletableFuture<EventRetrievalResult> getFulltextFilteredEvents(GepiRequestData requestData, int from, int numRows, boolean forCharts) {
        log.debug("Returning async result");
        return CompletableFuture.supplyAsync(() -> {
            SearchServerRequest serverRqst = getFulltextSearchRequest(requestData, from, numRows, forCharts);

            ElasticSearchCarrier<ElasticServerResponse> carrier = new ElasticSearchCarrier("FulltextFilteredEvents");
            carrier.addSearchServerRequest(serverRqst);
            long time = System.currentTimeMillis();
            log.debug("Sent full-text search server request");
            searchServerComponent.process(carrier);
            if (log.isDebugEnabled())
                log.debug("Server answered after {} seconds. Reading results.", (System.currentTimeMillis() - time) / 1000);

            EventRetrievalResult eventResult = eventResponseProcessingService
                    .getEventRetrievalResult(carrier.getSingleSearchServerResponse());
            eventResult.setStartRow(from);
            eventResult.setEndRow(from + numRows - 1);
            time = System.currentTimeMillis() - time;
            log.info("Retrieved {} fulltext-filtered events in {} seconds with forCharts={}", eventResult.getEventList().size(), time / 1000, forCharts);
            eventResult.setResultType(EventResultType.FULLTEXT_FILTERED);
            return eventResult;
        });
    }

    private SearchServerRequest getFulltextSearchRequest(GepiRequestData requestData, int from, int numRows, boolean forCharts) {
        BoolQuery eventQuery = EventQueries.getFulltextQuery(requestData);

        boolean downloadAll = forCharts || numRows == Integer.MAX_VALUE;

        SearchServerRequest serverRqst = new SearchServerRequest();
        serverRqst.query = eventQuery;
        serverRqst.index = documentIndex;
        serverRqst.start = from;
        serverRqst.rows = numRows;
        configureDeepPaging(serverRqst, downloadAll, forCharts, requestData.getEventRetrievalLimitForAggregations());
        if (!downloadAll && numRows > 0) {
            addHighlighting(serverRqst);
        }
        return serverRqst;
    }

    @Override
    public long getTotalNumberOfEvents() {
        SearchServerRequest serverRqst = new SearchServerRequest();
        serverRqst.query = new MatchAllQuery();
        serverRqst.index = documentIndex;
        serverRqst.rows = 0;
        ElasticSearchCarrier<ElasticServerResponse> carrier = new ElasticSearchCarrier("EventCount");
        carrier.addSearchServerRequest(serverRqst);
        long time = System.currentTimeMillis();
        searchServerComponent.process(carrier);
        long numFound = carrier.getSingleSearchServerResponse().getNumFound();
        time = System.currentTimeMillis() - time;
        log.debug("Determined the number of {} in the index in {} seconds", numFound, time / 1000);
        return numFound;
    }

    @Override
    public Future<EsAggregatedResult> getAggregatedEvents(GepiRequestData requestData) {
        Future<EsAggregatedResult> esResult;
        EnumSet<InputMode> inputMode = requestData.getInputMode();
        if (inputMode.contains(InputMode.AB)) {
            esResult = closedAggregatedSearch(requestData);
        } else if (inputMode.contains(InputMode.A)) {
            esResult = openAggregatedSearch(requestData);
        } else {
            // No IDs were entered
            esResult = getFulltextFilteredAggregatedEvents(requestData);
        }
        return esResult;
    }

    private Future<EsAggregatedResult> getFulltextFilteredAggregatedEvents(GepiRequestData requestData) {
        return null;
    }

    @Override
    public Future<EsAggregatedResult> openAggregatedSearch(GepiRequestData requestData) {
        try {
            final SearchServerRequest openSearchRequest = getOpenSearchRequest(requestData, 0, 0, false);
            return aggregatedSearch(requestData, openSearchRequest);
        } catch (Exception e) {
            log.error("Open aggregated search failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Future<EsAggregatedResult> closedAggregatedSearch(GepiRequestData requestData) {
        try {
            final SearchServerRequest closedSearchRequest = getClosedSearchRequest(requestData, 0, 0, false);
            return aggregatedSearch(requestData, closedSearchRequest);
        } catch (Exception e) {
            log.error("Closed aggregated search failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Future<EsAggregatedResult> fulltextAggregatedSearch(GepiRequestData requestData) {
        try {
            final SearchServerRequest fulltextSearchRequest= getFulltextSearchRequest(requestData, 0, 0, false);
            return aggregatedSearch(requestData, fulltextSearchRequest);
        } catch (Exception e) {
            log.error("Closed aggregated search failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>Returns the aggregated event counts for the given request data and the open, closed or fulltext serverRequest that was created from the requestData.</p>
     *
     * @param requestData
     * @param serverRequest
     * @return
     */
    private Future<EsAggregatedResult> aggregatedSearch(GepiRequestData requestData, SearchServerRequest serverRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Short comment: Fetch the aggregate names of the input A IDs so we can tell in the aggregate values
                // which argument belongs to A.
                // Long comment: The aggregationvalue field stores the symbol of the gene top-aggregates (orthology / homology / famplex / hgnc),
                // see de.julielab.gepi.indexing.GeneFilterBoard.orgid2topaggprefname, e.g. AKT---MTOR.
                // For genes it is the highest orthology aggregate. For FamPlex and HGNC Groups it is the
                // highest equal-name aggregate. The latter poses is a discrepancy because the GeneIdService does not map
                // FamPlex or HGNC Group inputs to the equal-name aggregate. This is not an issue, however,
                // because the equal-name aggregate always has the same name as its elements, if it exists (hence the name).
                final Future<Map<String, GepiConceptInfo>> aGeneInfo = requestData.getListAGePiIds() != null ? CompletableFuture.supplyAsync(() -> {
                    try {
                        return geneIdService.getGeneInfo(requestData.getAListIdsAsSet());
                    } catch (Exception e) {
                        log.error("Could not retrieve gene info");
                        throw new RuntimeException(e);
                    }
                }) : null;


                final TermsAggregation eventCountRequest = new TermsAggregation();
                eventCountRequest.name = "events";
                eventCountRequest.field = FIELD_AGGREGATION_VALUE;
                eventCountRequest.size = Integer.MAX_VALUE;

                serverRequest.addAggregationCommand(eventCountRequest);

                ElasticSearchCarrier<ElasticServerResponse> carrier = new ElasticSearchCarrier("AggregatedSearch");
                carrier.addSearchServerRequest(serverRequest);
                long time = System.currentTimeMillis();
                log.debug("Sent full-text search server request");
                searchServerComponent.process(carrier);
                if (log.isDebugEnabled())
                    log.debug("Server answered after {} seconds. Reading results.", (System.currentTimeMillis() - time) / 1000);


                EsAggregatedResult aggregatedResult = eventResponseProcessingService
                        .getEventRetrievalAggregatedResult(carrier.getSingleSearchServerResponse(), eventCountRequest, aGeneInfo != null ? aGeneInfo.get().values().stream().map(GepiConceptInfo::getSymbol).collect(Collectors.toSet()) : Collections.emptySet());
                return aggregatedResult;
            } catch (Exception e) {
                log.error("Open aggregated search failed", e);
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * Reorder the arguments of the result events such that the first argument
     * always corresponds to an ID in the query ID list.
     *
     * @param idSet       The set of query IDs.
     * @param eventResult The event result as returned by
     *                    {@link EventResponseProcessingService}.
     */
    private void reorderOutsideEventResultsArguments(Set<String> idSet, EventRetrievalResult eventResult) {
        // reorder all arguments such that the first argument corresponds to
        // the input ID that caused the match

        List<Event> reorderedEvents = new ArrayList<>(eventResult.getEventList());
        for (Iterator<Event> it = reorderedEvents.iterator(); it.hasNext(); ) {
            Event e = it.next();
            if (e.getArity() > 1) {
                final Argument secondArg = e.getArgument(1);
                if (idSet.contains(secondArg.getTopHomologyId())) {
                    e.swapArguments();
                }
            }
        }
        eventResult.setEvents(reorderedEvents);
    }

}
