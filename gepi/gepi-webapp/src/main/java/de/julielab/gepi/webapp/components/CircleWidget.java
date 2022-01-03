package de.julielab.gepi.webapp.components;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.javascript.InitializationPriority;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

public class CircleWidget extends GepiWidget {

    @Inject
    private JavaScriptSupport javaScriptSupport;

    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    @Property
    private String elementId;

    @InjectComponent("gepiwidgetlayout")
    private GepiWidgetLayout component;

    void afterRender() {
        if (component.isResultLoading() || component.isResultAvailable())
            javaScriptSupport.require("gepi/charts/circlechart").priority(InitializationPriority.LATE).invoke("init").with(elementId, component.getWidgetSettings());
    }

}
