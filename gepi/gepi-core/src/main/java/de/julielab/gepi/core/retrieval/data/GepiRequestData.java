package de.julielab.gepi.core.retrieval.data;

import de.julielab.gepi.core.retrieval.data.IdConversionResult;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class GepiRequestData {
    private List<String> eventTypes;
    private Future<IdConversionResult> listAGePiIds;
    private Future<IdConversionResult> listBGePiIds;
    private String sentenceFilterString;
    private String paragraphFilterString;
    private String filterFieldsConnectionOperator;
    private EnumSet<InputMode> inputMode;
    private long dataSessionId;

    public GepiRequestData(List<String> eventTypes, Future<IdConversionResult> listAGePiIds, Future<IdConversionResult> listBGePiIds, String sentenceFilterString, String paragraphFilterString, String filterFieldsConnectionOperator, EnumSet<InputMode> inputMode, long dataSessionId) {
        this.eventTypes = eventTypes;
        this.listAGePiIds = listAGePiIds;
        this.listBGePiIds = listBGePiIds;
        this.sentenceFilterString = sentenceFilterString;
        this.paragraphFilterString = paragraphFilterString;
        this.filterFieldsConnectionOperator = filterFieldsConnectionOperator;
        this.inputMode = inputMode;
        this.dataSessionId = dataSessionId;
    }

    public EnumSet<InputMode> getInputMode() {
        return inputMode;
    }

    public void setInputMode(EnumSet<InputMode> inputMode) {
        this.inputMode = inputMode;
    }

    public String getFilterFieldsConnectionOperator() {
        return filterFieldsConnectionOperator;
    }

    public void setFilterFieldsConnectionOperator(String filterFieldsConnectionOperator) {
        this.filterFieldsConnectionOperator = filterFieldsConnectionOperator;
    }

    public long getDataSessionId() {
        return dataSessionId;
    }

    public void setDataSessionId(long dataSessionId) {
        this.dataSessionId = dataSessionId;
    }

    public Set<String> getAListIdsAsSet() throws ExecutionException, InterruptedException {
        return listAGePiIds.get().getConvertedItems().values().stream().collect(Collectors.toSet());
    }

    public Set<String> getBListIdsAsSet() throws ExecutionException, InterruptedException {
        return listBGePiIds.get().getConvertedItems().values().stream().collect(Collectors.toSet());
    }

    public List<String> getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(List<String> eventTypes) {
        this.eventTypes = eventTypes;
    }

    public Future<IdConversionResult> getListAGePiIds() {
        return listAGePiIds;
    }

    public void setListAGePiIds(Future<IdConversionResult> listAGePiIds) {
        this.listAGePiIds = listAGePiIds;
    }

    public Future<IdConversionResult> getListBGePiIds() {
        return listBGePiIds;
    }

    public void setListBGePiIds(Future<IdConversionResult> listBGePiIds) {
        this.listBGePiIds = listBGePiIds;
    }

    public String getSentenceFilterString() {
        return sentenceFilterString;
    }

    public void setSentenceFilterString(String sentenceFilterString) {
        this.sentenceFilterString = sentenceFilterString;
    }

    public String getParagraphFilterString() {
        return paragraphFilterString;
    }

    public void setParagraphFilterString(String paragraphFilterString) {
        this.paragraphFilterString = paragraphFilterString;
    }

    @Override
    public String toString() {
        return "GepiRequestData{" +
                "eventTypes=" + eventTypes +
                ", listAGePiIds=" + listAGePiIds +
                ", listBGePiIds=" + listBGePiIds +
                ", sentenceFilterString='" + sentenceFilterString + '\'' +
                ", paragraphFilterString='" + paragraphFilterString + '\'' +
                ", filterFieldsConnectionOperator='" + filterFieldsConnectionOperator + '\'' +
                ", inputMode=" + inputMode +
                ", dataSessionId=" + dataSessionId +
                '}';
    }
}
