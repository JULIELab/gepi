package de.julielab.gepi.core.services;

import java.util.List;

import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;
import de.julielab.gepi.core.retrieval.data.GePiData;
import org.apache.tapestry5.json.JSONArray;

import de.julielab.gepi.core.retrieval.data.Event;
import org.apache.tapestry5.json.JSONObject;

public interface IGePiDataService {

    /**
     * <p>Puts data to the data cache. If the data already has a session ID, the respective cache entry will be updated.
     * Otherwise, a new ID will be generated and set to <tt>data</tt>. The final session ID is returned.</p>
     *
     *
     * @param dataSessionId
     * @param data The data of a new session.
     */
    void putData(long dataSessionId, GePiData data);

    /**
     * <p>Generates a new ID for a data session that can be used in {@link #getData(long)}}.</p>
     *
     * @return A new data session ID.
     */
    long newSession();

    /**
     * <p>Returns the data associated with the given session ID.</p>
     *
     * @param sessionId The session ID for which the data should be retrieved.
     * @return The data of the session or {@link GePiData#EMPTY} if no such data exists.
     */
    GePiData getData(long sessionId);

    /**
     * input structure for pie chart and bar chart
     *
     * @return JSONArray - json array of tuples (itself realised as an json array)
     */
    JSONArray getTargetArgCount(List<Event> e);

    /**
     * input structure required for sankey graph
     *
     * @return JSONArray - array of triplets ([<from, <to>, count])
     */
    JSONObject getPairedArgsCount(List<Event> e);

    JSONObject getPairedArgsCount(AggregatedEventsRetrievalResult aggregatedEvents);

    JSONObject getPairsWithCommonTarget(List<Event> evtList);

    JSONArray convertToJson(List<Event> eventList);
}
