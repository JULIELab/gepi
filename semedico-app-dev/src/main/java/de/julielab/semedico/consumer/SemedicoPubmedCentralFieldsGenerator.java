package de.julielab.semedico.consumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

import com.google.common.collect.Lists;

import de.julielab.jcore.types.AbstractSection;
import de.julielab.jcore.types.AbstractSectionHeading;
import de.julielab.jcore.types.AbstractText;
import de.julielab.jcore.types.Bibliography;
import de.julielab.jcore.types.Caption;
import de.julielab.jcore.types.Cell;
import de.julielab.jcore.types.Chemical;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Journal;
import de.julielab.jcore.types.MeshHeading;
import de.julielab.jcore.types.OntClassMentionAggregate;
import de.julielab.jcore.types.OntClassMentionSimple;
import de.julielab.jcore.types.Organism;
import de.julielab.jcore.types.Paragraph;
import de.julielab.jcore.types.Section;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.Zone;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.types.pubmed.ManualDescriptor;
import de.julielab.jcore.types.pubmed.OtherID;
import de.julielab.jcore.types.stemnet.CD_antigens;
import de.julielab.jcore.types.stemnet.MinorHA;
import de.julielab.jules.consumer.elasticsearch.ArrayFieldValue;
import de.julielab.jules.consumer.elasticsearch.FeaturePathSet;
import de.julielab.jules.consumer.elasticsearch.FeaturePathSets;
import de.julielab.jules.consumer.elasticsearch.FieldsGenerationException;
import de.julielab.jules.consumer.elasticsearch.FilterRegistry;
import de.julielab.jules.consumer.elasticsearch.InnerDocumentGenerator;
import de.julielab.jules.consumer.esconsumer.filter.FilterChain;
import de.julielab.jules.consumer.esconsumer.filter.UniqueFilter;
import de.julielab.jules.consumer.esconsumer.preanalyzed.Document;
import de.julielab.jules.consumer.esconsumer.preanalyzed.IFieldValue;
import de.julielab.jules.consumer.esconsumer.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jules.consumer.esconsumer.preanalyzed.PreanalyzedToken;
import de.julielab.jules.consumer.esconsumer.preanalyzed.RawToken;

public class SemedicoPubmedCentralFieldsGenerator extends AbstractPubmedPmcFieldsGenerator {

	public SemedicoPubmedCentralFieldsGenerator(FilterRegistry filterRegistry) {
		super(filterRegistry);
		addInnerDocumentGenerator(new EventInnerDocumentGenerator(filterRegistry, likelihoodValues));
	}

	@Override
	public Document addFields(JCas aJCas, Document doc) throws CASException, FieldsGenerationException {
		super.addFields(aJCas, doc);
		createParagraphsField(aJCas, doc);
		createFigureCaptionsField(aJCas, doc);
		createTableCaptionsField(aJCas, doc);
		createSectionsField(aJCas, doc);
		createOtherZonesField(aJCas, doc);
		createReferencesField(aJCas, doc);

		return doc;
	}

	protected void createPmidField(JCas aJCas, Document doc) throws CASException {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(Header.type).iterator();
		if (!it.hasNext())
			return;
		Header header = (Header) it.next();
		FSArray otherIDs = header.getOtherIDs();
		if (otherIDs == null || otherIDs.size() == 0)
			return;
		String pmid = null;
		for (int i = 0; i < otherIDs.size(); ++i) {
			OtherID otherId = (OtherID) otherIDs.get(i);
			if (otherId.getSource().equals("PubMed"))
				pmid = otherId.getId();
		}

		doc.addField("pmid", pmid);
	}

	protected void createPmcidField(JCas aJCas, Document doc) throws CASException {
		FeaturePathSet fps = new FeaturePathSet(Header.type, Arrays.asList("/docId"), null, null);
		List<RawToken> token = getTokensForAnnotationIndexes(new FeaturePathSets(fps), null, false, RawToken.class,
				null, null, aJCas);
		if (!token.isEmpty())
			doc.addField("pmcid", token.get(0));
	}

	private void createParagraphsField(JCas aJCas, Document doc) throws CASException, FieldsGenerationException {
		ArrayFieldValue paragraphs = getTextSpanFieldValues(aJCas, Paragraph.type);
		doc.addField("paragraphs", paragraphs);
	}

	private void createFigureCaptionsField(JCas aJCas, Document doc) throws CASException, FieldsGenerationException {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(Caption.type).iterator();
		FeaturePathSets featurePathSets = getPreanalyzedTextFeatureSets();
		ArrayFieldValue spanAnnotations = new ArrayFieldValue();
		while (it.hasNext()) {
			Caption caption = (Caption) it.next();
			if (!caption.getCaptionType().equals("figure"))
				continue;
			String spanText = caption.getCoveredText();
			List<PreanalyzedToken> tokensInSpan = getTokensForAnnotationIndexes(featurePathSets, null, true,
					PreanalyzedToken.class, caption, null, aJCas);
			int meanLikelihood = getMeanLikelihood(caption);
			Document spanDoc = new Document();
			spanDoc.addField("text", createPreanalyzedFieldValue(spanText, tokensInSpan));
			spanDoc.addField("likelihood", meanLikelihood);
			spanAnnotations.add(spanDoc);
		}
		doc.addField("figurecaptions", spanAnnotations);
	}

