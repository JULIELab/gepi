package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.components.data.ISearchServerDocument;
import de.julielab.elastic.query.components.data.aggregation.ITermsAggregationUnit;
import de.julielab.elastic.query.components.data.aggregation.TermsAggregation;
import de.julielab.elastic.query.components.data.aggregation.TermsAggregationResult;
import de.julielab.elastic.query.services.IElasticServerResponse;
import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.EsAggregatedResult;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.tapestry5.annotations.Log;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.*;

public class EventResponseProcessingService implements IEventResponseProcessingService {

    private Logger log;

    public EventResponseProcessingService(Logger log) {
        this.log = log;
    }

    @Log
    @Override
    public EventRetrievalResult getEventRetrievalResult(IElasticServerResponse response) {
        if (response.getQueryErrorMessage() != null) {
            log.error("Error while querying ElasticSearch: {}", response.getQueryErrorMessage());
            throw new IllegalStateException(
                    "The ElasticSearch server is down or was not queried correctly: There was no response.");
            // return new EventRetrievalResult();
        }
        // Gets, first, from all document hits their inner (event) hits and,
        // second, converts all inner event hits into instances of the Event
        // class and hence returns all events.
        Stream<Event> eventStream = resultDocuments2Events(response.getDocumentResults());
        EventRetrievalResult eventRetrievalResult = new EventRetrievalResult();
        eventRetrievalResult.setEvents(eventStream);
        eventRetrievalResult.setNumTotalRows(response.getNumFound());
        log.trace("Size of the event retrieval result (number of events): {}", eventRetrievalResult.getEventList().size());
        // postprocess eventPreferred names first with given neo4j information
//		eventPPService.setPreferredNameFromConceptId(eventRetrievalResult.getEventList());
//		eventPPService.setArgumentGeneIds(eventRetrievalResult.getEventList());
        return eventRetrievalResult;
    }


    @Override
    public EsAggregatedResult getEventRetrievalAggregatedResult(IElasticServerResponse searchServerResponse, TermsAggregation eventCountRequest, Set<String> aTopAggregateNames) {
        final EsAggregatedResult result = new EsAggregatedResult();
        result.setTotalNumEvents(searchServerResponse.getNumFound());
        final TermsAggregationResult eventCountResult = (TermsAggregationResult) searchServerResponse.getAggregationResult(eventCountRequest);
        for (ITermsAggregationUnit aggregationUnit : eventCountResult.getAggregationUnits()) {
            final String eventPairTerm = (String) aggregationUnit.getTerm();
            final List<String> eventPair = Arrays.asList(eventPairTerm.split(AGGREGATION_VALUE_DELIMITER));
            final int count = (int) aggregationUnit.getCount();
            if (aTopAggregateNames != null && !aTopAggregateNames.isEmpty()) {
                // If necessary, switch argument positions in order to sort the results for A- and B-List membership
                // First case: The event is unary and the sole argument does not belong to A. Then is must belong to be (or we have a name mismatch)
                if (!aTopAggregateNames.contains(eventPair.get(0)) && eventPair.get(1).equals(FIELD_VALUE_MOCK_ARGUMENT))
                    Collections.swap(eventPair, 0, 1);
                    // Second case: The event is binary and the A-argument is found in second place
                else if (!aTopAggregateNames.contains(eventPair.get(0)) && aTopAggregateNames.contains(eventPair.get(1)))
                    Collections.swap(eventPair, 0, 1);
            }
            // This adds the event and its count to the result and also adds up the arguments and their frequencies.
            // The ES index aggregationvalue field contains the arguments sorted alphabetically, so we cannot meet
            // the same combination of arguments twice.
            result.addArgumentPair(eventPair, count);
        }
        return result;
    }

