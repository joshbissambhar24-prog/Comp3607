package edu.uwi.bluejtelemetry.demo;

import edu.uwi.bluejtelemetry.core.*;
import edu.uwi.bluejtelemetry.export.*;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/** Offline sample data producer; proves both exporters without starting BlueJ. */
public final class DemoMain {
    private static final class AdjustableClock extends Clock {
        private Instant now = Instant.parse("2026-11-07T12:00:00Z");
        void setSeconds(long seconds) { now = Instant.parse("2026-11-07T12:00:00Z").plusSeconds(seconds); }
        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }
    public static void main(String[] args) throws Exception {
        AdjustableClock clock = new AdjustableClock();
        SessionRecorder recorder = new SessionRecorder(clock);
        TelemetryBus bus = new TelemetryBus(); bus.subscribe(recorder::accept);
        recorder.start("OOP1ExampleProject", "STUDENT_ID_PLACEHOLDER");
        SourceTracker sources = new SourceTracker(clock, bus);
        EventCreator.Compiler compiler = new EventCreator.Compiler();
        EventCreator.Execution execution = new EventCreator.Execution();
        sources.sample(Map.of("HelloWorld", "public class HelloWorld { }"));
        clock.setSeconds(40);
        sources.sample(Map.of("HelloWorld", "public class HelloWorld { public void greet() {} }"));
        clock.setSeconds(110);
        bus.publish(compiler.create(EventType.COMPILE_STARTED,"HelloWorld.java",clock.instant(),Map.of()));
        clock.setSeconds(113);
        bus.publish(compiler.create(EventType.COMPILATION_ERROR,"HelloWorld.java",clock.instant(),
            Map.of("category","SYNTAX","message","';' expected","line_number",4)));
        bus.publish(compiler.create(EventType.COMPILE_FAILED,"HelloWorld.java",clock.instant(),Map.of()));
        clock.setSeconds(330);
        sources.sample(Map.of("HelloWorld", "public class HelloWorld { public void greet() { System.out.println(\"Hello\"); } }",
            "Calculator", "public class Calculator { public int add(int a, int b) { return a+b; } }"));
        clock.setSeconds(410);
        bus.publish(compiler.create(EventType.COMPILE_STARTED,"HelloWorld.java",clock.instant(),Map.of()));
        clock.setSeconds(415);
        bus.publish(compiler.create(EventType.COMPILE_SUCCEEDED,"HelloWorld.java",clock.instant(),Map.of()));
        clock.setSeconds(465);
        bus.publish(execution.create(EventType.EXECUTION_FINISHED,"HelloWorld",clock.instant(),
            Map.of("method","greet","status","NORMAL_EXIT")));
        clock.setSeconds(472);
        bus.publish(execution.create(EventType.CONSOLE_OUTPUT_SUMMARY,"BlueJ Terminal",clock.instant(),
            new ConsoleSummaryParser().summarize("Hello, world!\n")));
        clock.setSeconds(870);
        sources.sample(Map.of("HelloWorld", "public class HelloWorld { public void greet() { System.out.println(\"Hello\"); } }",
            "Calculator", "public class Calculator { public int add(int a, int b) { return a+b; } public int subtract(int a, int b) { return a-b; } }"));
        clock.setSeconds(1040);
        bus.publish(compiler.create(EventType.COMPILE_STARTED,"Calculator.java",clock.instant(),Map.of()));
        clock.setSeconds(1042);
        bus.publish(compiler.create(EventType.COMPILE_SUCCEEDED,"Calculator.java",clock.instant(),Map.of()));
        clock.setSeconds(1100);
        var snapshot = recorder.end();
        var aggregator = new MetricsAggregator();
        Path dir = Path.of(args.length == 0 ? "demo-output" : args[0]);
        new JsonReportExporter(aggregator).export(snapshot,dir.resolve("sample.json"));
        new PdfReportExporter(aggregator).export(snapshot,dir.resolve("sample.pdf"));
        System.out.println("Created demonstration JSON and 2-page PDF in: " + dir.toAbsolutePath());
    }
}
