# Run this in PowerShell at the project root; edit -BlueJJar if installed elsewhere.
param([string]$BlueJJar = "", [switch]$SkipTests)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($BlueJJar)) {
    $candidates = @(
        "$env:ProgramFiles\BlueJ\lib\bluej.jar",
        "${env:ProgramFiles(x86)}\BlueJ\lib\bluej.jar",
        "$env:LOCALAPPDATA\Programs\BlueJ\lib\bluej.jar"
    )
    $BlueJJar = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if (-not $BlueJJar -or -not (Test-Path $BlueJJar)) {
    throw "Could not find BlueJ's lib\bluej.jar. Run .\build.ps1 -BlueJJar 'C:\path\to\BlueJ\lib\bluej.jar'"
}
New-Item -ItemType Directory -Force -Path .\lib | Out-Null
Copy-Item -Path $BlueJJar -Destination .\lib\bluej.jar -Force
Write-Host "Compiling against $BlueJJar"
if ($SkipTests) { mvn clean package -DskipTests } else { mvn clean verify }
if ($LASTEXITCODE -ne 0) { throw "Maven build failed. Review errors above." }
Write-Host "Extension JAR: $PSScriptRoot\target\bluej-telemetry-1.0.0.jar"
Write-Host "JaCoCo report: $PSScriptRoot\target\site\jacoco\index.html"
