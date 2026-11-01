// R5-A source contract verifier. No DB/network access; fail closed.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const check = (ok, msg) => { if (!ok) throw new Error(msg); };
let passed = 0;
const test = (ok, msg) => { check(ok, msg); passed++; };
try {
  const canonical = read('backend/src/main/java/com/behsazan/corebanking/organization/contact/CanonicalOrganizationContactService.java');
  const legacy = read('backend/src/main/java/com/behsazan/corebanking/organization/contact/OrganizationContactService.java');
  const migrationApi = read('backend/src/main/java/com/behsazan/corebanking/organization/contact/OrganizationContactMigrationReadinessController.java');
  const schema = read('database/oracle/org/contact-cutover-r5/01-ORG-R5-V2-UNIT-CONTACT-BACKFILL-OPT-IN.sql');
  const verify = read('database/oracle/org/contact-cutover-r5/02-ORG-R5-V2-CONTACT-COVERAGE-VERIFY-READONLY.sql');
  const ui = read('frontend/src/app/features/organization/organization-unit-contacts.component.ts');
  // R5 historic V2 migration artifacts remain immutable evidence, but no Runtime API may query them.
  test(legacy.includes('havingValue = "legacy", matchIfMissing = true') &&
       legacy.includes('INSERT INTO ORG.ORGANIZATION_UNIT_CONTACT_POINTS') &&
       !/\b(?:FROM|JOIN|INTO|UPDATE) ORG\.CONTACT_POINTS(?:\s|$)/m.test(legacy),
       'OUCP-only default mode not preserved');
  test(canonical.includes('havingValue = "canonical"') && canonical.includes('implements OrganizationContactOperations'),
       'Canonical configuration must alias OUCP');
  test(canonical.includes('new OrganizationContactService(jdbc, false)') && !canonical.includes('CONTACT_POINTS_V2'),
       'Legacy canonical mode still depends on V2');
  test(legacy.includes('ROW_VERSION=ROW_VERSION+1') && legacy.includes('AND ROW_VERSION=:expectedVersion') &&
       legacy.includes('lockUnit(input.unitId())'), 'OUCP concurrency contract removed');
  test(migrationApi.includes('@GetMapping("/readiness")') &&
       migrationApi.includes('HttpStatus.GONE') &&
       migrationApi.includes('ORG_V2_MIGRATION_ENDPOINT_RETIRED') &&
       !migrationApi.includes('CONTACT_POINTS_V2'), 'Historical migration route must return 410 without V2 SQL');
  test(schema.includes('DEFINE APPLY_CHANGES = NO') && schema.includes('DEFINE CONFIRM_BACKUP = NO') && schema.includes('DEFINE CONFIRM_SOURCE_QUIESCED = NO'), 'Stage flags must be NO');
  test(schema.includes('MERGE INTO ORG.CONTACT_POINTS_V2') && schema.includes('ORG.SEQ_CONTACT_POINTS_V2.NEXTVAL') && schema.includes('LEGACY_ASSOCIATION_ID') && schema.includes('LEGACY_MASTER_CREATED_AT'), 'Safe V2 merge lineage missing');
  test(!/\b(?:DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM|ALTER\s+TABLE|CREATE\s+TABLE|COMMIT\s*;)/i.test(schema) && schema.includes('NO AUTO_COMMIT'), 'Destructive SQL or automatic commit in migration');
  test(verify.includes('MISSING_LINKS') && verify.includes('SOURCE_BUSINESS_DRIFT') && verify.includes('NO DDL | NO DML'), 'Read-only verification missing');
  test(ui.includes('!this.writesEnabled()') && ui.includes('this.writesEnabled.set(state.writesEnabled !== false)'), 'UI read-only behavior changed');
  for (const name of ['backend/src/main/resources/application.yml','config/application.yml']) {
    const conf = read(name);
    test(conf.includes('storage-mode: ${ORG_CONTACT_STORAGE_MODE:legacy}') && conf.includes('unit-writes-enabled: ${ORG_CONTACT_UNIT_WRITES_ENABLED:false}'), 'Unsafe defaults in '+name);
  }
  console.log(`ORG_CONTACT_R5_STATIC_PASS=${passed}`);
} catch (e) { console.error('ORG_CONTACT_R5_STATIC_FAIL:', e.message); process.exitCode=1; }
