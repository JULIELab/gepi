package de.julielab.gepi.core.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import de.julielab.gepi.core.GepiCoreSymbolConstants;
import de.julielab.gepi.core.retrieval.data.ConceptName;
import de.julielab.gepi.core.retrieval.data.GeneSymbolNormalization;
import de.julielab.gepi.core.retrieval.data.GepiConceptInfo;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.neo4j.driver.*;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.neo4j.driver.Values.parameters;

public class GeneIdService implements IGeneIdService {
    public static final Pattern CONCEPT_ID_PATTERN = Pattern.compile("a?tid[0-9]+");

    public static final Pattern GENE_ID_PATTERN = Pattern.compile("(gene:)?[0-9]+", Pattern.CASE_INSENSITIVE);
    public static final Pattern UP_MNEMONIC_PATTERN = Pattern.compile("(up:|UP:)?[A-Z0-9]+_[A-Z0-9]+");
    // https://www.uniprot.org/help/accession_numbers
    public static final Pattern UP_ACCESSION_PATTERN = Pattern.compile("(up:|UP:)?[OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z][0-9]([A-Z][A-Z0-9]{2}[0-9]){1,2}");
    public static final Pattern FPLX_PATTERN = Pattern.compile("(fplx:|FPLX:).*");
    public static final Pattern HGNCG_PATTERN = Pattern.compile("(hgncg:|HGNCG:)([0-9]*)");
    public static final Pattern HGNC_PATTERN = Pattern.compile("(hgnc:|HGNC:)?HGNC:.*");
    public static final Pattern GO_PATTERN = Pattern.compile("(go:|GO:)?GO:.*");
    // https://www.ensembl.org/info/genome/stable_ids/index.html
    public static final Pattern ENSEMBL_PATTERN = Pattern.compile("(ens:|ENS:)?ENS.+[0-9]+");
    public static final Pattern NAME_PATTERN = Pattern.compile(".+");
    public static final String GENE_LABEL = "ID_MAP_NCBI_GENES";
    public static final String FPLX_LABEL = "FPLX";
    public static final String HGNC_GROUP_LABEL = "HGNC_GROUP";
    public static final String HGNC_LABEL = "HGNC";
    public static final String UP_LABEL = "UNIPROT";
    public static final String ENSEMBL_LABEL = "ENSEMBL";
    public static final String GO_LABEL = "GENE_ONTOLOGY";
    public static final String CONCEPT_LABEL = "CONCEPT";
    public static final String PROP_ORGID = "originalId";
    public static final String PROP_PREFNAME = "preferredName";
    public static final String PROP_ID = "id";
    // we also have a special property for the UniProtKB-ID (the mnemonic) but sourceIds are indexed anyway,
    // and it is predictable that the ID is assigned second to the accession. The import algorithm first
    // imports the original ID.
    public static final String PROP_UP_ID = "sourceIds1";
    private final Logger log;
    private Driver driver;
    private final LoadingCache<String, ConceptName> aggregationValueConceptNamesCache = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofHours(2)).maximumSize(100000).build(new CacheLoader<>() {
        @Override
        public ConceptName load(String s) {
            return getAggregationRelevantConceptNamesFromDatabase(List.of(s)).get(s);
        }

        @Override
        public Map<String, ConceptName> loadAll(Iterable<? extends String> keys) {
            return getAggregationRelevantConceptNamesFromDatabase(keys);
        }
    });
    private final LoadingCache<String, GepiConceptInfo> geneInfoCache = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofHours(2)).maximumSize(10000).build(new CacheLoader<>() {
        @Override
        public GepiConceptInfo load(String s) {
            return getGeneInfoFromDatabase(List.of(s)).get(s);
        }

        @Override
        public Map<String, GepiConceptInfo> loadAll(Iterable<? extends String> keys) {
            return getGeneInfoFromDatabase(keys);
        }
    });

    public GeneIdService(Logger log, @Symbol(GepiCoreSymbolConstants.NEO4J_BOLT_URL) String boltUrl) {
        this.log = log;
        if (boltUrl != null)
            driver = GraphDatabase.driver(boltUrl, AuthTokens.basic("neo4j", "julielab"));
    }

    /**
     * <p>
     * Fetches the complete paths from the input IDs to gene orthologs and stores the names of all concepts on that paths
     * that could be used in the aggregationvalue field.
     * </p>
     * <p>
     *     No effort is made to omit values that already exist in the cache. It is thus possible to first query for
     *     AMPK_ALPHA, retrieve and store all respective paths and later query for AMPK which will fetch all those
     *     paths again. This shouldn't be much of an issue since the hierarchies are not very deep and Neo4j does
     *     internal caching which should make the mentioned case still quite performant because the AMPK_ALPHA concepts
     *     have already been loaded in Neo4j.
     * </p>
     * @param conceptIds Concept IDs used in an event query.
     * @return IDs, names and next-on-path-IDs of the input concepts and those on the path to the gene ortholog aggregates, omitting the genes themselves because their names are never used in the aggregationvalue field.
     */
    private Map<String, ConceptName> getAggregationRelevantConceptNamesFromDatabase(Iterable<? extends String> conceptIds) {
        log.debug("Collecting possible concept names in aggregationvalues");
        long time = System.currentTimeMillis();
        Map<String, ConceptName> ret;
        try (Session session = driver.session()) {
            ret = session.readTransaction(tx -> {
                Map<String, ConceptName> symbolSet = new HashMap<>();
                // The double WITH usage in CALL circumvents a restriction in Cypher.
                // Normally, after a WITH in CALL, only very simple queries can be used,
                // WHERE is forbidden then, for example.
                // We use CALL to create a kind of CASE but with more complicated query
                // logic.
                // There are three cases:
                // 1. The input ID belongs to a FamPlex or HGNC Group concept
                // 2. The input ID belongs to a gene ontology concept
                // 3. The input ID belongs to a gene that is not part of an orthology cluster or the ID does belong to an orthology cluster or an equal-name aggregate. It cannot be a gene that is also part of an orthology or equal-name aggregate because we resolve the inputs to aggregates whenever possible.
                // In all cases we want all those concept IDs and their preferredNames that have the input ID as an AddOn term in indexing.
                // Because all those concepts will add the input ID and, thus, their event can also be queried by the input ID.
                // Their names can consequently appear in the aggregationvalue field at the place that was matched to the input ID.
                // All those names are needed for argument reordering because all those names could appear when the input ID is found.
                final String query = "MATCH (f:CONCEPT) WHERE f.id IN $conceptIds \n" +
                        "CALL {\n" +
                        "    WITH f\n" +
                        "    WITH f\n" +
                        "    WHERE f:FPLX OR f:HGNC_GROUP\n" +
                        "    MATCH p=(f)-[:IS_BROADER_THAN*..10]->(c) WHERE c:ID_MAP_NCBI_GENES\n" +
                        "    WITH c,[n in nodes(p) WHERE n:FPLX OR n:HGNC_GROUP | n.id] as ids,[n in nodes(p) WHERE n:FPLX OR n:HGNC_GROUP | n.preferredName] as familySymbols " +
                        "    OPTIONAL MATCH (c)<-[:HAS_ELEMENT*..2]-(a) WHERE (a:AGGREGATE_GENEGROUP OR a:AGGREGATE_TOP_ORTHOLOGY) AND NOT exists(()-[:HAS_ELEMENT]->(a)) \n" +
                        "    RETURN ids+[COALESCE(a.id,c.id)] as ids,familySymbols + [COALESCE(a.preferredName,c.preferredName)] as symbols\n" +
                        "\n" +
                        "    UNION\n" +
                        "\n" +
                        "    WITH f\n" +
                        "    WITH f\n" +
                        "    WHERE f:GENE_ONTOLOGY\n" +
                        "    MATCH p=(f)<-[:IS_ANNOTATED_WITH]->(c) WHERE c:ID_MAP_NCBI_GENES\n" +
                        "    WITH f, c " +
                        "    OPTIONAL MATCH (c)<-[:HAS_ELEMENT*..2]-(a) WHERE (a:AGGREGATE_GENEGROUP OR a:AGGREGATE_TOP_ORTHOLOGY) AND NOT exists(()-[:HAS_ELEMENT]->(a)) \n" +
                        "    RETURN [f.id, COALESCE(a.id, c.id)] as ids,[null, COALESCE(a.preferredName,c.preferredName)] as symbols\n" +
                        "\n" +
                        "    UNION\n" +
                        "\n" +
                        "    WITH f\n" +
                        "    WITH f\n" +
                        "    WHERE NOT (f:FPLX OR f:HGNC_GROUP OR f:GENE_ONTOLOGY)\n" +
                        "    OPTIONAL MATCH p=(f)<-[:HAS_ELEMENT*..2]-(a) WHERE (a:AGGREGATE_GENEGROUP OR a:AGGREGATE_TOP_ORTHOLOGY) AND NOT exists(()-[:HAS_ELEMENT]->(a)) \n" +
                        "    WITH f,[n in nodes(p) | n.id] as ids,[n in nodes(p) | n.preferredName] as aggregateSymbols " +
                        "    RETURN COALESCE(ids, [f.id]) as ids, COALESCE(aggregateSymbols, [f.preferredName]) as symbols\n" +
                        "}\n" +
                        "return ids,symbols";
                final Value parameters = parameters("conceptIds", conceptIds);
                Result result = tx.run(query, parameters);

                while (result.hasNext()) {
                    Record record = result.next();
                    final List<String> ids = record.get("ids").asList(Value::asString);
                    final List<String> symbols = record.get("symbols").asList(Value::asString).stream().map(v -> v.equals("null") ? null : v).collect(Collectors.toList());
                    if (ids.size() != symbols.size())
                        throw new IllegalStateException("A concept path was returned with different numbers of IDs and names.");
                    for (int i = 0; i < symbols.size(); i++) {
                        String symbol = symbols.get(i);
                        final String id = ids.get(i);
                        final ConceptName conceptName = symbolSet.compute(id, (k, v) -> v != null ? v : new ConceptName(symbol, new ArrayList<>()));
                        if (i + 1 < symbols.size()) {
                            conceptName.getNextToOrthologyClusters().add(ids.get(i + 1));
                        }
                        assert new HashSet<>(conceptName.getNextToOrthologyClusters()).size() == conceptName.getNextToOrthologyClusters().size() : "There are duplicated concept IDs in " + conceptName;
                    }
                }
                return symbolSet;
            });
        }
        time = System.currentTimeMillis() - time;
        log.debug("Retrieval of possible aggregation names took {}s", time/1000);
        return ret;
    }

    @Override
    public Map<String, ConceptName> getAggregationRelevantConceptNames(Iterable<String> conceptIds) {
        try {
             return aggregationValueConceptNamesCache.getAll(conceptIds);
        } catch (ExecutionException e) {
            log.error("Could not obtain concept names from the database.", e);
        }
        return Collections.emptyMap();
    }

    @Override
    public Set<String> getPossibleAggregationConceptNames(Iterable<String> conceptIds) {
        try {
            // This loads all ConceptNames - which also include pointers to the following concepts on the path to orthology aggregates - that could appear in the aggregationvaluefield for the input conceptIds. The cache automatically loads those that have not already been fetched from the database.
            aggregationValueConceptNamesCache.getAll(conceptIds);
            final Map<String, ConceptName> cacheMap = aggregationValueConceptNamesCache.asMap();
            Set<String> possibleNamesOnPathToOrthologyAggregates = new HashSet<>();
            getPossibleAggregationConceptNamesFromCacheMap(conceptIds, cacheMap, possibleNamesOnPathToOrthologyAggregates);
            return possibleNamesOnPathToOrthologyAggregates;
        } catch (ExecutionException e) {
            log.error("Could not obtain concept named from the database.", e);
        }
        return Collections.emptySet();
    }

    /**
     * <p>Recursively fetches the aggregation-relevant names from the concept on the path from the input conceptIds to the gene orthology aggregate nodes.</p>
     * @param conceptIds Start nodes on the path to orthology aggregates to collect the names for.
     * @param cacheMap The name cache as a map.
     * @param namesAccumulator A set where all the relevant names are assembled into.
     */
    private void getPossibleAggregationConceptNamesFromCacheMap(Iterable<String> conceptIds, Map<String, ConceptName> cacheMap, Set<String> namesAccumulator) {
        for (String conceptId : conceptIds) {
            final ConceptName conceptName = aggregationValueConceptNamesCache.getIfPresent(conceptId);
            if (conceptName.getName() != null)
                namesAccumulator.add(conceptName.getName());
            for (String nextId : conceptName.getNextToOrthologyClusters()) {
                getPossibleAggregationConceptNamesFromCacheMap(Set.of(nextId), cacheMap, namesAccumulator);
            }
        }
    }

    @Override
    public Future<IdConversionResult> convert(Stream<String> stream, IdType from, @Deprecated IdType to) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> sourceIds = stream.collect(Collectors.toList());
            Future<Multimap<String, String>> convertedIds;
            if (from == IdType.GENE_NAME) {
                convertedIds = convertConceptNames2AggregateIds(sourceIds.stream());
            } else if (from == IdType.GENE_ID) {
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
            Future<Multimap<String, String>> finalConvertedIds = convertedIds;
            try {
                Multimap<String, String> idMapping = finalConvertedIds.get();
                Map<String, IdType> input2IdType = sourceIds.stream().collect(Collectors.toMap(Function.identity(), x -> from));
                return new IdConversionResult(sourceIds, idMapping, input2IdType, to);
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
                    log.debug("Filtered {} gene IDs to {} gene IDs for tax IDs {} in {} seconds.", searchInput.length, filteredGeneIds.size(), taxIds, time / 1000);
                    return filteredGeneIds;
                });
            }
        });
    }

    @Override
    public Future<IdConversionResult> convert(Stream<String> stream, IdType to) {
        return CompletableFuture.supplyAsync(() -> {
            long time = System.currentTimeMillis();
            final Multimap<IdType, String> idsByType = determineIdTypes(stream);
            final List<Future<IdConversionResult>> convertedIds = new ArrayList<>();
            for (IdType from : idsByType.keySet()) {
                final Collection<String> sourceIds = idsByType.get(from);
                convertedIds.add(convert(sourceIds.stream(), from, to));
            }
            try {
                Multimap<String, String> combinedIdMappings = HashMultimap.create();
                Map<String, IdType> typeById = new HashMap<>();
                for (var idMapping : convertedIds) {
                    combinedIdMappings.putAll(idMapping.get().getConvertedItems());
                    typeById.putAll(idMapping.get().getInputIdTypeMapping());
                }
                time = System.currentTimeMillis() - time;
                log.info("Converted {} input IDs of possibly different types to {} IDs of type {} in {} seconds.", idsByType.values().size(), combinedIdMappings.size(), to, time / 1000);
                return new IdConversionResult(idsByType.values(), combinedIdMappings, typeById, to);
            } catch (Exception e) {
                log.error("Could not create an IdConversionResult instance", e);
                throw new IllegalStateException(e);
            }
        });
    }

    public Map<String, String> mapIdentifierToPrefix(Stream<String> idStream) {
        Matcher geneIdMatcher = GENE_ID_PATTERN.matcher("");
        Matcher uniProtMnemonicMatcher = UP_MNEMONIC_PATTERN.matcher("");
        Matcher uniProtAccessionMatcher = UP_ACCESSION_PATTERN.matcher("");
        Matcher goMatcher = GO_PATTERN.matcher("");
        Matcher ensemblMatcher = ENSEMBL_PATTERN.matcher("");
        Matcher famplexMatcher = FPLX_PATTERN.matcher("");
        Matcher hgncgMatcher = HGNCG_PATTERN.matcher("");
        Matcher hgncMatcher = HGNC_PATTERN.matcher("");
        final Matcher anyMatcher = NAME_PATTERN.matcher("");
        Map<String, String> prefixByIds = new HashMap<>();
        Iterator<String> iterator = idStream.iterator();
        List<Matcher> matchers = List.of(geneIdMatcher,
                uniProtMnemonicMatcher,
                uniProtAccessionMatcher,
                goMatcher,
                ensemblMatcher,
                famplexMatcher,
                hgncMatcher,
                hgncgMatcher,
                anyMatcher);
        while (iterator.hasNext()) {
            String identifier = iterator.next();
            for (Matcher m : matchers) {
                if (m.reset(identifier).matches()) {
                    if (m.groupCount() > 0 && m.group(1) != null)
                        prefixByIds.put(identifier.substring(m.end(1)), m.group(1));
                    else
                        prefixByIds.put(identifier, null);
                    break;
                }
            }
        }
        return prefixByIds;
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
                    // Get the highest element in the aggregation-hierarchy; the roots are those that are not elements of another aggregate.
                    // Note that this excludes equal name aggregates because those are not CONCEPTs themselves
                    String cypher = "MATCH (c:CONCEPT) WHERE c.preferredName_normalized IN $geneNames AND NOT exists((:AGGREGATE_TOP_ORTHOLOGY)-[:HAS_ELEMENT]->(c)) AND NOT exists((:AGGREGATE_GENEGROUP)-[:HAS_ELEMENT]->(c)) RETURN c.preferredName_normalized AS SOURCE_ID, c.id AS SEARCH_ID";
                    Result result = tx.run(
                            cypher,
                            parameters("geneNames", searchInput));

                    while (result.hasNext()) {
                        record = result.next();
                        final String sourceId = record.get("SOURCE_ID").asString();
                        final String searchId = record.get("SEARCH_ID").asString();
                        topAtids.put(sourceId, searchId);
                    }
                    time = System.currentTimeMillis() - time;
                    log.debug("Converted {} concept names to {} aggregate gene IDs in {} seconds", searchInput.length, topAtids.size(), time / 1000);
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
                possiblyIdTypes.add(IdType.GENE_ID);
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
        Map<IdType, Matcher> matchers = new LinkedHashMap<>();
        matchers.put(IdType.GENE_ID, geneIdMatcher);
        matchers.put(IdType.UNIPROT_MNEMONIC, uniProtMnemonicMatcher);
        matchers.put(IdType.UNIPROT_ACCESSION, uniProtAccessionMatcher);
        matchers.put(IdType.GO, goMatcher);
        matchers.put(IdType.ENSEMBL, ensemblMatcher);
        matchers.put(IdType.FAMPLEX, famplexMatcher);
        matchers.put(IdType.HGNC, hgncMatcher);
        matchers.put(IdType.HGNC_GROUP, hgncgMatcher);
        matchers.put(IdType.GENE_NAME, anyMatcher);
        while (iterator.hasNext()) {
            String identifier = iterator.next();
            for (IdType idType : matchers.keySet()) {
                Matcher m = matchers.get(idType);
                if (m.reset(identifier).matches()) {
                    idsByType.put(idType, identifier);
                    break;
                }
            }
        }
        return idsByType;
    }

    @Override
    public Future<Multimap<String, String>> convert2Gepi(Stream<String> input, String label, String idProperty) {
        final Map<String, String> id2prefix = mapIdentifierToPrefix(input);
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {
                return session.readTransaction(tx -> {
                    long time = System.currentTimeMillis();
                    Record record;
                    Multimap<String, String> topAtids = HashMultimap.create();

                    String[] searchInput = id2prefix.keySet().toArray(new String[0]);
                    final String query = "MATCH (c:" + label + ") WHERE c." + idProperty + " IN $ids return c." + idProperty + " AS SOURCE_ID,c.id AS SEARCH_ID";
                    final Value parameters = parameters("ids", searchInput);
                    Result result = tx.run(
                            query,
                            parameters);

                    while (result.hasNext()) {
                        record = result.next();
                        final String sourceId = record.get("SOURCE_ID").asString();
                        String prefix = id2prefix.get(sourceId) != null ? id2prefix.get(sourceId) : "";
                        topAtids.put(prefix + sourceId, record.get("SEARCH_ID").asString());
                    }
                    time = System.currentTimeMillis() - time;
                    log.debug("Converted {} input IDs to {} concept IDs in {} seconds.", searchInput.length, topAtids.size(), time / 1000);
                    return topAtids;
                });

            }
        });
    }

    @Override
    public CompletableFuture<Multimap<String, String>> convertConcept2AggregateGepiIds(Stream<String> input, String label, String idProperty) {
        final Map<String, String> id2prefix = mapIdentifierToPrefix(input);
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {

                return session.readTransaction(tx -> {
                    long time = System.currentTimeMillis();
                    Record record;
                    Multimap<String, String> topAtids = HashMultimap.create();

                    String[] searchInput = id2prefix.keySet().toArray(String[]::new);
                    final String query = "MATCH (c:CONCEPT:" + label + ") WHERE c." + idProperty + " IN $ids WITH c OPTIONAL MATCH (a:AGGREGATE)-[:HAS_ELEMENT*]->(c) WHERE NOT a:AGGREGATE_EQUAL_NAMES AND NOT ()-[:HAS_ELEMENT]->(a) return c." + idProperty + " AS SOURCE_ID,COALESCE(a.id,c.id) AS SEARCH_ID";
                    final Value parameters = parameters("ids", searchInput);
                    Result result = tx.run(
                            query,
                            parameters);

                    while (result.hasNext()) {
                        record = result.next();
                        final String sourceId = record.get("SOURCE_ID").asString();
                        final String searchId = record.get("SEARCH_ID").asString();
                        String prefix = id2prefix.get(sourceId) != null ? id2prefix.get(sourceId) : "";
                        topAtids.put(prefix + sourceId, searchId);
                    }
                    time = System.currentTimeMillis() - time;
                    log.debug("Converted {} input IDs to {} aggregate IDs in {} seconds.", searchInput.length, topAtids.size(), time / 1000);
                    return topAtids;
                });

            }
        });
    }

    public CompletableFuture<Multimap<String, String>> convertMappedGene2AggregateGepiIds(Stream<String> input, String label, String idProperty) {
        final Map<String, String> id2prefix = mapIdentifierToPrefix(input);
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = driver.session()) {

                return session.readTransaction(tx -> {
                    long time = System.currentTimeMillis();
                    Record record;
                    Multimap<String, String> topAtids = HashMultimap.create();

                    String[] searchInput = id2prefix.keySet().toArray(String[]::new);
                    final String query = "MATCH (n:CONCEPT:" + label + ")-[:IS_MAPPED_TO]->(c) WHERE n." + idProperty + " IN $ids WITH n,c OPTIONAL MATCH (a:AGGREGATE)-[:HAS_ELEMENT*]->(c) WHERE NOT a:AGGREGATE_EQUAL_NAMES AND NOT ()-[:HAS_ELEMENT]->(a) return n." + idProperty + " AS SOURCE_ID,COALESCE(a.id,c.id) AS SEARCH_ID";
                    final Value parameters = parameters("ids", searchInput);
                    Result result = tx.run(
                            query,
                            parameters);

                    while (result.hasNext()) {
                        record = result.next();
                        final String sourceId = record.get("SOURCE_ID").asString();
                        String prefix = id2prefix.get(sourceId) != null ? id2prefix.get(sourceId) : "";
                        topAtids.put(prefix + sourceId, record.get("SEARCH_ID").asString());
                    }
                    time = System.currentTimeMillis() - time;
                    log.debug("Converted {} input IDs mapped to genes to {} aggregate IDs in {} seconds.", searchInput.length, topAtids.size(), time / 1000);
                    return topAtids;
                });

            }
        });
    }

    @Override
    public Map<String, GepiConceptInfo> getGeneInfo(Iterable<String> conceptIds) {
        try {
            return geneInfoCache.getAll(conceptIds);
        } catch (Exception e) {
            throw new IllegalStateException("Error while trying to retrieve geneInfo for conceptIds " + String.join(", ", conceptIds), e);
        }
    }

    private Map<String, GepiConceptInfo> getGeneInfoFromDatabase(Iterable<? extends String> conceptIds) {
        Map<String, GepiConceptInfo> geneInfo;
        try (Session session = driver.session()) {
            geneInfo = session.readTransaction(tx -> {
                Map<String, GepiConceptInfo> innerGeneInfo = new HashMap<>();

                // Get the information about the genes.
                // Slight special handling for FamPlex-HGNCGroup aggregates: we also retrieve the originalId_divergentProperty.
                // The FamPlex-HGNCGroup aggregates have the originalId as copy_property. Thus, they obtain both original
                // IDs from the FamPlex and HGNC Group element. However, we do not know which one ends up in the originalId
                // property and which one in the divergent property. This depends on traversing order and is not determined
                // beforehand. Get both properties so we can decide which one to use. FamPlex IDs are a readable name,
                // HGNC Group IDs are numbers.
                final String query = "MATCH (c:CONCEPT) WHERE c.id IN $conceptIds RETURN c.id,c.originalId,c.preferredName,labels(c)";
                final Value parameters = parameters("conceptIds", conceptIds);
                Result result = tx.run(
                        query,
                        parameters);

                while (result.hasNext()) {
                    Record record = result.next();
                    String conceptId = record.get("c.id").asString();
                    String originalId = record.get("c.originalId").asString();
                    String preferredName = record.get("c.preferredName").asString();
                    List<String> labels = record.get("labels(c)").asList(Value::asString);
                    // For FamPlex-HGNCGroup aggregates, get the HGNC Group ID. For HGNC, we can give direct links
                    // to the source.
//                    if (labels.contains("AGGREGATE_FPLX_HGNC") && !StringUtils.isNumeric(originalId))
//                        originalId = divergentOriginalId.asList(Value::asString).stream().filter(StringUtils::isNumeric).limit(1).findAny().get();
                    innerGeneInfo.put(conceptId, GepiConceptInfo.builder()
                            .originalId(originalId)
                            .conceptId(conceptId).symbol(preferredName).labels(new HashSet<>(labels)).build());
                }
                return innerGeneInfo;
            });
        }
        return geneInfo;
    }

    /**
     * <p>Starting at FPLX or HGNC_GROUP concept nodes, retrieve the names of the sub-family concept and the top aggregate symbols of their gene elements.</p>
     * <p>This is used to find all possibilities for an A-List concept to create an event argument match since for families their sub-concepts are also found due to hyponym resolution.</p>
     *
     * @param conceptIds
     * @param propertyName
     * @return
     */
    @Override
    public Set<String> getGeneAggregateSymbolsForFamilyConcepts(Iterable<? extends String> conceptIds, String propertyName) {
        Set<String> ret;
        try (Session session = driver.session()) {
            ret = session.readTransaction(tx -> {
                Set<String> symbolSet = new HashSet<>();
                final String query = "MATCH p=(f)-[:IS_BROADER_THAN*]->(c) WHERE (f:FPLX OR f:HGNC_GROUP) AND f.id IN $conceptIds AND c:ID_MAP_NCBI_GENES " +
                        "WITH c, [n in nodes(p) WHERE n:FPLX OR n:HGNC_GROUP | n." + propertyName + "] as familySymbols " +
                        // from gene to ortholog, if that exists
                        "OPTIONAL MATCH (c)<-[:HAS_ELEMENT*]-(a) WHERE (a:AGGREGATE_GENEGROUP OR a:AGGREGATE_TOP_ORTHOLOGY) AND NOT exists(()-[:HAS_ELEMENT]->(a))" +
                        "WITH familySymbols, COALESCE(a." + propertyName + ",c." + propertyName + ") as geneSymbol " +
                        "RETURN familySymbols + [geneSymbol] as symbols";
                final Value parameters = parameters("conceptIds", conceptIds);
                Result result = tx.run(query, parameters);

                while (result.hasNext()) {
                    Record record = result.next();
                    final List<Object> symbols = record.get("symbols").asList();
                    symbols.stream().map(String.class::cast).forEach(symbolSet::add);
                }
                return symbolSet;
            });
        }
        return ret;
    }

    @Override
    public Set<String> getFamilyAndOrthologyGroupNodeProperties(Iterable<? extends String> conceptIds, String propertyName) {
        Set<String> ret;
        try (Session session = driver.session()) {
            ret = session.readTransaction(tx -> {
                Set<String> symbolSet = new HashSet<>();
                final String query = "MATCH (f:CONCEPT) WHERE f.id IN $conceptIds \n" +
                        "CALL {\n" +
                        "    WITH f\n" +
                        "    WITH f\n" +
                        "    WHERE f:FPLX OR f:HGNC_GROUP\n" +
                        "    MATCH p=(f:CONCEPT)-[:IS_BROADER_THAN*]->(c) WHERE f.id IN $conceptIds AND c:ID_MAP_NCBI_GENES\n" +
                        "    WITH c,[n in nodes(p) WHERE n:FPLX OR n:HGNC_GROUP | n." + propertyName + "] as familySymbols " +
                        "    OPTIONAL MATCH (c)<-[:HAS_ELEMENT*]-(a) WHERE (a:AGGREGATE_GENEGROUP OR a:AGGREGATE_TOP_ORTHOLOGY) AND NOT exists(()-[:HAS_ELEMENT]->(a)) \n" +
                        "    RETURN familySymbols + [COALESCE(a." + propertyName + ",c." + propertyName + ")] as symbols\n" +
                        "\n" +
                        "    UNION\n" +
                        "\n" +
                        "    WITH f\n" +
                        "    WITH f\n" +
                        "    WHERE NOT (f:FPLX OR f:HGNC_GROUP)\n" +
                        "    RETURN [f." + propertyName + "] as symbols\n" +
                        "}\n" +
                        "return symbols";
                final Value parameters = parameters("conceptIds", conceptIds);
                Result result = tx.run(query, parameters);

                while (result.hasNext()) {
                    Record record = result.next();
                    final List<Object> symbols = record.get("symbols").asList();
                    symbols.stream().map(String.class::cast).forEach(symbolSet::add);
                }
                return symbolSet;
            });
        }
        return ret;
    }

}
