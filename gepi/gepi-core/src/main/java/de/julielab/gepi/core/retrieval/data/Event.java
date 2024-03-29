package de.julielab.gepi.core.retrieval.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static de.julielab.gepi.core.retrieval.services.EventRetrievalService.FIELD_VALUE_MOCK_ARGUMENT;

public class Event {
    public static final Event EMPTY = new Event();
    static {
        EMPTY.setArguments(List.of(Argument.EMPTY, Argument.EMPTY));
        EMPTY.setMainEventType(FIELD_VALUE_MOCK_ARGUMENT);
        EMPTY.setAllEventTypes(List.of(FIELD_VALUE_MOCK_ARGUMENT));
        EMPTY.setLikelihood(1);
        EMPTY.setDocId(FIELD_VALUE_MOCK_ARGUMENT);
        EMPTY.setEventId(FIELD_VALUE_MOCK_ARGUMENT);
    }

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
    private List<String> geneMappingSources;
    private int arity;

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
        e.eventId = eventId;
        e.docId = docId;
        e.documentType = documentType;
        e.allEventTypes = allEventTypes != null ? new ArrayList<>(allEventTypes) : null;
        e.arguments = arguments != null ? new ArrayList<>(arguments) : null;
        e.likelihood = likelihood;
        e.mainEventType = mainEventType;
        e.numDistinctArguments = numDistinctArguments;
        e.sentence = sentence;
        return e;
    }

    /**
     * If there are exactly two arguments for this event, swap their positions.
     * @throws IllegalStateException If there are not exactly two arguments.
     */
    public void swapArguments() {
        if (arguments.size() != 2)
            throw new IllegalStateException("There are not exactly two arguments but " + arguments.size());
        Collections.swap(arguments, 0, 1);
    }

    public String getDocId() {
        if (getPmid() != null) return getPmid();
        throw new IllegalStateException("No document ID for event " + this);
    }

    public String getParagraph() {
        return paragraph;
    }

    public void setParagraph(String paragraph) {
        this.paragraph = paragraph;
    }

    public void setGeneMappingSources(List<String> geneMappingSources) {
        this.geneMappingSources = geneMappingSources;
    }

    public List<String> getGeneMappingSources() {
        return geneMappingSources;
    }

    public void setArity(int arity) {
        this.arity = arity;
    }

    public int getArity() {
        return arity;
    }
}
