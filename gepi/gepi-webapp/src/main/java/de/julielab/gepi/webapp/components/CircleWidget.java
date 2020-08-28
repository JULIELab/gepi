package de.julielab.gepi.webapp.components;

import de.julielab.gepi.core.services.IChartsDataManager;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import java.util.concurrent.ExecutionException;

public class CircleWidget extends GepiWidget {

    @Inject
    private JavaScriptSupport javaScriptSupport;

    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    @Property
    private String elementId;



    void afterRender() {
        javaScriptSupport.require("gepi/charts/circlechart").with(elementId);
    }

}
