package de.julielab.gepi.core.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import de.julielab.gepi.core.GepiCoreSymbolConstants;
import de.julielab.gepi.core.retrieval.data.GeneSymbolNormalization;
import de.julielab.gepi.core.retrieval.data.GepiGeneInfo;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.neo4j.driver.Values.parameters;

public class GeneIdService implements IGeneIdService {


    private final Driver driver;
    private Logger log;
    private LoadingCache<String, GepiGeneInfo> geneInfoCache = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(30)).maximumSize(10000).build(new CacheLoader<>() {
        @Override
        public GepiGeneInfo load(String s) {
            return getGeneInfoFromDatabase(List.of(s)).get(s);
        }

        @Override
        public Map<String, GepiGeneInfo> loadAll(Iterable<? extends String> keys) throws Exception {
            return getGeneInfoFromDatabase(keys);
        }
    });

    public GeneIdService(Logger log, @Symbol(GepiCoreSymbolConstants.NEO4J_BOLT_URL) String boltUrl) {
        this.log = log;
        driver = GraphDatabase.driver(boltUrl, AuthTokens.basic("neo4j", "julielab"));
    }

    @Override
    public Future<IdConversionResult> convert(Stream<String> stream, IdType from, IdType to, Collection<String> taxIds) {
        List<String> sourceIds = stream.collect(Collectors.toList());
        CompletableFuture<Multimap<String, String>> convertedIds;
        if (to == IdType.GEPI_AGGREGATE) {
            if (taxIds != null && !taxIds.isEmpty())
                throw new IllegalArgumentException("Input IDs should be converted to aggregates but there are also taxonomy IDs specified.");
            if (from == IdType.GENE_NAME) {
                convertedIds = convertGeneNames2AggregateIds(sourceIds.stream());
            } else if (from == IdType.GENE) {
                convertedIds = convertGene2AggregateIds(sourceIds.stream());
            } else {
                throw new IllegalArgumentException("From-ID type '" + from + "' is currently not supported");
            }
        } else if (to == IdType.GENE) {
            if (from == IdType.GENE_NAME) {
                if (taxIds != null && !taxIds.isEmpty())
                    convertedIds = convertGeneNames2GeneIds(sourceIds.stream(), taxIds);
                else
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
                IdConversionResult idConversionResult = new IdConversionResult(sourceIds, idMapping, from, to);
                return idConversionResult;
            } catch (Exception e) {
                log.error("Could not create an IdConversionResult instance", e);
                throw new IllegalStateException(e);
            }
        });
    }

    private CompletableFuture<Multimap<String, String>> convertGeneNames2GeneIds(Stream<String> geneNames, Collection<String> taxIds) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {

                return session.readTransaction(tx -> {
                    long time = System.currentTimeMillis();
                    Record record;
                    Multimap<String, String> topAtids = HashMultimap.create();

                    String[] searchInput = geneNames.map(String::toLowerCase).toArray(String[]::new);
                    log.debug("Running query to map gene names to NCBI gene IDs.");
                    String cypher = "MATCH (n:CONCEPT) WHERE n:ID_MAP_NCBI_GENES AND n.preferredName_lc IN $geneNames AND n.taxId in $taxIds " +
                            "RETURN DISTINCT n.preferredName_lc AS SOURCE_ID,n.originalId AS SEARCH_ID";
                    Result result = tx.run(
                            cypher,
                            parameters("geneNames", searchInput, "taxIds", taxIds));

                    while (result.hasNext()) {
                        record = result.next();
                        topAtids.put(record.get("SOURCE_ID").asString(), record.get("SEARCH_ID").asString());
                    }
                    time = System.currentTimeMillis() - time;
                    log.info("Converted {} gene names to {} gene IDs for tax IDs {} in {} seconds.", searchInput.length, topAtids.size(), taxIds, time / 1000);
                    return topAtids;
                });
            }
        });
    }

    private CompletableFuture<Multimap<String, String>> convertGeneNames2GeneIds(Stream<String> geneNames) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {

                return session.readTransaction(tx -> {
                    long time = System.currentTimeMillis();
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
                    time = System.currentTimeMillis() - time;
                    log.info("Converted {} gene names to {} gene IDs in {} seconds.", searchInput.length, topAtids.size(), time / 1000);
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
            try (Session session = driver.session()) {
                return session.readTransaction(tx -> {
                    long time = System.currentTimeMillis();
                    Record record;
                    Multimap<String, String> topAtids = HashMultimap.create();

                    Set<String> geneNameSet = geneNames.collect(Collectors.toSet());
                    String[] searchInput = geneNameSet.stream().map(GeneSymbolNormalization::normalize).toArray(String[]::new);
                    // get the highest element in the aggregation-hierarchy; the roots are those that are not elements of another aggregate
                    String cypher = "MATCH (c:CONCEPT) WHERE c.preferredName_lc IN $geneNames AND NOT ()-[:HAS_ELEMENT]->(c) RETURN c.preferredName_lc AS SOURCE_ID, c.id AS SEARCH_ID";
                    Result result = tx.run(
                            cypher,
                            parameters("geneNames", searchInput));

                    while (result.hasNext()) {
                        record = result.next();
                        topAtids.put(record.get("SOURCE_ID").asString(), record.get("SEARCH_ID").asString());
                    }
                    time = System.currentTimeMillis() - time;
                    log.info("Converted {} gene names to {} aggregate gene IDs in {} seconds", searchInput.length, topAtids.size(), time / 1000);
                    // Replace the normalized keys with the ones that the user actually specified. This is less confusing
                    // when the ID mapping is shown on the web site.
                    for (String originalInputName : geneNameSet) {
                        final String normalizedInputName = GeneSymbolNormalization.normalize(originalInputName);
                        final Collection<String> mappedIds = new ArrayList<>(topAtids.get(normalizedInputName));
                        if (mappedIds != null) {
                            topAtids.removeAll(normalizedInputName);
                            topAtids.putAll(originalInputName, mappedIds);
                        }
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
            try (Session session = driver.session()) {

                return session.readTransaction(tx -> {
                    Record record;
                    Multimap<String, String> topAtids = HashMultimap.create();

                    String[] searchInput = input.toArray(String[]::new);
//                    final String query = "MATCH (n:CONCEPT) WHERE n:ID_MAP_NCBI_GENES AND n.originalId IN $originalIds " +
//                            "OPTIONAL MATCH (n)<-[:HAS_ELEMENT]-(a:AGGREGATE_GENEGROUP) " +
//                            "WITH n,a " +
//                            "OPTIONAL MATCH (a)<-[:HAS_ELEMENT]-(top:AGGREGATE_TOP_ORTHOLOGY) " +
//                            "RETURN DISTINCT n.originalId AS SOURCE_ID, COALESCE(top.id,a.id) AS SEARCH_ID";
                    final String query = "MATCH (c:CONCEPT:ID_MAP_NCBI_GENES) WHERE c.originalId IN $originalIds WITH c OPTIONAL MATCH (a:AGGREGATE)-[:HAS_ELEMENT*]->(c) WHERE NOT ()-[:HAS_ELEMENT]->(a) return c.originalId AS SOURCE_ID,COALESCE(a.id,c.id) AS SEARCH_ID";
                    final Value parameters = parameters("originalIds", searchInput);
                    Result result = tx.run(
                            query,
                            parameters);

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
    public Map<String, GepiGeneInfo> getGeneInfo(Iterable<String> conceptIds) {
        try {
            return geneInfoCache.getAll(conceptIds);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, GepiGeneInfo> getGeneInfoFromDatabase(Iterable<? extends String> conceptIds) {
        Map<String, GepiGeneInfo> geneInfo;
        try (Session session = driver.session()) {
            geneInfo = session.readTransaction(tx -> {
                Map<String, GepiGeneInfo> innerGeneInfo = new HashMap<>();

                final String query = "MATCH (c:CONCEPT) WHERE c.id IN $conceptIds RETURN c.originalId,c.id,c.preferredName,c.synonyms,c.descriptions,labels(c)";
                final Value parameters = parameters("conceptIds", conceptIds);
                Result result = tx.run(
                        query,
                        parameters);

                while (result.hasNext()) {
                    Record record = result.next();
                    String conceptId = record.get("c.id").asString();
                    String originalId = record.get("c.originalId").asString();
                    String preferredName = record.get("c.preferredName").asString();
                    List<String> labels = record.get("labels(c)").asList(v -> v.asString());
                    innerGeneInfo.put(conceptId, GepiGeneInfo.builder().originalId(originalId).conceptId(conceptId).symbol(preferredName).labels(new HashSet<>(labels)).build());
                }
                return innerGeneInfo;
            });
        }
        return geneInfo;
    }

}
