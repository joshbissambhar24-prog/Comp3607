package edu.uwi.bluejtelemetry.core;

import java.util.List;
import java.util.Map;

public record SessionMetrics(long durationSeconds, long activeSeconds, long idleSeconds,
    int compilations, int successes, int failures, int compilationErrors, int executions,
    int runtimeExceptions, int consoleSummaries, int editBatches, long changedCharactersEstimate, double compileSuccessRate,
    double editBatchesPerMinute, double estimatedCharactersPerActiveMinute,
    List<Long> compileIntervalsSeconds, Map<String, Integer> errorFrequency,
    List<TelemetryEvent> progressionEvents) { }
