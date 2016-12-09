package de.julielab.semedico.consumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import com.google.common.collect.Lists;

import de.julielab.jcore.types.AbstractSection;
import de.julielab.jcore.types.AbstractSectionHeading;
import de.julielab.jcore.types.AbstractText;
import de.julielab.jcore.types.Cell;
import de.julielab.jcore.types.Chemical;
import de.julielab.jcore.types.EventMention;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Journal;
import de.julielab.jcore.types.MeshHeading;
import de.julielab.jcore.types.OntClassMentionAggregate;
import de.julielab.jcore.types.OntClassMentionSimple;
import de.julielab.jcore.types.Organism;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.types.pubmed.ManualDescriptor;
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

public abstract class AbstractPubmedPmcFieldsGenerator extends AbstractSemedicoFieldsGenerator {

	public AbstractPubmedPmcFieldsGenerator(FilterRegistry filterRegistry) {
		super(filterRegistry);
		addInnerDocumentGenerator(new EventInnerDocumentGenerator(filterRegistry, likelihoodValues));
	}

	/**
	 * Makes the following method calls for field creation:
	 * <ul>
	 * <li>createPmidField(aJCas, doc);
	 * <li>createPmcidField(aJCas, doc);
	 * <li>createTitleField(aJCas, doc);
	 * <li>createAbstractField(aJCas, doc);
	 * <li>createAbstractSectionsField(aJCas, doc);
	 * <li>createSentencesField(aJCas, doc);
	 * <li>createEventsField(aJCas, doc);
	 * <li>createKeywordsField(aJCas, doc);
	 * <li>createAuthorsField(aJCas, doc);
	 * <li>createJournalField(aJCas, doc);
	 * <li>createConceptListField(aJCas, doc);
	 * <li>createDateField(aJCas, doc);
	 * </ul>
	 */
	@Override
	public Document addFields(JCas aJCas, Document doc) throws CASException, FieldsGenerationException {
		createPmidField(aJCas, doc);
		createPmcidField(aJCas, doc);
		createTitleField(aJCas, doc);
		createAbstractField(aJCas, doc);
		createAbstractSectionsField(aJCas, doc);
		createSentencesField(aJCas, doc);
		createEventsField(aJCas, doc);
		createKeywordsField(aJCas, doc);
		createAuthorsField(aJCas, doc);
		createJournalAndPubtypeField(aJCas, doc);
		createConceptListField(aJCas, doc);
		createDateField(aJCas, doc);

		return doc;
	}

	protected abstract void createPmidField(JCas aJCas, Document doc) throws CASException;

	protected abstract void createPmcidField(JCas aJCas, Document doc) throws CASException;

	protected void createTitleField(JCas aJCas, Document doc) throws CASException {
		FeaturePathSets featurePathSets = getPreanalyzedTextFeatureSets();

		Title title = getDocumentTitle(aJCas);
		if (null != title && title.getBegin() >= 0 && title.getEnd() >= 0) {
			List<PreanalyzedToken> titleFieldValues = getTokensForAnnotationIndexes(featurePathSets, null, true,
					PreanalyzedToken.class, title, null, aJCas);
			doc.addField("title", createPreanalyzedFieldValue(title.getCoveredText(), titleFieldValues));
		}
	}

	protected void createAbstractField(JCas aJCas, Document doc) throws CASException {
		FeaturePathSets featurePathSets = getPreanalyzedTextFeatureSets();

		AbstractText abstractText = getAbstractText(aJCas);
		if (null == abstractText)
			return;
		List<PreanalyzedToken> abstractFieldValues = getTokensForAnnotationIndexes(featurePathSets, null, true,
				PreanalyzedToken.class, abstractText, null, aJCas);
		PreanalyzedFieldValue fieldValue = createPreanalyzedFieldValue(abstractText.getCoveredText(),
				abstractFieldValues);
		doc.addField("abstracttext", fieldValue);
	}

	protected void createAbstractSectionsField(JCas aJCas, Document doc)
			throws CASException, FieldsGenerationException {
		ArrayFieldValue abstractPartValues = new ArrayFieldValue();

		FeaturePathSets featurePathSets = getPreanalyzedTextFeatureSets();

		List<AbstractSection> abstractParts = getAbstractParts(aJCas);
		for (AbstractSection abstractPart : abstractParts) {
			List<PreanalyzedToken> abstractPartFieldValues = getTokensForAnnotationIndexes(featurePathSets, null, true,
					PreanalyzedToken.class, abstractPart, null, aJCas);
			PreanalyzedFieldValue fieldValue = createPreanalyzedFieldValue(abstractPart.getCoveredText(),
					abstractPartFieldValues);
			Document abstractPartDocument = new Document();
			abstractPartDocument.addField("text", fieldValue);
			abstractPartValues.add(abstractPartDocument);

			int meanLikelihood = getMeanLikelihood(abstractPart);
			abstractPartDocument.addField("likelihood", meanLikelihood);

			AbstractSectionHeading sectionHeading = (AbstractSectionHeading) abstractPart.getAbstractSectionHeading();
			abstractPartDocument.addField("label", sectionHeading.getLabel());
			abstractPartDocument.addField("nlmcategory", sectionHeading.getNlmCategory());
		}
		doc.addField("abstractsections", abstractPartValues);
	}

	protected void createSentencesField(JCas aJCas, Document doc) throws CASException, FieldsGenerationException {
		ArrayFieldValue sentences = getTextSpanFieldValues(aJCas, Sentence.type);
		doc.addField("sentences", sentences);
	}

