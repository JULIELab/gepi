package de.julielab.gepi.webapp.data;

import de.julielab.gepi.core.retrieval.data.GepiRequestData;

public class FilteredGepiRequestData extends GepiRequestData {

    private EventTypes eventTypeFilter;
    private String arg1SymbolFilter;
    private String arg1TextFilter;
    private String arg1IdFilter;
    private String arg2SymbolFilter;
    private String arg2TextFilter;

    public EventTypes getEventTypeFilter() {
        return eventTypeFilter;
    }

    public void setEventTypeFilter(EventTypes eventTypeFilter) {
        this.eventTypeFilter = eventTypeFilter;
    }

    public String getArg1SymbolFilter() {
        return arg1SymbolFilter;
    }

    public void setArg1SymbolFilter(String arg1SymbolFilter) {
        this.arg1SymbolFilter = arg1SymbolFilter;
    }

    public String getArg1TextFilter() {
        return arg1TextFilter;
    }

    public void setArg1TextFilter(String arg1TextFilter) {
        this.arg1TextFilter = arg1TextFilter;
    }

    public String getArg1IdFilter() {
        return arg1IdFilter;
    }

    public void setArg1IdFilter(String arg1IdFilter) {
        this.arg1IdFilter = arg1IdFilter;
    }

    public String getArg2SymbolFilter() {
        return arg2SymbolFilter;
    }

    public void setArg2SymbolFilter(String arg2SymbolFilter) {
        this.arg2SymbolFilter = arg2SymbolFilter;
    }

    public String getArg2TextFilter() {
        return arg2TextFilter;
    }

    public void setArg2TextFilter(String arg2TextFilter) {
        this.arg2TextFilter = arg2TextFilter;
    }

    public String getArg2IdFilter() {
        return arg2IdFilter;
    }

    public void setArg2IdFilter(String arg2IdFilter) {
        this.arg2IdFilter = arg2IdFilter;
    }

    private String arg2IdFilter;

    public FilteredGepiRequestData(GepiRequestData requestData) {
        super(requestData.getEventTypes(), requestData.getEventLikelihood(), requestData.getListAGePiIds(),requestData.getListBGePiIds(), requestData.getTaxId(), requestData.getSentenceFilterString(), requestData.getParagraphFilterString(), requestData.getFilterFieldsConnectionOperator(), requestData.getSectionNameFilterString(), requestData.getInputMode(), requestData.getDataSessionId());
    }

}
