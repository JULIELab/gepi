package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.components.ISearchServerComponent;
import de.julielab.elastic.query.components.data.ElasticSearchCarrier;
import de.julielab.elastic.query.components.data.ElasticServerResponse;
import de.julielab.elastic.query.components.data.HighlightCommand;
import de.julielab.elastic.query.components.data.SearchServerRequest;
import de.julielab.elastic.query.components.data.SortCommand.SortOrder;
import de.julielab.elastic.query.components.data.query.*;
import de.julielab.elastic.query.components.data.query.BoolClause.Occur;
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

import static de.julielab.elastic.query.components.data.query.BoolClause.Occur.FILTER;
import static de.julielab.elastic.query.components.data.query.BoolClause.Occur.SHOULD;

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

    public static final String FIELD_EVENT_LIKELIHOOD = "likelihood";

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
        CompletableFuture<EventRetrievalResult> esResult;
        EnumSet<InputMode> inputMode = requestData.getInputMode();
        if (inputMode.contains(InputMode.AB)) {
            log.debug("Calling EventRetrievalService for AB search");
            esResult = getBipartiteEvents(requestData.getListAGePiIds(), requestData.getListBGePiIds(), requestData.getEventTypes(), requestData.getSentenceFilterString(), requestData.getParagraphFilterString());
        } else if (inputMode.contains(InputMode.A)) {
            log.debug("Calling EventRetrievalService for A search");
            esResult = getOutsideEvents(requestData.getListAGePiIds(), requestData.getEventTypes(), requestData.getSentenceFilterString(), requestData.getParagraphFilterString());
        } else {
            // No IDs were entered
            log.debug("Calling EventRetrievalService for scope filtered events");
            esResult = getFulltextFilteredEvents(requestData.getEventTypes(), requestData.getSentenceFilterString(), requestData.getParagraphFilterString(), requestData.getFilterFieldsConnectionOperator());
        }
        return esResult;
    }

    @Override
    public CompletableFuture<EventRetrievalResult> getBipartiteEvents(Future<IdConversionResult> idStreamA,
                                                                      Future<IdConversionResult> idStreamB, List<String> eventTypes, String sentenceFilter, String paragraphFilter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Set<String> idSetA = idStreamA.get().getConvertedItems().values().stream().collect(Collectors.toSet());

                Set<String> idSetB = idStreamB.get().getConvertedItems().values().stream().collect(Collectors.toSet());

                log.debug("Retrieving bipartite events for {} A IDs and {} B IDs", idSetA.size(), idSetB.size());

                TermsQuery listA1Query = new TermsQuery(Collections.unmodifiableCollection(idSetA));
                listA1Query.field = FIELD_EVENT_ARGUMENT1SEARCH;
                TermsQuery listA2Query = new TermsQuery(Collections.unmodifiableCollection(idSetA));
                listA2Query.field = FIELD_EVENT_ARGUMENT2SEARCH;

                TermsQuery listB1Query = new TermsQuery(Collections.unmodifiableCollection(idSetB));
                listB1Query.field = FIELD_EVENT_ARGUMENT1SEARCH;
                TermsQuery listB2Query = new TermsQuery(Collections.unmodifiableCollection(idSetB));
                listB2Query.field = FIELD_EVENT_ARGUMENT2SEARCH;

                BoolClause a1b2Clause = new BoolClause();
                a1b2Clause.addQuery(listA1Query);
                a1b2Clause.addQuery(listB2Query);
                a1b2Clause.occur = Occur.MUST;

                BoolClause a2b1Clause = new BoolClause();
                a2b1Clause.addQuery(listA2Query);
                a2b1Clause.addQuery(listB1Query);
                a2b1Clause.occur = Occur.MUST;

                BoolQuery a1b2Query = new BoolQuery();
                a1b2Query.addClause(a1b2Clause);

                BoolQuery a2b1Query = new BoolQuery();
                a2b1Query.addClause(a2b1Clause);

                BoolClause argClause = new BoolClause();
                argClause.addQuery(a1b2Query);
                argClause.addQuery(a2b1Query);
                argClause.occur = Occur.SHOULD;

                BoolQuery mustQuery = new BoolQuery();
                mustQuery.addClause(argClause);

                BoolClause mustClause = new BoolClause();
                mustClause.addQuery(mustQuery);
                mustClause.occur = Occur.MUST;

                BoolQuery eventQuery = new BoolQuery();
                eventQuery.addClause(mustClause);

                if (!eventTypes.isEmpty()) {
                    TermsQuery eventTypesQuery = new TermsQuery(eventTypes.stream().collect(Collectors.toList()));
                    eventTypesQuery.field = FIELD_EVENT_MAINEVENTTYPE;
                    BoolClause eventTypeClause = new BoolClause();
                    eventTypeClause.addQuery(eventTypesQuery);
                    eventTypeClause.occur = FILTER;
                    eventQuery.addClause(eventTypeClause);
                }

                BoolQuery filterQuery = new BoolQuery();
                if (!StringUtils.isBlank(sentenceFilter)) {
                    // TODO should vs must should be adapted according to the user input
                    addFulltextSearchQuery(sentenceFilter, FIELD_EVENT_SENTENCE, SHOULD, filterQuery);
                }
                if (!StringUtils.isBlank(paragraphFilter)) {
                    // TODO should vs must should be adapted according to the user input
                    addFulltextSearchQuery(paragraphFilter, FIELD_EVENT_PARAGRAPH, SHOULD, filterQuery);
                }
                if (filterQuery.clauses != null) {
                    BoolClause fulltextFilterClause = new BoolClause();
                    fulltextFilterClause.occur = FILTER;
                    fulltextFilterClause.addQuery(filterQuery);
                    eventQuery.addClause(fulltextFilterClause);
                }

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
                        FIELD_EVENT_ARG_TEXT);
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

    /**
     * <p>Adds a filter clause to the given query that contains a simple query string query.</p>
     * <p>The query allows boolean operators and quotes to mark phrases.</p>
     *
     * @param filterQuery The query string. May contain boolean operators and quoted phrases.
     * @param field       The fulltext field to filter on.
     * @param occur
     * @param eventQuery  The top event query that is currently constructed.
     */
    private void addFulltextSearchQuery(String filterQuery, String field, Occur occur, BoolQuery eventQuery) {
        final SimpleQueryStringQuery textFilterQuery = new SimpleQueryStringQuery();
        textFilterQuery.flags = List.of(SimpleQueryStringQuery.Flag.ALL);
        textFilterQuery.query = filterQuery;
        textFilterQuery.fields = Arrays.asList(field);
        final BoolClause textFilterClause = new BoolClause();
        textFilterClause.addQuery(textFilterQuery);
        textFilterClause.occur = occur;
        eventQuery.addClause(textFilterClause);
    }

    @Override
    public CompletableFuture<EventRetrievalResult> getBipartiteEvents(IdConversionResult idStream1, IdConversionResult idStream2, List<String> eventTypes, String sentenceFilter, String paragraphFilter) {
        return getBipartiteEvents(CompletableFuture.completedFuture(idStream1), CompletableFuture.completedFuture(idStream2), eventTypes, sentenceFilter, paragraphFilter);
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
    public CompletableFuture<EventRetrievalResult> getOutsideEvents(Future<IdConversionResult> idStreamA, List<String> eventTypes, String sentenceFilter, String paragraphFilter) {
        log.debug("Returning async result");
        return CompletableFuture.supplyAsync(() -> {
            try {
                Set<String> idSet = idStreamA.get().getConvertedItems().values().stream().collect(Collectors.toSet());

                log.debug("Retrieving outside events for {} A IDs", idSet.size());
                log.trace("The A IDs are: {}", idSet);
                SearchServerRequest serverCmd = getOutsideServerRequest(idStreamA, eventTypes, sentenceFilter, paragraphFilter);

                ElasticSearchCarrier<ElasticServerResponse> carrier = new ElasticSearchCarrier("OutsideEvents");
                carrier.addSearchServerRequest(serverCmd);
                long time = System.currentTimeMillis();
                searchServerComponent.process(carrier);


                EventRetrievalResult eventResult = eventResponseProcessingService
                        .getEventRetrievalResult(carrier.getSingleSearchServerResponse());
                time = System.currentTimeMillis() - time;
                log.debug("Retrieved {} outside events from ElasticSearch in {} seconds", eventResult.getEventList().size(), time / 1000);
                eventResult.setResultType(EventResultType.OUTSIDE);
                reorderOutsideEventResultsArguments(idSet, eventResult);
                return eventResult;
            } catch (InterruptedException | ExecutionException e) {
                log.error("Could not retrieve the IDs for the query", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<EventRetrievalResult> getOutsideEvents(IdConversionResult idStream, List<String> eventTypes, String sentenceFilter, String paragraphFilter) {
        return getOutsideEvents(CompletableFuture.completedFuture(idStream), eventTypes, sentenceFilter, paragraphFilter);
    }

    @Override
    public SearchServerRequest getOutsideServerRequest(Future<IdConversionResult> idStreamA, List<String> eventTypes, String sentenceFilter, String paragraphFilter) throws ExecutionException, InterruptedException {
        TermsQuery termsQuery = new TermsQuery(Collections.unmodifiableCollection(idStreamA.get().getConvertedItems().values().stream().collect(Collectors.toSet())));
        termsQuery.field = FIELD_EVENT_ARGUMENTSEARCH;

        BoolClause termsClause = new BoolClause();
        termsClause.addQuery(termsQuery);
        termsClause.occur = Occur.MUST;

        BoolQuery eventQuery = new BoolQuery();
        eventQuery.addClause(termsClause);

        if (!eventTypes.isEmpty()) {
            TermsQuery eventTypesQuery = new TermsQuery(eventTypes.stream().collect(Collectors.toList()));
            eventTypesQuery.field = FIELD_EVENT_MAINEVENTTYPE;
            BoolClause eventTypeClause = new BoolClause();
            eventTypeClause.addQuery(eventTypesQuery);
            eventTypeClause.occur = FILTER;
            eventQuery.addClause(eventTypeClause);
        }

        if (!StringUtils.isBlank(sentenceFilter)) {
            addFulltextSearchQuery(sentenceFilter, FIELD_EVENT_SENTENCE, Occur.FILTER, eventQuery);
        }
        if (!StringUtils.isBlank(paragraphFilter)) {
            addFulltextSearchQuery(paragraphFilter, FIELD_EVENT_PARAGRAPH, Occur.FILTER, eventQuery);
        }


        SearchServerRequest serverCmd = new SearchServerRequest();
        serverCmd.query = eventQuery;
        serverCmd.index = documentIndex;
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
                FIELD_EVENT_ARG_TEXT);
        serverCmd.downloadCompleteResults = true;
        serverCmd.addSortCommand("_doc", SortOrder.ASCENDING);
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
        return serverCmd;
    }

    @Override
    public CompletableFuture<EventRetrievalResult> getFulltextFilteredEvents(List<String> eventTypes, String sentenceFilter, String paragraphFilter, String filterFieldsConnectionOperator) {
        log.debug("Returning async result");
        return CompletableFuture.supplyAsync(() -> {
            BoolQuery eventQuery = new BoolQuery();

            if (!eventTypes.isEmpty()) {
                TermsQuery eventTypesQuery = new TermsQuery(eventTypes.stream().collect(Collectors.toList()));
                eventTypesQuery.field = FIELD_EVENT_MAINEVENTTYPE;
                BoolClause eventTypeClause = new BoolClause();
                eventTypeClause.addQuery(eventTypesQuery);
                eventTypeClause.occur = FILTER;
                eventQuery.addClause(eventTypeClause);
            }

            BoolQuery fulltextQuery = new BoolQuery();

            Occur filterFieldsOccur = filterFieldsConnectionOperator.equalsIgnoreCase("and") ? Occur.MUST : Occur.SHOULD;
            if (!StringUtils.isBlank(sentenceFilter)) {
                addFulltextSearchQuery(sentenceFilter, FIELD_EVENT_SENTENCE, filterFieldsOccur, fulltextQuery);
            }
            if (!StringUtils.isBlank(paragraphFilter)) {
                addFulltextSearchQuery(paragraphFilter, FIELD_EVENT_PARAGRAPH, filterFieldsOccur, fulltextQuery);
            }

            BoolClause fulltextClause = new BoolClause();
            fulltextClause.addQuery(fulltextQuery);
            fulltextClause.occur = Occur.MUST;
            eventQuery.addClause(fulltextClause);

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
                    FIELD_EVENT_ARG_TEXT);
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
            if (e.getArguments().stream().map(a -> a.getTopHomologyId()).distinct().count() < 2) {
                it.remove();
                continue;
            }
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
