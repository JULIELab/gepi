package de.julielab.gepi.webapp.components;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.beaneditor.BeanModel;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.BeanModelSource;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import org.slf4j.Logger;

public class TableResultWidget extends GepiWidget {

	@Inject
	private Logger log;

    @Property
    private String viewMode;

	@Property
	private BeanModelEvent eventRow;

	@Property
	@Persist
	private List<BeanModelEvent> beanEvents;

	@Inject
	private BeanModelSource beanModelSource;

	@Inject
	private Messages messages;

	@Inject
    private ComponentResources resources;
	
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
			beanEvents = persistEsResult.get().getEventList().stream()
					.map(e -> new BeanModelEvent(e))
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
			return event.getPmid() != null ?  event.getPmid() : "";
		}
		
		public String getPmcId() {
			return event.getPmcid() != null ? event.getPmcid() : "";
		}
		
		public String getFirstArgumentText() {
			return event.getFirstArgument().getText();
		}

		public String getFirstArgumentGeneId() { return event.getFirstArgument().getGeneId(); }

		public String getSecondArgumentText() {
			return event.getSecondArgument().getText();
		}

        public String getSecondArgumentGeneId() { return event.getSecondArgument().getGeneId(); }
		
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

	public int getRowsPerPage() {
		return 5;
	}
}
