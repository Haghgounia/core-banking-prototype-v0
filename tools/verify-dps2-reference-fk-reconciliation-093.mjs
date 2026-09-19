import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const exists=rel=>fs.existsSync(path.join(root,rel));
const version=read('VERSION').trim();
const pom=read('backend/pom.xml');
const pkg=JSON.parse(read('frontend/package.json'));
const seedRel='database/oracle/dps2/reference-data/Deposit_Account_Opening_Reference_Data_Seed_2026-09-19.sql';
const migrationRel='database/oracle/dps2/migrations/0.9.3-reference-data-fk-reconciliation.sql';
const seed=read(seedRel);
const migration=read(migrationRel);
const phase5=read('database/oracle/dps2/migrations/0.5.0-phase5-opening-audit-change-management.sql');
const phase6=read('database/oracle/dps2/migrations/0.6.0-phase6-batch-opening.sql');
const auditService=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/audit/application/DepositOpeningAuditService.java');
const buildCmd=read('build-production.cmd');
const buildSh=read('build-production.sh');
const packageCmd=read('package-release.cmd');

const semverAtLeast=(actual,minimum)=>{const a=actual.split('.').map(Number),b=minimum.split('.').map(Number);for(let i=0;i<3;i++){if((a[i]||0)>(b[i]||0))return true;if((a[i]||0)<(b[i]||0))return false;}return true;};
const requiredPhase5=['USER','SYSTEM','CREATE','CHANGE_REQUESTED','CHANGE_APPROVED','CHANGE_REJECTED','CHANGE_APPLIED','ACCOUNT_LINKED','STATUS_CHANGE','SUBMITTED','APPROVED','COMPLETED','PRE_CHANGE','POST_CHANGE'];
const correctFkPairs=[
 ['DEPOSIT_OPENING_REQUEST','REQUEST_TYPE_CODE','REF_DEP_OPEN_REQUEST_TYPE','REQUEST_TYPE_CODE'],
 ['DEPOSIT_OPENING_REQUEST','REQUEST_STATUS_CODE','REF_DEP_OPEN_REQUEST_STATUS','REQUEST_STATUS_CODE'],
 ['DEPOSIT_OPENING_DECISION','DECISION_CODE','REF_DEP_OPEN_DECISION','DECISION_CODE'],
 ['DEPOSIT_OPENING_DECISION','DECISION_REASON_CODE','REF_DEP_OPEN_DECISION_REASON','DECISION_REASON_CODE'],
 ['DEPOSIT_OPENING_TERMS_ACCEPTANCE','ACCEPTANCE_SOURCE_CODE','REF_DEP_OPEN_ACCEPTANCE_SOURCE','ACCEPTANCE_SOURCE_CODE'],
 ['DEPOSIT_OPENING_TERMS_ACCEPTANCE','ACCEPTANCE_STATUS_CODE','REF_DEP_OPEN_ACCEPTANCE_STATUS','ACCEPTANCE_STATUS_CODE']
];

const checks=[
 [semverAtLeast(version,'0.9.3'),`VERSION must be >= 0.9.3, got ${version}`],
 [pom.includes(`<version>${version}-SNAPSHOT</version>`)&&pkg.version===version,'backend/frontend version sync is incomplete'],
 [exists(seedRel)&&exists(migrationRel),'0.9.3 seed or reconciliation migration is missing'],
 [seed.includes("u.owner = 'DPS2'")&&!/\buser_tables\b/i.test(seed),'master seed preflight must use ALL_TABLES with DPS2 owner'],
 [!/^MERGE INTO REF_DEP_OPEN_/m.test(seed),'master seed contains unqualified MERGE target(s)'],
 [!/(COUNT\(\*\) ROW_COUNT FROM )REF_DEP_OPEN_/m.test(seed),'master seed contains unqualified verification table(s)'],
 [requiredPhase5.every(code=>seed.includes(`'${code}'`)),'master seed does not restore the Phase 5 audit/snapshot catalog'],
 [phase5.includes("'USER' code")&&phase5.includes("'PRE_CHANGE'")&&auditService.includes('"CHANGE_REQUESTED"'),'Phase 5 source authority for reconciled values is missing'],
 [seed.includes("CHANGE_REASON_CODE=s.code")&&seed.includes("'OTHER' code")&&auditService.includes('"OTHER".equals(reasonCode)'),'OTHER change-reason model/application contract is not reconciled'],
 [seed.includes("'ROW_INVALID'")&&seed.includes("'PROCESSING_FAILED'")&&seed.includes("'ACTIVATION_FAILED'")&&phase6.includes("REF_DEP_OPEN_BATCH_ERROR_CODE"),'Phase 6 batch error taxonomy is not reconciled'],
 [seed.includes('-- SKIPPED: REF_DEP_OPEN_CHANNEL_ORG_MAP')&&!/^MERGE INTO DPS2\.REF_DEP_OPEN_CHANNEL_ORG_MAP/m.test(seed),'environment-specific channel/org mapping must remain unseeded'],
 [migration.includes('drop_wrong_fk')&&migration.includes("'REF_DEP_OPEN_REQUEST_STATUS','REQUEST_STATUS_CODE'")&&migration.includes("'REF_DEP_OPEN_DECISION_REASON','DECISION_REASON_CODE'")&&migration.includes("'REF_DEP_OPEN_ACCEPTANCE_STATUS','ACCEPTANCE_STATUS_CODE'"),'known wrong XMI FK mappings are not guarded'],
 [correctFkPairs.every(([s,sc,t,tc])=>migration.includes(`'${s}','${sc}','${t}','${tc}'`)),'one or more correct FK relationships are missing'],
 [migration.includes('orphan values=')&&migration.includes('NOT EXISTS'),'FK migration must reject orphan code values before DDL'],
 [migration.includes('WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK')&&migration.includes("owner='DPS2'"),'migration must be DPS2-qualified and fail-fast'],
 [buildCmd.includes('verify-dps2-reference-fk-reconciliation-093.mjs')&&buildSh.includes('verify-dps2-reference-fk-reconciliation-093.mjs'),'production builds do not invoke 0.9.3 verifier'],
 [packageCmd.includes('verify-dps2-reference-fk-reconciliation-093.mjs'),'release packaging does not invoke 0.9.3 verifier'],
 [!exists('PATCH-LAYOUT-MIGRATION-0.9.2.txt')&&exists('docs/patches/PATCH-LAYOUT-MIGRATION-0.9.2.txt'),'stale patch-layout note remains in root or was not archived'],
 [exists('docs/DPS2-0.9.3-REFERENCE-DATA-FK-RECONCILIATION-QA.md')&&exists('release/QA-REPORT-0.9.3.md')&&exists('release/RELEASE-NOTES-0.9.3.md'),'0.9.3 release documentation is incomplete']
];
const failed=checks.filter(([ok])=>!ok).map(([,msg])=>msg);
if(failed.length){console.error('DPS2 0.9.3 Reference Data / FK Reconciliation verification FAILED:');for(const msg of failed)console.error(`- ${msg}`);process.exit(1);}
console.log(`DPS2 0.9.3 Reference Data / FK Reconciliation verification OK: ${checks.length}/${checks.length} checks passed.`);
