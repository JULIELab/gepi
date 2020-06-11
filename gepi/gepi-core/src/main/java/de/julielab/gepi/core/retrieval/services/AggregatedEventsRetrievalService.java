package de.julielab.gepi.core.retrieval.services;

import de.julielab.gepi.core.GepiCoreSymbolConstants;
import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.neo4j.driver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class AggregatedEventsRetrievalService implements IAggregatedEventsRetrievalService {
    private final static Logger log = LoggerFactory.getLogger(AggregatedEventsRetrievalService.class);
    private final Driver driver;

    public AggregatedEventsRetrievalService(Logger log, @Symbol(GepiCoreSymbolConstants.NEO4J_BOLT_URL) String boltUrl) {
        driver = GraphDatabase.driver(boltUrl, AuthTokens.basic("neo4j", "julielab"));
    }

    @Override
    public CompletableFuture<AggregatedEventsRetrievalResult> getEvents(Future<Stream<String>> idStream1, List<String> eventTypes) {
        return CompletableFuture.supplyAsync(() -> {
            long time = System.currentTimeMillis();
            String eventTypeList = String.join("|", eventTypes);
            String queryTemplate = String.format("MATCH (a:AGGREGATE_GENEGROUP)-[:HAS_ELEMENT]->(c:CONCEPT) WHERE c.sourceIds0 IN $aList \n" +
                    "WITH DISTINCT a\n" +
                    "MATCH p=(a)-[:HAS_ELEMENT]->(:CONCEPT)-[r:%s]-(c:CONCEPT) WHERE NOT (c)<-[:HAS_ELEMENT]-()\n" +
                    "RETURN a.preferredName AS arg1Name,c.preferredName AS arg2Name,a.sourceIds0 AS arg1Id,c.sourceIds0 AS arg2Id,sum(r.totalCount) AS count\n" +
                    "\n" +
                    "UNION ALL\n" +
                    "MATCH (a:AGGREGATE_GENEGROUP)-[:HAS_ELEMENT]->(c) WHERE c.sourceIds0 IN $aList \n" +
                    "WITH DISTINCT a\n" +
                    "MATCH p=(a)-[:HAS_ELEMENT]->(:CONCEPT)-[r:%s]-(:CONCEPT)<-[:HAS_ELEMENT]-(a2:AGGREGATE_GENEGROUP)\n" +
                    "RETURN a.preferredName AS arg1Name,a2.preferredName AS arg2Name,a.sourceIds0 AS arg1Id,a2.sourceIds0 AS arg2Id,sum(r.totalCount) AS count\n" +
                    "\n" +
                    "UNION ALL\n" +
                    "MATCH (c:CONCEPT)-[r:%s]-(c2:CONCEPT) WHERE c.sourceIds0 IN $aList AND NOT (:AGGREGATE_GENEGROUP)-[:HAS_ELEMENT]->(c) AND NOT (c2)<-[:HAS_ELEMENT]-()\n" +
                    "RETURN c.preferredName AS arg1Name,c2.preferredName AS arg2Name,c.sourceIds0 AS arg1Id,c2.sourceIds0 AS arg2Id,sum(r.totalCount) AS count\n" +
                    "\n" +
                    "UNION ALL\n" +
                    "MATCH (c:CONCEPT)-[r:%s]-(:CONCEPT)<-[:HAS_ELEMENT]-(a:AGGREGATE_GENEGROUP) WHERE c.sourceIds0 IN $aList AND NOT (:AGGREGATE_GENEGROUP)-[:HAS_ELEMENT]->(c)\n" +
                    "RETURN c.preferredName AS arg1Name,a.preferredName AS arg2Name,c.sourceIds0 AS arg1Id,a.sourceIds0 AS arg2Id,sum(r.totalCount) AS count", eventTypeList, eventTypeList, eventTypeList, eventTypeList);
            try (Session s = driver.session(); Transaction tx = s.beginTransaction()) {
                try {
                    Result cypherResult = tx.run(queryTemplate, Map.of("aList", idStream1.get()));
                    AggregatedEventsRetrievalResult retrievalResult = new AggregatedEventsRetrievalResult();
                    while (cypherResult.hasNext()) {
                        Record record = cypherResult.next();
                        retrievalResult.add(
                                record.get("arg1Name").asString(),
                                record.get("arg2Name").asString(),
                                record.get("arg1Id").asString(),
                                record.get("arg2Id").asString(),
                                record.get("count").asInt());
                    }
                    time = System.currentTimeMillis() - time;
                    log.debug("Retrieving A-Search results from Neo4j of size {} took {}ms", retrievalResult.size(), time);
                    return retrievalResult;
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<AggregatedEventsRetrievalResult> getEvents(Future<Stream<String>> idStream1, Future<Stream<String>> idStream2, List<String> eventTypes) {
        return CompletableFuture.supplyAsync(() -> {
            long time = System.currentTimeMillis();
            String queryTemplate = String.format("MATCH (c:CONCEPT) WHERE c.sourceIds0 IN $aList \n" +
                    "OPTIONAL MATCH (a:AGGREGATE_GENEGROUP)-[:HAS_ELEMENT]->(c) WITH\n" +
                    "CASE a IS NULL\n" +
                    "WHEN true THEN c\n" +
                    "ELSE a\n" +
                    "END as c1\n" +
                    "MATCH (c:CONCEPT) WHERE c.sourceIds0 IN $bList \n" +
                    "OPTIONAL MATCH (a:AGGREGATE_GENEGROUP)-[:HAS_ELEMENT]->(c) WITH c1,\n" +
                    "CASE a IS NULL\n" +
                    "WHEN true THEN c\n" +
                    "ELSE a\n" +
                    "END as c2\n" +
                    "WITH DISTINCT c1,c2,\n" +
                    "CASE\n" +
                    "WHEN c1:AGGREGATE_GENEGROUP XOR c2:AGGREGATE_GENEGROUP THEN 2\n" +
                    "WHEN c1:AGGREGATE_GENEGROUP AND c2:AGGREGATE_GENEGROUP THEN 3\n" +
                    "ELSE 1\n" +
                    "END as l\n" +
                    "MATCH p=allShortestPaths((c1)-[r:HAS_ELEMENT|%s*..3]-(c2))\n" +
                    "WITH c1,c2,p,l,REDUCE(tc = 0, c IN [r in relationships(p) where EXISTS(r.totalCount) | r.totalCount] | tc + c) AS counts\n" +
                    "WHERE LENGTH(p) = l\n" +
                    "return c1.preferredName as arg1Name,c2.preferredName as arg2Name, c1.sourceIds0 as arg1Id,c2.sourceIds0 as arg2Id,sum(counts) AS count", String.join("|", eventTypes));
            try (Session s = driver.session(); Transaction tx = s.beginTransaction()) {
                try {
                    Result cypherResult = tx.run(queryTemplate, Map.of("aList", idStream1.get(), "bList", idStream2.get()));
                    AggregatedEventsRetrievalResult retrievalResult = new AggregatedEventsRetrievalResult();
                    while (cypherResult.hasNext()) {
                        Record record = cypherResult.next();
                        retrievalResult.add(
                                record.get("arg1Name").asString(),
                                record.get("arg2Name").asString(),
                                record.get("arg1Id").asString(),
                                record.get("arg2Id").asString(),
                                record.get("count").asInt());
                    }
                    time = System.currentTimeMillis() - time;
                    log.debug("Retrieving A-Search results from Neo4j of size {} took {}ms", retrievalResult.size(), time);
                    return retrievalResult;
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            return null;
        });
    }
}
