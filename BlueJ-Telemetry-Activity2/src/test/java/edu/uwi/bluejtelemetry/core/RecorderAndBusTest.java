package edu.uwi.bluejtelemetry.core;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RecorderAndBusTest {
    private static final Instant T = Instant.parse("2026-11-07T12:00:00Z");
    private final Clock fixed = Clock.fixed(T, ZoneOffset.UTC);
    @Test void observersReceiveEventsAndCanUnsubscribe() {
        TelemetryBus bus = new TelemetryBus(); List<TelemetryEvent> seen = new ArrayList<>();
        bus.subscribe(seen::add);
        TelemetryEvent e = new EventCreator.Compiler().create(EventType.COMPILE_STARTED, "A.java", T, Map.of());
        bus.publish(e); assertEquals(List.of(e), seen);
        java.util.function.Consumer<TelemetryEvent> extra = ev -> seen.add(ev);
        bus.subscribe(extra); bus.unsubscribe(extra); bus.publish(e);
        assertEquals(2, seen.size());
    }
    @Test void creatorsEnforceEventFamilies() {
        assertEquals(EventType.EDIT_DETECTED, new EventCreator.Editor()
            .create(EventType.EDIT_DETECTED,"A",T,Map.of()).type());
        assertThrows(IllegalArgumentException.class, () ->
            new EventCreator.Compiler().create(EventType.CLASS_CREATED,"A",T,Map.of()));
        assertEquals(EventType.RUNTIME_EXCEPTION, new EventCreator.Execution()
            .create(EventType.RUNTIME_EXCEPTION,"A",T,Map.of()).type());
    }
    @Test void recorderStartsAcceptsEndsAndIsImmutable() {
        SessionRecorder recorder = new SessionRecorder(fixed);
        assertFalse(recorder.active());
        assertThrows(IllegalStateException.class, recorder::snapshot);
        recorder.start("Demo", "");
        assertTrue(recorder.active());
        assertThrows(IllegalStateException.class, () -> recorder.start("Second","ID"));
        TelemetryEvent ev = new TelemetryEvent(T, EventType.CLASS_CREATED,"A",Map.of());
        recorder.accept(ev);
        assertEquals("STUDENT_ID_PLACEHOLDER", recorder.snapshot().studentId());
        assertEquals(1, recorder.snapshot().events().size());
        assertThrows(UnsupportedOperationException.class, () -> recorder.snapshot().events().add(ev));
        var ended = recorder.end();
        assertFalse(recorder.active()); assertEquals(T, ended.endedAt());
        recorder.accept(ev); assertEquals(1, recorder.snapshot().events().size());
        recorder.start("New", "123"); assertEquals("123", recorder.snapshot().studentId());
        assertEquals(0, recorder.snapshot().events().size());
    }
    @Test void eventAttributesCanIncludeUnknownErrorCodesAndRemainReadOnly() {
        java.util.Map<String,Object> details = new java.util.HashMap<>(); details.put("error_code",null);
        TelemetryEvent ev = new TelemetryEvent(T, EventType.COMPILATION_ERROR,"X",details);
        assertNull(ev.attributes().get("error_code"));
        assertThrows(UnsupportedOperationException.class,() -> ev.attributes().put("a",1));
        assertEquals("COMPILATION_ERROR", ev.asMap().get("type"));
    }
}
