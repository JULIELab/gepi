package de.julielab.gepi.core.retrieval.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tapestry5.ioc.annotations.Symbol;
import org.slf4j.Logger;

import de.julielab.elastic.query.components.ISearchServerComponent;
import de.julielab.elastic.query.components.data.SearchCarrier;
import de.julielab.elastic.query.components.data.SearchServerCommand;
import de.julielab.elastic.query.components.data.query.BoolClause;
import de.julielab.elastic.query.components.data.query.BoolClause.Occur;
import de.julielab.elastic.query.components.data.query.BoolQuery;
import de.julielab.elastic.query.components.data.query.InnerHits;
import de.julielab.elastic.query.components.data.query.NestedQuery;
import de.julielab.elastic.query.components.data.query.TermQuery;
import de.julielab.elastic.query.components.data.query.TermsQuery;
import de.julielab.gepi.core.GepiCoreSymbolConstants;
import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult.EventResultType;

/**
 * Gets any IDs, converts them to GePi IDs (or just queries the index?!) and
 * returns the found relations
 * 
 * @author faessler
 *
 */
public class EventRetrievalService implements IEventRetrievalService {

	public static final String FIELD_EVENTS = "events";

	public static final String FIELD_EVENT_MAINEVENTTYPE = FIELD_EVENTS + ".maineventtype";

	public static final String FIELD_EVENT_ARGUMENTSEARCH = FIELD_EVENTS + ".argumenttophomologyids";

	public static final String FIELD_EVENT_ARG_GENE_IDS = FIELD_EVENTS + ".argumentgeneids";

	public static final String FIELD_EVENT_ARG_CONCEPT_IDS = FIELD_EVENTS + ".argumentconceptids";

	public static final String FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS = FIELD_EVENTS + ".argumenttophomologyids";

	public static final String FIELD_EVENT_ARG_TEXT = FIELD_EVENTS + ".argumenttext";

	public static final String FIELD_EVENT_ARG_PREFERRED_NAME = FIELD_EVENTS + ".argumentpreferrednames";

	public static final String FIELD_EVENT_SENTENCE = FIELD_EVENTS + ".sentence";

	public static final String FIELD_EVENT_LIKELIHOOD = FIELD_EVENTS + ".likelihood";

	public static final String FIELD_EVENT_NUMDISTINCTARGUMENTS = FIELD_EVENTS + ".numdistinctarguments";

	public static final String FIELD_EVENT_NUMARGUMENTS = FIELD_EVENTS + ".numarguments";

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
	public CompletableFuture<EventRetrievalResult> getBipartiteEvents(Stream<String> idStreamA,
			Stream<String> idStreamB) {
		List<Object> idListA = idStreamA.collect(Collectors.toList());
		Set<String> idSetA = idListA.stream().map(String.class::cast).collect(Collectors.toSet());

		List<Object> idListB = idStreamB.collect(Collectors.toList());
		Set<String> idSetB = idListB.stream().map(String.class::cast).collect(Collectors.toSet());

		log.debug("Retrieving bipartite events for {} A IDs and {} B IDs", idListA.size(), idListB.size());

		TermsQuery listAQuery = new TermsQuery();
		listAQuery.terms = idListA;
		listAQuery.field = FIELD_EVENT_ARGUMENTSEARCH;

		TermsQuery listBQuery = new TermsQuery();
		listBQuery.terms = idListB;
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
		filterClause.occur = Occur.FILTER;

		BoolQuery eventQuery = new BoolQuery();
		eventQuery.addClause(listAClause);
		eventQuery.addClause(listBClause);
		eventQuery.addClause(filterClause);

		NestedQuery nestedQuery = new NestedQuery();
		nestedQuery.path = FIELD_EVENTS;
		nestedQuery.query = eventQuery;
		nestedQuery.innerHits = new InnerHits();
		nestedQuery.innerHits.addField(FIELD_EVENT_LIKELIHOOD);
		nestedQuery.innerHits.addField(FIELD_EVENT_SENTENCE);
		nestedQuery.innerHits.addField(FIELD_EVENT_MAINEVENTTYPE);
		nestedQuery.innerHits.addField(FIELD_EVENT_ARGUMENTSEARCH);
		nestedQuery.innerHits.addField(FIELD_EVENT_ARG_CONCEPT_IDS);
		nestedQuery.innerHits.addField(FIELD_EVENT_ARG_GENE_IDS);
		nestedQuery.innerHits.addField(FIELD_EVENT_ARG_PREFERRED_NAME);
		nestedQuery.innerHits.addField(FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS);
		nestedQuery.innerHits.addField(FIELD_EVENT_ARG_TEXT);
		nestedQuery.innerHits.addField(FIELD_EVENT_NUMARGUMENTS);
		nestedQuery.innerHits.addField(FIELD_EVENT_NUMDISTINCTARGUMENTS);

		SearchServerCommand serverCmd = new SearchServerCommand();
		serverCmd.query = nestedQuery;
		serverCmd.index = documentIndex;
		serverCmd.rows = 5;
		serverCmd.fieldsToReturn = Collections.emptyList();
		serverCmd.downloadCompleteResults = true;

		SearchCarrier carrier = new SearchCarrier("BipartiteEvents");
		carrier.addSearchServerCommand(serverCmd);
		searchServerComponent.process(carrier);

		return CompletableFuture.supplyAsync(() -> {

			EventRetrievalResult eventResult = eventResponseProcessingService
					.getEventRetrievalResult(carrier.getSingleSearchServerResponse());
			eventResult.setResultType(EventResultType.BIPARTITE);
			reorderBipartiteEventResultArguments(idSetA, idSetB, eventResult);
			return eventResult;
		});
	}

