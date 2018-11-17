package de.julielab.gepi.webapp.pages;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import de.julielab.gepi.core.services.IChartsDataManager;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.ActivationRequestParameter;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
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
    private Logger logger;

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
    @Persist
    private CompletableFuture<EventRetrievalResult> result;

    @Property
    private Event eventItem;

    @Persist
    private boolean hasLargeWidget;

    private boolean resultNonNullOnLoad;

    /**
     * This is an emergency exit against being locked in an error during development.
     */
    @ActivationRequestParameter
    private boolean reset;

    public Zone getOutputZone() {
        return outputZone;
    }

    public Zone getInputZone() {
        return inputZone;
    }

    void setupRender() {
        resultNonNullOnLoad = result != null;
    }

    // Handle call with an unwanted context
    Object onActivate(EventContext eventContext) {
        if (reset)
            result = null;
        return eventContext.getCount() > 0 ? new HttpError(404, "Resource not found") : null;
    }

    @Inject
    ComponentResources resources;


    void afterRender() {
        javaScriptSupport.require("gepi/base").invoke("setuptooltips");
        javaScriptSupport.require("gepi/charts/data").invoke("setDataUrl").with(resources.createEventLink("loadDataToClient").toAbsoluteURI());
        if (result != null) {
            // If there already is data at loading the page, the input panel is already hidden (see #getShowInputClass)
            // and we can display the widgets.
            javaScriptSupport.require("gepi/pages/index").invoke("readyForWidgets");
        }
    }

    /**
     * @return The class "into", causing the outputcol to show immediately, or the empty string which will hide the outputcol initially.
     */
    public String getShowOutputClass() {
        if (result != null && result.isDone())
            return "into";
        return "";
    }

    public String getShowInputClass() {
        if (result == null)
            return "into";
        return "";
    }

    public Object onReset() {
        result = null;
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


    @Inject
    Request request;
    @Inject
    private IChartsDataManager chartMnger;
    JSONObject onLoadDataToClient() {
        String datasource = request.getParameter("datasource");
        if (!datasource.equals("relationCounts"))
            throw new IllegalArgumentException("Unknown data source " + datasource);
        logger.debug("Sending data of type {} to the client ", datasource);
        try {
            return chartMnger.getPairedArgsCount(result.get().getEventList());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
}
