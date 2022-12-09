package de.julielab.gepi.core.services;

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

}