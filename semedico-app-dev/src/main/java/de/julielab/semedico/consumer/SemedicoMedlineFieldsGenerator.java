package de.julielab.semedico.consumer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import com.google.common.collect.Lists;

import de.julielab.jcore.types.Chemical;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.MeshHeading;
import de.julielab.jules.consumer.elasticsearch.ArrayFieldValue;
import de.julielab.jules.consumer.elasticsearch.FeaturePathSet;
import de.julielab.jules.consumer.elasticsearch.FeaturePathSets;
import de.julielab.jules.consumer.elasticsearch.FieldsGenerationException;
import de.julielab.jules.consumer.elasticsearch.FilterRegistry;
import de.julielab.jules.consumer.esconsumer.preanalyzed.Document;
import de.julielab.jules.consumer.esconsumer.preanalyzed.RawToken;

public class SemedicoMedlineFieldsGenerator extends AbstractPubmedPmcFieldsGenerator {

	private Matcher pmcIdMatcher;

	public SemedicoMedlineFieldsGenerator(FilterRegistry filterRegistry) {
		super(filterRegistry);
		addInnerDocumentGenerator(new EventInnerDocumentGenerator(filterRegistry, likelihoodValues));
		pmcIdMatcher = Pattern.compile("PMC([0-9]+)").matcher("");
	}

	@Override
	public Document addFields(JCas aJCas, Document doc) throws CASException, FieldsGenerationException {
		super.addFields(aJCas, doc);
		createMeshMajorField(aJCas, doc);
		createMeshMinorField(aJCas, doc);
		createSubstancesField(aJCas, doc);
		createAffiliationField(aJCas, doc);

		return doc;
	}

	protected void createPmcidField(JCas aJCas, Document doc) throws CASException {
		FeaturePathSet fps = new FeaturePathSet(Header.type, Arrays.asList("/otherIDs/id"), null, null);
		List<RawToken> tokens = getTokensForAnnotationIndexes(new FeaturePathSets(fps), null, false, RawToken.class,
				null, null, aJCas);
		if (!tokens.isEmpty()) {
			// there could be more than one other ID (in theory); we want to get
			// the PMCID
			RawToken pmcIdToken = null;
			for (RawToken token : tokens) {
				// most of the time, the ID has the form "PMCXXXXXXX". But
				// sometimes
				// there is even a date, too. We want to parse out the PMCID.
				// For
				// details:
				// https://www.nlm.nih.gov/bsd/mms/medlineelements.html#oid
				String idString = (String) token.token;
				pmcIdMatcher.reset(idString);
				if (pmcIdMatcher.find()) {
					token.token = pmcIdMatcher.group(1);
					pmcIdToken = token;
				}
			}
			doc.addField("pmcid", pmcIdToken);
		}
	}

	protected void createPmidField(JCas aJCas, Document doc) throws CASException {
		FeaturePathSet fps = new FeaturePathSet(Header.type, Arrays.asList("/docId"), null, null);
		List<RawToken> token = getTokensForAnnotationIndexes(new FeaturePathSets(fps), null, false, RawToken.class,
				null, null, aJCas);
		doc.addField("pmid", token.get(0));
	}

	private void createMeshMajorField(JCas aJCas, Document doc) {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(MeshHeading.type).iterator();
		Set<String> descriptors = new HashSet<>();
		while (it.hasNext()) {
			MeshHeading heading = (MeshHeading) it.next();
			boolean isMajorTopic = heading.getDescriptorNameMajorTopic();
			if (isMajorTopic) {
				descriptors.add(heading.getDescriptorName());
			}
		}
		ArrayFieldValue fieldValue = createRawArrayFieldValue(descriptors, meshFilterChain);
		doc.addField("meshmajor", fieldValue);
	}

	private void createMeshMinorField(JCas aJCas, Document doc) {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(MeshHeading.type).iterator();
		Set<String> descriptors = new HashSet<>();
		while (it.hasNext()) {
			MeshHeading heading = (MeshHeading) it.next();
			boolean isMajorTopic = heading.getDescriptorNameMajorTopic();
			if (!isMajorTopic) {
				// System.out.println(heading.getDescriptorName());
				descriptors.add(heading.getDescriptorName());
			}
		}
		ArrayFieldValue fieldValue = createRawArrayFieldValue(descriptors, meshFilterChain);
		doc.addField("meshminor", fieldValue);
	}

	private void createSubstancesField(JCas aJCas, Document doc) throws CASException {
		FeaturePathSets meshSets = new FeaturePathSets(
				new FeaturePathSet(Chemical.type, Lists.newArrayList("/nameOfSubstance"), null, null));
		List<RawToken> meshFieldTokens = getTokensForAnnotationIndexes(meshSets, meshFilterChain, false, RawToken.class,
				null, null, aJCas);

		doc.addField("substances", new ArrayFieldValue(meshFieldTokens));
	}

	private void createAffiliationField(JCas aJCas, Document doc) throws CASException {
		// this is the "easy" way; we could, of course, create inner author
		// objects with first name, last name, initials and affiliations, if the
		// has any advantage
		FeaturePathSet featureset = new FeaturePathSet(Header.type, Arrays.asList("/authors/affiliation"), null, null);
		List<RawToken> tokens = getTokensForAnnotationIndexes(new FeaturePathSets(featureset), null, false,
				RawToken.class, null, null, aJCas);
		doc.addField("affiliation", new ArrayFieldValue(tokens));
	}
}
