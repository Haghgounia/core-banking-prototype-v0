param([Parameter(Mandatory=$true)][string]$TargetRoot)
$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path -LiteralPath $TargetRoot).Path
$Jar = Join-Path $Root 'app\core-banking-prototype.jar'
$Version = (Get-Content -LiteralPath (Join-Path $Root 'VERSION') -Raw).Trim()
if ($Version -ne '0.11.0') { throw 'CLT_R6H_SOURCE_VERSION_NOT_0_11_0' }
if (Test-Path -LiteralPath (Join-Path $Root 'app\BUILD-DIRTY')) { throw 'CLT_R6H_BUILD_DIRTY_START_DENIED' }
if (-not (Test-Path -LiteralPath $Jar)) { throw 'CLT_R6H_RUNTIME_JAR_MISSING' }
if (-not (Test-Path -LiteralPath (Join-Path $Root 'app\BUILD-VERSION'))) { throw 'CLT_R6H_BUILD_VERSION_MISSING' }
if ((Get-Content -LiteralPath (Join-Path $Root 'app\BUILD-VERSION') -Raw).Trim() -ne $Version) {
    throw 'CLT_R6H_BUILD_VERSION_STALE'
}
# Bytecode proof: a successful build exit code alone does not guarantee the right CLT code is in the JAR.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [System.IO.Compression.ZipFile]::OpenRead($Jar)
try {
    $classPath = 'BOOT-INF/classes/com/behsazan/corebanking/collateral/localuat/'
    foreach ($className in @('CltLocalUatConfiguration.class', 'CltLocalUatBranchResolver.class',
                             'CltLocalUatAuthenticationFilter.class', 'CltLocalUatSecurityConfiguration.class')) {
        if (-not $z.GetEntry($classPath + $className)) {
            throw "CLT_R6H_JAR_CLASS_MISSING: $className"
        }
    }
    $entry = $z.GetEntry($classPath + 'CltLocalUatConfiguration.class')
    $memory = New-Object System.IO.MemoryStream
    $stream = $entry.Open()
    try { $stream.CopyTo($memory) } finally { $stream.Dispose() }
    $bytecode = [Text.Encoding]::GetEncoding(28591).GetString($memory.ToArray())
    $memory.Dispose()
    if (-not $bytecode.Contains('CLT_R6H_AUTO_BRANCH_20261104')) {
        throw 'CLT_R6H_JAR_STALE_NO_BUILD_MARKER'
    }
} finally { $z.Dispose() }
Write-Host 'CLT_R6H_JAR_ATTESTATION_PASS; CORRECT_CLT_CLASS_IN_JAR=YES'

# Preserve the existing Prototype JAR integrity gates (read-only checks; no IAM development).
foreach ($tool in @('tools\iam\s1a\verify-s1a-artifact.cjs',
                     'tools\iam\s1b\verify-s1b-artifact.cjs',
                     'tools\iam\s1b\verify-s1b-ro2-artifact.cjs',
                     'tools\iam\s1b\verify-s1b-ro3-artifact.cjs')) {
    $path = Join-Path $Root $tool
    if (-not (Test-Path -LiteralPath $path)) { throw "CLT_R6H_STANDARD_ARTIFACT_VERIFIER_MISSING: $tool" }
    & node $path $Jar
    if ($LASTEXITCODE -ne 0) { throw "CLT_R6H_STANDARD_ARTIFACT_VERIFIER_FAILED: $tool" }
}
if (Get-NetTCPConnection -LocalPort 8091 -State Listen -ErrorAction SilentlyContinue) {
    throw 'CLT_R6H_PORT_8091_ALREADY_IN_USE_STOP_PREVIOUS_INSTANCE'
}
$Config = Join-Path $Root 'config\application.yml'
if (-not (Test-Path -LiteralPath $Config)) { throw 'CLT_R6H_PROJECT_CONFIGURATION_MISSING' }
$OutDir = Join-Path $Root 'logs'
New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
$stdout = Join-Path $OutDir 'clt-r6h-stdout.log'
$stderr = Join-Path $OutDir 'clt-r6h-stderr.log'
# Explicit opt-in in this child process ONLY; do not write these settings into application.yml.
$env:CLT_LOCAL_UAT_READONLY_ENABLED = 'true'
$env:CLT_REFERENCE_READS_ENABLED = 'true'
$env:CLT_REGISTRY_READS_ENABLED = 'true'
$env:SERVER_ADDRESS = '127.0.0.1'
Remove-Item Env:\CLT_LOCAL_UAT_BRANCH_CODE -ErrorAction SilentlyContinue
foreach ($flag in @('CLT_REFERENCE_WRITES_ENABLED',
                     'CLT_INTRODUCTION_DRAFT_WRITES_ENABLED', 'CLT_INTRODUCTION_REVIEW_WORKFLOW_ENABLED',
                     'CLT_SPONSOR_REQUEST_WRITES_ENABLED', 'CLT_INSURANCE_REQUEST_WRITES_ENABLED',
                     'CLT_VALUATION_REQUEST_WRITES_ENABLED', 'CLT_ALLOCATION_INTAKE_WRITES_ENABLED',
                     'CLT_CAPACITY_LOCK_PROBE_ENABLED', 'CLT_UNALLOCATION_INTAKE_WRITES_ENABLED')) {
    Set-Item -Path ('Env:' + $flag) -Value 'false'
}
$ConfigUri = 'file:/' + ($Config -replace '\\','/')
$JavaArgs = '-DCLT_LOCAL_UAT_READONLY_ENABLED=true -jar "' + $Jar + '"' +
    ' --server.address=127.0.0.1 --server.port=8091' +
    ' --core-banking.collateral.reference.reads-enabled=true' +
    ' --core-banking.collateral.registry.reads-enabled=true' +
    ' --spring.config.additional-location=optional:' + $ConfigUri
