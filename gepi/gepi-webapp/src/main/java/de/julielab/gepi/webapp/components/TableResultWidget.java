package de.julielab.gepi.webapp.components;

import de.julielab.gepi.core.retrieval.data.*;
import de.julielab.gepi.core.retrieval.services.IEventRetrievalService;
import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.gepi.webapp.BeanModelEvent;
import de.julielab.gepi.webapp.base.TabPersistentField;
import de.julielab.gepi.webapp.EventPagesDataSource;
import de.julielab.java.utilities.FileUtilities;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.beanmodel.BeanModel;
import org.apache.tapestry5.beanmodel.services.BeanModelSource;
import org.apache.tapestry5.commons.Messages;
import org.apache.tapestry5.corelib.components.Grid;
import org.apache.tapestry5.http.services.Response;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class TableResultWidget extends GepiWidget {

    @Parameter
    protected EnumSet<InputMode> inputMode;
    @Inject
    private Logger log;
    @Property
    private String viewMode;
    @Property
    private BeanModelEvent eventRow;
//    @Property
//    @Persist("tab")
//    private List<BeanModelEvent> beanEvents;
    @Inject
    private BeanModelSource beanModelSource;
    @Inject
    private Messages messages;
    @Property
    private EventPagesDataSource eventSource;
    @Inject
    private IGePiDataService dataService;
    @Inject
    private ComponentResources resources;
    @Parameter
    private String sentenceFilterString;

    @Parameter
    private String paragraphFilterString;

    @Property
    @Persist(TabPersistentField.TAB)
    private BeanModel<BeanModelEvent> tableModel;

    @Property
    @Persist(TabPersistentField.TAB)
    private Format contextFormat;

    @Inject
    private IEventRetrievalService eventRetrievalService;

    @InjectComponent
    private Grid grid;

    void setupRender() {
        eventSource = new EventPagesDataSource(eventRetrievalService, requestData);
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
        log.debug("Waiting for table data.");
//            beanEvents = getEsResult().get().getEventList().stream()
//                    .map(e -> new BeanModelEvent(e))
//                    .collect(Collectors.toList());
        log.debug("Table data was loaded.");

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
    StreamResponse onDownload() {
        return new StreamResponse() {

            private File statisticsFile;

            @Override
            public void prepareResponse(Response response) {
                try {
                    statisticsFile = dataService.getOverviewExcel(getEsResult().get().getEventList(), requestData.getDataSessionId(), requestData.getInputMode(), requestData.getSentenceFilterString(), requestData.getParagraphFilterString(), requestData.getSectionNameFilterString());

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

}
