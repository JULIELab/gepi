package de.julielab.gepi.webapp.data;

import de.julielab.gepi.core.retrieval.data.GepiGeneInfo;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Getter
public class InputMapping {
    private String input;
    private GepiGeneInfo targetInfo;

    public boolean targetFound() {
        return targetInfo != null;
    }

    public String getTarget() {
        return targetInfo != null ? targetInfo.getSymbol() : "no gene symbol found";
    }

    public String getType() {
        if (targetInfo == null)
            return "not found";
        if (targetInfo.getLabels().contains("FPLX"))
            return "FamPlex Concept";
        if (targetInfo.getLabels().contains("HGNC_GROUP"))
            return "HGNC Gene Group";
        if (targetInfo.getLabels().contains("AGGREGATE_TOP_ORTHOLOGY") || targetInfo.getLabels().contains("AGGREGATE_GENEGROUP"))
            return "Orthology Cluster";
        return "NCBI Gene ID";
    }

    public String getConceptSourceLink() {
        if (targetInfo == null)
            return "#";
        if (targetInfo.getLabels().contains("FPLX"))
            return "https://github.com/sorgerlab/famplex/";
        if (targetInfo.getLabels().contains("HGNC_GROUP"))
            return "https://www.genenames.org/data/genegroup/#!";
        if (targetInfo.getLabels().contains("AGGREGATE_TOP_ORTHOLOGY") || targetInfo.getLabels().contains("AGGREGATE_GENEGROUP"))
            return "https://ncbiinsights.ncbi.nlm.nih.gov/2018/02/27/gene_orthologs-file-gene-ftp/";
        return "https://www.ncbi.nlm.nih.gov/gene/";
    }

    public String getTitle() {
        if (targetInfo == null)
            return "no database entry was found for the input name";
        if (targetInfo.getLabels().contains("AGGREGATE_FPLX_HGNC"))
            return "This concept exists in FamPlex and in HGNC groups.";
        if (targetInfo.getLabels().contains("FPLX"))
            return "FamPlex is a database that contains protein complexes, families and groups.";
        if (targetInfo.getLabels().contains("HGNC_GROUP"))
            return "HGNC organizes genes in group hierarchies.";
        if (targetInfo.getLabels().contains("AGGREGATE_TOP_ORTHOLOGY") || targetInfo.getLabels().contains("AGGREGATE_GENEGROUP"))
            return "The NCBI gene_orthologs file contains clusters of genes that are orthologous to each other. Input gene names are mapped to ortholog clusters by default to extend the result set coverage.";
        return "The NCBI Gene database is the gene database used to identify gene and gene product names found in document texts.";
    }
}
