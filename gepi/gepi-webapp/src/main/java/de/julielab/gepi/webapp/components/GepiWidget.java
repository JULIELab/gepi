package de.julielab.gepi.webapp.components;

import java.util.concurrent.CompletableFuture;

import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;

import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;

public class GepiWidget {

	@Parameter(defaultPrefix = BindingConstants.LITERAL, name = "class")
	@Property
	private String classes;

	/**
	 * For loading state display
	 */
	@Parameter
	@Property
	private CompletableFuture<EventRetrievalResult> esResult;

	/**
	 * For loading state display
	 */
	@Parameter
	@Property
	private CompletableFuture<AggregatedEventsRetrievalResult> neo4jResult;

	/**
	 * For loading state display
	 */
	@Property
	@Persist
	protected CompletableFuture<EventRetrievalResult> persistEsResult;

	/**
	 * For loading state display
	 */
	@Property
	@Persist
	protected CompletableFuture<AggregatedEventsRetrievalResult> persistNeo4jResult;

	@InjectComponent
	private GepiWidgetLayout gepiWidgetLayout;

	void setupRender() {
		persistEsResult = esResult;
		persistNeo4jResult = neo4jResult;
	}

	public boolean isLargeView() {
		final GepiWidgetLayout.ViewMode mode = gepiWidgetLayout.viewMode();
		return mode == GepiWidgetLayout.ViewMode.LARGE || mode == GepiWidgetLayout.ViewMode.FULLSCREEN;
	}
}
