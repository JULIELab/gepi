package de.julielab.gepi.webapp.components;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.GepiConceptInfo;
import de.julielab.gepi.core.retrieval.data.InputMode;
import de.julielab.gepi.core.retrieval.services.EventRetrievalService;
import de.julielab.gepi.core.retrieval.services.IEventRetrievalService;
import de.julielab.gepi.core.services.GeneIdService;
import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.gepi.core.services.IGeneIdService;
import de.julielab.gepi.webapp.BeanModelEvent;
import de.julielab.gepi.webapp.EventPagesDataSource;
import de.julielab.gepi.webapp.base.TabPersistentField;
import de.julielab.gepi.webapp.data.EventTypes;
import de.julielab.java.utilities.FileUtilities;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.beanmodel.BeanModel;
import org.apache.tapestry5.beanmodel.services.BeanModelSource;
import org.apache.tapestry5.commons.Messages;
import org.apache.tapestry5.http.Link;
import org.apache.tapestry5.http.services.Response;
import org.apache.tapestry5.ioc.LoggerSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Matcher;

import static de.julielab.gepi.core.services.GeneIdService.HGNCG_PATTERN;

@Import(stylesheet = {"context:css-components/tablewidget.css"})
public class TableResultWidget extends GepiWidget {
    public static final int ROWS_PER_PAGE = 10;
    @Parameter
    protected EnumSet<InputMode> inputMode;
    @Property
    List<EventTypes> eventTypes = List.of(EventTypes.values());
    @Property
    EventTypes filterEventType;
    @Property
    String filterArg1Symbol;
    @Property
    String filterArg1Name;
    @Property
    String filterArg1Id;
    @Property
    String filterArg2Symbol;
    @Property
    String filterArg2Name;
    @Property
    String filterArg2Id;
    @Inject
    private Logger log;
    @Property
    private String viewMode;
    @Property
    private BeanModelEvent eventRow;
    @Inject
    private BeanModelSource beanModelSource;
    @Inject
    private Messages messages;
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
    @Inject
    private LoggerSource loggerSource;
    @Environmental
    private JavaScriptSupport javaScriptSupport;

//    @Property
//    private List<String> selectedColumns;
    @Inject
    private IGeneIdService geneIdService;
//    @Inject
//    private TypeCoercer typeCoercer;
//
//    public ValueEncoder getColumnsEncoder() {
//        return new StringValueEncoder();
//    }
//
//    public SelectModel getColumnsModel() {
//        return new SelectModelImpl(tableModel.getPropertyNames().stream().map(p -> new OptionModelImpl(p)).toArray(OptionModel[]::new));
//    }

//    @Inject
//    private AjaxResponseRenderer ajaxResponseRenderer;
//
//    @InjectComponent
//    private GepiWidgetLayout gepiWidgetLayout;
//
    // for columns selection
//    public void onSuccessFromColumnsForm() {
//        tableModel.include(selectedColumns.toArray(String[]::new));
//        ajaxResponseRenderer.addRender(gepiWidgetLayout.getBodyZone());
//    }

    @Log
    void setupRender() {
        getEventSource();
        List<String> availableColumns = new ArrayList<>(List.of("firstArgumentPreferredName",
                "secondArgumentPreferredName",
//                "firstArgumentText",
//                "secondArgumentText",
                "firstArgumentGeneId",
                "secondArgumentGeneId",
//                "firstArgumentMatchType",
//                "secondArgumentMatchType",
                "allEventTypes",
                "factuality",
                "fulltextMatchSource",
                "docId",
//                "eventId",
                "context"
//                ,
//                "geneMappingSources"
        ));
        if (inputMode != null && !inputMode.contains(InputMode.FULLTEXT_QUERY))
            availableColumns.remove("fulltextMatchSource");

        tableModel = beanModelSource.createDisplayModel(BeanModelEvent.class, messages);
        tableModel.include(availableColumns.toArray(new String[0]));

        tableModel.get("firstArgumentPreferredName").label("Gene A Symbol");
        tableModel.get("secondArgumentPreferredName").label("Gene B Symbol");
//        tableModel.get("firstArgumentText").label("Gene A Text");
//        tableModel.get("secondArgumentText").label("Gene B Text");
        tableModel.get("firstArgumentGeneId").label("Gene A Gene ID");
        tableModel.get("secondArgumentGeneId").label("Gene B Gene ID");
//        tableModel.get("firstArgumentMatchType").label("gene A match type");
//        tableModel.get("secondArgumentMatchType").label("gene B match type");
        tableModel.get("allEventTypes").label("Relation Types");
        tableModel.get("docId").label("Document ID");
//        tableModel.get("eventId").label("event id");
//        tableModel.get("geneMappingSources").label("gene tagger");
        // Disable the sorting buttons. Since we reorder the event arguments so that arguments from list A
        // always appear as the "first" argument, we cannot sort in ElasticSearch because there is no fixed
        // field we could sort on for the gene arguments. Other columns would be possible to sort on but
        // this was never a user request so just leave it for now.
        for (String property : tableModel.getPropertyNames()) {
            tableModel.get(property).sortable(false);
        }
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
//        selectedColumns = tableModel.getPropertyNames();
    }

