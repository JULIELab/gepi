package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ArrayFieldValue;
import de.julielab.jcore.consumer.es.FieldGenerationException;
import de.julielab.jcore.consumer.es.FieldValueGenerator;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.RawToken;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.types.pubmed.OtherID;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.stream.Collectors;
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
                // Check if all arguments have been successfully mapped and if not, reject the argument pair
                boolean argumentWithoutId = false;
                for (FeatureStructure arg : argPair) {
                    ArgumentMention am = (ArgumentMention) arg;
                    if (am.getRef() == null) {
                        argumentWithoutId = true;
                        break;
                    }
                    ConceptMention cm = (ConceptMention) am.getRef();
                    if (cm.getResourceEntryList() == null) {
                        argumentWithoutId = true;
                        break;
                    }
                    if (cm.getResourceEntryList().size() == 0 || cm.getResourceEntryList().get(0) == null) {
                        argumentWithoutId = true;
                        break;
                    }
                    if (((ResourceEntry) cm.getResourceEntryList().get(0)).getEntryId().isBlank()) {
                        argumentWithoutId = true;
                        break;
                    }
                }
                if (argumentWithoutId)
                    continue;
                try {
                    JCas jCas = rel.getCAS().getJCas();
                    String docId = getDocumentId(jCas);
                    FieldCreationUtils.addDocumentId(document, rel);
                    if (likelihood != null)
                        document.addField("likelihood", FieldCreationUtils.likelihoodValues.get(likelihood.getLikelihood()));
                    String id = docId + "_" + rel.getId() + "_" + i + "_" + j;
                    document.setId(id);
                    document.addField("id", id);
                    document.addField("source", docId.startsWith("PMC") ? "pmc" : "pubmed");
                    document.addField("arguments", createRawFieldValueForAnnotations(argPair, "/ref/resourceEntryList/entryId", geneFb.gene2tid2atidAddonFilter));
                    document.addField("argumentgeneids", createRawFieldValueForAnnotations(argPair, "/ref/resourceEntryList/entryId"));
                    document.addField("argumentconceptids", createRawFieldValueForAnnotations(argPair, "/ref/resourceEntryList/entryId", geneFb.eg2tidReplaceFilter));
                    document.addField("argumenttophomoids", createRawFieldValueForAnnotations(argPair, "/ref/resourceEntryList/entryId", geneFb.eg2tophomoFilter));
                    document.addField("argumentcoveredtext", createRawFieldValueForAnnotations(argPair, "/:coveredText()"));
                    document.addField("argumentprefnames", createRawFieldValueForAnnotations(argPair, "/ref/resourceEntryList/entryId", geneFb.egid2prefNameReplaceFilter));
                    document.addField("argumenthomoprefnames", createRawFieldValueForAnnotations(argPair, "/ref/resourceEntryList/entryId", geneFb.egid2homoPrefNameReplaceFilter));
                    document.addField("argumentmatchtypes", Stream.of(argPair).map(ArgumentMention.class::cast).map(ArgumentMention::getRef).map(ConceptMention.class::cast).map(cm -> cm.getResourceEntryList(0).getConfidence().contains("9999") ? "exact" : "fuzzy").toArray());
                    document.addField("argument1", createRawFieldValueForAnnotation(argPair[0], "/ref/resourceEntryList/entryId", geneFb.gene2tid2atidAddonFilter));
                    document.addField("argument1geneid", createRawFieldValueForAnnotation(argPair[0], "/ref/resourceEntryList/entryId", null));
                    document.addField("argument1conceptid", createRawFieldValueForAnnotation(argPair[0], "/ref/resourceEntryList/entryId", geneFb.eg2tidReplaceFilter));
                    document.addField("argument1tophomoid", createRawFieldValueForAnnotation(argPair[0], "/ref/resourceEntryList/entryId", geneFb.eg2tophomoFilter));
                    document.addField("argument1coveredtext", createRawFieldValueForAnnotation(argPair[0], "/:coveredText()", null));
                    document.addField("argument1prefname", createRawFieldValueForAnnotation(argPair[0], "/ref/resourceEntryList/entryId", geneFb.egid2prefNameReplaceFilter));
                    document.addField("argument1homoprefname", createRawFieldValueForAnnotation(argPair[0], "/ref/resourceEntryList/entryId", geneFb.egid2homoPrefNameReplaceFilter));
                    document.addField("argument1matchtype", Stream.of(argPair).map(ArgumentMention.class::cast).map(ArgumentMention::getRef).map(ConceptMention.class::cast).map(cm -> cm.getResourceEntryList(0).getConfidence().contains("9999") ? "exact" : "fuzzy").toArray());
                    document.addField("argument2", createRawFieldValueForAnnotation(argPair[1], "/ref/resourceEntryList/entryId", geneFb.gene2tid2atidAddonFilter));
                    document.addField("argument2geneid", createRawFieldValueForAnnotation(argPair[1], "/ref/resourceEntryList/entryId", null));
                    document.addField("argument2conceptid", createRawFieldValueForAnnotation(argPair[1], "/ref/resourceEntryList/entryId", geneFb.eg2tidReplaceFilter));
                    document.addField("argument2tophomoid", createRawFieldValueForAnnotation(argPair[1], "/ref/resourceEntryList/entryId", geneFb.eg2tophomoFilter));
                    document.addField("argument2coveredtext", createRawFieldValueForAnnotation(argPair[1], "/:coveredText()", null));
                    document.addField("argument2prefname", createRawFieldValueForAnnotation(argPair[1], "/ref/resourceEntryList/entryId", geneFb.egid2prefNameReplaceFilter));
                    document.addField("argument2homoprefname", createRawFieldValueForAnnotation(argPair[1], "/ref/resourceEntryList/entryId", geneFb.egid2homoPrefNameReplaceFilter));
                    document.addField("argument2matchtype", Stream.of(argPair).map(ArgumentMention.class::cast).map(ArgumentMention::getRef).map(ConceptMention.class::cast).map(cm -> cm.getResourceEntryList(0).getConfidence().contains("9999") ? "exact" : "fuzzy").toArray());
                    document.addField("maineventtype", createRawFieldValueForAnnotation(rel.getRootRelation(), "/specificType", null));
                    document.addField("alleventtypes", Stream.of(rel.getRelations().toArray()).map(EventMention.class::cast).map(EventMention::getSpecificType).collect(Collectors.toSet()).toArray());
                    document.addField("ARGUMENT_FS", argPair);

                    // filter out reflexive events
                    if (((ArrayFieldValue) document.get("argumenttophomoids")).stream().map(RawToken.class::cast).map(RawToken::getTokenValue).distinct().count() == 2) {
                        relDocs.add(document);
                    }
                } catch (CASException e) {
                    throw new FieldGenerationException(e);
                }
            }
        }
        return relDocs;
    }

    private String getDocumentId(JCas jCas) {
        Header header = JCasUtil.selectSingle(jCas, Header.class);
        boolean ispmcDocument = false;
        FSArray otherIDs = header.getOtherIDs();
        if (otherIDs != null && otherIDs.size() > 0) {
            OtherID other = (OtherID) otherIDs.get(0);
            if (other.getSource().equals("PubMed"))
                ispmcDocument = true;
        }
        String docId = header.getDocId();
        if (ispmcDocument && !docId.startsWith("PMC"))
            docId = "PMC" + docId;
        return docId;
    }
}
