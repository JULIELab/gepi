package de.julielab.gepi.core.retrieval.data;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Set;

@Builder
@Getter
@ToString
public class GepiConceptInfo {
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
