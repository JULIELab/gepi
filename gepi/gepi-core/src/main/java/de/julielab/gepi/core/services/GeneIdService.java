package de.julielab.gepi.core.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import de.julielab.gepi.core.GepiCoreSymbolConstants;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.neo4j.driver.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.neo4j.driver.Values.parameters;

public class GeneIdService implements IGeneIdService {


    private Logger log;
    private String boltUrl;

    public GeneIdService(Logger log, @Symbol(GepiCoreSymbolConstants.NEO4J_BOLT_URL) String boltUrl) {
        this.log = log;

        this.boltUrl = boltUrl;
    }

    @Override
    public Future<IdConversionResult> convert(Stream<String> stream, IdType from, IdType to) {
        List<String> sourceIds = stream.collect(Collectors.toList());
        CompletableFuture<Multimap<String, String>> convertedIds;
        if (to == IdType.GEPI_AGGREGATE) {
            if (from == IdType.GENE_NAME) {
                convertedIds = convertGeneNames2AggregateIds(sourceIds.stream());
            } else if (from == IdType.GENE) {
                convertedIds = convertGene2AggregateIds(sourceIds.stream());
            } else {
                throw new IllegalArgumentException("From-ID type '" + from + "' is currently not supported");
            }
        } else if (to == IdType.GENE) {
            if (from == IdType.GENE_NAME) {
                convertedIds = convertGeneNames2GeneIds(sourceIds.stream());
            } else if (from == IdType.GENE) {
                HashMultimap<String, String> map = HashMultimap.create();
                sourceIds.forEach(id -> map.put(id, id));
                convertedIds = CompletableFuture.completedFuture(map);
            } else {
                throw new IllegalArgumentException("From-ID type '" + from + "' is currently not supported");
            }
        } else {
            throw new IllegalArgumentException("To-ID type '" + to + "' is currently not supported.");
        }
        Future<Multimap<String, String>> finalConvertedIds = convertedIds;
        return CompletableFuture.supplyAsync(() -> {
            try {
                Multimap<String, String> idMapping = finalConvertedIds.get();
//                idMapping.forEach((k,v) ->log.debug("{} -> {}", k, v));
                IdConversionResult idConversionResult = new IdConversionResult(sourceIds, idMapping, from, to);
                return idConversionResult;
            } catch (Exception e) {
                log.error("Could not create an IdConversionResult instance", e);
                throw new IllegalStateException(e);
            }
        });
    }

    private CompletableFuture<Multimap<String, String>> convertGeneNames2GeneIds(Stream<String> geneNames) {
        return CompletableFuture.supplyAsync(() -> {
            Driver driver = GraphDatabase.driver(boltUrl, AuthTokens.basic("neo4j", "julielab"));

            try (Session session = driver.session()) {

                return session.readTransaction(tx -> {
                    Record record;
                    Multimap<String, String> topAtids = HashMultimap.create();

                    String[] searchInput = geneNames.map(String::toLowerCase).toArray(String[]::new);
                    log.debug("Running query to map gene names to NCBI gene IDs.");
                    String cypher = "MATCH (n:CONCEPT) WHERE n:ID_MAP_NCBI_GENES AND n.preferredName_lc IN $geneNames " +
                            "RETURN DISTINCT n.preferredName_lc AS SOURCE_ID,n.originalId AS SEARCH_ID";
                    Result result = tx.run(
                            cypher,
                            parameters("geneNames", searchInput));

                    while (result.hasNext()) {
                        record = result.next();
                        topAtids.put(record.get("SOURCE_ID").asString(), record.get("SEARCH_ID").asString());
                    }
                    return topAtids;
                });
            }
        });
    }

