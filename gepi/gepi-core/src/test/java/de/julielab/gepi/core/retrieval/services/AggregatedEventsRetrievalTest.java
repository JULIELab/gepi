package de.julielab.gepi.core.retrieval.services;

import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.harness.junit.rule.Neo4jRule;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.apache.commons.lang3.concurrent.ConcurrentUtils.constantFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

// we don't use the service at the moment and, for some reason, the tests are very slow
@Ignore
public class AggregatedEventsRetrievalTest {
    @Rule
    public Neo4jRule neo4j = new Neo4jRule().withFixture(graphDatabaseService -> {
        setupDB(graphDatabaseService);
        return null;
    });

    @Test
    public void retrieveWithAggregateResolution() throws Exception {
        AggregatedEventsRetrievalService retrieval = new AggregatedEventsRetrievalService(LoggerFactory.getLogger(AggregatedEventsRetrievalService.class), null, neo4j.boltURI().toString());
        AggregatedEventsRetrievalResult events = retrieval.getEvents(constantFuture(Stream.of("c11")), constantFuture(Stream.of("c22")), List.of("Binding", "Regulation")).get();
        assertThat(events.size()).isEqualTo(1);
        assertThatCode(() -> events.seek(0)).doesNotThrowAnyException();
        assertThat(events.getArg1Id()).isEqualTo("a1");
        assertThat(events.getArg2Id()).isEqualTo("a2");
        assertThat(events.getArg2Name()).isEqualTo("Aggregate2");
        assertThat(events.getArg1Name()).isEqualTo("Aggregate1");
        assertThat(events.getCount()).isEqualTo(11);
    }

    @Test
    public void retrieveCompleteAB() throws Exception {
        // "complete" here just refers to the fact that we search for all "left" genes to all "right" genes according to the set graph (check the sketch in setup DB)
        AggregatedEventsRetrievalService retrieval = new AggregatedEventsRetrievalService(LoggerFactory.getLogger(AggregatedEventsRetrievalService.class), null, neo4j.boltURI().toString());
        AggregatedEventsRetrievalResult events = retrieval.getEvents(constantFuture(Stream.of("c11", "c12", "c3", "c4")), constantFuture(Stream.of("c21", "c22", "c5")), List.of("Binding", "Regulation")).get();
        assertThat(events.size()).isEqualTo(3);
        while (events.increment()) {
            if (events.getArg1Id().equals("a1") && events.getArg2Id().equals("a2"))
                assertThat(events.getCount()).isEqualTo(11);
            else if (events.getArg1Id().equals("c3") && events.getArg2Id().equals("a2"))
                assertThat(events.getCount()).isEqualTo(2);
            else if (events.getArg1Id().equals("c4") && events.getArg2Id().equals("c5"))
                assertThat(events.getCount()).isEqualTo(8);
            else
                Assertions.fail("An unexpected relation occurred between " + events.getArg1Id() + " and " + events.getArg2Id());
        }
    }

    @Test
    public void retrieveCompleteA() throws Exception {
        // This should result in the same as retrieveCompleteAB because in both cases we retrieve the whole test graph
        AggregatedEventsRetrievalService retrieval = new AggregatedEventsRetrievalService(LoggerFactory.getLogger(AggregatedEventsRetrievalService.class), null, neo4j.boltURI().toString());
        AggregatedEventsRetrievalResult events = retrieval.getEvents(constantFuture(Stream.of("c11", "c12", "c3", "c4")), List.of("Binding", "Regulation")).get();
        assertThat(events.size()).isEqualTo(3);
        while (events.increment()) {
            if (events.getArg1Id().equals("a1") && events.getArg2Id().equals("a2"))
                assertThat(events.getCount()).isEqualTo(11);
            else if (events.getArg1Id().equals("c3") && events.getArg2Id().equals("a2"))
                assertThat(events.getCount()).isEqualTo(2);
            else if (events.getArg1Id().equals("c4") && events.getArg2Id().equals("c5"))
                assertThat(events.getCount()).isEqualTo(8);
            else
                Assertions.fail("An unexpected relation occurred between " + events.getArg1Id() + " and " + events.getArg2Id());
        }
    }

