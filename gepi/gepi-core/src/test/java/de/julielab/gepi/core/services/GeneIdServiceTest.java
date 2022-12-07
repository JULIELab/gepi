package de.julielab.gepi.core.services;

import com.google.common.collect.Multimap;
import de.julielab.gepi.core.retrieval.data.GepiGeneInfo;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.junit.rule.Neo4jRule;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Stream;

import static de.julielab.gepi.core.services.GeneIdService.FPLX_LABEL;
import static org.assertj.core.api.Assertions.assertThat;
public class GeneIdServiceTest {
    @Rule()
    public Neo4jRule neo4j = new Neo4jRule().withFixture(graphDatabaseService -> {
        setupDB(graphDatabaseService);
        return null;
    });

    @Test
    public void convertGeneNames2AggregateIds() throws Exception {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final Multimap<String, String> idMap = geneIdService.convertConceptNames2AggregateIds(Stream.of("MTOR", "akt1")).get();
        assertThat(idMap.get("MTOR")).containsExactlyInAnyOrder("atid2", "tid2");
        assertThat(idMap.get("akt1")).containsExactly("atid3");
    }

    @Test
    public void convertGene2AggregateIds() throws Exception {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final Multimap<String, String> idMap = geneIdService.convertConcept2AggregateGepiIds(Stream.of("2475", "207", "56718"), FPLX_LABEL, "originalId").get();
        assertThat(idMap.get("2475")).containsExactlyInAnyOrder("atid2");
        assertThat(idMap.get("207")).containsExactly("atid3");
        assertThat(idMap.get("56718")).containsExactly("tid2");
    }

    @Test
    public void convertGeneNames2GeneIds() throws Exception {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final IdConversionResult conversionResult = geneIdService.convert(Stream.of("mtor", "akt1"), IGeneIdService.IdType.GENE_NAME, IGeneIdService.IdType.GEPI_CONCEPT).get();
        final Multimap<String, String> idMap = conversionResult.getConvertedItems();
        assertThat(idMap.get("mtor")).containsExactlyInAnyOrder("2475", "56717", "324254", "56718");
        assertThat(idMap.get("akt1")).containsExactly("207", "11651");
    }

    @Test
    public void convertGeneNames2GeneIdsWithTaxIds() throws Exception {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final IdConversionResult conversionResult = geneIdService.convert(Stream.of("mtor", "akt1"), IGeneIdService.IdType.GENE_NAME, IGeneIdService.IdType.GEPI_CONCEPT).get();
        final Multimap<String, String> idMap = conversionResult.getConvertedItems();
        assertThat(idMap.get("mtor")).containsExactlyInAnyOrder("2475");
        assertThat(idMap.get("akt1")).containsExactly("207");
    }

    @Test
    public void convertUnkownGeneNames() throws Exception {
        // akt1 should be "unknown" because we restrict the search to taxId 7955 (Danio Rerio)
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final IdConversionResult conversionResult = geneIdService.convert(Stream.of("mtor", "akt1"), IGeneIdService.IdType.GENE_NAME, IGeneIdService.IdType.GEPI_CONCEPT).get();
        final Multimap<String, String> idMap = conversionResult.getConvertedItems();
        assertThat(idMap.get("mtor")).containsExactlyInAnyOrder("324254");
        assertThat(conversionResult.getUnconvertedItems()).containsExactly("akt1");
    }

    @Test
    public void getGeneInfo() throws Exception {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final IdConversionResult conversionResult = geneIdService.convert(Stream.of("mtor", "akt1"), IGeneIdService.IdType.GENE_NAME, IGeneIdService.IdType.GEPI_AGGREGATE).get();
        final Map<String, GepiGeneInfo> geneInfo = geneIdService.getGeneInfo(conversionResult.getTargetIds());
        assertThat(geneInfo).containsKeys("atid2", "atid3");
        assertThat(geneInfo.get("tid2").getSymbol()).isEqualTo("Mtor");
        assertThat(geneInfo.get("atid2").getSymbol()).isEqualTo("mTOR");
        assertThat(geneInfo.get("atid2").getOriginalId()).isEqualTo("2475");
        assertThat(geneInfo.get("atid3").getSymbol()).isEqualTo("AKT1");
    }

