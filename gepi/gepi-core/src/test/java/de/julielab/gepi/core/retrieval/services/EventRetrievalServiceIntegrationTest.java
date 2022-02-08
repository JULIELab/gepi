package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.ElasticQuerySymbolConstants;
import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;
import de.julielab.gepi.core.services.GePiCoreTestModule;
import de.julielab.java.utilities.FileUtilities;
import org.apache.commons.io.IOUtils;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.*;

public class EventRetrievalServiceIntegrationTest {
    public static final String TEST_INDEX = "gepi_testindex";
    public static final String TEST_CLUSTER = "gepi_testcluster";
    private final static Logger log = LoggerFactory.getLogger(EventRetrievalServiceIntegrationTest.class);
    // in case we need to disable X-shield: https://stackoverflow.com/a/51172136/1314955
    public static GenericContainer es = new GenericContainer(
            new ImageFromDockerfile("gepicoreestest", true)
                    .withFileFromClasspath("Dockerfile", "dockercontext/Dockerfile")
                    .withFileFromClasspath("elasticsearch-mapper-preanalyzed-7.0.1-SNAPSHOT.zip", "dockercontext/elasticsearch-mapper-preanalyzed-7.0.1-SNAPSHOT.zip"))
            .withExposedPorts(9200)
            .withEnv("cluster.name", TEST_CLUSTER)
            .withEnv("discovery.type", "single-node");
    private static Registry registry;

    @BeforeClass
    public static void setup() throws Exception {
        setupES();

        registry = RegistryBuilder.buildAndStartupRegistry(GePiCoreTestModule.class);
    }

    private static void setupES() throws IOException, InterruptedException {
        es.start();
        Slf4jLogConsumer toStringConsumer = new Slf4jLogConsumer(log);
        es.followOutput(toStringConsumer, OutputFrame.OutputType.STDOUT);

        {
            // Create the test index
            URL url = new URL("http://localhost:" + es.getMappedPort(9200) + "/" + TEST_INDEX);
            String mapping = IOUtils.toString(new File("../gepi-indexing/gepi-indexing-base/src/main/resources/elasticSearchMapping.json").toURI(), UTF_8);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("PUT");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            //  String auth = Base64.getEncoder().encodeToString(("elastic:changeme").getBytes());
            // urlConnection.setRequestProperty("Authorization", "Basic " +  auth);
            urlConnection.setDoOutput(true);
            IOUtils.write(mapping, urlConnection.getOutputStream(), UTF_8);
            log.info("Response for index creation: {}", urlConnection.getResponseMessage());

            if (urlConnection.getErrorStream() != null) {
                String error = IOUtils.toString(urlConnection.getErrorStream(), UTF_8);
                log.error("Error when creating index: {}", error);
            }


        }


        {
            // Index the test documents (created with gepi-indexing-pipeline and the JsonWriter).
            File dir = new File("src/test/resources/test-index-input");
            File[] relationDocuments = dir.listFiles((dir1, name) -> name.endsWith("json.gz"));
            log.debug("Reading {} test relation documents for indexing", relationDocuments.length);
            List<String> bulkCommandLines = new ArrayList<>(relationDocuments.length);
            ObjectMapper om = new ObjectMapper();
            for (File doc : relationDocuments) {
                String jsonContents = IOUtils.toString(FileUtilities.getInputStreamFromFile(doc), UTF_8);
                Map<String, Object> indexMap = new HashMap<>();
                indexMap.put("_index", TEST_INDEX);
                indexMap.put("_id", doc.getName().replace(".json.gz", ""));
                Map<String, Object> map = new HashMap<>();
                map.put("index", indexMap);

                bulkCommandLines.add(om.writeValueAsString(map));
                bulkCommandLines.add(jsonContents);
            }
            log.debug("Indexing test documents");
            URL url = new URL("http://localhost:" + es.getMappedPort(9200) + "/_bulk");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            OutputStream outputStream = urlConnection.getOutputStream();
            IOUtils.writeLines(bulkCommandLines, System.getProperty("line.separator"), outputStream, "UTF-8");
            log.debug("Response for indexing: {}", urlConnection.getResponseMessage());
        }
        // Wait for ES to finish its indexing
        Thread.sleep(2000);
        {
            URL url = new URL("http://localhost:" + es.getMappedPort(9200) + "/" + TEST_INDEX + "/_count");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            String countResponse = IOUtils.toString(urlConnection.getInputStream(), UTF_8);
            log.debug("Response for the count of documents: {}", countResponse);
            assertTrue(countResponse.contains("count\":57"));
        }

        Properties testconfig = new Properties();
        String configPath = "src/test/resources/testconfiguration.properties";
        testconfig.load(new FileInputStream(configPath));
        testconfig.setProperty(ElasticQuerySymbolConstants.ES_PORT, String.valueOf(es.getMappedPort(9200)));
        testconfig.setProperty(ElasticQuerySymbolConstants.ES_CLUSTER_NAME, TEST_CLUSTER);
        testconfig.store(new FileOutputStream(configPath), "The port number is automatically set in " + EventRetrievalServiceIntegrationTest.class.getCanonicalName());
    }

