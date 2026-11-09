package edu.uwi.bluejtelemetry.core;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record TelemetryEvent(Instant timestamp, EventType type, String source, Map<String, Object> attributes) {
    public TelemetryEvent {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(type, "type");
        source = source == null ? "" : source;
        attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes == null ? Map.of() : attributes));
    }
    public Map<String, Object> asMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", timestamp.toString());
        m.put("type", type.name());
        m.put("source", source);
        m.put("attributes", attributes);
        return m;
    }
}
