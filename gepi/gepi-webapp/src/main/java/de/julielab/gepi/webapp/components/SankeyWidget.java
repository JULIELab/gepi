package de.julielab.gepi.webapp.components;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.services.javascript.InitializationPriority;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

public class SankeyWidget extends GepiWidget {

    @Inject
    private JavaScriptSupport javaScriptSupport;

    @Property
    private JSONArray eventsJSON;

    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    @Property
    private String elementId;

    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    @Property
    private boolean commonPartners;

    @InjectComponent("gepiwidgetlayout")
    private GepiWidgetLayout component;

    void afterRender() {
        if (component.isResultLoading() || component.isResultAvailable()) {
            if (commonPartners) {
                javaScriptSupport.require("gepi/charts/sankeychart").with(elementId, "commonPartnersHarmonicMean", component.getWidgetSettings());
            } else {
                javaScriptSupport.require("gepi/charts/sankeychart").with(elementId, "frequency", component.getWidgetSettings());
            }
        }
    }

    public String getWidgetTitle() {
        return commonPartners ? "Interactions with Common Partners" : "Most Frequent Interactions";
    }
}
