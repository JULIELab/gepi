package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.*;
import de.julielab.jcore.consumer.es.filter.ConstantOutputFilter;
import de.julielab.jcore.consumer.es.filter.FilterChain;
import de.julielab.jcore.consumer.es.filter.UniqueFilter;
import de.julielab.jcore.consumer.es.preanalyzed.*;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.types.pubmed.OtherID;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Main class to create index documents for events, i.e. {@link FlattenedRelation} objects.
 */
public class RelationDocumentGenerator extends DocumentGenerator {
    private final static Logger log = LoggerFactory.getLogger(RelationDocumentGenerator.class);
    private final RelationFieldValueGenerator relationFieldValueGenerator;
    private final TextFilterBoard textFb;
    private final GeneFilterBoard geneFb;
    private final ConstantOutputFilter constantTriggerFilter;
    private final Pattern SECTION_NUMBERING = Pattern.compile("^([0-9]+\\.)+([0-9]+)?\\s*");

    public RelationDocumentGenerator(FilterRegistry filterRegistry) {
        super(filterRegistry);
        relationFieldValueGenerator = new RelationFieldValueGenerator(filterRegistry);
        textFb = filterRegistry.getFilterBoard(TextFilterBoard.class);
        geneFb = filterRegistry.getFilterBoard(GeneFilterBoard.class);
        constantTriggerFilter = new ConstantOutputFilter("xtriggerx");
    }

    @Override
    public List<Document> createDocuments(JCas jCas) throws FieldGenerationException {
        List<Document> relDocs = new ArrayList<>();
        String docId = getDocumentId(jCas);
        Map<FlattenedRelation, Collection<Zone>> zoneIndex = JCasUtil.indexCovering(jCas, FlattenedRelation.class, Zone.class);
        Map<FlattenedRelation, Collection<Sentence>> sentIndex = JCasUtil.indexCovering(jCas, FlattenedRelation.class, Sentence.class);
        try {
            int i = 0;
            for (FlattenedRelation rel : jCas.<FlattenedRelation>getAnnotationIndex(FlattenedRelation.type)) {
                // exclude events where arguments are FamilyNames for now; we don't have IDs for them yet
                if (rel.getArguments().size() > 1) {
                    ArrayFieldValue relationPairDocuments = (ArrayFieldValue) relationFieldValueGenerator.generateFieldValue(rel);
                    for (IFieldValue fv : relationPairDocuments) {
                        Document relDoc = (Document) fv;
                        // Retrieve the argument pair of the current relation/event from the already created document for this relation
                        FeatureStructure[] argPair = ((ArrayFieldValue) relDoc.get("ARGUMENT_FS")).stream().map(RawToken.class::cast).map(t -> (FeatureStructure) t.getTokenValue()).toArray(FeatureStructure[]::new);
                        // We create the sentence as a document of its own. In the mapping we then could add it as
                        // an object or as a nested document. There is no need to make it a nested document so we will
                        // use the object mapping which performs better.
                        Document sentenceDocument = null;
                        Collection<Sentence> overlappingSentences = sentIndex.get(rel);
                        if (argPairLiesWithinSentence(overlappingSentences, argPair)) {
                            if (!overlappingSentences.isEmpty())
                                sentenceDocument = createSentenceDocument(jCas, docId, i, overlappingSentences.stream().findAny().get(), argPair, rel);
                            // Likewise for the paragraph-like containing annotation of the relation
                            Document paragraphDocument = createParagraphDocument(jCas, docId, rel, argPair, zoneIndex);

                            // skip events extracted PMC abstracts when there exists a corresponding PubMed document
                            if (paragraphDocument.containsKey("textscope") && paragraphDocument.get("textscope").toString().equals("abstract") && relDoc.get("source").toString().equals("pmc") && relDoc.containsKey("pmid")) {
                                log.debug("DEBUG MESSAGE: Event with arguments ({}, {}) from document {} omitted because it appeared in the abstract and the PubMed document {} corresponds to it", relDoc.get("argument1coveredtext"), relDoc.get("argument2coveredtext"), docId, relDoc.get("pmid"));
                                continue;
                            }

                            relDoc.addField("sentence", sentenceDocument);
                            relDoc.addField("paragraph", paragraphDocument);

                            relDocs.add(relDoc);
                        }
                    }
                }
                ++i;
            }
        } catch (CASException e) {
            throw new FieldGenerationException(e);
        }
        mergeEqualRelDocs(relDocs);
        // remove the temporary field for the UIMA argument objects
        relDocs.forEach(d -> d.remove("ARGUMENT_FS"));
        return relDocs;
    }

