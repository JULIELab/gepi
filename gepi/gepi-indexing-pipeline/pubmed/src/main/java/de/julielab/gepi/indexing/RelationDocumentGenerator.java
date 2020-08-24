package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.*;
import de.julielab.jcore.consumer.es.filter.ConstantOutputFilter;
import de.julielab.jcore.consumer.es.filter.FilterChain;
import de.julielab.jcore.consumer.es.filter.UniqueFilter;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedToken;
import de.julielab.jcore.consumer.es.preanalyzed.RawToken;
import de.julielab.jcore.types.ArgumentMention;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;

/**
 * Main class to create index documents for events, i.e. {@link FlattenedRelation} objects.
 */
public class RelationDocumentGenerator extends DocumentGenerator {

    private final RelationFieldValueGenerator relationFieldValueGenerator;
    private final TextFilterBoard textFb;
    private final GeneFilterBoard geneFb;
    private final ConstantOutputFilter constantTriggerFilter;
//    private final ConstantOutputFilter constantArgumentFilter;

    public RelationDocumentGenerator(FilterRegistry filterRegistry) {
        super(filterRegistry);
        relationFieldValueGenerator = new RelationFieldValueGenerator(filterRegistry);
        textFb = filterRegistry.getFilterBoard(TextFilterBoard.class);
        geneFb = filterRegistry.getFilterBoard(GeneFilterBoard.class);
        constantTriggerFilter = new ConstantOutputFilter("#trigger#");
//        constantArgumentFilter = new ConstantOutputFilter("#argument#");
    }

    @Override
    public List<Document> createDocuments(JCas jCas) throws FieldGenerationException {
        List<Document> relDocs = new ArrayList<>();
        Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);
        String docId = JCoReTools.getDocId(jCas);
        try {
            int i = 0;
            for (Sentence sentence : sentences) {
                FSIterator<Annotation> subiterator = jCas.getAnnotationIndex(FlattenedRelation.type).subiterator(sentence);
                while (subiterator.hasNext()) {
                    FlattenedRelation rel = (FlattenedRelation) subiterator.next();
                    if (rel.getArguments().size() > 1) {
                        ArrayFieldValue relationPairDocuments = (ArrayFieldValue) relationFieldValueGenerator.generateFieldValue(rel);
                        for (IFieldValue fv : relationPairDocuments) {
                            Document relDoc = (Document) fv;
                            FeatureStructure[] argPair = ((ArrayFieldValue) relDoc.get("ARGUMENT_FS")).stream().map(RawToken.class::cast).map(t -> (FeatureStructure) t.getTokenValue()).toArray(FeatureStructure[]::new);
                            relDoc.remove("ARGUMENT_FS");
                            // We create the sentence as a document of its own. In the mapping we then could add it as
                            // an object or as a nested document. There is no need to make it a nested document so we will
                            // use the object mapping which performs better.
                            Document sentenceDocument = new Document(docId + "_" + sentence.getId());
                            FeaturePathSets featurePathSets = new FeaturePathSets();
                            featurePathSets.add(new FeaturePathSet(Token.type, Arrays.asList("/:coveredText()"), null, textFb.textTokensFilter));
                            featurePathSets.add(new FeaturePathSet(Gene.type, Arrays.asList("/resourceEntryList/entryId"), null, new FilterChain(geneFb.gene2tid2atidAddonFilter, new UniqueFilter())));
                            featurePathSets.add(new FeaturePathSet(FlattenedRelation.type, Arrays.asList("/rootRelation/specificType"), null, constantTriggerFilter));
//                            featurePathSets.add(new FeaturePathSet(ArgumentMention.type, Arrays.asList("/begin"), null, constantArgumentFilter));
                            List<PreanalyzedToken> tokens = relationFieldValueGenerator.getTokensForAnnotationIndexes(featurePathSets, null, false, PreanalyzedToken.class, sentence, null, jCas);
                            // We only want the special highlighting term #argument# for the actual two arguments of the
                            // current relation. Thus we need to interlace the argument terms with the sentence terms.
                            addArgumentTokens(tokens, argPair);
                            // First sort by offset. For equal offsets, put the tokens with positionIncrement == 1 first.
                            Collections.sort(tokens, Comparator.<PreanalyzedToken>comparingInt(t -> t.start).thenComparing(t -> t.positionIncrement, Comparator.reverseOrder()));

                            sentenceDocument.addField("text", relationFieldValueGenerator.createPreanalyzedFieldValue(sentence.getCoveredText(), tokens));
                            sentenceDocument.addField("id", docId + "_" + (sentence.getId() != null ? sentence.getId() : i));
                            sentenceDocument.addField("likelihood", FieldCreationUtils.getMeanLikelihood(sentence));

                            relDoc.addField("sentence", sentenceDocument);
                            relDocs.add(relDoc);
                        }
                    }
                }
                ++i;
            }
        } catch (CASException e) {
            throw new FieldGenerationException(e);
        }
        return relDocs;
    }

    private void addArgumentTokens(List<PreanalyzedToken> tokens, FeatureStructure[] argPair) {
        ArgumentMention arg1 = (ArgumentMention) argPair[0];
        ArgumentMention arg2 = (ArgumentMention) argPair[1];

        PreanalyzedToken token1 = new PreanalyzedToken();
        token1.start = arg1.getBegin();
        token1.end = arg1.getEnd();
        token1.positionIncrement = 0;
        token1.term = "#argument#";

        PreanalyzedToken token2 = new PreanalyzedToken();
        token2.start = arg2.getBegin();
        token2.end = arg2.getEnd();
        token2.positionIncrement = 0;
        token2.term = "#argument#";

        tokens.add(token1);
        tokens.add(token2);
    }
}
