# ERS one-shot regression: backend tests + frontend build.
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File scripts\ci.ps1
# Exit code 0 = both stages passed. Logs: .tmp\ci_backend.log, .tmp\ci_frontend.log
# PowerShell 5.1 treats native-command stderr as an error record; with 'Stop' the
# JVM's "restricted method" warning aborts the run, so keep it at 'Continue' and
# judge each stage by the log text instead of by exit code.
$ErrorActionPreference = 'Continue'

$root = Split-Path -Parent $PSScriptRoot
$tmp  = Join-Path $root '.tmp'
$m2   = Join-Path $root '.tools\m2repo'
New-Item -ItemType Directory -Force -Path $tmp | Out-Null

Write-Host '[1/2] backend: mvn test (isolated db ers_test)'
$beLog = Join-Path $tmp 'ci_backend.log'
& mvn -B -f (Join-Path $root 'ers-backend\pom.xml') "-Dmaven.repo.local=$m2" test *> $beLog
Select-String -Path $beLog -Pattern 'Tests run:.*Skipped' | Select-Object -Last 1 | ForEach-Object { Write-Host $_.Line }
# mvn exit code is unreliable under PowerShell pipes (dev log 6.13): judge by log content.
if (-not (Select-String -Path $beLog -Pattern 'BUILD SUCCESS' -Quiet)) {
    Write-Host "[FAIL] backend tests, see $beLog"
    exit 1
}

Write-Host '[2/2] frontend: npm run build'
$web   = Join-Path $root 'ers-web'
$feLog = Join-Path $tmp 'ci_frontend.log'
$env:ESBUILD_BINARY_PATH = Join-Path $web 'node_modules\@esbuild\win32-x64\esbuild.exe'
$env:TMP  = Join-Path $tmp 'esbtmp'
$env:TEMP = $env:TMP
New-Item -ItemType Directory -Force -Path $env:TMP | Out-Null
Push-Location $web
try {
    & npm run build *> $feLog
} finally {
    Pop-Location
}
Select-String -Path $feLog -Pattern 'modules transformed|built in' | ForEach-Object { Write-Host $_.Line }
if (-not (Select-String -Path $feLog -Pattern 'built in' -Quiet)) {
    Write-Host "[FAIL] frontend build, see $feLog"
    exit 1
}

Write-Host '[OK] backend tests + frontend build passed.'
exit 0