    private void mergeEqualRelDocs(List<Document> relDocs) {
        // create a key consisting of the argument offsets, the mapped argument IDs and the main relation type.
        // We deem events to be equal that have those values in common (in this order).
        Map<String, Document> key2doc = new HashMap<>();
        Iterator<Document> docIt = relDocs.iterator();
        while (docIt.hasNext()) {
            Document document = docIt.next();
            FeatureStructure[] argPair = ((ArrayFieldValue) document.get("ARGUMENT_FS")).stream().map(RawToken.class::cast).map(t -> (FeatureStructure) t.getTokenValue()).toArray(FeatureStructure[]::new);
            StringBuilder keyBuilder = new StringBuilder();
            for (FeatureStructure fs : argPair) {
                ArgumentMention am = (ArgumentMention) fs;
                Gene g = (Gene) am.getRef();
                keyBuilder.append(g.getBegin());
                keyBuilder.append(g.getEnd());
                keyBuilder.append(document.get("argument1geneid").toString());
                keyBuilder.append(document.get("argument2geneid").toString());
            }
            keyBuilder.append(((RawToken) document.get("maineventtype")).getTokenValue().toString());
            String key = keyBuilder.toString();
            // Now use the key to find relations that have been extracted from multiple combinations of gene tagger, gene mapper and event extractor
            Document existingDoc = key2doc.get(key);
            if (existingDoc != null) {
                // merge the current document into the existing one
                IFieldValue existingRelationsource = existingDoc.get("relationsource");
                IFieldValue existingGenesource = existingDoc.get("genesource");
                IFieldValue existingGenemappingsource = existingDoc.get("genemappingsource");

                IFieldValue currentRelationsource = document.get("relationsource");
                IFieldValue currentGenesource = document.get("genesource");
                IFieldValue currentGenemappingsource = document.get("genemappingsource");

                // merge the fields into the existing document while avoiding duplicates
                ArrayFieldValue relationSourceValues = new ArrayFieldValue();
                relationSourceValues.addFlattened(existingRelationsource);
                relationSourceValues.addFlattened(currentRelationsource);
                final TreeSet<RawToken> relationSourceSet = relationSourceValues.stream().map(RawToken.class::cast).collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(t -> t.getTokenValue().toString()))));
                relationSourceValues = new ArrayFieldValue(new ArrayList<>(relationSourceSet));

