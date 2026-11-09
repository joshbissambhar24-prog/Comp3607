package edu.uwi.bluejtelemetry.export;

import edu.uwi.bluejtelemetry.core.SessionSnapshot;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Single-threaded background exporting, snapshot-safe and without BlueJ GUI calls. */
public final class ExportService {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "bluej-telemetry-export"); thread.setDaemon(false); return thread;
    });
    private final List<ReportExporter> exporters;
    public ExportService(List<ReportExporter> exporters) { this.exporters = List.copyOf(exporters); }
    public CompletableFuture<List<Path>> export(SessionSnapshot s, Path directory) {
        Objects.requireNonNull(s); Objects.requireNonNull(directory);
        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(directory);
                String base = "telemetry-" + s.startedAt().toString().replaceAll("[^a-zA-Z0-9-]", "-")
                    + "-" + s.sessionId().substring(0, Math.min(8,s.sessionId().length()));
                java.util.ArrayList<Path> output = new java.util.ArrayList<>();
                for (ReportExporter exporter : exporters) {
                    Path dest = directory.resolve(base + exporter.fileExtension());
                    Path temporary = Files.createTempFile(directory, "telemetry-", ".tmp");
                    try {
                        exporter.export(s, temporary);
                        try { Files.move(temporary, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                        catch (AtomicMoveNotSupportedException e) {
                            Files.move(temporary, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                        output.add(dest);
                    } finally { Files.deleteIfExists(temporary); }
                }
                return List.copyOf(output);
            } catch (IOException e) { throw new RuntimeException("Telemetry export failed", e); }
        }, executor);
    }
    public void shutdownAndFlush() {
        executor.shutdown();
        try { if (!executor.awaitTermination(20, TimeUnit.SECONDS)) executor.shutdownNow(); }
        catch (InterruptedException e) { executor.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
