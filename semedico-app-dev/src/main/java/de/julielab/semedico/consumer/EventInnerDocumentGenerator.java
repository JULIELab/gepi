package de.julielab.semedico.consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.JCasUtil;

import de.julielab.jcore.types.ArgumentMention;
import de.julielab.jcore.types.EventMention;
import de.julielab.jcore.types.GeneralEventMention;
import de.julielab.jcore.types.LikelihoodIndicator;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jules.consumer.elasticsearch.ArrayFieldValue;
import de.julielab.jules.consumer.elasticsearch.FieldsGenerationException;
import de.julielab.jules.consumer.elasticsearch.FilterRegistry;
import de.julielab.jules.consumer.elasticsearch.InnerDocumentGenerator;
import de.julielab.jules.consumer.esconsumer.filter.Filter;
import de.julielab.jules.consumer.esconsumer.filter.FilterChain;
import de.julielab.jules.consumer.esconsumer.filter.UniqueFilter;
import de.julielab.jules.consumer.esconsumer.preanalyzed.Document;
import de.julielab.jules.consumer.esconsumer.preanalyzed.IFieldValue;
import de.julielab.jules.consumer.esconsumer.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jules.consumer.esconsumer.preanalyzed.PreanalyzedToken;
import de.julielab.jules.consumer.esconsumer.preanalyzed.RawToken;

public class EventInnerDocumentGenerator extends InnerDocumentGenerator {

	private FilterChain uniqueHypernymsFilter;
	private SemedicoFilterBoard fb;
	private UniqueFilter uniqueFilter = new UniqueFilter();
	private Map<String, Integer> likelihoodValues;
	private String[] argumentMentionFps;
	private String[] eventMentionFps;
	private Filter[] uniqueHypernymsWordNormFilter;

	public EventInnerDocumentGenerator(FilterRegistry filterRegistry,
			Map<String, Integer> likelihoodValues) {
		super(filterRegistry);
		this.likelihoodValues = likelihoodValues;
		fb = (SemedicoFilterBoard) filterRegistry
				.getFilterBoard(SemedicoFilterBoard.SEMEDICO_BOARD);
		uniqueHypernymsFilter = new FilterChain(fb.hypernymsFilter, new UniqueFilter());
		argumentMentionFps = new String[] { "/ref/resourceEntryList/entryId", "/ref:coveredText()" };
		eventMentionFps = new String[] { "/specificType", null };
		uniqueHypernymsWordNormFilter = new Filter[] { uniqueHypernymsFilter,
				fb.wordNormalizationChain };
	}