                ArrayFieldValue geneSourceValues = new ArrayFieldValue();
                geneSourceValues.addFlattened(existingGenesource);
                geneSourceValues.addFlattened(currentGenesource);
                final TreeSet<RawToken> geneSourceSet = geneSourceValues.stream().map(RawToken.class::cast).collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(t -> t.getTokenValue().toString()))));
                geneSourceValues = new ArrayFieldValue(new ArrayList<>(geneSourceSet));

                ArrayFieldValue geneMappingSourceValues = new ArrayFieldValue();
                geneMappingSourceValues.addFlattened(existingGenemappingsource);
                geneMappingSourceValues.addFlattened(currentGenemappingsource);
                final TreeSet<RawToken> geneMappingSourceSet = geneMappingSourceValues.stream().map(RawToken.class::cast).collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(t -> t.getTokenValue().toString()))));
                geneMappingSourceValues = new ArrayFieldValue(new ArrayList<>(geneMappingSourceSet));

                existingDoc.addField("relationsource", relationSourceValues);
                existingDoc.addField("genesource", geneSourceValues);
                existingDoc.addField("genemappingsource", geneMappingSourceValues);

                // discard the current document as it has been merged into the existing one
                docIt.remove();
            } else {
                key2doc.put(key, document);
            }
        }
    }

    /**
     * Filter method as long as we don't have handling for FamilyName gene mentions
     *
     * @param arguments
     * @return
     */
    private boolean noFamilies(FSArray arguments) {
        boolean noFamilies = true;
        for (int i = 0; i < arguments.size(); ++i) {
            noFamilies = noFamilies && !"FamilyName".equals(((Gene) ((ArgumentMention) arguments.get(i)).getRef()).getSpecificType());
        }
        return noFamilies;
    }

    private boolean argPairLiesWithinSentence(Collection<Sentence> overlappingSentences, FeatureStructure[] argPair) {
        if (overlappingSentences.isEmpty())
            return false;
        Sentence sentence = overlappingSentences.stream().findAny().get();
        int fullTextSpanStart = sentence.getBegin();
        int fullTextSpanEnd = sentence.getEnd();
        ArgumentMention arg1 = (ArgumentMention) argPair[0];
        ArgumentMention arg2 = (ArgumentMention) argPair[1];
        if (arg1.getBegin() < fullTextSpanStart)
            return false;
        if (arg1.getEnd() > fullTextSpanEnd)
            return false;
        if (arg2.getBegin() < fullTextSpanStart)
            return false;
        if (arg2.getEnd() > fullTextSpanEnd)
            return false;
        return true;
    }

    private String getDocumentId(JCas jCas) {
        de.julielab.jcore.types.pubmed.Header header = JCasUtil.selectSingle(jCas, de.julielab.jcore.types.pubmed.Header.class);
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

    private Document createParagraphDocument(JCas jCas, String docId, FlattenedRelation rel, FeatureStructure[] argPair, Map<FlattenedRelation, Collection<Zone>> zoneIndex) throws CASException, FieldGenerationException {
        List<Zone> zonesAscending = zoneIndex.get(rel).stream().sorted(Comparator.comparingInt(z -> z.getEnd() - z.getBegin())).collect(Collectors.toList());
        ArrayFieldValue zoneHeadings = new ArrayFieldValue();
        IFieldValue textScope = null;
        Optional<Title> documentTitle = null;
        try {
            documentTitle = JCasUtil.select(jCas, Title.class).stream().filter(t -> t.getTitleType() != null && t.getTitleType().equals("document")).findAny();
        } catch (Exception e) {
            log.error("NPE for title " + JCasUtil.select(jCas, Title.class));
            throw e;
        }
        AnnotationFS paragraphLike = null;
        Map<Zone, Integer> zoneIds = new HashMap<>();
        int idCounter = 0;
        for (Zone z : zonesAscending) {
            zoneIds.put(z, idCounter++);
            if (paragraphLike == null) {
                if (z instanceof AbstractText)
                    paragraphLike = z;
                else if (z instanceof Paragraph)
                    paragraphLike = z;
                else if (z instanceof Caption)
                    paragraphLike = z;
            }
            if (z instanceof AbstractText) {
                zoneHeadings.add(new RawToken("Abstract"));
                textScope = new RawToken("abstract");
            }
            if (z instanceof AbstractSection) {
                zoneHeadings.add(new RawToken(removeSectionNumbering(((AbstractSection) z).getAbstractSectionHeading().getCoveredText())));
                textScope = new RawToken("abstract");
            } else if (z instanceof Section && ((Section) z).getSectionHeading() != null) {
                zoneHeadings.add(new RawToken(removeSectionNumbering(((Section) z).getSectionHeading().getCoveredText())));
                if (textScope == null)
                    textScope = new RawToken("body");
            } else if (z instanceof Caption) {
                zoneHeadings.add(new RawToken(removeSectionNumbering(z.getCoveredText())));
                textScope = new RawToken(((Caption) z).getCaptionType());
            }
        }
        // If we couldn't find one of the specified structures, use the smallest one
        if (paragraphLike == null && !zonesAscending.isEmpty())
            paragraphLike = zonesAscending.get(0);
        // If we couldn't find any zones, use the whole document. This is meant as a last fallback.
        if (paragraphLike == null) {
            paragraphLike = (AnnotationFS) jCas.getDocumentAnnotationFs();
            log.debug("Using document annotation as fallback because no other paragraph like structure was found for event {} in document {}: " + paragraphLike, rel, docId);
        }
        // The top title is the document title.
        documentTitle.ifPresent(t -> zoneHeadings.add(new RawToken(t.getCoveredText())));

        Document paragraphDocument = new Document(docId + "_par" + zoneIds.get(paragraphLike));

        log.trace("Creating preanalyzedFieldValue for paragraph");
        PreanalyzedFieldValue preanalyzedFieldValue = makePreanalyzedFulltextFieldValue(jCas, paragraphLike, argPair, rel);
        paragraphDocument.addField("text", preanalyzedFieldValue);
        paragraphDocument.addField("id", paragraphDocument.getId());
        paragraphDocument.addField("likelihood", FieldCreationUtils.getMeanLikelihood(paragraphLike));
        paragraphDocument.addField("headings", zoneHeadings);
        if (textScope != null)
            paragraphDocument.addField("textscope", textScope);

        return paragraphDocument;
    }

    /**
     * Headings often come with their numbering e.g. "3. Results". We do not care about the number, strip it.
     *
     * @param heading The complete heading of a section.
     * @return The heading without leading numbers.
     */
    private String removeSectionNumbering(String heading) {
        Matcher m = SECTION_NUMBERING.matcher(heading);
        if (m.find()) {
            return m.replaceFirst("");
        }
        return heading;
    }

    @NotNull
    private Document createSentenceDocument(JCas jCas, String docId, int i, Sentence sentence, FeatureStructure[] argPair, FlattenedRelation rel) throws CASException, FieldGenerationException {
        Document sentenceDocument = new Document(docId + "_" + sentence.getId());

        log.trace("Creating preanalyzedFieldValue for sentence");
        PreanalyzedFieldValue preanalyzedFieldValue = makePreanalyzedFulltextFieldValue(jCas, sentence, argPair, rel);

        sentenceDocument.addField("text", preanalyzedFieldValue);
        sentenceDocument.addField("id", docId + "_" + (sentence.getId() != null ? sentence.getId() : i));
        sentenceDocument.addField("likelihood", FieldCreationUtils.getMeanLikelihood(sentence));
        return sentenceDocument;
    }

    /**
     * <p>Creates a {@link PreanalyzedFieldValue} with added gene ID and aggregate gene ID as well as relation argument position information.</p>
     *
     * @param jCas         The jCas.
     * @param fullTextSpan The text containing annotation, i.e. the sentence or the paragraph-like FeatureStrucure.
     * @param argPair      The two arguments in focus that make up the current relation document.
     * @param relation     The relation being processed.
     * @return A pre-computed field value for an ElasticSearch index field. That value is not further analyzed by ElasticSearch but stored as specified here.
     * @throws CASException If CAS access fails.
     */
    private PreanalyzedFieldValue makePreanalyzedFulltextFieldValue(JCas jCas, AnnotationFS fullTextSpan, FeatureStructure[] argPair, FlattenedRelation relation) throws CASException {
        FeaturePathSets featurePathSets = new FeaturePathSets();
        featurePathSets.add(new FeaturePathSet(Token.type, Arrays.asList("/:coveredText()"), null, textFb.textTokensFilter));
        featurePathSets.add(new FeaturePathSet(Abbreviation.type, Arrays.asList("/textReference:coveredText()"), null, textFb.textTokensFilter));
        featurePathSets.add(new FeaturePathSet(Gene.type, Arrays.asList("/resourceEntryList/entryId"), null, new FilterChain(geneFb.gene2tid2atidAddonFilter, new UniqueFilter())));
//        featurePathSets.add(new FeaturePathSet(FlattenedRelation.type, Arrays.asList("/rootRelation/specificType"), null, constantTriggerFilter));
        List<PreanalyzedToken> tokens = relationFieldValueGenerator.getTokensForAnnotationIndexes(featurePathSets, null, true, PreanalyzedToken.class, fullTextSpan, null, jCas);
        // We only want the special highlighting term xargumentx for the actual two arguments of the
        // current relation. Thus we need to interlace the argument terms with the sentence terms.
        addArgumentAndTriggerTokens(tokens, argPair, fullTextSpan.getBegin(), fullTextSpan.getEnd(), relation);
        // First sort by offset. For equal offsets, put the tokens with positionIncrement == 1 first.
        Collections.sort(tokens, Comparator.<PreanalyzedToken>comparingInt(t -> t.start).thenComparing(t -> t.positionIncrement, Comparator.reverseOrder()));
        if (!tokens.isEmpty() && tokens.get(0).positionIncrement == 0)
            tokens.get(0).positionIncrement = 1;
        PreanalyzedFieldValue preanalyzedFieldValue = relationFieldValueGenerator.createPreanalyzedFieldValue(fullTextSpan.getCoveredText(), tokens);
//        if (!tokens.isEmpty() && tokens.get(0).positionIncrement <= 0)
//            throw new IllegalStateException("Created a token stream for an annotation of type " + fullTextSpan.getClass().getCanonicalName() + " where the first token has position increment " + tokens.get(0).positionIncrement + ". This is illegal. Token sequence is: " + tokens.stream().map(t -> "[" + t.term + "]" + " " + t.start + "-" + t.end + " i:" + t.positionIncrement).collect(Collectors.joining(" || ")));
        return preanalyzedFieldValue;
    }


    private void addArgumentAndTriggerTokens(List<PreanalyzedToken> tokens, FeatureStructure[] argPair, int fullTextSpanStart, int fullTextSpanEnd, FlattenedRelation relation) {
        ArgumentMention arg1 = (ArgumentMention) argPair[0];
        ArgumentMention arg2 = (ArgumentMention) argPair[1];

        PreanalyzedToken token1 = new PreanalyzedToken();
        token1.start = arg1.getBegin() - fullTextSpanStart;
        token1.end = arg1.getEnd() - fullTextSpanStart;
        token1.positionIncrement = 0;
        token1.term = "xargumentx";

        PreanalyzedToken token2 = new PreanalyzedToken();
        token2.start = arg2.getBegin() - fullTextSpanStart;
        token2.end = arg2.getEnd() - fullTextSpanStart;
        token2.positionIncrement = 0;
        token2.term = "xargumentx";

        final EventTrigger trigger = ((EventMention) relation.getRootRelation()).getTrigger();
        PreanalyzedToken triggerToken = new PreanalyzedToken();
        triggerToken.start = trigger.getBegin() - fullTextSpanStart;
        triggerToken.end = trigger.getEnd() - fullTextSpanStart;
        triggerToken.positionIncrement = 0;
        triggerToken.term = "xtriggerx";

        final LikelihoodIndicator likelihood = relation.getRootRelation().getLikelihood();
        PreanalyzedToken likelihoodToken = null;
        if (!likelihood.getLikelihood().equals("assertion")) {
            likelihoodToken = new PreanalyzedToken();
            likelihoodToken.start = likelihood.getBegin() - fullTextSpanStart;
            likelihoodToken.end = likelihood.getEnd() - fullTextSpanStart;
            likelihoodToken.positionIncrement = 0;
            likelihoodToken.term = "xlike"+FieldCreationUtils.likelihoodValues.get(likelihood.getLikelihood())+"x";
        }

        if (token1.start < 0 || token1.end > fullTextSpanEnd - fullTextSpanStart) {
            String docId;
            try {
                docId = JCoReTools.getDocId(arg1.getCAS().getJCas());
            } catch (CASException e) {
                docId = "<unknown>";
            }
            throw new IllegalStateException(String.format("xargumentx token offsets are out of bounds: %d-%d. Respective event argument text: \"%s\". Covering span annotation has offsets %d-%d, length %d. DocumentID: %s. Processed event is: %s", token1.start, token1.end, arg1.getCoveredText(), fullTextSpanStart, fullTextSpanEnd, fullTextSpanEnd - fullTextSpanStart, docId, relation));
        }
        if (token2.start < 0 || token2.end > fullTextSpanEnd - fullTextSpanStart) {
            String docId;
            try {
                docId = JCoReTools.getDocId(arg1.getCAS().getJCas());
            } catch (CASException e) {
                docId = "<unknown>";
            }
            throw new IllegalStateException(String.format("xargumentx token offsets are out of bounds: %d-%d. Respective event argument text: \"%s\". Covering span annotation has offsets %d-%d, length %d. DocumentID: %s. Processed event is: %s", token2.start, token2.end, arg2.getCoveredText(), fullTextSpanStart, fullTextSpanEnd, fullTextSpanEnd - fullTextSpanStart, docId, relation));
        }

        tokens.add(token1);
        tokens.add(token2);
        tokens.add(triggerToken);
        if (likelihoodToken != null)
            tokens.add(likelihoodToken);
    }
}
