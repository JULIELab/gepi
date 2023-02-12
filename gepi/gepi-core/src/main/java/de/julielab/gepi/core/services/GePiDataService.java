package de.julielab.gepi.core.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.julielab.gepi.core.GepiCoreSymbolConstants;
import de.julielab.gepi.core.retrieval.data.*;
import de.julielab.gepi.core.retrieval.data.Argument.ComparisonMode;
import de.julielab.java.utilities.IOStreamUtilities;
import org.apache.commons.collections.CollectionUtils;
import org.apache.tapestry5.annotations.Log;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GePiDataService implements IGePiDataService {

    public static final String SOURCE = "source";
    public static final String TARGET = "target";
    public static final String GEPI_TMP_DIR_NAME = "gepi";
    public static final String GEPI_EXCEL_FILE_PREFIX_NAME = "gepi-excel-";
    public static final String GEPI_EXCEL_STATUS_FILE_PREFIX = "gepi-excel-status-";
    public static final String EXCEL_FILE_SUCCESS_STATE = "GePI interactions have been successfully saved in Excel format.";
    protected static final HashMap<Integer, String> likelihoodNames;
    private static final Logger log = LoggerFactory.getLogger(GePiDataService.class);

    static {
        likelihoodNames = new HashMap<>();
        likelihoodNames.put(1, "negation");
        likelihoodNames.put(2, "low");
        likelihoodNames.put(3, "investigation");
        likelihoodNames.put(4, "moderate");
        likelihoodNames.put(5, "high");
        likelihoodNames.put(6, "assertion");
    }

    private final Path gepiTmpDir;
    private Cache<Long, GePiData> dataCache;
    private String excelFilePrefix;
    /**
     * This string is the scripts itself, not a reference to a file.
     */
    private String excelResultCreationScript;

    public GePiDataService(@Symbol(GepiCoreSymbolConstants.GEPI_TMP_DIR) String gepiTmpDir, @Symbol(GepiCoreSymbolConstants.GEPI_EXCEL_FILE_PREFIX) String excelFilePrefix) throws IOException {
        this.gepiTmpDir = Path.of(gepiTmpDir);
        this.excelFilePrefix = excelFilePrefix;
        // We use weak values. So when a user session is evicted,
        // its GePi data can also be removed as soon as possible.
        dataCache = CacheBuilder.newBuilder().weakValues().build();
        excelResultCreationScript = IOStreamUtilities.getStringFromInputStream(GePiDataService.class.getResourceAsStream("/ExcelResultCreation.py"));
    }

    @Log
    @Override
    public void putData(long dataSessionId, GePiData data) {
        data.setSessionId(dataSessionId);
        dataCache.put(dataSessionId, data);
    }

    @Override
    public long newSession() {
        long id;
        synchronized (dataCache) {
            do {
                id = System.currentTimeMillis();
            } while (dataCache.getIfPresent(id) != null);
        }
        return id;
    }

    @Log
    @Override
    public GePiData getData(long sessionId) {
        GePiData data = dataCache.getIfPresent(sessionId);
        log.trace("Data for dataSessionId {} was {}.", sessionId, data != null ? "found" : "not found");
        return data != null ? data : GePiData.EMPTY;
    }

    /**
     * sets json formatted input list for google charts that accept one entry
     * name + number (here target event gene + count of occurrences)
     * singleArgCountJson is array of arrays with [<gene name><count>]
     */
    @SuppressWarnings("unchecked")
    @Override
    public JSONArray getArgumentCount(List<Event> evtList, int argumentPosition) {
        List<Argument> arguments = new ArrayList<>();
        // get those arguments that were not part of the input
        evtList.forEach(e -> {
            Argument a = e.getArgument(argumentPosition);
            a.setComparisonMode(ComparisonMode.TOP_HOMOLOGY_PREFERRED_NAME);
            arguments.add(a);
        });

        // get the counts of how often event arguments appear
        Map<Argument, Integer> singleArgCount = CollectionUtils.getCardinalityMap(arguments);

        // sort entries
        singleArgCount = singleArgCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // put to json format
        JSONArray singleArgCountJson = new JSONArray();

        singleArgCount.forEach((k, v) -> {
            JSONArray tmp = new JSONArray();
            tmp.add(k.getTopHomologyPreferredName());
            tmp.add(v);
            singleArgCountJson.put(tmp);
        });
        return singleArgCountJson;
    }

    /**
     * <p>Converts a map of argument symbols and their frequencies into JSON format for chats.</p>
     * <p>The key is the top homology name in GePI.</p>
     * @param symbolFrequencies The number of occurrences in the event result for A- or B symbols.
     * @return A JSON representation of the input counts.
     */
    @Override
    public JSONArray getArgumentCount(Map<String, Integer> symbolFrequencies) {
        // put to json format
        JSONArray singleArgCountJson = new JSONArray();

        for (String argumentName : symbolFrequencies.keySet()) {
            final Integer count = symbolFrequencies.get(argumentName);
            final JSONArray nameCountPair = new JSONArray(argumentName, count);
            singleArgCountJson.add(nameCountPair);
        }

        return singleArgCountJson;
    }

    /**
     * sets json formated input list for google charts that accepts an entry
     * pair + number (here gene pair (from + to) + count of occurrences)
     * singleArgCountJson is array of arrays with [<gene name 1><gene name
     * 2><count>]
     */
    @SuppressWarnings("unchecked")
    @Override
    public JSONObject getPairedArgsCount(List<Event> evtList) {

        log.trace("Number of events for pair counting: {}", evtList.size());


        // get the count for how often pairs appear
        Map<Event, Integer> pairedArgCount = CollectionUtils.getCardinalityMap(evtList.stream().filter(e -> e.getArity() > 1).collect(Collectors.toList()));

        // convert the counts to JSON
        return getPairedArgsCount(pairedArgCount);
    }

    @Override
    public JSONObject getPairedArgsCount(Neo4jAggregatedEventsRetrievalResult aggregatedEvents) {
        JSONArray nodes = new JSONArray();
        JSONArray links = new JSONArray();

        log.debug("Converting aggregated event retrieveal result of size {} to JSON nodes and links.", aggregatedEvents.size());
        aggregatedEvents.rewind();
        Set<String> nodeIdAlreadySeen = new HashSet<>();
        while (aggregatedEvents.increment()) {
            JSONObject arg1 = new JSONObject("id", aggregatedEvents.getArg1Id(), "name", aggregatedEvents.getArg1Name());
            JSONObject arg2 = new JSONObject("id", aggregatedEvents.getArg2Id(), "name", aggregatedEvents.getArg2Name());
            JSONObject link = new JSONObject("source", aggregatedEvents.getArg1Id(), "target", aggregatedEvents.getArg2Id(), "frequency", aggregatedEvents.getCount(), "type", "interaction");
            if (nodeIdAlreadySeen.add(aggregatedEvents.getArg1Id()))
                nodes.put(arg1);
            if (nodeIdAlreadySeen.add(aggregatedEvents.getArg2Id()))
                nodes.put(arg2);
            links.put(link);
        }

        JSONObject nodesNLinks = new JSONObject();
        nodesNLinks.put("nodes", nodes);
        nodesNLinks.put("links", links);
        return nodesNLinks;
    }

    /**
     * <p>Returns nodes and links for the Sankey diagrams based on aggregated events counts.</p>
     * <p>The top homology preferred name is used for the node names.</p>
     * @param eventFrequencies A map assigning frequency counts to unique events
     * @return A JSON representation of nodes (arguments) and the frequency of events between the nodes (links).
     */
    @Override
    public JSONObject getPairedArgsCount(Map<Event, Integer> eventFrequencies) {
        JSONArray nodes = new JSONArray();
        JSONArray links = new JSONArray();
        JSONObject nodesNLinks = new JSONObject();
        Set<String> nodeIdAlreadySeen = new HashSet<>();
        for (Event e : eventFrequencies.keySet()) {
            final Integer count = eventFrequencies.get(e);
            JSONObject link = new JSONObject();
            link.put("source", e.getFirstArgument().getTopHomologyPreferredName());
            link.put("target", e.getSecondArgument().getTopHomologyPreferredName());
            link.put("frequency", count);


            links.add(link);

            if (nodeIdAlreadySeen.add(e.getFirstArgument().getTopHomologyPreferredName())) {
                nodes.add(getJsonObjectForArgument(e.getFirstArgument()));
            }
            if (nodeIdAlreadySeen.add(e.getSecondArgument().getTopHomologyPreferredName())) {
                nodes.add(getJsonObjectForArgument(e.getSecondArgument()));
            }
        }
        nodesNLinks.put("nodes", nodes);
        nodesNLinks.put("links", links);
        return nodesNLinks;
    }

    /**
     * sets json formated input list for google charts that accepts an entry
     * pair + number (here gene pair (from + to) + count of occurrences)
     * singleArgCountJson is array of arrays with [<gene name 1><gene name
     * 2><count>]
     */
    @SuppressWarnings("unchecked")
    @Override
    public JSONObject getPairsWithCommonTarget(List<Event> evtList) {
        // First, we build a structure that counts how often each target gene (genes on the - possibly implicit - B list)
        // interacts how often with which source (A list) gene. We build a map whose keys are the targets and the values
        // are maps counting for each source gene the number of interactions with the target.
        final Map<Argument, Map<Event, Integer>> target2EventCardinalities = evtList.stream().collect(Collectors.groupingBy(e -> e.getArgument(1),
                Collectors.collectingAndThen(Collectors.toList(), CollectionUtils::getCardinalityMap)));
        // The general harmonic mean formula for x1,...,xn: n / ( 1/x1 + ... + 1/xn)
        Function<Map<Event, Integer>, Double> harmonicMean = eventCounts -> eventCounts.size() / (eventCounts.values().stream().map(c -> 1d / c).reduce(1d, (sum, c) -> sum + c));
        final LinkedHashMap<Argument, Map<Event, Integer>> orderedMap = target2EventCardinalities.entrySet().stream().sorted((e1, e2) -> (int) Math.signum(harmonicMean.apply(e2.getValue()) - harmonicMean.apply(e1.getValue()))).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (k, v) -> k, LinkedHashMap::new));

        // put to json
        JSONArray nodes = new JSONArray();
        JSONArray links = new JSONArray();
        Set<String> nodeIdAlreadySeen = new HashSet<>();
        orderedMap.values().stream().forEachOrdered(map -> map.forEach((k, v) -> {
            JSONObject link = new JSONObject();
            link.put("source", k.getFirstArgument().getTopHomologyId());
            link.put("target", k.getSecondArgument().getTopHomologyId());
            link.put("frequency", v);
            link.put("type", k.getMainEventType());

            links.put(link);

            if (nodeIdAlreadySeen.add(k.getFirstArgument().getTopHomologyId())) {
                nodes.put(getJsonObjectForArgument(k.getFirstArgument()));
            }
            if (nodeIdAlreadySeen.add(k.getSecondArgument().getTopHomologyId())) {
                nodes.put(getJsonObjectForArgument(k.getSecondArgument()));
            }
        }));

        JSONObject nodesNLinks = new JSONObject();
        nodesNLinks.put("nodes", nodes);
        nodesNLinks.put("links", links);
        return nodesNLinks;
    }

    @Override
    public JSONArray convertToJson(List<Event> eventList) {
        JSONArray array = new JSONArray();
        for (Event event : eventList) {
            final Argument firstArgument = event.getFirstArgument();
            JSONObject source = getJsonObjectForArgument(firstArgument);

            final Argument secondArgument = event.getSecondArgument();
            JSONObject target = getJsonObjectForArgument(secondArgument);


            JSONObject o = new JSONObject();
            o.put("source", source);
            o.put("target", target);
            o.put("type", event.getMainEventType());

            array.put(o);
        }
        return array;
    }

    @Override
    public Path getOverviewExcel(Future<EventRetrievalResult> eventRetrievalResult, long dataSessionId, EnumSet<InputMode> inputMode, String sentenceFilterString, String paragraphFilterString, String sectionNameFilterString) throws IOException, ExecutionException, InterruptedException {
        long time = System.currentTimeMillis();
        log.info("Creating event statistics Excel file for dataSessionId {}", dataSessionId);
        final Path tempStatusFile = getTempStatusFile(dataSessionId);
        if (!Files.exists(tempStatusFile)) {
            if (!Files.exists(tempStatusFile.getParent()))
                Files.createDirectories(tempStatusFile.getParent());
            Files.createFile(tempStatusFile);
        }
        updateDownloadFileCreationsStatus("Step 1 of 3: Retrieving and storing all interactions for Excel sheet creation.", dataSessionId);
        Path tsvFile = getTempTsvDataFile(dataSessionId);
        Path xlsFile = getTempXlsDataFile(dataSessionId);
        writeOverviewTsvFile(eventRetrievalResult.get().getEventList(), tsvFile);
        updateDownloadFileCreationsStatus("Step 2 of 3: Retrieval of all interactions has finished. Creating Excel file.", dataSessionId);
        createExcelSummaryFile(tsvFile, xlsFile, inputMode, sentenceFilterString, paragraphFilterString, sectionNameFilterString);
        updateDownloadFileCreationsStatus(EXCEL_FILE_SUCCESS_STATE + " The file is ready for download.", dataSessionId);
        time = System.currentTimeMillis() - time;
        log.info("Excel sheet creation took {} seconds", time / 1000);
        return xlsFile;
    }

    @Override
    public String getDownloadFileCreationStatus(long dataSessionId) throws IOException {
        final Path tempStatusFile = getTempStatusFile(dataSessionId);
        final String status = Files.readString(tempStatusFile);
        return status;
    }

    @Override
    public boolean existsTempStatusFile(long dataSessionId) throws IOException {
        return Files.exists(getTempStatusFile(dataSessionId));
    }

    @Override
    public boolean isDownloadExcelFileReady(long dataSessionId) throws IOException {
        final String status = getDownloadFileCreationStatus(dataSessionId);
        return status.contains(EXCEL_FILE_SUCCESS_STATE);
    }

    private void updateDownloadFileCreationsStatus(String status, long dataSessionId) throws IOException {
        final Path tempStatusFile = getTempStatusFile(dataSessionId);
        Files.writeString(tempStatusFile, status);
    }

    private void createExcelSummaryFile(Path tsvFile, Path xlsFile, EnumSet<InputMode> inputMode, String sentenceFilterString, String paragraphFilterString, String sectionNameFilterString) throws IOException {
        ProcessBuilder builder = new ProcessBuilder().command("python3", "-c", excelResultCreationScript, tsvFile.toAbsolutePath().toString(), xlsFile.toAbsolutePath().toString(), inputMode.stream().map(InputMode::name).collect(Collectors.joining(" ")), sentenceFilterString != null ? sentenceFilterString : "<none>", paragraphFilterString != null ? paragraphFilterString : "<none>", sectionNameFilterString != null ? sectionNameFilterString : "<none>");
        log.info("xls builder command: {}", builder.command());
        Process process = builder.start();
        InputStream processInput = process.getInputStream();
        InputStream processErrors = process.getErrorStream();
        log.debug("Event to Excel conversion script output: {}", IOStreamUtilities.getStringFromInputStream(processInput));
        List<String> errorLines = IOStreamUtilities.getLinesFromInputStream(processErrors);
        if (!errorLines.isEmpty()) {
            log.error("Error occurred when trying to create Excel output: {}", String.join(System.getProperty("line.separator"), errorLines));
            if (Files.exists(xlsFile))
                Files.delete(xlsFile);
        }
        if (!Files.exists(xlsFile))
            throw new FileNotFoundException("The Excel file " + xlsFile.toAbsolutePath() + " does not exist.");
    }

    private void writeOverviewTsvFile(List<Event> events, Path file) throws IOException {
        log.debug("Writing event statistics tsv file to {}", file);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file.toFile(), UTF_8))) {
            List<String> row = new ArrayList<>();
            for (Event e : events) {
                row.add(e.getFirstArgument().getPreferredName());
                row.add(e.getSecondArgument().getPreferredName());
                row.add(e.getFirstArgument().getText());
                row.add(e.getSecondArgument().getText());
                row.add(e.getFirstArgument().getGeneId());
                row.add(e.getSecondArgument().getGeneId());
//                row.add(e.getFirstArgument().getMatchType());
//                row.add(e.getSecondArgument().getMatchType());
                row.add(String.join(",", e.getAllEventTypes()));
                row.add(likelihoodNames.get(e.getLikelihood()));
                row.add(e.getDocId());
                row.add(e.getEventId());
                if (e.isSentenceMatchingFulltextQuery()) {
                    row.add("sentence");
                    row.add(e.getSentence());
                } else if (e.isParagraphMatchingFulltextQuery()) {
                    row.add("paragraph");
                    row.add(e.getParagraph());
                } else {
                    row.add("N/A");
                    row.add(e.getSentence());
                }

                bw.write(String.join("\t", row));
                bw.newLine();

                row.clear();
            }
        }
    }

    private Path getTempStatusFile(long dataSessionId) {
        return Path.of(gepiTmpDir.toString(), GEPI_EXCEL_STATUS_FILE_PREFIX + dataSessionId + ".txt");
    }

    private Path getTempTsvDataFile(long dataSessionId) {
        return Path.of(gepiTmpDir.toString(), GEPI_EXCEL_FILE_PREFIX_NAME + dataSessionId + ".tsv");
    }

    @Override
    public Path getTempXlsDataFile(long dataSessionId) {
        return Path.of(gepiTmpDir.toString(), GEPI_EXCEL_FILE_PREFIX_NAME + dataSessionId + ".xlsx");
    }

    private JSONObject getJsonObjectForArgument(Argument argument) {
        JSONObject source = new JSONObject();
        source.put("id", argument.getTopHomologyPreferredName());
        source.put("name", argument.getTopHomologyPreferredName());
        return source;
    }

}
