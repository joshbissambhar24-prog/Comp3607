package edu.uwi.bluejtelemetry.core;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record SessionSnapshot(String sessionId, String projectName, String studentId,
                              Instant startedAt, Instant endedAt, List<TelemetryEvent> events) {
    public SessionSnapshot {
        Objects.requireNonNull(startedAt);
        Objects.requireNonNull(endedAt);
        if (endedAt.isBefore(startedAt)) throw new IllegalArgumentException("End cannot precede start");
        events = List.copyOf(events);
    }
}
