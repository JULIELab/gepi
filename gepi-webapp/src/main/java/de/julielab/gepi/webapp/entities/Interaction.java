package de.julielab.gepi.webapp.entities;

/**
 * This entity represents a result interaction, i.e. two genes or proteins, the
 * interaction type, the sentence in which the interaction was identified etc.
 * 
 * @author faessler
 *
 */
public class Interaction {
	private String documentId;
	private String interactionPartner1Id;
	private String interactionPartner2Id;
	private String interactionPartner1Text;
	private String interactionPartner2Text;
	private String interactionType;
	private String sentenceText;

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public String getInteractionPartner1Id() {
		return interactionPartner1Id;
	}

	public void setInteractionPartner1Id(String interactionPartner1Id) {
		this.interactionPartner1Id = interactionPartner1Id;
	}

	public String getInteractionPartner2Id() {
		return interactionPartner2Id;
	}

	public void setInteractionPartner2Id(String interactionPartner2Id) {
		this.interactionPartner2Id = interactionPartner2Id;
	}

	public String getInteractionPartner1Text() {
		return interactionPartner1Text;
	}

	public void setInteractionPartner1Text(String interactionPartner1Text) {
		this.interactionPartner1Text = interactionPartner1Text;
	}

	public String getInteractionPartner2Text() {
		return interactionPartner2Text;
	}

	public void setInteractionPartner2Text(String interactionPartner2Text) {
		this.interactionPartner2Text = interactionPartner2Text;
	}

	public String getInteractionType() {
		return interactionType;
	}

	public void setInteractionType(String interactionType) {
		this.interactionType = interactionType;
	}

	public String getSentenceText() {
		return sentenceText;
	}

	public void setSentenceText(String sentenceText) {
		this.sentenceText = sentenceText;
	}

}
