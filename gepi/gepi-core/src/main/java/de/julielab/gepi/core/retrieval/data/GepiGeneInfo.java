package de.julielab.gepi.core.retrieval.data;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Builder
@Getter
public class GepiGeneInfo {
    private String conceptId;
    private String originalId;
    private String symbol;
    private List<String> synonyms;
    private List<String> descriptions;
    private Set<String> labels;

    public boolean isAggregate() {
        return conceptId != null ? conceptId.startsWith("atid") : false;
    }

}
