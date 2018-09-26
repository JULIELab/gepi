package de.julielab.gepi.indexing;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.File;
import java.io.InputStream;
import java.net.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.testng.Assert.assertTrue;

@Test(suiteName = "integration-tests")
public class ElasticITServer {
    public static final String TEST_INDEX = "gepi_testindex";
    private final static Logger log = LoggerFactory.getLogger(ElasticITServer.class);
    // in case we need to disable X-shield: https://stackoverflow.com/a/51172136/1314955
    public static GenericContainer es = new GenericContainer(
            new ImageFromDockerfile("esgepitest", true)
            .withFileFromClasspath("Dockerfile","dockercontext/Dockerfile")
            .withFileFromClasspath("elasticsearch-mapper-preanalyzed-5.4.0.zip","dockercontext/elasticsearch-mapper-preanalyzed-5.4.0.zip"))
            .withExposedPorts(9200, 9300)
            .withStartupTimeout(Duration.ofMinutes(2)
            );

    @BeforeSuite(groups = "integration-tests")
    public static void setup() throws Exception {
        // Setting the authentication for elasticsearch. The docker image comes with a default X-Shield installation.
        //Authenticator.setDefault(new Authenticator() {
        //protected PasswordAuthentication getPasswordAuthentication() {
        //      return new PasswordAuthentication("elastic", "changeme".toCharArray());
        //     }
        //});
        es.start();
       // es.withClasspathResourceMapping("elasticsearch-mapper-preanalyzed-5.4.0.jar",
         //       "/usr/share/elasticsearch/plugins/elasticsearch-mapper-preanalyzed-5.4.0.jar",
           //     BindMode.READ_WRITE);
        Slf4jLogConsumer toStringConsumer = new Slf4jLogConsumer(log);
        es.followOutput(toStringConsumer, OutputFrame.OutputType.STDOUT);

        {
            // Create the test index
            URL url = new URL("http://localhost:" + es.getMappedPort(9200) + "/" + TEST_INDEX);
            String mapping = IOUtils.toString(new File("src/main/resources/elasticSearchMapping.json").toURI());
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("PUT");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            //  String auth = Base64.getEncoder().encodeToString(("elastic:changeme").getBytes());
            // urlConnection.setRequestProperty("Authorization", "Basic " +  auth);
            urlConnection.setDoOutput(true);
            IOUtils.write(mapping, urlConnection.getOutputStream());
            try {
                if (urlConnection.getInputStream() != null) {
                    String response = IOUtils.toString(urlConnection.getInputStream());
                    log.info("Response for index creation: {}", response);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (urlConnection.getErrorStream() != null) {
                String error = IOUtils.toString(urlConnection.getErrorStream());
                log.error("Error when creating index: {}", error);
            }
        }

        {
            URL url = new URL("http://localhost:" + es.getMappedPort(9200) + "/" + TEST_INDEX);
            URLConnection urlConnection = url.openConnection();
            System.out.println(IOUtils.toString(urlConnection.getInputStream()));
        }


    }

    @AfterSuite(groups = "integration-tests")
    public static void shutdownSuite() {
        es.stop();
    }
}
