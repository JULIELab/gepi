package de.julielab.semedico.consumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import de.julielab.jcore.types.AbstractSection;
import de.julielab.jcore.types.AbstractText;
import de.julielab.jcore.types.Cell;
import de.julielab.jcore.types.ConceptMention;
import de.julielab.jcore.types.Date;
import de.julielab.jcore.types.EventMention;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.LikelihoodIndicator;
import de.julielab.jcore.types.OntClassMentionAggElement;
import de.julielab.jcore.types.OntClassMentionAggregate;
import de.julielab.jcore.types.OntClassMentionSimple;
import de.julielab.jcore.types.Organism;
import de.julielab.jcore.types.PubType;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.types.stemnet.CD_antigens;
import de.julielab.jcore.types.stemnet.MinorHA;
import de.julielab.jules.consumer.elasticsearch.ArrayFieldValue;
import de.julielab.jules.consumer.elasticsearch.FeaturePathSet;
import de.julielab.jules.consumer.elasticsearch.FeaturePathSets;
import de.julielab.jules.consumer.elasticsearch.FieldsGenerationException;
import de.julielab.jules.consumer.elasticsearch.FieldsGenerator;
import de.julielab.jules.consumer.elasticsearch.FilterRegistry;
import de.julielab.jules.consumer.esconsumer.filter.FilterChain;
import de.julielab.jules.consumer.esconsumer.filter.ReplaceFilter;
import de.julielab.jules.consumer.esconsumer.filter.SemedicoTermFilter;
import de.julielab.jules.consumer.esconsumer.filter.SemedicoTermsHypernymsFilter;
import de.julielab.jules.consumer.esconsumer.filter.UniqueFilter;
import de.julielab.jules.consumer.esconsumer.preanalyzed.Document;
import de.julielab.jules.consumer.esconsumer.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jules.consumer.esconsumer.preanalyzed.PreanalyzedToken;

/**
 * A common super class for Semedico-related FieldsGenerators. Offers a few
 * helper methods required commonly in the Semedico-context.
 * 
 * @author faessler
 * 
 */
public abstract class AbstractSemedicoFieldsGenerator extends FieldsGenerator {

	protected final SemedicoFilterBoard fb;
	protected final SemedicoTermsHypernymsFilter hypernymsFilter;
	protected final ReplaceFilter meshTermReplaceFilter;
	// protected final ValidWordFilter validTermsFilter;
	protected final SemedicoTermFilter semedicoTermFilter;
	// protected final SemedicoFacetIdReplaceFilter
	// eventTermCategoryReplaceFilter;
	protected final FilterChain tokenFilterChain;
	protected final FilterChain journalFilterChain;
	protected final FilterChain eventFilterChain;
	protected final FilterChain facetRecommenderFilterChain;
	protected final FilterChain meshFilterChain;
	// protected final Map<String, String> eventTermPatterns;
	protected final HashMap<String, Integer> likelihoodValues;
	protected final HashMap<Integer, String> inverseLikelihoodValues;
	protected FeaturePathSets preanalyzedTextFeaturePathSets;
	protected final ReplaceFilter elementsAggregateIdReplaceFilter;

	public AbstractSemedicoFieldsGenerator(FilterRegistry filterRegistry) {
		super(filterRegistry);

		fb = (SemedicoFilterBoard) filterRegistry.getFilterBoard(SemedicoFilterBoard.SEMEDICO_BOARD);

		hypernymsFilter = fb.hypernymsFilter;
		meshTermReplaceFilter = fb.meshTermReplaceFilter;
		// validTermsFilter = fb.validTermsFilter;
		semedicoTermFilter = fb.semedicoTermFilter;
		// eventTermCategoryReplaceFilter = fb.eventTermCategoryReplaceFilter;
		tokenFilterChain = fb.tokenFilterChain;
		journalFilterChain = fb.journalFilterChain;
		eventFilterChain = fb.eventFilterChain;
		facetRecommenderFilterChain = fb.facetRecommenderFilterChain;
		meshFilterChain = fb.meshFilterChain;
		elementsAggregateIdReplaceFilter = fb.elementsAggregateIdReplaceFilter;
		// eventTermPatterns = fb.eventTermPatterns;
		likelihoodValues = new HashMap<>();
		likelihoodValues.put("negation", 1);
		likelihoodValues.put("low", 2);
		likelihoodValues.put("investigation", 3);
		likelihoodValues.put("moderate", 4);
		likelihoodValues.put("high", 5);
		likelihoodValues.put("assertion", 6);
		inverseLikelihoodValues = new HashMap<>();
		for (Entry<String, Integer> e : likelihoodValues.entrySet())
			inverseLikelihoodValues.put(e.getValue(), e.getKey());
	}

