package de.julielab.gepi.webapp;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;

import java.util.List;
import java.util.stream.Collectors;

public class BeanModelEvent {

    private Event event;

    public BeanModelEvent(Event event) {
        this.event = event;
    }

    public String getDocId() {
        return event.getDocId();
    }

    public String getEventId() {
        return event.getEventId();
    }

    public String getFirstArgumentText() {
        return event.getFirstArgument().getText();
    }

    public String getFirstArgumentGeneId() {
        return event.getFirstArgument().getGeneId();
    }

    public String getSecondArgumentText() {
        return event.getSecondArgument().getText();
    }

    public String getSecondArgumentGeneId() {
        return event.getSecondArgument().getGeneId();
    }

    public String getFirstArgumentPreferredName() {
        return event.getFirstArgument().getPreferredName();
    }

    public String getSecondArgumentPreferredName() {
        return event.getSecondArgument().getPreferredName();
    }

    public String getMainEventType() {
        return event.getMainEventType();
    }

    public String getFirstArgumentMatchType() {
        return event.getFirstArgument().getMatchType();
    }

    public String getSecondArgumentMatchType() {
        return event.getSecondArgument().getMatchType();
    }

    public String getGeneMappingSources() {
        return event.getGeneMappingSources().stream().collect(Collectors.joining(", "));
    }

    public String getFactuality() {
        switch (event.getLikelihood()) {
            case 1:
                return "negation";
            case 2:
                return "low";
            case 3:
                return "investigation";
            case 4:
                return "moderate";
            case 5:
                return "high";
            case 6:
                return "assertion";
            default:
                throw new IllegalArgumentException("Illegal likelihood ordinal " + event.getLikelihood());
        }
    }

    public String getFirstArgumentTextWithPreferredName() {
        Argument argument = event.getFirstArgument();
        return argument.getText() + " (" + argument.getPreferredName() + ")";
    }

    public String getAllEventTypes() {
        return String.join(", ", event.getAllEventTypes());
    }

    public String getContext() {
        if (event.isParagraphMatchingFulltextQuery() && !event.isSentenceMatchingFulltextQuery())
            return event.getSentence() + "<br>" + event.getHlParagraph();
        if (event.isSentenceMatchingFulltextQuery())
            return event.getSentence() + "<br>" + event.getHlSentence();
        return event.getSentence();
    }

    public String getSecondArgumentTextWithPreferredName() {
        Argument argument = event.getSecondArgument();
        if (null != argument)
            return argument.getText() + " (" + argument.getPreferredName() + ")";
        return "";
    }

    public String getFulltextMatchSource() {
        if (event.isSentenceMatchingFulltextQuery())
            return "sentence";
        if (event.isParagraphMatchingFulltextQuery())
            return "paragraph";
        return "none";
//        System.out.println(event.getEventId());
//        System.out.println(event.getSentence());
//        System.out.println(event.getHlSentence());
//        System.out.println(event.getParagraph());
//        System.out.println(event.getHlParagraph());
//        throw new IllegalStateException("The full text match source of event " + event + " was requested but neither the sentence nor the paragraph have a match. Either this is not a fulltext query request or there is an result that actually doesn't match the query.");
    }
}
