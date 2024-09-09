package de.julielab.gepi.webapp.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.tapestry5.http.services.Request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static de.julielab.gepi.webapp.components.GepiInput.INTERACTION_RETRIEVAL_LIMIT_FOR_AGGREGATIONS;

public class GepiQueryParameters {
    public static final String LISTA = "lista";
    public static final String LISTB = "listb";
    public static final String ALIST = "alist";
    public static final String BLIST = "blist";
    public static final String TAXID = "taxids";
    public static final String TAXIDA = "taxidsA";
    public static final String TAXIDB = "taxidsB";
    public static final String EVENTTYPES = "eventtypes";
    public static final String FACTUALITY = "factuality";
    public static final String FILTERFIELDSCONNECTIONOPERATOR = "filterconnector";
    public static final String SENTENCEFILTER = "sentencefilter";
    public static final String PARAGRAPHFILTER = "paragraphfilter";
    public static final String SECTIONNAMEFILTER = "sectionnamefilter";
    public static final String INCLUDE_UNARY = "includeunary";
    public static final String DOCID = "docid";
    public static final String INTERACTION_RETRIEVAL_LIMIT = "limit";
    /**
     * "web" for a GePI HTML page to be rendered in the browser,
     * "excel" for the Excel sheet download,
     * "tsv" for the pure result table in TSV format
     */
    public static final String FORMAT = "format";
    private String listATextAreaValue;
    private String listBTextAreaValue;
    private String taxId;
    private String taxIdA;
    private String taxIdB;
    private List<EventTypes> selectedEventTypes;
    private int eventLikelihood;
    private String filterFieldsConnectionOperator;
    private String sentenceFilterString;
    private String paragraphFilterString;
    private String sectionNameFilterString;
    private String docid;
    private boolean includeUnary;
    private boolean formdata;
    private int interactionRetrievalLimitForAggregations;
    private String format;

    public GepiQueryParameters(Request request) {
        readParameters(request);
    }

    public boolean isValidRequest() {
        // we don't want to intercept requests coming from the input form
        return !formdata && (!StringUtils.isBlank(listATextAreaValue) || !StringUtils.isBlank(sentenceFilterString) || !StringUtils.isBlank(paragraphFilterString) || !StringUtils.isBlank(sectionNameFilterString) || !StringUtils.isBlank(docid));
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

    public String getDocid() {
        return docid;
    }

    public void setDocid(String docid) {
        this.docid = docid;
    }

    public boolean isIncludeUnary() {
        return includeUnary;
    }

    public String getFormat() {
        return format;
    }

    private void readParameters(Request request) {
        formdata = request.getParameter("t:formdata") != null;
        // If the request comes from the input form, this GepiQueryParameters query is invalid anyway.
        // We only use it for rest-like queries entered into the browser address bar directly
        if (!formdata) {
            listATextAreaValue = request.getParameter(LISTA);
            if (listATextAreaValue == null)
                listATextAreaValue = request.getParameter(ALIST);
            if (listATextAreaValue != null)
                listATextAreaValue = Arrays.stream(listATextAreaValue.split("[\n,]")).map(this::decodeUrlEncoding).collect(Collectors.joining("\n"));
            listBTextAreaValue = request.getParameter(LISTB);
            if (listBTextAreaValue == null)
                listBTextAreaValue = request.getParameter(BLIST);
            if (listBTextAreaValue != null)
                listBTextAreaValue = Arrays.stream(listBTextAreaValue.split("[\n,]")).map(this::decodeUrlEncoding).collect(Collectors.joining("\n"));
            taxId = request.getParameter(TAXID);
            taxIdA = request.getParameter(TAXIDA);
            taxIdB = request.getParameter(TAXIDB);
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
            if (sentenceFilterString != null)
                sentenceFilterString = decodeUrlEncoding(sentenceFilterString);
            paragraphFilterString = request.getParameter(PARAGRAPHFILTER);
            if (paragraphFilterString != null)
                paragraphFilterString = decodeUrlEncoding(paragraphFilterString);
            sectionNameFilterString = request.getParameter(SECTIONNAMEFILTER);
            if (sectionNameFilterString != null)
                sectionNameFilterString = decodeUrlEncoding(sectionNameFilterString);
            docid = request.getParameter(DOCID);
            includeUnary = Boolean.parseBoolean(request.getParameter(INCLUDE_UNARY));
            interactionRetrievalLimitForAggregations = INTERACTION_RETRIEVAL_LIMIT_FOR_AGGREGATIONS;
            format = request.getParameter(FORMAT) != null ? request.getParameter(FORMAT).toLowerCase() : null;
            if (format == null)
                format = "web";
            try {
                interactionRetrievalLimitForAggregations = Integer.parseInt(request.getParameter(INTERACTION_RETRIEVAL_LIMIT));
            } catch (NumberFormatException e) {
                // no number given
            }
        }
    }

    private String decodeUrlEncoding(String encodedString) {
        return encodedString.replaceAll("\\$002520", " ")
                .replaceAll("\\$00253[Aa]", ":")
                .replaceAll("\\$00252[Cc]", ",")
                .replaceAll("\\$002522", "\"")
                .replaceAll("\\$007[Cc]", "|")
                .replaceAll("\\$002[Bb]","+")
                .replaceAll("\\$00250[Aa]", "\n");
    }

    public int getInteractionRetrievalLimitForAggregations() {
        return interactionRetrievalLimitForAggregations;
    }

    public void setInteractionRetrievalLimitForAggregations(int interactionRetrievalLimitForAggregations) {
        this.interactionRetrievalLimitForAggregations = interactionRetrievalLimitForAggregations;
    }

    public String getTaxIdA() {
        return taxIdA;
    }

    public String getTaxIdB() {
        return taxIdB;
    }
}
