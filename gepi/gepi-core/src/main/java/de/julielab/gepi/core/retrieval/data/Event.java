package de.julielab.gepi.core.retrieval.data;

import java.util.List;
import java.util.stream.Collectors;

public class Event {
	private List<String> allArgumentTokens;

	private List<String> allEventTypes;

	private String highlightedSentence;

	private int likelihood;
	private String mainEventType;
	private String sentence;
	private List<String> allArguments;

	public List<String> getAllArguments() {
		if (allArguments == null)
			allArguments = allArgumentTokens.stream().filter(a -> a.matches("^[0-9]+$")).collect(Collectors.toList());
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

	public String getSentence() {
		return sentence;
	}

	/**
	 * The index might store multiple tokens for a single arguments, e.g. its
	 * NCBI Gene ID, its term ID, its aggregate IDs, the original word etc. All
	 * those are set here.
	 * 
	 * @param allArguments
	 */
	public void setAllArgumentTokens(List<String> allArguments) {
		this.allArgumentTokens = allArguments;
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

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	public int getNumArguments() {
		return getAllArguments().size();
	}

	public int getNumDistinctArguments() {
		return (int) getAllArguments().stream().distinct().count();
	}

	public String getArgument(int position) {
		return getAllArguments().get(position);
	}

	@Override
	public String toString() {
		return getMainEventType() + ": " + String.join(", ", getAllArguments());
	}
	
	
}