	protected AbstractText getAbstractText(JCas aJCas) {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(AbstractText.type).iterator();
		AbstractText abstractText = null;
		// we search for the abstract that ends first; this makes no further
		// sense, of course, their even shouldn't be multiple abstract. But at
		// time of writing this, there is an error where there are two abstracts
		// and one might be too long, causing errors when calling
		// 'getCoveredText()'. And we don't have time for error analysis and
		// reprocessing.
		// int minEnd = Integer.MAX_VALUE;
		if (it.hasNext()) {
			AbstractText text = (AbstractText) it.next();
			abstractText = text;
			// if (text.getEnd() < minEnd) {
			// abstractText = text;
			// minEnd = abstractText.getEnd();
			// }
		}
		// if (abstractText.getBegin() < 0)
		// abstractText.setBegin(0);
		// if (abstractText.getEnd() >= aJCas.getDocumentText().length())
		// abstractText.setEnd(aJCas.getDocumentText().length() - 1);
		if (it.hasNext()) {
			Header h = (Header) aJCas.getAnnotationIndex(Header.type).iterator().next();
			log.warn("Document {} (src {}) has multiple abstract annotations", h.getDocId(), h.getSource());
		}
		return abstractText;
	}

	protected Title getDocumentTitle(JCas aJCas) {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(Title.type).iterator();
		Title title = null;
		while (it.hasNext()) {
			Title titleAnnotation = (Title) it.next();
			if (titleAnnotation.getTitleType() != null && titleAnnotation.getTitleType().equals("document")) {
				if (null == title)
					title = titleAnnotation;
				else
					log.warn("Found multiple document titles: " + title + ", " + titleAnnotation);
			}
		}
		return title;
	}

	/**
	 * Returns a list of parts of a structured abstract, if any. For
	 * non-structured abstracts, an empty list is returned.
	 * 
	 * @param aJCas
	 * @return
	 */
	protected List<AbstractSection> getAbstractParts(JCas aJCas) {
		List<AbstractSection> abstractParts = new ArrayList<>();
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(AbstractSection.type).iterator();
		while (it.hasNext()) {
			AbstractSection abstractSection = (AbstractSection) it.next();
			// we do this due to the same reason as for abstractText
			// String documentText = aJCas.getDocumentText();
			// if (abstractSection.getEnd() >= documentText.length())
			// abstractSection.setEnd(documentText.length() - 1);
			// if (abstractSection.getBegin() < 0)
			// abstractSection.setBegin(0);
			abstractParts.add(abstractSection);
		}
		return abstractParts;
	}

	protected FeaturePathSets getPreanalyzedTextFeatureSets() {

		if (null == preanalyzedTextFeaturePathSets) {
			long time = System.currentTimeMillis();
			ArrayList<String> fpResourceEntryId = Lists.newArrayList("/resourceEntryList/entryId");
			FilterChain termsAndHypernymsFilterChain = new FilterChain();
			// termsAndHypernymsFilterChain.add(validTermsFilter);
			termsAndHypernymsFilterChain.add(semedicoTermFilter);
			termsAndHypernymsFilterChain.add(hypernymsFilter);
			termsAndHypernymsFilterChain.add(new UniqueFilter());
			preanalyzedTextFeaturePathSets = new FeaturePathSets();
			preanalyzedTextFeaturePathSets.add(new FeaturePathSet(Token.type, null, null, tokenFilterChain));
			preanalyzedTextFeaturePathSets
					.add(new FeaturePathSet(Gene.type, fpResourceEntryId, null, termsAndHypernymsFilterChain));
			preanalyzedTextFeaturePathSets
					.add(new FeaturePathSet(EventMention.type, Arrays.asList("/specificType"), null, null));
			preanalyzedTextFeaturePathSets.add(new FeaturePathSet(OntClassMentionSimple.type,
					Arrays.asList("/specificType"), null, termsAndHypernymsFilterChain));
			preanalyzedTextFeaturePathSets.add(new FeaturePathSet(OntClassMentionAggElement.type,
					Arrays.asList("/specificType"), null, termsAndHypernymsFilterChain));
			preanalyzedTextFeaturePathSets.add(new FeaturePathSet(OntClassMentionAggregate.type,
					Arrays.asList("/specificType"), null, semedicoTermFilter));
			preanalyzedTextFeaturePathSets.add(new FeaturePathSet(CD_antigens.type, Arrays.asList("/specificType"),
					null, termsAndHypernymsFilterChain));
			preanalyzedTextFeaturePathSets.add(new FeaturePathSet(MinorHA.type, Arrays.asList("/specificType"), null,
					termsAndHypernymsFilterChain));
			preanalyzedTextFeaturePathSets.add(
					new FeaturePathSet(Cell.type, Arrays.asList("/specificType"), null, termsAndHypernymsFilterChain));
			preanalyzedTextFeaturePathSets
					.add(new FeaturePathSet(Organism.type, fpResourceEntryId, null, termsAndHypernymsFilterChain));
			time = System.currentTimeMillis() - time;
			featureSetsCreationTime += time;
		}
		return preanalyzedTextFeaturePathSets;
	}

