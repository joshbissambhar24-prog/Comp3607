package edu.uwi.bluejtelemetry.export;

import edu.uwi.bluejtelemetry.core.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class ExportServiceTest {
    @TempDir Path dir;
    @Test void savesBothFormatsWithoutLeavingTempFiles() throws Exception {
        Instant t = Instant.parse("2026-11-07T12:00:00Z");
        var session = new SessionSnapshot("12345678-id","project","student",t,t.plusSeconds(2),List.of());
        MetricsAggregator aggregator = new MetricsAggregator();
        ExportService service = new ExportService(List.of(
            new JsonReportExporter(aggregator), new PdfReportExporter(aggregator)));
        try {
            List<Path> files = service.export(session,dir).get(15,TimeUnit.SECONDS);
            assertEquals(2,files.size());
            assertTrue(files.stream().allMatch(Files::exists));
            try (var stream = Files.list(dir)) {
                assertEquals(2,stream.count());
            }
        } finally { service.shutdownAndFlush(); }
    }
}
