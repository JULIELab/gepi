package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ArrayFieldValue;
import de.julielab.jcore.consumer.es.FieldGenerationException;
import de.julielab.jcore.consumer.es.FieldValueGenerator;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.types.ArgumentMention;
import de.julielab.jcore.types.ConceptMention;
import de.julielab.jcore.types.LikelihoodIndicator;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.stream.Stream;

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
     * To be called from {@link RelationFieldValueGenerator}. Creates the argument pair documents from a passed {@link FlattenedRelation} instance.
     *
     * @param fs The {@link de.julielab.jcore.types.ext.FlattenedRelation} to create an index document for.
     * @return
     * @throws FieldGenerationException
     */
    @Override
    public IFieldValue generateFieldValue(FeatureStructure fs) throws FieldGenerationException {
        ArrayFieldValue relDocs = new ArrayFieldValue();
        FlattenedRelation rel = (FlattenedRelation) fs;
        FSArray allArguments = rel.getArguments();
        LikelihoodIndicator likelihood = rel.getRootRelation().getLikelihood();
        for (int i = 0; i < allArguments.size() - 1; ++i) {
            for (int j = i + 1; j < allArguments.size(); ++j) {
                Document document = new Document();

                FeatureStructure[] argPair = new FeatureStructure[]{allArguments.get(i), allArguments.get(j)};
                try {
                    JCas jCas = rel.getCAS().getJCas();
                    String docId = JCoReTools.getDocId(jCas);
                    FieldCreationUtils.addDocumentId(document, rel);
                    if (likelihood != null)
                        document.addField("likelihood", FieldCreationUtils.likelihoodValues.get(likelihood.getLikelihood()));
                    String id = docId + "_" + rel.getId() + "_" + i + "_" + j;
                    document.setId(id);
                    document.addField("id", id);
                    document.addField("arguments", createRawFieldValueForAnnotations(argPair, "/ref/resourceEntryList/entryId", geneFb.gene2tid2atidAddonFilter));
                    document.addField("argumentgeneids", createRawFieldValueForAnnotations(argPair, "/ref/resourceEntryList/entryId"));
                    document.addField("argumentconceptids", createRawFieldValueForAnnotations(argPair, "/ref/resourceEntryList/entryId", geneFb.eg2tidReplaceFilter));
                    document.addField("argumenttophomoids", createRawFieldValueForAnnotations(argPair, "/ref/resourceEntryList/entryId", geneFb.eg2tophomoFilter));
                    document.addField("argumentcoveredtext", createRawFieldValueForAnnotations(argPair, "/:coveredText()"));
                    document.addField("argumentprefnames", createRawFieldValueForAnnotations(argPair, "/ref/resourceEntryList/entryId", geneFb.egid2prefNameReplaceFilter));
                    document.addField("argumenthomoprefnames", createRawFieldValueForAnnotations(argPair, "/ref/resourceEntryList/entryId", geneFb.egid2homoPrefNameReplaceFilter));
                    document.addField("argumentmatchtypes", Stream.of(argPair).map(ArgumentMention.class::cast).map(ArgumentMention::getRef).map(ConceptMention.class::cast).map(cm -> cm.getConfidence().contains("9999") ? "exact" : "fuzzy").toArray());
                    document.addField("maineventtype", createRawFieldValueForAnnotation(rel.getRootRelation(), "/specificType", null));
                    document.addField("ARGUMENT_FS", argPair);

                    relDocs.add(document);
                } catch (CASException e) {
                    throw new FieldGenerationException(e);
                }
            }
        }
        return relDocs;
    }
}
