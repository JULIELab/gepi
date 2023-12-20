package de.julielab.gepi.webapp.data;

import de.julielab.gepi.core.retrieval.data.GepiConceptInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class InputMapping {
    private String input;
//    private GepiConceptInfo inputInfo;
    private GepiConceptInfo targetInfo;

    public boolean targetFound() {
        return targetInfo != null;
    }

    public String getTarget() {
        return targetInfo != null ? targetInfo.getSymbol() : "no gene symbol found";
    }

    public String getType() {
        if (targetInfo == null)
            return "not found";
        if (targetInfo.getLabels().contains("AGGREGATE_FPLX_HGNC"))
            return "FPLX / HGNC Group";
        if (targetInfo.getLabels().contains("FPLX"))
            return "FamPlex Concept";
        if (targetInfo.getLabels().contains("HGNC_GROUP"))
            return "HGNC Gene Group";
        if (targetInfo.getLabels().contains("AGGREGATE_TOP_ORTHOLOGY") || targetInfo.getLabels().contains("AGGREGATE_GENEGROUP"))
            return "Orthology Cluster";
        if (targetInfo.getLabels().contains("UNIPROT"))
            return "UniProt Concept";
        if (targetInfo.getLabels().contains("GENE_ONTOLOGY"))
            return "GO Concept";
        if (targetInfo.getLabels().contains("ENSEMBL"))
            return "Ensembl Gene ID";
        if (targetInfo.getLabels().contains("HGNC"))
            return "HGNC Gene ID";
        return "NCBI Gene ID";
    }

//    public String getInputSourceLink() {
//        if (inputIdType == null)
//            return "#";
//        if (inputIdType == IdType.FAMPLEX)
//            return "https://github.com/sorgerlab/famplex/";
//        if (inputIdType == IdType.HGNC_GROUP)
//            return "https://www.genenames.org/data/genegroup/#!/group/" + targetInfo.getOriginalId();
//        if (inputIdType == IdType.HGNC)
//            return "https://www.genenames.org/data/gene-symbol-report/#!/hgnc_id/" + targetInfo.getOriginalId();
//        if (targetInfo.getLabels().contains("AGGREGATE_TOP_ORTHOLOGY") || targetInfo.getLabels().contains("AGGREGATE_GENEGROUP"))
//            return "https://ncbiinsights.ncbi.nlm.nih.gov/2018/02/27/gene_orthologs-file-gene-ftp/";
//        if (targetInfo.getLabels().contains("UNIPROT"))
//            return "https://www.uniprot.org/uniprotkb/" + targetInfo.getOriginalId();
//        if (targetInfo.getLabels().contains("GENE_ONTOLOGY"))
//            return "http://amigo.geneontology.org/amigo/term/" + targetInfo.getOriginalId();
//        if (targetInfo.getLabels().contains("ENSEMBL"))
//            return "http://www.ensembl.org/id/" + targetInfo.getOriginalId();
//        return "https://www.ncbi.nlm.nih.gov/gene/";
//    }

    public String getConceptSourceLink() {
        if (targetInfo == null)
            return "#";
        if (targetInfo.getLabels().contains("FPLX"))
            return "https://github.com/sorgerlab/famplex/";
        if (targetInfo.getLabels().contains("HGNC_GROUP"))
            return "https://www.genenames.org/data/genegroup/#!/group/" + targetInfo.getOriginalId();
        if (targetInfo.getLabels().contains("HGNC"))
            return "https://www.genenames.org/data/gene-symbol-report/#!/hgnc_id/" + targetInfo.getOriginalId();
        if (targetInfo.getLabels().contains("AGGREGATE_TOP_ORTHOLOGY") || targetInfo.getLabels().contains("AGGREGATE_GENEGROUP"))
            return "https://ncbiinsights.ncbi.nlm.nih.gov/2018/02/27/gene_orthologs-file-gene-ftp/";
        if (targetInfo.getLabels().contains("UNIPROT"))
            return "https://www.uniprot.org/uniprotkb/" + targetInfo.getOriginalId();
        if (targetInfo.getLabels().contains("GENE_ONTOLOGY"))
            return "http://amigo.geneontology.org/amigo/term/" + targetInfo.getOriginalId();
        if (targetInfo.getLabels().contains("ENSEMBL"))
            return "http://www.ensembl.org/id/" + targetInfo.getOriginalId();
        return "https://www.ncbi.nlm.nih.gov/gene/";
    }

    public String getTitle() {
        if (targetInfo == null)
            return "no database entry was found for the input name";
        if (targetInfo.getLabels().contains("AGGREGATE_FPLX_HGNC"))
            return "This concept exists in FamPlex and in HGNC groups. Click to open external source.";
        if (targetInfo.getLabels().contains("FPLX"))
            return "FamPlex is a database that contains protein complexes, families and groups. Click to open external source.";
        if (targetInfo.getLabels().contains("HGNC_GROUP"))
            return "HGNC organizes genes in group hierarchies. Click to open external source.";
        if (targetInfo.getLabels().contains("AGGREGATE_TOP_ORTHOLOGY") || targetInfo.getLabels().contains("AGGREGATE_GENEGROUP"))
            return "The NCBI gene_orthologs file contains clusters of genes that are orthologous to each other. Input gene names are mapped to ortholog clusters by default to extend the result set coverage. Click to open external source.";
        if (targetInfo.getLabels().contains("UNIPROT"))
            return "The UniProt protein ID was mapped to an NCBI Gene ID. Click to open external source.";
        if (targetInfo.getLabels().contains("GENE_ONTOLOGY"))
            return "The Gene Ontology contains formal descriptions of gene functions. NCBI Gene IDs may be annotated with GO terms. Searching for a GO term finds the genes annotated with it. Click to open external source.";
        if (targetInfo.getLabels().contains("ENSEMBL"))
            return "The Ensembl gene database ID was mapped to an NCBI Gene ID. Click to open external source.";
        if (targetInfo.getLabels().contains("HGNC"))
            return "The HGNC gene database ID was mapped to an NCBI Gene ID. Click to open external source.";
        return "The NCBI Gene database is the gene database used to identify gene and gene product names found in document texts. Click to open external source.";
    }
}
