package de.julielab.gepi.webapp.services;

import de.julielab.gepi.core.GepiCoreSymbolConstants;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;

/**
 * <p>
 * GePI creates Excel files for result download. Those are put into the temporary directory and should be evicted
 * by the operating system anyway. But to be on the safe side and go easy on our resources, we delete them
 * ourselves, probably sooner than the OS would.
 * </p>
 */
public class TempFileCleaner implements ITempFileCleaner{
    private Logger log;
    private Clock clock;
    private Path gepiTmpDir;
    private String excelFilePrefix;

    public TempFileCleaner(Logger log, Clock clock, @Symbol(GepiCoreSymbolConstants.GEPI_TMP_DIR) Path gepiTmpDir, @Symbol(GepiCoreSymbolConstants.GEPI_EXCEL_FILE_PREFIX) String excelFilePrefix) {
        this.log = log;
        this.clock = clock;
        this.gepiTmpDir = gepiTmpDir;
        this.excelFilePrefix = excelFilePrefix;
    }

    /**
     * <p>Removed temporary files according to their specific life cycle policy.</p>
     * <p>Currently, temporary files that are cleaned after some time are
     * <ul>
     *     <li>Excel download files</li>
     * </ul></p>
     */
    public void cleanup() {
        cleanExcelDownloadFiles();
    }

    private void cleanExcelDownloadFiles() {
        if (Files.exists(gepiTmpDir)) {
            try {
                final Iterator<Path> excelFileIt = Files.list(gepiTmpDir).filter(p -> p.getFileName().toString().startsWith(excelFilePrefix)).iterator();
                while (excelFileIt.hasNext()) {
                    Path excelFile = excelFileIt.next();
                    final Instant lastModifiedTime = Files.getLastModifiedTime(excelFile).toInstant();
                    if (Duration.between(lastModifiedTime, clock.instant()).toHours() > 24) {
                        log.info("Removing excel download file {} because it is older than a day", excelFile);
                        Files.delete(excelFile);
                    }
                }
            } catch (IOException e) {
                log.error("Could not list Excel download files in temporary directory.", e);
            }
        }
    }
}
