package de.julielab.gepi.webapp.components;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SupportsInformalParameters;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;

@Import(stylesheet= {"context:css-components/gepiwidgetlayout.less"})
@SupportsInformalParameters
public class GepiWidgetLayout {
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

	/**
	 * Overwrite in widget classes.
	 */
	@Property
	protected String loadingMessage = "Data is being loaded, please wait...";

	@Persist
	private CompletableFuture<EventRetrievalResult> persistResult;

	void setupRender() {
		persistResult = result;
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

	void onLoad() {
		Link eventLink = resources.createEventLink("refreshContent");
		javaScriptSupport.require("gepi/components/gepiwidgetlayout").invoke("loadWidgetContent")
				.with(eventLink.toAbsoluteURI(), widgetZone.getClientId());
	}
	
	public String getZoneId() {
		String zoneId = "widgetzone_" + clientId;
		return zoneId;
	}
}
