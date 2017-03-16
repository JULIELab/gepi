package de.julielab.gepi.webapp.components;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;

public class GepiWidget {
	@Parameter(defaultPrefix=BindingConstants.LITERAL)
	@Property
	private String widgettitle;
	
	@Parameter(defaultPrefix=BindingConstants.LITERAL)
	@Property
	private String clientId;
	
}
