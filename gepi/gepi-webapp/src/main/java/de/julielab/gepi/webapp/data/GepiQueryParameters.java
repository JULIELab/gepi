package de.julielab.gepi.webapp.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.tapestry5.http.services.Request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class GepiQueryParameters {
    public static final String LISTA = "lista";
    public static final String LISTB = "listb";
    public static final String TAXID = "taxid";
    public static final String EVENTTYPES = "eventtypes";
    public static final String FACTUALITY = "factuality";
    public static final String FILTERFIELDSCONNECTIONOPERATOR = "filterfieldsconnectionoperator";
    public static final String SENTENCEFILTER = "sentencefilter";
    public static final String PARAGRAPHFILTER = "paragraphfilter";
    public static final String SECTIONNAMEFILTER = "sectionnamefilter";
    private String listATextAreaValue;
    private String listBTextAreaValue;
    private String taxId;
    private List<EventTypes> selectedEventTypes;
    private int eventLikelihood;
    private String filterFieldsConnectionOperator;
    private String sentenceFilterString;
    private String paragraphFilterString;
    private String sectionNameFilterString;
    private boolean formdata;

    public GepiQueryParameters(Request request) {
        readParameters(request);
    }

    public boolean isValidRequest() {
        // we don't want to intercept requests coming from the input form
        return !formdata && !StringUtils.isBlank(listATextAreaValue) || !StringUtils.isBlank(sentenceFilterString) || !StringUtils.isBlank(paragraphFilterString) || !StringUtils.isBlank(sectionNameFilterString);
    }

    public String getListATextAreaValue() {
        return listATextAreaValue;
    }

    public String getListBTextAreaValue() {
        return listBTextAreaValue;
    }

    public String getTaxId() {
        return taxId;
    }

    public List<EventTypes> getSelectedEventTypes() {
        return selectedEventTypes;
    }

    public int getEventLikelihood() {
        return eventLikelihood;
    }

    public String getFilterFieldsConnectionOperator() {
        return filterFieldsConnectionOperator;
    }

    public String getSentenceFilterString() {
        return sentenceFilterString;
    }

    public String getParagraphFilterString() {
        return paragraphFilterString;
    }

    public String getSectionNameFilterString() {
        return sectionNameFilterString;
    }

    private void readParameters(Request request) {
        listATextAreaValue = request.getParameter(LISTA);
        if (listATextAreaValue != null)
            listATextAreaValue = Arrays.stream(listATextAreaValue.split("[\n,]")).collect(Collectors.joining("\n"));
        listBTextAreaValue = request.getParameter(LISTB);
        if (listBTextAreaValue != null)
            listBTextAreaValue = Arrays.stream(listBTextAreaValue.split("[\n,]")).collect(Collectors.joining("\n"));
        taxId = request.getParameter(TAXID);
        selectedEventTypes = new ArrayList<>(EnumSet.allOf(EventTypes.class));
        final String eventTypesString = request.getParameter(EVENTTYPES);
        if (!StringUtils.isBlank(eventTypesString)) {
            selectedEventTypes.clear();
            final String[] eventTypes = eventTypesString.split(",");
            for (String eventType : eventTypes) {
                for (EventTypes type : EventTypes.values()) {
                    if (eventType.equalsIgnoreCase(type.name()))
                        selectedEventTypes.add(type);
                }
            }
        }
        eventLikelihood = 1;
        try {
            eventLikelihood = Integer.parseInt(request.getParameter(FACTUALITY));
        } catch (NumberFormatException e) {
            // not given or not a number
        }
        filterFieldsConnectionOperator = request.getParameter(FILTERFIELDSCONNECTIONOPERATOR);
        if (filterFieldsConnectionOperator == null)
            filterFieldsConnectionOperator = "AND";
        else
            filterFieldsConnectionOperator = filterFieldsConnectionOperator.toUpperCase();
        sentenceFilterString = request.getParameter(SENTENCEFILTER);
        paragraphFilterString = request.getParameter(PARAGRAPHFILTER);
        sectionNameFilterString = request.getParameter(SECTIONNAMEFILTER);
        formdata = request.getParameter("t:formdata") != null;
    }
}
