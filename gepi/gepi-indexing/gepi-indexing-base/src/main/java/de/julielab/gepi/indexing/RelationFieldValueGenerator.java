package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ArrayFieldValue;
import de.julielab.jcore.consumer.es.FieldGenerationException;
import de.julielab.jcore.consumer.es.FieldValueGenerator;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.filter.*;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.RawToken;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.types.pubmed.OtherID;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.HashMap;
import java.util.Map;
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


    public static final String UNARY_EVENT_MOCK_TEXT = "none";
    public static final String DUMMY = "dummy";
    private final TextFilterBoard textFb;
    private final GeneFilterBoard geneFb;
    private final Filter geneComponentIdProcessingfilter;
    private final Filter lastDottedPathElementFilter;
    private final Filter uniqueNumberRegExFilter;
    private final Filter defaultTaxFilter;

    public RelationFieldValueGenerator(FilterRegistry filterRegistry) {
        super(filterRegistry);
        textFb = filterRegistry.getFilterBoard(TextFilterBoard.class);
        geneFb = filterRegistry.getFilterBoard(GeneFilterBoard.class);
        lastDottedPathElementFilter = new RegExReplaceFilter(".*\\.", "", false);
        Map<String, String> replaceMap = new HashMap<>();
        // In an older version of the GNormPlusFormatMultiplier, the GNormPlus genes were not given any componentId
        replaceMap.put(null, "GNormPlus");
        replaceMap.put("null", "GNormPlus");
        replaceMap.put("ExtendedProteinsMerger", "FlairNerAnnotator");
        replaceMap.put("ProteinConsistencyTagger", "FlairNerAnnotator");
        replaceMap.put("GeneMapper / QuercusMappingCore", "GeNo");
        replaceMap.put("GNormPlusFormatMultiplierReader", "GNormPlus");
        geneComponentIdProcessingfilter = new FilterChain(new RegExSplitFilter(","), lastDottedPathElementFilter, new ReplaceFilter(replaceMap), new UniqueFilter());
        uniqueNumberRegExFilter = new FilterChain(new RegExFilter("[0-9]+", false), new UniqueFilter());
        defaultTaxFilter = new FilterChain(geneFb.egid2taxidReplaceFilter, new ReplaceNullFilter("9606", true));
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
        JCas jCas;
        ArgumentMention mockArgument;
        try {
            jCas = rel.getCAS().getJCas();
        } catch (CASException e) {
            throw new FieldGenerationException(e);
        }
        //  Set some rather arbitrary ID to family names so we can create relation documents from them
        // without causing exceptions. Family event partners can then be found in A-searches, at least
        setMockIdToFamilies(rel);
        for (int i = 0; i < allArguments.size(); ++i) {
            for (int j = i; j < allArguments.size(); ++j) {
                FeatureStructure[] argPair;
                // is this a unary event?
                if (j == i && allArguments.size() == 1)
                    argPair = new FeatureStructure[]{allArguments.get(i), getMockArgument(jCas, (Annotation) allArguments.get(i))};
                else if (j > i)
                    argPair = new FeatureStructure[]{allArguments.get(i), allArguments.get(j)};
                else
                    continue;

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


                final Gene arg1Gene = (Gene) ((ArgumentMention) argPair[0]).getRef();
                final Gene arg2Gene = (Gene) ((ArgumentMention) argPair[1]).getRef();
                // An older version of the GNormPlus BioC Format reader did not set the Gene#componentId feature, so fall back to the resource entry, if necessary
                if (arg1Gene.getComponentId() == null)
                    arg1Gene.setComponentId("GNormPlus");
                if (arg2Gene.getComponentId() == null)
                    arg2Gene.setComponentId("GNormPlus");
                FSArray arg1ResourceEntries = arg1Gene.getResourceEntryList();
                FSArray arg2ResourceEntries = arg2Gene.getResourceEntryList();
                // iterate over all combinations of multiple IDs for the gene arguments
                for (int k = 0; k < arg1ResourceEntries.size() && arg1ResourceEntries.get(k) != null; ++k) {
                    for (int l = 0; l < arg2ResourceEntries.size() && arg2ResourceEntries.get(l) != null; ++l) {
                        Document document = new Document();
                        try {
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
                            document.addField("argument1", createRawFieldValueForParallelAnnotations(new FeatureStructure[]{argPair[0], argPair[0], argPair[0], argPair[0], argPair[0]}, new String[]{arg1EntryIdPath, arg1EntryIdPath, arg1EntryIdPath, arg1EntryIdPath, arg1EntryIdPath}, new Filter[]{geneFb.orgid2tid2atidAddonFilter, geneFb.eg2famplexFilter, geneFb.eg2hgncFilter, geneFb.eg2gohypertidFilter, geneFb.orgid2equalnameatidReplaceFilter}, new UniqueFilter()));
//                            document.addField("argument1geneid", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, null));
//                            document.addField("argument1taxid", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.egid2taxidReplaceFilter));
//                            document.addField("argument1conceptid", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.eg2tidReplaceFilter));
//                            document.addField("argument1tophomoid", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.eg2tophomoFilter));
//                            document.addField("argument1famplexid", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.eg2famplexFilter));
//                            document.addField("argument1hgncgroupid", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.eg2hgncFilter));
//                            document.addField("argument1goids", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.eg2gotidFilter));
//                            document.addField("argument1coveredtext", createRawFieldValueForAnnotation(argPair[0], "/:coveredText()", null));
//                            document.addField("argument1prefname", createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.egid2prefNameReplaceFilter));
                            final IFieldValue arg1HomoPrefNameValue = createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.orgid2topaggprefname);
//                            document.addField("argument1homoprefname", arg1HomoPrefNameValue);
                            document.addField("argument1homoprefnameaggvalue", arg1HomoPrefNameValue);
//                            final IFieldValue arg1GoPrefnames = createRawFieldValueForAnnotation(argPair[0], arg1EntryIdPath, geneFb.eg2goprefnameFilter);
//                            document.addField("argument1goprefnames", arg1GoPrefnames);
//                            document.addField("argument1matchtype", Stream.of(argPair).map(ArgumentMention.class::cast).map(ArgumentMention::getRef).map(ConceptMention.class::cast).map(cm -> cm.getResourceEntryList(0).getConfidence() == null || cm.getResourceEntryList(0).getConfidence().contains("9999") ? "exact" : "fuzzy").toArray());
//                            document.addField("argument1genesource", createRawFieldValueForAnnotation(argPair[0], "/ref/componentId", geneComponentIdProcessingfilter));
//                            document.addField("argument1genemappingsource", createRawFieldValueForAnnotation(argPair[0], "/ref/resourceEntryList[" + k + "]/componentId", geneComponentIdProcessingfilter));
                            document.addField("argument2", createRawFieldValueForParallelAnnotations(new FeatureStructure[]{argPair[1], argPair[1], argPair[1], argPair[1], argPair[1]}, new String[]{arg2EntryIdPath, arg2EntryIdPath, arg2EntryIdPath, arg2EntryIdPath, arg2EntryIdPath}, new Filter[]{geneFb.orgid2tid2atidAddonFilter, geneFb.eg2famplexFilter, geneFb.eg2hgncFilter, geneFb.eg2gohypertidFilter, geneFb.orgid2equalnameatidReplaceFilter}, new UniqueFilter()));
//                            document.addField("argument2geneid", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, null));
//                            document.addField("argument2taxid", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.egid2taxidReplaceFilter));
//                            document.addField("argument2conceptid", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.eg2tidReplaceFilter));
//                            document.addField("argument2tophomoid", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.eg2tophomoFilter));
//                            document.addField("argument2famplexid", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.eg2famplexFilter));
//                            document.addField("argument2hgncgroupid", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.eg2hgncFilter));
//                            document.addField("argument2goids", createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.eg2gotidFilter));
//                            document.addField("argument2coveredtext", createRawFieldValueForAnnotation(argPair[1], "/:coveredText()", null));
//                            document.addField("argument2prefname", createRawFieldValueForFieldValue(document.getAsRawToken("argument2conceptid"), geneFb.conceptid2prefNameFilter));
                            final IFieldValue arg2HomoPrefNameValue = createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.orgid2topaggprefname);
//                            document.addField("argument2homoprefname", arg2HomoPrefNameValue);
                            document.addField("argument2homoprefnameaggvalue", arg2HomoPrefNameValue);
//                            final IFieldValue arg2GoPrefnames = createRawFieldValueForAnnotation(argPair[1], arg2EntryIdPath, geneFb.eg2goprefnameFilter);
//                            document.addField("argument2goprefnames", arg2GoPrefnames);
//                            document.addField("argument2matchtype", Stream.of(argPair).map(ArgumentMention.class::cast).map(ArgumentMention::getRef).map(ConceptMention.class::cast).map(cm -> cm.getResourceEntryList(0).getConfidence() == null || cm.getResourceEntryList(0).getConfidence().contains("9999") ? "exact" : "fuzzy").toArray());
//                            document.addField("argument2genesource", createRawFieldValueForAnnotation(argPair[1], "/ref/componentId", geneComponentIdProcessingfilter));
//                            document.addField("argument2genemappingsource", createRawFieldValueForAnnotation(argPair[1], "/ref/resourceEntryList[" + k + "]/componentId", geneComponentIdProcessingfilter));

                            // maps genes to orthologs and gene ontology terms they are annotated with and their families, complexes and groups (everything "bottom-up" where the genes are the bottom)
                            document.addField("argumentsfamiliesgroups", createRawFieldValueForParallelAnnotations(new FeatureStructure[]{argPair[0], argPair[1], argPair[0], argPair[1], argPair[0], argPair[1], argPair[0], argPair[1], argPair[0], argPair[1]}, new String[]{arg1EntryIdPath, arg2EntryIdPath, arg1EntryIdPath, arg2EntryIdPath, arg1EntryIdPath, arg2EntryIdPath, arg1EntryIdPath, arg2EntryIdPath, arg1EntryIdPath, arg2EntryIdPath}, new Filter[]{geneFb.orgid2tid2atidAddonFilter, geneFb.orgid2tid2atidAddonFilter, geneFb.eg2famplexFilter, geneFb.eg2famplexFilter, geneFb.eg2hgncFilter, geneFb.eg2hgncFilter, geneFb.eg2gohypertidFilter, geneFb.eg2gohypertidFilter, geneFb.orgid2equalnameatidReplaceFilter, geneFb.orgid2equalnameatidReplaceFilter}, new UniqueFilter()));
                            // maps to orthologs and gene ontology terms but not to families, complexes and groups; if a family was found directly, it is included, but genes won't add the families they belong to. When searching for a family, this field won't return the family members.
                            document.addField("argumentsnoinferencefamiliesgroups", createRawFieldValueForParallelAnnotations(new FeatureStructure[]{argPair[0], argPair[1], argPair[0], argPair[1], argPair[0], argPair[1]}, new String[]{arg1EntryIdPath, arg2EntryIdPath, arg1EntryIdPath, arg2EntryIdPath, arg1EntryIdPath, arg2EntryIdPath}, new Filter[]{geneFb.orgid2tid2atidAddonFilter, geneFb.orgid2tid2atidAddonFilter, geneFb.eg2gohypertidFilter, geneFb.eg2gohypertidFilter, geneFb.orgid2equalnameatidReplaceFilter, geneFb.orgid2equalnameatidReplaceFilter}, new UniqueFilter()));
                            // maps to orthologs and gene ontology terms but not to families, plus for complexes, the IDS of their subunits is added ("top-down"
                            document.addField("argumentscomplexes2members", createRawFieldValueForParallelAnnotations(new FeatureStructure[]{argPair[0], argPair[1], argPair[0], argPair[1], argPair[0], argPair[1], argPair[0], argPair[1]}, new String[]{arg1EntryIdPath, arg2EntryIdPath, arg1EntryIdPath, arg2EntryIdPath, arg1EntryIdPath, arg2EntryIdPath, arg1EntryIdPath, arg2EntryIdPath}, new Filter[]{geneFb.orgid2tid2atidAddonFilter, geneFb.orgid2tid2atidAddonFilter, geneFb.complextid2membertidAddonFilter, geneFb.complextid2membertidAddonFilter, geneFb.eg2gohypertidFilter, geneFb.eg2gohypertidFilter, geneFb.orgid2equalnameatidReplaceFilter, geneFb.orgid2equalnameatidReplaceFilter}, new UniqueFilter()));
                            // add everything from the above into one field
                            ArrayFieldValue allArgumentsValues = new ArrayFieldValue();
                            allArgumentsValues.addFlattened(document.getAsArrayFieldValue("argumentsfamiliesgroups"));
                            allArgumentsValues.addFlattened(document.getAsArrayFieldValue("argumentsnoinferencefamiliesgroups"));
                            allArgumentsValues.addFlattened(document.getAsArrayFieldValue("argumentscomplexes2members"));
                            document.addField("arguments", createRawFieldValueForFieldValue(allArgumentsValues, new UniqueFilter()));
                            final String[] entryIdPathPair = {arg1EntryIdPath, arg2EntryIdPath};
                            document.addField("argumentgeneids", createRawFieldValueForParallelAnnotations(argPair, entryIdPathPair, null, null));
                            document.addField("argumenttaxids", createRawFieldValueForParallelAnnotations(new FeatureStructure[]{argPair[0], argPair[1], argPair[0], argPair[1]}, new String[]{arg1EntryIdPath, arg2EntryIdPath, "/ref/species", "/ref/species"}, new Filter[]{geneFb.egid2taxidReplaceFilter, geneFb.egid2taxidReplaceFilter, defaultTaxFilter, defaultTaxFilter}, uniqueNumberRegExFilter));
                            // Add the tax IDs given to FamilyNames. This a bit tricky because the document won't
                            // store empty values so we need to be careful with null values.
//                            ArrayFieldValue argumenttaxids = document.getAsArrayFieldValue("argumenttaxids");
//                            boolean argumenttaxidsNull = false;
//                            if (argumenttaxids == null) {
//                                argumenttaxidsNull = true;
//                                argumenttaxids = new ArrayFieldValue();
//                            }
//                            argumenttaxids.addFlattened(createRawFieldValueForParallelAnnotations(argPair, new String[]{"/ref/species", "/ref/species"}, null, numberRegExFilter));
//                            if (argumenttaxidsNull)
//                                document.addField("argumenttaxids", argumenttaxids);
                            document.addField("argumentconceptids", createRawFieldValueForParallelAnnotations(argPair, entryIdPathPair, new Filter[]{geneFb.orgid2atidReplaceFilter, geneFb.orgid2atidReplaceFilter}, null));
                            document.addField("argumenttophomoids", createRawFieldValueForParallelAnnotations(argPair, entryIdPathPair, new Filter[]{geneFb.orgid2topaggFilter, geneFb.orgid2topaggFilter}, null));
//                            document.addField("argumentfamplexids", document.getAsArrayFieldValue("argument1famplexid"), document.getAsArrayFieldValue("argument2famplexid"));
//                            document.addField("argumenthgncgroupids", document.getAsArrayFieldValue("argument1hgncgroupid"), document.getAsArrayFieldValue("argument2hgncgroupid"));
                            document.addField("argumentgoids", createRawFieldValueForParallelAnnotations(argPair, entryIdPathPair, new Filter[]{geneFb.eg2gotidFilter, geneFb.eg2gotidFilter}, null));
                            document.addField("argumentcoveredtext", createRawFieldValueForParallelAnnotations(argPair, new String[]{"/:coveredText()", "/:coveredText()"}, null, null));
                            document.addField("argumentprefnames", createRawFieldValueForFieldValue(document.getAsArrayFieldValue("argumentconceptids"), geneFb.conceptid2prefNameFilter));
                            document.addField("argumenthomoprefnames", createRawFieldValueForParallelAnnotations(argPair, entryIdPathPair, new Filter[]{geneFb.orgid2topaggprefname, geneFb.orgid2topaggprefname}, null));
//                            document.addField("argumentconceptprefnames", createRawFieldValueForParallelAnnotations(argPair, new String[]{arg1EntryIdPath, arg2EntryIdPath}, new Filter[]{geneFb.eg2top, geneFb.egid2homoPrefNameReplaceFilter}, null));
//                            document.addField("argumentmatchtypes", Stream.of(argPair).map(ArgumentMention.class::cast).map(ArgumentMention::getRef).map(ConceptMention.class::cast).map(cm -> cm.getResourceEntryList(0).getConfidence() == null || cm.getResourceEntryList(0).getConfidence().contains("9999") ? "exact" : "fuzzy").toArray());

                            document.addField("maineventtype", createRawFieldValueForAnnotation(rel.getRootRelation(), "/specificType", null));
                            document.addField("alleventtypes", Stream.of(rel.getRelations().toArray()).map(EventMention.class::cast).map(EventMention::getSpecificType).collect(Collectors.toSet()).toArray());
                            document.addField("containsfamily", Stream.of(argPair).map(ArgumentMention.class::cast).map(ArgumentMention::getRef).map(ConceptMention.class::cast).map(ConceptMention::getSpecificType).anyMatch(st -> "FamilyName".equals(st) || "protein_familiy_or_group".equals(st)));
                            // reduce the short to the last element to make things a bit shorter (often, the component IDs are the fully qualified Java class name)
                            document.addField("relationsource", rel.getRootRelation().getComponentId());
                            document.addField("genemappingsource", createRawFieldValueForParallelAnnotations(argPair, new String[]{"/ref/resourceEntryList[" + k + "]/componentId", "/ref/resourceEntryList[" + l + "]/componentId"}, null, geneComponentIdProcessingfilter));
                            document.addField("genesource", createRawFieldValueForAnnotations(argPair, new String[]{"/ref/componentId"}, null, geneComponentIdProcessingfilter));
                            document.addField("mixedgenesource", !arg1Gene.getComponentId().equals(arg2Gene.getComponentId()));
                            document.addField("mixedgenemappingsource", !arg1Gene.getResourceEntryList(k).getComponentId().equals(arg2Gene.getResourceEntryList(l).getComponentId()));
                            try {
                                final Date pubDate = JCasUtil.selectSingle(jCas, Header.class).getPubTypeList(0).getPubDate();
                                String month = pubDate.getMonth() < 10 ? "0"+pubDate.getMonth() : String.valueOf(pubDate.getMonth());
                                String day = pubDate.getDay() < 10 ? "0"+pubDate.getDay() : String.valueOf(pubDate.getDay());
                                final String value = pubDate.getYear() + "-" + month + "-" + day;
                                document.addField("pubdate", value);
                            } catch (Exception e) {
                                log.warn("Could not obtain publication date for document {}", docId, e);
                                throw e;
                            }
                            document.addField("ARGUMENT_FS", argPair);
                            // For ElasticSearch aggregations, we create terms in the form 'symbol1---symbol2'. We also sort the symbols so that the same pair of symbols is always stored in the same order
                            // in order to reduce the number of unique values.
                            // Then we can use ElasticSearch aggregations to count interactions occurrences instead of retrieving all documents and counting ourselves.
                            document.addField("aggregationvalue", document.getAsArrayFieldValue("argumenthomoprefnames").stream().map(IFieldValue::toString).sorted().collect(Collectors.joining("---")));
//                            final ArrayFieldValue go1Values = new ArrayFieldValue(arg1GoPrefnames);
//                            final ArrayFieldValue go2Values = new ArrayFieldValue(arg2GoPrefnames);
//                            for (IFieldValue go1 : go1Values) {
//                                document.addField("aggregationvaluegogene", go1.toString() + "---" + arg2HomoPrefNameValue.toString());
//                                for (IFieldValue go2 : go2Values) {
//                                    document.addField("aggregationvaluegenego", arg1HomoPrefNameValue.toString() + "---" + go2.toString());
//                                    document.addField("aggregationvaluegogo", go1 + "---" + go2);
//                                }
//                            }
//                            if (go1Values.isEmpty()) {
//                                for (IFieldValue go2 : go2Values) {
//                                    document.addField("aggregationvaluegenego", arg1HomoPrefNameValue.toString() + "---" + go2.toString());
//                                }
//                            }
                            document.addField("numarguments", DUMMY.equals(((ArgumentMention) argPair[1]).getRole()) ? 1 : 2);

                            // filter out reflexive events
                            if (((ArrayFieldValue) document.get("argumenttophomoids")).stream().map(RawToken.class::cast).map(RawToken::getTokenValue).distinct().count() == 2) {
                                relDocs.add(document);
                            }
                        } catch (CASException e) {
                            log.error("Could not create interaction document", e);
                            throw new FieldGenerationException(e);
                        }
                    }
                }
            }
        }
        return relDocs;
    }

    private ArgumentMention getMockArgument(JCas jCas, Annotation firstArgument) {
        // We copy the offsets of the first argument to make sure they both end up in the same sentence.
        // Otherwise, the RelationDocumentGenerator would filter it out.
        // A side effect is that, when looking at the argumentcoveredtext field for unary events,
        // it looks as though the event had the same argument twice because its the same covered text
        final Gene gene = new Gene(jCas, firstArgument.getBegin(), firstArgument.getEnd());
        final ResourceEntry re = new ResourceEntry(jCas);
        re.setEntryId(UNARY_EVENT_MOCK_TEXT);
        re.setSource(getClass().getSimpleName() + " dummy interaction partner for a unary event.");
        gene.setResourceEntryList(JCoReTools.addToFSArray(null, re));
        final ArgumentMention argument = new ArgumentMention(jCas, gene.getBegin(), gene.getEnd());
        argument.setRef(gene);
        argument.setRole(DUMMY);
        return argument;
    }

    private String reduceToLastDottedPathElement(String dottedPath) {
        if (dottedPath == null)
            return dottedPath;
        String[] split = dottedPath.split("\\.");
        return split[split.length - 1];
    }

    /**
     * We want to offer relations with gene families. Some of those get concept IDs assigned in the GePi indexing pipeline.
     * Most, however, will not be recognized or even not have a specific database entry at all. For those, we just
     * add their covered as ID. This won't be used for search but just be a placeholder that still allows seeing to which
     * part of text it refers.
     *
     * @param rel
     * @throws CASException
     */
    private void setMockIdToFamilies(FlattenedRelation rel) throws FieldGenerationException {
        try {
            for (FeatureStructure fs : rel.getArguments()) {
                ArgumentMention am = (ArgumentMention) fs;
                Gene gene = (Gene) am.getRef();
                // only assign a mock ID if there is not already one given
                if (gene.getResourceEntryList() == null && ("FamilyName".equals(gene.getSpecificType()) || "protein_familiy_or_group".equals(gene.getSpecificType()))) {
                    FSArray resourceEntryList = new FSArray(rel.getCAS().getJCas(), 1);
                    ResourceEntry resourceEntry = new ResourceEntry(rel.getCAS().getJCas(), gene.getBegin(), gene.getEnd());
                    // in lack of something better
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
