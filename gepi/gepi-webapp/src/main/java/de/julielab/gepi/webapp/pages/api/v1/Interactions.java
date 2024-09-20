package de.julielab.gepi.webapp.pages.api.v1;

import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.core.retrieval.data.GepiRequestData;
import de.julielab.gepi.core.retrieval.services.IEventRetrievalService;
import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.gepi.webapp.base.TabPersistentField;
import de.julielab.gepi.webapp.components.GepiInput;
import de.julielab.gepi.webapp.components.TableResultWidget;
import de.julielab.gepi.webapp.data.GepiQueryParameters;
import de.julielab.gepi.webapp.pages.Index;
import de.julielab.java.utilities.FileUtilities;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.http.services.Request;
import org.apache.tapestry5.http.services.Response;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.apache.tapestry5.services.HttpStatus;
import org.apache.tapestry5.util.TextStreamResponse;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;

public class Interactions {

    @Inject
    private Logger log;

    @Inject
    private ApplicationStateManager asm;

    @Inject
    private Request request;

    @Inject
    private IGePiDataService dataService;

    @Inject
    private IEventRetrievalService eventRetrievalService;

    @InjectPage
    private Index index;

    @Property
    @Persist(TabPersistentField.TAB)
    private GepiRequestData requestData;

    @Property
    @Persist(TabPersistentField.TAB)
    private long dataSessionId;

    @OnEvent(EventConstants.HTTP_GET)
    Object onHttpGet() {
        if (requestData == null) {
            dataSessionId = dataService.newSession();
            log.debug("Current dataSessionId is 0, initializing GePi session with ID {}", dataSessionId);
            log.debug("No session");
        }

        final GepiQueryParameters gepiQueryParameters = new GepiQueryParameters(request);
        if (gepiQueryParameters.isValidRequest()) {
            log.info("Received valid query parameters for GePI search.");
            GepiInput gepiInput = index.getGepiInput();
            TableResultWidget tableResultWidget = index.getTableResultWidget();
            gepiInput.executeSearch(gepiQueryParameters, dataSessionId);
            if (requestData == null)
                requestData = gepiInput.getRequestData();
            switch (gepiQueryParameters.getFormat()) {
                case "excel":
                    return tableResultWidget.onDownload();
                case "tsv":
                    final Future<EventRetrievalResult> unrolledRetrievalResult = dataService.getUnrolledResult4download(requestData, eventRetrievalService);
                    try {
                        final EventRetrievalResult eventRetrievalResult = unrolledRetrievalResult.get();
                        final List<Event> eventList = eventRetrievalResult.getEventList();
                        final Path tsvFile = dataService.writeOverviewTsvFile(eventList, requestData.getDataSessionId());
                        return new TextStreamResponse("text/csv", "UTF-8") {

                            @Override
                            public void prepareResponse(Response response) {
                                try {
                                    response.setHeader("Content-Length", "" + Files.size(tsvFile)); // output into file
                                    response.setHeader("Content-disposition", "attachment; filename=" + tsvFile.getFileName());
                                } catch (Exception e) {
                                    log.error("Could not create TSV result for dataSessionId {}", requestData.getDataSessionId(), e);
                                }
                            }

                            @Override
                            public InputStream getStream() throws IOException {
                                return FileUtilities.getInputStreamFromFile(tsvFile.toFile());
                            }

                            @Override
                            public String getContentType() {
                                return "text/csv";
                            }
                        };
                    } catch (Exception e) {
                        log.error("Could not serve TSV file due to Exception", e);
                        JSONObject response = new JSONObject("status", 500, "errorMessage", "Internal Server Error");
                        return new HttpStatus(500, response.toString());
                    }
                default:
                    JSONObject response = new JSONObject("status", HttpStatus.badRequest().getStatusCode(), "errorMessage", "Unsupported return format '" + gepiQueryParameters.getFormat() + "'. Valid values are 'excel' and 'tsv'.");
                    return new HttpStatus(HttpStatus.badRequest().getStatusCode(), response.toString());
            }
        } else {
            log.debug("Query parameters did not contain a valid GePI search.");
        }
        JSONObject response = new JSONObject("status", HttpStatus.badRequest().getStatusCode(), "errorMessage", "Not a valid GePI request. Refer to the help page of the GePI Web application.");
        return new HttpStatus(HttpStatus.badRequest().getStatusCode(), response.toString());
    }
}
