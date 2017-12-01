package de.julielab.gepi.webapp.components;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.beaneditor.BeanModel;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.BeanModelSource;

import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.Argument;

public class TableResultWidget extends GepiWidget {
	@Property
	private BeanModelEvent eventRow;

	@Property
	@Persist
	private List<BeanModelEvent> beanEvents;

	@Inject
	private BeanModelSource beanModelSource;

	@Inject
	private Messages messages;

	@Property
	@Persist
	private BeanModel<BeanModelEvent> tableModel;

	void setupRender() {
		super.setupRender();
		tableModel = beanModelSource.createDisplayModel(BeanModelEvent.class, messages);
		tableModel.include("firstArgumentText", "firstArgumentPreferredName",
				"secondArgumentText", "secondArgumentPreferredName",
				"medlineId", "pmcId", "mainEventType", "sentence");
		tableModel.get("firstArgumentText").label("gene A text");
		tableModel.get("firstArgumentPreferredName").label("gene A symbol");
		tableModel.get("secondArgumentText").label("gene B text");
		tableModel.get("secondArgumentPreferredName").label("gene B symbol");
		tableModel.get("medlineId").label("medline id");
		tableModel.get("pmcId").label("pmc id");
		tableModel.get("mainEventType").label("event type");
	}

	void onUpdateTableData() {
		try {
				beanEvents = persistResult.get().getEventList().stream().map(e -> new BeanModelEvent(e))
				.collect(Collectors.toList());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	public static class BeanModelEvent {

		private Event event;

		public BeanModelEvent(Event event) {
			this.event = event;
		}
		
		public String getMedlineId() {
			return event.getDocumentType().toLowerCase().equals("medline") ? event.getDocumentId(): "";
		}
		
		public String getPmcId() {
			return event.getDocumentType().toLowerCase().equals("pmc") ? event.getDocumentId(): "";
		}
		
		public String getFirstArgumentText() {
			return event.getFirstArgument().getText();
		}
		
		public String getSecondArgumentText() {
			return event.getSecondArgument().getText();
		}
		
		public String getFirstArgumentPreferredName() {
			return event.getFirstArgument().getPreferredName();
		}
		
		public String getSecondArgumentPreferredName() {
			return event.getSecondArgument().getPreferredName();
		}

		public String getMainEventType() {
			return event.getMainEventType();
		}
		
		public String getFirstArgumentTextWithPreferredName() {
			Argument argument = event.getFirstArgument();
			return argument.getText() + " (" + argument.getPreferredName() + ")";
		}
				
		public String getSentence() {
			return event.getSentence();
		}

		public String getSecondArgumentTextWithPreferredName() {
			Argument argument = event.getSecondArgument();
			if (null != argument)
				return argument.getText() + " (" + argument.getPreferredName() + ")";
			return "";
		}
	}
}
