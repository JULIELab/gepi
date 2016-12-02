package de.julielab.semedico.consumer;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;

import de.julielab.jcore.types.Header;
import de.julielab.jules.consumer.elasticsearch.FeaturePathSet;
import de.julielab.jules.consumer.elasticsearch.FeaturePathSets;
import de.julielab.jules.consumer.elasticsearch.FieldsGenerationException;
import de.julielab.jules.consumer.elasticsearch.FilterRegistry;
import de.julielab.jules.consumer.esconsumer.preanalyzed.Document;
import de.julielab.jules.consumer.esconsumer.preanalyzed.RawToken;

public class GepiFieldsGenerator extends AbstractPubmedPmcFieldsGenerator {
	private Matcher pmcIdMatcher;
	
	public GepiFieldsGenerator(FilterRegistry filterRegistry) {
		super(filterRegistry);
		addInnerDocumentGenerator(new EventInnerDocumentGenerator(filterRegistry, likelihoodValues));
		pmcIdMatcher = Pattern.compile("PMC([0-9]+)").matcher("");
	}

	@Override
	public Document addFields(JCas aJCas, Document doc) throws CASException, FieldsGenerationException {
		super.addFields(aJCas, doc);
//		createMeshMajorField(aJCas, doc);
//		createMeshMinorField(aJCas, doc);
//		createSubstancesField(aJCas, doc);
//		createAffiliationField(aJCas, doc);
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

}
