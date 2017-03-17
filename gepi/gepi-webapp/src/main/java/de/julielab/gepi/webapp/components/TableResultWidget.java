package de.julielab.gepi.webapp.components;

import java.util.concurrent.CompletableFuture;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;

import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;

public class TableResultWidget  {
	@Parameter(defaultPrefix = BindingConstants.LITERAL, name = "class")
	@Property
	private String classes;

	@Parameter
	@Property
	protected CompletableFuture<EventRetrievalResult> result;
	
}
