package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ArrayFieldValue;
import de.julielab.jcore.consumer.es.FieldGenerationException;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.RawToken;
import de.julielab.jcore.consumer.es.sharedresources.AddonTermsProvider;
import de.julielab.jcore.consumer.es.sharedresources.ListProvider;
import de.julielab.jcore.consumer.es.sharedresources.MapProvider;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.types.pubmed.Header;
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

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertNotNull;

public class RelationFieldValueGeneratorTest {


    private FilterRegistry filterRegistry;

    @Test
    public void testFilterBoard() throws ResourceInitializationException, ResourceAccessException {
        ExternalResourceDescription gene2tid = ExternalResourceFactory.createExternalResourceDescription(AddonTermsProvider.class, "file:src/test/resources/egid2tid.txt");
        ExternalResourceDescription tid2atid = ExternalResourceFactory.createExternalResourceDescription(AddonTermsProvider.class, "file:src/test/resources/tid2atid.txt");
        ExternalResourceDescription stopwords = ExternalResourceFactory.createExternalResourceDescription(ListProvider.class, "file:src/test/resources/stopwords.txt");
        UimaContext uimaContext = UimaContextFactory.createUimaContext("egid2tid", gene2tid, "tid2atid", tid2atid, "stopwords", stopwords);
        filterRegistry = new FilterRegistry(uimaContext);
        filterRegistry.addFilterBoard(GeneFilterBoard.class, new GeneFilterBoard());
        filterRegistry.addFilterBoard(TextFilterBoard.class, new TextFilterBoard());
        assertNotNull(filterRegistry.getFilterBoard(GeneFilterBoard.class));
        assertNotNull(filterRegistry.getFilterBoard(TextFilterBoard.class));
    }

    @Test(dependsOnMethods = "testFilterBoard")
    public void testCreateRelationDocument() throws UIMAException, FieldGenerationException {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-semantics-biology-types",
                "de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.extensions.jcore-semantics-mention-extension-types");

        jCas.setDocumentText("A regulates B.");

        Header header = new Header(jCas);
        header.setDocId("123456");
        header.addToIndexes();

        LikelihoodIndicator likelihoodIndicator = new LikelihoodIndicator(jCas, 0, 5);
        likelihoodIndicator.setLikelihood("high");
        likelihoodIndicator.addToIndexes();

        new Token(jCas, 0, 1).addToIndexes();
        new Token(jCas, 2, 11).addToIndexes();
        new Token(jCas, 12, 13).addToIndexes();
        new Token(jCas, 13, 14).addToIndexes();

        Gene gene = new Gene(jCas, 0, 1);
        gene.addToIndexes();
        GeneResourceEntry geneResourceEntry = new GeneResourceEntry(jCas, 0, 1);
        geneResourceEntry.setEntryId("42");
        gene.setResourceEntryList(JCoReTools.addToFSArray(null, geneResourceEntry));

        Gene gene2 = new Gene(jCas, 12, 13);
        gene2.addToIndexes();
        GeneResourceEntry geneResourceEntry2 = new GeneResourceEntry(jCas, 12, 13);
        geneResourceEntry2.setEntryId("43");
        gene2.setResourceEntryList(JCoReTools.addToFSArray(null, geneResourceEntry2));

        EventMention em = new EventMention(jCas, 2, 11);
        em.addToIndexes();
        // note that the actual specificType for regulation is the one from the BioNLP Shared Task and not just "regulation",
        // this is just for the test
        em.setSpecificType("regulation");
        em.setLikelihood(likelihoodIndicator);
        ArgumentMention am1 = new ArgumentMention(jCas, 0, 1);
        am1.setRef(gene);
        am1.addToIndexes();
        ArgumentMention am2 = new ArgumentMention(jCas, 12, 13);
        am2.setRef(gene2);
        am2.addToIndexes();
        em.setArguments(JCoReTools.addToFSArray(null, Arrays.asList(am1, am2)));

        FlattenedRelation fr = new FlattenedRelation(jCas, 2, 11);
        fr.setId("FE0");
        fr.setArguments(em.getArguments());
        fr.setRootRelation(em);
        fr.setRelations(JCoReTools.addToFSArray(null, em));
        fr.addToIndexes();

        Sentence sentence = new Sentence(jCas, 0, jCas.getDocumentText().length());
        sentence.setId("0");
        sentence.addToIndexes();

        RelationDocumentGenerator relationDocumentGenerator = new RelationDocumentGenerator(filterRegistry);
        Document relationDocument = relationDocumentGenerator.createDocuments(jCas).get(0);

        assertNotNull(relationDocument);
        assertThat(relationDocument).containsKeys("pmid", "id", "likelihood", "sentence", "allarguments",
                "alleventtypes", "maineventtype");

        PreanalyzedFieldValue preAnalyzedSentence = (PreanalyzedFieldValue) ((Document)relationDocument.get("sentence")).get("text");
        assertThat(preAnalyzedSentence.fieldString).isNotBlank();
        assertThat(preAnalyzedSentence.tokens).extracting(t -> t.term).containsExactly("a", "42", "tid42", "atid42", "#argument#", "regul", "#trigger#", "b", "43", "tid43", "atid43", "#argument#");

        assertThat((ArrayFieldValue) relationDocument.get("allarguments")).extracting("tokenValue").containsExactly("42", "tid42", "atid42", "43", "tid43", "atid43");

        assertThat(relationDocument.get("likelihood")).extracting("tokenValue").containsExactly(5);

        assertThat(relationDocument.get("id")).extracting("tokenValue").containsExactly("123456_FE0");

        assertThat(((Document)relationDocument.get("sentence")).get("id")).extracting("tokenValue").containsExactly("123456_0");
    }
}
