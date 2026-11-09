package edu.uwi.bluejtelemetry.export;

import edu.uwi.bluejtelemetry.core.SessionSnapshot;
import java.io.IOException;
import java.nio.file.Path;

/** Strategy pattern: independent JSON and PDF exporters. */
public interface ReportExporter {
    String fileExtension();
    void export(SessionSnapshot snapshot, Path destination) throws IOException;
}
