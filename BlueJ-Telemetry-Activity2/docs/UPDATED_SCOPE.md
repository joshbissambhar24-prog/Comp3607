# Updated Project Scope — Activity 2

**Question 1: Telemetry Data Collection Boundaries**

Our Java plugin will monitor one active BlueJ project per session. It will record project opening and closure; discovered Java class creation, modification, renaming and deletion; user-generated compilation attempts, outcomes, diagnostic messages, categories and line numbers; and method invocation outcomes, including runtime exception status. Where BlueJ exposes no standardized diagnostic code, the code will be null instead of invented. Editor interactions will be estimated using Java source snapshots approximately every three seconds, recording edit batches, approximate changed-character counts and method-definition changes. Typing cadence and active versus idle time will be estimates rather than individual keystroke measurements. Mouse movements and unrelated IDE activity are excluded.

**Question 2: Deliverables and Report Specifications**

The plugin will generate a timestamped JSON file and a professional PDF report of at most two pages. JSON will contain a session ID, student ID placeholder, project name, start/end times, session duration, user-generated compilation attempts, success and failure counts, error messages and lines, execution status, compile intervals, edit activity, active/idle estimates, class/method progression and all recorded events. The PDF will show a statistics table, active versus idle time comparison, compilation-success-versus-failure bar chart, compilation activity line chart, error-category frequency breakdown and class/method timeline. Console text can be copied from the BlueJ terminal and manually summarised through the plugin; automatic console interception is not available in the public API.

**Question 3: Technical Integration and Performance**

The solution will use Java 21, the BlueJ Extensions API, Maven, GitHub and JUnit 5. Observer will decouple event collection from storage, Factory Method will create typed event records, and Strategy will separate JSON from PDF exporters. SOLID responsibilities will be assigned across listener adapters, source trackers, event models, aggregators and export services. Java editor reads will be bounded and sampled rather than performed on each keystroke, while JSON/PDF exports will run on a background executor to keep the GUI responsive. Data will remain in memory during the single-project session, with snapshots exported manually or when the project closes. JaCoCo will enforce 75%+ line coverage for testable core/export packages.
