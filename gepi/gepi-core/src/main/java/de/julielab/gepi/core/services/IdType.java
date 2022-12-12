package de.julielab.gepi.core.services;

/**
 * This enumeration lists the kinds of IDs that GePi can work with.
 *
 * @author faessler
 */
public enum IdType {
    /**
     * UniProt / SwissProt accession IDs
     *
     * @see <url>http://www.uniprot.org</url>
     */
    UNIPROT_ACCESSION,
    /**
     * UniProt / SwissProt mnemonics (e.g. IL2_HUMAN)
     *
     * @see <url>http://www.uniprot.org</url>
     */
    UNIPROT_MNEMONIC,
    GO,
    ENSEMBL,
    /**
     * NCBI Gene ID
     *
     * @see <url>https://www.ncbi.nlm.nih.gov/gene</url>
     */
    GENE_ID, GENE_NAME, UNKNOWN,
    /**
     * The GePi-internal gene orthology aggregate IDs
     */
    GEPI_AGGREGATE,
    /**
     * FamPlex protein complex/family
     *
     * @see <url>https://github.com/sorgerlab/famplex</url>
     */
    FAMPLEX,
    HGNC,
    /**
     * HGNC Gene Group
     *
     * @see <url>https://www.genenames.org/data/genegroup/#!/</url>
     */
    HGNC_GROUP
}
