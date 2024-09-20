package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.components.data.ISearchServerDocument;
import de.julielab.elastic.query.services.IElasticServerResponse;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.*;
import static org.assertj.core.api.Assertions.assertThat;

public class EventResponseProcessingServiceTest {
    @Test
    public void highlightMerging() {
        final EventResponseProcessingService service = new EventResponseProcessingService(LoggerFactory.getLogger(EventPostProcessingService.class));
        final Map<String, List<String>> sentenceHighlights = Map.of(EventRetrievalService.FIELD_EVENT_SENTENCE_TEXT_ARGUMENTS, List.of("These results suggest that CINC produced in the pleural exudate may participate in neutrophil infiltration, that IL-6 induced in the plasma stimulates T-kininogen production, and that endogenous <em class=\"hl-argument\">TNF</em> may be partly involved in the induction of CINC and <em class=\"hl-argument\">IL-6</em> in this zymosan inflammation."),
                EventRetrievalService.FIELD_EVENT_SENTENCE_TEXT_FILTER, List.of("These results suggest that CINC produced in the pleural exudate may participate in neutrophil infiltration, that IL-6 <em class=\"hl-trigger\">induced</em> in the plasma <em class=\"hl-trigger\">stimulates</em> T-kininogen production, and that endogenous TNF may be partly <em class=\"hl-trigger\">involved</em> in the induction of CINC and IL-6 in this zymosan inflammation."),
                EventRetrievalService.FIELD_EVENT_SENTENCE_TEXT_TRIGGER, List.of("These results suggest that CINC produced in the pleural exudate may participate in <em class=\"hl-filter\">neutrophil</em> <em class=\"hl-filter\">infiltration</em>, that IL-6 induced in the plasma stimulates T-kininogen production, and that endogenous TNF may be partly involved in the induction of CINC and IL-6 in this zymosan inflammation."));
        final EventRetrievalResult result = service.getEventRetrievalResult(getESResponseWithHighlighting(sentenceHighlights));
        assertThat(result.getEventList()).isNotNull().isNotEmpty();
        final Event event = result.getEventList().get(0);
        assertThat(event.getHlSentence()).isEqualTo("These results suggest that CINC produced in the pleural exudate may participate in <em class=\"hl-filter\">neutrophil</em> <em class=\"hl-filter\">infiltration</em>, that IL-6 <em class=\"hl-trigger\">induced</em> in the plasma <em class=\"hl-trigger\">stimulates</em> T-kininogen production, and that endogenous <em class=\"hl-argument\">TNF</em> may be partly <em class=\"hl-trigger\">involved</em> in the induction of CINC and <em class=\"hl-argument\">IL-6</em> in this zymosan inflammation.");
    }

    private IElasticServerResponse getESResponseWithHighlighting(Map<String, List<String>> sentenceHighlights) {
        final IElasticServerResponse response = Mockito.mock(IElasticServerResponse.class, inv -> null);

        Mockito.when(response.getDocumentResults()).thenReturn(Stream.of(getSearchServerDocument(Map.of(EventRetrievalService.FIELD_PMID, "1234", EventRetrievalService.FIELD_EVENT_ARG_CONCEPT_IDS, List.of("tid007", "tid006"),FIELD_EVENT_ARG_TOP_HOMOLOGY_IDS, List.of("atid1", "atid2"), FIELD_NUM_ARGUMENTS, 1, FIELD_SOURCE, "pubmed"), sentenceHighlights)));
        Mockito.when(response.getNumFound()).thenReturn(1L);
        return response;
    }


    private ISearchServerDocument getSearchServerDocument(Map<String, Object> values, Map<String, List<String>> highlights) {
        return new ISearchServerDocument() {
            @Override
            public String getId() {
                return (String) values.get(EventRetrievalService.FIELD_PMID);
            }

            @Override
            public <V> Optional<V> get(String s) {
                return (Optional<V>) Optional.ofNullable(values.get(s));
            }

            @Override
            public <V> Optional<V> getFieldValue(String s) {
                return (Optional<V>) Optional.ofNullable(values.get(s));
            }

            @Override
            public Optional<List<Object>> getFieldValues(String s) {
                return Optional.ofNullable((List<Object>) values.get(s));
            }

            @Override
            public float getScore() {
                return 0;
            }

            @Override
            public Map<String, List<String>> getHighlights() {
                return highlights;
            }
        };
    }
}