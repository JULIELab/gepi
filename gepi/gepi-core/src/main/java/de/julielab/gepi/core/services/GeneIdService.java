package de.julielab.gepi.core.services;

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
import org.apache.commons.lang3.StringUtils;
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


    public static final Pattern GENE_ID_PATTERN = Pattern.compile("(gene:)?[0-9]+", Pattern.CASE_INSENSITIVE);
    public static final Pattern UP_MNEMONIC_PATTERN = Pattern.compile("(up:)?[A-Z0-9]+_[A-Z0-9]+", Pattern.CASE_INSENSITIVE);
    // https://www.uniprot.org/help/accession_numbers
    public static final Pattern UP_ACCESSION_PATTERN = Pattern.compile("(up:)?[OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z][0-9]([A-Z][A-Z0-9]{2}[0-9]){1,2}", Pattern.CASE_INSENSITIVE);
    public static final Pattern FPLX_PATTERN = Pattern.compile("fplx:.*", Pattern.CASE_INSENSITIVE);
    public static final Pattern HGNCG_PATTERN = Pattern.compile("hgncg:.*", Pattern.CASE_INSENSITIVE);
    public static final Pattern HGNC_PATTERN = Pattern.compile("hgnc:.*", Pattern.CASE_INSENSITIVE);
    public static final Pattern GO_PATTERN = Pattern.compile("go:.*", Pattern.CASE_INSENSITIVE);
    // https://www.ensembl.org/info/genome/stable_ids/index.html
    public static final Pattern ENSEMBL_PATTERN = Pattern.compile("ens:ENS.+[0-9]+", Pattern.CASE_INSENSITIVE);
    public static final Pattern NAME_PATTERN = Pattern.compile(".+", Pattern.CASE_INSENSITIVE);
    public static final String GENE_LABEL = "ID_MAP_NCBI_GENES";
    public static final String FPLX_LABEL = "FPLX";
    public static final String HGNC_GROUP_LABEL = "HGNC_GROUP";
    public static final String HGNC_LABEL = "HGNC";
    public static final String UP_LABEL = "UNIPROT";
    public static final String ENSEMBL_LABEL = "ENSEMBL";
    public static final String GO_LABEL = "GENE_ONTOLOGY";
    private final Driver driver;
    private final Logger log;
    private final LoadingCache<String, GepiGeneInfo> geneInfoCache = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(30)).maximumSize(10000).build(new CacheLoader<>() {
        @Override
        public GepiGeneInfo load(String s) {
            return getGeneInfoFromDatabase(List.of(s)).get(s);
        }

        @Override
        public Map<String, GepiGeneInfo> loadAll(Iterable<? extends String> keys) {
            return getGeneInfoFromDatabase(keys);
        }
    });

    public GeneIdService(Logger log, @Symbol(GepiCoreSymbolConstants.NEO4J_BOLT_URL) String boltUrl) {
        this.log = log;
        driver = GraphDatabase.driver(boltUrl, AuthTokens.basic("neo4j", "julielab"));
    }

    @Override
    public Future<IdConversionResult> convert(Stream<String> stream, IdType from, @Deprecated IdType to) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> sourceIds = stream.collect(Collectors.toList());
            Future<Multimap<String, String>> convertedIds;
//            if (to == IdType.GEPI_AGGREGATE) {
                if (from == IdType.GENE_NAME) {
                    convertedIds = convertConceptNames2AggregateIds(sourceIds.stream());
                } else if (from == IdType.GEPI_CONCEPT) {
                    convertedIds = convertConcept2AggregateGepiIds(sourceIds.stream(), GENE_LABEL, "originalId");
                } else if (from == IdType.FAMPLEX) {
                    convertedIds = convert2Gepi(sourceIds.stream(), FPLX_LABEL, "originalId");
                } else if (from == IdType.HGNC_GROUP) {
                    convertedIds = convert2Gepi(sourceIds.stream(), HGNC_GROUP_LABEL, "originalId");
                } else if (from == IdType.UNIPROT_ACCESSION) {
                    convertedIds = convertMappedGene2AggregateGepiIds(sourceIds.stream(), UP_LABEL, "originalId");
                } else if (from == IdType.UNIPROT_MNEMONIC) {
                    convertedIds = convertMappedGene2AggregateGepiIds(sourceIds.stream(), UP_LABEL, "sourceIds1");
                } else if (from == IdType.ENSEMBL) {
                    convertedIds = convertMappedGene2AggregateGepiIds(sourceIds.stream(), ENSEMBL_LABEL, "originalId");
                } else if (from == IdType.HGNC) {
                    convertedIds = convertMappedGene2AggregateGepiIds(sourceIds.stream(), HGNC_LABEL, "originalId");
                } else if (from == IdType.GO) {
                    // GO term GePI IDs are directly added to the index (in contrast to UP, ENSEMBL and HGNC IDs which are just mapped to their Gene IDs so we can search for the Gene IDs directly)
                    // for the ability to resolve GO-hypernyms.
                    convertedIds = convert2Gepi(sourceIds.stream(), GO_LABEL, "originalId");
                } else {
                    throw new IllegalArgumentException("From-ID type '" + from + "' is currently not supported");
                }
