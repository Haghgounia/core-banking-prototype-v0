param(
  [Parameter(Mandatory = $true)][string] $ProjectRoot,
  [switch] $Apply
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
try {
  $packageRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
  $filesRoot = Join-Path $packageRoot 'files'
  $manifest = Get-Content -LiteralPath (Join-Path $packageRoot 'manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
  $project = (Resolve-Path -LiteralPath $ProjectRoot).Path
  foreach ($marker in @('backend\pom.xml','frontend\package.json','tools\verify-org-forms.mjs')) {
    if (-not (Test-Path -LiteralPath (Join-Path $project $marker))) { throw "Not a Core Banking Prototype project: missing $marker" }
  }
  $changes = New-Object System.Collections.Generic.List[object]
  foreach ($item in $manifest.files) {
    $relative = $item.path.Replace('/', '\')
    $source = Join-Path $filesRoot $relative
    $target = Join-Path $project $relative
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { throw "Package missing: $relative" }
    $payloadHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $source).Hash.ToLowerInvariant()
    if ($payloadHash -ne $item.targetHash) { throw "Patch integrity check failed: $relative" }
    $exists = Test-Path -LiteralPath $target -PathType Leaf
    $currentHash = if ($exists) {(Get-FileHash -Algorithm SHA256 -LiteralPath $target).Hash.ToLowerInvariant()} else {''}
    if ($currentHash -eq $item.targetHash) { continue }
    if ($item.originalHash -eq '') {
      if ($exists) { throw "Unexpected existing new file: $relative. STOP: do not overwrite another patch." }
    } elseif (-not $exists -or $currentHash -ne $item.originalHash) {
      throw "Source version mismatch: $relative. STOP: patch only applies to source ZIP 2026-10-19-1000."
    }
    $changes.Add([PSCustomObject]@{Relative=$relative; Source=$source; Target=$target; Exists=$exists})
  }
  Write-Host "ORG-GRADE P3/P4 PRECHECK PASS: $($manifest.files.Count) files, $($changes.Count) pending changes."
  if (-not $Apply) {
    Write-Host 'CHECK ONLY; NO FILES CHANGED. Rerun with APPLY to install.'
    exit 0
  }
  if ($changes.Count -eq 0) { Write-Host 'ALREADY APPLIED: no files changed.'; exit 0 }
  $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
  $backupRoot = Join-Path $packageRoot ('backups\' + $stamp)
  New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
  foreach ($change in $changes) {
    if ($change.Exists) {
      $backupTarget = Join-Path $backupRoot $change.Relative
      New-Item -ItemType Directory -Force -Path (Split-Path -Parent $backupTarget) | Out-Null
      Copy-Item -LiteralPath $change.Target -Destination $backupTarget -Force
    }
  }
  Write-Host "Backup created: $backupRoot"
  foreach ($change in $changes) {
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $change.Target) | Out-Null
    Copy-Item -LiteralPath $change.Source -Destination $change.Target -Force
    Write-Host "INSTALLED $($change.Relative)"
  }
  Write-Host 'ORG-GRADE P3/P4 FILE INSTALL COMPLETED. NO ORACLE SQL EXECUTED.'
  exit 0
} catch {
  Write-Error $_
  exit 1
}