    @AfterClass
    public static void shutdown() {
        es.stop();
    }

    @Test
    public void testGetOutsideEvents() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.getOutsideEvents(IdConversionResult.of("10243"), Collections.emptyList(), null, null);
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(3);

        final List<String> eventTypes = outsideEvents.get().getEventList().stream().map(Event::getMainEventType).collect(Collectors.toList());
        final List<Argument> arguments = outsideEvents.get().getEventList().stream().map(Event::getArguments).flatMap(List::stream).collect(Collectors.toList());

        final List<String> geneids = arguments.stream().map(Argument::getGeneId).collect(Collectors.toList());

        assertThat(eventTypes).containsExactlyInAnyOrder("Positive_regulation", "Positive_regulation", "Binding");
        assertThat(geneids).containsExactlyInAnyOrder("10243", "10243", "10243", "2617", "26036", "8870");
    }

    @Test
    public void testGetOutsideEventsWithEventTypeFilter1() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.getOutsideEvents(IdConversionResult.of("10243"), Arrays.asList("Positive_regulation"), null, null);
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(2);
    }

    @Test
    public void testGetOutsideEventsWithEventTypeFilter2() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.getOutsideEvents(IdConversionResult.of("3930"), Arrays.asList("Negative_regulation"), null, null);
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testGetOutsideEventsWithSentenceFilter1() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.getOutsideEvents(IdConversionResult.of("10243"), Arrays.asList("Positive_regulation"), "essential", null);
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(2);
    }

    @Test
    public void testGetOutsideEventsWithSentenceFilter2() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> outsideEvents = eventRetrievalService.getOutsideEvents(IdConversionResult.of("3930"), Arrays.asList("Positive_regulation"), "stress", null);
        assertThat(outsideEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testGetBipartiteEvents() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.getBipartiteEvents(IdConversionResult.of("10243"), IdConversionResult.of("8870"), Collections.emptyList(), null, null);
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(1);

        final List<String> eventTypes = bipartiteEventsEvents.get().getEventList().stream().map(Event::getMainEventType).collect(Collectors.toList());
        final List<Argument> arguments = bipartiteEventsEvents.get().getEventList().stream().map(Event::getArguments).flatMap(List::stream).collect(Collectors.toList());

        final List<String> geneids = arguments.stream().map(Argument::getGeneId).collect(Collectors.toList());
        final List<String> argumentPrefNames = arguments.stream().map(Argument::getPreferredName).collect(Collectors.toList());

        assertThat(eventTypes).containsExactlyInAnyOrder("Binding");
        assertThat(geneids).containsExactlyInAnyOrder("10243", "8870");
        assertThat(argumentPrefNames).containsExactlyInAnyOrder("GPHN", "IER3");
    }

    @Test
    public void testGetBipartiteEventsWithEventTypeFilter1() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.getBipartiteEvents(IdConversionResult.of("10243"), IdConversionResult.of("8870"), Arrays.asList("Binding"), null, null);
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(1);
    }

    @Test
    public void testGetBipartiteEventsWithEventTypeFilter2() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.getBipartiteEvents(IdConversionResult.of("10243"), IdConversionResult.of("8870"), Arrays.asList("Negative_regulation"), null, null);
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void testGetBipartiteEventsWithSentenceFilter1() throws Exception {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.getBipartiteEvents(IdConversionResult.of("10243"), IdConversionResult.of("8870"), Arrays.asList("Binding"), "sequence", null);
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(1);
    }

    @Test
    public void testGetBipartiteEventsWithSentenceFilter2() throws Exception {
        // there should be 0 hits because the 'stress' keyword is not contained in the found event sentence
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        CompletableFuture<EventRetrievalResult> bipartiteEventsEvents = eventRetrievalService.getBipartiteEvents(IdConversionResult.of("10243"), IdConversionResult.of("8870"), Arrays.asList("Binding"), "stress", null);
        assertThat(bipartiteEventsEvents.get().getEventList().size()).isEqualTo(0);
    }

    @Test
    public void getTotalNumberOfEvents() {
        IEventRetrievalService eventRetrievalService = registry.getService(IEventRetrievalService.class);
        long totalNumberOfEvents = eventRetrievalService.getTotalNumberOfEvents();
        // the number of JSON files in the test index directory
        assertThat(totalNumberOfEvents).isEqualTo(57);
    }
}
