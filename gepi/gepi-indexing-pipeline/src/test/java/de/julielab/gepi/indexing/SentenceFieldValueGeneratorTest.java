package de.julielab.gepi.indexing;

import com.google.common.annotations.VisibleForTesting;
import de.julielab.jcore.consumer.es.ArrayFieldValue;
import de.julielab.jcore.consumer.es.FieldGenerationException;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.RawToken;
import de.julielab.jcore.consumer.es.sharedresources.AddonTermsProvider;
import de.julielab.jcore.consumer.es.sharedresources.IMapProvider;
import de.julielab.jcore.consumer.es.sharedresources.ListProvider;
import de.julielab.jcore.consumer.es.sharedresources.MapProvider;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.types.pubmed.OtherID;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.UimaContextFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.testng.annotations.Test;

import java.lang.reflect.Array;

import static org.assertj.core.api.Assertions.*;
import static org.testng.Assert.assertNotNull;

public class SentenceFieldValueGeneratorTest {


    private FilterRegistry filterRegistry;

    @Test
    public void testFilterBoard() throws ResourceInitializationException, ResourceAccessException {
        ExternalResourceDescription gene2tid = ExternalResourceFactory.createExternalResourceDescription(AddonTermsProvider.class, "file:src/test/resources/geneToDatabaseIds.txt");
        ExternalResourceDescription tid2atid = ExternalResourceFactory.createExternalResourceDescription(AddonTermsProvider.class, "file:src/test/resources/geneDatabaseIdsToAggregates.txt");
        ExternalResourceDescription stopwords = ExternalResourceFactory.createExternalResourceDescription(ListProvider.class, "file:src/test/resources/stopwords.txt");
        UimaContext uimaContext = UimaContextFactory.createUimaContext("geneToDatabaseIds", gene2tid, "geneDatabaseIdsToAggregates", tid2atid, "stopwords", stopwords);
        filterRegistry = new FilterRegistry(uimaContext);
        filterRegistry.addFilterBoard(GepiFilterBoard.class, new GepiFilterBoard());
        GepiFilterBoard fb = filterRegistry.getFilterBoard(GepiFilterBoard.class);
        assertNotNull(fb.geneDatabaseIdsToAggregatesFilter);
    }

    @Test(dependsOnMethods = "testFilterBoard")
    public void testCreateSentenceDocument() throws UIMAException, FieldGenerationException {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-semantics-biology-types",
                "de.julielab.jcore.types.jcore-document-meta-pubmed-types");

        jCas.setDocumentText("This is a-gene.");

        Header header = new Header(jCas);
        header.setDocId("123456");
        OtherID otherID = new OtherID(jCas);
        otherID.setId("654321");
        otherID.setSource("PMC");
        header.setOtherIDs(JCoReTools.addToFSArray(null, otherID));
        header.addToIndexes();

        LikelihoodIndicator likelihoodIndicator = new LikelihoodIndicator(jCas, 0, 5);
        likelihoodIndicator.setLikelihood("high");
        likelihoodIndicator.addToIndexes();

        new Token(jCas, 0, 4).addToIndexes();
        new Token(jCas, 5, 7).addToIndexes();
        // This token is obviously a bad one; "a-gene.". This is basically a test that the LuceneStandardTokenizer
        // is doing its job.
        new Token(jCas, 8, 15).addToIndexes();

        Gene gene = new Gene(jCas, 10, 14);
        gene.addToIndexes();
        GeneResourceEntry geneResourceEntry = new GeneResourceEntry(jCas, 10, 14);
        geneResourceEntry.setEntryId("42");
        gene.setResourceEntryList(JCoReTools.addToFSArray(null, geneResourceEntry));

        Sentence sentence = new Sentence(jCas, 0, jCas.getDocumentText().length());
        sentence.setId("0");
        sentence.addToIndexes();

        SentenceFieldValueGenerator fvGenerator = new SentenceFieldValueGenerator(filterRegistry);
        Document sentenceDocument = (Document) fvGenerator.generateFieldValue(sentence);

        assertNotNull(sentenceDocument);
        assertThat(sentenceDocument).containsKeys("pmid", "pmcid", "id", "likelihood", "sentence");

        PreanalyzedFieldValue preAnalyzedSentence = (PreanalyzedFieldValue) sentenceDocument.get("sentence");
        assertThat(preAnalyzedSentence.fieldString).isEqualTo("This is a-gene.");
        assertThat(preAnalyzedSentence.tokens).extracting(t -> t.term).containsExactly("this", "is", "a", "gene", "42", "tid42", "atid42");

        assertThat(sentenceDocument.get("likelihood")).extracting("tokenValue").containsExactly(5);

        assertThat(sentenceDocument.get("id")).extracting("tokenValue").containsExactly("123456_0");
    }
}
