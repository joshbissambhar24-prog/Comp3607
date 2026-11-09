# Activity 2 Requirement / Implementation Matrix

| Requirement | Status in source | Caveat |
|---|---|---|
| Project opening and closure | Implemented | Root package closing / extension termination; project creation/deletion cannot be reliably inferred from events |
| Java class creation, modification and deletion | Implemented | Creation/deletion detected by 3s snapshots and direct class listener; initial classes are marked present, not created |
| Typing cadence, idle time, edit frequency | Estimated | Changes detected from editor buffer snapshots, not raw keystroke timestamps; idle is inferred via 60s activity windows |
| Compile attempts, success/fail | Implemented | Only `isUserGeneratedCompilation` events counted |
| Errors and line numbers | Implemented | Uses BlueJ messages, error line; standardized diagnostic codes absent so optional regex extraction, otherwise `null` |
| Execution outcomes / exceptions | Implemented | `InvocationFinishedEvent` only; exception types and stacktraces are not available in that callback |
| Console output summaries | Partial, manual | User must copy BlueJ Terminal text and paste into Tools > Telemetry Analytics > Import console output; no automatic capture |
| JSON event stream / metadata | Implemented | Schema v1.0; session metadata, metrics, timestamped events, optional diagnostic fields |
| PDF 2-page report | Implemented | One bar chart, one cumulative compile line chart, active/idle comparison, error table, class/method timeline |
| JUnit 5 unit tests | Source included | Run `mvn clean verify` on machine with Maven, internet and BlueJ jar |
| JaCoCo 75% minimum | Configured | Excludes BlueJ GUI adapter / demo, subject to running tests locally |
| Three design patterns | Implemented | Observer, Factory Method, Strategy |
| SOLID Java / Maven / GitHub | Implemented as code structure | Create/push remote GitHub repo after reviewing and team commits |

**Important:** This is a development baseline, not evidence of successful testing inside a real BlueJ installation. Integration testing, JUnit coverage results, and GUI behaviour must be checked on the team's machines before submission.
