package edu.uwi.bluejtelemetry.core;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SourceTrackerTest {
    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");
    @Test void detectsNewModifiedAndDeletedClassesWithoutInventingInitialCreations() {
        TelemetryBus bus = new TelemetryBus(); List<TelemetryEvent> events = new ArrayList<>(); bus.subscribe(events::add);
        SourceTracker tracker = new SourceTracker(Clock.fixed(NOW, ZoneOffset.UTC),bus);
        tracker.sample(Map.of("Existing", "public class Existing {}"));
        assertEquals(EventType.CLASS_PRESENT,events.get(0).type());
        tracker.sample(Map.of("Existing", "public class Existing { public void run() {} }",
            "New", "public class New { public void test() {} }"));
        assertTrue(events.stream().anyMatch(e -> e.type() == EventType.CLASS_CREATED && e.source().equals("New")));
        assertTrue(events.stream().anyMatch(e -> e.type() == EventType.METHOD_DEFINED));
        assertEquals(1, events.stream().filter(e -> e.type() == EventType.EDIT_DETECTED).count());
        tracker.sample(Map.of("New","public class New { public void test() {} }"));
        assertEquals(1, events.stream().filter(e -> e.type() == EventType.CLASS_DELETED).count());
        tracker.remove("New");
        assertEquals(2, events.stream().filter(e -> e.type() == EventType.CLASS_DELETED).count());
        tracker.reset(); assertTrue(tracker.knownSources().isEmpty());
    }
    @Test void renameDoesNotMisreportDeleteAndCreate() {
        TelemetryBus bus = new TelemetryBus(); List<TelemetryEvent> events = new ArrayList<>(); bus.subscribe(events::add);
        SourceTracker tracker = new SourceTracker(Clock.fixed(NOW,ZoneOffset.UTC),bus);
        tracker.sample(Map.of("Old","public class Old {}"));
        tracker.rename("Old","New");
        tracker.sample(Map.of("New","public class Old {}"));
        assertEquals(1, events.stream().filter(e -> e.type()==EventType.CLASS_RENAMED).count());
        assertEquals(0, events.stream().filter(e -> e.type()==EventType.CLASS_DELETED).count());
    }
    @Test void recognizesChangedCharactersAndMethods() {
        assertEquals(1, SourceTracker.approximateCharactersChanged("abc","abxc"));
        assertEquals(2, SourceTracker.approximateCharactersChanged("abc","ayc"));
        MethodScanner scanner = new MethodScanner();
        assertTrue(scanner.scan("public class A { public void greet() { } }").contains("greet"));
        assertTrue(scanner.scan("public class A { public int add(int a, int b) { return a+b; } }").contains("add"));
        assertFalse(scanner.scan("public class A { if (true) { } }").contains("if"));
    }
}
