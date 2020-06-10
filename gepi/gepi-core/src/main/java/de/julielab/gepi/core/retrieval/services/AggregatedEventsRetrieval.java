package de.julielab.gepi.core.retrieval.services;

import de.julielab.gepi.core.GepiCoreSymbolConstants;
import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.neo4j.driver.*;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class AggregatedEventsRetrieval implements IAggregatedEventsRetrieval {

    private final Driver driver;

    public AggregatedEventsRetrieval(Logger log, @Symbol(GepiCoreSymbolConstants.NEO4J_BOLT_URL) String boltUrl) {
        driver = GraphDatabase.driver(boltUrl, AuthTokens.basic("neo4j", "julielab"));
    }

    @Override
    public AggregatedEventsRetrievalResult getEvents(Future<Stream<String>> idStream1, List<String> eventTypes) {
        return null;
    }

    @Override
    public AggregatedEventsRetrievalResult getEvents(Future<Stream<String>> idStream1, Future<Stream<String>> idStream2, List<String> eventTypes) {
        String queryTemplate = "MATCH (c:CONCEPT) WHERE c.sourceIds0 IN $aList \n" +
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
                "WITH c1,c2,\n" +
                "CASE\n" +
                "WHEN c1:AGGREGATE_GENEGROUP XOR c2:AGGREGATE_GENEGROUP THEN 2\n" +
                "WHEN c1:AGGREGATE_GENEGROUP AND c2:AGGREGATE_GENEGROUP THEN 3\n" +
                "ELSE 1\n" +
                "END as l\n" +
                "MATCH p=allShortestPaths((c1)-[:HAS_ELEMENT|Regulation|Binding|Positive_regulation*..3]-(c2))\n" +
                "WITH c1,c2,p,l,REDUCE(tc = 0, c IN [r in relationships(p) where EXISTS(r.totalCount) | r.totalCount] | tc + c) AS counts\n" +
                "WHERE LENGTH(p) = l\n" +
                "return c1.preferredName as arg1Name,c2.preferredName as arg2Name, c1.sourceIds0 as arg1Id,c2.sourceIds0 as arg2Id,sum(counts) AS count";
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
                return retrievalResult;
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
