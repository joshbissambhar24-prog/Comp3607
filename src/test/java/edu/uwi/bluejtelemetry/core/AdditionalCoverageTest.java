package edu.uwi.bluejtelemetry.core;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AdditionalCoverageTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);

    @Test void exceptionOutcomesAreDistinguishedFromNormalInvocations() {
        assertTrue(InvocationOutcome.isRuntimeException("EXCEPTION_EXIT"));
        assertTrue(InvocationOutcome.isRuntimeException("exception_exit"));
        assertFalse(InvocationOutcome.isRuntimeException("NORMAL_EXIT"));
        assertFalse(InvocationOutcome.isRuntimeException(null));
        assertEquals("<constructor>", InvocationOutcome.methodName(null));
        assertEquals("<constructor>", InvocationOutcome.methodName(" "));
        assertEquals("sayHello", InvocationOutcome.methodName("sayHello"));
    }

    @Test void consoleOutputIsSummarizedWithoutStoringFullTranscript() {
        Map<String, Object> data = new ConsoleSummaryParser().summarize("Hello Josh\nException in thread main\n\nerror: example");
        assertEquals("manual_console_copy_paste", data.get("collection_method"));
        assertEquals(3, data.get("non_blank_line_count"));
        assertEquals(2, data.get("exception_or_error_line_count"));
        assertEquals("Hello Josh", data.get("first_line_excerpt"));
        assertFalse(data.containsKey("full_output"));
    }

    @Test void deletionEventIsEmittedOnceAndRenameKeepsHistory() {
        TelemetryBus bus = new TelemetryBus();
        List<TelemetryEvent> events = new ArrayList<>();
        bus.subscribe(events::add);
        SourceTracker tracker = new SourceTracker(CLOCK, bus);
        tracker.sample(Map.of("Student", "class Student {}"));
        tracker.rename("Student", "Learner");
        tracker.sample(Map.of("Learner", "class Student {}"));
        tracker.remove("Learner");
        tracker.sample(Map.of());
        assertEquals(1, events.stream().filter(e -> e.type() == EventType.CLASS_RENAMED).count());
        assertEquals(1, events.stream().filter(e -> e.type() == EventType.CLASS_DELETED).count());
    }
}
