package de.julielab.gepi.core.retrieval.data;

import java.util.List;

public class Event {
	private List<String> allEventTypes;
	
	private List<Argument> arguments;

	private String highlightedSentence;

	private int likelihood;

	private String mainEventType;

	private int numDistinctArguments;
	private String sentence;
	public List<String> getAllEventTypes() {
		return allEventTypes;
	}
	public Argument getArgument(int position) {
		return arguments.get(position);
	}

	public List<Argument> getArguments() {
		return arguments;
	}

	public String getFirstArgumentGeneId() {
		return arguments.get(0).getGeneId();
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

	public String getSecondArgumentGeneId() {
		if (getNumArguments() > 1)
			return arguments.get(1).getGeneId();
		return "";
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

	@Override
	public String toString() {
		return getMainEventType() + ": " + arguments;
	}

}
