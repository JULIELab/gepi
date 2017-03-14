package de.julielab.gepi.core.retrieval.data;

import java.util.List;

public class Event {
	private List<String> allArguments;

	private List<String> allEventTypes;

	private String highlightedSentence;

	private int likelihood;
	private String mainEventType;
	private int numArguments;
	private String sentence;
	public List<String> getAllArguments() {
		return allArguments;
	}
	public List<String> getAllEventTypes() {
		return allEventTypes;
	}

	public String getHighlightedSentence() {
		return highlightedSentence;
	}

	public int getLikelihood() {
		return likelihood;
	}

	public String getMainEventType() {
		return mainEventType;
	}

	public int getNumArguments() {
		return numArguments;
	}

	public String getSentence() {
		return sentence;
	}

	public void setAllArguments(List<String> allArguments) {
		this.allArguments = allArguments;
	}

	public void setAllEventTypes(List<String> allEventTypes) {
		this.allEventTypes = allEventTypes;
	}

	public void setHighlightedSentence(String highlightedSentence) {
		this.highlightedSentence = highlightedSentence;
	}

	public void setLikelihood(int likelihood) {
		this.likelihood = likelihood;
	}

	public void setMainEventType(String mainEventType) {
		this.mainEventType = mainEventType;
	}

	public void setNumArguments(int numArguments) {
		this.numArguments = numArguments;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}
}
