package de.julielab.gepi.webapp.components;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

@Import(stylesheet = {"context:css-components/barchartwidget.css"})
public class BarChartWidget extends GepiWidget {

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
            javaScriptSupport.require("gepi/charts/barchart").with(elementId, component.getWidgetSettings());
        }
    }

}
