package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.components.ISearchServerComponent;
import de.julielab.elastic.query.components.data.ElasticSearchCarrier;
import de.julielab.elastic.query.components.data.ElasticServerResponse;
import de.julielab.elastic.query.components.data.HighlightCommand;
import de.julielab.elastic.query.components.data.SearchServerRequest;
import de.julielab.elastic.query.components.data.SortCommand.SortOrder;
import de.julielab.elastic.query.components.data.query.*;
import de.julielab.gepi.core.GepiCoreSymbolConstants;
import de.julielab.gepi.core.retrieval.data.*;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult.EventResultType;
import org.apache.commons.lang.StringUtils;
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

    public static final String FIELD_EVENT_ARG_CONCEPT_IDS = "argumentconceptids";

    public static final String FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS = "argumenttophomoids";

    public static final String FIELD_EVENT_ARG_MATCH_TYPES = "argumentmatchtypes";

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

    public static final String FIELD_EVENT_SENTENCE = "sentence.text";

    public static final String FIELD_EVENT_PARAGRAPH = "paragraph.text";

    public static final String FIELD_PARAGRAPH_HEADINGS = "paragraph.headings";

    public static final String FIELD_EVENT_LIKELIHOOD = "likelihood";

    public static final String FIELD_GENE_MAPPING_SOURCE = "genemappingsource";

    private static final int SCROLL_SIZE = 2000;
    private Logger log;
    private ISearchServerComponent searchServerComponent;
    private String documentIndex;
    private IEventResponseProcessingService eventResponseProcessingService;

    public EventRetrievalService(@Symbol(GepiCoreSymbolConstants.INDEX_DOCUMENTS) String documentIndex, Logger log,
                                 IEventResponseProcessingService eventResponseProcessingService,
                                 ISearchServerComponent searchServerComponent) {
        this.documentIndex = documentIndex;
        this.log = log;
        this.eventResponseProcessingService = eventResponseProcessingService;
        this.searchServerComponent = searchServerComponent;
    }

    @Override
    public CompletableFuture<EventRetrievalResult> getEvents(GepiRequestData requestData) {
        return getEvents(requestData, 0, Integer.MAX_VALUE);
    }

    /**
     * If <tt>numRows</tt> is set to 0, only argument pairs are retrieved for the charts.
     *
     * @param requestData
     * @param from
     * @param numRows
     * @return
     */
    @Override
    public CompletableFuture<EventRetrievalResult> getEvents(GepiRequestData requestData, int from, int numRows) {
        CompletableFuture<EventRetrievalResult> esResult;
        EnumSet<InputMode> inputMode = requestData.getInputMode();
        if (inputMode.contains(InputMode.AB)) {
            log.debug("Calling EventRetrievalService for AB search");
            esResult = getBipartiteEvents(requestData.getListAGePiIds(), requestData.getListBGePiIds(), requestData.getEventTypes(), requestData.getSentenceFilterString(), requestData.getParagraphFilterString(), requestData.getSectionNameFilterString());
        } else if (inputMode.contains(InputMode.A)) {
            log.debug("Calling EventRetrievalService for A search");
            esResult = getOutsideEvents(requestData, from, numRows);
        } else {
            // No IDs were entered
            log.debug("Calling EventRetrievalService for scope filtered events");
            esResult = getFulltextFilteredEvents(requestData.getEventTypes(), requestData.getSentenceFilterString(), requestData.getParagraphFilterString(), requestData.getFilterFieldsConnectionOperator(), requestData.getSectionNameFilterString());
        }
        return esResult;
    }

    @Override
    public CompletableFuture<EventRetrievalResult> getBipartiteEvents(Future<IdConversionResult> idStreamA,
                                                                      Future<IdConversionResult> idStreamB, List<String> eventTypes, String sentenceFilter, String paragraphFilter, String sectionNameFilterString) {
        return CompletableFuture.supplyAsync(() -> {
            try {

                Set<String> idSetA = idStreamA.get().getConvertedItems().values().stream().collect(Collectors.toSet());

                Set<String> idSetB = idStreamB.get().getConvertedItems().values().stream().collect(Collectors.toSet());

                log.debug("Retrieving bipartite events for {} A IDs and {} B IDs", idSetA.size(), idSetB.size());

                BoolQuery eventQuery = EventQueries.getBipartiteQuery(eventTypes, sentenceFilter, paragraphFilter, idSetA, idSetB);

                SearchServerRequest serverCmd = new SearchServerRequest();
                serverCmd.query = eventQuery;
                serverCmd.index = documentIndex;
                serverCmd.rows = SCROLL_SIZE;
                serverCmd.fieldsToReturn = Collections.emptyList();
                serverCmd.fieldsToReturn = Arrays.asList(
                        FIELD_PMID,
                        FIELD_PMCID,
                        FIELD_EVENT_LIKELIHOOD,
                        FIELD_EVENT_SENTENCE,
                        FIELD_EVENT_PARAGRAPH,
                        FIELD_EVENT_MAINEVENTTYPE,
                        FIELD_EVENT_ALL_EVENTTYPES,
                        FIELD_EVENT_ARG_GENE_IDS,
                        FIELD_EVENT_ARG_CONCEPT_IDS,
                        FIELD_EVENT_ARG_PREFERRED_NAME,
                        FIELD_EVENT_ARG_MATCH_TYPES,
                        FIELD_EVENT_ARG_HOMOLOGY_PREFERRED_NAME,
                        FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS,
                        FIELD_EVENT_ARG_TEXT,
                        FIELD_GENE_MAPPING_SOURCE);
                serverCmd.downloadCompleteResults = true;
                serverCmd.addSortCommand("_doc", SortOrder.ASCENDING);
                if (!StringUtils.isBlank(sentenceFilter) || !StringUtils.isBlank(paragraphFilter)) {
                    HighlightCommand hlc = new HighlightCommand();
                    hlc.addField(FIELD_EVENT_SENTENCE, 10, 0);
                    hlc.addField(FIELD_EVENT_PARAGRAPH, 10, 0);
                    hlc.fields.forEach(f -> {
//                f.boundaryChars = new char[]{'\n', '\t'};
//                f.type = HighlightCommand.Highlighter.fastvector;
                        f.pre = "<b>";
                        f.post = "</b>";
//                MatchQuery hlQuery = new MatchQuery();
//                hlQuery.field = FIELD_EVENT_SENTENCE;
//                hlQuery.query = "xargumentx";
//                f.highlightQuery = hlQuery;
                    });
                    serverCmd.addHighlightCmd(hlc);
                }


                ElasticSearchCarrier<ElasticServerResponse> carrier = new ElasticSearchCarrier<>("BipartiteEvents");
                carrier.addSearchServerRequest(serverCmd);
                searchServerComponent.process(carrier);


                EventRetrievalResult eventResult = eventResponseProcessingService
                        .getEventRetrievalResult(carrier.getSingleSearchServerResponse());
                log.debug("Retrieved {} bipartite events from ElasticSearch ", eventResult.getEventList().size());
                eventResult.setResultType(EventResultType.BIPARTITE);
                reorderBipartiteEventResultArguments(idSetA, idSetB, eventResult);
                return eventResult;
            } catch (InterruptedException | ExecutionException e) {
                log.error("Could not retrieve the IDs for the query", e);
            }
            return null;
        });
    }


    @Override
    public CompletableFuture<EventRetrievalResult> getBipartiteEvents(IdConversionResult idStream1, IdConversionResult idStream2, List<String> eventTypes, String sentenceFilter, String paragraphFilter, String sectionNameFilter) {
        return getBipartiteEvents(CompletableFuture.completedFuture(idStream1), CompletableFuture.completedFuture(idStream2), eventTypes, sentenceFilter, paragraphFilter, sectionNameFilter);
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
    public CompletableFuture<EventRetrievalResult> getOutsideEvents(GepiRequestData requestData) {
        return getOutsideEvents(requestData, 0, Integer.MAX_VALUE);
    }

    @Override
    public CompletableFuture<EventRetrievalResult> getOutsideEvents(GepiRequestData gepiRequestData, int from, int numRows) {
        assert gepiRequestData.getListAGePiIds() != null : "No A-list IDs set.";
        log.debug("Returning async result");
        return CompletableFuture.supplyAsync(() -> {
            try {
                Set<String> idSet = gepiRequestData.getAListIdsAsSet();

                log.debug("Retrieving outside events for {} A IDs", idSet.size());
                log.trace("The A IDs are: {}", idSet);
                SearchServerRequest serverCmd = getOutsideServerRequest(gepiRequestData, from, numRows);

                ElasticSearchCarrier<ElasticServerResponse> carrier = new ElasticSearchCarrier("OutsideEvents");
                carrier.addSearchServerRequest(serverCmd);
                long time = System.currentTimeMillis();
                log.debug("Sent server request");
                searchServerComponent.process(carrier);
                log.debug("Server answered. Reading results.");


                EventRetrievalResult eventResult = eventResponseProcessingService
                        .getEventRetrievalResult(carrier.getSingleSearchServerResponse());
                time = System.currentTimeMillis() - time;
                log.debug("Retrieved {} outside events from ElasticSearch in {} seconds", eventResult.getEventList().size(), time / 1000);
                eventResult.setResultType(EventResultType.OUTSIDE);
                reorderOutsideEventResultsArguments(idSet, eventResult);
                log.debug("After reordering, the event list has {} elements", eventResult.getEventList().size());
                return eventResult;
            } catch (InterruptedException | ExecutionException e) {
                log.error("Could not retrieve the IDs for the query", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public SearchServerRequest getOutsideServerRequest(GepiRequestData requestData) throws ExecutionException, InterruptedException {
        return getOutsideServerRequest(requestData, 0, Integer.MAX_VALUE);
    }

    @Override
    public SearchServerRequest getOutsideServerRequest(GepiRequestData requestData, int from, int numRows) throws ExecutionException, InterruptedException {
        BoolQuery eventQuery = EventQueries.getOutsideQuery(requestData);

        boolean downloadCompleteResults = numRows == 0 || numRows == Integer.MAX_VALUE;

        SearchServerRequest serverCmd = new SearchServerRequest();
        serverCmd.query = eventQuery;
        serverCmd.index = documentIndex;
        serverCmd.start = from;
        serverCmd.rows = numRows;
        if (downloadCompleteResults)
            serverCmd.rows = SCROLL_SIZE;
        serverCmd.fieldsToReturn = Arrays.asList(
                FIELD_PMID,
                FIELD_PMCID,
                FIELD_EVENT_LIKELIHOOD,
                FIELD_EVENT_SENTENCE,
                FIELD_EVENT_MAINEVENTTYPE,
                FIELD_EVENT_ALL_EVENTTYPES,
                FIELD_EVENT_ARG_GENE_IDS,
                FIELD_EVENT_ARG_CONCEPT_IDS,
                FIELD_EVENT_ARG_PREFERRED_NAME,
                FIELD_EVENT_ARG_MATCH_TYPES,
                FIELD_EVENT_ARG_HOMOLOGY_PREFERRED_NAME,
                FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS,
                FIELD_EVENT_ARG_TEXT,
                FIELD_GENE_MAPPING_SOURCE);
        if (numRows == 0)
            serverCmd.fieldsToReturn = Arrays.asList(
                    FIELD_EVENT_ARG_GENE_IDS,
                    FIELD_EVENT_ARG_CONCEPT_IDS,
                    FIELD_EVENT_ARG_PREFERRED_NAME,
                    FIELD_EVENT_ARG_HOMOLOGY_PREFERRED_NAME,
                    FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS,
                    FIELD_EVENT_ARG_TEXT);
        serverCmd.downloadCompleteResults = downloadCompleteResults;
        serverCmd.addSortCommand("_doc", SortOrder.ASCENDING);
        if (numRows != 0) {
            HighlightCommand hlc = new HighlightCommand();
            hlc.addField(FIELD_EVENT_SENTENCE, 10, 0);
            hlc.addField(FIELD_EVENT_PARAGRAPH, 10, 0);
            hlc.fields.forEach(f -> {
                f.pre = "<b>";
                f.post = "</b>";
                TermQuery tq = new TermQuery();
                tq.field = f.field;
                tq.term = "xargumentx";
                f.highlightQuery = tq;
            });
            serverCmd.addHighlightCmd(hlc);
        }
        return serverCmd;
    }


    @Override
    public CompletableFuture<EventRetrievalResult> getFulltextFilteredEvents(List<String> eventTypes, String sentenceFilter, String paragraphFilter, String filterFieldsConnectionOperator, String sectionNameFilterString) {
        log.debug("Returning async result");
        return CompletableFuture.supplyAsync(() -> {
            BoolQuery eventQuery = EventQueries.getFulltextQuery(eventTypes, sentenceFilter, paragraphFilter, filterFieldsConnectionOperator);

            SearchServerRequest serverCmd = new SearchServerRequest();
            serverCmd.query = eventQuery;
            serverCmd.index = documentIndex;
            serverCmd.rows = SCROLL_SIZE;
            serverCmd.fieldsToReturn = Arrays.asList(
                    FIELD_PMID,
                    FIELD_PMCID,
                    FIELD_EVENT_LIKELIHOOD,
                    FIELD_EVENT_SENTENCE,
                    FIELD_EVENT_PARAGRAPH,
                    FIELD_EVENT_MAINEVENTTYPE,
                    FIELD_EVENT_ALL_EVENTTYPES,
                    FIELD_EVENT_ARG_GENE_IDS,
                    FIELD_EVENT_ARG_CONCEPT_IDS,
                    FIELD_EVENT_ARG_PREFERRED_NAME,
                    FIELD_EVENT_ARG_MATCH_TYPES,
                    FIELD_EVENT_ARG_HOMOLOGY_PREFERRED_NAME,
                    FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS,
                    FIELD_EVENT_ARG_TEXT,
                    FIELD_GENE_MAPPING_SOURCE);
            serverCmd.downloadCompleteResults = true;
            serverCmd.addSortCommand("_doc", SortOrder.ASCENDING);

            HighlightCommand hlc = new HighlightCommand();
            hlc.addField(FIELD_EVENT_SENTENCE, 10, 0);
            hlc.addField(FIELD_EVENT_PARAGRAPH, 10, 0);
            hlc.fields.forEach(f -> {
//                f.boundaryChars = new char[]{'\n', '\t'};
//                f.type = HighlightCommand.Highlighter.fastvector;
                f.pre = "<b>";
                f.post = "</b>";
//                MatchQuery hlQuery = new MatchQuery();
//                hlQuery.field = FIELD_EVENT_SENTENCE;
//                hlQuery.query = "xargumentx";
//                f.highlightQuery = hlQuery;
            });
            serverCmd.addHighlightCmd(hlc);


            ElasticSearchCarrier<ElasticServerResponse> carrier = new ElasticSearchCarrier("FulltextFilteredEvents");
            carrier.addSearchServerRequest(serverCmd);
            long time = System.currentTimeMillis();
            searchServerComponent.process(carrier);


            EventRetrievalResult eventResult = eventResponseProcessingService
                    .getEventRetrievalResult(carrier.getSingleSearchServerResponse());
            time = System.currentTimeMillis() - time;
            log.debug("Retrieved {} fulltext-filtered events in {} seconds", eventResult.getEventList().size(), time / 1000);
            eventResult.setResultType(EventResultType.FULLTEXT_FILTERED);
            return eventResult;
        });
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
            // remove events that do not have any other argument than the
            // input ID itself
//            if (e.getArguments().stream().map(a -> a.getTopHomologyId()).distinct().count() < 2) {
//                it.remove();
//                continue;
//            }
            int inputIdPosition = -1;
            for (int i = 0; i < e.getNumArguments(); ++i) {
                Argument g = e.getArgument(i);
                // see also above comment in reorderBipartiteEventResultArguments method:
                // compare via top-homologyId, not geneId; see also #60 and #62
                if (idSet.contains(g.getTopHomologyId()) || idSet.contains(g.getConceptId()) || idSet.contains(g.getGeneId())) {
                    inputIdPosition = i;
                    break;
                }
            }
            if (inputIdPosition == -1)
                throw new IllegalStateException(
                        "An event was returned that does not contain an input argument ID: " + e);
            if (inputIdPosition > 0) {
                List<Argument> arguments = e.getArguments();
                Argument tmp = arguments.get(0);
                arguments.set(0, arguments.get(inputIdPosition));
                arguments.set(inputIdPosition, tmp);
            }
        }
        eventResult.setEvents(reorderedEvents);
    }

}
