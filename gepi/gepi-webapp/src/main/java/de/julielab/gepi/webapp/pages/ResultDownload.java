package de.julielab.gepi.webapp.pages;

import de.julielab.gepi.core.services.IGePiDataService;
import de.julielab.java.utilities.FileUtilities;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.http.services.Response;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResultDownload {
    @Inject
    private Logger log;

    @Property
    private long dataSessionId;

    @Inject
    private IGePiDataService dataService;
    @Environmental
    private ComponentResources resources;

    public void onActivate(long dataSessionId) {
        this.dataSessionId = dataSessionId;
    }

    public boolean isSessionResultExists() {
        try {
            return dataService.existsTempStatusFile(dataSessionId);
        } catch (IOException e) {
            log.error("Could not check whether Excel results status file for dataSessionId {} does exists.", dataService, e);
            return false;
        }
    }

    public String getAlertClass() {
        try {
            return dataService.isDownloadExcelFileReady(dataSessionId) ? "alert-success" : "alert-info";
        } catch (IOException e) {
            log.error("Could not check whether Excel result file for dataSessionId {} is ready for determining alert class.", dataSessionId, e);
            return "alert-danger";
        }
    }

    public String getResultFileStatus() {
        try {
            return dataService.getDownloadFileCreationStatus(dataSessionId);
        } catch (IOException e) {
            log.error("Could not retrieve the contents of the download status file.", e);
            return "An internal error has occurred while checking for the download file. Please try again later. If this continues to happen, please send an E-Mail to sas" + "cha" + "." + "scha" + "euble@le" + "ibniz-hki" + "." + "de";
        }
    }

    public boolean isDownloadFileReady() {
        try {
            return dataService.isDownloadExcelFileReady(dataSessionId);
        } catch (IOException e) {
            log.error("Could not check whether Excel result file for dataSessionId {} is ready.", dataSessionId, e);
            return false;
        }
    }


    public Object onDownloadExcelFile(long dataSessionId) {
        log.info("Got dataSessionId {}", dataSessionId);
        try {
            if (!dataService.isDownloadExcelFileReady(dataSessionId))
                return null;
        } catch (IOException e) {
            log.error("Could not check whether Excel result file for dataSessionId {} is ready in order to download it.", dataSessionId, e);
        }
        return new StreamResponse() {

            private Path statisticsFile;

            @Override
            public void prepareResponse(Response response) {
                try {
                    statisticsFile = dataService.getTempXlsDataFile(dataSessionId);
                    response.setHeader("Content-Length", "" + Files.size(statisticsFile)); // output into file
                    response.setHeader("Content-disposition", "attachment; filename=" + statisticsFile.getFileName());
                } catch (Exception e) {
                    log.error("Could not download Excel result for dataSessionId {}", dataSessionId, e);
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

    public ComponentResources getComponentResources() {
        return resources;
    }

}
