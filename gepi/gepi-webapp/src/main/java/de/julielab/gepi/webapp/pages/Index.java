package de.julielab.gepi.webapp.pages;

import de.julielab.gepi.core.retrieval.data.*;
import de.julielab.gepi.core.retrieval.services.IEventRetrievalService;
import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.gepi.webapp.base.TabPersistentField;
import de.julielab.gepi.webapp.components.GepiInput;
import de.julielab.gepi.webapp.components.TableResultWidget;
import de.julielab.gepi.webapp.data.GepiQueryParameters;
import de.julielab.gepi.webapp.state.GePiSessionState;
import de.julielab.java.utilities.FileUtilities;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.http.services.Request;
import org.apache.tapestry5.http.services.Response;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Start page of application gepi-webapp.
 */
@Import(stylesheet = {"context:css-pages/index.css"})
public class Index {
    @Inject
    private ComponentResources resources;
    @Inject
    private Request request;
    @Inject
    private Logger log;
    @Environmental
    private JavaScriptSupport javaScriptSupport;
    @Property
    @Inject
    @Symbol(SymbolConstants.TAPESTRY_VERSION)
    private String tapestryVersion;
    @InjectPage
    private About about;
    @InjectComponent
    private Zone outputZone;
    @InjectComponent
    private Zone inputZone;
    @Property
    private Event eventItem;
    @Property
    @Persist(TabPersistentField.TAB)
    private long dataSessionId;
    @Property
    @Persist(TabPersistentField.TAB)
    private GepiRequestData requestData;
    //    @Parameter
//    private long dataSessionIdParameter;
//    @Property
//    @Persist(TabPersistentField.TAB)
//    private EnumSet<InputMode> inputMode;
//    @Property
//    @Persist(TabPersistentField.TAB)
//    private String sentenceFilterString;
//    @Property
//    @Persist(TabPersistentField.TAB)
//    private String paragraphFilterString;
    @Persist(TabPersistentField.TAB)
    private boolean hasLargeWidget;

    private boolean resultNonNullOnLoad;

    @Inject
    private IGePiDataService dataService;

    @InjectComponent
    private GepiInput gepiInput;
    @InjectComponent
    private TableResultWidget tableResultWidget;
    private boolean sessionExists = false;
    @InjectPage
    private ResultDownload resultDownload;

    public Zone getOutputZone() {
        return outputZone;
    }

    public Zone getInputZone() {
        return inputZone;
    }

    void setupRender() {
        GePiData data = dataService.getData(dataSessionId);
        resultNonNullOnLoad = data != null && (data.getUnrolledResult4charts() != null || data.getNeo4jAggregatedResult() != null);
    }
    @Inject
    private ApplicationStateManager asm;
    Object onActivate(EventContext eventContext) {
        if (requestData == null) {
            final GePiSessionState gePiSessionState = asm.get(GePiSessionState.class);
            dataSessionId = dataService.newSession();
            log.debug("Current dataSessionId is 0, initializing GePi session with ID {}", dataSessionId);
            log.debug("No session");
        } else {
            log.debug("Session with ID {}", dataSessionId);
            log.debug("Existing dataSessionId is {}", dataSessionId);
            sessionExists = true;
        }

        final GepiQueryParameters gepiQueryParameters = new GepiQueryParameters(request);
        if (gepiQueryParameters.isValidRequest()) {
            log.info("Received valid query parameters for GePI search.");
            gepiInput.executeSearch(gepiQueryParameters, dataSessionId);
            switch (gepiQueryParameters.getFormat()) {
                case "excel":
                    return tableResultWidget.onDownload();
                case "tsv":
                    final Future<EventRetrievalResult> unrolledRetrievalResult = dataService.getUnrolledResult4download(requestData, eventRetrievalService);
                    try {
                        final Path tsvFile = dataService.writeOverviewTsvFile(unrolledRetrievalResult.get().getEventList(), requestData.getDataSessionId());
                        return new StreamResponse() {

                            @Override
                            public void prepareResponse(Response response) {
                                try {
                                    response.setHeader("Content-Length", "" + Files.size(tsvFile)); // output into file
                                    response.setHeader("Content-disposition", "attachment; filename=" + tsvFile.getFileName());
                                } catch (Exception e) {
                                    log.error("Could not create TSV result for dataSessionId {}", requestData.getDataSessionId(), e);
                                }
                            }

                            @Override
                            public InputStream getStream() throws IOException {
                                return FileUtilities.getInputStreamFromFile(tsvFile.toFile());
                            }

                            @Override
                            public String getContentType() {
                                return "text/csv";
                            }
                        };
                    } catch (Exception e) {
                     log.error("Could not serve TSV file due to Exception", e);
                    }
                    return dataService;
                default: return this;
            }
            //return this;
        } else {
            log.debug("Query parameters did not contain a valid GePI search.");
        }
        return null;
    }
    @Inject
    private IEventRetrievalService eventRetrievalService;
    public Future<EventRetrievalResult> getUnrolledResult4download() {
        return dataService.getData(requestData.getDataSessionId()).getUnrolledResult4download().get();
    }

