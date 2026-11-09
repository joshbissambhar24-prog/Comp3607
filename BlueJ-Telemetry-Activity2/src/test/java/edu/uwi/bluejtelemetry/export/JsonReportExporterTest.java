package edu.uwi.bluejtelemetry.export;

import edu.uwi.bluejtelemetry.core.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class JsonReportExporterTest {
    private static final Instant T = Instant.parse("2026-11-07T12:00:00Z");
    @TempDir Path temp;
    @Test void serializesValidNestedStructureAndEscapesControlCharacters() throws Exception {
        JsonWriter w = new JsonWriter();
        java.util.Map<String,Object> ordered = new java.util.LinkedHashMap<>();
        ordered.put("a","hi\n\"x\""); ordered.put("b",new Object[]{true,null,12});
        assertEquals("{\"a\":\"hi\\n\\\"x\\\"\",\"b\":[true,null,12]}",
            w.stringify(ordered));
        assertEquals("[1,2,3]", w.stringify(List.of(1,2,3)));
        assertEquals("null",w.stringify(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> w.stringify(new Object()));
    }
    @Test void exportsRequiredKeysTimestampsErrorDetailsAndNullErrorCode() throws Exception {
        Map<String,Object> attr = new HashMap<>(); attr.put("message","cannot find symbol");
        attr.put("error_code",null); attr.put("line_number",7); attr.put("category","UNKNOWN_SYMBOL");
        var s = new SessionSnapshot("s1","BlueJ Demo","UWI-STUDENT",T,T.plusSeconds(120),List.of(
            new TelemetryEvent(T.plusSeconds(5),EventType.COMPILE_STARTED,"A.java", Map.of()),
            new TelemetryEvent(T.plusSeconds(7),EventType.COMPILATION_ERROR,"A.java",attr),
            new TelemetryEvent(T.plusSeconds(8),EventType.COMPILE_FAILED,"A.java",Map.of())));
        JsonReportExporter exporter = new JsonReportExporter(new MetricsAggregator());
        Path file = temp.resolve("report.json"); exporter.export(s,file);
        String json = Files.readString(file);
        assertEquals(".json",exporter.fileExtension());
        assertTrue(json.contains("\"session_id\":\"s1\""));
        assertTrue(json.contains("\"total_compilations\":1"));
        assertTrue(json.contains("\"error_code\":null"));
        assertTrue(json.contains("\"compilation_error_count\":1"));
        assertTrue(json.contains("\"console_output_capture_status\""));
        assertTrue(json.contains("\"2026-11-07T12:00:07Z\""));
        assertTrue(exporter.createSchema(s).containsKey("class_and_method_timeline"));
    }
}
