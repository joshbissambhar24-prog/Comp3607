# Updated archive — changes and verification

## What changed

1. `build.ps1` now looks for extracted `Downloads/BlueJ-windows-*/bluej/lib/bluej.jar`, in addition to the usual installation paths. You can still pass `-BlueJJar` explicitly.
2. JaCoCo updated from 0.8.12 to 0.8.14 for compatibility with newer JDK class files (including the Java 25 warning seen previously).
3. Runtime exception classification is now in a small, independently testable `InvocationOutcome` component used by the BlueJ listener.
4. Added JUnit tests for exception outcomes, console text summaries, and rename/delete event handling.

## Validate on the same computer used for the successful build

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\build.ps1
Start-Process .\target\site\jacoco\index.html
```

Expected: Maven `BUILD SUCCESS`, all JUnit tests passing, and JaCoCo's 75% line-coverage check passing. The actual results must be confirmed on your computer; this archive is source code, not a prebuilt BlueJ plugin.

Then copy `target/bluej-telemetry-1.0.0.jar` to your BlueJ test project's `extensions2` folder and reopen it. Repeat the compile/error/run/export test. For runtime exception tracking, deliberately run a method that throws e.g. `throw new IllegalStateException("test");` and check for `RUNTIME_EXCEPTION` in JSON. For console text, copy BlueJ Terminal output and use **Tools > Telemetry Analytics > Import console output (paste summary)** before exporting.

## Remaining API/data limitations

Typing cadence and idle time are estimates from sampled source text and detected actions, not literal key press timing. Console output requires manual paste, and class creation/deletion may be missed if it happens between polls. Project creation/deletion cannot be differentiated reliably from open/close via the extension events. The BlueJ compilation event API does not expose a standardized error code, so JSON uses `null` when the compiler message lacks an explicit code. Do not claim direct monitoring for these limitations.

The 75% rule is applied to the testable `core` and `export` packages. The live BlueJ/JavaFX adapter and demo are excluded from this threshold; use manual BlueJ integration testing as evidence for the adapter.
