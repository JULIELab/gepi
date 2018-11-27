package de.julielab.gepi.indexing;

import de.julielab.jcore.consumer.es.ElasticSearchConsumer;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.sharedresources.AddonTermsProvider;
import de.julielab.jcore.consumer.es.sharedresources.ListProvider;
import de.julielab.jcore.consumer.es.sharedresources.MapProvider;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.UimaContextFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.testng.annotations.Test;

import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testng.Assert.assertNotNull;

@Test(suiteName = "integration-tests")
public class RelationFieldValueGeneratorIT {
    private FilterRegistry filterRegistry;

    @Test
    public void testFilterBoard() throws ResourceInitializationException, ResourceAccessException {
        ExternalResourceDescription gene2tid = ExternalResourceFactory.createExternalResourceDescription(AddonTermsProvider.class, "file:src/test/resources/egid2tid.txt");
        ExternalResourceDescription tid2atid = ExternalResourceFactory.createExternalResourceDescription(AddonTermsProvider.class, "file:src/test/resources/tid2atid.txt");
        ExternalResourceDescription tid2prefName = ExternalResourceFactory.createExternalResourceDescription(MapProvider.class, "file:src/test/resources/tid2prefName.txt");
        ExternalResourceDescription tid2homoPrefName = ExternalResourceFactory.createExternalResourceDescription(MapProvider.class, "file:src/test/resources/tid2homoPrefName.txt");
        ExternalResourceDescription tid2tophomo = ExternalResourceFactory.createExternalResourceDescription(MapProvider.class, "file:src/test/resources/tid2tophomo.txt");
        ExternalResourceDescription stopwords = ExternalResourceFactory.createExternalResourceDescription(ListProvider.class, "file:src/test/resources/stopwords.txt");
        UimaContext uimaContext = UimaContextFactory.createUimaContext("egid2tid", gene2tid,  "tid2atid", tid2atid, "tid2tophomo", tid2tophomo, "stopwords", stopwords, "tid2prefName", tid2prefName, "tid2homoPrefName", tid2homoPrefName);
        filterRegistry = new FilterRegistry(uimaContext);
        filterRegistry.addFilterBoard(GeneFilterBoard.class, new GeneFilterBoard());
        filterRegistry.addFilterBoard(TextFilterBoard.class, new TextFilterBoard());
        assertNotNull(filterRegistry.getFilterBoard(GeneFilterBoard.class));
        assertNotNull(filterRegistry.getFilterBoard(TextFilterBoard.class));
    }

    @Test(dependsOnMethods = "testFilterBoard")
    public void testIndexing() throws Exception {
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

        AnalysisEngineDescription esConsumer = AnalysisEngineFactory.createEngineDescription(ElasticSearchConsumer.class,
                ElasticSearchConsumer.PARAM_DOC_GENERATORS, new String[]{RelationDocumentGenerator.class.getCanonicalName()},
                ElasticSearchConsumer.PARAM_FILTER_BOARDS, new String[]{TextFilterBoard.class.getCanonicalName(), GeneFilterBoard.class.getCanonicalName()},
                ElasticSearchConsumer.PARAM_URLS, new String[]{"http://localhost:" + ElasticITServer.es.getMappedPort(9200)},
                ElasticSearchConsumer.PARAM_INDEX_NAME, ElasticITServer.TEST_INDEX,
                ElasticSearchConsumer.PARAM_TYPE, "relations");
        ExternalResourceFactory.createDependencyAndBind(esConsumer, "egid2tid", AddonTermsProvider.class, "file:egid2tid.txt");
        ExternalResourceFactory.createDependencyAndBind(esConsumer, "stopwords", ListProvider.class, "file:stopwords.txt");
        ExternalResourceFactory.createDependencyAndBind(esConsumer, "tid2atid", AddonTermsProvider.class, "file:tid2atid.txt");
        ExternalResourceFactory.createDependencyAndBind(esConsumer, "tid2prefName", MapProvider.class, "file:tid2prefName.txt");
        ExternalResourceFactory.createDependencyAndBind(esConsumer, "tid2tophomo", MapProvider.class, "file:tid2tophomo.txt");

        AnalysisEngine engine = AnalysisEngineFactory.createEngine(esConsumer);
        assertThatCode(() -> engine.process(jCas)).doesNotThrowAnyException();
        assertThatCode(() -> engine.collectionProcessComplete()).doesNotThrowAnyException();
        // Give ES a second for indexing
        Thread.sleep(1500);
    }

    @Test(dependsOnMethods = "testIndexing")
    public void testSearch() throws Exception {
        URLConnection urlConnection = new URL("http://localhost:" + ElasticITServer.es.getMappedPort(9200) + "/" + ElasticITServer.TEST_INDEX + "/_search?q=allarguments:tid42").openConnection();
        String result = IOUtils.toString(urlConnection.getInputStream());
        System.out.println(result);
        assertThat(result.contains("hits\":{\"total\":1")).isTrue();

        urlConnection = new URL("http://localhost:" + ElasticITServer.es.getMappedPort(9200) + "/" + ElasticITServer.TEST_INDEX + "/_search?q=maineventtype:regulation&stored_fields=sentence.text").openConnection();
        result = IOUtils.toString(urlConnection.getInputStream());
        assertThat(result.contains("A regulates B.")).isTrue();
    }
}
