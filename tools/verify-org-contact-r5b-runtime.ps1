$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:8091/api/v1/organization/unit-contacts'
try {
    $state = Invoke-RestMethod -Uri ($base + '/readiness') -Method Get -TimeoutSec 20
    $migration = Invoke-RestMethod -Uri ($base + '/migration/readiness') -Method Get -TimeoutSec 20
    if ($state.storageMode -ne 'CANONICAL') { throw 'Contact storage mode is not CANONICAL.' }
    if ($state.ready -ne $true) { throw 'Canonical V2 schema readiness is false.' }
    if ($state.writesEnabled -ne $false) { throw 'FAIL CLOSED: canonical contact writes are enabled.' }
    if ($migration.sourceCoverageComplete -ne $true) { throw 'Legacy link coverage is incomplete.' }
    if ([long]$migration.missingLegacyLinks -ne 0 -or [long]$migration.orphanV2LegacyLinks -ne 0 -or [long]$migration.servicePointContactAssociations -ne 0) {
        throw 'Migration coverage or POS isolation safety check failed.'
    }
    if ([long]$migration.sourceBusinessDrift -ne 0) {
        throw 'Legacy source and V2 have diverged before read-only cutover.'
    }
    if ([long]$migration.legacyUnitAssociations -ne [long]$migration.canonicalV2Rows) {
        throw 'Canonical V2 row count does not match legacy association count.'
    }
    Write-Output ('ORG_R5B_CANONICAL_READONLY_RUNTIME_PASS | MODE=CANONICAL | WRITES=FALSE | LEGACY=' + $migration.legacyUnitAssociations + ' | V2=' + $migration.canonicalV2Rows + ' | MISSING=0 | DRIFT=0')
    exit 0
} catch {
    Write-Error ('ORG_R5B_CANONICAL_READONLY_RUNTIME_FAIL | ' + $_.Exception.Message)
    exit 1
}
