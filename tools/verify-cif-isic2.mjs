import {readFileSync, readdirSync, statSync} from 'node:fs';
import {join} from 'node:path';

const root = new URL('..', import.meta.url).pathname;
const read = p => readFileSync(join(root, p), 'utf8');
const assert = (ok, msg) => { if (!ok) throw new Error(msg); };

const ddl = read('database/oracle/cif/isic2/01-create-isic2-tables.sql');
const seed = read('database/oracle/cif/isic2/02-import-isic-rev4-unsd.sql');
const ir = read('database/oracle/cif/isic2/03-register-ir-sci-release.sql');
const verify = read('database/oracle/cif/isic2/04-verify-isic2.sql');
const routes = read('frontend/src/app/app.routes.ts');
const menuTs = read('frontend/src/app/features/reference-menu/reference-menu.component.ts');
const menuHtml = read('frontend/src/app/features/reference-menu/reference-menu.component.html');
const activityTs = read('frontend/src/app/features/isic-reference/isic-activity-page.component.ts');
const releaseTs = read('frontend/src/app/features/isic-reference/isic-release-page.component.ts');
const activityHtml = read('frontend/src/app/features/isic-reference/isic-activity-page.component.html');
const backend = read('backend/src/main/java/com/behsazan/corebanking/cif/isic/oracle/IsicRepository.java');
const controller = read('backend/src/main/java/com/behsazan/corebanking/cif/isic/web/IsicController.java');
const legacyService = read('backend/src/main/java/com/behsazan/corebanking/cif/application/CifService.java');
const legacyModel = read('backend/src/main/resources/cif/party-reference/party-reference-model.json');

assert(read('VERSION').trim() === '0.3.61-prototype-fee-p1', 'VERSION mismatch');
assert(ddl.includes('CREATE TABLE CIF.REF_ISIC_RELEASE'), 'REF_ISIC_RELEASE DDL missing');
assert(ddl.includes('CREATE TABLE CIF.REF_ISIC_ACTIVITY2'), 'REF_ISIC_ACTIVITY2 DDL missing');
assert(ddl.includes('CREATE OR REPLACE VIEW CIF.V_REF_ISIC_ACTIVITY_LOOKUP2'), 'ISIC2 lookup view missing');
const executableSql = (ddl + '\n' + seed + '\n' + ir).split('\n').filter(line => !line.trim().startsWith('--')).join('\n');
assert(!/\b(CREATE|ALTER|DROP|TRUNCATE|DELETE\s+FROM|INSERT\s+INTO|MERGE\s+INTO|UPDATE)\s+CIF\.REF_ISIC_ACTIVITY\b(?!2)/i.test(executableSql), 'ISIC2 scripts must not mutate legacy REF_ISIC_ACTIVITY');

const mergeCount = (seed.match(/^MERGE INTO CIF\.REF_ISIC_ACTIVITY2 T$/gm) || []).length;
assert(mergeCount === 766, `Expected 766 activity merges, got ${mergeCount}`);
for (const [level, expected] of [['SECTION',21],['DIVISION',88],['GROUP',238],['CLASS',419]]) {
  const count = (seed.match(new RegExp(`T\\.LEVEL_CODE='${level}'`, 'g')) || []).length;
  assert(count === expected, `Expected ${expected} ${level} rows, got ${count}`);
}
assert((seed.match(/T\.TRANSLATION_STATUS='BANK_VERIFIED'/g) || []).length === 4, 'Expected four bank-verified Persian labels');
assert(ir.includes("'IR-SCI' VARIANT_CODE") && ir.includes("T.DATASET_STATUS_CODE='DRAFT'") && ir.includes('T.IS_ACTIVE=0'), 'IR-SCI must remain inactive draft');
assert(verify.includes('CIF.REF_ISIC_ACTIVITY2') && verify.includes('CIF.V_REF_ISIC_ACTIVITY_LOOKUP2'), 'Validation script must target ISIC2 objects');

assert(routes.includes("cif/reference-data/isic-releases") && routes.includes("cif/reference-data/isic-activities"), 'Dedicated ISIC routes missing');
assert(menuTs.includes('REF_ISIC_RELEASE') && menuTs.includes('REF_ISIC_ACTIVITY2'), 'Dedicated ISIC menu entries missing');
assert(menuHtml.includes('طبقه‌بندی فعالیت اقتصادی ISIC'), 'ISIC menu section missing');
assert(releaseTs.includes('IsicReleasePageComponent'), 'Release form component missing');
assert(activityTs.includes('IsicActivityPageComponent') && activityTs.includes('toggleTree'), 'Hierarchical activity form missing');
assert(activityHtml.includes('CIF.REF_ISIC_ACTIVITY2') && activityHtml.includes('CIF.V_REF_ISIC_ACTIVITY_LOOKUP2'), 'Activity form technical metadata missing');

assert(backend.includes('REF_ISIC_ACTIVITY2') && backend.includes('REF_ISIC_RELEASE'), 'Backend repository does not target ISIC2 tables');
assert(controller.includes('/api/v1/cif/isic') && controller.includes('/releases') && controller.includes('/activities'), 'ISIC API contract missing');
assert(legacyService.includes('"REF_ISIC_ACTIVITY"'), 'Legacy Party ISIC validation must remain on old table in this release');
assert(legacyModel.includes('REF_ISIC_ACTIVITY'), 'Legacy reference catalog entry was unexpectedly removed');

console.log(`CIF ISIC2 verification OK: ${mergeCount} official UNSD rows, dedicated Release/Activity2 forms, legacy table untouched.`);