	protected void createEventsField(JCas aJCas, Document doc) throws CASException, FieldsGenerationException {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(FlattenedRelation.type).iterator();
		InnerDocumentGenerator eventGenerator = getInnerDocumentGenerator(EventInnerDocumentGenerator.class);
		ArrayFieldValue events = new ArrayFieldValue();
		while (it.hasNext()) {
			FlattenedRelation event = (FlattenedRelation) it.next();
			IFieldValue eventDoc = eventGenerator.generateDocument(event);
			events.add(eventDoc);
		}
		doc.addField("events", events);
	}

	protected void createKeywordsField(JCas aJCas, Document doc) throws CASException {
		FeaturePathSet fps = new FeaturePathSet(ManualDescriptor.type, Arrays.asList("/keywordList/name"));
		FeaturePathSets fpss = new FeaturePathSets(fps);
		List<RawToken> keywords = getTokensForAnnotationIndexes(fpss, null, false, RawToken.class, null, null, aJCas);
		doc.addField("keywords", new ArrayFieldValue(keywords));
	}

	protected void createAuthorsField(JCas aJCas, Document doc) throws CASException {
		// TODO check: haben wir auch die "initials" im Typen "AuthorInfo"?
		// Hinzufügen! Gibt es dafür vielleicht eine besonders clevere Art und
		// Weise?! Vermutlich einfach: Die Initialen scheinen immer nur den oder
		// die Vornamen abzukürzen (falls sie vorhanden sind), man kann also
		// einfach einen neuen Namen zusammen bauen, bei dem der Vorname
		// entsprechend auf Initialen gesetzt ist; vielleicht brauchen wir aber
		// sowieso ein eigenes Feld, damit wir das in Semedico unterscheiden
		// können; aber wir würden das gerne im Author-Feld mit indexieren,
		// damit wir alles suchen können, es aber nicht storen. Also zweifach
		// indexieren, einfach storen. Geht irgendwie glaube ich, ES
		// Dokumentation, ich koooooomme....
		FeaturePathSet unfilteredset = new FeaturePathSet(Header.type,
				Arrays.asList("/authors/lastName", "/authors/foreName"), ", ", null);
		List<RawToken> tokens = getTokensForAnnotationIndexes(new FeaturePathSets(unfilteredset), null, false,
				RawToken.class, null, null, aJCas);
		doc.addField("authors", new ArrayFieldValue(tokens));
	}

	protected void createJournalAndPubtypeField(JCas aJCas, Document doc) throws CASException {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(Journal.type).iterator();

		// currently, our PMC documents don't always have their journal set
		// (BioC-PMC has this information not always complete)
		if (!it.hasNext())
			return;
		Journal journal = (Journal) it.next();

		Document journalFieldValue = new Document();
		journalFieldValue.addField("title", journal.getTitle());
		journalFieldValue.addField("volume", journal.getVolume());
		journalFieldValue.addField("issue", journal.getIssue());
		journalFieldValue.addField("pages", journal.getPages());

		doc.addField("journal", journalFieldValue);
		
		String publicationType = journal.getName();
		doc.addField("pubtype", publicationType);
	}

	protected void createConceptListField(JCas aJCas, Document doc) throws CASException {

		FilterChain meshFilterChainWithoutHypernyms = new FilterChain();
		meshFilterChainWithoutHypernyms.add(meshTermReplaceFilter);
		meshFilterChainWithoutHypernyms.add(semedicoTermFilter);

		// DistributedField<RawToken> distributedField = new DistributedField<>(
		// "facetTermsNoHypernymsAggregates", fb.termFacetIds,
		// fb.aggFacetIds, null);

		ArrayList<String> featurePathsEntryId = Lists.newArrayList("/resourceEntryList/entryId");
		FeaturePathSets featurePathSets = new FeaturePathSets();

		featurePathSets.add(new FeaturePathSet(MeshHeading.type, Lists.newArrayList("/descriptorName"), null,
				meshFilterChainWithoutHypernyms));
		featurePathSets.add(new FeaturePathSet(Chemical.type, Lists.newArrayList("/nameOfSubstance"), null,
				meshFilterChainWithoutHypernyms));
		featurePathSets.add(new FeaturePathSet(OntClassMentionSimple.type, Arrays.asList("/specificType"), null, null));
		featurePathSets.add(new FeaturePathSet(OntClassMentionAggregate.type, Arrays.asList("/specificType"), null, null));
		// for genes, we currently don't want the basic NCBI Gene entries but the homology aggregates
		featurePathSets.add(new FeaturePathSet(Gene.type, featurePathsEntryId, null, elementsAggregateIdReplaceFilter));
		featurePathSets.add(new FeaturePathSet(Organism.type, featurePathsEntryId, null, null));
		featurePathSets.add(new FeaturePathSet(CD_antigens.type, Arrays.asList("/specificType"), null, null));
		featurePathSets.add(new FeaturePathSet(MinorHA.type, Arrays.asList("/specificType"), null, null));
		featurePathSets.add(new FeaturePathSet(Cell.type, Arrays.asList("/specificType"), null, null));
		featurePathSets.add(new FeaturePathSet(EventMention.type, Arrays.asList("/specificType"), null, null));

		List<RawToken> tokens = getTokensForAnnotationIndexes(featurePathSets,
				new FilterChain(semedicoTermFilter, new UniqueFilter()), false, RawToken.class, null, null, aJCas);

		doc.addField("conceptlist", new ArrayFieldValue(tokens));
	}

	protected void createDateField(JCas aJCas, Document doc) throws CASException {
		List<String> pubDateStrings = getPubDateStrings(aJCas);
		if (!pubDateStrings.isEmpty()) {
			doc.addField("date", pubDateStrings.get(0));
		}
	}

}
