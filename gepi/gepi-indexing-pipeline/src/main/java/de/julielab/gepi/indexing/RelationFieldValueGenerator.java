package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.FieldGenerationException;
import de.julielab.jcore.consumer.es.FieldValueGenerator;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.filter.FilterChain;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.types.LikelihoodIndicator;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;

/**
 * "_parent": {
 * "type": "sentences"
 * },
 * "properties": {
 * "pmid": {
 * "type": "keyword",
 * "store": true
 * },
 * "pmcid": {
 * "type": "keyword",
 * "store": true
 * },
 * "allarguments": {
 * "type": "keyword",
 * "store": true
 * },
 * "maineventtype": {
 * "type": "keyword",
 * "store": true
 * },
 * "alleventtypes": {
 * "type": "keyword",
 * "store": true
 * },
 * "id": {
 * "type": "keyword",
 * "store": true
 * },
 * "likelihood": {
 * "type": "integer",
 * "store": true
 * },
 * "sentence_filtering": {
 * "type": "text",
 * "norms": {
 * "enabled": false
 * }
 * },
 * "sentenceid": {
 * "type": "keyword",
 * "store": true
 * }
 */
public class RelationFieldValueGenerator extends FieldValueGenerator {


    private final TextFilterBoard textFb;
    private final GeneFilterBoard geneFb;

    public RelationFieldValueGenerator(FilterRegistry filterRegistry) {
        super(filterRegistry);
        textFb = filterRegistry.getFilterBoard(TextFilterBoard.class);
        geneFb = filterRegistry.getFilterBoard(GeneFilterBoard.class);
    }

    /**
     * @param fs The {@link de.julielab.jcore.types.ext.FlattenedRelation} to create an index document for.
     * @return
     * @throws FieldGenerationException
     */
    @Override
    public IFieldValue generateFieldValue(FeatureStructure fs) throws FieldGenerationException {
        Document document = new Document();
        FlattenedRelation rel = (FlattenedRelation) fs;
        try {
            JCas jCas = rel.getCAS().getJCas();
            String docId = JCoReTools.getDocId(jCas);
            FieldCreationUtils.addDocumentId(document, rel);
            LikelihoodIndicator likelihood = rel.getRootRelation().getLikelihood();
            if (likelihood != null)
                document.addField("likelihood", FieldCreationUtils.likelihoodValues.get(likelihood.getLikelihood()));
            document.setId(docId + "_" + rel.getId());
            document.addField("id", docId + "_" + rel.getId());
            document.addField("numargs", rel.getArguments().size());
            document.addField("allarguments", createRawFieldValueForAnnotations(rel.getArguments().toArray(), "/ref/resourceEntryList/entryId", geneFb.gene2tid2atidAddonFilter));
            document.addField("alleventtypes", createRawFieldValueForAnnotations(rel.getRelations().toArray(), "/specificType"));
            document.addField("maineventtype", createRawFieldValueForAnnotation(rel.getRootRelation(), "/specificType", null));
        } catch (CASException e) {
            throw new FieldGenerationException(e);
        }
        return document;
    }
}
