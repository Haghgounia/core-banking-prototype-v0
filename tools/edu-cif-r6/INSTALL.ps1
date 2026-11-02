$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$controller = Join-Path $repo 'backend\src\main\java\com\behsazan\corebanking\referencedata\education\web\EduLookupController.java'
$descriptor = Join-Path $repo 'backend\src\main\java\com\behsazan\corebanking\referencedata\education\descriptor\EduCanonicalDescriptorProvider.java'
$payload = Join-Path $PSScriptRoot 'EduLookupController.java'
$oldHash = '59913095ce7d597e16438a5502a38515e8cfb4f03b9ec0e194554d7ee7dd4a4a'
$newHash = '8fd5118f912860cbab5041955934121fcc8204bf91449f23561a0ed69b87ec8d'
if (-not (Test-Path $controller) -or -not (Test-Path $descriptor)) { throw 'EDU_R6_REQUIRED_SOURCE_MISSING' }
$cHash = (Get-FileHash -LiteralPath $controller -Algorithm SHA256).Hash.ToLowerInvariant()
if ($cHash -ne $oldHash -and $cHash -ne $newHash) { throw "EDU_R6_CONTROLLER_UNEXPECTED_HASH=$cHash; STOP_NO_FILES_CHANGED" }
$dText = [System.IO.File]::ReadAllText($descriptor,[System.Text.Encoding]::UTF8)
$eduNeedle = 'private static final String SCHEMA = "EDU";'
$cifNeedle = 'private static final String SCHEMA = "CIF";'
if ($dText.Contains($eduNeedle)) {
  if ($dText.Split(@($eduNeedle),[System.StringSplitOptions]::None).Length -ne 2) { throw 'EDU_R6_DESCRIPTOR_MULTIPLE_SCHEMA_DECLARATIONS' }
  $newD = $dText.Replace($eduNeedle,$cifNeedle)
} elseif ($dText.Contains($cifNeedle)) {
  $newD = $dText
} else { throw 'EDU_R6_DESCRIPTOR_SCHEMA_UNEXPECTED; STOP_NO_FILES_CHANGED' }
$applyC = ($cHash -eq $oldHash)
$applyD = ($newD -cne $dText)
if (-not $applyC -and -not $applyD) { Write-Host 'EDU_CIF_R6_ALREADY_APPLIED_PASS'; exit 0 }
$backupRoot = Join-Path $repo ('tools\edu-cif-r6\backups\' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
if ($applyC) { Copy-Item -LiteralPath $controller -Destination (Join-Path $backupRoot 'EduLookupController.java') -Force }
if ($applyD) { Copy-Item -LiteralPath $descriptor -Destination (Join-Path $backupRoot 'EduCanonicalDescriptorProvider.java') -Force }
try {
  if ($applyC) { Copy-Item -LiteralPath $payload -Destination $controller -Force }
  if ($applyD) { [System.IO.File]::WriteAllText($descriptor,$newD,[System.Text.UTF8Encoding]::new($false)) }
  $afterC = (Get-FileHash -LiteralPath $controller -Algorithm SHA256).Hash.ToLowerInvariant()
  $afterD = [System.IO.File]::ReadAllText($descriptor,[System.Text.Encoding]::UTF8)
  if ($afterC -ne $newHash -or -not $afterD.Contains($cifNeedle) -or $afterD.Contains($eduNeedle)) { throw 'EDU_R6_POSTCHECK_FAILED' }
} catch {
  if ($applyC) { Copy-Item (Join-Path $backupRoot 'EduLookupController.java') $controller -Force }
  if ($applyD) { Copy-Item (Join-Path $backupRoot 'EduCanonicalDescriptorProvider.java') $descriptor -Force }
  throw
}
Write-Host 'EDU_CIF_R6_SOURCE_CUTOVER_PASS; NO_ORACLE_CHANGES; GEO_UNTOUCHED; BUILD_REQUIRED'
Write-Host ('BACKUP_DIR=' + $backupRoot)
