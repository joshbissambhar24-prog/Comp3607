package edu.uwi.bluejtelemetry.export;

import edu.uwi.bluejtelemetry.core.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Two-page PDF: summaries, compile bar chart, cumulative compile line graph, error and class timelines. */
public final class PdfReportExporter implements ReportExporter {
    private static final double[] NAVY = {0.08, 0.15, 0.27};
    private static final double[] BLUE = {0.12, 0.38, 0.71};
    private static final double[] CYAN = {0.07, 0.58, 0.68};
    private static final double[] RED = {0.79, 0.25, 0.27};
    private static final double[] LIGHT = {0.94, 0.96, 0.98};
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());
    private final MetricsAggregator aggregator;
    public PdfReportExporter(MetricsAggregator aggregator) { this.aggregator = aggregator; }
    @Override public String fileExtension() { return ".pdf"; }
    @Override public void export(SessionSnapshot snapshot, Path destination) throws IOException {
        SessionMetrics m = aggregator.calculate(snapshot);
        MinimalPdf pdf = new MinimalPdf();
        firstPage(pdf.addPage(), snapshot, m);
        secondPage(pdf.addPage(), snapshot, m);
        pdf.save(destination);
    }
    private static void color(MinimalPdf.Page p, double[] rgb) { p.fill(rgb[0],rgb[1],rgb[2]); }
    private static void ink(MinimalPdf.Page p, double[] rgb) { p.stroke(rgb[0],rgb[1],rgb[2]); }
    private static void header(MinimalPdf.Page p, String tag, int number) {
        color(p, NAVY); p.rectangle(0, 765, 595, 77);
        color(p, new double[]{1,1,1}); p.text("BLUEJ TELEMETRY", 40, 806, 21, true);
        p.text(tag, 40, 781, 10, false);
        color(p, NAVY); p.text("OOP1 | Session Analytics", 40, 748, 10, true);
        color(p, new double[]{0.5,0.55,0.6}); p.text("Page " + number + " of 2", 493, 30, 9, false);
    }
    private static void title(MinimalPdf.Page p, String text, double y) {
        color(p, NAVY); p.text(text, 40, y, 13, true);
        ink(p, new double[]{0.82,0.87,0.92}); p.line(40, y-9, 554, y-9, 0.75);
    }
    private static void metricBox(MinimalPdf.Page p, int x, String value, String label) {
        color(p, LIGHT); p.rectangle(x, 633, 120, 67);
        color(p, NAVY); p.text(value, x+12, 659, 18, true);
        color(p, BLUE); p.text(label, x+12, 640, 9, false);
    }
    private static String number(double v) { return String.format(Locale.ROOT, "%.1f", v); }
    private static String clip(String s, int count) {
        if (s == null) return "";
        return s.length() <= count ? s : s.substring(0, count-3) + "...";
    }
    private static void firstPage(MinimalPdf.Page p, SessionSnapshot s, SessionMetrics m) {
        header(p, "Session overview and activity visualisations", 1);
        color(p, NAVY); p.text("Project: " + clip(s.projectName(), 48), 40, 726, 11, true);
        p.text("Student: " + clip(s.studentId(), 33), 348, 726, 10, false);
        color(p, BLUE); p.text("Session: " + s.sessionId().substring(0, Math.min(8,s.sessionId().length())) + "   |   " + TIME.format(s.startedAt()), 40, 708, 9, false);
        metricBox(p, 40, String.valueOf(m.compilations()), "Compilation attempts");
        metricBox(p, 173, number(m.compileSuccessRate()) + "%", "Compile success rate");
        metricBox(p, 306, String.valueOf(m.executions()), "Executions");
        metricBox(p, 439, String.valueOf(m.compilationErrors()), "Compiler errors");
        title(p, "Estimated coding activity", 605);
        color(p, NAVY); p.text("Active: " + m.activeSeconds()/60 + "m " + m.activeSeconds()%60 + "s", 40, 579, 10, false);
        p.text("Idle: " + m.idleSeconds()/60 + "m " + m.idleSeconds()%60 + "s", 212, 579, 10, false);
        color(p, BLUE); p.text("Activity estimated from observed edit/compile/execution events (60s active window)", 40, 548, 8, false);
        title(p, "Compilation results", 499);
        bar(p, 40, 454, 150, m.successes(), Math.max(1, m.compilations()), BLUE, "Successful");
        bar(p, 40, 428, 150, m.failures(), Math.max(1, m.compilations()), RED, "Failed");
        color(p, NAVY); p.text("Compile intervals (seconds): " + clip(m.compileIntervalsSeconds().toString(), 53), 340, 459, 9, false);
        p.text("Runtime exceptions: " + m.runtimeExceptions(), 340, 437, 9, false);
        p.text("Manual console summaries: " + m.consoleSummaries(), 340, 416, 9, false);
        title(p, "Compilation activity over time", 382);
        graph(p, s, m);
        title(p, "Editing and session statistics", 205);
        drawRow(p, 180, "Session duration", m.durationSeconds() + " seconds");
        drawRow(p, 158, "Detected edit batches", String.valueOf(m.editBatches()));
        drawRow(p, 136, "Estimated changed characters", String.valueOf(m.changedCharactersEstimate()));
        drawRow(p, 114, "Estimated characters / active minute", number(m.estimatedCharactersPerActiveMinute()));
        drawRow(p, 92, "Detected edits / minute", number(m.editBatchesPerMinute()));
        color(p, BLUE); p.text("Metrics reflect sampling, not direct keystroke tracking.", 40, 61, 8, false);
    }
    private static void bar(MinimalPdf.Page p, int x, int y, int maxWidth, int count,
                            int maxCount, double[] rgb, String label) {
        color(p, NAVY); p.text(label, x, y, 10, false);
        color(p, LIGHT); p.rectangle(x+89, y-4, maxWidth, 13);
        color(p, rgb); p.rectangle(x+89, y-4, maxWidth * Math.min(count, maxCount) / (double)maxCount, 13);
        color(p, NAVY); p.text(String.valueOf(count), x+89+maxWidth+9, y, 10, true);
    }
    private static void graph(MinimalPdf.Page p, SessionSnapshot s, SessionMetrics m) {
        int x = 56, y = 246, w = 482, h = 105;
        ink(p, new double[]{0.75,0.8,0.85}); p.line(x, y, x+w, y, 0.7); p.line(x, y, x, y+h, 0.7);
        List<TelemetryEvent> starts = s.events().stream().filter(e -> e.type() == EventType.COMPILE_STARTED)
            .sorted(Comparator.comparing(TelemetryEvent::timestamp)).toList();
        if (starts.isEmpty()) { color(p, BLUE); p.text("No compilation events in this session", 151, 288, 10, false); return; }
        ink(p, BLUE);
        double duration = Math.max(1, m.durationSeconds());
        double previousX = x, previousY = y;
        for (int i=0; i<starts.size(); i++) {
            long elapsed = Math.max(0, Duration.between(s.startedAt(), starts.get(i).timestamp()).toSeconds());
            double nextX = x + w * Math.min(1, elapsed / duration);
            double nextY = y + h * (i + 1) / Math.max(1, starts.size());
            p.line(previousX, previousY, nextX, nextY, 1.65);
            color(p, CYAN); p.rectangle(nextX-2, nextY-2, 4, 4);
            ink(p, BLUE); previousX = nextX; previousY = nextY;
        }
        color(p, NAVY); p.text("Start", x, 233, 8, false); p.text("End", x+w-14, 233, 8, false);
    }
    private static void drawRow(MinimalPdf.Page p, int y, String key, String value) {
        color(p, LIGHT); p.rectangle(40, y-7, 514, 20);
        color(p, NAVY); p.text(key, 50, y, 9, false); p.text(value, 346, y, 9, true);
    }
    private static void secondPage(MinimalPdf.Page p, SessionSnapshot s, SessionMetrics m) {
        header(p, "Errors, lifecycle progression and collection notes", 2);
        title(p, "Compilation error frequency", 706);
        color(p, NAVY); p.text("Error category", 48, 672, 10, true); p.text("Count", 482, 672, 10, true);
        List<Map.Entry<String,Integer>> frequencies = new ArrayList<>(m.errorFrequency().entrySet());
        frequencies.sort(Map.Entry.<String,Integer>comparingByValue().reversed());
        int y = 649;
        if (frequencies.isEmpty()) { p.text("No compiler errors recorded", 48, y, 10, false); y-=23; }
        for (int i=0; i<Math.min(8, frequencies.size()); i++) {
            var e = frequencies.get(i);
            color(p, LIGHT); p.rectangle(40,y-8,514,21);
            color(p, NAVY); p.text(clip(e.getKey(), 43),48,y,10,false); p.text(String.valueOf(e.getValue()),492,y,10,true);
            y -= 26;
        }
        title(p, "Class creation / deletion and method definitions", 400);
        color(p, NAVY); p.text("Timestamp", 48, 368, 9, true);
        p.text("Event", 201, 368, 9, true); p.text("Class / method", 338, 368, 9, true);
        int ty = 343;
        List<TelemetryEvent> timeline = m.progressionEvents();
        if (timeline.isEmpty()) { p.text("No class or method progression events", 48, ty, 10, false); ty -= 22; }
        for (int i=0; i<Math.min(9, timeline.size()); i++) {
            TelemetryEvent ev = timeline.get(i);
            color(p, LIGHT); p.rectangle(40, ty-8, 514, 21);
            color(p, NAVY); p.text(TIME.format(ev.timestamp()), 48, ty, 8, false);
            p.text(ev.type().name(), 201, ty, 8, false);
            String label = ev.source();
            if (ev.type()==EventType.METHOD_DEFINED) label += "." + ev.attributes().get("method") + "()";
            p.text(clip(label, 30), 338, ty, 8, false);
            ty -= 25;
        }
        if (timeline.size() > 9) { color(p, BLUE); p.text("+ " + (timeline.size()-9) + " more in JSON timeline", 48, Math.max(84,ty-2), 9, false); }
        color(p, NAVY); p.text("Collection and accuracy notes", 40, 83, 11, true);
        color(p, BLUE); p.text("Edit cadence, active time and method detection are estimates from sampled source text.", 40, 66, 8, false);
        p.text("Console text is imported by manual paste; standard compiler codes may be unavailable.", 40, 53, 8, false);
    }
}
