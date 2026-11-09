package edu.uwi.bluejtelemetry.core;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure aggregation; no BlueJ GUI or I/O needed for JUnit tests. */
public final class MetricsAggregator {
    private final long activityWindowSeconds;
    public MetricsAggregator() { this(60); }
    public MetricsAggregator(long activityWindowSeconds) {
        if (activityWindowSeconds < 1) throw new IllegalArgumentException("activity window must be positive");
        this.activityWindowSeconds = activityWindowSeconds;
    }
    public SessionMetrics calculate(SessionSnapshot snapshot) {
        List<TelemetryEvent> events = snapshot.events().stream()
            .sorted(Comparator.comparing(TelemetryEvent::timestamp)).toList();
        int starts = 0, success = 0, failure = 0, errors = 0, exec = 0, exceptions = 0, summaries = 0, edits = 0;
        long changedChars = 0;
        Instant lastCompile = null;
        List<Long> intervals = new ArrayList<>();
        Map<String, Integer> errFreq = new LinkedHashMap<>();
        List<TelemetryEvent> progression = new ArrayList<>();
        for (TelemetryEvent event : events) {
            switch (event.type()) {
                case COMPILE_STARTED -> {
                    starts++;
                    if (lastCompile != null) intervals.add(Math.max(0,
                        Duration.between(lastCompile, event.timestamp()).toSeconds()));
                    lastCompile = event.timestamp();
                }
                case COMPILE_SUCCEEDED -> success++;
                case COMPILE_FAILED -> failure++;
                case COMPILATION_ERROR -> {
                    errors++;
                    String category = String.valueOf(event.attributes().getOrDefault("category", "OTHER"));
                    errFreq.merge(category, 1, Integer::sum);
                }
                case EXECUTION_FINISHED -> exec++;
                case RUNTIME_EXCEPTION -> exceptions++;
                case CONSOLE_OUTPUT_SUMMARY -> summaries++;
                case EDIT_DETECTED -> {
                    edits++;
                    Object c = event.attributes().get("changed_characters_estimate");
                    if (c instanceof Number n) changedChars += Math.max(0, n.longValue());
                }
                case CLASS_CREATED, CLASS_DELETED, METHOD_DEFINED -> progression.add(event);
                default -> { }
            }
        }
        long duration = Math.max(0, Duration.between(snapshot.startedAt(), snapshot.endedAt()).toSeconds());
        long active = calculateActiveTime(events, snapshot.startedAt(), snapshot.endedAt());
        long idle = Math.max(0, duration - active);
        double successRate = starts == 0 ? 0 : 100.0 * success / starts;
        double editsPerMinute = duration == 0 ? 0 : 60.0 * edits / duration;
        double cadence = active == 0 ? 0 : 60.0 * changedChars / active;
        return new SessionMetrics(duration, active, idle, starts, success, failure,
            errors, exec, exceptions, summaries, edits, changedChars, successRate, editsPerMinute,
            cadence, List.copyOf(intervals), Map.copyOf(errFreq), List.copyOf(progression));
    }
    private long calculateActiveTime(List<TelemetryEvent> events, Instant start, Instant end) {
        Instant currentStart = null, currentEnd = null;
        long seconds = 0;
        for (TelemetryEvent event : events) {
            if (!countsAsActivity(event.type())) continue;
            Instant a = event.timestamp();
            if (a.isBefore(start)) a = start;
            if (!a.isBefore(end)) continue;
            Instant b = a.plusSeconds(activityWindowSeconds);
            if (b.isAfter(end)) b = end;
            if (currentStart == null) { currentStart = a; currentEnd = b; }
            else if (!a.isAfter(currentEnd)) { if (b.isAfter(currentEnd)) currentEnd = b; }
            else { seconds += Duration.between(currentStart, currentEnd).toSeconds(); currentStart = a; currentEnd = b; }
        }
        if (currentStart != null) seconds += Duration.between(currentStart, currentEnd).toSeconds();
        return Math.min(seconds, Math.max(0, Duration.between(start, end).toSeconds()));
    }
    private boolean countsAsActivity(EventType t) {
        return switch (t) {
            case EDIT_DETECTED, CLASS_CREATED, CLASS_DELETED, CLASS_RENAMED,
                 COMPILE_STARTED, EXECUTION_FINISHED, RUNTIME_EXCEPTION -> true;
            default -> false;
        };
    }
}
