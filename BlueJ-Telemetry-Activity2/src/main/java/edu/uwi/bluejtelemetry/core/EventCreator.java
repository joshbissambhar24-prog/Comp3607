package edu.uwi.bluejtelemetry.core;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;

/** Factory Method: concrete creators each own a family of event types. */
public abstract class EventCreator {
    protected abstract EnumSet<EventType> allowedTypes();
    public final TelemetryEvent create(EventType type, String source, Instant at, Map<String, Object> details) {
        if (!allowedTypes().contains(type)) throw new IllegalArgumentException("Unsupported event for creator: " + type);
        return new TelemetryEvent(at, type, source, details);
    }
    public static final class Lifecycle extends EventCreator {
        @Override protected EnumSet<EventType> allowedTypes() {
            return EnumSet.of(EventType.PROJECT_OPENED, EventType.PROJECT_CLOSED, EventType.CLASS_PRESENT,
                EventType.CLASS_CREATED, EventType.CLASS_DELETED, EventType.CLASS_RENAMED,
                EventType.CLASS_MODIFIED, EventType.METHOD_DEFINED);
        }
    }
    public static final class Editor extends EventCreator {
        @Override protected EnumSet<EventType> allowedTypes() { return EnumSet.of(EventType.EDIT_DETECTED); }
    }
    public static final class Compiler extends EventCreator {
        @Override protected EnumSet<EventType> allowedTypes() {
            return EnumSet.of(EventType.COMPILE_STARTED, EventType.COMPILE_SUCCEEDED, EventType.COMPILE_FAILED,
                EventType.COMPILATION_ERROR, EventType.COMPILATION_WARNING);
        }
    }
    public static final class Execution extends EventCreator {
        @Override protected EnumSet<EventType> allowedTypes() {
            return EnumSet.of(EventType.EXECUTION_FINISHED, EventType.RUNTIME_EXCEPTION, EventType.CONSOLE_OUTPUT_SUMMARY);
        }
    }
}
