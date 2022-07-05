package de.julielab.gepi.webapp.services;

import de.julielab.elastic.query.components.ISearchServerComponent;
import de.julielab.gepi.core.retrieval.services.TestcontainersElasticSearch;
import de.julielab.gepi.core.services.GePiCoreTestModule;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import static org.assertj.core.api.Assertions.*;
public class StatisticsCollectorTest {
    public static GenericContainer es = TestcontainersElasticSearch.getEsTestContainer();
    private static Registry registry;
    private static IStatisticsCollector statisticsCollector;

    @BeforeClass
    public static void setup() throws Exception {
        es.start();
        TestcontainersElasticSearch.populateEsTestInstance(es);

        registry = RegistryBuilder.buildAndStartupRegistry(GePiCoreTestModule.class);
        statisticsCollector = new StatisticsCollector(TestcontainersElasticSearch.TEST_INDEX, LoggerFactory.getLogger(StatisticsCollector.class), registry.getService(ISearchServerComponent.class));
        statisticsCollector.run();
    }

    @AfterClass
    public static void shutdown() {
        es.stop();
    }

    @Test
    public void testGetNumDocuments() {
        statisticsCollector.run();
        assertThat(statisticsCollector.getStats().getNumTotalInteractions()).isGreaterThanOrEqualTo(57);
    }
}