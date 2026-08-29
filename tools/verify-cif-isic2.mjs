import {readFileSync} from 'node:fs';
import {join, resolve, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const read = p => readFileSync(join(root, p), 'utf8');
const assert = (ok, msg) => { if (!ok) throw new Error(msg); };

const ddl = read('database/oracle/cif/isic2/01-create-isic2-tables.sql');
const seed = read('database/oracle/cif/isic2/02-import-isic-rev4-unsd.sql');
const ir = read('database/oracle/cif/isic2/03-register-ir-sci-release.sql');
const verifySql = read('database/oracle/cif/isic2/04-verify-isic2.sql');
const reset = read('database/oracle/cif/isic2/00-reset-isic2-redesign.sql');
const routes = read('frontend/src/app/app.routes.ts');
const menuTs = read('frontend/src/app/features/reference-menu/reference-menu.component.ts');
const menuHtml = read('frontend/src/app/features/reference-menu/reference-menu.component.html');
const activityTs = read('frontend/src/app/features/isic-reference/isic-activity-page.component.ts');
const releaseTs = read('frontend/src/app/features/isic-reference/isic-release-page.component.ts');
const activityHtml = read('frontend/src/app/features/isic-reference/isic-activity-page.component.html');
const releaseHtml = read('frontend/src/app/features/isic-reference/isic-release-page.component.html');
const backend = read('backend/src/main/java/com/behsazan/corebanking/cif/isic/oracle/IsicRepository.java');
const service = read('backend/src/main/java/com/behsazan/corebanking/cif/isic/application/IsicService.java');
const controller = read('backend/src/main/java/com/behsazan/corebanking/cif/isic/web/IsicController.java');

assert(/^\d+\.\d+\.\d+$/.test(read('VERSION').trim()), 'VERSION must use semantic x.y.z format');
assert(ddl.includes('CREATE TABLE CIF.REF_ISIC_RELEASE'), 'REF_ISIC_RELEASE DDL missing');
assert(ddl.includes('CREATE TABLE CIF.REF_ISIC_ACTIVITY2'), 'REF_ISIC_ACTIVITY2 DDL missing');
assert(ddl.includes('CREATE TABLE CIF.REF_ISIC_ACTIVITY_NOTE'), 'REF_ISIC_ACTIVITY_NOTE DDL missing');
assert(ddl.includes('CREATE OR REPLACE VIEW CIF.V_REF_ISIC_ACTIVITY_LOOKUP2'), 'ISIC lookup view missing');
assert(ddl.includes('NAME_FA                 VARCHAR2(500 CHAR) NOT NULL'), 'Activity NAME_FA must be mandatory');
assert(ddl.includes('NAME_EN                 VARCHAR2(500 CHAR) NOT NULL'), 'Activity NAME_EN must be mandatory');
assert(ddl.includes('PARENT_ACTIVITY_ID      NUMBER(19)'), 'Parent activity ID missing');
assert(ddl.includes('LEVEL_NO                NUMBER(2) NOT NULL'), 'LEVEL_NO missing');
assert(ddl.includes('FOREIGN KEY (ISIC_RELEASE_ID, PARENT_ACTIVITY_ID)'), 'Same-release composite parent FK missing');
assert(ddl.includes("LEVEL_CODE='SECTION' AND LEVEL_NO=1") && ddl.includes("LEVEL_CODE='CLASS' AND LEVEL_NO=4"), 'Level code/no consistency check missing');
assert(ddl.includes("LEVEL_CODE IN ('CLASS','NATIONAL_SUBCLASS') AND IS_SELECTABLE=1"), 'Leaf selectability constraint missing');
assert(ddl.includes("NOTE_TYPE_CODE IN ('EXPLANATORY','INCLUDES','ALSO_INCLUDES','EXCLUDES')"), 'Standard note-type model missing');
assert(ddl.includes("LANGUAGE_CODE") && ddl.includes('NOTE_TEXT             CLOB NOT NULL'), 'Language-aware CLOB note storage missing');

const activityStart = ddl.indexOf('CREATE TABLE CIF.REF_ISIC_ACTIVITY2');
const noteStart = ddl.indexOf('CREATE TABLE CIF.REF_ISIC_ACTIVITY_NOTE');
const activityBlock = ddl.slice(activityStart, noteStart);
for (const oldColumn of ['BASE_ISIC_CODE','PARENT_ISIC_CODE','SECTION_CODE','DESCRIPTION_FA','DESCRIPTION_EN','INCLUSIONS_FA','INCLUSIONS_EN','EXCLUSIONS_FA','EXCLUSIONS_EN']) {
  assert(!activityBlock.includes(oldColumn), `${oldColumn} must not be stored in REF_ISIC_ACTIVITY2`);
}

const mergeCount = (seed.match(/^MERGE INTO CIF\.REF_ISIC_ACTIVITY2 T$/gm) || []).length;
assert(mergeCount === 766, `Expected 766 activity merges, got ${mergeCount}`);
for (const [level, expected] of [['SECTION',21],['DIVISION',88],['GROUP',238],['CLASS',419]]) {
  const count = (seed.match(new RegExp(`T\\.LEVEL_CODE='${level}'`, 'g')) || []).length;
  assert(count === expected, `Expected ${expected} ${level} rows, got ${count}`);
}
assert((seed.match(/T\.TRANSLATION_STATUS_CODE='BANK_TRANSLATED'/g) || []).length === 766, 'Every UNSD Rev.4 row must be BANK_TRANSLATED');
assert((seed.match(/T\.NAME_FA='[^']+'/g) || []).length >= 766, 'Every UNSD Rev.4 row must have NAME_FA');
assert((seed.match(/T\.NAME_EN='[^']+'/g) || []).length >= 766, 'Every UNSD Rev.4 row must have NAME_EN');
assert(ir.includes("'IR-SCI' VARIANT_CODE") && ir.includes("T.DATASET_STATUS_CODE='DRAFT'") && ir.includes('T.IS_ACTIVE=0'), 'IR-SCI must remain inactive draft');
assert(verifySql.includes('V_TOTAL<>766') && verifySql.includes('V_FA<>766') && verifySql.includes('V_EN<>766') && verifySql.includes('V_SELECTABLE<>419'), 'Validation script must verify complete bilingual dataset');
assert(verifySql.includes("V_OLD_COLUMNS<>0") && verifySql.includes("V_NOTE_TABLE<>1"), 'Database verification must reject legacy Activity2 schema and require note table');
assert(reset.includes('DROP TABLE CIF.REF_ISIC_ACTIVITY_NOTE') && reset.includes('DROP TABLE CIF.REF_ISIC_ACTIVITY2') && reset.includes('DROP TABLE CIF.REF_ISIC_RELEASE'), 'Reset must target all redesigned ISIC objects');
assert(!reset.includes("DROP TABLE CIF.REF_ISIC_ACTIVITY'"), 'Legacy REF_ISIC_ACTIVITY must not be dropped');

assert(routes.includes('cif/reference-data/isic-releases') && routes.includes('cif/reference-data/isic-activities'), 'Dedicated ISIC routes missing');
assert(menuTs.includes('REF_ISIC_RELEASE') && menuTs.includes('REF_ISIC_ACTIVITY2'), 'Dedicated ISIC menu entries missing');
assert(menuHtml.includes('طبقه‌بندی فعالیت اقتصادی ISIC'), 'ISIC menu section missing');
assert(releaseTs.includes('Validators.required, Validators.maxLength(500)') && releaseHtml.includes('نام فارسی *'), 'Release Persian name must be required in UI');
assert(activityTs.includes('parentActivityId') && activityTs.includes('levelNo()') && activityTs.includes("translationStatusCode:'BANK_TRANSLATED'"), 'Activity form does not use clean hierarchy fields');
assert(activityTs.includes('alsoInclusionsFa') && activityTs.includes('alsoInclusionsEn'), 'Also-includes fields missing from activity editor');
assert(activityHtml.includes('CIF.REF_ISIC_ACTIVITY_NOTE') && activityHtml.includes('همچنین شامل - فارسی') && activityHtml.includes('همچنین شامل - انگلیسی'), 'Normalized ISIC note UI missing');
assert(!activityHtml.includes('baseIsicCode') && !activityHtml.includes('sectionCode'), 'Old denormalized activity fields still exist in editor');

assert(backend.includes('REF_ISIC_ACTIVITY2') && backend.includes('REF_ISIC_ACTIVITY_NOTE') && backend.includes('PARENT_ACTIVITY_ID'), 'Backend repository does not target normalized ISIC tables');
assert(backend.includes('ALSO_INCLUDES') && backend.includes('syncActivityNotes'), 'Backend note persistence missing');
assert(!backend.includes('DESCRIPTION_FA=:descriptionFa') && !backend.includes('INCLUSIONS_FA=:inclusionsFa'), 'Backend must not persist notes as Activity2 columns');
assert(service.includes('parentActivityId') && service.includes('سطح والد') && service.includes('alsoInclusionsFa'), 'Backend hierarchy/note validation contract missing');
assert(controller.includes('/api/v1/cif/isic') && controller.includes('parentActivityId'), 'ISIC API clean hierarchy contract missing');

console.log(`CIF ISIC verification OK: ${mergeCount} bilingual UNSD rows, release-aware hierarchy, normalized explanatory notes, dedicated forms.`);
