#Requires -Version 5.1
<#
ES2-PR1: Read-only Kafka broker/topic preflight for the Core Banking Prototype.
No Kafka topic creation; no Kafka messages; no Oracle access; no application mutations.
Environment: Windows 10, Docker CLI, apache/kafka container.
#>
[CmdletBinding()]
param(
    [ValidatePattern('^[A-Za-z0-9._-]{1,160}$')]
    [string]$Topic = 'corebanking.pst.events.v1',
    [ValidatePattern('^[A-Za-z0-9._-]{1,160}$')]
    [string]$ContainerName = 'kafka',
    [ValidatePattern('^[A-Za-z0-9._:-]{3,255}$')]
    [string]$ContainerBootstrap = 'localhost:9092',
    [string]$HostAddress = '127.0.0.1',
    [ValidateRange(1,65535)]
    [int]$HostPort = 9092
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$KafkaTopics = '/opt/kafka/bin/kafka-topics.sh'
$Step = 'initialization'
$FailCode = 2

function Invoke-DockerChecked {
    param([string[]]$Arguments)
    $result = @(& docker @Arguments)
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed with exit code $LASTEXITCODE. Step: $Step"
    }
    return $result
}

try {
    Write-Output 'ES2_PREFLIGHT_VERSION=ES2-PR1'
    Write-Output "EFFECTIVE_TOPIC=$Topic"
    Write-Output "BROKER_CONTAINER=$ContainerName"
    Write-Output 'MODE=READ_ONLY_NO_ORACLE_NO_MESSAGES'

    $Step = 'Docker availability'
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'docker CLI is not available in PATH.'
    }
    $state = @(Invoke-DockerChecked -Arguments @('inspect','--format','{{.State.Running}}',$ContainerName))
    if ($state.Count -ne 1 -or $state[0].Trim() -ne 'true') {
        throw "Kafka container '$ContainerName' is not running."
    }
    Write-Output '[PASS] Docker Kafka container is running'

    $Step = 'Windows host TCP reachability'
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $connect = $client.BeginConnect($HostAddress, $HostPort, $null, $null)
        if (-not $connect.AsyncWaitHandle.WaitOne(4000)) {
            throw "TCP connect timed out for ${HostAddress}:$HostPort"
        }
        $client.EndConnect($connect)
    } finally {
        $client.Close()
    }
    Write-Output "[PASS] Host TCP ${HostAddress}:$HostPort is reachable"

    $Step = 'Kafka broker metadata / topic listing'
    $topics = @(Invoke-DockerChecked -Arguments @(
        'exec',$ContainerName,$KafkaTopics,'--bootstrap-server',$ContainerBootstrap,'--list'
    ))
    Write-Output "[PASS] Broker responded to metadata request; topic count=$($topics.Count)"

    $FailCode = 3
    if ($topics -notcontains $Topic) {
        throw "Topic '$Topic' does not exist on the queried cluster. No topic was created."
    }
    Write-Output "[PASS] Exact Kafka topic '$Topic' exists"

    $Step = 'Topic partitions, leaders and ISR'
    $FailCode = 4
    $details = @(Invoke-DockerChecked -Arguments @(
        'exec',$ContainerName,$KafkaTopics,'--bootstrap-server',$ContainerBootstrap,
        '--describe','--topic',$Topic
    ))
    foreach ($line in $details) { Write-Output "DETAIL | $line" }

    $partitionLines = @($details | Where-Object { $_ -match 'Partition:\s*\d+' })
    if ($partitionLines.Count -lt 1) { throw 'No partition metadata found for the topic.' }
    $expectedPartitions = $null
    foreach ($line in $details) {
        if ($line -match 'PartitionCount:\s*(\d+)') {
            $expectedPartitions = [int]$Matches[1]
            break
        }
    }
    $ids = @{}
    foreach ($line in $partitionLines) {
        if ($line -notmatch 'Partition:\s*(\d+)') { continue }
        $part = [int]$Matches[1]
        if ($ids.ContainsKey($part)) { throw "Duplicate partition metadata for partition $part" }
        $ids[$part] = $true
        if ($line -notmatch 'Leader:\s*(-?\d+)') { throw "No leader metadata for partition $part" }
        $leader = [int]$Matches[1]
        if ($leader -lt 0) { throw "Unavailable leader for partition $part" }
        if ($line -notmatch 'Replicas:\s*([0-9,]+)') { throw "No replica list for partition $part" }
        $replicas = @($Matches[1].Split(',') | Sort-Object)
        if ($line -notmatch 'Isr:\s*([0-9,]+)') { throw "No ISR list for partition $part" }
        $isr = @($Matches[1].Split(',') | Sort-Object)
        if (($replicas -join ',') -ne ($isr -join ',')) {
            throw "ISR mismatch for partition $part (replicas=$($replicas -join ','); isr=$($isr -join ','))"
        }
    }
    if ($null -ne $expectedPartitions -and $ids.Count -ne $expectedPartitions) {
        throw "Partition count mismatch. Expected=$expectedPartitions actual=$($ids.Count)"
    }
    Write-Output "[PASS] Partition leaders and ISR are healthy; partitions=$($ids.Count)"
    Write-Output 'ES2_PREFLIGHT_STATUS=PASS'
    Write-Output 'NOTE=This validates Kafka metadata only; backend-to-Kafka delivery and Oracle receipt remain untested.'
    exit 0
} catch {
    Write-Output "[FAIL] $Step - $($_.Exception.Message)"
    if ($FailCode -eq 3) {
        Write-Output 'ACTION=Confirm effective backend topic setting; then create the topic only with explicit environment approval.'
        Write-Output 'SUGGESTION=docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic corebanking.pst.events.v1 --partitions 3 --replication-factor 1'
    }
    Write-Output "ES2_PREFLIGHT_STATUS=FAIL_$FailCode"
    exit $FailCode
}
