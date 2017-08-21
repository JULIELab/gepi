package de.julielab.gepi.core.retrieval.data;

import java.util.ArrayList;
import java.util.List;

public class Event {
	protected List<String> allEventTypes;

	protected List<Argument> arguments;

	protected String highlightedSentence;

	protected int likelihood;

	protected String mainEventType;

	protected int numDistinctArguments;
	
	protected String sentence;
	
	protected String documentId;
	
	protected String documentType;

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public List<String> getAllEventTypes() {
		return allEventTypes;
	}

	public Argument getArgument(int position) {
		return arguments.get(position);
	}

	public List<Argument> getArguments() {
		return arguments;
	}

	public String getHighlightedSentence() {
		return highlightedSentence;
	}

	public int getLikelihood() {
		return likelihood;
	}

	public String getMainEventType() {
		if (mainEventType != null)
			return mainEventType;
		return "";
	}

	public int getNumArguments() {
		return arguments.size();
	}

	public int getNumDistinctArguments() {
		return numDistinctArguments;
	}

	public String getSentence() {
		if (sentence != null)
			return sentence;
		return "";
	}

	public void setAllEventTypes(List<String> allEventTypes) {
		this.allEventTypes = allEventTypes;
	}

	public void setArguments(List<Argument> arguments) {
		this.arguments = arguments;
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

	public void setNumDistinctArguments(int numDistinctArguments) {
		this.numDistinctArguments = numDistinctArguments;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	public Argument getFirstArgument() {
		return getArgument(0);
	}

	public Argument getSecondArgument() {
		if (arguments.size() < 2)
			return null;
		return getArgument(1);
	}

	@Override
	public String toString() {
		return getMainEventType() + ": " + arguments;
	}

	public Event copy() {
		Event e = new Event();
		e.allEventTypes = allEventTypes != null ? new ArrayList<>(allEventTypes) : null;
		e.arguments = arguments != null ? new ArrayList<>(arguments) : null;
		e.highlightedSentence = highlightedSentence;
		e.likelihood = likelihood;
		e.mainEventType = mainEventType;
		e.numDistinctArguments = numDistinctArguments;
		e.sentence = sentence;
		return e;
	}

}
