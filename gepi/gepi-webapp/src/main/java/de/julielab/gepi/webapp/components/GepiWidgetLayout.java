package de.julielab.gepi.webapp.components;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import java.util.Optional;

import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;
import de.julielab.gepi.core.services.GePiDataService;
import de.julielab.gepi.core.services.IGePiDataService;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Log;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SupportsInformalParameters;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.webapp.pages.Index;
import org.slf4j.Logger;

@Import(stylesheet = {"context:css-components/gepiwidgetlayout.css"})
@SupportsInformalParameters
final public class GepiWidgetLayout {

    @Inject
    private Logger log;

    @Inject
    private IGePiDataService dataService;

    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    @Property
    private String widgettitle;

    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    @Property
    private String clientId;

    @Parameter(defaultPrefix = BindingConstants.LITERAL, name = "class")
    @Property
    private String classes;

    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    @Property
    private String sizeClass;
    @Parameter(name = "viewMode")
    @Property
    private String viewModeParam;
    @Parameter(value = "false")
    @Property
    private boolean useTapestryZoneUpdates;
    @Parameter
    private long dataSessionId;

    @InjectComponent
    private Zone widgetZone;
    @Inject
    private AjaxResponseRenderer ajaxResponseRenderer;
    @Inject
    private ComponentResources resources;
    @Environmental
    private JavaScriptSupport javaScriptSupport;

    @Persist
    @Property
    private String viewMode;
    @InjectPage
    private Index index;

    void setupRender() {
        if (getEsResult() == null)
            viewMode = null;
        if (viewMode == null)
            viewMode = ViewMode.OVERVIEW.name().toLowerCase();
    }

    void afterRender() {
        if (useTapestryZoneUpdates) {
            JSONObject widgetSettings = getWidgetSettings();
            JSONObject widgetObject = new JSONObject("widgetSettings", widgetSettings);
            // Not called for sankey, circle and all other widgets managing their JS themselves.
            javaScriptSupport.require("gepi/components/widgetManager").invoke("addWidget")
                    .with(clientId, widgetObject);
        }
    }

    public Future<EventRetrievalResult> getEsResult() {
        return dataService.getData(dataSessionId).getUnrolledResult();
    }

    public Future<AggregatedEventsRetrievalResult> getNeo4jResult() {
        return dataService.getData(dataSessionId).getAggregatedResult();
    }

    /**
     * To be used by concrete Widget classes.
     *
     * @return
     */
    public JSONObject getWidgetSettings() {
        Link toggleViewModeEventLink = resources.createEventLink("toggleViewMode");
        Link refreshContentEventLink = resources.createEventLink("refreshContent");
        JSONObject widgetSettings = new JSONObject();
        widgetSettings.put("handleId", getResizeHandleId());
        widgetSettings.put("widgetId", clientId);
        widgetSettings.put("toggleViewModeUrl", toggleViewModeEventLink.toAbsoluteURI());
        widgetSettings.put("refreshContentsUrl", refreshContentEventLink.toAbsoluteURI());
        widgetSettings.put("zoneElementId", widgetZone.getClientId());
        widgetSettings.put("useTapestryZoneUpdates", useTapestryZoneUpdates);
        widgetSettings.put("dataSessionId", dataSessionId);
        return widgetSettings;
    }

    public boolean isRenderBody() {
        // For widgets completely managed from JavaScript (sankey, pie), just render their basics because
        // the rest will be done in JS.
        // For Tapestry components
        return !useTapestryZoneUpdates || isResultAvailable();
    }

    public boolean isResultLoading() {
        if (getEsResult() != null && !getEsResult().isDone()) {
            return true;
        }
        return getEsResult() != null && !getEsResult().isDone();
    }

    public boolean isResultAvailable() {
        if (getNeo4jResult() != null && getNeo4jResult().isDone())
            return true;
        return getEsResult() != null && getEsResult().isDone();
    }

    void onRefreshContent() throws InterruptedException, ExecutionException {
        // If there is data from Neo4j, use that.
        if (getEsResult() != null && getNeo4jResult() == null) {
            log.debug("Waiting for ElasticSearch to return its results.");
            getEsResult().get();
            log.debug("ES result finished.");
        } else if (getNeo4jResult() != null) {
            log.debug("Waiting for Neo4j to return its results.");
            getNeo4jResult().get();
        }
        ajaxResponseRenderer.addRender(widgetZone);
    }

    public ViewMode viewMode() {
        return ViewMode.valueOf(viewMode.toUpperCase());
    }

    void onToggleViewMode() {
        System.out.println("toggle!!");
        switch (viewMode) {
            case "fullscreen":
                break;
            case "large":
                viewMode = ViewMode.OVERVIEW.name().toLowerCase();
                index.setHasLargeWidget(false);
                break;
            case "overview":
                viewMode = ViewMode.LARGE.name().toLowerCase();
                index.setHasLargeWidget(true);
                break;
        }
        ajaxResponseRenderer.addRender(widgetZone);
    }

    void onLoad() {
        if (useTapestryZoneUpdates) {
            javaScriptSupport.require("gepi/components/widgetManager").invoke("refreshWidget")
                    .with(clientId);
        }
    }

    public String getZoneId() {
        String zoneId = "widgetzone_" + clientId;
        return zoneId;
    }

    public String getResizeHandleId() {
        return clientId + "_resize";
    }


    @Log
    public boolean isLarge() {
        return viewMode.equals(ViewMode.LARGE.name().toLowerCase());
    }

    public enum ViewMode {
        /**
         * The widget is in its overview mode, shown in juxtaposition to other widgets.
         */
        OVERVIEW,
        /**
         * The widget covers the main view area of GePi, hiding other widgets.
         */
        LARGE,
        /**
         * The widget is in fullscreen mode, covering the complete computer screen.
         */
        FULLSCREEN
    }
}