    private Stream<Event> resultDocuments2Events(Stream<ISearchServerDocument> documents) {
        return documents.map(eventDocument -> {
            Optional<String> pmid = eventDocument.getFieldValue(FIELD_PMID);
            Optional<String> pmcid = eventDocument.getFieldValue(FIELD_PMCID);
            List<Object> conceptIds = eventDocument.getFieldValues(FIELD_EVENT_ARG_CONCEPT_IDS)
                    .orElse(Collections.emptyList());
            List<Object> geneIds = eventDocument.getFieldValues(FIELD_EVENT_ARG_GENE_IDS)
                    .orElse(Collections.emptyList());
            List<Object> topHomologyIds = eventDocument.getFieldValues(FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS)
                    .orElse(Collections.emptyList());
            List<Object> argPrefNames = eventDocument.getFieldValues(FIELD_EVENT_ARG_PREFERRED_NAME)
                    .orElse(Collections.emptyList());
            List<Object> argHomologyPrefNames = eventDocument.getFieldValues(FIELD_EVENT_ARG_HOMOLOGY_PREFERRED_NAME)
                    .orElse(Collections.emptyList());
            List<Object> allEventTypes = eventDocument.getFieldValues(FIELD_EVENT_ALL_EVENTTYPES)
                    .orElse(Collections.emptyList());
            List<Object> texts = eventDocument.getFieldValues(FIELD_EVENT_ARG_TEXT).orElse(Collections.emptyList());
            Optional<String> mainEventType = eventDocument.get(FIELD_EVENT_MAINEVENTTYPE);
            Optional<Integer> likelihood = eventDocument.get(FIELD_EVENT_LIKELIHOOD);
            Optional<String> sentence = eventDocument.get(FIELD_EVENT_SENTENCE_TEXT);
            Optional<String> paragraph = eventDocument.get(FIELD_EVENT_PARAGRAPH_TEXT);
            List<String> sentenceArgumentHl = eventDocument.getHighlights().get(FIELD_EVENT_SENTENCE_TEXT_ARGUMENTS);
            List<String> sentenceTriggerHl = eventDocument.getHighlights().get(FIELD_EVENT_SENTENCE_TEXT_TRIGGER);
            List<String> sentenceFilterHl = eventDocument.getHighlights().get(FIELD_EVENT_SENTENCE_TEXT);
            List<String> sentenceLikelihood1Hl = eventDocument.getHighlights().get(FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_1);
            List<String> sentenceLikelihood2Hl = eventDocument.getHighlights().get(FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_2);
            List<String> sentenceLikelihood3Hl = eventDocument.getHighlights().get(FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_3);
            List<String> sentenceLikelihood4Hl = eventDocument.getHighlights().get(FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_4);
            List<String> sentenceLikelihood5Hl = eventDocument.getHighlights().get(FIELD_EVENT_SENTENCE_TEXT_LIKELIHOOD_5);
            List<String> paragraphFilterHl = eventDocument.getHighlights().get(FIELD_EVENT_PARAGRAPH_TEXT_FILTER);
            List<String> geneMappingSources = eventDocument.getFieldValues(FIELD_GENE_MAPPING_SOURCE).orElse(Collections.emptyList()).stream().map(Object::toString).collect(Collectors.toList());
            int eventArity = (int) eventDocument.getFieldValue(FIELD_NUM_ARGUMENTS).get();
            String eventId = eventDocument.getId();

            // The index is shaped so that there are always two arguments. The second might is the mock argument
            // for unary arguments.
            int numArguments = 2;
            List<Argument> arguments = new ArrayList<>(numArguments);
            for (int i = 0; i < numArguments; ++i) {
                String conceptId = i < conceptIds.size() ? (String) conceptIds.get(i) : null;
                String geneId = i < geneIds.size() ? ((String) geneIds.get(i)).replace("---", " / ") : null;
                String topHomologyId = i < topHomologyIds.size() ? (String) topHomologyIds.get(i) : null;
//                String famplexId = i < famplexIds.size() ? (String) famplexIds.get(i) : null;
//                String hgncGroupId = i < hgncGroupIds.size() ? (String) hgncGroupIds.get(i) : null;
                String text = i < texts.size() ? StringUtils.normalizeSpace((String) texts.get(i)) : null;

                if (conceptId != null) {
                    // assert conceptId != null : "No concept ID received from event document with
                    // ID " + eventDocument.getId() + ":\n" + eventDocument;
                    assert conceptId != null;
                    assert topHomologyId != null;

                    final Argument argument = new Argument(geneId, conceptId, topHomologyId, text);
//                    // We don't want to fetch the gene info for charts. For charts, we don't load the covered text field,
//                    // so we use this as indicator.
//                    if (text != null && !FIELD_VALUE_MOCK_ARGUMENT.equals(conceptId)) {
//                        try {
//                            final GepiConceptInfo geneInfo = geneIdService.getGeneInfo(List.of(conceptId)).get(conceptId);
//                            argument.setGeneInfo(geneInfo);
//                        } catch (Exception e) {
//                            // This is expected when a gene ID is known to GNormPlus but not to GePI.
//                            // This happens because GNormPlus uses a little older version of NCBI Gene. Some IDs,
//                            // e.g. 474256, have been discontinued. Filter out the event.
//                            log.debug("Could not load gene info for concept with ID {}", conceptId);
//                            return null;
//                        }
//                    }
                    arguments.add(argument);
                } else {
                    log.warn(
                            "Came over event document where the concept Id of an argument missing. Document is skipped. This must be fixed in the index. The document has ID {}. Full document:\n{}", eventDocument.getId(),
                            eventDocument);
                    return null;
                }
            }

            Event event = new Event();
            event.setArity(eventArity);
            // Only one ID is present currently
            pmid.ifPresent(event::setDocId);
            pmcid.ifPresent(event::setDocId);
            event.setEventId(eventId);
            event.setArguments(arguments);
            if (likelihood.isPresent())
                event.setLikelihood(likelihood.get());
            if (mainEventType.isPresent())
                event.setMainEventType(mainEventType.get());
            event.setAllEventTypes(allEventTypes.stream().map(String.class::cast).collect(Collectors.toList()));
            String mergedSentenceHl = mergeHighlighting(sentenceArgumentHl, sentenceTriggerHl, sentenceFilterHl, sentenceLikelihood1Hl, sentenceLikelihood2Hl, sentenceLikelihood3Hl, sentenceLikelihood4Hl, sentenceLikelihood5Hl);
            if (mergedSentenceHl != null)
                event.setHlSentence(StringUtils.normalizeSpace(mergedSentenceHl));
            if (sentence.isPresent())
                event.setSentence(StringUtils.normalizeSpace(sentence.get()));
            if (paragraphFilterHl != null && !paragraphFilterHl.isEmpty())
                event.setHlParagraph(StringUtils.normalizeSpace(paragraphFilterHl.get(0)));
            if (paragraph.isPresent())
                event.setParagraph(StringUtils.normalizeSpace(paragraph.get()));
            for (int i = 0; i < event.getNumArguments(); i++) {
                if (!argPrefNames.isEmpty())
                    event.getArgument(i).setPreferredName((String) argPrefNames.get(i));
                if (!argHomologyPrefNames.isEmpty())
                    event.getArgument(i).setTopHomologyPreferredName((String) argHomologyPrefNames.get(i));
            }
            event.setSentenceMatchingFulltextQuery(sentenceFilterHl != null && !sentenceFilterHl.isEmpty());
            event.setParagraphMatchingFulltextQuery(paragraphFilterHl != null && !paragraphFilterHl.isEmpty());
            event.setGeneMappingSources(geneMappingSources);
            return event;
        }).filter(Objects::nonNull);
    }

