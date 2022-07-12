package de.julielab.gepi.webapp.pages;

import de.julielab.gepi.core.retrieval.data.*;
import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.gepi.webapp.base.TabPersistentField;
import de.julielab.gepi.webapp.components.GepiInput;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.http.services.Request;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.HttpError;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Start page of application gepi-webapp.
 */
@Import(stylesheet = {"context:css-pages/index.css"}, library = {"context:bootstrap-5.1.3-dist/js/bootstrap.bundle.min.js"})
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

    public Zone getOutputZone() {
        return outputZone;
    }

    public Zone getInputZone() {
        return inputZone;
    }

    void setupRender() {
        if (requestData == null) {
            dataSessionId = dataService.newSession();
            log.debug("Current dataSessionId is 0, initializing GePi session with ID {}", dataSessionId);
        } else {
            log.debug("Existing dataSessionId is {}", dataSessionId);
        }
        GePiData data = dataService.getData(dataSessionId);
        resultNonNullOnLoad = data != null && (data.getUnrolledResult() != null || data.getAggregatedResult() != null);
    }

    // Handle call with an unwanted context
    Object onActivate(EventContext eventContext) {
        return eventContext.getCount() > 0 ? new HttpError(404, "Resource not found") : null;
    }

    void afterRender() {
        javaScriptSupport.require("gepi/base").invoke("setuptooltips");
        javaScriptSupport.require("gepi/charts/data").invoke("setDataUrl").with(resources.createEventLink("loadDataToClient").toAbsoluteURI());
        if (isResultPresent()) {
            // If there already is data at loading the page, the input panel is already hidden (see #getShowInputClass)
            // and we can display the widgets.
            log.debug("Sending the ready signal for the widgets");
            javaScriptSupport.require("gepi/pages/index").invoke("readyForWidgets");
        }
    }

    private Future<EventRetrievalResult> getEsResult() {
        System.out.println("persistent dataSessionId for getEsResult " + dataSessionId);
        return dataService.getData(dataSessionId).getUnrolledResult();
    }

    private Future<AggregatedEventsRetrievalResult> getNeo4jResult() {
        return dataService.getData(dataSessionId).getAggregatedResult();
    }

    public boolean isResultPresent() {
        Future<EventRetrievalResult> esResult = getEsResult();
        Future<AggregatedEventsRetrievalResult> neo4jResult = getNeo4jResult();
        return (esResult != null && esResult.isDone()) || (neo4jResult != null && neo4jResult.isDone());
    }

    /**
     * @return The class "into", causing the outputcol to show immediately, or the empty string which will hide the outputcol initially.
     */
    public String getShowOutputClass() {
        if (isResultPresent())
            return "show";
        return "";
    }

//    public String getShowInputClass() {
//        if (getEsResult() == null && getNeo4jResult() == null)
//            return "show";
//        return "";
//    }

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

    public String getBodyScrollClass() {
        return hasLargeWidget ? "noScroll" : "";
    }

    public String getWidgetOverlayShowClass() {
        return hasLargeWidget ? "into" : "";
    }

    /**
     * Called from the client to retrieve the data for chart display.
     *
     * @return Aggregated data representation, i.e. counts of argument ID pairs.
     */
    JSONObject onLoadDataToClient() {
        String datasource = request.getParameter("datasource");
        long dataSessionId = Long.parseLong(Optional.ofNullable(request.getParameter("dataSessionId")).orElse("0"));
        log.debug("Received data request for '{}' for dataSessionId {} from the client.", datasource, dataSessionId);
        if (!datasource.equals("relationCounts") && !datasource.equals("acounts") && datasource.equals("bcounts"))
            throw new IllegalArgumentException("Unknown data source " + datasource);
        GePiData data = dataService.getData(dataSessionId);
        if (data.getUnrolledResult() == null && data.getAggregatedResult() == null)
            throw new IllegalStateException("The ES result and the Neo4j result for dataSessionId " + dataSessionId + " are both null.");
        try {
            log.debug("Creating JSON object from results.");
            JSONObject jsonObject = null;
            if (data.getAggregatedResult() != null) {
                AggregatedEventsRetrievalResult aggregatedEvents = data.getAggregatedResult().get();
                log.debug("Obtained aggregated events retrieval result with {} events.", aggregatedEvents.size());
                jsonObject = dataService.getPairedArgsCount(aggregatedEvents);
            }
            else {
                if (datasource.equals("relationCounts")) {
                    List<Event> eventList = data.getUnrolledResult().get().getEventList();
                    log.debug("Obtained unrolled list of individual events of size {}.", eventList.size());
                    jsonObject = dataService.getPairedArgsCount(eventList);
                } else if (datasource.equals("acounts")) {
                    JSONArray aCounts = dataService.getArgumentCount(data.getUnrolledResult().get().getEventList(), 0);
                    jsonObject = new JSONObject();
                    jsonObject.put("argumentcounts", aCounts);
                } else if (datasource.equals("bcounts")) {
                    JSONArray bCounts = dataService.getArgumentCount(data.getUnrolledResult().get().getEventList(), 1);
                    jsonObject = new JSONObject();
                    jsonObject.put("argumentcounts", bCounts);
                }
            }
            log.debug("Sending data of type {} to the client ", datasource);
            return jsonObject;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }


}
