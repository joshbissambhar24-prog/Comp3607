package edu.uwi.bluejtelemetry.core;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BlueJEventMapperTest {
    private final BlueJEventMapper mapper = new BlueJEventMapper();
    private final Instant now = Instant.parse("2026-11-09T15:00:00Z");

    @Test void mapsCompilationCallbacksToEvents() {
        assertEquals(EventType.COMPILE_STARTED,
            mapper.compilation(EventType.COMPILE_STARTED, "Student.java", now).type());
        assertEquals(EventType.COMPILE_SUCCEEDED,
            mapper.compilation(EventType.COMPILE_SUCCEEDED, "Student.java", now).type());
        assertEquals(EventType.COMPILE_FAILED,
            mapper.compilation(EventType.COMPILE_FAILED, "Student.java", now).type());
        assertThrows(IllegalArgumentException.class,
            () -> mapper.compilation(EventType.CLASS_CREATED, "Student.java", now));
    }

    @Test void mapsCompilerErrorWithLineAndNullCode() {
        TelemetryEvent event = mapper.diagnostic(EventType.COMPILATION_ERROR, "Student.java", now,
            "cannot find symbol", 10);
        assertEquals("UNKNOWN_SYMBOL", event.attributes().get("category"));
        assertEquals(10, event.attributes().get("line_number"));
        assertEquals("cannot find symbol", event.attributes().get("message"));
        assertNull(event.attributes().get("error_code"));
        assertEquals(now, event.timestamp());
    }

    @Test void handlesAvailableDiagnosticCodesAndWarnings() {
        TelemetryEvent event = mapper.diagnostic(EventType.COMPILATION_WARNING, "Student.java", now,
            "compiler.err.some_code", 4);
        assertEquals("compiler.err.some_code", event.attributes().get("error_code"));
        assertThrows(IllegalArgumentException.class,
            () -> mapper.diagnostic(EventType.EXECUTION_FINISHED, "X", now, "", 0));
    }

    @Test void mapsNormalInvocationAndConstructor() {
        List<TelemetryEvent> result = mapper.invocation("Student", "", "NORMAL_EXIT", now);
        assertEquals(1, result.size());
        assertEquals("<constructor>", result.get(0).attributes().get("method"));
        assertEquals("NORMAL_EXIT", result.get(0).attributes().get("status"));
    }

    @Test void mapsRuntimeExceptionToTwoEvents() {
        List<TelemetryEvent> result = mapper.invocation("Student", "sayHello", "EXCEPTION_EXIT", now);
        assertEquals(2, result.size());
        assertEquals(EventType.EXECUTION_FINISHED, result.get(0).type());
        assertEquals(EventType.RUNTIME_EXCEPTION, result.get(1).type());
        assertEquals(false, result.get(1).attributes().get("exception_details_available"));
    }
}
