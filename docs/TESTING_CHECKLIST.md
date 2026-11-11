# Manual verification checklist

Record a screenshot or exported JSON for each test. Check only the cases actually observed.

| Scenario | Expected observable output | Result |
|---|---|---|
| Project opened | `PROJECT_OPENED` | Not rerun on this revision |
| New Java class | `CLASS_CREATED` after first sampled creation | Not rerun |
| Class edit | `CLASS_MODIFIED`, `EDIT_DETECTED` | Not rerun |
| Class deletion | `CLASS_DELETED` | Not rerun |
| Valid compilation | `COMPILE_SUCCEEDED` | Not rerun |
| Compiler error | `COMPILATION_ERROR` with message and line | Not rerun |
| Method invocation | `EXECUTION_FINISHED` | Not rerun |
| Thrown exception | `RUNTIME_EXCEPTION` | Not rerun |
| Manual terminal import | `CONSOLE_OUTPUT_SUMMARY` | Not rerun |
| Manual export | JSON and 2-page PDF | Not rerun |

**Limitations:** typing cadence and idle time are estimates from sampled editor states; terminal data requires manual import; BlueJ may not expose standardized compiler error codes; project creation/deletion is not directly observable from package open/close callbacks. Do not present these estimates as exact measurements.
