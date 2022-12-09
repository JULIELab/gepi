package de.julielab.gepi.core.services;

import com.google.common.collect.Multimap;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
public class GeneIdServiceTest {
    @Test
    public void mapIdentifierToPrefix() {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), null);
        final Map<String, String> ids2prefixes = geneIdService.mapIdentifierToPrefix(Stream.of("gene:2475", "go:GO:00158484", "up:IL2_HUMAN", "23350", "UP:IL2_MOUSE"));
        Map<String, String> expected = new HashMap<>(Map.of("2475", "gene:", "GO:00158484", "go:", "IL2_HUMAN", "up:", "IL2_MOUSE", "UP:"));
        expected.put("23350", null);
        assertThat(ids2prefixes).containsAllEntriesOf(expected);
    }

    @Test
    public void determineIdTypes() {
        final GeneIdService geneIdService = new GeneIdService(LoggerFactory.getLogger(GeneIdService.class), null);
        final Multimap<IGeneIdService.IdType, String> type2id = geneIdService.determineIdTypes(Stream.of("gene:2475", "go:GO:00158484", "GO:1234", "up:IL2_HUMAN", "23350", "P23476", "HGNC:HGNC:4567", "ens:ENS3457923", "fplx:AMPK", "hgncg:5643"));
        assertThat(type2id.keys()).containsExactlyInAnyOrder(IGeneIdService.IdType.GENE_ID, IGeneIdService.IdType.GO, IGeneIdService.IdType.GO, IGeneIdService.IdType.UNIPROT_MNEMONIC, IGeneIdService.IdType.GENE_ID, IGeneIdService.IdType.UNIPROT_ACCESSION, IGeneIdService.IdType.HGNC, IGeneIdService.IdType.ENSEMBL, IGeneIdService.IdType.FAMPLEX, IGeneIdService.IdType.HGNC_GROUP);
        assertThat(type2id.get(IGeneIdService.IdType.GENE_ID)).containsExactlyInAnyOrder("gene:2475", "23350");
        assertThat(type2id.get(IGeneIdService.IdType.GO)).containsExactlyInAnyOrder("go:GO:00158484", "GO:1234");
        assertThat(type2id.get(IGeneIdService.IdType.UNIPROT_MNEMONIC)).containsExactlyInAnyOrder("up:IL2_HUMAN");
        assertThat(type2id.get(IGeneIdService.IdType.UNIPROT_ACCESSION)).containsExactlyInAnyOrder("P23476");
        assertThat(type2id.get(IGeneIdService.IdType.HGNC)).containsExactlyInAnyOrder("HGNC:HGNC:4567");
        assertThat(type2id.get(IGeneIdService.IdType.ENSEMBL)).containsExactlyInAnyOrder("ens:ENS3457923");
        assertThat(type2id.get(IGeneIdService.IdType.FAMPLEX)).containsExactlyInAnyOrder("fplx:AMPK");
        assertThat(type2id.get(IGeneIdService.IdType.HGNC_GROUP)).containsExactlyInAnyOrder("hgncg:5643");

    }

}