param(
 [Parameter(Mandatory=$true)][string]$ProjectRoot,
 [switch]$Approve
)
$ErrorActionPreference='Stop'
try {
  if (-not $Approve) { throw 'No changes made. Specify -Approve after reviewing the patch.' }
  $patch=(Resolve-Path -LiteralPath $PSScriptRoot).Path
  $target=(Resolve-Path -LiteralPath $ProjectRoot).Path
  $originals=@{
    'backend/src/main/java/com/behsazan/corebanking/deposit/account/transaction/application/DepositTransactionService.java'='2b6a472776abc40ae8c632c815453afb659f5fb10bd064a33571b12746d3e710';
    'backend/src/main/java/com/behsazan/corebanking/deposit/account/transaction/oracle/DepositTransactionRepository.java'='7c97586d2749db3929e30909abecea679a05c9f4dd092fe56193f8c176da5320'
  }
  foreach ($rel in $originals.Keys) {
    $relative=$rel.Replace('/',[IO.Path]::DirectorySeparatorChar)
    $dest=Join-Path $target $relative
    $source=Join-Path $patch $relative
    if (-not (Test-Path -LiteralPath $dest -PathType Leaf)) { throw "Expected baseline missing: $rel" }
    $hash=(Get-FileHash -LiteralPath $dest -Algorithm SHA256).Hash.ToLowerInvariant()
    $patchHash=(Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($hash -ne $originals[$rel] -and $hash -ne $patchHash) {
      throw "BASELINE_MISMATCH - refusing overwrite: $rel. This patch only supports ZIP 1443."
    }
  }
  $backup=Join-Path $target ('patch-backups\R1D5-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
  foreach ($rel in $originals.Keys) {
    $dest=Join-Path $target ($rel.Replace('/',[IO.Path]::DirectorySeparatorChar))
    $save=Join-Path $backup ($rel.Replace('/',[IO.Path]::DirectorySeparatorChar))
    New-Item -Path (Split-Path $save -Parent) -ItemType Directory -Force | Out-Null
    Copy-Item -LiteralPath $dest -Destination $save -Force
  }
  $copied=0
  foreach ($folder in @('backend','database','tools','docs','config')) {
    $from=Join-Path $patch $folder
    if (-not (Test-Path $from)) { continue }
    Get-ChildItem -LiteralPath $from -Recurse -File | ForEach-Object {
      $rel=$_.FullName.Substring($patch.Length).TrimStart('\','/')
      $dest=Join-Path $target $rel
      New-Item -Path (Split-Path $dest -Parent) -ItemType Directory -Force | Out-Null
      Copy-Item -LiteralPath $_.FullName -Destination $dest -Force
      $copied++
    }
  }
  Write-Host "R1D5_INSTALL_STATUS=PASS" -ForegroundColor Green
  Write-Host "R1D5_COPIED_FILES=$copied"
  Write-Host "R1D5_BACKUP=$backup"
  Write-Host 'Posting is OFF by default. No Oracle SQL was executed. Run node tools\posting\verify-posting-r1d5.mjs from your project root.'
  exit 0
} catch {
  Write-Host ("R1D5_INSTALL_ERROR=" + $_.Exception.Message) -ForegroundColor Red
  Write-Host 'R1D5_INSTALL_STATUS=FAIL'
  exit 1
}
