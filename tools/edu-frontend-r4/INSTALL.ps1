param([string]$Root='')
$ErrorActionPreference='Stop'
if (-not $Root) {$Root=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path}
$target=Join-Path $Root 'frontend/src/app/features/reference-data/presentation'
$base=Join-Path $PSScriptRoot 'files'
$manifest=Get-Content (Join-Path $PSScriptRoot 'manifest.json') -Raw | ConvertFrom-Json
foreach($name in @('reference-page.component.ts','reference-page.component.html')) {
 $current=Join-Path $target $name
 if (-not (Test-Path $current)) {throw "Missing: $current"}
 $hash=(Get-FileHash $current -Algorithm SHA256).Hash.ToLowerInvariant()
 $expected=$manifest.baseline.$name
 $patched=(Get-FileHash (Join-Path $base $name) -Algorithm SHA256).Hash.ToLowerInvariant()
 if ($hash -ne $expected -and $hash -ne $patched) {throw "SOURCE_CONFLICT $name. No files were changed."}
}
$backup=Join-Path $Root ('patch-backups/EDU_FRONTEND_R4_'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force -Path $backup | Out-Null
foreach($name in @('reference-page.component.ts','reference-page.component.html')) {Copy-Item (Join-Path $target $name) (Join-Path $backup $name)}
foreach($name in @('reference-page.component.ts','reference-page.component.html','edu-lookup-panel.component.ts')) {Copy-Item (Join-Path $base $name) (Join-Path $target $name) -Force}
Write-Host 'EDU_FRONTEND_R4_INSTALLED=PASS'
Write-Host ('BACKUP='+$backup)
Write-Host 'NEXT: bin\\stop.cmd then build-production.cmd then bin\\start.cmd'