	private void createTableCaptionsField(JCas aJCas, Document doc) throws CASException, FieldsGenerationException {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(Caption.type).iterator();
		FeaturePathSets featurePathSets = getPreanalyzedTextFeatureSets();
		ArrayFieldValue spanAnnotations = new ArrayFieldValue();
		while (it.hasNext()) {
			Caption caption = (Caption) it.next();
			if (!caption.getCaptionType().equals("table"))
				continue;
			String spanText = caption.getCoveredText();
			List<PreanalyzedToken> tokensInSpan = getTokensForAnnotationIndexes(featurePathSets, null, true,
					PreanalyzedToken.class, caption, null, aJCas);
			int meanLikelihood = getMeanLikelihood(caption);
			Document spanDoc = new Document();
			spanDoc.addField("text", createPreanalyzedFieldValue(spanText, tokensInSpan));
			spanDoc.addField("likelihood", meanLikelihood);
			spanAnnotations.add(spanDoc);
		}
		doc.addField("tablecaptions", spanAnnotations);
	}

	private void createSectionsField(JCas aJCas, Document doc) throws CASException, FieldsGenerationException {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(Section.type).iterator();
		FeaturePathSets featurePathSets = getPreanalyzedTextFeatureSets();
		ArrayFieldValue spanAnnotations = new ArrayFieldValue();
		while (it.hasNext()) {
			Section section = (Section) it.next();
			if (section.getBegin() < 0 || section.getEnd() > aJCas.getDocumentText().length() - 1) {
				Header header = (Header) aJCas.getAnnotationIndex(Header.type).iterator().next();
				log.warn("Section annotation in document {} occured with begin={} and end={} (document text length: {}). Ignoring",
						new Object[] { header.getDocId(), section.getBegin(), section.getEnd(),
								aJCas.getDocumentText().length() });
				continue;
			}
			String spanText = section.getCoveredText();
			List<PreanalyzedToken> tokensInSpan = getTokensForAnnotationIndexes(featurePathSets, null, true,
					PreanalyzedToken.class, section, null, aJCas);
			int meanLikelihood = getMeanLikelihood(section);
			Document spanDoc = new Document();
			spanDoc.addField("text", createPreanalyzedFieldValue(spanText, tokensInSpan));
			spanDoc.addField("likelihood", meanLikelihood);
			Title sectionHeading = section.getSectionHeading();
			if (null != sectionHeading && sectionHeading.getBegin() >= 0 && sectionHeading.getEnd() >= sectionHeading.getBegin() && sectionHeading.getEnd() < aJCas.getDocumentText().length()) {
				List<PreanalyzedToken> headingTokens = getTokensForAnnotationIndexes(featurePathSets, null, true,
						PreanalyzedToken.class, sectionHeading, null, aJCas);
				spanDoc.addField("title", createPreanalyzedFieldValue(sectionHeading.getCoveredText(), headingTokens));
				int meanLikelihoodHeading = getMeanLikelihood(sectionHeading);
				spanDoc.addField("titlelikelihood", meanLikelihoodHeading);
			}
			spanAnnotations.add(spanDoc);
		}
		doc.addField("sections", spanAnnotations);
	}

	private void createOtherZonesField(JCas aJCas, Document doc) throws CASException, FieldsGenerationException {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(Zone.type).iterator();
		FeaturePathSets featurePathSets = getPreanalyzedTextFeatureSets();
		ArrayFieldValue spanAnnotations = new ArrayFieldValue();
		while (it.hasNext()) {
			Zone zone = (Zone) it.next();
			if (!zone.getClass().equals(Zone.class))
				continue;
			String spanText = zone.getCoveredText();
			List<PreanalyzedToken> tokensInSpan = getTokensForAnnotationIndexes(featurePathSets, null, true,
					PreanalyzedToken.class, zone, null, aJCas);
			int meanLikelihood = getMeanLikelihood(zone);
			Document spanDoc = new Document();
			spanDoc.addField("text", createPreanalyzedFieldValue(spanText, tokensInSpan));
			spanDoc.addField("likelihood", meanLikelihood);
			spanAnnotations.add(spanDoc);
		}
		doc.addField("zones", spanAnnotations);
	}

	private void createReferencesField(JCas aJCas, Document doc) throws CASException, FieldsGenerationException {
		ArrayFieldValue paragraphs = getTextSpanFieldValues(aJCas, Bibliography.type);
		doc.addField("references", paragraphs);
	}

}
