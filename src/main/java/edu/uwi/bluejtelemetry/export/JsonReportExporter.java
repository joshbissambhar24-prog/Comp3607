package edu.uwi.bluejtelemetry.export;

import edu.uwi.bluejtelemetry.core.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonReportExporter implements ReportExporter {
    private final MetricsAggregator aggregator;
    private final JsonWriter writer = new JsonWriter();
    public JsonReportExporter(MetricsAggregator aggregator) { this.aggregator = aggregator; }
    @Override public String fileExtension() { return ".json"; }
    public Map<String, Object> createSchema(SessionSnapshot snapshot) {
        SessionMetrics m = aggregator.calculate(snapshot);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", "1.0");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("session_id", snapshot.sessionId());
        metadata.put("student_id", snapshot.studentId());
        metadata.put("project_name", snapshot.projectName());
        metadata.put("started_at", snapshot.startedAt().toString());
        metadata.put("ended_at", snapshot.endedAt().toString());
        metadata.put("session_duration_seconds", m.durationSeconds());
        root.put("session", metadata);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_compilations", m.compilations());
        summary.put("successful_compilations", m.successes());
        summary.put("failed_compilations", m.failures());
        summary.put("compilation_error_count", m.compilationErrors());
        summary.put("execution_count", m.executions());
        summary.put("runtime_exception_count", m.runtimeExceptions());
        summary.put("compile_success_rate_percent", m.compileSuccessRate());
        summary.put("compile_intervals_seconds", m.compileIntervalsSeconds());
        summary.put("active_coding_seconds_estimate", m.activeSeconds());
        summary.put("idle_seconds_estimate", m.idleSeconds());
        summary.put("edit_batches", m.editBatches());
        summary.put("changed_characters_estimate", m.changedCharactersEstimate());
        summary.put("edit_batches_per_minute", m.editBatchesPerMinute());
        summary.put("estimated_characters_per_active_minute", m.estimatedCharactersPerActiveMinute());
        summary.put("error_frequency_by_category", m.errorFrequency());
        List<Map<String, Object>> consoleSummaries = new ArrayList<>();
        for (TelemetryEvent event : snapshot.events()) {
            if (event.type() == EventType.CONSOLE_OUTPUT_SUMMARY) consoleSummaries.add(event.asMap());
        }
        summary.put("console_output_summaries", consoleSummaries);
        summary.put("console_output_capture_status", consoleSummaries.isEmpty()
            ? "NOT_CAPTURED__MANUAL_PASTE_REQUIRED" : "MANUALLY_PASTED_BY_USER");
        root.put("summary", summary);
        List<Map<String, Object>> timeline = new ArrayList<>();
        for (TelemetryEvent ev : m.progressionEvents()) timeline.add(ev.asMap());
        root.put("class_and_method_timeline", timeline);
        List<Map<String, Object>> events = new ArrayList<>();
        for (TelemetryEvent ev : snapshot.events()) events.add(ev.asMap());
        root.put("events", events);
        root.put("collection_notes", List.of("Editor events are sampled; not literal keystrokes.",
            "Activity and cadence are estimates, not verified keyboard focus.",
            "BlueJ compile API has messages/lines but no standard diagnostic code field.",
            "BlueJ console text can be imported manually using the Tools menu; automatic terminal interception is not available."));
        return root;
    }
    @Override public void export(SessionSnapshot snapshot, Path destination) throws IOException {
        Files.createDirectories(destination.toAbsolutePath().getParent());
        Files.writeString(destination, writer.stringify(createSchema(snapshot)) + "\n", StandardCharsets.UTF_8);
    }
}
