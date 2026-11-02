$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$controller = Join-Path $repo 'backend\src\main\java\com\behsazan\corebanking\referencedata\education\web\EduLookupController.java'
$descriptor = Join-Path $repo 'backend\src\main\java\com\behsazan\corebanking\referencedata\education\descriptor\EduCanonicalDescriptorProvider.java'
if (-not (Test-Path -LiteralPath $controller) -or -not (Test-Path -LiteralPath $descriptor)) { throw 'R7_REQUIRED_FILES_MISSING' }
$utf8 = [System.Text.Encoding]::UTF8
$c = [System.IO.File]::ReadAllText($controller,$utf8)
$d = [System.IO.File]::ReadAllText($descriptor,$utf8)
$newC = $c
foreach ($key in @('fields','levels','institutions','field-levels')) {
  $oldSql = [System.IO.File]::ReadAllText((Join-Path $PSScriptRoot "$key.old.sql.txt"),$utf8).TrimEnd("`r","`n")
  $newSql = [System.IO.File]::ReadAllText((Join-Path $PSScriptRoot "$key.new.sql.txt"),$utf8).TrimEnd("`r","`n")
  $oldLine = '"' + $key + '", "' + $oldSql + '"'
  $newLine = '"' + $key + '", "' + $newSql + '"'
  if ($newC.Contains($oldLine)) {
    if ($newC.Split(@($oldLine),[System.StringSplitOptions]::None).Length -ne 2) { throw "R7_MULTIPLE_OLD_$key" }
    $newC = $newC.Replace($oldLine,$newLine)
    Write-Host "R7_REPLACE_$key"
  } elseif ($newC.Contains($newLine)) {
    Write-Host "R7_ALREADY_CIF_$key"
  } else { throw "R7_UNEXPECTED_QUERY_$key; NO_FILES_CHANGED" }
}
if ($newC -match 'EDU\.V_EDUCATION_' -or $newC -match 'EDU\.EDUCATION_') { throw 'R7_EDU_QUERY_REMAINS; NO_FILES_CHANGED' }
$oldSchema = 'private static final String SCHEMA = "EDU";'
$newSchema = 'private static final String SCHEMA = "CIF";'
if ($d.Contains($oldSchema)) {
  if ($d.Split(@($oldSchema),[System.StringSplitOptions]::None).Length -ne 2) { throw 'R7_DESCRIPTOR_UNEXPECTED; NO_FILES_CHANGED' }
  $newD = $d.Replace($oldSchema,$newSchema)
} elseif ($d.Contains($newSchema)) { $newD = $d }
else { throw 'R7_DESCRIPTOR_UNEXPECTED; NO_FILES_CHANGED' }
if ($newC -ceq $c -and $newD -ceq $d) { Write-Host 'EDU_CIF_R7_ALREADY_APPLIED_PASS'; exit 0 }
$backup = Join-Path $PSScriptRoot ('backups\'+(Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Path $backup -Force | Out-Null
Copy-Item -LiteralPath $controller -Destination (Join-Path $backup 'EduLookupController.java')
Copy-Item -LiteralPath $descriptor -Destination (Join-Path $backup 'EduCanonicalDescriptorProvider.java')
try {
  $noBom = New-Object System.Text.UTF8Encoding($false)
  if ($newC -cne $c) { [System.IO.File]::WriteAllText($controller,$newC,$noBom) }
  if ($newD -cne $d) { [System.IO.File]::WriteAllText($descriptor,$newD,$noBom) }
  $checkC=[System.IO.File]::ReadAllText($controller,$utf8)
  $checkD=[System.IO.File]::ReadAllText($descriptor,$utf8)
  if ($checkC -cne $newC -or $checkD -cne $newD) { throw 'R7_VERIFY_IO_FAILED' }
  foreach ($key in @('fields','levels','institutions','field-levels')) {
    $sql = [System.IO.File]::ReadAllText((Join-Path $PSScriptRoot "$key.new.sql.txt"),$utf8).TrimEnd("`r","`n")
    if (-not $checkC.Contains(('"'+$key+'", "'+$sql+'"'))) { throw "R7_VERIFY_$key" }
  }
  if (-not $checkD.Contains($newSchema)) { throw 'R7_VERIFY_DESCRIPTOR' }
} catch {
  Copy-Item -LiteralPath (Join-Path $backup 'EduLookupController.java') -Destination $controller -Force
  Copy-Item -LiteralPath (Join-Path $backup 'EduCanonicalDescriptorProvider.java') -Destination $descriptor -Force
  throw
}
Write-Host 'EDU_CIF_R7_SOURCE_PASS; FOUR_CIF_QUERIES; CIF_DESCRIPTOR; GEO_UNTOUCHED; NO_ORACLE_CHANGES'
Write-Host ('BACKUP=' + $backup)
Write-Host 'NEXT: bin\stop.cmd && build-production.cmd && bin\start.cmd (start only after successful build)'
