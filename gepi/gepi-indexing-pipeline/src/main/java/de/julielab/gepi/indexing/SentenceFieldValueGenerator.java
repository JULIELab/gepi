package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.*;
import de.julielab.jcore.consumer.es.filter.FilterChain;
import de.julielab.jcore.consumer.es.filter.LowerCaseFilter;
import de.julielab.jcore.consumer.es.filter.SnowballFilter;
import de.julielab.jcore.consumer.es.filter.UniqueFilter;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedToken;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.types.pubmed.OtherID;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.jcore.utility.JCoReUtilitiesException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;
import java.util.List;

public class SentenceFieldValueGenerator extends FieldValueGenerator {

    private final GepiFilterBoard filterBoard;

    public SentenceFieldValueGenerator(FilterRegistry filterRegistry) {
        super(filterRegistry);
        filterBoard = filterRegistry.getFilterBoard(GepiFilterBoard.class);
    }

    @Override
    public IFieldValue generateFieldValue(FeatureStructure fs) throws FieldGenerationException {
        Document sentenceDocument = new Document();
        Sentence sentence = (Sentence) fs;
        FeaturePathSets featurePathSets = new FeaturePathSets();
        featurePathSets.add(new FeaturePathSet(Token.type, Arrays.asList("/:coveredText()"), null, filterBoard.textTokensFilter));
        featurePathSets.add(new FeaturePathSet(Gene.type, Arrays.asList("/resourceEntryList/entryId"), null, new FilterChain(filterBoard.geneDatabaseIdsToAggregatesFilter, new UniqueFilter())));
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