    @Test
    public void retrieveCompleteARegulation() throws Exception {
        // Query all nodes but restrict to regulation
        AggregatedEventsRetrievalService retrieval = new AggregatedEventsRetrievalService(LoggerFactory.getLogger(AggregatedEventsRetrievalService.class), null, neo4j.boltURI().toString());
        AggregatedEventsRetrievalResult events = retrieval.getEvents(constantFuture(Stream.of("c11", "c12", "c3", "c4")), List.of("Regulation")).get();
        assertThat(events.size()).isEqualTo(2);
        while (events.increment()) {
            if (events.getArg1Id().equals("a1") && events.getArg2Id().equals("a2"))
                assertThat(events.getCount()).isEqualTo(6);
            else if (events.getArg1Id().equals("c4") && events.getArg2Id().equals("c5"))
                assertThat(events.getCount()).isEqualTo(3);
            else
                Assertions.fail("An unexpected relation occurred between " + events.getArg1Id() + " and " + events.getArg2Id());
        }
    }

    @Test
    public void retrieveCompleteABRegulation() throws Exception {
        // Query all nodes but restrict to regulation; again the same result as with the A-search variant
        AggregatedEventsRetrievalService retrieval = new AggregatedEventsRetrievalService(LoggerFactory.getLogger(AggregatedEventsRetrievalService.class), null, neo4j.boltURI().toString());
        AggregatedEventsRetrievalResult events = retrieval.getEvents(constantFuture(Stream.of("c11", "c12", "c3", "c4")), constantFuture(Stream.of("c21", "c22", "c5")), List.of("Regulation")).get();
        assertThat(events.size()).isEqualTo(2);
        while (events.increment()) {
            if (events.getArg1Id().equals("a1") && events.getArg2Id().equals("a2"))
                assertThat(events.getCount()).isEqualTo(6);
            else if (events.getArg1Id().equals("c4") && events.getArg2Id().equals("c5"))
                assertThat(events.getCount()).isEqualTo(3);
            else
                Assertions.fail("An unexpected relation occurred between " + events.getArg1Id() + " and " + events.getArg2Id());
        }
    }

    @Test
    public void doASearch1() throws Exception {
        AggregatedEventsRetrievalService retrieval = new AggregatedEventsRetrievalService(LoggerFactory.getLogger(AggregatedEventsRetrievalService.class), null, neo4j.boltURI().toString());
        AggregatedEventsRetrievalResult events = retrieval.getEvents(constantFuture(Stream.of("c12")), List.of("Binding", "Regulation")).get();
        // The result should actually exactly be the same as in retrieveWithAggregateResolution
        assertThat(events.size()).isEqualTo(1);
        events.seek(0);
        assertThat(events.getArg1Id()).isEqualTo("a1");
        assertThat(events.getArg2Id()).isEqualTo("a2");
        assertThat(events.getArg2Name()).isEqualTo("Aggregate2");
        assertThat(events.getArg1Name()).isEqualTo("Aggregate1");
        assertThat(events.getCount()).isEqualTo(11);
    }

    @Test
    public void doASearch2() throws Exception {
        AggregatedEventsRetrievalService retrieval = new AggregatedEventsRetrievalService(LoggerFactory.getLogger(AggregatedEventsRetrievalService.class), null, neo4j.boltURI().toString());
        AggregatedEventsRetrievalResult events = retrieval.getEvents(constantFuture(Stream.of("c12", "c3")), List.of("Binding", "Regulation")).get();
        assertThat(events.size()).isEqualTo(2);
        while (events.increment()) {
            if (events.getArg1Id().equals("a1") && events.getArg2Id().equals("a2"))
                assertThat(events.getCount()).isEqualTo(11);
            else if (events.getArg1Id().equals("c3") && events.getArg2Id().equals("a2"))
                assertThat(events.getCount()).isEqualTo(2);
            else
                Assertions.fail("An unexpected relation occurred between " + events.getArg1Id() + " and " + events.getArg2Id());
        }
    }

