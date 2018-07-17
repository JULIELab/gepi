package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.DocumentGenerator;
import de.julielab.jcore.consumer.es.FieldGenerationException;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.types.Sentence;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SentenceDocumentGenerator extends DocumentGenerator {

    private final SentenceFieldValueGenerator sentenceFieldValueGenerator;

    public SentenceDocumentGenerator(FilterRegistry filterRegistry) {
        super(filterRegistry);
        sentenceFieldValueGenerator = new SentenceFieldValueGenerator(filterRegistry);
    }

    @Override
    public List<Document> createDocuments(JCas aJCas) throws CASException, FieldGenerationException {
        Collection<Sentence> sentences = JCasUtil.select(aJCas, Sentence.class);
        List<Document> sentenceDocuments = new ArrayList<>(sentences.size());
        for (Sentence sentence : sentences) {
            Document sentenceDocument = (Document) sentenceFieldValueGenerator.generateFieldValue(sentence);
            sentenceDocuments.add(sentenceDocument);
        }
        return sentenceDocuments;
    }
}
