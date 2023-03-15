package de.julielab.gepi.webapp.components;

import de.julielab.gepi.core.retrieval.data.EsAggregatedResult;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.GepiRequestData;
import de.julielab.gepi.core.retrieval.data.Neo4jAggregatedEventsRetrievalResult;
import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.gepi.webapp.base.TabPersistentField;
import de.julielab.gepi.webapp.data.ResultType;
import de.julielab.gepi.webapp.pages.Index;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.http.Link;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.slf4j.Logger;

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
    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    private ResultType resultType;
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
    @Parameter(value="false")
    @Property
    protected boolean resizable;

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

    public Future<?> getEsResult() {
        switch (resultType) {
            case PAGED:
                return getPagedEsResult();
            case UNROLLED:
               return getUnrolledEsResult();
            case AGGREGATED:
                return getAggregatedEsResult();
            default:
                throw new IllegalArgumentException("Unknown resultType '" + resultType + "'");
        }
    }

    private Future<EsAggregatedResult> getAggregatedEsResult() {
        return dataService.getData(requestData.getDataSessionId()).getEsAggregatedResult();
    }

    public Future<EventRetrievalResult> getPagedEsResult() {
        return dataService.getData(requestData.getDataSessionId()).getPagedResult();
    }

    public Future<EventRetrievalResult> getUnrolledEsResult() {
        return dataService.getData(requestData.getDataSessionId()).getUnrolledResult4charts();
    }

    public Future<Neo4jAggregatedEventsRetrievalResult> getNeo4jResult() {
        return dataService.getData(requestData.getDataSessionId()).getNeo4jAggregatedResult();
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

    /**
     * For widgets completely managed from JavaScript (sankey, pie), we always render the body and also the
     * loading message, see isResultLoading(). Such widgets then fetch data via Ajax und remove the loading message
     * themselves without a Tapestry zone update.
     * For Tapestry components, we refrain from rendering the body as long as the data is not available. For them,
     * too, there is an Ajax call but through the TapestryZoneManager JavaScript object in WidgetManager.js.
     * @return Whether the result required by this dashboard element - unrolled result or paged result - is currently available.
     */
    public boolean isRenderBody() {
        return !useTapestryZoneUpdates || isResultAvailable();
    }

    /**
     * <p>
     * Used to determine weather a loading message should be displayed while an Ajax call is started to fetch
     * the actual data when it is ready.
     * </p>
     * @return
     */
    public boolean isResultLoading() {
        if (!waitForData)
            return false;
//        return getEsResult() != null && !getEsResult().isDone();

        return !dataService.getData(requestData.getDataSessionId()).isAnyResultAvailable();
    }

    public boolean isResultAvailable() {
        if (getNeo4jResult() != null && getNeo4jResult().isDone())
            return true;
        return getEsResult() != null && getEsResult().isDone();
    }

    void onRefreshContent() throws InterruptedException, ExecutionException {
        log.info("Got refresh content request for {} in Thread {}", clientId, Thread.currentThread().getName());
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
        log.info("Serving refresh content request for {} in Thread {}", clientId, Thread.currentThread().getName());
    }

    void onLoad() {
        log.info("Got load event for {} in Thread {}", clientId, Thread.currentThread().getName());
        if (useTapestryZoneUpdates) {
            javaScriptSupport.require("gepi/components/widgetManager").invoke("refreshWidget")
                    .with(clientId);
            log.info("Sending refreshWidget to {} in Thread {}", clientId, Thread.currentThread().getName());
        }
    }

    public ViewMode viewMode() {
        return ViewMode.valueOf(viewMode.toUpperCase());
    }

    void onToggleViewMode() {
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



    public String getZoneId() {
        String zoneId = "widgetzone_" + clientId;
        return zoneId;
    }

    public String getResizeHandleId() {
        return clientId + "_resize";
    }

    public Zone getBodyZone() {
        return (Zone) resources.getEmbeddedComponent("widgetZone");
    }

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