    void afterRender() {
//        System.out.println("Server Name: " + request.getServerName());
//        System.out.println("Server Port: " + request.getServerPort());
//        System.out.println("Local Port: " + request.getLocalPort());
//        System.out.println("Is Secure: " + request.isSecure());
//        System.out.println("Path: " + request.getPath());
//        System.out.println("Remote Host: " + request.getRemoteHost());
//        for (var name : request.getAttributeNames())
//            System.out.println("Attribute " + name + ": " + request.getAttribute(name));
        javaScriptSupport.require("gepi/base").invoke("setuptooltips");
        javaScriptSupport.require("gepi/charts/data").invoke("setDataUrl").with(resources.createEventLink("loadDataToClient").toAbsoluteURI(productionMode));
        javaScriptSupport.require("gepi/pages/index").invoke("setupDownloadUrlCopyButton");
//        javaScriptSupport.require("gepi/pages/index").invoke("displayRoadworksWarningToast");
        if (isResultPresent()) {
            // If there already is data at loading the page, the input panel is already hidden (see #getShowInputClass)
            // and we can display the widgets.
            log.debug("Sending the ready signal for the widgets");
            javaScriptSupport.require("gepi/pages/index").invoke("readyForWidgets");
        } else {
            log.debug("No result present, not showing results but input form.");
        }
        if (!sessionExists) {
            log.info("No session exists, displaying cookie consent modal");
            javaScriptSupport.require("gepi/pages/index").invoke("displayCookieConsentOffcanvas");
        } else {
            log.info("There already is a session");
        }
    }

    public boolean isResultPresent() {
        final boolean resultPresent = dataService.getData(dataSessionId).isAnyResultAvailable();
        log.debug("Is any result available: {}", resultPresent);
        return resultPresent;
    }

//    public String getShowInputClass() {
//        if (getEsResult() == null && getNeo4jResult() == null)
//            return "show";
//        return "";
//    }

    /**
     * @return The class "into", causing the outputcol to show immediately, or the empty string which will hide the outputcol initially.
     */
    public String getShowOutputClass() {
        if (isResultPresent())
            return "show";
        return "";
    }

    public Object onReset() {
        log.debug("Reset!");
        requestData = null;
        gepiInput.reset();
        return this;
    }

    public boolean hasLargeWidget() {
        return hasLargeWidget;
    }

    public void setHasLargeWidget(boolean hasLargeWidget) {
        this.hasLargeWidget = hasLargeWidget;
    }

//    public String getWidgetOverlayShowClass() {
//        return hasLargeWidget ? "into" : "";
//    }

    public String getBodyScrollClass() {
        return hasLargeWidget ? "noScroll" : "";
    }

