package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.components.ISearchServerComponent;
import de.julielab.elastic.query.components.data.ElasticSearchCarrier;
import de.julielab.elastic.query.components.data.ElasticServerResponse;
import de.julielab.elastic.query.components.data.SearchServerRequest;
import de.julielab.elastic.query.components.data.SortCommand.SortOrder;
import de.julielab.elastic.query.components.data.query.*;
import de.julielab.elastic.query.components.data.query.BoolClause.Occur;
import de.julielab.gepi.core.GepiCoreSymbolConstants;
import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult.EventResultType;
import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.julielab.elastic.query.components.data.query.BoolClause.Occur.FILTER;

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

    public static final String FIELD_EVENT_ARGUMENTSEARCH = "allarguments";

    public static final String FIELD_EVENT_ARG_GENE_IDS = "allargumentgeneids";

    public static final String FIELD_EVENT_ARG_CONCEPT_IDS = "allargumentconceptids";

    public static final String FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS = "allargumenttophomoids";

    public static final String FIELD_EVENT_ARG_TEXT = "allargumentcoveredtext";

    public static final String FIELD_EVENT_ARG_PREFERRED_NAME = "allargumentprefnames";

    public static final String FIELD_EVENT_ARG_HOMOLOGY_PREFERRED_NAME = "allargumenthomoprefnames";

    public static final String FIELD_EVENT_SENTENCE = "sentence.text";

    public static final String FIELD_EVENT_LIKELIHOOD = "likelihood";

    public static final String FIELD_EVENT_NUMARGUMENTS = "numargs";
    private static final int SCROLL_SIZE = 100;
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

    /**
     * BROKEN AS OF OCTOBER 2018 DUE TO INDEX REBUILD; COPY QUERY PART FROM OUTSIDE EVENTS AND ADAPT
     *
     * @param idStreamA
     * @param idStreamB
     * @return
     */
    @Override
    public CompletableFuture<EventRetrievalResult> getBipartiteEvents(Future<Stream<String>> idStreamA,
                                                                      Future<Stream<String>> idStreamB, List<String> eventTypes, String sentenceFilter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Object> idListA = idStreamA.get().collect(Collectors.toList());
                Set<String> idSetA = idListA.stream().map(String.class::cast).collect(Collectors.toSet());

                List<Object> idListB = idStreamB.get().collect(Collectors.toList());
                Set<String> idSetB = idListB.stream().map(String.class::cast).collect(Collectors.toSet());

                log.debug("Retrieving bipartite events for {} A IDs and {} B IDs", idListA.size(), idListB.size());

                TermsQuery listAQuery = new TermsQuery(idListA);
                listAQuery.field = FIELD_EVENT_ARGUMENTSEARCH;

                TermsQuery listBQuery = new TermsQuery(idListB);
                listBQuery.field = FIELD_EVENT_ARGUMENTSEARCH;

                TermQuery filterQuery = new TermQuery();
                filterQuery.term = 2;
                filterQuery.field = FIELD_EVENT_NUMARGUMENTS;

                BoolClause listAClause = new BoolClause();
                listAClause.addQuery(listAQuery);
                listAClause.occur = Occur.MUST;

                BoolClause listBClause = new BoolClause();
                listBClause.addQuery(listBQuery);
                listBClause.occur = Occur.MUST;

                BoolClause filterClause = new BoolClause();
                filterClause.addQuery(filterQuery);
                filterClause.occur = FILTER;

                BoolQuery eventQuery = new BoolQuery();
                eventQuery.addClause(listAClause);
                eventQuery.addClause(listBClause);
                eventQuery.addClause(filterClause);

                if (!eventTypes.isEmpty()) {
                    TermsQuery eventTypesQuery = new TermsQuery(eventTypes.stream().collect(Collectors.toList()));
                    eventTypesQuery.field = FIELD_EVENT_MAINEVENTTYPE;
                    BoolClause eventTypeClause = new BoolClause();
                    eventTypeClause.addQuery(eventTypesQuery);
                    eventTypeClause.occur = FILTER;
                    eventQuery.addClause(eventTypeClause);
                }

                if (!StringUtils.isBlank(sentenceFilter)) {
                    final SimpleQueryStringQuery sentenceFilterQuery = new SimpleQueryStringQuery();
                    sentenceFilterQuery.query = sentenceFilter;
                    sentenceFilterQuery.fields = Arrays.asList(FIELD_EVENT_SENTENCE);
                    sentenceFilterQuery.flags = Arrays.asList(SimpleQueryStringQuery.Flag.AND, SimpleQueryStringQuery.Flag.NOT, SimpleQueryStringQuery.Flag.OR);
                    final BoolClause sentenceFilterClause = new BoolClause();
                    sentenceFilterClause.addQuery(sentenceFilterQuery);
                    sentenceFilterClause.occur = FILTER;
                    eventQuery.addClause(sentenceFilterClause);
                }

                SearchServerRequest serverCmd = new SearchServerRequest();
                serverCmd.query = eventQuery;
                serverCmd.index = documentIndex;
                serverCmd.indexTypes = Arrays.asList("relations");
                serverCmd.rows = SCROLL_SIZE;
                serverCmd.fieldsToReturn = Collections.emptyList();
                serverCmd.fieldsToReturn = Arrays.asList(
                        FIELD_PMID,
                        FIELD_PMCID,
                        FIELD_EVENT_LIKELIHOOD,
                        FIELD_EVENT_SENTENCE,
                        FIELD_EVENT_MAINEVENTTYPE,
                        FIELD_EVENT_ARG_GENE_IDS,
                        FIELD_EVENT_ARG_CONCEPT_IDS,
                        FIELD_EVENT_ARG_PREFERRED_NAME,
                        FIELD_EVENT_ARG_HOMOLOGY_PREFERRED_NAME,
                        FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS,
                        FIELD_EVENT_ARG_TEXT,
                        FIELD_EVENT_NUMARGUMENTS);
                serverCmd.downloadCompleteResults = true;
                serverCmd.addSortCommand("_doc", SortOrder.ASCENDING);

                ElasticSearchCarrier<ElasticServerResponse> carrier = new ElasticSearchCarrier<>("BipartiteEvents");
                carrier.addSearchServerRequest(serverCmd);
                searchServerComponent.process(carrier);


                EventRetrievalResult eventResult = eventResponseProcessingService
                        .getEventRetrievalResult(carrier.getSingleSearchServerResponse());
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
    public CompletableFuture<EventRetrievalResult> getBipartiteEvents(Stream<String> idStream1, Stream<String> idStream2, List<String> eventTypes, String sentenceFilter) {
        return getBipartiteEvents(CompletableFuture.completedFuture(idStream1), CompletableFuture.completedFuture(idStream2), eventTypes, sentenceFilter);
    }

    /**
     * Reorders the arguments of the events to make the first argument correspond to
     * the A ID list and the second argument to the B ID list. Also adds new events
     * in case of more than two ID hits in the same so we can handle all results as
     * binary events.
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

        // It might happen that an event has more than two gene arguments.
        // Thus, it might also happen, that more than two input IDs are
        // found in the event. However, GePi currently only handles binary
        // events. Hence, we split larger events into pairs of arguments if
        // necessary.
        List<Event> extendedResults = new ArrayList<>();
        for (Iterator<Event> it = eventResult.getEventList().iterator(); it.hasNext(); ) {
            Event e = it.next();
            List<Integer> idAHits = new ArrayList<>();
            List<Integer> idBHits = new ArrayList<>();
            for (int i = 0; i < e.getNumArguments(); ++i) {
                Argument g = e.getArgument(i);
                // As we expand given ids to top-homology ids we need to compare to those
                // not the input genes, e.g. g.geneId(); see also #60 and #62
                if (idSetA.contains(g.getTopHomologyId()) || idSetA.contains(g.getConceptId()) || idSetA.contains(g.getGeneId())) {
                    idAHits.add(i);
                }
                if (idSetB.contains(g.getTopHomologyId()) || idSetB.contains(g.getConceptId()) || idSetB.contains(g.getGeneId())) {
                    idBHits.add(i);
                }
            }
            if (idAHits.size() + idBHits.size() < 2)
                throw new IllegalStateException(
                        "An event was returned that does not contain one of the input argument IDs: " + e);
            // create events for all pairs of indices containing an input
            // ID, if the IDs at those indices are different
            List<Argument> originalArguments = new ArrayList<>(e.getArguments());
            for (int i = 0; i < idAHits.size(); ++i) {
                for (int j = 0; j < idBHits.size(); ++j) {
                    // for the first combination, use the original event
                    // object; for all others, we need a copy
                    Event event;
                    List<Argument> arguments;
                    if (i == 0 && j == 0) {
                        event = e;
                        arguments = e.getArguments();
                    } else {
                        event = e.copy();
                        extendedResults.add(event);
                        arguments = new ArrayList<>(originalArguments);
                    }
                    // arrange the arguments according to the positions of
                    // listAId and listBId
                    int inputAIdPosition = idAHits.get(i);
                    int inputBIdPosition = idBHits.get(j);
                    if (inputAIdPosition > 0)
                        Collections.swap(arguments, 0, inputAIdPosition);
                    if (inputBIdPosition > 1)
                        Collections.swap(arguments, 1, inputBIdPosition);
                }
            }
        }
        eventResult.getEventList().addAll(extendedResults);
    }

    @Override
    public CompletableFuture<EventRetrievalResult> getOutsideEvents(Future<Stream<String>> idStream, List<String> eventTypes, String sentenceFilter) {
        log.debug("Returning async result");
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Object> idList = idStream.get().collect(Collectors.toList());
                Set<String> idSet = idList.stream().map(String.class::cast).collect(Collectors.toSet());

                log.debug("Retrieving outside events for {} A IDs", idList.size());
                log.trace("The A IDs are: {}", idList);
                TermsQuery termsQuery = new TermsQuery(idList);
                termsQuery.field = FIELD_EVENT_ARGUMENTSEARCH;

                TermQuery filterQuery = new TermQuery();
                filterQuery.term = 2;
                filterQuery.field = FIELD_EVENT_NUMARGUMENTS;

                BoolClause termsClause = new BoolClause();
                termsClause.addQuery(termsQuery);
                termsClause.occur = Occur.MUST;
                BoolClause filterClause = new BoolClause();
                filterClause.addQuery(filterQuery);
                filterClause.occur = FILTER;

                BoolQuery eventQuery = new BoolQuery();
                eventQuery.addClause(termsClause);
                eventQuery.addClause(filterClause);

                if (!eventTypes.isEmpty()) {
                    TermsQuery eventTypesQuery = new TermsQuery(eventTypes.stream().collect(Collectors.toList()));
                    eventTypesQuery.field = FIELD_EVENT_MAINEVENTTYPE;
                    BoolClause eventTypeClause = new BoolClause();
                    eventTypeClause.addQuery(eventTypesQuery);
                    eventTypeClause.occur = FILTER;
                    eventQuery.addClause(eventTypeClause);
                }

                if (!StringUtils.isBlank(sentenceFilter)) {
                    final SimpleQueryStringQuery sentenceFilterQuery = new SimpleQueryStringQuery();
                    sentenceFilterQuery.query = sentenceFilter;
                    sentenceFilterQuery.fields = Arrays.asList(FIELD_EVENT_SENTENCE);
                    sentenceFilterQuery.flags = Arrays.asList(SimpleQueryStringQuery.Flag.AND, SimpleQueryStringQuery.Flag.NOT, SimpleQueryStringQuery.Flag.OR);
                    final BoolClause sentenceFilterClause = new BoolClause();
                    sentenceFilterClause.addQuery(sentenceFilterQuery);
                    sentenceFilterClause.occur = FILTER;
                    eventQuery.addClause(sentenceFilterClause);
                }


                SearchServerRequest serverCmd = new SearchServerRequest();
                serverCmd.query = eventQuery;
                serverCmd.index = documentIndex;
                serverCmd.indexTypes = Arrays.asList("relations");
                serverCmd.rows = SCROLL_SIZE;
                serverCmd.fieldsToReturn = Arrays.asList(
                        FIELD_PMID,
                        FIELD_PMCID,
                        FIELD_EVENT_LIKELIHOOD,
                        FIELD_EVENT_SENTENCE,
                        FIELD_EVENT_MAINEVENTTYPE,
                        FIELD_EVENT_ARG_GENE_IDS,
                        FIELD_EVENT_ARG_CONCEPT_IDS,
                        FIELD_EVENT_ARG_PREFERRED_NAME,
                        FIELD_EVENT_ARG_HOMOLOGY_PREFERRED_NAME,
                        FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS,
                        FIELD_EVENT_ARG_TEXT,
                        FIELD_EVENT_NUMARGUMENTS);
                serverCmd.downloadCompleteResults = true;
                serverCmd.addSortCommand("_doc", SortOrder.ASCENDING);

                ElasticSearchCarrier<ElasticServerResponse> carrier = new ElasticSearchCarrier("OutsideEvents");
                carrier.addSearchServerRequest(serverCmd);
                searchServerComponent.process(carrier);


                EventRetrievalResult eventResult = eventResponseProcessingService
                        .getEventRetrievalResult(carrier.getSingleSearchServerResponse());
                eventResult.setResultType(EventResultType.OUTSIDE);
                reorderOutsideEventResultsArguments(idSet, eventResult);
                return eventResult;
            } catch (InterruptedException | ExecutionException e) {
                log.error("Could not retrieve the IDs for the query", e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<EventRetrievalResult> getOutsideEvents(Stream<String> idStream, List<String> eventTypes, String sentenceFilter) {
        return getOutsideEvents(CompletableFuture.completedFuture(idStream), eventTypes, sentenceFilter);
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

        for (Iterator<Event> it = eventResult.getEventList().iterator(); it.hasNext(); ) {
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
    }

}
