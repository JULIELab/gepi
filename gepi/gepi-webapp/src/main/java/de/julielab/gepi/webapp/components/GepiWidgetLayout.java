package de.julielab.gepi.webapp.components;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.Optional;

import de.julielab.gepi.core.retrieval.data.AggregatedEventsRetrievalResult;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Log;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SupportsInformalParameters;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.webapp.pages.Index;
import org.slf4j.Logger;

@Import(stylesheet = {"context:css-components/gepiwidgetlayout.css"})
@SupportsInformalParameters
final public class GepiWidgetLayout {
 
    @Inject
    private Logger log;

    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    @Property
    private String widgettitle;

    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    @Property
    private String clientId;

    @Parameter(defaultPrefix = BindingConstants.LITERAL, name = "class")
    @Property
    private String classes;

    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    @Property
    private String sizeClass;
    @Parameter(name = "viewMode")
    @Property
    private String viewModeParam;
    @Parameter(value = "false")
    @Property
    private boolean useTapestryZoneUpdates;
    @InjectComponent
    private Zone widgetZone;
    @Inject
    private AjaxResponseRenderer ajaxResponseRenderer;
    @Inject
    private ComponentResources resources;
    @Environmental
    private JavaScriptSupport javaScriptSupport;
  
    @Persist
    @Property
    private String viewMode;
    @InjectPage
    private Index index;

    void setupRender() {
        if (getEsResult() == null)
            viewMode = null;
        if (viewMode == null)
            viewMode = ViewMode.OVERVIEW.name().toLowerCase();
    }

    void afterRender() {
        if (useTapestryZoneUpdates) {
            JSONObject widgetSettings = getWidgetSettings();
            javaScriptSupport.require("gepi/components/widgetManager").invoke("addWidget")
                    .with(clientId, widgetSettings);
        }
    }

    public CompletableFuture<EventRetrievalResult> getEsResult() {
        return index.getEsResult();
    }

    public CompletableFuture<AggregatedEventsRetrievalResult> getNeo4jResult() {
        return index.getNeo4jResult();
    }

    /**
     * To be used by concrete Widget classes.
     *
     * @return
     */
    public JSONObject getWidgetSettings() {
        Link toggleViewModeEventLink = resources.createEventLink("toggleViewMode");
        Link refreshContentEventLink = resources.createEventLink("refreshContent");
        JSONObject widgetSettings = new JSONObject();
        widgetSettings.put("handleId", getResizeHandleId());
        widgetSettings.put("widgetId", clientId);
        widgetSettings.put("toggleViewModeUrl", toggleViewModeEventLink.toAbsoluteURI());
        widgetSettings.put("refreshContentsUrl", refreshContentEventLink.toAbsoluteURI());
        widgetSettings.put("zoneElementId", widgetZone.getClientId());
        widgetSettings.put("useTapestryZoneUpdates", useTapestryZoneUpdates);
        return widgetSettings;
    }

    public boolean isDownload() {
        return clientId.equals("tableresult_widget");
    }

    public boolean isRenderBody() {
        // For widgets completely managed from JavaScript (sankey, pie), just render their basics because
        // the rest will be done in JS.
        // For Tapestry components
        return !useTapestryZoneUpdates || isResultAvailable();
    }

    public boolean isResultLoading() {
        if (getEsResult() != null && !getEsResult().isDone()) {
            return true;
        }
        return getEsResult() != null && !getEsResult().isDone();
    }

    public boolean isResultAvailable() {
        if (getEsResult() != null && getNeo4jResult().isDone())
            return true;
        return getEsResult() != null && getEsResult().isDone();
    }

    void onRefreshContent() throws InterruptedException, ExecutionException {
        // If there is data from Neo4j, use that.
        if (getEsResult() != null && getNeo4jResult() == null) {
            log.debug("Waiting for ElasticSearch to return its results.");
            getEsResult().get();
            log.debug("ES result finished.");
        }
        else if (getNeo4jResult() != null) {
            log.debug("Waiting for Neo4j to return its results.");
            getNeo4jResult().get();
        }
        ajaxResponseRenderer.addRender(widgetZone);
    }

    public ViewMode viewMode() {
        return ViewMode.valueOf(viewMode.toUpperCase());
    }

    void onToggleViewMode() {
        switch (viewMode) {
            case "fullscreen":
                break;
            case "large":
                viewMode = ViewMode.OVERVIEW.name().toLowerCase();
                index.setHasLargeWidget(false);
                break;
            case "overview":
                viewMode = ViewMode.LARGE.name().toLowerCase();
                index.setHasLargeWidget(true);
                break;
        }
        ajaxResponseRenderer.addRender(widgetZone);
    }

    void onLoad() {
        if (useTapestryZoneUpdates) {
            javaScriptSupport.require("gepi/components/widgetManager").invoke("refreshWidget")
                    .with(clientId);
        }
    }

    public String getZoneId() {
        String zoneId = "widgetzone_" + clientId;
        return zoneId;
    }

    public String getResizeHandleId() {
        return clientId + "_resize";
    }

    @Log
    public boolean isLarge() {
        return viewMode.equals(ViewMode.LARGE.name().toLowerCase());
    }