    /**
     * Called from the client to retrieve the data for chart display.
     *
     * @return Aggregated data representation, i.e. counts of argument ID pairs.
     */
    JSONObject onLoadDataToClient() {
        String datasource = request.getParameter("datasource");
        log.info("Got loadDataToClient event for {} in Thread {}", datasource, Thread.currentThread().getName());
        long dataSessionId = Long.parseLong(Optional.ofNullable(request.getParameter("dataSessionId")).orElse("0"));
        log.debug("[{}] Received data request for '{}' for dataSessionId {} from the client.", dataSessionId, datasource, dataSessionId);
        if (!datasource.equals("relationCounts") && !datasource.equals("acounts") && !datasource.equals("bcounts"))
            throw new IllegalArgumentException("Unknown data source " + datasource);
        GePiData data = dataService.getData(dataSessionId);
        if (data.getEsAggregatedResult() == null && data.getUnrolledResult4charts() == null && data.getNeo4jAggregatedResult() == null)
            throw new IllegalStateException("The ES result and the Neo4j result for dataSessionId " + dataSessionId + " are both null.");
        try {
            log.debug("Creating JSON object from results.");
            JSONObject jsonObject = null;
            if (data.getNeo4jAggregatedResult() != null) {
                Neo4jAggregatedEventsRetrievalResult aggregatedEvents = data.getNeo4jAggregatedResult().get();
                log.debug("[{}] Obtained Neo4j-aggregated events retrieval result with {} events.", dataSessionId, aggregatedEvents.size());
                jsonObject = dataService.getPairedArgsCount(aggregatedEvents);
            }
            else if (data.getEsAggregatedResult() != null) {
                final EsAggregatedResult esAggregatedResult = data.getEsAggregatedResult().get();
                if (datasource.equals("relationCounts")) {
                    final List<Pair<Event, Integer>> eventFrequencies = esAggregatedResult.getEventFrequencies();
                    log.debug("[{}] Obtained ES-aggregated events retrieval result with {} events.", dataSessionId, eventFrequencies.size());
                    jsonObject = dataService.getPairedArgsCountFromPairs(eventFrequencies);
                } else if (datasource.equals("acounts")) {
                    JSONArray aCounts = dataService.getArgumentCount(esAggregatedResult.getASymbolFrequencies());
                    log.debug("[{}] Obtained A list counts of size {} from ES-aggregation.", dataSessionId, aCounts.size());
                    jsonObject = new JSONObject();
                    jsonObject.put("argumentcounts", aCounts);
                } else if (datasource.equals("bcounts")) {
                    JSONArray bCounts = dataService.getArgumentCount(esAggregatedResult.getBSymbolFrequencies());
                    log.debug("[{}] Obtained B list counts of size {} from ES-aggregation.", dataSessionId, bCounts.size());
                    jsonObject = new JSONObject();
                    jsonObject.put("argumentcounts", bCounts);
                }
            }
            else {
                if (datasource.equals("relationCounts")) {
                    List<Event> eventList = data.getUnrolledResult4charts().get().getEventList();
                    log.debug("[{}] Obtained unrolled list of individual events of size {}.", dataSessionId, eventList.size());
                    jsonObject = dataService.getPairedArgsCount(eventList);
                } else if (datasource.equals("acounts")) {
                    JSONArray aCounts = dataService.getArgumentCount(data.getUnrolledResult4charts().get().getEventList(), 0);
                    log.debug("[{}] Obtained A list counts of size {}.", dataSessionId, aCounts.size());
                    jsonObject = new JSONObject();
                    jsonObject.put("argumentcounts", aCounts);
                } else if (datasource.equals("bcounts")) {
                    JSONArray bCounts = dataService.getArgumentCount(data.getUnrolledResult4charts().get().getEventList(), 1);
                    log.debug("[{}] Obtained B list counts of size {}.", dataSessionId, bCounts.size());
                    jsonObject = new JSONObject();
                    jsonObject.put("argumentcounts", bCounts);
                }
            }
            log.debug("Sending data of type {} to the client ", datasource);
            log.info("Serving loadDataToClient event for {} in Thread {}", datasource, Thread.currentThread().getName());
            return jsonObject;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getStatsSmColSize() {
        if (!isAList()) {
            // no gene lists given
            return 6;
        } else if (isAList() && !isBList()) {
            // only a list given
            return 8;
        }
        // AB lists given
        return 12;
    }

    public int getStatsLgColSize() {
        if (!isAList()) {
            // no gene lists given
            return 7;
        } else if (isAList() && !isBList()) {
            // only a list given
            return 12;
        }
        // AB lists given
        return 12;
    }

    public int getStatsXxlColSize() {
        if (!isAList()) {
            // no gene lists given
            return 6;
        } else if (isAList() && !isBList()) {
            // only a list given
            return 12;
        }
        // AB lists given
        return 12;
    }

    public int getStatsXxxlColSize() {
        if (!isAList()) {
            // no gene lists given
            return 4;
        } else if (isAList() && !isBList()) {
            // only a list given
            return 8;
        }
        // AB lists given
        return 12;
    }

    public int getPieSmColSize() {
        if (!isBList()) {
            return 12 - getStatsSmColSize();
        }
        // AB lists given
        return 6;
    }

    public int getPieLgColSize() {
        if (!isAList()) {
            // no gene lists given
            return 12 - getStatsLgColSize();
        } else if (isAList() && !isBList()) {
            // only a list given
            return 6;
        }
        // AB lists given
        return 6;
    }

    public int getPieXxlColSize() {
        if (!isBList()) {
            return 6;
        }
        return 6;
    }

    public int getPieXxxlColSize() {
        if (!isAList()) {
            // no gene lists given
            return 4;
        } else if (!isBList()) {
            return 12 - getStatsXxxlColSize();
        }
        // AB lists given
        return 6;
    }

    public int getBarXxlColSize() {
        if (!isAList()) {
            // no gene lists given
            return 4;
        } else if (!isBList()) {
            return 6;
        }
        // AB lists given
        return 6;
    }

    public int getBarXxxlColSize() {
        if (!isAList()) {
            // no gene lists given
            return 4;
        } else if (!isBList()) {
            return 4;
        }
        // AB lists given
        return 6;
    }

    public int getSankeyXxlColSize() {
        if (!isAList()) {
            // no gene lists given
            return 4;
        } else if (isBList()) {
            // a and b lists given
            return 6;
        }
        // only a list given
        return 6;
    }

    public int getSankeyXxxlColSize() {
        if (!isAList()) {
            // no gene lists given
            return 6;
        } else if (isBList()) {
            // a and b lists given
            return 6;
        }
        // only a list given
        return 4;
    }

    public int getSankeyComSmColSize() {
        if (!isAList()) {
            // no gene lists given
            return 12;
        } else if (isBList()) {
            // a and b lists given
            return 6;
        }
        // only a list given
        return 4;
    }

    public int getSankeyComLgColSize() {
        if (!isAList()) {
            // no gene lists given
            return 12;
        } else if (isBList()) {
            // a and b lists given
            return 6;
        }
        // only a list given
        return 6;
    }

    private boolean isAList() {
        return requestData.getInputMode().contains(InputMode.A) || isBList();
    }

    private boolean isBList() {
        return requestData.getInputMode().contains(InputMode.AB);
    }

    @Inject
    @Symbol(SymbolConstants.PRODUCTION_MODE)
    private boolean productionMode;
    @Inject
    private PageRenderLinkSource pageRenderLS;

    public String getResultFileDownloadLink() {
        return pageRenderLS.createPageRenderLinkWithContext(ResultDownload.class, dataSessionId).toAbsoluteURI(productionMode);
    }

}
