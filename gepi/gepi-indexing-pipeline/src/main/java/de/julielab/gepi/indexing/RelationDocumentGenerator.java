package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.*;
import de.julielab.jcore.consumer.es.filter.FilterChain;
import de.julielab.jcore.consumer.es.filter.UniqueFilter;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedToken;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class RelationDocumentGenerator extends DocumentGenerator {

    private final RelationFieldValueGenerator relationFieldValueGenerator;
    private final TextFilterBoard textFb;
    private final GeneFilterBoard geneFb;
    private final RelationFilterBoard relationFb;
    private final FilterChain eventName2tid2atidAddonFilter;

    public RelationDocumentGenerator(FilterRegistry filterRegistry) {
        super(filterRegistry);
        relationFieldValueGenerator = new RelationFieldValueGenerator(filterRegistry);
        textFb = filterRegistry.getFilterBoard(TextFilterBoard.class);
        geneFb = filterRegistry.getFilterBoard(GeneFilterBoard.class);
        relationFb = filterRegistry.getFilterBoard(RelationFilterBoard.class);
        eventName2tid2atidAddonFilter = new FilterChain(relationFb.eventName2tidReplaceFilter, textFb.tid2atidAddonFilter);
    }

    @Override
    public List<Document> createDocuments(JCas jCas) throws FieldGenerationException {
        List<Document> relDocs = new ArrayList<>();
        Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);
        String docId = JCoReTools.getDocId(jCas);
        try {
            for (Sentence sentence : sentences) {
                FSIterator<Annotation> subiterator = jCas.getAnnotationIndex(FlattenedRelation.type).subiterator(sentence);
                while (subiterator.hasNext()) {
                    FlattenedRelation rel = (FlattenedRelation) subiterator.next();
                    Document relationDocument = (Document) relationFieldValueGenerator.generateFieldValue(rel);
                    relationDocument.setParentId(docId + "_" + sentence.getId());

                    FeaturePathSets featurePathSets = new FeaturePathSets();
                    featurePathSets.add(new FeaturePathSet(Token.type, Arrays.asList("/:coveredText()"), null, textFb.textTokensFilter));
                    featurePathSets.add(new FeaturePathSet(Gene.type, Arrays.asList("/resourceEntryList/entryId"), null, new FilterChain(geneFb.gene2tid2atidAddonFilter, new UniqueFilter())));
                    featurePathSets.add(new FeaturePathSet(FlattenedRelation.type, Arrays.asList("/rootRelation/specificType"), null, eventName2tid2atidAddonFilter));
                    List<PreanalyzedToken> tokens = relationFieldValueGenerator.getTokensForAnnotationIndexes(featurePathSets, null, true, PreanalyzedToken.class, sentence, null, jCas);
                    relationDocument.addField("sentence_filtering", relationFieldValueGenerator.createPreanalyzedFieldValue(null, tokens));
                    relationDocument.addField("sentenceid", docId + "_" + sentence.getId());
                    relDocs.add(relationDocument);
                }
            }
        } catch (CASException e) {
            throw new FieldGenerationException(e);
        }
        return relDocs;
    }
}
