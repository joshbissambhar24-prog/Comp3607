# BlueJ Telemetry Analytics Plugin — Activity 2

Java 21 / BlueJ 5.4+ (including 6.x) / Maven / JUnit 5 / JaCoCo. This repo starts from the team's Activity 1 scope and extends it for Activity 2's mandatory deliverables. Due **November 9, 2026, 10:00 PM**.

## What is included

- BlueJ extension `edu.uwi.bluejtelemetry.bluej.BlueJTelemetryExtension` with a real BlueJ `Main-Class` manifest.
- Listeners: project/package, class state/rename/remove, user-generated compilation events, invocation completion and exception status.
- Bounded editor **snapshot polling every 3 seconds** to estimate changes, edit cadence and idle time (no keystroke interception).
- `Tools > Telemetry Analytics > Export JSON + PDF now` and `Import console output (paste summary)`.
- Session recording, compile interval/error aggregators, inferred active/idle time (60-second activity windows), typed JSON events and a two-page native Java PDF with charts/tables.
- JUnit 5 unit tests and JaCoCo 75% line coverage rule for core/export packages. Git repository files and a worked demo. See `docs/FIXES_AND_VERIFICATION.md` for archive updates.

## Build on Windows using PowerShell

1. Install **BlueJ 5.4 or newer**, **Java JDK 21**, and **Apache Maven**. Check Java and Maven in PowerShell:

   ```powershell
   java -version
   mvn -version
   ```

2. Copy BlueJ's own `bluej.jar` into this project. The location is normally `C:\Program Files\BlueJ\lib\bluej.jar`, but it may differ:

   ```powershell
   cd "C:\Users\YOUR_USER\Downloads\BlueJ-Telemetry-Activity2"
   New-Item -ItemType Directory -Force lib | Out-Null
   Copy-Item "C:\Program Files\BlueJ\lib\bluej.jar" ".\lib\bluej.jar"
   ```

   If BlueJ is installed elsewhere, browse its installation folder and find `lib\bluej.jar`. **Do not commit this jar to GitHub.** The project references it at compile time as `system` dependency; Maven never bundles it into the output extension JAR.

3. Build and test, generating JaCoCo coverage report:

   ```powershell
   .\build.ps1
   ```

   Or, if you copied the BlueJ jar manually, run `mvn clean verify`.

   Expected files: `target\bluej-telemetry-1.0.0.jar`, `target\site\jacoco\index.html`, and test reports in `target\surefire-reports`. `mvn clean verify` fails if the configured coverage rule is not met. A first build requires internet for Maven/JUnit/JavaFX dependencies.

4. Install the extension into a BlueJ project you created:

   ```powershell
   New-Item -ItemType Directory -Force "C:\PATH\TO\YOUR\BLUEJ_PROJECT\extensions2" | Out-Null
   Copy-Item ".\target\bluej-telemetry-1.0.0.jar" "C:\PATH\TO\YOUR\BLUEJ_PROJECT\extensions2\"
   ```

   Or put it in your global BlueJ extensions folder `C:\Users\YOUR_USER\bluej\extensions2`. **Restart BlueJ or reopen the project** to load the extension. This project uses the BlueJ 5+ `extensions2` directory, **not** the legacy `extensions` folder.

5. Open a BlueJ project, edit Java classes, compile (including intentionally breaking/fixing a line for test evidence), and invoke methods. In BlueJ, open **Tools > Telemetry Analytics > Export JSON + PDF now**. Output is saved to `<your BlueJ project>\telemetry-reports` and is also exported when the tracked root project closes. To get a console summary, copy terminal output, choose **Tools > Telemetry Analytics > Import console output (paste summary)**, then export again.

6. Verify the resulting JSON opens in a JSON viewer and the PDF has 2 pages. Run JUnit tests again after changes and take screenshots of `mvn clean verify`, the JaCoCo coverage report, BlueJ Tools menu and generated PDF/JSON.

## Verify demo exports without launching BlueJ

The core/export packages have no third-party runtime dependencies. Use Java 21 to run `edu.uwi.bluejtelemetry.demo.DemoMain` from compiled classes, or inspect `samples/sample.json` and `samples/sample.pdf` included in the ZIP.

## Patterns and SOLID responsibilities

- **Observer:** `TelemetryBus` broadcasts immutable `TelemetryEvent`s, and `SessionRecorder` subscribes.
- **Factory Method:** `EventCreator` subclasses create and validate lifecycle, editor, compilation and execution events.
- **Strategy:** `ReportExporter` has independent JSON and PDF strategies; `ExportService` selects both without knowing how they render.
- **SOLID:** `SourceTracker` owns editing detection, `MetricsAggregator` calculates values, listener adapter maps BlueJ callbacks, `SessionRecorder` holds a session, exporters perform output only. The aggregator and exporter logic can be unit-tested without BlueJ's GUI.

## Folder structure

```text
src/main/java/edu/uwi/bluejtelemetry/
  bluej/      BlueJ API/JavaFX integration
  core/       events, bus, session recorder, source sampler, metrics
  export/     JSON and PDF strategies; background file export
  demo/       sample session creation without BlueJ
src/test/java/edu/uwi/bluejtelemetry/
  core/       JUnit 5 logic tests
  export/     JUnit 5 serialization/PDF/export tests
docs/         updated scope and feature/limitation matrix
samples/      demonstration JSON and PDF
```

## Limitations you must explain in the demo / submission

The public BlueJ Extensions API does **not** provide a direct generic keystroke listener, authoritative editor focus/idle status, standardized compiler error codes or an automatic BlueJ Terminal stdout stream. Do not claim those features are directly observed. Sampling produces estimates; `METHOD_DEFINED` uses a regex heuristic and can miss advanced Java syntax; console summaries require deliberate manual paste. BlueJ project **creation/deletion** is not directly exposed by the package opened/closing events; this plugin tracks observed opening/closing, not guaranteed creation/deletion. A class can be created/deleted between 3-second polls without detection; class removal events reduce that risk. Runtime exception event status is available but stack traces are not provided through the invocation listener.

The JUnit tests do not replace an actual **BlueJ 6.x integration test**. Source was developed against BlueJ's documented `extensions2` API; verify compilation and menu behaviour with your installed `bluej.jar` and JDK before claiming the integration works.

Official docs: https://www.bluej.org/extensions/writingextensions2.html and https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/package-summary.html

## GitHub workflow (after the team reviews the source)

```powershell
git init
git add .
git commit -m "Implement BlueJ telemetry baseline with tests and reports"
git branch -M main
git remote add origin https://github.com/YOUR-TEAM/bluej-telemetry.git
git push -u origin main
```

Assign ownership: **Member 1** BlueJ adapter/source sampling, **Member 2** data model/metrics/JSON, **Member 3** PDF/tests/integration; all three should review and merge pull requests. Never commit student identifiers, telemetry data, BlueJ's proprietary installation jar or passwords.
