package de.julielab.gepi.core.retrieval.data;

import java.util.ArrayList;
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
	
	/**
	 * Since multiple atids are available per event partner only provide the first one, assuming this 
	 * one corresponds to the aggregated id, where all homologous genes are connected to.
	 * The first atid per event partner follows directly after the entrez gene id.
	 * 
	 * TODO: Make sure we filter for the correct atids (is the order (first atid is the group id) always guaranteed?).
	 */
	public List<String> getFirstAtidArguments() {
		List<String> firstAtids = new ArrayList<String>();
		
		for (int i = 0; i < allArgumentTokens.size()-1; i++) {
			if ( allArgumentTokens.get(i).matches("^[0-9]+$") )
				firstAtids.add( allArgumentTokens.get(i+1) );
		}
		return firstAtids;
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
		if (mainEventType != null)
			return mainEventType;
		return "";
	}

	public String getSentence() {
		if (sentence != null)
			return sentence;
		return "";
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

	public String getFirstArgumentGeneId() {
		return getAllArguments().get(0);
	}

	public String getSecondArgumentGeneId() {
		if (getNumArguments() > 1)
			return getAllArguments().get(1);
		return "";
	}
	
	/**
	 * Provide all Tokens known to this event as one String.
	 * @return String representation of all tokens known to this event.
	 */
	public String getAllTokensToString() {
		return "All tokens: " + String.join( ", ", allArgumentTokens.toString() );
	}

}
