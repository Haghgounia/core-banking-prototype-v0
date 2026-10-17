#Requires -Version 5.1
<#
Administrative Kafka metadata write. NEVER invoked by the preflight.
Intended only for an approved, one-broker local/POC environment.
#>
[CmdletBinding()]
param(
    [switch]$ApproveCreate,
    [ValidatePattern('^[A-Za-z0-9._-]{1,160}$')]
    [string]$Topic = 'corebanking.pst.events.v1',
    [ValidatePattern('^[A-Za-z0-9._-]{1,160}$')]
    [string]$ContainerName = 'kafka',
    [ValidatePattern('^[A-Za-z0-9._:-]{3,255}$')]
    [string]$ContainerBootstrap = 'localhost:9092',
    [ValidateRange(1,100)]
    [int]$Partitions = 3
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$KafkaTopics = '/opt/kafka/bin/kafka-topics.sh'
if (-not $ApproveCreate) {
    Write-Output '[BLOCKED] Explicit -ApproveCreate is required. No changes made.'
    exit 6
}
try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'docker CLI missing' }
    $topics = @(& docker exec $ContainerName $KafkaTopics --bootstrap-server $ContainerBootstrap --list)
    if ($LASTEXITCODE -ne 0) { throw 'Unable to check existing topics; no create attempted.' }
    if ($topics -contains $Topic) {
        Write-Output "[NOOP] Topic '$Topic' already exists; no changes made. Inspect with read-only preflight."
        exit 0
    }
    Write-Output "[APPROVED ACTION] Creating Kafka topic '$Topic' with $Partitions partitions and replication factor 1."
    & docker exec $ContainerName $KafkaTopics --bootstrap-server $ContainerBootstrap `
        --create --if-not-exists --topic $Topic --partitions $Partitions --replication-factor 1
    if ($LASTEXITCODE -ne 0) { throw 'Kafka topic creation failed.' }
    Write-Output '[PASS] Kafka topic creation command succeeded. Run the read-only preflight now.'
    exit 0
} catch {
    Write-Output "[FAIL] $($_.Exception.Message)"
    exit 5
}
