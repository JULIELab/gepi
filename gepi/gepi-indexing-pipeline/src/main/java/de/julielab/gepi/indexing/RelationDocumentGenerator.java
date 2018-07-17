package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.DocumentGenerator;
import de.julielab.jcore.consumer.es.FieldGenerationException;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;

import java.util.List;

public class RelationDocumentGenerator extends DocumentGenerator {
    public RelationDocumentGenerator(FilterRegistry filterRegistry) {
        super(filterRegistry);
    }

    @Override
    public List<Document> createDocuments(JCas jCas) throws CASException, FieldGenerationException {
        return null;
    }
}
