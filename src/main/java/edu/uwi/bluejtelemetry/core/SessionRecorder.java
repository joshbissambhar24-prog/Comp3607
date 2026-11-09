package edu.uwi.bluejtelemetry.core;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** One project per session. Records immutable snapshots for concurrent exports. */
public final class SessionRecorder {
    private final Clock clock;
    private String id, projectName, studentId;
    private Instant startedAt, finishedAt;
    private final List<TelemetryEvent> events = new ArrayList<>();
    public SessionRecorder(Clock clock) { this.clock = Objects.requireNonNull(clock); }
    public synchronized void start(String project, String student) {
        if (startedAt != null && finishedAt == null) throw new IllegalStateException("Session already active");
        id = UUID.randomUUID().toString();
        projectName = Objects.requireNonNull(project);
        studentId = student == null || student.isBlank() ? "STUDENT_ID_PLACEHOLDER" : student;
        startedAt = clock.instant(); finishedAt = null; events.clear();
    }
    public synchronized boolean active() { return startedAt != null && finishedAt == null; }
    public synchronized void accept(TelemetryEvent event) {
        if (active() && !event.timestamp().isBefore(startedAt)) events.add(event);
    }
    public synchronized SessionSnapshot snapshot() {
        if (startedAt == null) throw new IllegalStateException("No session recorded");
        return new SessionSnapshot(id, projectName, studentId, startedAt,
            finishedAt == null ? clock.instant() : finishedAt, List.copyOf(events));
    }
    public synchronized SessionSnapshot end() {
        if (!active()) throw new IllegalStateException("No active session");
        finishedAt = clock.instant();
        return snapshot();
    }
}
