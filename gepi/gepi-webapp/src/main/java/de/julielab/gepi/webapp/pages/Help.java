package de.julielab.gepi.webapp.pages;

import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

@Import(stylesheet = {"context:css-pages/help.css"})
public class Help {

    @Environmental
    private JavaScriptSupport javaScriptSupport;

    public void afterRender() {
//        javaScriptSupport.require("gepi/pages/help").invoke("setupScrollSpy");
        javaScriptSupport.require("gepi/base").invoke("setuptooltips");
    }
}
