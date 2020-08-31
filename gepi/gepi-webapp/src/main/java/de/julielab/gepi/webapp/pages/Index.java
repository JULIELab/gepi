package de.julielab.gepi.webapp.pages;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;
import de.julielab.gepi.core.retrieval.data.GePiData;
import de.julielab.gepi.core.services.IGePiDataService;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.HttpError;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.slf4j.Logger;

import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;

/**
 * Start page of application gepi-webapp.
 */
@Import(stylesheet = {"context:css-pages/index.less"}, library = {"context:mybootstrap/js/dropdown.js"})
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
    @Persist
    private long dataSessionId;
    @Parameter
    private long dataSessionIdParameter;
    @Persist
    private boolean hasLargeWidget;
    private boolean resultNonNullOnLoad;

    @Inject
    private IGePiDataService dataService;


    public Zone getOutputZone() {
        return outputZone;
    }

    public Zone getInputZone() {
        return inputZone;
    }

    void setupRender() {
        if (dataSessionId == 0) {
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
            return "into";
        return "";
    }

    public String getShowInputClass() {
        if (getEsResult() == null && getNeo4jResult() == null)
            return "into";
        return "";
    }

    public Object onReset() {
        log.debug("Reset!");
        dataSessionId = 0;
//        dataSessionIdParameter = 0;
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
        if (!datasource.equals("relationCounts"))
            throw new IllegalArgumentException("Unknown data source " + datasource);
        log.debug("Checked datasource name");
        GePiData data = dataService.getData(dataSessionId);
        if (data.getUnrolledResult() == null && data.getAggregatedResult() == null)
            throw new IllegalStateException("The ES result and the Neo4j result for dataSessionId " + dataSessionId + " are both null.");
        log.debug("Checked if results are null.");
        try {
            log.debug("Creating JSON object from results.");
            JSONObject jsonObject;
            if (data.getAggregatedResult() != null) {
                AggregatedEventsRetrievalResult aggregatedEvents = data.getAggregatedResult().get();
                log.debug("Obtained aggregated events retrieval result with {} events.", aggregatedEvents.size());
                jsonObject = dataService.getPairedArgsCount(aggregatedEvents);
            }
            else {
                List<Event> eventList = data.getUnrolledResult().get().getEventList();
                log.debug("Obtained unrolled list of individual events of size {}.", eventList.size());
                jsonObject = dataService.getPairedArgsCount(eventList);
            }
            log.debug("Sending data of type {} with {} nodes and {} links to the client ", datasource, jsonObject.getJSONArray("nodes").length(), jsonObject.getJSONArray("links").length());
            return jsonObject;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

}