	/**
	 * Reorders the arguments of the events to make the first argument
	 * correspond to the A ID list and the second argument to the B ID list.
	 * Also adds new events in case of more than two ID hits in the same so we
	 * can handle all results as binary events.
	 * 
	 * @param idSetA
	 *            The set of list A query IDs.
	 * @param idSetB
	 *            The set of list B query IDs.
	 * @param eventResult
	 *            The event result as returned by the
	 *            {@link EventResponseProcessingService}.
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
		for (Iterator<Event> it = eventResult.getEventList().iterator(); it.hasNext();) {
			Event e = it.next();
			List<Integer> idAHits = new ArrayList<>();
			List<Integer> idBHits = new ArrayList<>();
			for (int i = 0; i < e.getNumArguments(); ++i) {
				Argument g = e.getArgument(i);
				// As we expand given ids to top-homology ids we need to compare to those
				// not the input genes, e.g. g.geneId(); see also #60 and #62
				// TODO support other IDs
				if (idSetA.contains(g.getTopHomologyId())) {
					idAHits.add(i);
				}
				if (idSetB.contains(g.getTopHomologyId())) {
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
	public CompletableFuture<EventRetrievalResult> getOutsideEvents(Stream<String> idStream) {

		// Stream<String> gepiGeneIds =
		// conversionService.convert2Gepi(idStream);
		List<Object> idList = idStream.collect(Collectors.toList());
		Set<String> idSet = idList.stream().map(String.class::cast).collect(Collectors.toSet());

		log.debug("Retrieving outside events for {} A IDs", idList.size());
		
		TermsQuery termsQuery = new TermsQuery();
		termsQuery.terms = idList;
		termsQuery.field = FIELD_EVENT_ARGUMENTSEARCH;

		TermQuery filterQuery = new TermQuery();
		filterQuery.term = 2;
		filterQuery.field = FIELD_EVENT_NUMARGUMENTS;

		BoolClause termsClause = new BoolClause();
		termsClause.addQuery(termsQuery);
		termsClause.occur = Occur.MUST;
		BoolClause filterClause = new BoolClause();
		filterClause.addQuery(filterQuery);
		filterClause.occur = Occur.FILTER;

		BoolQuery eventQuery = new BoolQuery();
		eventQuery.addClause(termsClause);
		eventQuery.addClause(filterClause);

		NestedQuery nestedQuery = new NestedQuery();
		nestedQuery.path = FIELD_EVENTS;
		nestedQuery.query = eventQuery;
		nestedQuery.innerHits = new InnerHits();
		nestedQuery.innerHits.addField(FIELD_EVENT_LIKELIHOOD);
		nestedQuery.innerHits.addField(FIELD_EVENT_SENTENCE);
		nestedQuery.innerHits.addField(FIELD_EVENT_MAINEVENTTYPE);
		nestedQuery.innerHits.addField(FIELD_EVENT_ARGUMENTSEARCH);
		nestedQuery.innerHits.addField(FIELD_EVENT_ARG_CONCEPT_IDS);
		nestedQuery.innerHits.addField(FIELD_EVENT_ARG_GENE_IDS);
		nestedQuery.innerHits.addField(FIELD_EVENT_ARG_PREFERRED_NAME);
		nestedQuery.innerHits.addField(FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS);
		nestedQuery.innerHits.addField(FIELD_EVENT_ARG_TEXT);
		nestedQuery.innerHits.addField(FIELD_EVENT_NUMARGUMENTS);
		nestedQuery.innerHits.addField(FIELD_EVENT_NUMDISTINCTARGUMENTS);

		SearchServerCommand serverCmd = new SearchServerCommand();
		serverCmd.query = nestedQuery;
		serverCmd.index = documentIndex;
		serverCmd.rows = 5;
		serverCmd.fieldsToReturn = Collections.emptyList();
		serverCmd.downloadCompleteResults = true;

		SearchCarrier carrier = new SearchCarrier("OutsideEvents");
		carrier.addSearchServerCommand(serverCmd);
		searchServerComponent.process(carrier);

		return CompletableFuture.supplyAsync(() -> {
			EventRetrievalResult eventResult = eventResponseProcessingService
					.getEventRetrievalResult(carrier.getSingleSearchServerResponse());
			eventResult.setResultType(EventResultType.OUTSIDE);
			reorderOutsideEventResultsArguments(idSet, eventResult);
			return eventResult;
		});
	}

	/**
	 * Reorder the arguments of the result events such that the first argument
	 * always corresponds to an ID in the query ID list.
	 * 
	 * @param idSet
	 *            The set of query IDs.
	 * @param eventResult
	 *            The event result as returned by
	 *            {@link EventResponseProcessingService}.
	 */
	private void reorderOutsideEventResultsArguments(Set<String> idSet, EventRetrievalResult eventResult) {
		// reorder all arguments such that the first argument corresponds to
		// the input ID that caused the match

		for (Iterator<Event> it = eventResult.getEventList().iterator(); it.hasNext();) {
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
				// TODO support other IDs
				if (idSet.contains(g.getTopHomologyId())) {
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
