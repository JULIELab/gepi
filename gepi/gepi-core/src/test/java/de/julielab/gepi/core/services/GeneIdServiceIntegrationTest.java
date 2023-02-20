package de.julielab.gepi.core.services;

import com.google.common.collect.Multimap;
import de.julielab.gepi.core.retrieval.data.GepiConceptInfo;
import de.julielab.gepi.core.retrieval.data.IdConversionResult;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.junit.rule.Neo4jRule;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static de.julielab.gepi.core.services.GeneIdService.*;
import static org.assertj.core.api.Assertions.assertThat;

public class GeneIdServiceIntegrationTest {
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
        final Multimap<String, String> idMap = geneIdService.convertConcept2AggregateGepiIds(Stream.of("2475", "207", "56718"), CONCEPT_LABEL, PROP_ORGID).get();
        assertThat(idMap.get("2475")).containsExactlyInAnyOrder("atid2");
        assertThat(idMap.get("207")).containsExactly("atid3");
        assertThat(idMap.get("56718")).containsExactly("tid2");
    }

    @Test
    public void convertPrefixedGene2AggregateIds() throws Exception {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final Multimap<String, String> idMap = geneIdService.convertConcept2AggregateGepiIds(Stream.of("gene:2475"), CONCEPT_LABEL, PROP_ORGID).get();
        assertThat(idMap.get("gene:2475")).containsExactlyInAnyOrder("atid2");
    }

    @Test
    public void convertGo2ConceptId() throws Exception {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final Multimap<String, String> idMap = geneIdService.convert2Gepi(Stream.of("go:GO:1234"), GO_LABEL, PROP_ORGID).get();
        assertThat(idMap.get("go:GO:1234")).containsExactlyInAnyOrder("tid6");

        // without prefix
        final Multimap<String, String> idMap2 = geneIdService.convert2Gepi(Stream.of("GO:1234"), GO_LABEL, PROP_ORGID).get();
        assertThat(idMap2.get("GO:1234")).containsExactlyInAnyOrder("tid6");

        // upper case prefix
        final Multimap<String, String> idMap3 = geneIdService.convert2Gepi(Stream.of("GO:GO:1234"), GO_LABEL, PROP_ORGID).get();
        assertThat(idMap3.get("GO:GO:1234")).containsExactlyInAnyOrder("tid6");
    }

    @Test
    public void convertUp2AggregateIds() throws Exception {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final Multimap<String, String> idMap = geneIdService.convertMappedGene2AggregateGepiIds(Stream.of("up:P42345"), UP_LABEL, PROP_ORGID).get();
        assertThat(idMap.get("up:P42345")).containsExactlyInAnyOrder("atid2");

        // without prefix
        final Multimap<String, String> idMap2 = geneIdService.convertMappedGene2AggregateGepiIds(Stream.of("P42345"), UP_LABEL, PROP_ORGID).get();
        assertThat(idMap2.get("P42345")).containsExactlyInAnyOrder("atid2");

        // upper case prefix
        final Multimap<String, String> idMap3 = geneIdService.convertMappedGene2AggregateGepiIds(Stream.of("UP:P42345"), UP_LABEL, PROP_ORGID).get();
        assertThat(idMap3.get("UP:P42345")).containsExactlyInAnyOrder("atid2");

        // mnemonic
        final Multimap<String, String> idMap4 = geneIdService.convertMappedGene2AggregateGepiIds(Stream.of("up:MTOR_HUMAN"), UP_LABEL, PROP_UP_ID).get();
        assertThat(idMap4.get("up:MTOR_HUMAN")).containsExactlyInAnyOrder("atid2");

        // mnemonic without prefix
        final Multimap<String, String> idMap5 = geneIdService.convertMappedGene2AggregateGepiIds(Stream.of("MTOR_HUMAN"), UP_LABEL, PROP_UP_ID).get();
        assertThat(idMap5.get("MTOR_HUMAN")).containsExactlyInAnyOrder("atid2");
    }

    @Test
    public void convert() throws Exception {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final IdConversionResult conversionResult = geneIdService.convert(Stream.of("gene:2475", "GO:1234", "UP:MTOR_HUMAN"), IdType.GEPI_AGGREGATE).get();
        final Multimap<String, String> idMap = conversionResult.getConvertedItems();
        assertThat(idMap.keySet()).containsExactlyInAnyOrder("gene:2475", "GO:1234", "UP:MTOR_HUMAN");
        assertThat(idMap.get("gene:2475")).containsExactly("atid2");
        assertThat(idMap.get("GO:1234")).containsExactly("tid6");
        assertThat(idMap.get("UP:MTOR_HUMAN")).containsExactly("atid2");
    }

    @Test
    public void convertGeneNames2ConceptIds() throws Exception {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final IdConversionResult conversionResult = geneIdService.convert(Stream.of("mtor", "akt1"), IdType.GENE_NAME, IdType.GENE_ID).get();
        final Multimap<String, String> idMap = conversionResult.getConvertedItems();
        assertThat(idMap.get("mtor")).containsExactlyInAnyOrder("atid2", "tid2");
        assertThat(idMap.get("akt1")).containsExactly("atid3");
    }

    @Test
    public void convertGeneNames2GeneIdsWithTaxIds() throws Exception {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final IdConversionResult conversionResult = geneIdService.convert(Stream.of("mtor", "akt1"), IdType.GENE_NAME, IdType.GENE_ID).get();
        final Multimap<String, String> idMap = conversionResult.getConvertedItems();
        assertThat(idMap.get("mtor")).containsExactlyInAnyOrder("atid2", "tid2");
        assertThat(idMap.get("akt1")).containsExactly("atid3");
    }

    @Test
    public void conceptNameNormalization() throws Exception {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final IdConversionResult conversionResult = geneIdService.convert(Stream.of("&aK t-1?/"), IdType.GENE_NAME, IdType.GENE_ID).get();
        final Multimap<String, String> idMap = conversionResult.getConvertedItems();
        assertThat(idMap.keySet()).containsExactlyInAnyOrder("&aK t-1?/");
        assertThat(idMap.get("&aK t-1?/")).containsExactly("atid3");
    }

    @Test
    public void getGeneInfo() throws Exception {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final IdConversionResult conversionResult = geneIdService.convert(Stream.of("mtor", "akt1"), IdType.GENE_NAME, IdType.GEPI_AGGREGATE).get();
        final Map<String, GepiConceptInfo> geneInfo = geneIdService.getGeneInfo(conversionResult.getTargetIds());
        assertThat(geneInfo).containsKeys("atid2", "atid3");
        assertThat(geneInfo.get("tid2").getSymbol()).isEqualTo("Mtor");
        assertThat(geneInfo.get("atid2").getSymbol()).isEqualTo("mTOR");
        assertThat(geneInfo.get("atid2").getOriginalId()).isEqualTo("2475");
        assertThat(geneInfo.get("atid3").getSymbol()).isEqualTo("AKT1");
    }

    @Test
    public void getSymbolsFromFamilyConceptNode() {
        // We want the homology top aggregate symbol for a HGNC_GROUP concept. The top homology has AKT1 as symbol,
        // the actual gene "Akt1" for differentiation.
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final Set<String> symbols = geneIdService.getGeneAggregateSymbolsForFamilyConcepts(List.of("tid8"), PROP_PREFNAME);
        assertThat(symbols).containsExactlyInAnyOrder("AKT", "AKT1");
    }

    @Test
    public void getSymbolsFromFamilyConceptNode2() {
        // We want the homology top aggregate symbol for a FPLX concept. However, the connected rat:Mtor node
        // does not have a top aggregate. So its own symbol should be returned.
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final Set<String> symbols = geneIdService.getGeneAggregateSymbolsForFamilyConcepts(List.of("tid9"), PROP_PREFNAME);
        assertThat(symbols).containsExactlyInAnyOrder("MTOR_FAMILY", "Mtor");
    }

    @Test
    public void getSymbolsFromFamilyConceptNode3() {
        // We want the symbols of the AMPK start node, its sub-family children and the top-orthology aggregate symbol
        // of its genes. All those elements could appear due hyponym resolution when we search for AMPK
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final Set<String> symbols = geneIdService.getGeneAggregateSymbolsForFamilyConcepts(List.of("tid10"), PROP_PREFNAME);
        assertThat(symbols).containsExactlyInAnyOrder("AMPK", "AMPK_ALPHA", "AMPK_BETA", "PRKAA1", "PRKAB1");
    }

    @Test
    public void getFamilyAndOrthologyGroupNodeProperties() {
        // getFamilyAndOrthologyGroupNodeProperties() tests are very similar to the getSymbolsFromFamilyConceptNode() tests except that the
        // getFamilyAndOrthologyGroupNodeProperties() has a conditional return value that does what
        // getSymbolsFromFamilyConceptNode() does when applied to a family node or, if applied to another node,
        // just returns that node's target property.
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final Set<String> symbols = geneIdService.getFamilyAndOrthologyGroupNodeProperties(List.of("tid8"), PROP_PREFNAME);
        assertThat(symbols).containsExactlyInAnyOrder("AKT", "AKT1");
    }

    @Test
    public void getFamilyAndOrthologyGroupNodeProperties2() {
        // getFamilyAndOrthologyGroupNodeProperties() tests are very similar to the getSymbolsFromFamilyConceptNode() tests except that the
        // getFamilyAndOrthologyGroupNodeProperties() has a conditional return value that does what
        // getSymbolsFromFamilyConceptNode() does when applied to a family node or, if applied to another node,
        // just returns that node's target property.
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final Set<String> symbols = geneIdService.getFamilyAndOrthologyGroupNodeProperties(List.of("tid9"), PROP_PREFNAME);
        assertThat(symbols).containsExactlyInAnyOrder("MTOR_FAMILY", "Mtor");
    }

    @Test
    public void getFamilyAndOrthologyGroupNodeProperties3() {
        // getFamilyAndOrthologyGroupNodeProperties() tests are very similar to the getSymbolsFromFamilyConceptNode() tests except that the
        // getFamilyAndOrthologyGroupNodeProperties() has a conditional return value that does what
        // getSymbolsFromFamilyConceptNode() does when applied to a family node or, if applied to another node,
        // just returns that node's target property.
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final Set<String> symbols = geneIdService.getFamilyAndOrthologyGroupNodeProperties(List.of("tid10"), PROP_PREFNAME);
        assertThat(symbols).containsExactlyInAnyOrder("AMPK", "AMPK_ALPHA", "AMPK_BETA", "PRKAA1", "PRKAB1");
    }

    @Test
    public void getFamilyAndOrthologyGroupNodeProperties4() {
        // Here we start from a top aggregate instead of a family concept so we just expect the orthology name back
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), neo4j.boltURI().toString());
        final Set<String> symbols = geneIdService.getFamilyAndOrthologyGroupNodeProperties(List.of("atid2"), PROP_PREFNAME);
        assertThat(symbols).containsExactlyInAnyOrder("mTOR");
    }

    private void setupDB(GraphDatabaseService graphDatabaseService) {
        // Example graph with two groups of genes: mTOR and Akt1.
        // This graph does not show the real data but is shaped to cover the test cases.
        //                            f:FACET
        //                 ---------------------------------------------------------------------
        //               /             |             \                  \                     \
        //     t(mTOR,atid2)  r(Mtor,tid2)      a3(AKT1,atid3)     prkaa1(PRKAA1,tid12)    prkab1(PRKAB1, tid13)
        //       /       \                             /      \
        //    a(atid0)  a2(atid1)                 akth(tid3)  aktm(tid4)
        //    /     \         |
        // h(tid0)  m(tid1)  d(tid7)
        //
        //
        // Mappings:
        //  u(MTOR_HUMAN,tid5)--h(tid0)
        //  go(GO:1234,tid6)--h(tid0)
        //  go(GO:1234,tid6)--akth(tid3)
        //
        // Families:
        // akt(HGNC_GROUP:AKT, tid8)--akth(tid3)
        // mf(FPLX:MTOR, tid9)--r(tid2)
        // ampk(FPLX:AMPK, tid10)--ampka(tid11)
        // ampka(FPLX:AMPKA, tid11)--PRKAA1(tid12)
        // ampkb(FPLX:AMPKB, tid14)--PRKAB1(tid13)
        final String testData = "CREATE (f:FACET {id:'fid0', name:'Genes'})," +
                "(a:AGGREGATE_GENEGROUP:AGGREGATE:CONCEPT {preferredName:'mTOR',preferredName_normalized:'mtor',id:'atid0'})," +
                "(a2:AGGREGATE_GENEGROUP:AGGREGATE:CONCEPT {preferredName:'mTOR',preferredName_normalized:'mtor',id:'atid1'})," +
                "(t:AGGREGATE_TOP_ORTHOLOGY:AGGREGATE:CONCEPT {preferredName:'mTOR',preferredName_normalized:'mtor',id:'atid2',originalId:'2475'})," +
                "(h:ID_MAP_NCBI_GENES:CONCEPT {preferredName:'mTOR',preferredName_normalized:'mtor',originalId:'2475',id:'tid0',taxId:'9606'})," +
                "(m:ID_MAP_NCBI_GENES:CONCEPT {preferredName:'Mtor',preferredName_normalized:'mtor',originalId:'56717',id:'tid1',taxId:'10090'})," +
                "(d:ID_MAP_NCBI_GENES:CONCEPT {preferredName:'mtor',preferredName_normalized:'mtor',originalId:'324254',id:'tid7',taxId:'7955'})," +
                "(r:ID_MAP_NCBI_GENES:CONCEPT {preferredName:'Mtor',preferredName_normalized:'mtor',originalId:'56718',id:'tid2',taxId:'10116'})," +
                "(f)-[:HAS_ROOT_CONCEPT]->(t)," +
                "(f)-[:HAS_ROOT_CONCEPT]->(r)," +
                "(t)-[:HAS_ELEMENT]->(a)," +
                "(t)-[:IS_BROADER_THAN]->(a)," +
                "(t)-[:HAS_ELEMENT]->(a2)," +
                "(t)-[:IS_BROADER_THAN]->(a2)," +
                "(a)-[:HAS_ELEMENT]->(h)," +
                "(a)-[:IS_BROADER_THAN]->(h)," +
                "(a)-[:HAS_ELEMENT]->(m)," +
                "(a)-[:IS_BROADER_THAN]->(m)," +
                "(a2)-[:HAS_ELEMENT]->(d)," +
                "(a2)-[:IS_BROADER_THAN]->(d)," +
                "(a3:AGGREGATE_GENEGROUP:AGGREGATE:CONCEPT {preferredName:'AKT1',preferredName_normalized:'akt1',id:'atid3'})," +
                "(akth:ID_MAP_NCBI_GENES:CONCEPT {preferredName:'Akt1',preferredName_normalized:'akt1',originalId:'207',id:'tid3',taxId:'9606'})," +
                "(aktm:ID_MAP_NCBI_GENES:CONCEPT {preferredName:'Akt1',preferredName_normalized:'akt1',originalId:'11651',id:'tid4',taxId:'10090'})," +
                "(f)-[:HAS_ROOT_CONCEPT]->(a3)," +
                "(a3)-[:HAS_ELEMENT]->(akth)," +
                "(a3)-[:HAS_ELEMENT]->(aktm)," +
                "(u:UNIPROT:CONCEPT {originalId:'P42345',id:'tid5',sourceIds1:'MTOR_HUMAN'})," +
                "(go:GENE_ONTOLOGY:CONCEPT {originalId:'GO:1234',id:'tid6'})," +
                "(u)-[:IS_MAPPED_TO]->(h)," +
                "(go)<-[:IS_ANNOTATED_WITH]-(h)," +
                "(go)<-[:IS_ANNOTATED_WITH]-(akth)," +
                "(akt:HGNC_GROUP:CONCEPT {id:'tid8',preferredName:'AKT'})," +
                "(mf:FPLX:CONCEPT {id:'tid9',preferredName:'MTOR_FAMILY'})," +
                "(ampk:FPLX:CONCEPT {id:'tid10',preferredName:'AMPK'})," +
                "(ampka:FPLX:CONCEPT {id:'tid11',preferredName:'AMPK_ALPHA'})," +
                "(ampkb:FPLX:CONCEPT {id:'tid14',preferredName:'AMPK_BETA'})," +
                "(prkaa1:ID_MAP_NCBI_GENES:CONCEPT {id:'tid12',preferredName:'PRKAA1'})," +
                "(prkab1:ID_MAP_NCBI_GENES:CONCEPT {id:'tid13',preferredName:'PRKAB1'})," +
                "(akt)-[:IS_BROADER_THAN]->(akth)," +
                "(mf)-[:IS_BROADER_THAN]->(r)," +
                "(ampk)-[:IS_BROADER_THAN]->(ampka),"+
                "(ampk)-[:IS_BROADER_THAN]->(ampkb),"+
                "(ampka)-[:IS_BROADER_THAN]->(prkaa1),"+
                "(ampkb)-[:IS_BROADER_THAN]->(prkab1)";
        try (final Transaction tx = graphDatabaseService.beginTx()) {
            tx.execute(testData);
            tx.commit();
        }

    }
}