$JavaPath = (Get-Command java -ErrorAction Stop).Source
$proc = Start-Process -FilePath $JavaPath -ArgumentList $JavaArgs -WorkingDirectory $Root `
                      -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
Write-Host ('CLT_R6H_BOOTING_PID=' + $proc.Id + '; BRANCH=AUTO_FROM_ORACLE; NO_JWT; NO_BRANCH_PROMPT')
$healthy = $false
for ($i=0; $i -lt 75; $i++) {
    $proc.Refresh()
    if ($proc.HasExited) { break }
    try {
        $health = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8091/actuator/health' -TimeoutSec 2
        if ($health.StatusCode -eq 200) { $healthy=$true; break }
    } catch { }
    Start-Sleep -Milliseconds 750
}
if (-not $healthy) {
    $proc.Refresh()
    if (-not $proc.HasExited) { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue }
    Write-Host 'CLT_R6H_STARTUP_FAILED; RECENT_STDERR:'
    if (Test-Path $stderr) { Get-Content -LiteralPath $stderr -Tail 10 }
    if (Test-Path $stdout) { Get-Content -LiteralPath $stdout -Tail 18 }
    throw 'CLT_R6H_STARTUP_FAILED_REVIEW_LOGS_NO_UAT_CLAIM'
}
$runtimeLog = if (Test-Path $stdout) {Get-Content -LiteralPath $stdout -Raw} else {''}
if (-not $runtimeLog.Contains('CLT_R6H_LOCAL_BOOTSTRAP_ACTIVE')) {
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    throw 'CLT_R6H_STARTUP_LOCAL_MODE_NOT_ATTESTED'
}
Write-Host 'CLT_R6H_STARTUP_PASS; HEALTH=200; LOCAL_MODE_ATTESTED=YES'
$checks = @(
    @{Name='registry-readiness'; Url='http://127.0.0.1:8091/api/v1/collaterals/readiness'},
    @{Name='registry-page'; Url='http://127.0.0.1:8091/api/v1/collaterals?page=0&size=1'},
    @{Name='report-overview'; Url='http://127.0.0.1:8091/api/v1/collaterals/reports/overview'},
    @{Name='reference-summary'; Url='http://127.0.0.1:8091/api/v1/collaterals/reports/reference-summary'}
)
$results = @()
foreach ($c in $checks) {
    $status = 0
    try {
        $res = Invoke-WebRequest -UseBasicParsing -Uri $c.Url -Method Get -TimeoutSec 10
        $status = [int]$res.StatusCode
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $status = [int]$_.Exception.Response.StatusCode
        }
    }
    Write-Host ('CLT_R6H_R6D_' + $c.Name + '_HTTP=' + $status)
    $results += [PSCustomObject]@{name=$c.Name;httpStatus=$status;passed=($status -eq 200)}
}
$receipt = [PSCustomObject]@{
    format='CLT-R6H-RUNTIME-1'; version='0.11.0';
    generatedAt=(Get-Date).ToUniversalTime().ToString('o');
    processId=$proc.Id; healthHttp=200; jarAttested=$true;
    branchSelection='SERVER_ORACLE_AUTO_ONE_BRANCH';
    identity='LOCAL_ONLY_NO_IAM'; financialWrites=$false;
    r6d=@($results); analystUat='NOT_RUN'; oracleUatApproved=$false
}
$receiptPath = Join-Path $Root 'tools\clt\r6h\runtime-receipt.json'
$receipt | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $receiptPath -Encoding UTF8
$passed = @($results | Where-Object { $_.passed }).Count
if ($passed -ne 4) {
    Write-Warning "CLT_R6H_RUNTIME_READS_PARTIAL=$passed/4; Oracle/API needs review. The backend remains running for inspection. UAT NOT APPROVED."
    exit 2
}
Write-Host ('CLT_R6H_R6D_RUNTIME_READS_PASS=4/4; ORACLE_READ_PATH=VERIFIED; UAT=NOT_APPROVED; PID=' + $proc.Id)
Write-Host 'Open http://127.0.0.1:8091/#/collateral/uat'
exit 0
