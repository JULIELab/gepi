package de.julielab.gepi.core.retrieval.data;

import java.lang.ref.WeakReference;
import java.util.concurrent.Future;

/**
 * A single class to hold the input and result data of a GePi request.
 */
public class GePiData {
    public static final GePiData EMPTY = new GePiData();
    private long sessionId;
    private Future<Neo4jAggregatedEventsRetrievalResult> neo4jAggregatedResult;
    private Future<EventRetrievalResult> unrolledResult4charts;
    private WeakReference<Future<EventRetrievalResult>> unrolledResult4download;
    private Future<EsAggregatedResult> esAggregatesResult;
    private Future<EventRetrievalResult> pagedResult;
    private Future<IdConversionResult> listAIdConversionResult;
    private Future<IdConversionResult> listBIdConversionResult;

    public Future<EsAggregatedResult> getEsAggregatedResult() {
        return esAggregatesResult;
    }

    public GePiData(Future<Neo4jAggregatedEventsRetrievalResult> aggregatedResult, Future<EventRetrievalResult> unrolledResult4charts, Future<EsAggregatedResult> esAggregatesResult, Future<EventRetrievalResult> pagedResult, Future<IdConversionResult> listAIdConversionResult, Future<IdConversionResult> listBIdConversionResult) {
        this.neo4jAggregatedResult = aggregatedResult;
        this.unrolledResult4charts = unrolledResult4charts;
        this.esAggregatesResult = esAggregatesResult;
        this.pagedResult = pagedResult;
        this.listAIdConversionResult = listAIdConversionResult;
        this.listBIdConversionResult = listBIdConversionResult;
        unrolledResult4download = new WeakReference<>(null);
    }
    private GePiData() {
        // for the EMPTY constant
    }

    public boolean isAnyResultAvailable() {
        if (neo4jAggregatedResult != null && neo4jAggregatedResult.isDone())
            return true;
        if (esAggregatesResult != null && esAggregatesResult.isDone())
            return true;
        if (pagedResult != null && pagedResult.isDone())
            return true;
        return false;
    }
    public WeakReference<Future<EventRetrievalResult>> getUnrolledResult4download() {
        return unrolledResult4download;
    }

    public void setUnrolledResult4download(WeakReference<Future<EventRetrievalResult>> unrolledResult4download) {
        this.unrolledResult4download = unrolledResult4download;
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

    public Future<Neo4jAggregatedEventsRetrievalResult> getNeo4jAggregatedResult() {
        return neo4jAggregatedResult;
    }

    public Future<EventRetrievalResult> getUnrolledResult4charts() {
        return unrolledResult4charts;
    }

    public Future<IdConversionResult> getListAIdConversionResult() {
        return listAIdConversionResult;
    }

    public Future<IdConversionResult> getListBIdConversionResult() {
        return listBIdConversionResult;
    }
}
