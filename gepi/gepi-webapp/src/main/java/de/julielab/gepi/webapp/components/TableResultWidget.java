package de.julielab.gepi.webapp.components;

import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.beaneditor.BeanModel;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.BeanModelSource;

import de.julielab.gepi.core.retrieval.data.Event;

public class TableResultWidget extends GepiWidget {
	@Property
	private Event eventRow;

	@Inject
	private BeanModelSource beanModelSource;

	@Inject
	private Messages messages;

	@Property
	@Persist
	private BeanModel<Event> tableModel;

	void setupRender() {
		super.setupRender();
		tableModel = beanModelSource.createDisplayModel(Event.class, messages);
		tableModel.include("firstArgumentGeneId", "secondArgumentGeneId", "mainEventType", "sentence");
	}
}