//            } else if (to == IdType.GEPI_CONCEPT) {
//                if (from == IdType.GENE_NAME) {
//                    convertedIds = convertGeneNames2GeneIds(sourceIds.stream());
//                } else if (from == IdType.GEPI_CONCEPT) {
//                    convertedIds = filterGeneIdsForTaxonomyIds(sourceIds.stream(), taxIds);
//                } else if (from == IdType.FAMPLEX) {
//                    convertedIds = convert2Gepi(sourceIds.stream(), FPLX_LABEL, "originalId");
//                } else if (from == IdType.HGNC_GROUP) {
//                    convertedIds = convert2Gepi(sourceIds.stream(), HGNC_LABEL, "originalId");
//                } else if (from == IdType.UNIPROT_ACCESSION) {
//                    convertedIds = convertMappedGene2AggregateGepiIds(sourceIds.stream(), UP_LABEL, "originalId");
//                } else if (from == IdType.UNIPROT_MNEMONIC) {
//                    convertedIds = convertMappedGene2AggregateGepiIds(sourceIds.stream(), UP_LABEL, "sourceIds1");
//                } else if (from == IdType.ENSEMBL) {
//                    convertedIds = convertMappedGene2AggregateGepiIds(sourceIds.stream(), ENSEMBL_LABEL, "originalId");
//                } else if (from == IdType.HGNC) {
//                    convertedIds = convertMappedGene2AggregateGepiIds(sourceIds.stream(), HGNC_LABEL, "originalId");
//                } else if (from == IdType.GO) {
//                    // GO term GePI IDs are directly added to the index (in contrast to UP, ENSEMBL and HGNC IDs which are just mapped to their Gene IDs so we can search for the Gene IDs directly)
//                    // for the ability to resolve GO-hypernyms.
//                    convertedIds = convert2Gepi(sourceIds.stream(), GO_LABEL, "originalId");
//                } else {
//                    throw new IllegalArgumentException("From-ID type '" + from + "' is currently not supported");
//                }
//            } else {
//                throw new IllegalArgumentException("To-ID type '" + to + "' is currently not supported.");
//            }
            Future<Multimap<String, String>> finalConvertedIds = convertedIds;
            try {
                Multimap<String, String> idMapping = finalConvertedIds.get();
                return new IdConversionResult(sourceIds, idMapping, to);
            } catch (Exception e) {
                log.error("Could not create an IdConversionResult instance", e);
                throw new IllegalStateException(e);
            }
        });
    }

    private Future<Multimap<String, String>> filterGeneIdsForTaxonomyIds(Stream<String> stream, Collection<String> taxIds) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {

                return session.readTransaction(tx -> {
                    long time = System.currentTimeMillis();
                    Record record;
                    Multimap<String, String> filteredGeneIds = HashMultimap.create();

                    String[] searchInput = stream.toArray(String[]::new);
                    String cypher = "MATCH (n:CONCEPT:ID_MAP_NCBI_GENES) WHERE n.originalId IN $geneIds AND n.taxId in $taxIds " +
                            "RETURN n.originalId AS SEARCH_ID";
                    Result result = tx.run(
                            cypher,
                            parameters("geneIds", searchInput, "taxIds", taxIds));

                    while (result.hasNext()) {
                        record = result.next();
                        filteredGeneIds.put(record.get("SEARCH_ID").asString(), record.get("SEARCH_ID").asString());
                    }
                    time = System.currentTimeMillis() - time;
                    log.info("Filtered {} gene IDs to {} gene IDs for tax IDs {} in {} seconds.", searchInput.length, filteredGeneIds.size(), taxIds, time / 1000);
                    return filteredGeneIds;
                });
            }
        });
    }

    @Override
    public Future<IdConversionResult> convert(Stream<String> stream, IdType to) {
        return CompletableFuture.supplyAsync(() -> {
            final Multimap<IdType, String> idsByType = determineIdTypes(stream);
            final List<Future<IdConversionResult>> convertedIds = new ArrayList<>();
            for (IdType from : idsByType.keySet()) {
                final Collection<String> sourceIds = idsByType.get(from);
                convertedIds.add(convert(sourceIds.stream(), from, to));
//            if (to == IdType.GEPI_AGGREGATE) {
//                if (taxIds != null && !taxIds.isEmpty())
//                    throw new IllegalArgumentException("Input IDs should be converted to aggregates but there are also taxonomy IDs specified.");
//                if (from == IdType.GENE_NAME) {
//                    convertedIds.add(convertGeneNames2AggregateIds(sourceIds.stream()));
//                } else if (from == IdType.GENE) {
//                    convertedIds.add(convertGene2AggregateIds(sourceIds.stream()));
//                } else {
//                    throw new IllegalArgumentException("From-ID type '" + from + "' is currently not supported");
//                }
//            } else if (to == IdType.GENE) {
//                if (from == IdType.GENE_NAME) {
//                    if (taxIds != null && !taxIds.isEmpty())
//                        convertedIds.add(convertGeneNames2GeneIds(sourceIds.stream(), taxIds));
//                    else
//                        convertedIds.add(convertGeneNames2GeneIds(sourceIds.stream()));
//                } else if (from == IdType.GENE) {
//                    HashMultimap<String, String> map = HashMultimap.create();
//                    sourceIds.forEach(id -> map.put(id, id));
//                    convertedIds.add(CompletableFuture.completedFuture(map));
//                } else {
//                    throw new IllegalArgumentException("From-ID type '" + from + "' is currently not supported");
//                }
//            } else {
//                throw new IllegalArgumentException("To-ID type '" + to + "' is currently not supported.");
//            }
            }
            try {
                Multimap<String, String> combinedIdMappings = HashMultimap.create();
                for (var idMapping : convertedIds)
                    combinedIdMappings.putAll(idMapping.get().getConvertedItems());
                return new IdConversionResult(idsByType.values(), combinedIdMappings, to);
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
    public CompletableFuture<Multimap<String, String>> convertConceptNames2AggregateIds(Stream<String> geneNames) {
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
                        topAtids.removeAll(normalizedInputName);
                        topAtids.putAll(originalInputName, mappedIds);
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
                possiblyIdTypes.add(IdType.GEPI_CONCEPT);
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
    public Multimap<IdType, String> determineIdTypes(Stream<String> idStream) {
        Matcher geneIdMatcher = GENE_ID_PATTERN.matcher("");
        Matcher uniProtMnemonicMatcher = UP_MNEMONIC_PATTERN.matcher("");
        Matcher uniProtAccessionMatcher = UP_ACCESSION_PATTERN.matcher("");
        Matcher goMatcher = GO_PATTERN.matcher("");
        Matcher ensemblMatcher = ENSEMBL_PATTERN.matcher("");
        Matcher famplexMatcher = FPLX_PATTERN.matcher("");
        Matcher hgncgMatcher = HGNCG_PATTERN.matcher("");
        Matcher hgncMatcher = HGNC_PATTERN.matcher("");
        final Matcher anyMatcher = NAME_PATTERN.matcher("");
        Multimap<IdType, String> idsByType = HashMultimap.create();
        Iterator<String> iterator = idStream.iterator();
        Map<IdType, Matcher> matchers = Map.of(IdType.GEPI_CONCEPT, geneIdMatcher,
                IdType.UNIPROT_MNEMONIC, uniProtMnemonicMatcher,
                IdType.UNIPROT_ACCESSION, uniProtAccessionMatcher,
                IdType.GO, goMatcher,
                IdType.ENSEMBL, ensemblMatcher,
                IdType.FAMPLEX, famplexMatcher,
                IdType.HGNC, hgncMatcher,
                IdType.HGNC_GROUP, hgncgMatcher,
                IdType.GENE_NAME, anyMatcher);
        while (iterator.hasNext()) {
            String identifier = iterator.next();
            for (IdType idType : matchers.keySet()) {
                Matcher m = matchers.get(idType);
                if (m.reset(identifier).matches()) {
                    idsByType.put(idType, geneIdMatcher.groupCount() > 0 ? identifier.substring(m.end(1)) : identifier);
                    break;
                }
            }
        }
        return idsByType;
    }

    @Override
    public Future<Multimap<String, String>> convert2Gepi(Stream<String> input, String label, String idProperty) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {

                return session.readTransaction(tx -> {
                    Record record;
                    Multimap<String, String> topAtids = HashMultimap.create();

                    String[] searchInput = input.toArray(String[]::new);
                    final String query = "MATCH (c:$label) WHERE c.$idProperty IN $originalIds return c.originalId AS SOURCE_ID,c.id AS SEARCH_ID";
                    final Value parameters = parameters("originalIds", searchInput, "label", label, "idProperty", idProperty);
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
    public CompletableFuture<Multimap<String, String>> convertConcept2AggregateGepiIds(Stream<String> input, String label, String idProperty) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {

                return session.readTransaction(tx -> {
                    Record record;
                    Multimap<String, String> topAtids = HashMultimap.create();

                    String[] searchInput = input.toArray(String[]::new);
                    final String query = "MATCH (c:CONCEPT:$label) WHERE c.$idProperty IN $originalIds WITH c OPTIONAL MATCH (a:AGGREGATE)-[:HAS_ELEMENT*]->(c) WHERE NOT ()-[:HAS_ELEMENT]->(a) return c.originalId AS SOURCE_ID,COALESCE(a.id,c.id) AS SEARCH_ID";
                    final Value parameters = parameters("label", label, "originalIds", searchInput, "idProperty", idProperty);
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

    private CompletableFuture<Multimap<String, String>> convertMappedGene2AggregateGepiIds(Stream<String> input, String label, String idProperty) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {

                return session.readTransaction(tx -> {
                    Record record;
                    Multimap<String, String> topAtids = HashMultimap.create();

                    String[] searchInput = input.toArray(String[]::new);
                    final String query = "MATCH (:CONCEPT:$label)-[:IS_MAPPED_TO]->(c) WHERE c.$idProperty IN $ids WITH c OPTIONAL MATCH (a:AGGREGATE)-[:HAS_ELEMENT*]->(c) WHERE NOT ()-[:HAS_ELEMENT]->(a) return c.originalId AS SOURCE_ID,COALESCE(a.id,c.id) AS SEARCH_ID";
                    final Value parameters = parameters("label", label, "ids", searchInput, "idProperty", idProperty);
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

                // Get the information about the genes.
                // Slight special handling for FamPlex-HGNCGroup aggregates: we also retrieve the originalId_divergentProperty.
                // The FamPlex-HGNCGroup aggregates have the originalId as copy_property. Thus, they obtain both original
                // IDs from the FamPlex and HGNC Group element. However, we do not know which one ends up in the originalId
                // property and which one in the divergent property. This depends on traversing order and is not determined
                // beforehand. Get both properties so we can decide which one to use. FamPlex IDs are a readable name,
                // HGNC Group IDs are numbers.
                final String query = "MATCH (c:CONCEPT) WHERE c.id IN $conceptIds RETURN c.originalId,c.originalId_divergentProperty,c.id,c.preferredName,c.synonyms,c.descriptions,labels(c)";
                final Value parameters = parameters("conceptIds", conceptIds);
                Result result = tx.run(
                        query,
                        parameters);

                while (result.hasNext()) {
                    Record record = result.next();
                    String conceptId = record.get("c.id").asString();
                    String originalId = record.get("c.originalId").asString();
                    Value divergentOriginalId = record.get("c.originalId_divergentProperty");
                    String preferredName = record.get("c.preferredName").asString();
                    List<String> labels = record.get("labels(c)").asList(Value::asString);
                    // For FamPlex-HGNCGroup aggregates, get the HGNC Group ID. For HGNC, we can give direct links
                    // to the source.
                    if (labels.contains("AGGREGATE_FPLX_HGNC") && !StringUtils.isNumeric(originalId))
                        originalId = divergentOriginalId.asList(Value::asString).stream().filter(StringUtils::isNumeric).limit(1).findAny().get();
                    innerGeneInfo.put(conceptId, GepiGeneInfo.builder().originalId(originalId).conceptId(conceptId).symbol(preferredName).labels(new HashSet<>(labels)).build());
                }
                return innerGeneInfo;
            });
        }
        return geneInfo;
    }

}