    /**
     * Pressing the Download Link/Button for the Table View
     */
    @Log
    StreamResponse onActionFromDownload() {
        if (!getEsResult().isDone()) {
            //TODO: how to handle case when download button is clicked, but the request is not yet fully done

        }
        return new StreamResponse() {
            private final String[] HEADER = new String[]{
                    "Gene1 Name", "Gene1 EntrezID", "Gene1 PreferredName",
                    "Gene2 Name", "Gene2 EntrezID", "Gene2 PreferredName",
                    "Medline ID", "PMC ID", "Event type", "Sentence"
            };
            private InputStream inputStream;
            private String delim = "\t";
            private String finame = "gepi_table";
            private String ext = "xls";

            @Override
            public void prepareResponse(Response response) {
                EventRetrievalResult eResult = null;
                try {
                    eResult = getEsResult().get();
                } catch (InterruptedException | ExecutionException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                if (ext == "xls") {
                    Workbook tableresultxls = createXLS(eResult);
                    try {
                        tableresultxls.write(outputStream);
                        inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else {
                    String tableresultcsv = createCSV(eResult);
                    try {
                        String output = "";
                        output = tableresultcsv;
                        outputStream.write(output.getBytes());
                        inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    response.setHeader("Content-Length", "" + outputStream.size()); // output into file
                    response.setHeader("Content-Length", "" + inputStream.available());
                    response.setHeader("Content-disposition", "attachment; filename=" + finame + "." + ext);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            @Override
            public InputStream getStream() throws IOException {
                return inputStream;
            }

            @Override
            public String getContentType() {
                if (ext == "xls") {
                    return "application/vnd.ms-excel";
                } else {
                    return "text/csv";
                }
            }

            private String createLine(String[] argList) {
                Optional<String> singleLine = Stream
                        .of(argList)
                        .reduce((x, y) -> x + delim + y);
                if (singleLine.isPresent()) {
                    return singleLine.get() + System.getProperty("line.separator");
                }
                return System.getProperty("line.separator");
            }

            private String createCSV(EventRetrievalResult eResult) {
                String header = createLine(HEADER);
                StringBuilder sResult = new StringBuilder(header);

                for (Event e : eResult.getEventList()) {
                    Argument firstArgument = e.getFirstArgument();
                    Argument secondArgument = e.getSecondArgument();
                    String line = createLine(new String[]{
                            firstArgument.getText() != null ? firstArgument.getText() : "",
                            firstArgument.getGeneId() != null ? firstArgument.getGeneId() : "",
                            firstArgument.getPreferredName() != null ? firstArgument.getPreferredName() : "",
                            secondArgument.getText() != null ? secondArgument.getText() : "",
                            secondArgument.getGeneId() != null ? secondArgument.getGeneId() : "",
                            secondArgument.getPreferredName() != null ? secondArgument.getPreferredName() : "",
                            e.getDocumentType().toLowerCase().equals("medline") ? e.getEventId() : "",
                            e.getDocumentType().toLowerCase().equals("pmc") ? e.getEventId() : "",
                            e.getMainEventType() != null ? e.getMainEventType() : "",
                            e.getSentence() != null ? e.getSentence().replaceAll("\\R", " ") : "",
                    });
                    sResult.append(line);
                }
                ;

                return sResult.toString();
            }

            private Workbook createXLS(EventRetrievalResult eResult) {
                Workbook wb = new HSSFWorkbook();
                Sheet sheet = wb.createSheet("results");
                int rowNum = 0;
                int cellNum = 0;
                Row curRow = sheet.createRow(rowNum);
                for (String h : HEADER) {
                    Cell cell = curRow.createCell(cellNum);
                    CellStyle cs = wb.createCellStyle();
                    Font f = wb.createFont();

                    f.setBold(true);
                    cs.setFont(f);
                    cell.setCellStyle(cs);
                    cell.setCellValue(h);
                    cellNum++;
                }

                for (Event e : eResult.getEventList()) {
                    rowNum++;
                    Argument firstArgument = e.getFirstArgument();
                    Argument secondArgument = e.getSecondArgument();
                    curRow = sheet.createRow(rowNum);
                    curRow.createCell(0).setCellValue(firstArgument.getText() != null ? firstArgument.getText() : "");
                    curRow.createCell(1).setCellValue(firstArgument.getGeneId() != null ? firstArgument.getGeneId() : "");
                    curRow.createCell(2).setCellValue(firstArgument.getPreferredName() != null ? firstArgument.getPreferredName() : "");
                    curRow.createCell(3).setCellValue(secondArgument.getText() != null ? secondArgument.getText() : "");
                    curRow.createCell(4).setCellValue(secondArgument.getGeneId() != null ? secondArgument.getGeneId() : "");
                    curRow.createCell(5).setCellValue(secondArgument.getPreferredName() != null ? secondArgument.getPreferredName() : "");
                    curRow.createCell(6).setCellValue(e.getDocumentType().toLowerCase().equals("medline") ? e.getEventId() : "");
                    curRow.createCell(7).setCellValue(e.getDocumentType().toLowerCase().equals("pmc") ? e.getEventId() : "");
                    curRow.createCell(8).setCellValue(e.getMainEventType() != null ? e.getMainEventType() : "");
                    curRow.createCell(9).setCellValue(e.getSentence() != null ? e.getSentence().replaceAll("\\R", " ") : "");
                }
                return wb;
            }
        };
    }

    public enum ViewMode {
        /**
         * The widget is in its overview mode, shown in juxtaposition to other widgets.
         */
        OVERVIEW,
        /**
         * The widget covers the main view area of GePi, hiding other widgets.
         */
        LARGE,
        /**
         * The widget is in fullscreen mode, covering the complete computer screen.
         */
        FULLSCREEN
    }
}
