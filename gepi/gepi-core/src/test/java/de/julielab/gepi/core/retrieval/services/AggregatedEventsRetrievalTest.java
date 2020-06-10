package de.julielab.gepi.core.retrieval.services;

import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.harness.junit.rule.Neo4jRule;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.apache.commons.lang3.concurrent.ConcurrentUtils.constantFuture;
import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.*;
public class AggregatedEventsRetrievalTest {
    @Rule
    public Neo4jRule neo4j = new Neo4jRule().withFixture(graphDatabaseService -> {
        setupDB(graphDatabaseService);
        return null;
    });

    @Test
    public void retrieve() {
        AggregatedEventsRetrieval retrieval = new AggregatedEventsRetrieval(LoggerFactory.getLogger(AggregatedEventsRetrieval.class), neo4j.boltURI().toString());
        AggregatedEventsRetrievalResult events = retrieval.getEvents(constantFuture(Stream.of("c11")), constantFuture(Stream.of("c22")), List.of("Binding", "Regulation"));
        assertThat(events.size()).isEqualTo(1);
        assertThatCode(() -> events.seek(0)).doesNotThrowAnyException();
        assertThat(events.getArg1Id()).isEqualTo("a1");
        assertThat(events.getArg2Id()).isEqualTo("a2");
        assertThat(events.getArg2Name()).isEqualTo("Aggregate2");
        assertThat(events.getArg1Name()).isEqualTo("Aggregate1");
        assertThat(events.getCount()).isEqualTo(11);
    }

    /**
     * Creates the test graph
     * @param graphDb
     */
    public void setupDB(GraphDatabaseService graphDb) {
        Label conceptLabel = Label.label("CONCEPT");
        Label aggGeneGroupLabel = Label.label("AGGREGATE_GENEGROUP");
        RelationshipType hasElement = RelationshipType.withName("HAS_ELEMENT");
        RelationshipType binding = RelationshipType.withName("Binding");
        RelationshipType regulation = RelationshipType.withName("Regulation");
        String idProp = "sourceIds0";
        String nameProp = "preferredName";
        String countProp = "totalCount";
        try(Transaction tx = graphDb.beginTx()) {
            // Create two aggregates with two nodes, respectively
            Node a1 = tx.createNode(aggGeneGroupLabel);
            Node a2 = tx.createNode(aggGeneGroupLabel);
            Node c11 = tx.createNode(conceptLabel);
            Node c12 = tx.createNode(conceptLabel);
            Node c21 = tx.createNode(conceptLabel);
            Node c22 = tx.createNode(conceptLabel);
            a1.setProperty(idProp, "a1");
            a2.setProperty(idProp, "a2");
            c11.setProperty(idProp, "c11");
            c12.setProperty(idProp, "c12");
            c21.setProperty(idProp, "c21");
            c22.setProperty(idProp, "c22");
            a1.setProperty(nameProp, "Aggregate1");
            a2.setProperty(nameProp, "Aggregate2");
            c11.setProperty(nameProp, "Concept11");
            c12.setProperty(nameProp, "Concept12");
            c21.setProperty(nameProp, "Concept21");
            c22.setProperty(nameProp, "Concept22");

            // Connect the aggregates to their elements
            a1.createRelationshipTo(c11, hasElement);
            a1.createRelationshipTo(c12, hasElement);
            a2.createRelationshipTo(c21, hasElement);
            a2.createRelationshipTo(c22, hasElement);

            // Create interactions between the concepts
            // c11 and c21 have two relations, regulation and binding
            // c12 and c22 have a regulation relationship
            Relationship r11_21 = c11.createRelationshipTo(c21, regulation);
            r11_21.setProperty(countProp, 2);
            Relationship b11_21 = c11.createRelationshipTo(c21, binding);
            b11_21.setProperty(countProp, 5);
            Relationship r12_22 = c12.createRelationshipTo(c22, regulation);
            r12_22.setProperty(countProp, 4);

            tx.commit();
        }
    }
}