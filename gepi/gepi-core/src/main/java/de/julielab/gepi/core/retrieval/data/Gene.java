package de.julielab.gepi.core.retrieval.data;

public class Gene {
	private String geneId;
	private String conceptId;
	private String topHomologyId;
	private String preferredName;
	private String text;

	public Gene(String geneId, String conceptId, String topHomologyId, String preferredName, String text) {
		super();
		this.geneId = geneId;
		this.conceptId = conceptId;
		this.topHomologyId = topHomologyId;
		this.preferredName = preferredName;
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

}
