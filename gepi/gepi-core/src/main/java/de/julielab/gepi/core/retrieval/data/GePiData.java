package de.julielab.gepi.core.retrieval.data;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * A single class to hold the input and result data of a GePi request.
 */
public class GePiData {
    public static final GePiData EMPTY = new GePiData();
    private long sessionId;
    private Future<AggregatedEventsRetrievalResult> aggregatedResult;
    private Future<EventRetrievalResult> unrolledResult;
    private Future<EventRetrievalResult> pagedResult;
    private Future<IdConversionResult> listAIdConversionResult;
    private Future<IdConversionResult> listBIdConversionResult;
    public GePiData(Future<AggregatedEventsRetrievalResult> aggregatedResult, Future<EventRetrievalResult> unrolledResult, Future<EventRetrievalResult> pagedResult, Future<IdConversionResult> listAIdConversionResult, Future<IdConversionResult> listBIdConversionResult) {
        this.aggregatedResult = aggregatedResult;
        this.unrolledResult = unrolledResult;
        this.pagedResult = pagedResult;
        this.listAIdConversionResult = listAIdConversionResult;
        this.listBIdConversionResult = listBIdConversionResult;
    }

    private GePiData() {
        // for the EMPTY constant
    }

    public Future<EventRetrievalResult> getPagedResult() {
        return pagedResult;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public Future<AggregatedEventsRetrievalResult> getAggregatedResult() {
        return aggregatedResult;
    }

    public Future<EventRetrievalResult> getUnrolledResult() {
        return unrolledResult;
    }

    public Future<IdConversionResult> getListAIdConversionResult() {
        return listAIdConversionResult;
    }

    public Future<IdConversionResult> getListBIdConversionResult() {
        return listBIdConversionResult;
    }
}
