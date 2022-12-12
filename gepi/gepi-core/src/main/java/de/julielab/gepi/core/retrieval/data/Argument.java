package de.julielab.gepi.core.retrieval.data;

import org.apache.commons.lang3.StringUtils;

import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_VALUE_MOCK_ARGUMENT;

public class Argument implements Comparable<Argument> {

    private String geneId;
    private String conceptId;
    private String topHomologyId;
    private String topHomologyPreferredName;
    private String preferredName;
    private String text;
    private ComparisonMode comparisonMode = ComparisonMode.TOP_HOMOLOGY_ID;
    private GepiConceptInfo geneInfo;

    public Argument(String geneId, String conceptId, String topHomologyId, String text) {
        super();
        this.geneId = geneId;
        this.conceptId = conceptId;
        this.topHomologyId = topHomologyId;
        this.text = text;
    }

    public String getTopHomologyPreferredName() {
        return topHomologyPreferredName;
    }

    public void setTopHomologyPreferredName(String topHomologyPreferredName) {
        this.topHomologyPreferredName = StringUtils.isAllLowerCase(topHomologyPreferredName) && !topHomologyPreferredName.equals(FIELD_VALUE_MOCK_ARGUMENT) ? topHomologyPreferredName.toUpperCase() : topHomologyPreferredName;

    }

    public String getGeneId() {
        return geneId;
    }

    public void setGeneId(String geneId) {
        this.geneId = geneId;
    }

    public String getConceptId() {
        return conceptId;
    }

    public void setConceptId(String conceptId) {
        this.conceptId = conceptId;
    }

    public String getTopHomologyId() {
        return topHomologyId;
    }

    public void setTopHomologyId(String topHomologyId) {
        this.topHomologyId = topHomologyId;
    }

    public String getPreferredName() {
        if (preferredName ==  null)
            throw new IllegalStateException("The preferredName field for Argument with concept id " + conceptId + " is null.");
        return preferredName;
    }

    public void setPreferredName(String preferredName) {
        this.preferredName = StringUtils.isAllLowerCase(preferredName) && !preferredName.equals(FIELD_VALUE_MOCK_ARGUMENT) ? preferredName.toUpperCase() : preferredName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String toString() {
        return preferredName;
    }

    @Override
    public int compareTo(Argument o) {
        switch (getComparisonMode()) {
            case TOP_HOMOLOGY_PREFERRED_NAME:
                return topHomologyPreferredName.compareTo(o.topHomologyPreferredName);
            case TOP_HOMOLOGY_ID:
            default:
                return topHomologyId.compareTo(o.topHomologyId);
        }
    }

    @Override
    public int hashCode() {
        String keyItem;
        switch (getComparisonMode()) {
            case TOP_HOMOLOGY_PREFERRED_NAME:
                keyItem = topHomologyPreferredName;
                break;
            case TOP_HOMOLOGY_ID:
            default:
                keyItem = topHomologyId;
                break;
        }
        final int prime = 31;
        int result = 1;
        result = prime * result + ((keyItem == null) ? 0 : keyItem.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        String id;
        String otherId;
        Argument other = (Argument) obj;
        switch (getComparisonMode()) {
            case TOP_HOMOLOGY_PREFERRED_NAME:
                id = topHomologyPreferredName;
                otherId = other.topHomologyPreferredName;
                break;
            case TOP_HOMOLOGY_ID:
            default:
                id = topHomologyId;
                otherId = other.topHomologyId;
                break;

        }
        if (id == null) {
            if (otherId != null)
                return false;
        } else if (!id.equals(otherId))
            return false;
        return true;
    }

    public ComparisonMode getComparisonMode() {
        return comparisonMode;
    }

    public void setComparisonMode(ComparisonMode comparisonMode) {
        this.comparisonMode = comparisonMode;
    }

    public void setGeneInfo(GepiConceptInfo geneInfo) {
        this.geneInfo = geneInfo;
    }

    public GepiConceptInfo getGeneInfo() {
        return geneInfo;
    }

    public enum ComparisonMode {
        TOP_HOMOLOGY_ID, TOP_HOMOLOGY_PREFERRED_NAME
    }

}
