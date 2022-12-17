package de.julielab.gepi.webapp;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import org.apache.poi.wp.usermodel.Paragraph;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BeanModelEvent {

    private Event event;

    public BeanModelEvent(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
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
        return event.getArity() > 1 ? event.getSecondArgument().getText() : "";
    }

    public String getSecondArgumentGeneId() {
        return event.getArity() > 1 ? event.getSecondArgument().getGeneId() : "";
    }

    public String getFirstArgumentPreferredName() {
        return event.getFirstArgument().getPreferredName();
    }

    public String getSecondArgumentPreferredName() {
        return event.getArity() > 1 ? event.getSecondArgument().getPreferredName() : "";
    }

    public String getMainEventType() {
        return event.getMainEventType();
    }

    public String getGeneMappingSources() {
        return event.getGeneMappingSources().stream().collect(Collectors.joining(", "));
    }

    public String getFactuality() {
        String class1;
        String class2;
        String class3;
        String likelihood;
        switch (event.getLikelihood()) {
            case 1:
                class1 = "star-empty";
                class2 = "star-empty";
                class3 = "star-empty";
                likelihood = "negation";
                break;
            case 2:
                class1 = "star-half";
                class2 = "star-empty";
                class3 = "star-empty";
                likelihood = "low";
                break;
            case 3:
                class1 = "star-full";
                class2 = "star-empty";
                class3 = "star-empty";
                likelihood = "investigation";
                break;
            case 4:
                class1 = "star-full";
                class2 = "star-half";
                class3 = "star-empty";
                likelihood = "moderate";
                break;
            case 5:
                class1 = "star-full";
                class2 = "star-full";
                class3 = "star-half";
                likelihood = "high";
                break;
            case 6:
                class1 = "star-full";
                class2 = "star-full";
                class3 = "star-full";
                likelihood = "assertion";
                break;
            default:
                throw new IllegalArgumentException("Illegal likelihood ordinal " + event.getLikelihood());
        }
        return "<div title=\"" + likelihood + "\" data-bs-toggle=\"default-tooltip\" class=\"text-center\"><span class=\"symbol-background " + class1 + "\">&nbsp;</span><span class=\"symbol-background " + class2 + "\">&nbsp;</span><span class=\"symbol-background " + class3 + "\">&nbsp;</span></div>";
    }

    public String getFirstArgumentTextWithPreferredName() {
        Argument argument = event.getFirstArgument();
        return argument.getText() + " (" + argument.getPreferredName() + ")";
    }

    public String getAllEventTypes() {
        return String.join(", ", event.getAllEventTypes());
    }

    public String getContext() {
        if (!event.isParagraphMatchingFulltextQuery())
            return "<div class=\"ms-4\"><span class=\"info-interaction-sentence\" title=\"sentence of occurrence\" data-bs-toggle=\"default-tooltip\">&nbsp;</span>" + event.getHlSentence() + "</div>";
        StringBuilder paragraphHighlight4display = new StringBuilder();
        final String hlParagraph = event.getHlParagraph().trim();
        final String hlParagraphWoTags = hlParagraph.replaceAll("<[^>]+>", "");
        if (!Character.isUpperCase(hlParagraph.charAt(0)))
            paragraphHighlight4display.append("...");
        paragraphHighlight4display.append(hlParagraph);
        if (!Pattern.matches("\\p{Punct}", String.valueOf(hlParagraphWoTags.charAt(hlParagraphWoTags.length() - 1))))
            paragraphHighlight4display.append("...");
        return "<div class=\"ms-4\"><div><span class=\"info-interaction-sentence\" title=\"sentence of occurrence\" data-bs-toggle=\"default-tooltip\">&nbsp;</span>" + event.getHlSentence() + "</div><div><span class=\"info-paragraph-match\" title=\"paragraph-level full text filter match\" data-bs-toggle=\"default-tooltip\">&nbsp;</span>" + paragraphHighlight4display + "</div></div>";
    }

    public String getSecondArgumentTextWithPreferredName() {
        Argument argument = event.getSecondArgument();
        if (null != argument && event.getArity() > 1)
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
