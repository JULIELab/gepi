package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ArrayFieldValue;
import de.julielab.jcore.consumer.es.FieldGenerationException;
import de.julielab.jcore.consumer.es.FieldValueGenerator;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.filter.Filter;
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

import java.util.Arrays;
import java.util.Optional;
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
        // Only a temporary solution: Set some rather arbitrary ID to family names so we can create relation documents from them
        // without causing exceptions. Family event partners can then be found in A-searches, at least
        setMockIdToFamilies(rel);
        for (int i = 0; i < allArguments.size() - 1; ++i) {
            for (int j = i + 1; j < allArguments.size(); ++j) {
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

                FSArray arg1ResourceEntries = ((Gene) ((ArgumentMention) argPair[0]).getRef()).getResourceEntryList();
                FSArray arg2ResourceEntries = ((Gene) ((ArgumentMention) argPair[1]).getRef()).getResourceEntryList();
                // iterate over all combinations of multiple IDs for the gene arguments
                for (int k = 0; k < arg1ResourceEntries.size() && arg1ResourceEntries.get(k) != null; ++k) {
                    for (int l = 0; l < arg2ResourceEntries.size() && arg2ResourceEntries.get(l) != null; ++l) {
                        Document document = new Document();
                        try {
                            JCas jCas = rel.getCAS().getJCas();
                            String docId = getDocumentId(jCas);
                            FieldCreationUtils.addDocumentId(document, rel);
                            if (likelihood != null)
                                document.addField("likelihood", FieldCreationUtils.likelihoodValues.get(likelihood.getLikelihood()));
                            String id = docId + "_" + rel.getId() + "_" + i + "." + k + "_" + j + "." + l;
                            document.setId(id);
                            document.addField("id", id);
                            document.addField("source", docId.startsWith("PMC") ? "pmc" : "pubmed");
                            String arg1EntryIdPath = "/ref/resourceEntryList[" + k + "]/entryId";
                            String arg2EntryIdPath = "/ref/resourceEntryList[" + l + "]/entryId";
                            document.addField("arguments", createRawFieldValueForParallelAnnotations(argPair, new String[]{arg1EntryIdPath, arg2EntryIdPath}, new Filter[]{geneFb.gene2tid2atidAddonFilter, geneFb.gene2tid2atidAddonFilter}, null));
                            document.addField("argumentgeneids", createRawFieldValueForParallelAnnotations(argPair, new String[]{arg1EntryIdPath, arg2EntryIdPath}, null, null));
                            document.addField("argumentconceptids", createRawFieldValueForParallelAnnotations(argPair, new String[]{arg1EntryIdPath, arg2EntryIdPath}, new Filter[]{geneFb.eg2tidReplaceFilter, geneFb.eg2tidReplaceFilter}, null));
                            document.addField("argumenttophomoids", createRawFieldValueForParallelAnnotations(argPair, new String[]{arg1EntryIdPath, arg2EntryIdPath}, new Filter[]{geneFb.eg2tophomoFilter, geneFb.eg2tophomoFilter}, null));
                            document.addField("argumentfamplexids", createRawFieldValueForParallelAnnotations(argPair, new String[]{arg1EntryIdPath, arg2EntryIdPath}, new Filter[]{geneFb.eg2famplexFilter, geneFb.eg2famplexFilter}, null));
                            document.addField("argumenthgncgroupids", createRawFieldValueForParallelAnnotations(argPair, new String[]{arg1EntryIdPath, arg2EntryIdPath}, new Filter[]{geneFb.eg2hgncFilter, geneFb.eg2hgncFilter}, null));
                            document.addField("argumentcoveredtext", createRawFieldValueForAnnotations(argPair, "/:coveredText()"));
                            document.addField("argumentprefnames", createRawFieldValueForParallelAnnotations(argPair, new String[]{arg1EntryIdPath, arg2EntryIdPath}, new Filter[]{geneFb.egid2prefNameReplaceFilter, geneFb.egid2prefNameReplaceFilter}, null));
                            document.addField("argumenthomoprefnames", createRawFieldValueForParallelAnnotations(argPair, new String[]{arg1EntryIdPath, arg2EntryIdPath}, new Filter[]{geneFb.egid2homoPrefNameReplaceFilter, geneFb.egid2homoPrefNameReplaceFilter}, null));
                            document.addField("argumentmatchtypes", Stream.of(argPair).map(ArgumentMention.class::cast).map(ArgumentMention::getRef).map(ConceptMention.class::cast).map(cm -> cm.getResourceEntryList(0).getConfidence() == null || cm.getResourceEntryList(0).getConfidence().contains("9999") ? "exact" : "fuzzy").toArray());
                            document.addField("argument1", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.gene2tid2atidAddonFilter));
                            document.addField("argument1geneid", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, null));
                            document.addField("argument1conceptid", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.eg2tidReplaceFilter));
                            document.addField("argument1tophomoid", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.eg2tophomoFilter));
                            document.addField("argument1famplexid", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.eg2famplexFilter));
                            document.addField("argument1hgncgroupid", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.eg2hgncFilter));
                            document.addField("argument1coveredtext", createRawFieldValueForAnnotation(argPair[0], "/:coveredText()", null));
                            document.addField("argument1prefname", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.egid2prefNameReplaceFilter));
                            document.addField("argument1homoprefname", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.egid2homoPrefNameReplaceFilter));
                            document.addField("argument1matchtype", Stream.of(argPair).map(ArgumentMention.class::cast).map(ArgumentMention::getRef).map(ConceptMention.class::cast).map(cm -> cm.getResourceEntryList(0).getConfidence() == null || cm.getResourceEntryList(0).getConfidence().contains("9999") ? "exact" : "fuzzy").toArray());
                            document.addField("argument2", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.gene2tid2atidAddonFilter));
                            document.addField("argument2geneid", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, null));
                            document.addField("argument2conceptid", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.eg2tidReplaceFilter));
                            document.addField("argument2tophomoid", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.eg2tophomoFilter));
                            document.addField("argument2famplexid", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.eg2famplexFilter));
                            document.addField("argument2hgncgroupid", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.eg2hgncFilter));
                            document.addField("argument2coveredtext", createRawFieldValueForAnnotation(argPair[1], "/:coveredText()", null));
                            document.addField("argument2prefname", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.egid2prefNameReplaceFilter));
                            document.addField("argument2homoprefname", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.egid2homoPrefNameReplaceFilter));
                            document.addField("argument2matchtype", Stream.of(argPair).map(ArgumentMention.class::cast).map(ArgumentMention::getRef).map(ConceptMention.class::cast).map(cm -> cm.getResourceEntryList(0).getConfidence() == null || cm.getResourceEntryList(0).getConfidence().contains("9999") ? "exact" : "fuzzy").toArray());
                            document.addField("maineventtype", createRawFieldValueForAnnotation(rel.getRootRelation(), "/specificType", null));
                            document.addField("alleventtypes", Stream.of(rel.getRelations().toArray()).map(EventMention.class::cast).map(EventMention::getSpecificType).collect(Collectors.toSet()).toArray());
                            document.addField("containsfamily", Stream.of(argPair).map(ArgumentMention.class::cast).map(ArgumentMention::getRef).map(ConceptMention.class::cast).map(ConceptMention::getSpecificType).anyMatch(st -> "FamilyName".equals(st) || "protein_familiy_or_group".equals(st)));
                            // reduce the short to the last element to make things a bit shorter (often, the component IDs are the fully qualified Java class name)
                            document.addField("relationsource", reduceToLastDottedPathElement(rel.getRootRelation().getComponentId()));
                            document.addField("genemappingsource", reduceToLastDottedPathElement(((Gene) ((ArgumentMention) argPair[0]).getRef()).getResourceEntryList(0).getComponentId()));
                            // An older version of the GNormPlus BioC Format reader did not set the Gene#componentId feature, so fall back to the resource entry, if necessary
                            document.addField("genesource", Optional.ofNullable(reduceToLastDottedPathElement(((ArgumentMention) argPair[0]).getRef().getComponentId()))
                                    .orElse(document.get("genemappingsource") != null ? document.get("genemappingsource").toString() : null));
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
            }
        }
        return relDocs;
    }

    private String reduceToLastDottedPathElement(String dottedPath) {
        if (dottedPath == null)
            return dottedPath;
        String[] split = dottedPath.split("\\.");
        return split[split.length - 1];
    }

    /**
     * temporary method to integrate family names into event documents as long as we don't have an actual family name mapping
     *
     * @param rel
     * @throws CASException
     */
    // TODO after the mapping is available, merge this with existing mappings.
    // Most family names won't actually have a mapping, for those continue to use the covered text; but we should
    // normalize it a bit (lowercase, remove punctuation) and do the same thing when we search for names
    // in GePi
    private void setMockIdToFamilies(FlattenedRelation rel) throws FieldGenerationException {
        try {
            for (FeatureStructure fs : rel.getArguments()) {
                ArgumentMention am = (ArgumentMention) fs;
                Gene gene = (Gene) am.getRef();
                if ("FamilyName".equals(gene.getSpecificType()) || "protein_familiy_or_group".equals(gene.getSpecificType())) {
                    FSArray resourceEntryList = new FSArray(rel.getCAS().getJCas(), 1);
                    ResourceEntry resourceEntry = new ResourceEntry(rel.getCAS().getJCas(), gene.getBegin(), gene.getEnd());
                    // in lack of something better at the moment
                    resourceEntry.setEntryId(gene.getCoveredText());
                    resourceEntryList.set(0, resourceEntry);
                    gene.setResourceEntryList(resourceEntryList);
                }
            }
        } catch (CASException e) {
            throw new FieldGenerationException(e);
        }
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
