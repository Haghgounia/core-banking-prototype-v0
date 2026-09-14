import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=relative=>fs.readFileSync(path.join(root,relative),'utf8');
const exists=relative=>fs.existsSync(path.join(root,relative));

const version=read('VERSION').trim();
const versionAtLeast=(actual,minimum)=>{const a=actual.split('.').map(Number),m=minimum.split('.').map(Number);for(let i=0;i<3;i++){if(a[i]>m[i])return true;if(a[i]<m[i])return false}return true};
const pom=read('backend/pom.xml');
const packageJson=JSON.parse(read('frontend/package.json'));
const models=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/audit/domain/DepositOpeningAuditModels.java');
const service=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/audit/application/DepositOpeningAuditService.java');
const repository=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/audit/oracle/DepositOpeningAuditRepository.java');
const controller=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/web/DepositOpeningController.java');
const aggregateService=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/application/DepositOpeningAggregateService.java');
const accountService=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/application/DepositAccountLifecycleService.java');
const migration=read('database/oracle/dps2/migrations/0.5.0-phase5-opening-audit-change-management.sql');
const frontendService=read('frontend/src/app/features/four-deposits/deposit-opening.service.ts');
const componentTs=read('frontend/src/app/features/four-deposits/deposit-opening-change-management.component.ts');
const componentHtml=read('frontend/src/app/features/four-deposits/deposit-opening-change-management.component.html');
const routes=read('frontend/src/app/app.routes.ts');
const shell=read('frontend/src/app/layout/app-shell.component.html');

const controlledTables=[
  'DEPOSIT_OPENING_CHANGE_SET','DEPOSIT_OPENING_AUDIT_EVENT','DEPOSIT_OPENING_AUDIT_FIELD_CHANGE',
  'DEPOSIT_OPENING_SNAPSHOT','DEPOSIT_OPENING_STATUS_HISTORY'
];
const aggregateTargets=[
  'DEPOSIT_OPENING_REQUEST','DEPOSIT_OPENING_PARTY','DEPOSIT_OPENING_SIGNATORY',
  'DEPOSIT_OPENING_SIGNATORY_AUTHORITY','DEPOSIT_OPENING_TERM','DEPOSIT_OPENING_MATURITY_INSTRUCTION',
  'DEPOSIT_OPENING_FUNDING','DEPOSIT_OPENING_DOCUMENT','DEPOSIT_OPENING_DECISION'
];
const endpoints=[
  '@PostMapping("/requests/{id}/change-sets")',
  '@PostMapping("/requests/{id}/change-sets/{changeSetId}/approve")',
  '@PostMapping("/requests/{id}/change-sets/{changeSetId}/reject")',
  '@PostMapping("/requests/{id}/change-sets/{changeSetId}/apply")',
  '@GetMapping("/requests/{id}/audit-trail")',
  '@GetMapping("/requests/{id}/snapshots/{snapshotId}")'
];

