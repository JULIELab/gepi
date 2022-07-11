package de.julielab.gepi.webapp.components;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Log;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.services.IGePiDataService;

public class PieChartWidget extends GepiWidget {

    @Inject
    private JavaScriptSupport javaScriptSupport;

    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    @Property
    private String elementId;

    @Property
    private JSONArray eventsJSON;

    @InjectComponent("gepiwidgetlayout")
    private GepiWidgetLayout component;

    @Log
    void afterRender() {
        if (component.isResultLoading() || component.isResultAvailable()) {
            javaScriptSupport.require("gepi/charts/piechart").with(elementId, component.getWidgetSettings());
        }
    }

}
