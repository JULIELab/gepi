package de.julielab.gepi.core.retrieval.services;

import java.util.Collections;
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
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.services.IGeneIdService;

/**
 * Gets any IDs, converts them to GePi IDs (or just queries the index?!) and
 * returns the found relations
 * 
 * @author faessler
 *
 */
public class EventRetrievalService implements IEventRetrievalService {
	
	public static final String FIELD_EVENTS = "events";

	private static final String FIELD_EVENT_MAINEVENTTYPE = FIELD_EVENTS + ".maineventtype";

	private static final String FIELD_EVENT_ALLARGUMENTS = FIELD_EVENTS + ".allarguments";

	private static final String FIELD_EVENT_SENTENCE = FIELD_EVENTS + ".sentence";

	private static final String FIELD_EVENT_LIKELIHOOD  = FIELD_EVENTS + ".likelihood";
	
	private static final String FIELD_EVENT_NUMARGUMENTS  = FIELD_EVENTS + ".numarguments";

	private Logger log;
	private IGeneIdService conversionService;
	private ISearchServerComponent searchServerComponent;

	private String documentIndex;

	public EventRetrievalService(@Symbol(GepiCoreSymbolConstants.INDEX_DOCUMENTS) String documentIndex, Logger log, IGeneIdService conversionService,
			ISearchServerComponent searchServerComponent) {
		this.documentIndex = documentIndex;
		this.log = log;
		this.conversionService = conversionService;
		this.searchServerComponent = searchServerComponent;
	}

	@Override
	public EventRetrievalResult getBipartiteEvents(Stream<String> idStream1, Stream<String> idStream2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EventRetrievalResult getOutsideEvents(Stream<String> idStream) {

//		Stream<String> gepiGeneIds = conversionService.convert2Gepi(idStream);

		TermsQuery termsQuery = new TermsQuery();
		termsQuery.terms = idStream.collect(Collectors.toList());
		termsQuery.field = FIELD_EVENT_ALLARGUMENTS;

		TermQuery filterQuery = new TermQuery();
		filterQuery.term = 2;
		filterQuery.field = FIELD_EVENT_NUMARGUMENTS;

		BoolClause termsClause = new BoolClause();
		termsClause.addQuery(termsQuery);
		termsClause.occur = Occur.MUST;
//		BoolClause filterClause = new BoolClause();
//		filterClause.addQuery(filterQuery);
//		filterClause.occur = Occur.FILTER;

		BoolQuery eventQuery = new BoolQuery();
		eventQuery.addClause(termsClause);
//		eventQuery.addClause(filterClause);
		
		NestedQuery nestedQuery = new NestedQuery();
		nestedQuery.path = FIELD_EVENTS;
		nestedQuery.query = eventQuery;
		nestedQuery.innerHits = new InnerHits();
		nestedQuery.innerHits.addField(FIELD_EVENT_LIKELIHOOD);
		nestedQuery.innerHits.addField(FIELD_EVENT_SENTENCE);
		nestedQuery.innerHits.addField(FIELD_EVENT_ALLARGUMENTS);
		nestedQuery.innerHits.addField(FIELD_EVENT_MAINEVENTTYPE);


		SearchServerCommand serverCmd = new SearchServerCommand();
		serverCmd.query = nestedQuery;
		serverCmd.index = documentIndex;
		serverCmd.rows = 5;
		serverCmd.fieldsToReturn = Collections.emptyList();

		SearchCarrier carrier = new SearchCarrier("OutsideEvents");
		carrier.addSearchServerCommand(serverCmd);
		searchServerComponent.process(carrier);
		
		
		return null;
	}

}
