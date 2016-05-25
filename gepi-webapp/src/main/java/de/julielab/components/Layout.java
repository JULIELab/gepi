package de.julielab.components;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 * Layout component for pages of application test-project.
 */
@Import(module = "bootstrap/collapse")
public class Layout {
	@Inject
	private ComponentResources resources;

	/**
	 * The page title, for the <title> element and the
	 * <h1>element.
	 */
	@Property
	@Parameter(required = true, defaultPrefix = BindingConstants.LITERAL)
	private String title;

	@Property
	private String pageName;

	@Environmental
	private JavaScriptSupport javaScriptSupport;

	@Property
	@Inject
	@Symbol(SymbolConstants.APPLICATION_VERSION)
	private String appVersion;

	public String getClassForPageName() {
		return resources.getPageName().equalsIgnoreCase(pageName) ? "active" : null;
	}

	public String[] getPageNames() {
		return new String[] { "Index", "About", "Contact" };
	}

	public void afterRender() {
		javaScriptSupport.require("gepi/base").invoke("setuptooltips");
	}
}
