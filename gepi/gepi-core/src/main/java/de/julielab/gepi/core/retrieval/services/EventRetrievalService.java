package de.julielab.gepi.core.retrieval.services;

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
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult.EventResultType;
import de.julielab.gepi.core.retrieval.data.Argument;

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

	public static final String FIELD_EVENT_ARGUMENTSEARCH = FIELD_EVENTS + ".argumentsearch";

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
	public CompletableFuture<EventRetrievalResult> getBipartiteEvents(Stream<String> idStream1,
			Stream<String> idStream2) {
		TermsQuery listAQuery = new TermsQuery();
		listAQuery.terms = idStream1.collect(Collectors.toList());
		listAQuery.field = FIELD_EVENT_ARGUMENTSEARCH;

		TermsQuery listBQuery = new TermsQuery();
		listBQuery.terms = idStream1.collect(Collectors.toList());
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
		nestedQuery.innerHits.addField(FIELD_EVENT_ARGUMENTSEARCH);
		nestedQuery.innerHits.addField(FIELD_EVENT_MAINEVENTTYPE);

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
			// try {
			// Thread.sleep(2000);
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

			EventRetrievalResult eventResult = eventResponseProcessingService
					.getEventRetrievalResult(carrier.getSingleSearchServerResponse());
			eventResult.setResultType(EventResultType.BIPARTITE);
			return eventResult;
		});
	}

	@Override
	public CompletableFuture<EventRetrievalResult> getOutsideEvents(Stream<String> idStream) {

		// Stream<String> gepiGeneIds =
		// conversionService.convert2Gepi(idStream);
		List<Object> idList = idStream.collect(Collectors.toList());
		Set<String> idSet = idList.stream().map(String.class::cast).collect(Collectors.toSet());

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
			// try {
			// Thread.sleep(2000);
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			//
			EventRetrievalResult eventResult = eventResponseProcessingService
					.getEventRetrievalResult(carrier.getSingleSearchServerResponse());
			eventResult.setResultType(EventResultType.OUTSIDE);
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
					// TODO support other IDs
					if (idSet.contains(g.getGeneId())) {
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
			return eventResult;
		});
	}

}
