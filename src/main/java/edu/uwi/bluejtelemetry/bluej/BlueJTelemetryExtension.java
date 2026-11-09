package edu.uwi.bluejtelemetry.bluej;

import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;
import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;
import bluej.extensions2.MenuGenerator;
import bluej.extensions2.SourceType;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ClassListener;
import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileListener;
import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.InvocationFinishedListener;
import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;
import edu.uwi.bluejtelemetry.core.*;
import edu.uwi.bluejtelemetry.export.*;
import javafx.application.Platform;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Main-Class entry point loaded by BlueJ 5+/6; intentionally does not include BlueJ API in JAR. */
public final class BlueJTelemetryExtension extends Extension
    implements PackageListener, ClassListener, CompileListener, InvocationFinishedListener {

    private final Clock clock = Clock.systemUTC();
    private final SessionRecorder recorder = new SessionRecorder(clock);
    private final TelemetryBus bus = new TelemetryBus();
    private final SourceTracker tracker = new SourceTracker(clock, bus);
    private final EventCreator.Lifecycle lifecycle = new EventCreator.Lifecycle();
    private final EventCreator.Compiler compiler = new EventCreator.Compiler();
    private final EventCreator.Execution execution = new EventCreator.Execution();
    private final ErrorClassifier errorClassifier = new ErrorClassifier();
    private final MetricsAggregator aggregator = new MetricsAggregator();
    private final ExportService exports = new ExportService(List.of(
        new JsonReportExporter(aggregator), new PdfReportExporter(aggregator)));
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "bluej-telemetry-sampler"); t.setDaemon(true); return t;
    });
    private final AtomicBoolean samplingPending = new AtomicBoolean();
    private volatile BlueJ api;
    private volatile BProject project;
    private volatile Path projectDirectory;

    public BlueJTelemetryExtension() { bus.subscribe(recorder::accept); }

    @Override public void startup(BlueJ bluej) {
        api = bluej;
        bluej.addPackageListener(this);
        bluej.addClassListener(this);
        bluej.addCompileListener(this);
        bluej.addInvocationFinishedListener(this);
        bluej.setMenuGenerator(new MenuGenerator() {
            @Override public MenuItem getToolsMenuItem(BPackage pkg) {
                Menu menu = new Menu("Telemetry Analytics");
                MenuItem export = new MenuItem("Export JSON + PDF now");
                export.setOnAction(event -> exportCurrent());
                MenuItem paste = new MenuItem("Import console output (paste summary)");
                paste.setOnAction(event -> importConsole());
                menu.getItems().addAll(export, paste);
                menu.setDisable(!recorder.active());
                return menu;
            }
            @Override public void notifyPostToolsMenu(BPackage pkg, MenuItem item) {
                item.setDisable(!recorder.active());
            }
        });
        try {
            BProject[] open = bluej.getOpenProjects();
            if (open.length > 0) startProject(open[0]);
        } catch (Exception e) { log("No initial project: " + e.getMessage()); }
        scheduler.scheduleAtFixedRate(() -> {
            if (!recorder.active() || !samplingPending.compareAndSet(false, true)) return;
            try { Platform.runLater(() -> {
                try { sampleActiveProject(); }
                finally { samplingPending.set(false); }
            }); } catch (RuntimeException e) { samplingPending.set(false); }
        }, 3, 3, TimeUnit.SECONDS);
    }

    private synchronized void startProject(BProject p) {
        if (recorder.active()) return; // Explicit scope: only one active project/session.
        try {
            projectDirectory = p.getDir().toPath();
            recorder.start(p.getName(), System.getProperty("bluej.telemetry.studentId", "STUDENT_ID_PLACEHOLDER"));
            project = p;
            tracker.reset();
            bus.publish(lifecycle.create(EventType.PROJECT_OPENED, p.getName(), clock.instant(),
                Map.of("project_directory", projectDirectory.toString())));
            sampleActiveProject();
            log("Session started: " + projectDirectory);
        } catch (Exception e) { log("Cannot open telemetry session: " + e.getMessage()); }
    }

    /** Read in-memory Java editor content where possible. Never saves/modifies the student's code. */
    private void sampleActiveProject() {
        BProject p = project;
        if (p == null || !recorder.active()) return;
        Map<String,String> sources = new HashMap<>();
        try {
            for (BPackage pkg : p.getPackages()) {
                for (BClass cls : pkg.getClasses()) {
                    try {
                        if (cls.getSourceType() != SourceType.Java) continue;
                        String name = cls.getName();
                        JavaEditor open = cls.getJavaEditorIfOpen();
                        String current = null;
                        if (open != null) {
                            int length = open.getTextLength();
                            if (length <= 200_000) {
                                current = open.getText(open.getTextLocationFromOffset(0),
                                    open.getTextLocationFromOffset(length));
                            }
                        } else {
                            String simple = name.substring(name.lastIndexOf('.')+1);
                            Path file = pkg.getDir().toPath().resolve(simple + ".java");
                            if (Files.isRegularFile(file) && Files.size(file) <= 200_000)
                                current = Files.readString(file, StandardCharsets.UTF_8);
                        }
                        if (current != null) sources.put(name, current);
                        else {
                            String cached = tracker.knownSources().get(name);
                            if (cached != null) sources.put(name, cached);
                        }
                    } catch (Exception ignored) { /* Isolate problematic class; other classes still sampled. */ }
                }
            }
            tracker.sample(sources);
        } catch (Exception e) {
            // When a project closes, its handles become invalid. Avoid reporting false deletions.
            log("Sampler skipped: " + e.getMessage());
        }
    }

    @Override public void packageOpened(PackageEvent event) {
        try { if (!recorder.active()) startProject(event.getPackage().getProject()); }
        catch (Exception e) { log("Project open event error: " + e.getMessage()); }
    }
    @Override public void packageClosing(PackageEvent event) {
        try {
            // Root package indicates the tracked project's main window is closing.
            if (recorder.active() && project != null &&
                event.getPackage().getProject().getDir().toPath().equals(projectDirectory) &&
                event.getPackage().getName().isEmpty()) endProject();
        } catch (Exception e) { log("Project close event error: " + e.getMessage()); }
    }
    private synchronized void endProject() {
        if (!recorder.active()) return;
        try {
            bus.publish(lifecycle.create(EventType.PROJECT_CLOSED, recorder.snapshot().projectName(), clock.instant(), Map.of()));
            var completed = recorder.end();
            export(completed);
            project = null;
            tracker.reset();
        } catch (Exception e) { log("Could not end project: " + e.getMessage()); }
    }
    private void importConsole() {
        if (!recorder.active()) return;
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Telemetry Console Output Import");
        dialog.setHeaderText("Copy text from the BlueJ Terminal and paste it below.");
        TextArea area = new TextArea();
        area.setPromptText("Paste BlueJ Terminal output here...");
        area.setPrefRowCount(10);
        area.setWrapText(true);
        dialog.getDialogPane().setContent(area);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == ButtonType.OK ? area.getText() : null);
        dialog.showAndWait().filter(text -> !text.isBlank()).ifPresent(text -> {
            bus.publish(execution.create(EventType.CONSOLE_OUTPUT_SUMMARY,"BlueJ Terminal",clock.instant(),
                new ConsoleSummaryParser().summarize(text)));
            log("Console output summary saved in session; select Export JSON + PDF now.");
        });
    }

    private void exportCurrent() {
        if (!recorder.active()) { log("No active telemetry session"); return; }
        try { sampleActiveProject(); export(recorder.snapshot()); }
        catch (Exception e) { log("Export request failed: " + e.getMessage()); }
    }
    private void export(SessionSnapshot snapshot) {
        Path dir = projectDirectory.resolve("telemetry-reports");
        exports.export(snapshot, dir).whenComplete((paths, error) -> {
            if (error != null) log("Telemetry export failed: " + error.getMessage());
            else log("JSON and PDF saved to: " + dir);
        });
    }

    @Override public void classStateChanged(ClassEvent event) {
        if (!recorder.active()) return;
        // A change callback can occur repeatedly during typing. The 3-second sampler
        // does the actual work; do not enqueue an FX task for every editor change.
    }
    @Override public void classNameChanged(ClassEvent event) {
        if (!recorder.active()) return;
        try { tracker.rename(event.getOldName(), event.getBClass().getName()); }
        catch (Exception e) { log("Rename could not be tracked: " + e.getMessage()); }
    }
    @Override public void classRemoved(ClassEvent event) {
        if (!recorder.active()) return;
        try { tracker.remove(event.getBClass().getName()); }
        catch (Exception e) { log("Class deletion could not be tracked: " + e.getMessage()); }
    }
    @Override public void compileStarted(CompileEvent event) {
        if (!recorder.active() || !event.isUserGeneratedCompilation()) return;
        bus.publish(compiler.create(EventType.COMPILE_STARTED, files(event), clock.instant(), Map.of()));
    }
    @Override public void compileSucceeded(CompileEvent event) {
        if (!recorder.active() || !event.isUserGeneratedCompilation()) return;
        bus.publish(compiler.create(EventType.COMPILE_SUCCEEDED, files(event), clock.instant(), Map.of()));
    }
    @Override public void compileFailed(CompileEvent event) {
        if (!recorder.active() || !event.isUserGeneratedCompilation()) return;
        bus.publish(compiler.create(EventType.COMPILE_FAILED, files(event), clock.instant(), Map.of()));
    }
    @Override public void compileError(CompileEvent event) {
        recordCompilerDiagnostic(event, EventType.COMPILATION_ERROR);
    }
    @Override public void compileWarning(CompileEvent event) {
        recordCompilerDiagnostic(event, EventType.COMPILATION_WARNING);
    }
    private void recordCompilerDiagnostic(CompileEvent event, EventType t) {
        if (!recorder.active() || !event.isUserGeneratedCompilation()) return;
        String message = event.getErrorMessage() == null ? "" : event.getErrorMessage();
        Map<String,Object> attrs = new HashMap<>();
        attrs.put("message", message);
        attrs.put("line_number", event.getErrorLineNumber());
        attrs.put("category", errorClassifier.category(message));
        attrs.put("error_code", errorClassifier.explicitCode(message));
        bus.publish(compiler.create(t, files(event), clock.instant(), attrs));
    }
    private String files(CompileEvent event) {
        java.io.File[] files = event.getFiles();
        return files == null || files.length == 0 ? "" : files[0].getName();
    }
    @Override public void invocationFinished(InvocationFinishedEvent event) {
        if (!recorder.active()) return;
        String status = event.getEventType().name();
        String cls = event.getClassName() == null ? "" : event.getClassName();
        String method = InvocationOutcome.methodName(event.getMethodName());
        bus.publish(execution.create(EventType.EXECUTION_FINISHED, cls, clock.instant(),
            Map.of("method", method, "status", status)));
        if (InvocationOutcome.isRuntimeException(status))
            bus.publish(execution.create(EventType.RUNTIME_EXCEPTION, cls, clock.instant(),
                Map.of("method", method, "exception_details_available", false)));
    }
    @Override public void terminate() {
        scheduler.shutdownNow();
        endProject();
        exports.shutdownAndFlush();
    }
    private void log(String message) { System.out.println("[BlueJ Telemetry] " + message); }
    @Override public boolean isCompatible() { return getExtensionsAPIVersionMajor() >= 3; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getName() { return "OOP1 BlueJ Telemetry"; }
    @Override public String getDescription() { return "Records Java class, edit, compilation and invocation telemetry; exports JSON and PDF."; }
    @Override public URL getURL() {
        try { return new URL("https://www.bluej.org/extensions/writingextensions2.html"); }
        catch (MalformedURLException e) { return null; }
    }
}
