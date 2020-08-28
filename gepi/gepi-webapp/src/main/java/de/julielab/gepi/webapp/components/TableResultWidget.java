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
        tableModel = beanModelSource.createDisplayModel(BeanModelEvent.class, messages);
        tableModel.include(
                "firstArgumentPreferredName",
                "secondArgumentPreferredName",
                "firstArgumentText",
                "secondArgumentText",
                "firstArgumentGeneId",
                "secondArgumentGeneId",
                "firstArgumentMatchType",
                "secondArgumentMatchType",
                "allEventTypes",
                "docId",
                "sentence");
        tableModel.get("firstArgumentPreferredName").label("gene A symbol");
        tableModel.get("secondArgumentPreferredName").label("gene B symbol");
        tableModel.get("firstArgumentText").label("gene A text");
        tableModel.get("secondArgumentText").label("gene B text");
        tableModel.get("firstArgumentGeneId").label("gene A gene ID");
        tableModel.get("secondArgumentGeneId").label("gene B gene ID");
        tableModel.get("firstArgumentMatchType").label("gene A match type");
        tableModel.get("secondArgumentMatchType").label("gene B match type");
        tableModel.get("allEventTypes").label("relation types");
        tableModel.get("docId").label("document id");
    }

    void onUpdateTableData() {
        try {
            log.debug("Waiting for table data.");
            beanEvents = getEsResult().get().getEventList().stream()
                    .map(e -> new BeanModelEvent(e))
                    .collect(Collectors.toList());
            log.debug("Table data was loaded.");

        } catch (InterruptedException | ExecutionException e) {
            log.error("Exception occurred when trying to access ES event results.", e);
        } catch (NullPointerException e) {
            log.error("NPE occurred when trying to access ES event results. The persistentEsResult is: {}", getEsResult());
            throw e;
        }
    }

    public int getRowsPerPage() {
        return 10;
    }

    public String getDocumentUrl() {
        String docId = eventRow.getDocId();
        if (docId.startsWith("PMC"))
            return "https://www.ncbi.nlm.nih.gov/pmc/articles/" + docId;
        else
            return "https://www.ncbi.nlm.nih.gov/pubmed/" + docId;
    }

    public String getArticleReferenceTitle() {
        if (eventRow.getDocId().contains("PMC"))
            return "Open in PubmedCentral";
        return "Open in Pubmed";
    }


    public static class BeanModelEvent {

        private Event event;

        public BeanModelEvent(Event event) {
            this.event = event;
        }

        public String getDocId() {
            if (event.getEventId().startsWith("pmc")) return "PMC" + event.getPmcid();
            else if (event.getPmid() != null) return event.getPmid();
            throw new IllegalStateException("No document ID for event " + event);
        }

        public String getFirstArgumentText() {
            return event.getFirstArgument().getText();
        }

        public String getFirstArgumentGeneId() {
            return event.getFirstArgument().getGeneId();
        }

        public String getSecondArgumentText() {
            return event.getSecondArgument().getText();
        }

        public String getSecondArgumentGeneId() {
            return event.getSecondArgument().getGeneId();
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

        public String getFirstArgumentMatchType() {
            return event.getFirstArgument().getMatchType();
        }

        public String getSecondArgumentMatchType() {
            return event.getSecondArgument().getMatchType();
        }

        public String getFirstArgumentTextWithPreferredName() {
            Argument argument = event.getFirstArgument();
            return argument.getText() + " (" + argument.getPreferredName() + ")";
        }

        public String getAllEventTypes() {
            return String.join(", ", event.getAllEventTypes());
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