    @Override
    public Future<Stream<String>> convertUniprot2Gene(Stream<String> uniprotIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<Stream<String>> convertGene2Gepi(Stream<String> geneIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<Multimap<String, String>> convertGeneNames2AggregateIds(Stream<String> geneNames) {
        return CompletableFuture.supplyAsync(() -> {
            Driver driver = GraphDatabase.driver(boltUrl, AuthTokens.basic("neo4j", "julielab"));

            try (Session session = driver.session()) {

                return session.readTransaction(tx -> {
                    Record record;
                    Multimap<String, String> topAtids = HashMultimap.create();

                    String[] searchInput = geneNames.map(String::toLowerCase).toArray(String[]::new);
                    String cypher = "MATCH (a:AGGREGATE_GENEGROUP) WHERE a.preferredName_lc IN $geneNames RETURN a.preferredName_lc AS SOURCE_ID,a.id AS SEARCH_ID\n" +
                            "UNION\n" +
                            "MATCH (c:CONCEPT)<-[:HAS_ROOT_CONCEPT]-(f:FACET) WHERE c:ID_MAP_NCBI_GENES AND c.preferredName_lc IN $geneNames RETURN c.preferredName_lc AS SOURCE_ID,c.id AS SEARCH_ID";
                    log.debug("Running query to map gene names to aggregate IDs.");
                    Result result = tx.run(
                            cypher,
                            parameters("geneNames", searchInput));

                    while (result.hasNext()) {
                        record = result.next();
                        topAtids.put(record.get("SOURCE_ID").asString(), record.get("SEARCH_ID").asString());
                    }
                    return topAtids;
                });
            }
        });
    }

    /**
     * <p>Uses pattern matching to recognize IDs and returns the ID type whose pattern could be matched the most often.</p>
     *
     * @param idStream The gene/protein input lists
     * @return The inferred id type
     */
    @Override
    public IdType determineIdType(Stream<String> idStream) {
        Multiset<IdType> possiblyIdTypes = HashMultiset.create();
        Iterator<String> iterator = idStream.iterator();
        Matcher numericMatcher = Pattern.compile("[0-9]+").matcher("");
        Matcher uniProtMnemonicMatcher = Pattern.compile("[A-Z0-9]+_[A-Z0-9]+").matcher("");
        while (iterator.hasNext()) {
            String identifier = iterator.next();
            if (numericMatcher.reset(identifier).matches())
                possiblyIdTypes.add(IdType.GENE);
            else if (uniProtMnemonicMatcher.reset(identifier).matches())
                possiblyIdTypes.add(IdType.UNIPROT_ACCESSION);
            else
                possiblyIdTypes.add(IdType.GENE_NAME);
        }
        IdType maxType = IdType.UNKNOWN;
        long max = -1;
        for (IdType idType : EnumSet.complementOf(EnumSet.of(IdType.UNKNOWN))) {
            long count = possiblyIdTypes.count(idType);
            if (count > max) {
                max = count;
                maxType = idType;
            }
        }
        return maxType;
    }

    @Override
    public Future<Stream<String>> convert2Gepi(Stream<String> idStream) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CompletableFuture<Multimap<String, String>> convertGene2AggregateIds(Stream<String> input) {
        return CompletableFuture.supplyAsync(() -> {
            Driver driver = GraphDatabase.driver(boltUrl, AuthTokens.basic("neo4j", "julielab"));

            try (Session session = driver.session()) {

                return session.readTransaction(tx -> {
                    Record record;
                    Multimap<String, String> topAtids = HashMultimap.create();

                    String[] searchInput = input.toArray(String[]::new);
                    log.debug("Running query to map gene IDs to aggregate IDs.");
                    Result result = tx.run(
                            "MATCH (n:CONCEPT) WHERE n:ID_MAP_NCBI_GENES AND n.originalId IN $originalIds " +
                                    "OPTIONAL MATCH (n)<-[:HAS_ELEMENT]-(a:AGGREGATE_GENEGROUP) " +
                                    "WITH n,a " +
                                    "OPTIONAL MATCH (a)<-[:HAS_ELEMENT]-(top:AGGREGATE_TOP_ORTHOLOGY) " +
                                    "RETURN DISTINCT n.originalId AS SOURCE_ID, COALESCE(top.id,a.id) AS SEARCH_ID",
                            parameters("originalIds", searchInput));

                    while (result.hasNext()) {
                        record = result.next();
                        topAtids.put(record.get("SOURCE_ID").asString(), record.get("SEARCH_ID").asString());
                    }
                    return topAtids;
                });

            }
        });
    }

}
