package de.julielab.gepi.core.retrieval.data;

public class Argument implements Comparable<Argument> {

	public enum ComparisonMode {
		TOP_HOMOLOGY
	}

	private String geneId;
	private String conceptId;
	private String topHomologyId;
	private String preferredName;
	private String text;
	private ComparisonMode comparisonMode = ComparisonMode.TOP_HOMOLOGY;

	public Argument(String geneId, String conceptId, String topHomologyId, String text) {
		super();
		this.geneId = geneId;
		this.conceptId = conceptId;
		this.topHomologyId = topHomologyId;
		this.text = text;
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
		return preferredName;
	}

	public void setPreferredName(String preferredName) {
		this.preferredName = preferredName;
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
		case TOP_HOMOLOGY:
			return topHomologyId.compareTo(o.topHomologyId);
		default:
			return topHomologyId.compareTo(o.topHomologyId);
		}
	}

	@Override
	public int hashCode() {
		String keyItem;
		switch(getComparisonMode()) {
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
		switch(getComparisonMode()) {
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

}