    /**
     * When the form containing the filter elements is submitted, we want to re-render the table via AJAX
     */
//    void onValidateFromFilterCriteria() {
//        if (request.isXHR()) {
//            ajaxResponseRenderer.addRender(tableZone);
//        }
//    }
    public EventPagesDataSource getEventSource() {
//        FilteredGepiRequestData filteredRequest = new FilteredGepiRequestData(requestData);
//        filteredRequest.setEventTypeFilter(filterEventType);
        return new EventPagesDataSource(loggerSource.getLogger(EventPagesDataSource.class), dataService.getData(requestData.getDataSessionId()).getPagedResult(), eventRetrievalService, geneIdService, requestData);
    }

    void onUpdateTableData() {
        log.debug("Waiting for table data.");
//            beanEvents = getEsResult().get().getEventList().stream()
//                    .map(e -> new BeanModelEvent(e))
//                    .collect(Collectors.toList());
        log.debug("Table data was loaded.");

    }

    public int getRowsPerPage() {
        return EventRetrievalService.DEFAULT_PAGE_SIZE;
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

            private Path statisticsFile;

            @Override
            public void prepareResponse(Response response) {
                try {
                    Future<EventRetrievalResult> unrolledResult4download = getUnrolledResult4download();
                    // Check if we have the download data cached. Otherwise, get it and cache it
                    if (unrolledResult4download == null) {
                        long time = System.currentTimeMillis();
                        log.info("[{}] Retrieving unrolled result for Excel sheet creation.", requestData.getDataSessionId());
                        unrolledResult4download = eventRetrievalService.getEvents(requestData, 0, Integer.MAX_VALUE, false);
                        // We use a weak reference for the complete data since it requires much memory because of all
                        // the context data. The GC should be able to evict it, if necessary.
                        dataService.getData(requestData.getDataSessionId()).setUnrolledResult4download(new WeakReference<>(unrolledResult4download));
                        time = System.currentTimeMillis() - time;
                        log.info("[{}] Unrolled result retrieval for Excel sheet creation took {} seconds", requestData.getDataSessionId(), time / 1000);
                    }
                    statisticsFile = dataService.getOverviewExcel(unrolledResult4download, requestData.getDataSessionId(), requestData.getInputMode(), requestData.getSentenceFilterString(), requestData.getParagraphFilterString(), requestData.getSectionNameFilterString());

                    response.setHeader("Content-Length", "" + Files.size(statisticsFile)); // output into file
                    response.setHeader("Content-disposition", "attachment; filename=" + statisticsFile.getFileName());
                } catch (Exception e) {
                    log.error("Could not create Excel result for dataSessionId {}", requestData.getDataSessionId(), e);
                }
            }

            @Override
            public InputStream getStream() throws IOException {
                return FileUtilities.getInputStreamFromFile(statisticsFile.toFile());
            }

            @Override
            public String getContentType() {
                return "application/vnd.ms-excel";
            }
        };
    }

    public String getArgumentLink(int argPosition) {
        Argument argument = argPosition == 1 ? eventRow.getEvent().getFirstArgument() : eventRow.getEvent().getSecondArgument();
        String conceptId = argument.getConceptId();
        String originalId = argument.getGeneId();
        // Retrieving the gene info for each argument in sequence is inefficient. Thus, the info has been pre-fetched in
        // EventPagesDataSource and is now quickly accessed through the cache.
        GepiConceptInfo targetInfo = conceptId.equals(EventRetrievalService.FIELD_VALUE_MOCK_ARGUMENT) || !GeneIdService.CONCEPT_ID_PATTERN.matcher(conceptId).matches() ? null : geneIdService.getGeneInfo(List.of(conceptId)).get(conceptId);
        if (targetInfo == null)
            return "#";
        if (targetInfo.getLabels().contains("HGNC_GROUP") || targetInfo.getLabels().contains("AGGREGATE_FPLX_HGNC")) {
            // for groups that appear in HGNC and FamPlex, we prefer the HGNC ID since we can link to it
            final Matcher m = HGNCG_PATTERN.matcher(originalId);
            if(!m.find())
                // this should not happen
                return "#";
            return "https://www.genenames.org/data/genegroup/#!/group/" + m.group(2);
        }
        if (targetInfo.getLabels().contains("FPLX"))
            return "https://github.com/sorgerlab/famplex/";
        if (targetInfo.getLabels().contains("ID_MAP_NCBI_GENES"))
            return "https://www.ncbi.nlm.nih.gov/gene/" + originalId;
        return "#";
    }

    public void afterRender() {
        final Link downloadEventLink = resources.createEventLink("download");
        javaScriptSupport.require("gepi/charts/tablewidget").invoke("download").with(downloadEventLink.toAbsoluteURI());
        javaScriptSupport.require("gepi/charts/tablewidget").invoke("setupHighlightTooltips");
        javaScriptSupport.require("gepi/base").invoke("setuptooltips");
    }
}