    /**
     * <p>Convenience signature for {@link #mergeHighlighting(String...)}.</p>
     *
     * @param highlights Lists of highlight string that might be null or empty as they come from the ElasticSearch document.
     * @return The merged highlights of all first list elements (additional items are ignored!) of each input list that is not blank.
     */
    private String mergeHighlighting(List<String>... highlights) {
        return mergeHighlighting(Arrays.stream(highlights).filter(Objects::nonNull).filter(Predicate.not(Collection::isEmpty)).map(l -> l.get(0)).toArray(String[]::new));
    }

    /**
     * <p>Merges different highlighting of the same text string via HTML tags into a single text string with all the highlight tags.</p>
     *
     * @param highlights The different highlightings of the same text.
     * @return The combined highlighted string or <code>null</code> if all input highlights were <code>null</code>.
     */
    private String mergeHighlighting(String... highlights) {
        Pattern tagPattern = Pattern.compile("<[^>]+>");
        // Build position-tag maps. This list will contain one position-tag map for each highlighted string
        List<SortedMap<Integer, String>> tagMaps = new ArrayList<>();
        for (String hl : highlights) {
            if (hl == null || hl.isBlank())
                continue;
            final Matcher tagMatcher = tagPattern.matcher(hl);
            // Sums up the encountered tag lengths. Thus, the start of a tag in the highlighted string minus the offset
            // is the start of the tag without counting previous tags, hence, in the original string.
            int offset = 0;
            // This map stores the position of each tag in the original, non-highlighted string.
            SortedMap<Integer, String> pos2tag = new TreeMap<>();
            while (tagMatcher.find()) {
                final int tagPos = tagMatcher.start();
                final String tag = tagMatcher.group();
                pos2tag.put(tagPos - offset, tag);
                offset += tag.length();
            }
            tagMaps.add(pos2tag);
        }
        if (tagMaps.isEmpty())
            return null;

        // we will add the elements of the merged highlight string from end to start
        List<String> reversedMergedHighlight = new ArrayList<>();
        String nonHighlightedString = tagPattern.matcher(highlights[0]).replaceAll("");
        int lastPos = nonHighlightedString.length();
        // Assemble the merged highlight string. In each iteration we determine the remaining tag with the largest
        // offset and add the text between the previous tag and itself to the merged string.
        while (tagMaps.stream().anyMatch(Predicate.not(Map::isEmpty))) {
            // find the map with the offset-highest tag
            int maxPosIndex = getMaxOffsetTagIndex(tagMaps);
            // the last tag as in the highest offset position of all tags of all highlights
            final SortedMap<Integer, String> highestOffsetTagMap = tagMaps.get(maxPosIndex);
            int pos = highestOffsetTagMap.lastKey();
            String tag = highestOffsetTagMap.get(pos);
            try {
                reversedMergedHighlight.add(nonHighlightedString.substring(pos, lastPos));
            } catch (Exception e) {
                e.printStackTrace();
            }
            reversedMergedHighlight.add(tag);
            // Save the start position of this tag. For the next tag we will need it as the end point of the substring
            // on the nonHighlightedString.
            lastPos = pos;
            // Remove this tag so that in the next iteration we get the preceeding one.
            highestOffsetTagMap.remove(pos);
            if (highestOffsetTagMap.isEmpty())
                tagMaps.remove(maxPosIndex);
        }
        // All tags processed. We now only miss the text from the beginning up to the first tag.
        reversedMergedHighlight.add(nonHighlightedString.substring(0, lastPos));
        return IntStream.range(1, reversedMergedHighlight.size() + 1).mapToObj(i -> reversedMergedHighlight.get(reversedMergedHighlight.size() - i)).collect(Collectors.joining());
    }

    private int getMaxOffsetTagIndex(List<SortedMap<Integer, String>> tagMaps) {
        int indexWithHightestOffsetTag = 0;
        int highestOffset = -1;
        for (int i = 0; i < tagMaps.size(); i++) {
            SortedMap<Integer, String> tagMap = tagMaps.get(i);
            if (tagMap.lastKey() > highestOffset) {
                highestOffset = tagMap.lastKey();
                indexWithHightestOffsetTag = i;
            }
        }
        return indexWithHightestOffsetTag;
    }

}
