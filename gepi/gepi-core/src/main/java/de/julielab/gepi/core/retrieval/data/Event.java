package de.julielab.gepi.core.retrieval.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Event {
    protected List<String> allEventTypes;

    protected List<Argument> arguments;

    protected int likelihood;

    protected String mainEventType;

    protected int numDistinctArguments;

    protected String sentence;

    protected String eventId;

    protected String documentType;

    protected String docId;

    protected boolean sentenceMatchingFulltextQuery;
    protected boolean paragraphMatchingFulltextQuery;
    String hlSentence;
    private String paragraph;
    private String hlParagraph;

    public String getHlParagraph() {
        return hlParagraph;
    }

    public void setHlParagraph(String hlParagraph) {
        this.hlParagraph = hlParagraph;
    }

    public String getHlSentence() {
        return hlSentence;
    }

    public void setHlSentence(String hlSentence) {
        this.hlSentence = hlSentence;
    }

    public boolean isSentenceMatchingFulltextQuery() {
        return sentenceMatchingFulltextQuery;
    }

    public void setSentenceMatchingFulltextQuery(boolean sentenceMatchingFulltextQuery) {
        this.sentenceMatchingFulltextQuery = sentenceMatchingFulltextQuery;
    }

    public boolean isParagraphMatchingFulltextQuery() {
        return paragraphMatchingFulltextQuery;
    }

    public void setParagraphMatchingFulltextQuery(boolean paragraphMatchingFulltextQuery) {
        this.paragraphMatchingFulltextQuery = paragraphMatchingFulltextQuery;
    }

    public String getPmid() {
        return docId;
    }

    public void setDocId(String pmid) {
        this.docId = pmid;
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

    public void setAllEventTypes(List<String> allEventTypes) {
        this.allEventTypes = allEventTypes;
    }

    public Argument getArgument(int position) {
        return arguments.get(position);
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public void setArguments(List<Argument> arguments) {
        this.arguments = arguments;
    }

    public int getLikelihood() {
        return likelihood;
    }

    public void setLikelihood(int likelihood) {
        this.likelihood = likelihood;
    }

    public String getMainEventType() {
        if (mainEventType != null)
            return mainEventType;
        return "";
    }

    public void setMainEventType(String mainEventType) {
        this.mainEventType = mainEventType;
    }

    public int getNumArguments() {
        return arguments.size();
    }

    public int getNumDistinctArguments() {
        return numDistinctArguments;
    }

    public void setNumDistinctArguments(int numDistinctArguments) {
        this.numDistinctArguments = numDistinctArguments;
    }

    public String getSentence() {
        if (sentence != null)
            return sentence;
        return "";
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
        e.likelihood = likelihood;
        e.mainEventType = mainEventType;
        e.numDistinctArguments = numDistinctArguments;
        e.sentence = sentence;
        return e;
    }

    public String getDocId() {
        if (getEventId().startsWith("PMC")) return "PMC" + docId;
        else if (getPmid() != null) return getPmid();
        throw new IllegalStateException("No document ID for event " + this);
    }

    public String getParagraph() {
        return paragraph;
    }

    public void setParagraph(String paragraph) {
        this.paragraph = paragraph;
    }
}
