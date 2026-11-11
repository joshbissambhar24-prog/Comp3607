# BlueJ Telemetry Analytics Plugin

A Java extension for **BlueJ** that records programming-session activity and generates structured **JSON telemetry** and a **two-page PDF analytics report**. Developed for the **COMP3607 Object-Oriented Programming** team project at The University of the West Indies, St. Augustine.

**Repository:** https://github.com/joshbissambhar24-prog/Comp3607

## Overview

The plugin helps describe how a learner works on a Java project in BlueJ. It monitors supported project, class, compilation and execution events, samples source-code changes, and calculates session metrics. Users can export the session at any time or when the project closes.

### Features

| Area | Functionality |
| --- | --- |
| Project lifecycle | Records project opening and closing |
| Class lifecycle | Detects class creation, modifications, renaming and deletion where observable |
| Editor activity | Samples source content approximately every 3 seconds to estimate edit batches, changed characters, editing cadence and idle time |
| Compilation | Counts user-generated attempts, successes and failures; records diagnostic messages, categories and line numbers |
| Execution | Records supported method/constructor invocations and their completion or exception status |
| Console output | Allows users to **manually paste** BlueJ terminal text for a summary |
| JSON export | Produces timestamped event records, session metadata and aggregate metrics |
| PDF report | Produces a two-page report with compilation charts, active/idle estimates, error frequencies and class/method progression |

## Requirements

- **Java JDK 21**
- **Apache Maven**
- **BlueJ 5.4+** (tested with **BlueJ 6.0.1** on Windows)
- **Windows PowerShell** to run the supplied build script

The build compiles against BlueJ's local `bluej.jar`. This dependency is not included in the repository.

## Build and installation

1. Clone the repository and open it in VS Code or a terminal:

   ```powershell
   git clone https://github.com/joshbissambhar24-prog/Comp3607.git
   cd Comp3607
   ```

2. Verify Java and Maven:

   ```powershell
   java -version
   mvn -version
   ```

3. Build the extension and run tests:

   ```powershell
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
   .\build.ps1
   ```

   The script looks for `bluej.jar` in typical BlueJ installation paths and extracted BlueJ Windows downloads. If it cannot find the file, supply its location explicitly:

   ```powershell
   .\build.ps1 -BlueJJar "C:\path\to\bluej\lib\bluej.jar"
   ```

   A successful build creates:

   - `target/bluej-telemetry-1.0.0.jar` — extension to install
   - `target/site/jacoco/index.html` — coverage report
   - `target/surefire-reports/` — JUnit results

4. Create or open a BlueJ project, then make an **`extensions2`** folder inside that project directory. Copy `target/bluej-telemetry-1.0.0.jar` into it:

   ```text
   YourBlueJProject/
   ├── package.bluej
   └── extensions2/
       └── bluej-telemetry-1.0.0.jar
   ```

5. Close and reopen the BlueJ project. The extension should appear under **Tools → Telemetry Analytics**.

## Using the extension

1. Create, edit and compile Java classes in BlueJ. Include a successful and a failed compilation to test diagnostics.
2. Create an object and invoke a method to record supported execution activity.
3. To include console content, copy text from the BlueJ terminal and choose **Tools → Telemetry Analytics → Import console output (paste summary)**.
4. Select **Tools → Telemetry Analytics → Export JSON + PDF now**.
5. Find the output files in the BlueJ project's **`telemetry-reports/`** folder. The plugin also attempts an export when the tracked project closes.

The **JSON** contains session metadata (including a configurable student ID placeholder), timestamped events, compilation/error details, compile intervals, execution counts, and editing/activity estimates. The **PDF** summarizes the session in two pages using charts and tables. Examples are in [`samples/`](samples/).

## Testing and coverage

Run the test suite and generate the coverage report with:

```powershell
.\build.ps1
Start-Process ".\target\site\jacoco\index.html"
```

The most recently verified local build (November 9, 2026) reported:

| Measure | Result |
| --- | ---: |
| JUnit 5 tests | **20 passed; 0 failed** |
| Instruction coverage | **95%** |
| Branch coverage | **80%** |
| Maven build | **BUILD SUCCESS** |

JaCoCo enforces **at least 75% line coverage** on the testable core and export code. **Coverage percentages exclude the BlueJ GUI adapter and demo package**; these figures are not whole-project coverage. BlueJ 6.0.1 integration was also exercised manually, including compiling Java code and exporting PDF/JSON reports.

## Architecture and design patterns

```text
src/main/java/edu/uwi/bluejtelemetry/
├── bluej/    BlueJ Extensions API adapter and menu integration
├── core/     Events, session recorder, source sampling and metric calculations
├── export/   JSON/PDF exporters and export service
└── demo/     Standalone sample-session generator

src/test/java/edu/uwi/bluejtelemetry/
├── core/     Event, recorder, editing and metric tests
└── export/   JSON/PDF and export-service tests

docs/         Scope, feature matrix and verification notes
samples/      Example telemetry JSON and PDF
```

The design uses **three patterns (no Singleton)**:

- **Observer:** `TelemetryBus` notifies `SessionRecorder` of telemetry events.
- **Factory Method:** `EventCreator` produces validated event objects for different activity categories.
- **Strategy:** `ReportExporter` defines interchangeable JSON and PDF export implementations used by `ExportService`.

The implementation separates BlueJ integration, event recording, metric calculation and file generation to support **SOLID principles** and independently testable components.

## Known limitations

- **Typing cadence and idle time are estimates** based on periodic editor snapshots and activity windows, not individual keystrokes or guaranteed editor-focus measurements.
- **Console text is not captured automatically**; users must paste it through the plugin menu.
- **Standard compiler diagnostic codes may be unavailable** through BlueJ; missing codes are represented as `null`, while messages and line numbers are recorded when supplied.
- **Project creation/deletion is not directly exposed** as a reliable lifecycle distinction by the public extension events; project open/close events are recorded instead.
- **Method definitions are inferred** from source text and may not identify every Java syntax form.
- Runtime exception status is recorded where exposed by BlueJ, but full exception stack traces are not necessarily available.

## Documentation

- [Project scope](docs/UPDATED_SCOPE.md)
- [Requirement and limitation matrix](docs/FEATURE_MATRIX.md)
- [Fixes and verification notes](docs/FIXES_AND_VERIFICATION.md)
- [BlueJ Extensions API documentation](https://www.bluej.org/extensions/writingextensions2.html)

## Team and academic context

Built as a **three-member team project** for COMP3607 at UWI St. Augustine. Add the names and contributions of the three team members here before final submission.

**Note:** `STUDENT_ID_PLACEHOLDER` is intentional as a default metadata value. Avoid committing personal student data or real student telemetry to a public repository without permission.
