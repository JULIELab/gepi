package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.components.data.query.BoolClause;
import de.julielab.elastic.query.components.data.query.BoolQuery;
import de.julielab.elastic.query.components.data.query.SimpleQueryStringQuery;
import de.julielab.elastic.query.components.data.query.TermsQuery;
import de.julielab.gepi.core.retrieval.data.GepiRequestData;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static de.julielab.elastic.query.components.data.query.BoolClause.Occur.FILTER;
import static de.julielab.elastic.query.components.data.query.BoolClause.Occur.SHOULD;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.*;

public class EventQueries {
    public static BoolQuery getClosedQuery(List<String> eventTypes, String sentenceFilter, String paragraphFilter, String sectionNameFilter, Set<String> idSetA, Set<String> idSetB) {
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
        a1b2Clause.occur = BoolClause.Occur.MUST;

        BoolClause a2b1Clause = new BoolClause();
        a2b1Clause.addQuery(listA2Query);
        a2b1Clause.addQuery(listB1Query);
        a2b1Clause.occur = BoolClause.Occur.MUST;

        BoolQuery a1b2Query = new BoolQuery();
        a1b2Query.addClause(a1b2Clause);

        BoolQuery a2b1Query = new BoolQuery();
        a2b1Query.addClause(a2b1Clause);

        BoolClause argClause = new BoolClause();
        argClause.addQuery(a1b2Query);
        argClause.addQuery(a2b1Query);
        argClause.occur = BoolClause.Occur.SHOULD;

        BoolQuery mustQuery = new BoolQuery();
        mustQuery.addClause(argClause);

        BoolClause mustClause = new BoolClause();
        mustClause.addQuery(mustQuery);
        mustClause.occur = BoolClause.Occur.MUST;

        BoolQuery eventQuery = new BoolQuery();
        eventQuery.addClause(mustClause);

        if (eventTypes != null && !eventTypes.isEmpty()) {
            TermsQuery eventTypesQuery = new TermsQuery(new ArrayList<>(eventTypes));
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
        if (!StringUtils.isBlank(sectionNameFilter)) {
            // TODO should vs must should be adapted according to the user input
            addFulltextSearchQuery(sectionNameFilter, FIELD_PARAGRAPH_HEADINGS, FILTER, eventQuery);
        }
        if (filterQuery.clauses != null) {
            BoolClause fulltextFilterClause = new BoolClause();
            fulltextFilterClause.occur = FILTER;
            fulltextFilterClause.addQuery(filterQuery);
            eventQuery.addClause(fulltextFilterClause);
        }
        return eventQuery;
    }

    public static BoolQuery getOpenQuery(GepiRequestData requestData) throws InterruptedException, ExecutionException {
        List<String> eventTypes = requestData.getEventTypes();
        String sentenceFilter = requestData.getSentenceFilterString();
        String paragraphFilter = requestData.getParagraphFilterString();
        String sectionNameFilter = requestData.getSectionNameFilterString();

        TermsQuery termsQuery = new TermsQuery(Collections.unmodifiableCollection(requestData.getAListIdsAsSet()));
        termsQuery.field = FIELD_EVENT_ARGUMENTSEARCH;

        BoolClause termsClause = new BoolClause();
        termsClause.addQuery(termsQuery);
        termsClause.occur = BoolClause.Occur.MUST;

        BoolQuery eventQuery = new BoolQuery();
        eventQuery.addClause(termsClause);

        if (eventTypes != null && !eventTypes.isEmpty()) {
            TermsQuery eventTypesQuery = new TermsQuery(new ArrayList<>(eventTypes));
            eventTypesQuery.field = FIELD_EVENT_MAINEVENTTYPE;
            BoolClause eventTypeClause = new BoolClause();
            eventTypeClause.addQuery(eventTypesQuery);
            eventTypeClause.occur = FILTER;
            eventQuery.addClause(eventTypeClause);
        }

        if (!StringUtils.isBlank(sentenceFilter)) {
            addFulltextSearchQuery(sentenceFilter, FIELD_EVENT_SENTENCE, FILTER, eventQuery);
        }
        if (!StringUtils.isBlank(paragraphFilter)) {
            addFulltextSearchQuery(paragraphFilter, FIELD_EVENT_PARAGRAPH, FILTER, eventQuery);
        }
        if (!StringUtils.isBlank(sectionNameFilter)) {
            addFulltextSearchQuery(sectionNameFilter, FIELD_PARAGRAPH_HEADINGS, FILTER, eventQuery);
        }
        return eventQuery;
    }

    public static BoolQuery getFulltextQuery(List<String> eventTypes, String sentenceFilter, String paragraphFilter, String sectionNameFilter, String filterFieldsConnectionOperator) {
        BoolQuery eventQuery = new BoolQuery();

        if (!eventTypes.isEmpty()) {
            TermsQuery eventTypesQuery = new TermsQuery(new ArrayList<>(eventTypes));
            eventTypesQuery.field = FIELD_EVENT_MAINEVENTTYPE;
            BoolClause eventTypeClause = new BoolClause();
            eventTypeClause.addQuery(eventTypesQuery);
            eventTypeClause.occur = FILTER;
            eventQuery.addClause(eventTypeClause);
        }

        BoolQuery fulltextQuery = new BoolQuery();

        BoolClause.Occur filterFieldsOccur = filterFieldsConnectionOperator.equalsIgnoreCase("and") ? BoolClause.Occur.MUST : BoolClause.Occur.SHOULD;
        if (!StringUtils.isBlank(sentenceFilter)) {
            addFulltextSearchQuery(sentenceFilter, FIELD_EVENT_SENTENCE, filterFieldsOccur, fulltextQuery);
        }
        if (!StringUtils.isBlank(paragraphFilter)) {
            addFulltextSearchQuery(paragraphFilter, FIELD_EVENT_PARAGRAPH, filterFieldsOccur, fulltextQuery);
        }
        if (!StringUtils.isBlank(sectionNameFilter)) {
            addFulltextSearchQuery(sectionNameFilter, FIELD_PARAGRAPH_HEADINGS, filterFieldsOccur, eventQuery);
        }
        BoolClause fulltextClause = new BoolClause();
        fulltextClause.addQuery(fulltextQuery);
        fulltextClause.occur = BoolClause.Occur.MUST;
        eventQuery.addClause(fulltextClause);
        return eventQuery;
    }

    /**
     * <p>Adds a filter clause to the given query that contains a simple query string query.</p>
     * <p>The query allows boolean operators and quotes to mark phrases.</p>
     *
     * @param filterQuery The query string. May contain boolean operators and quoted phrases.
     * @param field       The fulltext field to filter on.
     * @param occur       FILTER, MUST or SHOULD.
     * @param eventQuery  The top event query that is currently constructed.
     */
    private static void addFulltextSearchQuery(String filterQuery, String field, BoolClause.Occur occur, BoolQuery eventQuery) {
        final SimpleQueryStringQuery textFilterQuery = new SimpleQueryStringQuery();
        textFilterQuery.flags = List.of(SimpleQueryStringQuery.Flag.ALL);
        textFilterQuery.query = filterQuery;
        textFilterQuery.fields = Arrays.asList(field);
        final BoolClause textFilterClause = new BoolClause();
        textFilterClause.addQuery(textFilterQuery);
        textFilterClause.occur = occur;
        eventQuery.addClause(textFilterClause);
    }
}
