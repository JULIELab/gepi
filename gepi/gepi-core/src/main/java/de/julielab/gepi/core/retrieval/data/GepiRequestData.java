package de.julielab.gepi.core.retrieval.data;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class GepiRequestData implements Cloneable {
    private List<String> eventTypes;
    private Future<IdConversionResult> listAGePiIds;
    private Future<IdConversionResult> listBGePiIds;
    private String sentenceFilterString;
    private String paragraphFilterString;
    private String filterFieldsConnectionOperator = "AND";
    private EnumSet<InputMode> inputMode;
    private String docId;
    private long dataSessionId;
    private boolean includeUnary;
    private int eventLikelihood;
    private String[] taxId;
    private String[] taxIdsA;
    private String[] taxIdsB;
    private String sectionNameFilterString;
    private int pageSize = 10;

    public GepiRequestData(List<String> eventTypes, boolean includeUnary, int eventLikelihood, Future<IdConversionResult> listAGePiIds, Future<IdConversionResult> listBGePiIds, String[] taxId, String[] taxIdA, String[] taxIdB, String sentenceFilterString, String paragraphFilterString, String filterFieldsConnectionOperator, String sectionNameFilterString, EnumSet<InputMode> inputMode, String docId, long dataSessionId) {
        this.includeUnary = includeUnary;
        this.eventLikelihood = eventLikelihood;
        this.taxId = taxId;
        this.taxIdsA = taxIdA;
        this.taxIdsB = taxIdB;
        this.sectionNameFilterString = sectionNameFilterString;
        this.eventTypes = eventTypes;
        this.listAGePiIds = listAGePiIds;
        this.listBGePiIds = listBGePiIds;
        this.sentenceFilterString = sentenceFilterString;
        this.paragraphFilterString = paragraphFilterString;
        this.filterFieldsConnectionOperator = filterFieldsConnectionOperator;
        this.inputMode = inputMode;
        this.docId = docId;
        this.dataSessionId = dataSessionId;
    }

    public GepiRequestData() {

    }

    public String getDocId() {
        return docId;
    }

    public boolean isIncludeUnary() {
        return includeUnary;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String[] getTaxId() {
        return taxId;
    }

    public int getEventLikelihood() {
        return eventLikelihood;
    }

    public String getSectionNameFilterString() {
        return sectionNameFilterString;
    }

    public EnumSet<InputMode> getInputMode() {
        return inputMode;
    }

    public GepiRequestData withInputMode(EnumSet<InputMode> inputMode) {
        this.inputMode = inputMode;
        return this;
    }

    public String getFilterFieldsConnectionOperator() {
        return filterFieldsConnectionOperator;
    }

    public GepiRequestData withFilterFieldsConnectionOperator(String filterFieldsConnectionOperator) {
        this.filterFieldsConnectionOperator = filterFieldsConnectionOperator;
        return this;
    }

    public long getDataSessionId() {
        return dataSessionId;
    }

    public GepiRequestData withDataSessionId(long dataSessionId) {
        this.dataSessionId = dataSessionId;
        return this;
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

    public GepiRequestData withTaxId(String... taxIds) {
        this.taxId = taxIds;
        return this;
    }

    public GepiRequestData withIncludeUnary(boolean includeUnary) {
        this.includeUnary = includeUnary;
        return this;
    }

    public GepiRequestData withEventTypes(List<String> eventTypes) {
        this.eventTypes = eventTypes;
        return this;
    }

    public GepiRequestData withEventTypes(String... eventTypes) {
        this.eventTypes = Arrays.asList(eventTypes);
        return this;
    }

    public Future<IdConversionResult> getListAGePiIds() {
        return listAGePiIds;
    }

    public GepiRequestData withListAGePiIds(Future<IdConversionResult> listAGePiIds) {
        this.listAGePiIds = listAGePiIds;
        return this;
    }

    public GepiRequestData withListAGePiIds(IdConversionResult listAGePiIds) {
        this.listAGePiIds = CompletableFuture.supplyAsync(() -> listAGePiIds);
        return this;
    }


    public Future<IdConversionResult> getListBGePiIds() {
        return listBGePiIds;
    }

    public GepiRequestData withListBGePiIds(Future<IdConversionResult> listBGePiIds) {
        this.listBGePiIds = listBGePiIds;
        return this;
    }


    public GepiRequestData withListBGePiIds(IdConversionResult listBGePiIds) {
        this.listBGePiIds = CompletableFuture.supplyAsync(() -> listBGePiIds);
        return this;
    }

    public String getSentenceFilterString() {
        return sentenceFilterString;
    }

    public GepiRequestData withSentenceFilterString(String sentenceFilterString) {
        this.sentenceFilterString = sentenceFilterString;
        return this;
    }

    public String getParagraphFilterString() {
        return paragraphFilterString;
    }

    public GepiRequestData withParagraphFilterString(String paragraphFilterString) {
        this.paragraphFilterString = paragraphFilterString;
        return this;
    }

    public GepiRequestData withSectionNameFilterString(String sectionNameFilterString) {
        this.sectionNameFilterString = sectionNameFilterString;
        return this;
    }

    public GepiRequestData withDocId(String docid) {
        this.docId = docid;
        return this;
    }

    public GepiRequestData withMinimumEventLikelihood(int eventLikelihood) {
        this.eventLikelihood = eventLikelihood;
        return this;
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

    public String[] getTaxIdsB() {
        return taxIdsB;
    }

    public String[] getTaxIdsA() {
        return taxIdsA;
    }
}
