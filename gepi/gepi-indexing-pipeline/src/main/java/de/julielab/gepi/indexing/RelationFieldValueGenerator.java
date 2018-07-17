package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.*;
import de.julielab.jcore.consumer.es.filter.FilterChain;
import de.julielab.jcore.consumer.es.filter.UniqueFilter;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedToken;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.LikelihoodIndicator;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;
import java.util.List;

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


    private final RelationFilterBoard relationFb;
    private final TextFilterBoard textFb;
    private final FilterChain eventName2tid2atidAddonFilter;
    private final GeneFilterBoard geneFb;

    public RelationFieldValueGenerator(FilterRegistry filterRegistry) {
        super(filterRegistry);
        relationFb = filterRegistry.getFilterBoard(RelationFilterBoard.class);
        textFb = filterRegistry.getFilterBoard(TextFilterBoard.class);
        geneFb = filterRegistry.getFilterBoard(GeneFilterBoard.class);
        eventName2tid2atidAddonFilter = new FilterChain(relationFb.eventName2tidAddonFilter, textFb.tid2atidAddonFilter);
    }

    /**
     * @param fs The {@link Sentence} to get overlapped {@link de.julielab.jcore.types.ext.FlattenedRelation} instances from.
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
            document.addField("id", docId + "_" + rel.getId());
            document.addField("allarguments", createRawFieldValueForAnnotations(rel.getArguments().toArray(), "/ref/resourceEntryList/entryId", geneFb.gene2tid2atidAddonFilter));
            document.addField("alleventtypes", createRawFieldValueForAnnotations(rel.getRelations().toArray(), "/specificType", eventName2tid2atidAddonFilter));
            document.addField("maineventtype", createRawFieldValueForAnnotation(rel.getRootRelation(), "/specificType", eventName2tid2atidAddonFilter));


        } catch (CASException e) {
            throw new FieldGenerationException(e);
        }
        return null;
    }
}
