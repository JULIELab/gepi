package de.julielab.gepi.webapp.components;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Log;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SupportsInformalParameters;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.webapp.pages.Index;

@Import(stylesheet = { "context:css-components/gepiwidgetlayout.css" })
@SupportsInformalParameters
public class GepiWidgetLayout {
	
	public enum ViewMode {
		/**
		 * The widget is in its overview mode, shown in juxtaposition to other widgets.
		 */
		OVERVIEW, 
		/**
		 * The widget covers the main view area of GePi, hiding other widgets.
		 */
		LARGE, 
		/**
		 * The widget is in fullscreen mode, covering the complete computer screen.
		 */
		FULLSCREEN
	}
	
	@Parameter(defaultPrefix = BindingConstants.LITERAL)
	@Property
	private String widgettitle;

	@Parameter(defaultPrefix = BindingConstants.LITERAL)
	@Property
	private String clientId;

	@Parameter(defaultPrefix = BindingConstants.LITERAL, name = "class")
	@Property
	private String classes;

	@Parameter
	@Property
	protected CompletableFuture<EventRetrievalResult> result;

	@InjectComponent
	private Zone widgetZone;

	@Inject
	private AjaxResponseRenderer ajaxResponseRenderer;

	@Inject
	private ComponentResources resources;

	@Environmental
	private JavaScriptSupport javaScriptSupport;

	@Persist
	private CompletableFuture<EventRetrievalResult> persistResult;

	@Persist
	@Property
	private String viewMode;

	void setupRender() {
		persistResult = result;
		if (result == null)
			viewMode = null;
		if (viewMode == null)
			viewMode = ViewMode.OVERVIEW.name().toLowerCase();
	}
	
	void afterRender() {
		Link eventLink = resources.createEventLink("toggleViewMode");
		javaScriptSupport.require("gepi/components/gepiwidgetlayout").invoke("setupViewModeHandle")
				.with(getResizeHandleId(), clientId, eventLink.toAbsoluteURI(), widgetZone.getClientId());
	}

	public boolean isResultReady() {
		return persistResult != null && persistResult.isDone();
	}

	public boolean isResultLoading() {
		return persistResult != null && !persistResult.isDone();
	}

	void onRefreshContent() throws InterruptedException, ExecutionException {
		persistResult.get();
		ajaxResponseRenderer.addRender(widgetZone);
	}
	
	@InjectPage
	private Index index;
	void onToggleViewMode() {
		switch (viewMode) {
		case "fullscreen":
			break;
		case "large":
			viewMode = ViewMode.OVERVIEW.name().toLowerCase();
			index.setHasLargeWidget(false);
			break;
		case "overview":
			viewMode = ViewMode.LARGE.name().toLowerCase();
			index.setHasLargeWidget(true);
			break;
		}
		ajaxResponseRenderer.addRender(widgetZone);
	}

	void onLoad() {
		Link eventLink = resources.createEventLink("refreshContent");
		javaScriptSupport.require("gepi/components/gepiwidgetlayout").invoke("loadWidgetContent")
				.with(eventLink.toAbsoluteURI(), widgetZone.getClientId());
	}

	public String getZoneId() {
		String zoneId = "widgetzone_" + clientId;
		return zoneId;
	}

	public String getResizeHandleId() {
		return clientId + "_resize";
	}
	
	public String getSizeClass() {
		return viewMode;
	}
	
	@Log
	public boolean isLarge() {
		return viewMode.equals(ViewMode.LARGE.name().toLowerCase());
	}
}
