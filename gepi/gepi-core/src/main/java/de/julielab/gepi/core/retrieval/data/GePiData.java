package de.julielab.gepi.core.retrieval.data;

import com.google.j2objc.annotations.Weak;

import java.lang.ref.WeakReference;
import java.util.concurrent.Future;

/**
 * A single class to hold the input and result data of a GePi request.
 */
public class GePiData {
    public static final GePiData EMPTY = new GePiData();
    private long sessionId;
    private Future<AggregatedEventsRetrievalResult> aggregatedResult;
    private Future<EventRetrievalResult> unrolledResult4charts;
    private WeakReference<Future<EventRetrievalResult>> unrolledResult4download;
    private Future<EventRetrievalResult> pagedResult;
    private Future<IdConversionResult> listAIdConversionResult;
    private Future<IdConversionResult> listBIdConversionResult;
    public GePiData(Future<AggregatedEventsRetrievalResult> aggregatedResult, Future<EventRetrievalResult> unrolledResult4charts, Future<EventRetrievalResult> pagedResult, Future<IdConversionResult> listAIdConversionResult, Future<IdConversionResult> listBIdConversionResult) {
        this.aggregatedResult = aggregatedResult;
        this.unrolledResult4charts = unrolledResult4charts;
        this.pagedResult = pagedResult;
        this.listAIdConversionResult = listAIdConversionResult;
        this.listBIdConversionResult = listBIdConversionResult;
        unrolledResult4download = new WeakReference<>(null);
    }
    private GePiData() {
        // for the EMPTY constant
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

    public Future<AggregatedEventsRetrievalResult> getAggregatedResult() {
        return aggregatedResult;
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
