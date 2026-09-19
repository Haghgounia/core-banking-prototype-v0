import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const exists = rel => fs.existsSync(path.join(root, rel));

const migrationRel = 'database/oracle/dps2/migrations/0.9.2-phase4-phase9-account-schema-reconciliation.sql';
const migration = read(migrationRel);
const accountRepo = read('backend/src/main/java/com/behsazan/corebanking/deposit/account/oracle/DepositAccountRepository.java');
const opsRepo = read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/oracle/DepositAccountOperationsRepository.java');
const servicingRepo = read('backend/src/main/java/com/behsazan/corebanking/deposit/account/servicing/oracle/DepositAccountServicingRepository.java');
const config = read('backend/src/main/resources/application.yml');
const version = read('VERSION').trim();
const semverAtLeast=(actual,minimum)=>{const a=actual.split('.').map(Number),b=minimum.split('.').map(Number);for(let i=0;i<3;i++){if((a[i]||0)>(b[i]||0))return true;if((a[i]||0)<(b[i]||0))return false;}return true;};

const rootInstall = fs.readdirSync(root).filter(name => /^INSTALL-.*\.txt$/i.test(name));
const checks = [
  [semverAtLeast(version,'0.9.2'), `VERSION must be >= 0.9.2, got ${version}`],
  [exists(migrationRel), '0.9.2 reconciliation migration is missing'],
  [migration.includes('WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK'), 'migration must stop on SQL error'],
  [migration.includes("DPS2.DEPOSIT_OPENING_REQUEST is missing"), 'migration must guard the Opening prerequisite'],
  [migration.includes('CREATE TABLE DPS2.DEPOSIT_ACCOUNT ('), 'migration must reconcile DEPOSIT_ACCOUNT'],
  [migration.includes('CREATE TABLE DPS2.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT ('), 'migration must reconcile lifecycle history'],
  [migration.includes("CHECK (ACCOUNT_STATUS_CODE IN ('PENDING_ACTIVATION','ACTIVE','CLOSED'))"), 'account status contract mismatch'],
  [migration.includes("CHECK (EVENT_TYPE_CODE IN ('CREATE','ACTIVATE','CLOSE'))"), 'lifecycle event contract mismatch'],
  [migration.includes('REFERENCES DPS2.DEPOSIT_ACCOUNT (ACCOUNT_ID)'), 'lifecycle FK must be explicitly DPS2-qualified'],
  [migration.includes('TRG_DEP_ACCT_EVT_APPEND_ONLY'), 'append-only lifecycle trigger missing'],
  [!migration.includes('FEE.DEPOSIT_ACCOUNT'), 'reconciliation migration must not depend on FEE schema'],
  [accountRepo.includes('SEQ_DEPOSIT_ACCOUNT') && accountRepo.includes('SEQ_DEP_ACCOUNT_LIFECYCLE_EVT'), 'Phase 4 repository sequences do not match reconciled schema'],
  [opsRepo.includes('A.RECORD_VERSION') && opsRepo.includes('.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT'), 'Account Operations read model does not match reconciled schema'],
  [servicingRepo.includes("ACCOUNT_STATUS_CODE='CLOSED'") && servicingRepo.includes("'CLOSE', 'ACTIVE', 'CLOSED'"), 'Phase 9 closure transition does not match reconciled constraints'],
  [/deposit-account:\s*DPS2/.test(config), 'default deposit-account schema must be DPS2'],
  [!exists('README-FA.txt'), 'legacy README-FA.txt must not remain in root'],
  [!exists('config/application.yml_') && !exists('backend/src/main/resources/application.yml_'), 'stale application.yml_ backups must not remain in active source'],
  [rootInstall.length === 0, `INSTALL files must not remain in root: ${rootInstall.join(', ')}`],
  [exists('docs/patches/PATCH-0.3.2-BUILD-FIX1-README-FA.txt'), 'relocated 0.3.2 build-fix note is missing'],
  [exists('docs/DPS2-0.9.2-PHASE4-PHASE9-ACCOUNT-SCHEMA-RECONCILIATION-QA.md') && exists('docs/install/INSTALL-0.9.2-FA.txt') && exists('docs/patches/PATCH-0.9.2-README-FA.txt'), '0.9.2 QA/install/patch documentation is incomplete'],
];

const failed = checks.filter(([ok]) => !ok).map(([, message]) => message);
if (failed.length) {
  console.error('DPS2 0.9.2 Account Schema Reconciliation verification FAILED:');
  for (const message of failed) console.error(`- ${message}`);
  process.exit(1);
}
console.log(`DPS2 0.9.2 Account Schema Reconciliation verification OK: ${checks.length}/${checks.length} checks passed.`);
