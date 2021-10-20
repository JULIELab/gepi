package de.julielab.gepi.webapp.components;

import java.io.*;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.InputMode;
import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.gepi.webapp.base.TabPersistentField;
import de.julielab.java.utilities.FileUtilities;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.Log;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.beanmodel.BeanModel;
import org.apache.tapestry5.beanmodel.services.BeanModelSource;
import org.apache.tapestry5.commons.Messages;
import org.apache.tapestry5.http.services.Response;
import org.apache.tapestry5.ioc.annotations.Inject;

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
    @Persist("tab")
    private List<BeanModelEvent> beanEvents;

    @Inject
    private BeanModelSource beanModelSource;

    @Inject
    private Messages messages;

    @Inject
    private IGePiDataService dataService;

    @Inject
    private ComponentResources resources;

    @Property
    @Persist(TabPersistentField.TAB)
    private BeanModel<BeanModelEvent> tableModel;

    @Property
    @Persist(TabPersistentField.TAB)
    private Format contextFormat;

    void setupRender() {
        List<String> availableColumns = new ArrayList<>(List.of("firstArgumentPreferredName",
                "secondArgumentPreferredName",
                "firstArgumentText",
                "secondArgumentText",
                "firstArgumentGeneId",
                "secondArgumentGeneId",
                "firstArgumentMatchType",
                "secondArgumentMatchType",
                "allEventTypes",
                "fulltextMatchSource",
                "docId",
                "eventId",
                "context"));
        if (inputMode != null && !inputMode.contains(InputMode.FULLTEXT_QUERY))
            availableColumns.remove("fulltextMatchSource");

        tableModel = beanModelSource.createDisplayModel(BeanModelEvent.class, messages);
        tableModel.include(availableColumns.toArray(new String[0]));

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
        tableModel.get("eventId").label("event id");

        contextFormat = new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo.append(obj);
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return source;
            }
        };
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

    /**
     * Pressing the Download Link/Button for the Table View
     */
    @Log
    StreamResponse onDownload(long dataSessionId) {
        return new StreamResponse() {

            private File statisticsFile;

            @Override
            public void prepareResponse(Response response) {
                try {
                    statisticsFile = dataService.getOverviewExcel(getEsResult().get().getEventList(), dataSessionId, inputMode);

                    response.setHeader("Content-Length", "" + statisticsFile.length()); // output into file
                    response.setHeader("Content-disposition", "attachment; filename=" + statisticsFile.getName());
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            @Override
            public InputStream getStream() throws IOException {
                return FileUtilities.getInputStreamFromFile(statisticsFile);
            }

            @Override
            public String getContentType() {
                return "application/vnd.ms-excel";
            }
        };
    }

    public static class BeanModelEvent {

        private Event event;

        public BeanModelEvent(Event event) {
            this.event = event;
        }

        public String getDocId() {
            return event.getDocId();
        }

        public String getEventId() {
            return event.getEventId();
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

        public String getContext() {
            if (event.isParagraphMatchingFulltextQuery() && !event.isSentenceMatchingFulltextQuery())
                return event.getHlParagraph();
            if (event.isSentenceMatchingFulltextQuery())
                return event.getHlSentence();
            return event.getSentence();
        }

        public String getSecondArgumentTextWithPreferredName() {
            Argument argument = event.getSecondArgument();
            if (null != argument)
                return argument.getText() + " (" + argument.getPreferredName() + ")";
            return "";
        }

        public String getFulltextMatchSource() {
            if (event.isSentenceMatchingFulltextQuery())
                return "sentence";
            if (event.isParagraphMatchingFulltextQuery())
                return "paragraph";
            System.out.println(event.getEventId());
            System.out.println(event.getSentence());
            System.out.println(event.getHlSentence());
            System.out.println(event.getParagraph());
            System.out.println(event.getHlParagraph());
            throw new IllegalStateException("The full text match source of event " + event + " was requested but neither the sentence nor the paragraph have a match. Either this is not a fulltext query request or there is an result that actually doesn't match the query.");
        }
    }
}
