package edu.uwi.bluejtelemetry.export;

import edu.uwi.bluejtelemetry.core.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PdfReportExporterTest {
    @TempDir Path temp;
    private static final Instant T = Instant.parse("2026-11-07T12:00:00Z");
    @Test void generatesWellFormedTwoPagePdfWithChartsAndTables() throws Exception {
        var s = new SessionSnapshot("s1","Project","Student",T,T.plusSeconds(720),List.of(
            new TelemetryEvent(T.plusSeconds(10),EventType.COMPILE_STARTED,"X.java",Map.of()),
            new TelemetryEvent(T.plusSeconds(12),EventType.COMPILATION_ERROR,"X.java",Map.of("category","SYNTAX")),
            new TelemetryEvent(T.plusSeconds(14),EventType.COMPILE_FAILED,"X.java",Map.of()),
            new TelemetryEvent(T.plusSeconds(190),EventType.COMPILE_STARTED,"X.java",Map.of()),
            new TelemetryEvent(T.plusSeconds(195),EventType.COMPILE_SUCCEEDED,"X.java",Map.of()),
            new TelemetryEvent(T.plusSeconds(230),EventType.CLASS_CREATED,"NewClass",Map.of()),
            new TelemetryEvent(T.plusSeconds(320),EventType.METHOD_DEFINED,"NewClass",Map.of("method","greet"))));
        PdfReportExporter exporter = new PdfReportExporter(new MetricsAggregator());
        Path file = temp.resolve("report.pdf"); exporter.export(s,file);
        byte[] content = Files.readAllBytes(file);
        assertEquals(".pdf",exporter.fileExtension());
        assertTrue(new String(content,java.nio.charset.StandardCharsets.ISO_8859_1).startsWith("%PDF-1.4"));
        String text = new String(content,java.nio.charset.StandardCharsets.ISO_8859_1);
        assertEquals(2, text.split("/Type /Page /Parent",-1).length-1);
        assertTrue(text.contains("Compilation results"));
        assertTrue(text.contains("Error category"));
        assertTrue(content.length > 2500);
    }
    @Test void emptyReportStillProducesTwoPages() throws Exception {
        var s = new SessionSnapshot("s1","Project","Student",T,T.plusSeconds(60),List.of());
        Path file = temp.resolve("empty.pdf"); new PdfReportExporter(new MetricsAggregator()).export(s,file);
        assertTrue(Files.size(file)>1000);
    }
}
