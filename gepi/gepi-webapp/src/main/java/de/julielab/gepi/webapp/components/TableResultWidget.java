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
import de.julielab.gepi.core.retrieval.data.Gene;

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
		tableModel.include("firstArgumentTextWithPreferredName", "secondArgumentTextWithPreferredName", "sentence");
		tableModel.get("firstArgumentTextWithPreferredName").label("gene A");
		tableModel.get("secondArgumentTextWithPreferredName").label("gene B");
	}

	void onUpdateTableData() {
		try {
				beanEvents = persistResult.get().getEventList().stream().map(e -> new BeanModelEvent(e))
				.collect(Collectors.toList());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	public static class BeanModelEvent extends Event {

		public BeanModelEvent(Event event) {
			this.arguments = event.getArguments();
			this.highlightedSentence = event.getHighlightedSentence();
			this.sentence = event.getSentence();
			this.numDistinctArguments = event.getNumDistinctArguments();
			this.allEventTypes = event.getAllEventTypes();
			this.mainEventType = event.getMainEventType();
		}

		public String getFirstArgumentTextWithPreferredName() {
			Gene argument = getFirstArgument();
			return argument.getText() + " (" + argument.getPreferredName() + ")";
		}

		public String getSecondArgumentTextWithPreferredName() {
			Gene argument = getSecondArgument();
			if (null != argument)
				return argument.getText() + " (" + argument.getPreferredName() + ")";
			return "";
		}
	}
}
