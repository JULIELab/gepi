package de.julielab.gepi.core.retrieval.services;

import de.julielab.elastic.query.ElasticQuerySymbolConstants;
import de.julielab.java.utilities.FileUtilities;
import de.julielab.java.utilities.IOStreamUtilities;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestcontainersElasticSearch {
    public static final String TEST_INDEX = "gepi_testindex";
    public static final String TEST_CLUSTER = "gepi_testcluster";
    private final static Logger log = LoggerFactory.getLogger(TestcontainersElasticSearch.class);

    public static GenericContainer getEsTestContainer() {
        return new GenericContainer(
                new ImageFromDockerfile("gepicoreestest", true)
                        .withFileFromClasspath("Dockerfile", "dockercontext/Dockerfile")
                        .withFileFromClasspath("elasticsearch-mapper-preanalyzed-7.17.7-SNAPSHOT.zip", "dockercontext/elasticsearch-mapper-preanalyzed-7.17.7-SNAPSHOT.zip"))
                .withExposedPorts(9200)
                .withEnv("cluster.name", TEST_CLUSTER)
                .withEnv("discovery.type", "single-node");
    }

    /**
     * Sets up a GePI index and adds the test documents from the <code>gepi-test-data</code> project. These data
     * were created from a set of PubMed documents whose IDs are to be found under the <code>src/main/resources</code>
     * directory of the <code>gepi-test-data</code> project.
     *
     * @param es
     * @throws IOException
     * @throws InterruptedException
     */
    public static void populateEsTestInstance(GenericContainer es) throws IOException, InterruptedException {
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
            File dir = new File("target/generated-resources/test-index-input");
            File[] relationDocuments = dir.listFiles((dir1, name) -> name.endsWith("json") || name.endsWith(".json.gz"));
            if (relationDocuments == null || relationDocuments.length == 0)
                fail("The test document files could not be found. You might need to mvn build the project first to unpack the data which is imported from the gepi-test-data module.");
            log.debug("Reading {} test relation documents for indexing", relationDocuments.length);
            List<String> bulkCommandLines = new ArrayList<>(relationDocuments.length);
            ObjectMapper om = new ObjectMapper();
            for (File doc : relationDocuments) {
                String jsonContents = IOUtils.toString(FileUtilities.getInputStreamFromFile(doc), UTF_8);
                jsonContents = StringUtils.normalizeSpace(jsonContents);
                Map<String, Object> indexMap = new HashMap<>();
                indexMap.put("_index", TEST_INDEX);
                indexMap.put("_id", doc.getName().replace(".json.gz", "").replace(".json", ""));
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
            if (urlConnection.getErrorStream() != null)
                log.debug("Error messages for indexing: {}", IOStreamUtilities.getStringFromInputStream(urlConnection.getErrorStream()));
        }
        // Wait for ES to finish its indexing
        Thread.sleep(2000);
        {
            URL url = new URL("http://localhost:" + es.getMappedPort(9200) + "/" + TEST_INDEX + "/_count");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            String countResponse = IOUtils.toString(urlConnection.getInputStream(), UTF_8);
            log.debug("Response for the count of documents: {}", countResponse);
//            assertTrue(countResponse.contains("count\":57"));
        }

        Properties testconfig = new Properties();
        String configPath = "src/test/resources/testconfiguration.properties";
        testconfig.load(new FileInputStream(configPath));
        testconfig.setProperty(ElasticQuerySymbolConstants.ES_PORT, String.valueOf(es.getMappedPort(9200)));
        testconfig.setProperty(ElasticQuerySymbolConstants.ES_CLUSTER_NAME, TEST_CLUSTER);
        testconfig.store(new FileOutputStream(configPath), "The port number is automatically set in " + EventRetrievalServiceIntegrationTest.class.getCanonicalName());
    }

}
