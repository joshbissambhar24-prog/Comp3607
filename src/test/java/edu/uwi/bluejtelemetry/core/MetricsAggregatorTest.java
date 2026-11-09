package edu.uwi.bluejtelemetry.core;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MetricsAggregatorTest {
    private static final Instant T = Instant.parse("2026-11-07T12:00:00Z");
    private TelemetryEvent ev(long s, EventType type, Map<String,Object> details) {
        return new TelemetryEvent(T.plusSeconds(s),type,"A.java",details);
    }
    @Test void countsAttemptsOutcomesIntervalsAndErrorsWithoutDoubleCounting() {
        SessionSnapshot snapshot = new SessionSnapshot("id","project","student",T,T.plusSeconds(300),List.of(
            ev(10,EventType.COMPILE_STARTED,Map.of()), ev(12,EventType.COMPILATION_ERROR,Map.of("category","SYNTAX")),
            ev(13,EventType.COMPILE_FAILED,Map.of()), ev(100,EventType.COMPILE_STARTED,Map.of()),
            ev(110,EventType.COMPILE_SUCCEEDED,Map.of()), ev(111,EventType.EXECUTION_FINISHED,Map.of()),
            ev(112,EventType.RUNTIME_EXCEPTION,Map.of()),
            ev(155,EventType.EDIT_DETECTED,Map.of("changed_characters_estimate",30)),
            ev(160,EventType.METHOD_DEFINED,Map.of("method","main"))));
        SessionMetrics m = new MetricsAggregator(30).calculate(snapshot);
        assertEquals(300, m.durationSeconds()); assertEquals(2,m.compilations());
        assertEquals(1,m.successes()); assertEquals(1,m.failures());
        assertEquals(50,m.compileSuccessRate()); assertEquals(1,m.compilationErrors());
        assertEquals(1,m.executions()); assertEquals(1,m.runtimeExceptions());
        assertEquals(List.of(90L),m.compileIntervalsSeconds());
        assertEquals(1,m.errorFrequency().get("SYNTAX").intValue());
        assertEquals(30,m.changedCharactersEstimate()); assertEquals(1,m.editBatches());
        assertEquals(1,m.progressionEvents().size());
        assertTrue(m.activeSeconds() > 30 && m.activeSeconds() <= m.durationSeconds());
        assertEquals(300,m.activeSeconds()+m.idleSeconds());
    }
    @Test void emptySessionIsEntirelyIdleAndNeverDividesByZero() {
        var s = new SessionSnapshot("id","p","s",T,T.plusSeconds(180),List.of());
        SessionMetrics m = new MetricsAggregator().calculate(s);
        assertEquals(0,m.activeSeconds()); assertEquals(180,m.idleSeconds());
        assertEquals(0,m.compileSuccessRate()); assertEquals(0,m.estimatedCharactersPerActiveMinute());
    }
    @Test void overlappingWindowsAreMergedAndClipped() {
        var s = new SessionSnapshot("id","p","s",T,T.plusSeconds(100),List.of(
            ev(0,EventType.EDIT_DETECTED,Map.of()), ev(40,EventType.EDIT_DETECTED,Map.of()),
            ev(85,EventType.COMPILE_STARTED,Map.of())));
        SessionMetrics m = new MetricsAggregator(60).calculate(s);
        assertEquals(100,m.activeSeconds()); assertEquals(0,m.idleSeconds());
    }
    @Test void summarisesManuallyPastedConsoleOutput() {
        var summary = new ConsoleSummaryParser().summarize("Hello\njava.lang.Exception: Oops\n");
        assertEquals(2,summary.get("non_blank_line_count"));
        assertEquals(1,summary.get("exception_or_error_line_count"));
        assertEquals("Hello",summary.get("first_line_excerpt"));
        assertEquals(0,new ConsoleSummaryParser().summarize("").get("character_count"));
    }
    @Test void classifiesCompilerMessagesAndOptionalCodes() {
        var c = new ErrorClassifier();
        assertEquals("UNKNOWN_SYMBOL", c.category("cannot find symbol"));
        assertEquals("TYPE_MISMATCH", c.category("incompatible types"));
        assertEquals("SYNTAX", c.category("';' expected"));
        assertEquals("DUPLICATE_DECLARATION", c.category("variable already defined"));
        assertEquals("METHOD_ARGUMENTS", c.category("wrong argument list"));
        assertEquals("OTHER", c.category("mysterious compiler error"));
        assertNull(c.explicitCode("cannot find symbol"));
        assertEquals("compiler.err.expected",c.explicitCode("compiler.err.expected"));
    }
}
