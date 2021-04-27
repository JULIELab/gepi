package de.julielab.gepi.indexing;

import com.sun.istack.NotNull;
import de.julielab.jcore.consumer.es.*;
import de.julielab.jcore.consumer.es.filter.ConstantOutputFilter;
import de.julielab.jcore.consumer.es.filter.FilterChain;
import de.julielab.jcore.consumer.es.filter.UniqueFilter;
import de.julielab.jcore.consumer.es.preanalyzed.*;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.types.pubmed.OtherID;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.jcore.utility.index.JCoReOverlapAnnotationIndex;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.List;
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
                if (rel.getArguments().size() > 1) {
                    ArrayFieldValue relationPairDocuments = (ArrayFieldValue) relationFieldValueGenerator.generateFieldValue(rel);
                    for (IFieldValue fv : relationPairDocuments) {
                        Document relDoc = (Document) fv;
                        FeatureStructure[] argPair = ((ArrayFieldValue) relDoc.get("ARGUMENT_FS")).stream().map(RawToken.class::cast).map(t -> (FeatureStructure) t.getTokenValue()).toArray(FeatureStructure[]::new);
                        relDoc.remove("ARGUMENT_FS");
                        // We create the sentence as a document of its own. In the mapping we then could add it as
                        // an object or as a nested document. There is no need to make it a nested document so we will
                        // use the object mapping which performs better.
                        Document sentenceDocument = null;
                        Collection<Sentence> overlappingSentences = sentIndex.get(rel);
                        if (!overlappingSentences.isEmpty())
                            sentenceDocument = createSentenceDocument(jCas, docId, i, overlappingSentences.stream().findAny().get(), argPair);
                        // Likewise for the paragraph-like containing annotation of the relation
                        Document paragraphDocument = createParagraphDocument(jCas, docId, rel, argPair, zoneIndex);

                        relDoc.addField("sentence", sentenceDocument);
                        relDoc.addField("paragraph", paragraphDocument);

                        relDocs.add(relDoc);
                    }
                }
                ++i;
            }
        } catch (CASException e) {
            throw new FieldGenerationException(e);
        }
        return relDocs;
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
        Optional<Title> documentTitle = JCasUtil.select(jCas, Title.class).stream().filter(t -> t.getTitleType().equals("document")).findAny();
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
            if (z instanceof AbstractSection)
                zoneHeadings.add(new RawToken(((AbstractSection) z).getAbstractSectionHeading().getCoveredText()));
            else if (z instanceof Section && ((Section) z).getSectionHeading() != null)
                zoneHeadings.add(new RawToken(((Section) z).getSectionHeading().getCoveredText()));
            else if (z instanceof Caption)
                zoneHeadings.add(new RawToken(z.getCoveredText()));
        }
        // If we couldn't fine one of the specified structures, use the smallest one
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

        log.trace("Creating preanalyzedFieldValue for paragaph");
        PreanalyzedFieldValue preanalyzedFieldValue = makePreanalyzedFulltextFieldValue(jCas, paragraphLike, argPair);
        paragraphDocument.addField("text", preanalyzedFieldValue);
        paragraphDocument.addField("id", paragraphDocument.getId());
        paragraphDocument.addField("likelihood", FieldCreationUtils.getMeanLikelihood(paragraphLike));
        paragraphDocument.addField("headings", zoneHeadings);

        return paragraphDocument;
    }

    @NotNull
    private Document createSentenceDocument(JCas jCas, String docId, int i, Sentence sentence, FeatureStructure[] argPair) throws CASException, FieldGenerationException {
        Document sentenceDocument = new Document(docId + "_" + sentence.getId());

        log.trace("Creating preanalyzedFieldValue for sentence");
        PreanalyzedFieldValue preanalyzedFieldValue = makePreanalyzedFulltextFieldValue(jCas, sentence, argPair);

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
     * @return A pre-computed field value for an ElasticSearch index field. That value is not further analyzed by ElasticSearch but stored as specified here.
     * @throws CASException If CAS access fails.
     */
    private PreanalyzedFieldValue makePreanalyzedFulltextFieldValue(JCas jCas, AnnotationFS fullTextSpan, FeatureStructure[] argPair) throws CASException {
        FeaturePathSets featurePathSets = new FeaturePathSets();
        featurePathSets.add(new FeaturePathSet(Token.type, Arrays.asList("/:coveredText()"), null, textFb.textTokensFilter));
        featurePathSets.add(new FeaturePathSet(Abbreviation.type, Arrays.asList("/textReference:coveredText()"), null, textFb.textTokensFilter));
        featurePathSets.add(new FeaturePathSet(Gene.type, Arrays.asList("/resourceEntryList/entryId"), null, new FilterChain(geneFb.gene2tid2atidAddonFilter, new UniqueFilter())));
        featurePathSets.add(new FeaturePathSet(FlattenedRelation.type, Arrays.asList("/rootRelation/specificType"), null, constantTriggerFilter));
        List<PreanalyzedToken> tokens = relationFieldValueGenerator.getTokensForAnnotationIndexes(featurePathSets, null, true, PreanalyzedToken.class, fullTextSpan, null, jCas);
        // We only want the special highlighting term xargumentx for the actual two arguments of the
        // current relation. Thus we need to interlace the argument terms with the sentence terms.
        addArgumentTokens(tokens, argPair, fullTextSpan.getBegin(), fullTextSpan.getEnd());
        // First sort by offset. For equal offsets, put the tokens with positionIncrement == 1 first.
        Collections.sort(tokens, Comparator.<PreanalyzedToken>comparingInt(t -> t.start).thenComparing(t -> t.positionIncrement, Comparator.reverseOrder()));
        PreanalyzedFieldValue preanalyzedFieldValue = relationFieldValueGenerator.createPreanalyzedFieldValue(fullTextSpan.getCoveredText(), tokens);
        if (!tokens.isEmpty() && tokens.get(0).positionIncrement <= 0)
            throw new IllegalStateException("Created a token stream for an annotation of type " + fullTextSpan.getClass().getCanonicalName() + " where the first token has position increment " + tokens.get(0).positionIncrement + ". This is illegal. Token sequence is: " + tokens.stream().map(t -> "[" + t.term + "]" + " " + t.start + "-" + t.end + " i:" + t.positionIncrement).collect(Collectors.joining(" || ")));
        return preanalyzedFieldValue;
    }


    private void addArgumentTokens(List<PreanalyzedToken> tokens, FeatureStructure[] argPair, int fullTextSpanStart, int fullTextSpanEnd) {
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

        if (token1.start < 0 || token1.end > fullTextSpanEnd-fullTextSpanStart)
            throw new IllegalStateException(String.format("xargumentx token offsets are out of bounds: %d-%d. Covering span annotation has offsets %d-%d, length %d", token1.start, token1.end, fullTextSpanStart, fullTextSpanEnd, fullTextSpanEnd-fullTextSpanStart));
        if (token2.start < 0 || token2.end > fullTextSpanEnd-fullTextSpanStart)
            throw new IllegalStateException(String.format("xargumentx token offsets are out of bounds: %d-%d. Covering span annotation has offsets %d-%d, length %d", token2.start, token2.end, fullTextSpanStart, fullTextSpanEnd, fullTextSpanEnd-fullTextSpanStart));

        tokens.add(token1);
        tokens.add(token2);
    }
}