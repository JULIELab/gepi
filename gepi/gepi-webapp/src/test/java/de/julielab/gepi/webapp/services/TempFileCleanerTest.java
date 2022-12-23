package de.julielab.gepi.webapp.services;

import de.julielab.gepi.core.services.GePiDataService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class TempFileCleanerTest {
    private static Path gepiTmpDir;
    private static String gepiExcelPrefix;

    @BeforeClass
    public static void setup() throws IOException {
        final Random random = new Random();
        gepiTmpDir = Path.of("src/test/resources/", "gepi-tmp-test");
        Files.createDirectories(gepiTmpDir);
        gepiExcelPrefix = GePiDataService.GEPI_EXCEL_FILE_PREFIX_NAME;
    }

    @AfterClass
    public static void shutdown() throws IOException {
        FileUtils.deleteQuietly(gepiTmpDir.toFile());
    }

    @Test
    public void cleanup() throws IOException {

        // Create two test files. One created now, the other in 6 hours from now (the file is from the future!).
        // We will then set the clock now in 25 hours and make sure that the first file is deleted after
        // its 24h time and second is allowed to stay.
        final Path tmpFile1 = Path.of(gepiTmpDir.toString(), gepiExcelPrefix + "1.test");
        Files.createFile(tmpFile1);
        final Path tmpFile2 = Path.of(gepiTmpDir.toString(), gepiExcelPrefix + "2.test");
        Files.createFile(tmpFile2);
        Instant halfDayFromNow = Instant.now().plus(1, ChronoUnit.HALF_DAYS);
        Files.setLastModifiedTime(tmpFile2, FileTime.from(halfDayFromNow));

        // Now let the cleaner run.
        Instant in25h = Instant.now().plus(25, ChronoUnit.HOURS);
        final TempFileCleaner cleaner = new TempFileCleaner(LoggerFactory.getLogger(TempFileCleaner.class), Clock.fixed(in25h, ZoneId.of("UTC")), gepiTmpDir, gepiExcelPrefix);
        assertThatCode(() -> cleaner.cleanup()).doesNotThrowAnyException();
        assertThat(tmpFile1).doesNotExist();
        assertThat(tmpFile2).exists();
    }

}