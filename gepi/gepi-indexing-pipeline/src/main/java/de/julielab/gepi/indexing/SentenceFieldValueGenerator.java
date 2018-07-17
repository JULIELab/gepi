package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.*;
import de.julielab.jcore.consumer.es.filter.FilterChain;
import de.julielab.jcore.consumer.es.filter.UniqueFilter;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedToken;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;
import java.util.List;

public class SentenceFieldValueGenerator extends FieldValueGenerator {

    private final GeneFilterBoard geneFb;
    private final TextFilterBoard textFb;

    public SentenceFieldValueGenerator(FilterRegistry filterRegistry) {
        super(filterRegistry);
        geneFb = filterRegistry.getFilterBoard(GeneFilterBoard.class);
        textFb = filterRegistry.getFilterBoard(TextFilterBoard.class);
    }

    @Override
    public IFieldValue generateFieldValue(FeatureStructure fs) throws FieldGenerationException {
        Document sentenceDocument = new Document();
        Sentence sentence = (Sentence) fs;
        FeaturePathSets featurePathSets = new FeaturePathSets();
        featurePathSets.add(new FeaturePathSet(Token.type, Arrays.asList("/:coveredText()"), null, textFb.textTokensFilter));
        featurePathSets.add(new FeaturePathSet(Gene.type, Arrays.asList("/resourceEntryList/entryId"), null, new FilterChain(geneFb.gene2tid2atidAddonFilter, new UniqueFilter())));
        try {
            JCas jCas = sentence.getCAS().getJCas();
            List<PreanalyzedToken> tokens = getTokensForAnnotationIndexes(featurePathSets, null, true, PreanalyzedToken.class, sentence, null, jCas);
            PreanalyzedFieldValue completeSentenceFieldValue = createPreanalyzedFieldValue(sentence.getCoveredText(), tokens);
            sentenceDocument.addField("sentence", completeSentenceFieldValue);

            sentenceDocument.addField("id", JCoReTools.getDocId(jCas) + "_" + sentence.getId());
            sentenceDocument.addField("likelihood", FieldCreationUtils.getMeanLikelihood(sentence));
            FieldCreationUtils.addDocumentId(sentenceDocument, sentence);
        } catch (CASException e) {
            throw new FieldGenerationException(e);
        }
        return sentenceDocument;
    }
}
