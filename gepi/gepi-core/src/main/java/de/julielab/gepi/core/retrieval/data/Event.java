package de.julielab.gepi.core.retrieval.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Event {
	protected List<String> allEventTypes;

	protected List<Argument> arguments;

	protected String highlightedSentence;

	protected int likelihood;

	protected String mainEventType;

	protected int numDistinctArguments;
	
	protected String sentence;
	
	protected String eventId;
	
	protected String documentType;

	protected String pmid;

	protected String pmcid;

	public String getPmid() {
		return pmid;
	}

	public void setPmid(String pmid) {
		this.pmid = pmid;
	}

	public String getPmcid() {
		return pmcid;
	}

	public void setPmcid(String pmcid) {
		this.pmcid = pmcid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Event event = (Event) o;
		return Objects.equals(arguments, event.arguments);
	}

	@Override
	public int hashCode() {

		return Objects.hash(arguments);
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
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

	public String getDocId() {
		if (getEventId().startsWith("pmc")) return "PMC" + getPmcid();
		else if (getPmid() != null) return getPmid();
		throw new IllegalStateException("No document ID for event " + this);
	}
}