    @Test
    public void doASearch3() throws Exception {
        AggregatedEventsRetrievalService retrieval = new AggregatedEventsRetrievalService(LoggerFactory.getLogger(AggregatedEventsRetrievalService.class), null, neo4j.boltURI().toString());
        AggregatedEventsRetrievalResult events = retrieval.getEvents(constantFuture(Stream.of("c4")), List.of("Binding", "Regulation")).get();
        // The result should actually exactly be the same as in retrieveWithAggregateResolution
        assertThat(events.size()).isEqualTo(1);
        events.seek(0);
        assertThat(events.getArg1Id()).isEqualTo("c4");
        assertThat(events.getArg2Id()).isEqualTo("c5");
        assertThat(events.getArg1Name()).isEqualTo("Concept4");
        assertThat(events.getArg2Name()).isEqualTo("Concept5");
        assertThat(events.getCount()).isEqualTo(8);
    }

    /**
     * Creates the test graph
     *
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
        // create a test graph:
        // a1--c11--reg(2)bind(5)--c21--a2
        //   \-c12------reg(4)-----c22-/
        //   c3-------bind(2)-----/
        //   c4----reg(3)bind(5)---c5
        try (Transaction tx = graphDb.beginTx()) {
            // Create two aggregates with two nodes, respectively
            Node a1 = tx.createNode(aggGeneGroupLabel);
            Node a2 = tx.createNode(aggGeneGroupLabel);
            Node c11 = tx.createNode(conceptLabel);
            Node c12 = tx.createNode(conceptLabel);
            Node c21 = tx.createNode(conceptLabel);
            Node c22 = tx.createNode(conceptLabel);
            Node c3 = tx.createNode(conceptLabel);
            Node c4 = tx.createNode(conceptLabel);
            Node c5 = tx.createNode(conceptLabel);
            a1.setProperty(idProp, "a1");
            a2.setProperty(idProp, "a2");
            c11.setProperty(idProp, "c11");
            c12.setProperty(idProp, "c12");
            c21.setProperty(idProp, "c21");
            c22.setProperty(idProp, "c22");
            c3.setProperty(idProp, "c3");
            c4.setProperty(idProp, "c4");
            c5.setProperty(idProp, "c5");
            a1.setProperty(nameProp, "Aggregate1");
            a2.setProperty(nameProp, "Aggregate2");
            c11.setProperty(nameProp, "Concept11");
            c12.setProperty(nameProp, "Concept12");
            c21.setProperty(nameProp, "Concept21");
            c22.setProperty(nameProp, "Concept22");
            c3.setProperty(nameProp, "Concept3");
            c4.setProperty(nameProp, "Concept4");
            c5.setProperty(nameProp, "Concept5");

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
            Relationship b3_22 = c3.createRelationshipTo(c22, binding);
            b3_22.setProperty(countProp, 2);
            Relationship r4_5 = c4.createRelationshipTo(c5, regulation);
            r4_5.setProperty(countProp, 3);
            Relationship b4_5 = c4.createRelationshipTo(c5, binding);
            b4_5.setProperty(countProp, 5);

            tx.commit();
            // Cypher variant to build this graph:
            // create p=(a1:AGGREGATE{sourceIds0:'a1'})-[:HAS_ELEMENT]->(c11:CONCEPT{sourceIds0:'c11'})-[:Regulation {totalCount:2}]->(c21:CONCEPT{sourceIds0:'c21'})<-[:HAS_ELEMENT]-(a:AGGREGATE{sourceIds0:'a2'}) return p
            // match (c:CONCEPT {sourceIds0:'c11'}),(c2:CONCEPT {sourceIds0: 'c21'}) create (c)-[:Binding{totalCount:5}]->(c2)
            // match (a1:AGGREGATE {sourceIds0:'a1'}),(a2:AGGREGATE {sourceIds0:'a2'}) create p=(a1)-[:HAS_ELEMENT]->(c12:CONCEPT {sourceIds0:'c12'})-[:Regulation {totalCount:4}]->(c22:CONCEPT {sourceIds0:'c22'})<-[:HAS_ELEMENT]-(a2) return p
            // match (c22:CONCEPT {sourceIds0:'c22'}) create (c3:CONCEPT {sourceIds0:'c3'})-[:Binding {totalCount:2}]->(c22)
            // create p=(c4:CONCEPT{sourceIds0:'c4'})-[:Regulation {totalCount:3}]->(c5:CONCEPT{sourceIds0:'c5'}) return p
            // match (c4:CONCEPT{sourceIds0:'c4'}),(c5:CONCEPT{sourceIds0:'c5'}) create p=(c4)-[:Binding {totalCount:5}]->(c5) return p
        }
    }
}