const checks=[
  [versionAtLeast(version,'0.5.0'),`VERSION must preserve Phase 5 or later source, got ${version}`],
  [pom.includes(`<version>${version}-SNAPSHOT</version>`),`backend Maven version is not ${version}-SNAPSHOT`],
  [packageJson.version===version,`frontend version must match ${version}, got ${packageJson.version}`],
  [models.includes('record FieldMutationRequest(')&&models.includes('String entityName')&&models.includes('Long entityId'),'aggregate-aware mutation contract missing'],
  [models.includes('List<Long> auditEventIds'),'per-entity audit event response contract missing'],
  [endpoints.every(item=>controller.includes(item)),'Phase 5 API endpoints are incomplete'],
  [repository.includes('CHANGEABLE_ENTITIES')&&aggregateTargets.every(item=>repository.includes(`"${item}"`)),'Opening aggregate entity whitelist is incomplete'],
  [repository.includes('ALL_TAB_COLUMNS')&&repository.includes('mutableFieldMetadata'),'Oracle metadata field validation is missing'],
  [repository.includes('SYSTEM_COLUMNS')&&repository.includes('OPENING_REQUEST_ID')&&repository.includes('OPENING_SIGNATORY_ID')&&repository.includes('OPENING_TERM_ID'),'protected key/system column guard is missing'],
  [repository.includes('OwnershipMode.SIGNATORY')&&repository.includes('OwnershipMode.TERM')&&repository.includes('EXISTS (SELECT 1'),'indirect child ownership validation is missing'],
  [service.includes('repository.lockOpening(openingRequestId)')&&repository.includes(' FOR UPDATE'),'Opening/root locking guard is missing'],
  [service.includes('repository.lockEntityFields')&&service.includes('repository.updateEntityFields'),'target row lock/update workflow is missing'],
  [service.includes('repository.bumpOpeningVersion')&&service.includes('long newVersion = opening.recordVersion() + 1'),'aggregate version advancement contract is missing'],
  [service.includes('"PRE_CHANGE"')&&service.includes('"POST_CHANGE"')&&repository.includes('canonicalOpeningSnapshot'),'before/after canonical snapshot contract missing'],
  [service.includes('hash(oldText)')&&service.includes('hash(newText)')&&(migration.includes("'SHA256'")||repository.includes("'SHA256'")),'SHA-256 field/snapshot evidence missing'],
  [service.includes('core-banking.deposit-opening.audit.sensitive-fields')&&service.includes('[MASKED]')&&service.includes('hash(oldText)'),'configurable sensitive-value masking is missing'],
  [service.includes('Opening تکمیل‌شده')&&service.includes('Deposit Account Operations'),'COMPLETED bounded-context mutation guard missing'],
  [aggregateService.includes('recordOpeningCreated')&&accountService.includes('recordAccountLinked')&&accountService.includes('recordOpeningCompleted'),'Opening/account lifecycle audit integration is incomplete'],
  [controlledTables.every(table=>repository.includes(table)),'controlled audit/change repository coverage is incomplete'],
  [migration.includes('TRG_DEP_OPEN_AUD_EVT_APPEND_ONLY')&&migration.includes('TRG_DEP_OPEN_AUD_FLD_APPEND_ONLY')&&migration.includes('TRG_DEP_OPEN_SNAPSHOT_APPEND_ONLY')&&migration.includes('TRG_DEP_OPEN_STATUS_APPEND_ONLY'),'append-only database triggers are incomplete'],
  [!migration.includes('COMMENT ON TRIGGER'),'migration contains unsupported COMMENT ON TRIGGER syntax'],
  [frontendService.includes('DepositOpeningFieldMutation')&&frontendService.includes('auditEventIds:number[]'),'Angular aggregate change contract is stale'],
  [componentTs.includes('mutableEntities')&&componentTs.includes('DEPOSIT_OPENING_SIGNATORY_AUTHORITY')&&componentHtml.includes('موجودیت Opening Aggregate'),'dedicated aggregate-aware Angular UI is incomplete'],
  [routes.includes("path: 'four-deposits/change-management'")&&shell.includes('/four-deposits/change-management'),'Phase 5 route/navigation is missing'],
  [exists('docs/DPS2-0.5.0-FOUR-DEPOSITS-PHASE5-AUDIT-CHANGE-MANAGEMENT-QA.md')&&exists('docs/install/INSTALL-0.5.0-FA.txt')&&exists('docs/patches/PATCH-0.5.0-README-FA.txt'),'0.5.0 QA/install/patch documentation is incomplete']
];

const failed=checks.filter(([ok])=>!ok).map(([,message])=>message);
if(failed.length){
  console.error('DPS2 Deposit Opening Phase 5 verification FAILED:');
  for(const message of failed)console.error(`- ${message}`);
  process.exit(1);
}
console.log('DPS2 Deposit Opening Phase 5 verification OK: controlled Change Sets, aggregate ownership guards, append-only audit/status/snapshots, per-entity field evidence and dedicated UI/API contract.');