    @Test
    public void filterGeneIdsForTaxonomyIds() throws Exception {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        // We expect the input genes to be filtered for the taxonomy IDs
        final IdConversionResult conversionResult = geneIdService.convert(Stream.of("2475", "56717", "324254", "56718"), IGeneIdService.IdType.GEPI_CONCEPT, IGeneIdService.IdType.GEPI_CONCEPT).get();
        final Multimap<String, String> convertedItems = conversionResult.getConvertedItems();
        assertThat(convertedItems.size()).isEqualTo(2);
        assertThat(convertedItems.keySet()).containsExactly("56717", "56718");
    }

    private void setupDB(GraphDatabaseService graphDatabaseService) {
        // Example graph with two groups of genes: mTOR and Akt1.
        // This graph does not show the real data but is shaped to cover the test cases.
        //                            f:FACET
        //                 ----------------------------
        //                /             |             \
        //     t:(mTOR,atid2)  r:CONCEPT(mTOR,tid2) a3:(Akt1,atid3)
        //       /       \                             /        \
        //    a(atid0)  a2(atid1)                 akth(tid3)    aktm(tid4)
        //    /     \         |
        // h(tid0)  m(tid1)  d(tid1)
        final String testData = "CREATE (f:FACET {id:'fid0', name:'Genes'})," +
                "(a:AGGREGATE_GENEGROUP:AGGREGATE:CONCEPT {preferredName:'mTOR',preferredName_lc:'mtor',id:'atid0'})," +
                "(a2:AGGREGATE_GENEGROUP:AGGREGATE:CONCEPT {preferredName:'mTOR',preferredName_lc:'mtor',id:'atid1'})," +
                "(t:AGGREGATE_TOP_ORTHOLOGY:AGGREGATE:CONCEPT {preferredName:'mTOR',preferredName_lc:'mtor',id:'atid2',originalId:'2475'})," +
                "(h:ID_MAP_NCBI_GENES:CONCEPT {preferredName:'mTOR',preferredName_lc:'mtor',originalId:'2475',id:'tid0',taxId:'9606'})," +
                "(m:ID_MAP_NCBI_GENES:CONCEPT {preferredName:'Mtor',preferredName_lc:'mtor',originalId:'56717',id:'tid1',taxId:'10090'}),"+
                "(d:ID_MAP_NCBI_GENES:CONCEPT {preferredName:'mtor',preferredName_lc:'mtor',originalId:'324254',id:'tid1',taxId:'7955'}),"+
                "(r:ID_MAP_NCBI_GENES:CONCEPT {preferredName:'Mtor',preferredName_lc:'mtor',originalId:'56718',id:'tid2',taxId:'10116'})," +
                "(f)-[:HAS_ROOT_CONCEPT]->(t),"+
                "(f)-[:HAS_ROOT_CONCEPT]->(r),"+
                "(t)-[:HAS_ELEMENT]->(a),"+
                "(t)-[:HAS_ELEMENT]->(a2),"+
                "(a)-[:HAS_ELEMENT]->(h),"+
                "(a)-[:HAS_ELEMENT]->(m),"+
                "(a2)-[:HAS_ELEMENT]->(d),"+
                "(a3:AGGREGATE_GENEGROUP:AGGREGATE:CONCEPT {preferredName:'AKT1',preferredName_lc:'akt1',id:'atid3'})," +
                "(akth:ID_MAP_NCBI_GENES:CONCEPT {preferredName:'AKT1',preferredName_lc:'akt1',originalId:'207',id:'tid3',taxId:'9606'}),"+
                "(aktm:ID_MAP_NCBI_GENES:CONCEPT {preferredName:'Akt1',preferredName_lc:'akt1',originalId:'11651',id:'tid4',taxId:'10090'}),"+
                "(f)-[:HAS_ROOT_CONCEPT]->(a3),"+
                "(a3)-[:HAS_ELEMENT]->(akth),"+
                "(a3)-[:HAS_ELEMENT]->(aktm)";
        try (final Transaction tx = graphDatabaseService.beginTx()) {
            tx.execute(testData);
            tx.commit();
        }

    }
}