	@Override
	public IFieldValue generateDocument(FeatureStructure fs) throws FieldsGenerationException {
		FlattenedRelation rel = (FlattenedRelation) fs;

		Document relDoc = new Document();
		ArrayFieldValue agentTerms = new ArrayFieldValue();
		ArrayFieldValue patientTerms = new ArrayFieldValue();
		ArrayFieldValue argumentTerms = new ArrayFieldValue();
		ArrayFieldValue eventTerms = new ArrayFieldValue();
		ArrayFieldValue mainEventTerms = new ArrayFieldValue();
		PreanalyzedFieldValue sentenceFieldValue;
		String flatEventId = rel.getId();

		RawToken likelihood;

		try {
			if (rel.getAgents() != null)
				agentTerms.addFlattened(createRawFieldValueForAnnotations(
						rel.getAgents().toArray(), argumentMentionFps,
						uniqueHypernymsWordNormFilter, uniqueFilter));

			if (rel.getPatients() != null)
				patientTerms
						.addFlattened(createRawFieldValueForAnnotations(
								rel.getPatients().toArray(), argumentMentionFps,
								uniqueHypernymsWordNormFilter, uniqueFilter));

			argumentTerms.addFlattened(createRawFieldValueForAnnotations(rel.getArguments()
					.toArray(), argumentMentionFps, uniqueHypernymsWordNormFilter, uniqueFilter));

			eventTerms.addFlattened(createRawFieldValueForAnnotations(rel.getRelations().toArray(),
					eventMentionFps, uniqueHypernymsWordNormFilter, uniqueFilter));

			mainEventTerms.addFlattened(createRawFieldValueForAnnotation(rel.getRootRelation(),
					eventMentionFps, uniqueHypernymsWordNormFilter));

			LikelihoodIndicator likelihoodAnnotation = rel.getRootRelation().getLikelihood();
			Integer likelihoodOrdinal = likelihoodValues.get(likelihoodAnnotation.getLikelihood());
			likelihood = new RawToken(likelihoodOrdinal);

			Sentence sentence = JCasUtil.selectCovering(Sentence.class, rel).get(0);
			List<PreanalyzedToken> eventTokens = createHighlightingFieldForEventSentence(rel,
					likelihoodAnnotation, sentence);
			sentenceFieldValue = createPreanalyzedFieldValue(sentence.getCoveredText(), eventTokens);
		} catch (CASException e) {
			throw new FieldsGenerationException("Error while creating inner event document", e);
		}

		relDoc.addField("agent", agentTerms);
		relDoc.addField("patient", patientTerms);
		relDoc.addField("allarguments", argumentTerms);
		relDoc.addField("alleventtypes", eventTerms);
		relDoc.addField("maineventtype", mainEventTerms);
		relDoc.addField("id", flatEventId);

		relDoc.addField("likelihood", likelihood);
		relDoc.addField("sentence", sentenceFieldValue);
		return relDoc;
	}

	private List<PreanalyzedToken> createHighlightingFieldForEventSentence(FlattenedRelation rel,
			LikelihoodIndicator likelihoodAnnotation, Sentence sentence) {
		int offset = sentence.getBegin();
		List<PreanalyzedToken> eventTokens = new ArrayList<>();
		List<AnnotationFS> highlightElements = new ArrayList<>();
		if (likelihoodAnnotation.getEnd() > 0)
			highlightElements.add(likelihoodAnnotation);
		for (int i = 0; i < rel.getArguments().size(); ++i) {
			ArgumentMention arg = rel.getArguments(i);
			highlightElements.add(arg);
		}
		for (int i = 0; i < rel.getRelations().size(); ++i) {
			GeneralEventMention em = rel.getRelations(i);
			highlightElements.add(em);
		}
		Collections.sort(highlightElements, new Comparator<AnnotationFS>() {

			@Override
			public int compare(AnnotationFS o1, AnnotationFS o2) {
				return o1.getBegin() - o2.getBegin();
			}

		});
		for (AnnotationFS a : highlightElements) {
			int start = a.getBegin() - offset;
			int end = a.getEnd() - offset;

			// sometimes the offsets are not restricted to the current sentence
			// which does not work with sentence highlighting
			if (start < 0 || end < 0 || start > end)
				continue;

			eventTokens.add(createPreanalyzedTokenInTokenSequence(eventTokens, "event", start, end,
					1, null, null, 0));

			try {
				if (a instanceof ArgumentMention) {
					List<PreanalyzedToken> argTokens = createPreanalyzedTokensForAnnotation(a,
							argumentMentionFps, uniqueHypernymsWordNormFilter);
					for (PreanalyzedToken t : argTokens) {
						t.start = start;
						t.end = end;
					}
					eventTokens.addAll(argTokens);
				}
				if (a instanceof EventMention) {
					List<PreanalyzedToken> emTokens = createPreanalyzedTokensForAnnotation(a,
							eventMentionFps, uniqueHypernymsWordNormFilter);
					for (PreanalyzedToken t : emTokens) {
						t.start = start;
						t.end = end;
					}
					eventTokens.addAll(emTokens);
				}
			} catch (CASException e) {
				e.printStackTrace();
			}

		}
		return eventTokens;
	}
}
