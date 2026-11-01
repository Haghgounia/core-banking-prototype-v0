$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$payload = Join-Path $PSScriptRoot 'files'
$expected = Get-Content (Join-Path $PSScriptRoot 'baseline-sha256.json') -Raw | ConvertFrom-Json
$targets = @(
  'backend/src/main/java/com/behsazan/corebanking/referencedata/education/descriptor/EduCanonicalDescriptorProvider.java',
  'backend/src/main/java/com/behsazan/corebanking/referencedata/education/web/EduLookupController.java'
)
$operations = @()
foreach ($rel in $targets) {
  $localFile = Join-Path $repo ($rel -replace '/', '\')
  $payloadFile = Join-Path $payload ($rel -replace '/', '\')
  if (-not (Test-Path $payloadFile)) { throw "PAYLOAD_MISSING $rel" }
  $newHash = (Get-FileHash $payloadFile -Algorithm SHA256).Hash.ToLowerInvariant()
  if (Test-Path $localFile) {
    $actual = (Get-FileHash $localFile -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -eq $newHash) { Write-Host "ALREADY_APPLIED $rel"; continue }
    $oldHash = ($expected.PSObject.Properties[$rel].Value).ToLowerInvariant()
    if ($actual -ne $oldHash) { throw "BASELINE_MISMATCH $rel Actual=$actual; do not overwrite locally modified source" }
  } elseif ($rel -notlike '*EduLookupController.java') { throw "REQUIRED_FILE_MISSING $rel" }
  $operations += @{ Source=$payloadFile; Target=$localFile; Relative=$rel }
}
$backupRoot = Join-Path $repo ('tools\edu-cif-r5\backups\' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
foreach ($item in $operations) {
  $backup = Join-Path $backupRoot ($item.Relative -replace '/', '\')
  if (Test-Path $item.Target) {
    New-Item -ItemType Directory -Force -Path (Split-Path $backup) | Out-Null
    Copy-Item $item.Target $backup -Force
  }
}
foreach ($item in $operations) {
  New-Item -ItemType Directory -Force -Path (Split-Path $item.Target) | Out-Null
  Copy-Item $item.Source $item.Target -Force
  Write-Host "APPLIED $($item.Relative)"
}
# Remove the disposable read-only test panel ONLY when both R4 files match exactly.
$html = Join-Path $repo 'frontend\src\app\features\reference-data\presentation\reference-page.component.html'
$ts = Join-Path $repo 'frontend\src\app\features\reference-data\presentation\reference-page.component.ts'
$h4 = (Get-Content (Join-Path $PSScriptRoot 'r4-html.sha256') -Raw).Trim().ToLowerInvariant()
$t4 = (Get-Content (Join-Path $PSScriptRoot 'r4-ts.sha256') -Raw).Trim().ToLowerInvariant()
if ((Test-Path $html) -and (Test-Path $ts) -and
   (Get-FileHash $html -Algorithm SHA256).Hash.ToLowerInvariant() -eq $h4 -and
   (Get-FileHash $ts -Algorithm SHA256).Hash.ToLowerInvariant() -eq $t4) {
  $htmlText = Get-Content $html -Raw -Encoding UTF8
  $tsText = Get-Content $ts -Raw -Encoding UTF8
  $htmlNew = $htmlText.Replace("  @if (store.resource().startsWith('edu-education-')) {`n    <app-edu-lookup-panel />`n  }`n", '')
  $tsNew = $tsText.Replace("import {EduLookupPanelComponent} from './edu-lookup-panel.component';`n", '').Replace(', EduLookupPanelComponent', '')
  if ($htmlNew -ne $htmlText -and $tsNew -ne $tsText) {
    foreach ($path in @($html,$ts)) {
      $relative = $path.Substring($repo.Length).TrimStart('\')
      $backup = Join-Path $backupRoot $relative
      New-Item -ItemType Directory -Force -Path (Split-Path $backup) | Out-Null
      Copy-Item $path $backup -Force
    }
    [System.IO.File]::WriteAllText($html,$htmlNew,[System.Text.UTF8Encoding]::new($false))
    [System.IO.File]::WriteAllText($ts,$tsNew,[System.Text.UTF8Encoding]::new($false))
    Write-Host 'R4_DEMO_PANEL_DETACHED; component file preserved for safe cleanup later'
  } else { Write-Host 'R4_DEMO_PANEL_CLEANUP_SKIPPED_LINE_ENDINGS_OR_STRUCTURE' }
} else { Write-Host 'R4_DEMO_PANEL_NOT_EXACT_MATCH; left unchanged for safety' }
Write-Host 'EDU_CIF_R5_SOURCE_APPLY_PASS; NO_DATABASE_CHANGES; BUILD_REQUIRED'
