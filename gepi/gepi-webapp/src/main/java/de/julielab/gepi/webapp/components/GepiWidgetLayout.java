package de.julielab.gepi.webapp.components;

import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.GepiRequestData;
import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.gepi.webapp.base.TabPersistentField;
import de.julielab.gepi.webapp.pages.Index;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.http.Link;
import org.apache.tapestry5.http.services.Response;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
    @Parameter(value = "false")
    @Property
    private boolean waitForData;
    @Parameter
    @Property
    protected GepiRequestData requestData;
    @Parameter(value="false")
    @Property
    protected boolean downloadable;

    @InjectComponent
    private Zone widgetZone;
    @Inject
    private AjaxResponseRenderer ajaxResponseRenderer;
    @Inject
    private ComponentResources resources;
    @Environmental
    private JavaScriptSupport javaScriptSupport;

    @Persist(TabPersistentField.TAB)
    @Property
    private String viewMode;
    @InjectPage
    private Index index;

    void setupRender() {
        if (getEsResult() == null)
            viewMode = null;
        if (viewMode == null)
            viewMode = ViewMode.SMALL.name().toLowerCase();
        if (useTapestryZoneUpdates) {
            // normally, JavaScript is put into afterRender() to allow access to the rendered HTML elements of
            // a component. Here, however, we need to add the widget to the WidgetManager before we render
            // the component. During component rendering, the refreshContents() event handler is called
            // for which the WidgetManager needs to have the widget already.
            JSONObject widgetSettings = getWidgetSettings();
            JSONObject widgetObject = new JSONObject("widgetSettings", widgetSettings);
            // Not called for sankey, circle and all other widgets managing their JS themselves.
            javaScriptSupport.require("gepi/components/widgetManager").invoke("addWidget")
                    .with(clientId, widgetObject);
        }
    }

    public Future<EventRetrievalResult> getEsResult() {
        return dataService.getData(requestData.getDataSessionId()).getUnrolledResult();
    }

    public Future<AggregatedEventsRetrievalResult> getNeo4jResult() {
        return dataService.getData(requestData.getDataSessionId()).getAggregatedResult();
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
        widgetSettings.put("viewMode", "small");
        widgetSettings.put("toggleViewModeUrl", toggleViewModeEventLink.toAbsoluteURI());
        widgetSettings.put("refreshContentsUrl", refreshContentEventLink.toAbsoluteURI());
        widgetSettings.put("zoneElementId", widgetZone.getClientId());
        widgetSettings.put("useTapestryZoneUpdates", useTapestryZoneUpdates);
        widgetSettings.put("dataSessionId", requestData.getDataSessionId());
        return widgetSettings;
    }

    public boolean isRenderBody() {
        // For widgets completely managed from JavaScript (sankey, pie), just render their basics because
        // the rest will be done in JS.
        // For Tapestry components
        return !useTapestryZoneUpdates || isResultAvailable();
    }

    public boolean isResultLoading() {
        if (!waitForData)
            return false;
//        log.info("ESResult: {}", getEsResult());
        if (getEsResult() != null)
//        log.info("ESResult done: {}", getEsResult().isDone());
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
                viewMode = ViewMode.SMALL.name().toLowerCase();
                index.setHasLargeWidget(false);
                break;
            case "small":
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
        SMALL,
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
