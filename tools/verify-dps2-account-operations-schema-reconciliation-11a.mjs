import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const exists = rel => fs.existsSync(path.join(root, rel));
const version = read('VERSION').trim();

const migrationRel = 'database/oracle/dps2/migrations/0.11.0-phase11a-account-operations-schema-reconciliation.sql';
const verifierRel = 'database/oracle/dps2/verification/0.11.0-phase11a-account-operations-schema-verifier.sql';
const qaRel = 'docs/DPS2-0.11.0-PHASE11A-ACCOUNT-OPERATIONS-SCHEMA-RECONCILIATION-QA.md';
const installRel = 'docs/install/INSTALL-0.11.0-FA.txt';

const migration = exists(migrationRel) ? read(migrationRel) : '';
const dbVerifier = exists(verifierRel) ? read(verifierRel) : '';
const buildCmd = read('build-production.cmd');
const buildSh = read('build-production.sh');
const packageRelease = read('package-release.cmd');
const pom = read('backend/pom.xml');
const pkg = JSON.parse(read('frontend/package.json'));
const lock = JSON.parse(read('frontend/package-lock.json'));

const checks = [];
const add = (name, ok, detail='') => checks.push({name, ok, detail});

add('VERSION is 0.11.0', version === '0.11.0', version);
add('frontend version matches', pkg.version === version, pkg.version);
add('frontend lock root version matches', lock.version === version && lock.packages?.['']?.version === version);
add('backend Maven version matches', pom.includes(`<version>${version}-SNAPSHOT</version>`));
add('Phase 11A migration exists', exists(migrationRel));
add('Phase 11A DB verifier exists', exists(verifierRel));
add('Phase 11A QA doc exists', exists(qaRel));
add('0.11.0 install guide exists', exists(installRel));
add('migration is fail-fast', migration.includes('WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK'));
add('migration is explicitly no-business-DML', migration.includes('no INSERT/UPDATE/DELETE/MERGE'));
add('legacy account-status constraint is dropped', migration.includes("drop_constraint_if_exists('DEPOSIT_ACCOUNT', 'CK_DEP_ACCOUNT_STATUS')"));
add('canonical account-status contract includes SUSPENDED', migration.includes("'PENDING_ACTIVATION','ACTIVE','SUSPENDED','DORMANT','CLOSED'"));
add('legacy lifecycle event constraint is dropped', migration.includes("drop_constraint_if_exists('DEPOSIT_ACCOUNT_LIFECYCLE_EVENT', 'CK_DEP_ACCT_EVT_TYPE')"));
add('lifecycle event contract includes SUSPEND', migration.includes("'CREATE','ACTIVATE','SUSPEND','REACTIVATE','MARK_DORMANT','CLOSE','REOPEN'"));
add('activation-run default reconciled', migration.includes("MODIFY (RUN_STATUS_CODE DEFAULT 'STARTED')"));
add('external-registry default reconciled', migration.includes("MODIFY (REGISTRATION_STATUS_CODE DEFAULT 'NOT_SENT')"));
add('migration emits PASS marker', migration.includes('PHASE11A_SCHEMA_RECONCILIATION_PASS'));
add('DB verifier checks legacy account constraint removal', dbVerifier.includes('legacy CK_DEP_ACCOUNT_STATUS removed'));
add('DB verifier checks lifecycle event expansion', dbVerifier.includes('lifecycle supports MARK_DORMANT') && dbVerifier.includes('lifecycle supports REOPEN'));
add('DB verifier checks defaults', dbVerifier.includes('activation-run default is STARTED') && dbVerifier.includes('external-registry default is NOT_SENT'));
add('DB verifier emits baseline marker', dbVerifier.includes('PHASE11A_DB_BASELINE_PASS'));
add('Windows build runs Phase 11A verifier', buildCmd.includes('verify-dps2-account-operations-schema-reconciliation-11a.mjs'));
add('Unix build runs Phase 11A verifier', buildSh.includes('verify-dps2-account-operations-schema-reconciliation-11a.mjs'));
add('source packaging runs Phase 11A verifier', packageRelease.includes('verify-dps2-account-operations-schema-reconciliation-11a.mjs'));
add('0.10 verifier README root artifact is upgrade-managed', read('tools/migrate-root-layout.mjs').includes('PATCH-0.10.0-FINAL-VERIFIER-DOCKER-FALLBACK-README.txt'));
add('Phase 11A Windows apply helper exists', exists('tools/apply-dps2-phase11a.cmd'));
add('Phase 11A apply helper supports Docker fallback', read('tools/apply-dps2-phase11a.cmd').includes('Trying Oracle Docker container fallback'));
add('Phase 11A apply helper emits implementation marker', read('tools/apply-dps2-phase11a.cmd').includes('PHASE11A_IMPLEMENTATION_PASS'));

// Safety: statements that mutate business rows are not allowed in Phase 11A.
const normalized = migration
  .replace(/^\s*--.*$/gm, '')
  .replace(/DBMS_OUTPUT\.PUT_LINE\([^;]*\);/gi, '')
  .toUpperCase();
add('migration contains no INSERT statement', !/\bINSERT\s+INTO\b/.test(normalized));
add('migration contains no UPDATE statement', !/\bUPDATE\s+[A-Z0-9_$#.]+\s+SET\b/.test(normalized));
add('migration contains no DELETE statement', !/\bDELETE\s+FROM\b/.test(normalized));
add('migration contains no MERGE statement', !/\bMERGE\s+INTO\b/.test(normalized));

let pass = 0;
for (const c of checks) {
  console.log(`${c.ok ? 'PASS' : 'FAIL'} | ${c.name}${c.detail ? ` | ${c.detail}` : ''}`);
  if (c.ok) pass++;
}
const fail = checks.length - pass;
console.log('------------------------------------------------------------');
console.log(`PHASE11A_STATIC_VERIFIER_PASS=${pass}`);
console.log(`PHASE11A_STATIC_VERIFIER_FAIL=${fail}`);
if (fail) process.exit(1);
console.log('PHASE11A_STATIC_BASELINE_PASS');
