param([Parameter(Mandatory=$true)][string]$TargetRoot)
$ErrorActionPreference = 'Stop'
# Deprecated PowerShell implementation is replaced with the same Node launcher as .cmd.
# This wrapper is retained for compatibility. The recommended path is the CMD file.
Write-Host 'CLT_R6K_PS1_COMPAT_ENTRY; NODE_LAUNCHER=YES'
$script = Join-Path (Split-Path -Parent $PSScriptRoot) 'r6k\run-local-uat.cjs'
if (-not (Test-Path -LiteralPath $script)) { throw 'CLT_R6K_NODE_LAUNCHER_NOT_FOUND' }
& node $script $TargetRoot
if ($LASTEXITCODE -ne 0) { throw "CLT_R6K_NODE_LAUNCH_FAILED; CODE=$LASTEXITCODE" }
exit 0