	protected ArrayFieldValue getTextSpanFieldValues(JCas aJCas, int spanType)
			throws CASException, FieldsGenerationException {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(spanType).iterator();
		FeaturePathSets featurePathSets = getPreanalyzedTextFeatureSets();
		ArrayFieldValue spanAnnotations = new ArrayFieldValue();
		while (it.hasNext()) {
			Annotation span = it.next();
			if (span.getBegin() < 0 || span.getEnd() > aJCas.getDocumentText().length()) {
				log.warn("Annotation of type {} occured with begin={} and end={} (document text length: {}). Ignoring",
						new Object[] { span.getClass().getName(), span.getBegin(), span.getEnd(),
								aJCas.getDocumentText().length() });
				continue;
			}
			String spanText = span.getCoveredText();
			List<PreanalyzedToken> tokensInSpan = getTokensForAnnotationIndexes(featurePathSets, null, true,
					PreanalyzedToken.class, span, null, aJCas);
			int meanLikelihood = getMeanLikelihood(span);
			Document spanDoc = new Document();
			spanDoc.addField("text", createPreanalyzedFieldValue(spanText, tokensInSpan));
			spanDoc.addField("likelihood", meanLikelihood);
			if (!tokensInSpan.isEmpty())
				spanAnnotations.add(spanDoc);
		}
		return spanAnnotations;
	}

	protected int getMeanLikelihood(AnnotationFS annotation) throws FieldsGenerationException {
		List<Integer> likelihoods = new ArrayList<>();
		try {
			// here we get all the likelihoods explicitly stated in the text
			// covered by the annotation parameter
			FSIterator<Annotation> it = annotation.getCAS().getJCas().getAnnotationIndex(LikelihoodIndicator.type)
					.subiterator(annotation);
			while (it.hasNext()) {
				LikelihoodIndicator likelihood = (LikelihoodIndicator) it.next();
				if (null != likelihood) {
					Integer likelihoodOrdinal = likelihoodValues.get(likelihood.getLikelihood());
					likelihoods.add(likelihoodOrdinal);
				}
			}

			// assertions are mostly not expressed explicitly but hold by the
			// absence of another likelihood indicator; technically, the
			// LikelihoodAssignment component creates one assertion-likelihood
			// and places it at begin=0, end=0. Thus, we check if some
			// ConceptMention in the sought text span refers to this specific
			// out-of-bounds-likelihood, since all in-bounds-likelhoods have
			// been covered by the loop above.
			it = annotation.getCAS().getJCas().getAnnotationIndex(ConceptMention.type).subiterator(annotation);
			while (it.hasNext()) {
				ConceptMention concept = (ConceptMention) it.next();
				LikelihoodIndicator likelihood = concept.getLikelihood();
				if (null != likelihood && (likelihood.getBegin() >= annotation.getEnd()
						|| likelihood.getEnd() <= annotation.getBegin())) {
					Integer likelihoodOrdinal = likelihoodValues.get(likelihood.getLikelihood());
					likelihoods.add(likelihoodOrdinal);
				}
			}
		} catch (CASRuntimeException | CASException e) {
			throw new FieldsGenerationException(
					"Exception occurred while determining mean likelihood of text covered by annotation " + annotation
							+ ".",
					e);
		}

		// if there is no likelihood indicator whatsoever, we assume certain
		// facts in the text passage
		if (likelihoods.isEmpty())
			return likelihoodValues.get("assertion");

		// mean of ordinals is contentious but in our case it should result in
		// something like a "mean likelihood"
		double sum = 0;
		for (Integer likelihoodOrdinal : likelihoods) {
			sum += likelihoodOrdinal;
		}
		double mean = sum / likelihoods.size();
		int meanLikelihoodValue = (int) Math.round(mean);
		return meanLikelihoodValue;
		// String ret = inverseLikelihoodValues.get(meanLikelihoodValue);
		// return ret;
	}

	protected List<String> getPubDateStrings(JCas aJCas) {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(Header.type).iterator();
		if (!it.hasNext())
			return Collections.emptyList();
		List<String> pubDateStrings = new ArrayList<>();
		Header h = (Header) it.next();
		FSArray pubTypeList = h.getPubTypeList();
		for (int i = 0; i < pubTypeList.size(); i++) {
			PubType pubType = (PubType) pubTypeList.get(i);
			Date pubDate = pubType.getPubDate();
			int year = pubDate.getYear();
			if (year == 0)
				continue;
			int month = pubDate.getMonth();
			if (month == 0)
				month = 1;
			int day = pubDate.getDay();
			if (day == 0)
				day = 1;
			String monthStr = String.valueOf(month);
			String dayStr = String.valueOf(day);
			if (monthStr.length() == 1)
				monthStr = 0 + monthStr;
			if (dayStr.length() == 1)
				dayStr = 0 + dayStr;
			String pubDateString = year + "-" + monthStr + "-" + dayStr;
			pubDateStrings.add(pubDateString);
		}
		return pubDateStrings;
	}
}
