package de.julielab.gepi.core.retrieval.data;

/**
 * Normalization of gene symbols for searching and comparison.
 */
public class GeneSymbolNormalization {
    public static String normalize(String symbol) {
        return symbol.toLowerCase().replaceAll("\\p{P}+|\\s+", "");
    }
}
