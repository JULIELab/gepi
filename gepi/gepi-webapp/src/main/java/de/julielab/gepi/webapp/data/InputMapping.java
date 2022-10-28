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
            return "FamPlex Family";
        if (targetInfo.getLabels().contains("HGNC_GROUP"))
            return "HGNC Gene Group";
        if (targetInfo.getLabels().contains("AGGREGATE_TOP_ORTHOLOGY") || targetInfo.getLabels().contains("AGGREGATE_GENEGROUP"))
            return "Orthology Cluster";
        return "NCBI Gene ID";
    }
}
