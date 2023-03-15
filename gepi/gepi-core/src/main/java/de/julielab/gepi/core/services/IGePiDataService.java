package de.julielab.gepi.core.services;

import de.julielab.gepi.core.retrieval.data.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
    JSONArray getArgumentCount(List<Event> e, int argumentPosition);
    JSONArray getArgumentCount(Map<String, Integer> aSymbolFrequencies);

    /**
     * input structure required for sankey graph
     *
     * @return JSONArray - array of triplets ([<from, <to>, count])
     */
    JSONObject getPairedArgsCount(List<Event> e);

    JSONObject getPairedArgsCount(Neo4jAggregatedEventsRetrievalResult aggregatedEvents);

    JSONObject getPairedArgsCount(Map<Event, Integer> eventFrequencies);

    JSONObject getPairedArgsCountFromPairs(List<Pair<Event, Integer>> eventFrequencies);

    JSONObject getPairsWithCommonTarget(List<Event> evtList);

    JSONArray convertToJson(List<Event> eventList);

    /**
     * <p>Creates the result format that was delivered to our partners in the past.</p>
     * <p>This is an Excel workbook with multiple sheets for some explanation and statistics about source
     * genes, target genes and events.</p>
     * <p>To do this, the event data is written to a temporary file, a Python-Pandas script is applied and the
     * resulting Excel file is then read back in the form of the InputStream.</p>
     * @param events The events to create the result workbook for.
     * @param inputMode
     * @param sentenceFilterString
     * @param paragraphFilterString
     * @param sectionNameFilterString
     * @return An InputStream of the created Excel file.
     */
    Path getOverviewExcel(Future<EventRetrievalResult> events, long dataSessionId, EnumSet<InputMode> inputMode, String sentenceFilterString, String paragraphFilterString, String sectionNameFilterString) throws IOException, ExecutionException, InterruptedException;

    String getDownloadFileCreationStatus(long dataSessionId) throws IOException;

    boolean existsTempStatusFile(long dataSessionId) throws IOException;

    boolean isDownloadExcelFileReady(long dataSessionId) throws IOException;

    Path getTempXlsDataFile(long dataSessionId);

}