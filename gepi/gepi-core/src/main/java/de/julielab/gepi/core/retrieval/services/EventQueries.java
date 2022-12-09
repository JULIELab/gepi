package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.components.data.query.*;
import de.julielab.gepi.core.retrieval.data.GepiRequestData;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static de.julielab.elastic.query.components.data.query.BoolClause.Occur.*;
import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.*;

public class EventQueries {
    public static BoolQuery getClosedQuery(GepiRequestData requestData, Set<String> idSetA, Set<String> idSetB) {
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
        a1b2Clause.occur = MUST;

        BoolClause a2b1Clause = new BoolClause();
        a2b1Clause.addQuery(listA2Query);
        a2b1Clause.addQuery(listB1Query);
        a2b1Clause.occur = MUST;

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
        mustClause.occur = MUST;

        BoolQuery eventQuery = new BoolQuery();
        eventQuery.addClause(mustClause);

        if (requestData.getEventTypes() != null && !requestData.getEventTypes().isEmpty()) {
            TermsQuery eventTypesQuery = new TermsQuery(new ArrayList<>(requestData.getEventTypes()));
            eventTypesQuery.field = FIELD_EVENT_ALL_EVENTTYPES;
            BoolClause eventTypeClause = new BoolClause();
            eventTypeClause.addQuery(eventTypesQuery);
            eventTypeClause.occur = FILTER;
            eventQuery.addClause(eventTypeClause);
        }

        BoolQuery filterQuery = new BoolQuery();
        BoolClause.Occur sentenceParagraphOccur = requestData.getFilterFieldsConnectionOperator().equalsIgnoreCase("and") ? MUST : SHOULD;
        if (!StringUtils.isBlank(requestData.getSentenceFilterString())) {
            addFulltextSearchQuery(requestData.getSentenceFilterString(), FIELD_EVENT_SENTENCE_TEXT, sentenceParagraphOccur, filterQuery);
        }
        if (!StringUtils.isBlank(requestData.getParagraphFilterString())) {
            addFulltextSearchQuery(requestData.getParagraphFilterString(), FIELD_EVENT_PARAGRAPH_TEXT, sentenceParagraphOccur, filterQuery);
        }
        if (!StringUtils.isBlank(requestData.getSectionNameFilterString())) {
            addFulltextSearchQuery(requestData.getSectionNameFilterString(), FIELD_PARAGRAPH_HEADINGS, FILTER, eventQuery);
        }
        if (filterQuery.clauses != null) {
            BoolClause fulltextFilterClause = new BoolClause();
            fulltextFilterClause.occur = FILTER;
            fulltextFilterClause.addQuery(filterQuery);
            eventQuery.addClause(fulltextFilterClause);
        }
        if (requestData.getEventLikelihood() > 1)
            addEventLikelihoodFilter(eventQuery, requestData.getEventLikelihood());
        if (requestData.getTaxId() != null && requestData.getTaxId().length > 0) {
            final TermsQuery taxQuery = new TermsQuery(Arrays.stream(requestData.getTaxId()).collect(Collectors.toList()));
            taxQuery.field = FIELD_EVENT_TAX_IDS;
            BoolClause taxIdFilterClause = new BoolClause();
            taxIdFilterClause.occur = FILTER;
            taxIdFilterClause.addQuery(taxQuery);
            eventQuery.addClause(taxIdFilterClause);
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
        termsClause.occur = MUST;

        BoolQuery eventQuery = new BoolQuery();
        eventQuery.addClause(termsClause);

        if (eventTypes != null && !eventTypes.isEmpty()) {
            TermsQuery eventTypesQuery = new TermsQuery(new ArrayList<>(eventTypes));
            eventTypesQuery.field = FIELD_EVENT_ALL_EVENTTYPES;
            BoolClause eventTypeClause = new BoolClause();
            eventTypeClause.addQuery(eventTypesQuery);
            eventTypeClause.occur = FILTER;
            eventQuery.addClause(eventTypeClause);
        }

        BoolQuery filterQuery = new BoolQuery();
        BoolClause.Occur sentenceParagraphOccur = requestData.getFilterFieldsConnectionOperator().equalsIgnoreCase("and") ? MUST : SHOULD;
        if (!StringUtils.isBlank(sentenceFilter)) {
            addFulltextSearchQuery(sentenceFilter, FIELD_EVENT_SENTENCE_TEXT, sentenceParagraphOccur, filterQuery);
        }
        if (!StringUtils.isBlank(paragraphFilter)) {
            addFulltextSearchQuery(paragraphFilter, FIELD_EVENT_PARAGRAPH_TEXT, sentenceParagraphOccur, filterQuery);
        }
        if (!StringUtils.isBlank(sectionNameFilter)) {
            addFulltextSearchQuery(sectionNameFilter, FIELD_PARAGRAPH_HEADINGS, FILTER, eventQuery);
        }

        if (filterQuery.clauses != null) {
            BoolClause fulltextFilterClause = new BoolClause();
            fulltextFilterClause.occur = FILTER;
            fulltextFilterClause.addQuery(filterQuery);
            eventQuery.addClause(fulltextFilterClause);
        }
        if (requestData.getEventLikelihood() > 1)
            addEventLikelihoodFilter(eventQuery, requestData.getEventLikelihood());
        if (requestData.getTaxId() != null && requestData.getTaxId().length > 0) {
            final TermsQuery taxQuery = new TermsQuery(Arrays.stream(requestData.getTaxId()).collect(Collectors.toList()));
            taxQuery.field = FIELD_EVENT_TAX_IDS;
            BoolClause taxIdFilterClause = new BoolClause();
            taxIdFilterClause.occur = FILTER;
            taxIdFilterClause.addQuery(taxQuery);
            eventQuery.addClause(taxIdFilterClause);
        }

        return eventQuery;
    }

    private static void addEventLikelihoodFilter(BoolQuery eventQuery, int likelihood) {
        final RangeQuery query = new RangeQuery();
        query.field = FIELD_EVENT_LIKELIHOOD;
        query.greaterThanOrEqual = likelihood;
        BoolClause likelihoodFilterClause = new BoolClause();
        likelihoodFilterClause.occur = FILTER;
        likelihoodFilterClause.addQuery(query);
        eventQuery.addClause(likelihoodFilterClause);
    }

    public static BoolQuery getFulltextQuery(List<String> eventTypes, int eventLikelihood, String sentenceFilter, String paragraphFilter, String sectionNameFilter, String filterFieldsConnectionOperator, String[] taxIds) {
        BoolQuery eventQuery = new BoolQuery();

        if (eventTypes != null && !eventTypes.isEmpty()) {
            TermsQuery eventTypesQuery = new TermsQuery(new ArrayList<>(eventTypes));
            eventTypesQuery.field = FIELD_EVENT_ALL_EVENTTYPES;
            BoolClause eventTypeClause = new BoolClause();
            eventTypeClause.addQuery(eventTypesQuery);
            eventTypeClause.occur = FILTER;
            eventQuery.addClause(eventTypeClause);
        }

        BoolQuery fulltextQuery = new BoolQuery();
        BoolClause.Occur filterFieldsOccur = filterFieldsConnectionOperator.equalsIgnoreCase("and") ? MUST : BoolClause.Occur.SHOULD;
        if (!StringUtils.isBlank(sentenceFilter)) {
            addFulltextSearchQuery(sentenceFilter, FIELD_EVENT_SENTENCE_TEXT, filterFieldsOccur, fulltextQuery);
        }
        if (!StringUtils.isBlank(paragraphFilter)) {
            addFulltextSearchQuery(paragraphFilter, FIELD_EVENT_PARAGRAPH_TEXT, filterFieldsOccur, fulltextQuery);
        }
        if (!StringUtils.isBlank(sectionNameFilter)) {
            addFulltextSearchQuery(sectionNameFilter, FIELD_PARAGRAPH_HEADINGS, filterFieldsOccur, eventQuery);
        }
        BoolClause fulltextClause = new BoolClause();
        fulltextClause.addQuery(fulltextQuery);
        fulltextClause.occur = MUST;
        eventQuery.addClause(fulltextClause);
        if (eventLikelihood > 1) {
            addEventLikelihoodFilter(eventQuery, eventLikelihood);
        }
        if (taxIds != null && taxIds.length > 0) {
            final TermsQuery taxQuery = new TermsQuery(Arrays.stream(taxIds).collect(Collectors.toList()));
            taxQuery.field = FIELD_EVENT_TAX_IDS;
            BoolClause taxIdFilterClause = new BoolClause();
            taxIdFilterClause.occur = FILTER;
            taxIdFilterClause.addQuery(taxQuery);
            eventQuery.addClause(taxIdFilterClause);
        }
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
