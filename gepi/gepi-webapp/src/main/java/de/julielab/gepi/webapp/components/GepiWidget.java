package de.julielab.gepi.webapp.components;

import java.util.concurrent.CompletableFuture;

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
	
	@Parameter
	@Property
	private CompletableFuture<EventRetrievalResult> result;
	
	@Property
	@Persist
	protected CompletableFuture<EventRetrievalResult> persistResult;

	@InjectComponent
	private GepiWidgetLayout gepiWidgetLayout;

	void setupRender() {
		persistResult = result;
	}

	public boolean isLargeView() {
		final GepiWidgetLayout.ViewMode mode = gepiWidgetLayout.viewMode();
		return mode == GepiWidgetLayout.ViewMode.LARGE || mode == GepiWidgetLayout.ViewMode.FULLSCREEN;
	}
}
