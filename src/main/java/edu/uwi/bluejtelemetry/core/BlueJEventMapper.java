package edu.uwi.bluejtelemetry.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Converts BlueJ callback values to domain events without requiring a BlueJ GUI. */
public final class BlueJEventMapper {
    private final EventCreator.Compiler compiler = new EventCreator.Compiler();
    private final EventCreator.Execution execution = new EventCreator.Execution();
    private final ErrorClassifier classifier = new ErrorClassifier();

    public TelemetryEvent compilation(EventType type, String source, Instant at) {
        if (type != EventType.COMPILE_STARTED && type != EventType.COMPILE_SUCCEEDED &&
            type != EventType.COMPILE_FAILED)
            throw new IllegalArgumentException("Not a compilation lifecycle event: " + type);
        return compiler.create(type, source, at, Map.of());
    }

    public TelemetryEvent diagnostic(EventType type, String source, Instant at, String message, int line) {
        if (type != EventType.COMPILATION_ERROR && type != EventType.COMPILATION_WARNING)
            throw new IllegalArgumentException("Not a diagnostic event: " + type);
        String text = message == null ? "" : message;
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("message", text);
        attrs.put("line_number", line);
        attrs.put("category", classifier.category(text));
        attrs.put("error_code", classifier.explicitCode(text));
        return compiler.create(type, source, at, attrs);
    }

    /** Always creates the completion event; adds a runtime exception event if BlueJ reports one. */
    public List<TelemetryEvent> invocation(String cls, String method, String status, Instant at) {
        String normalizedStatus = status == null ? "UNKNOWN" : status;
        String methodName = InvocationOutcome.methodName(method);
        List<TelemetryEvent> result = new ArrayList<>();
        result.add(execution.create(EventType.EXECUTION_FINISHED, cls, at,
            Map.of("method", methodName, "status", normalizedStatus)));
        if (InvocationOutcome.isRuntimeException(normalizedStatus))
            result.add(execution.create(EventType.RUNTIME_EXCEPTION, cls, at,
                Map.of("method", methodName, "exception_details_available", false)));
        return List.copyOf(result);
    }
}
