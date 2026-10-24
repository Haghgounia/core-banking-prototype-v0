import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const content = file => fs.readFileSync(path.join(root, file), 'utf8');
const check = (condition, message) => { if (!condition) throw new Error(message); };
try {
  const sql = content('database/oracle/org/contacts-r1/01-ORG-CONTACT-MASTER-OPT-IN.sql');
  const api = content('backend/src/main/java/com/behsazan/corebanking/organization/contact/OrganizationContactController.java');
  const service = content('backend/src/main/java/com/behsazan/corebanking/organization/contact/OrganizationContactService.java');
  const ui = content('frontend/src/app/features/organization/organization-unit-contacts.component.ts');
  const legacy = content('backend/src/main/java/com/behsazan/corebanking/referencedata/management/application/ReferenceService.java');
  const routes = content('frontend/src/app/app.routes.ts');
  const menu = content('frontend/src/app/features/organization/organization-menu.component.ts');
  const title = content('frontend/src/app/features/organization/organization-unit-locations-canonical.component.ts');
  const cmd = content('build-production.cmd');
  const sh = content('build-production.sh');
  check(sql.includes('DEFINE APPLY_CHANGES = NO'), 'Oracle install must default to preview');
  check(sql.includes("IF v_apply = 'NO' THEN") && sql.includes('RETURN;'), 'Preview must return before first DDL');
  check(sql.includes('EXECUTE IMMEDIATE q\'~\n    SELECT COUNT(*) FROM ('), 'First install must use dynamic SQL for not-yet-existing contact master');
  check(!sql.includes('SELECT COUNT(*) INTO v_orphans FROM ('), 'Static PL/SQL query causes ORA-00942 on the very first run');
  for(const code of ['CREATE TABLE ORG.CONTACT_POINTS','FK_OUCP_CONTACT_MASTER','FK_SPCP_CONTACT_MASTER','ROW_VERSION','NO LEGACY/CIF DATA WAS CHANGED'])
    check(sql.includes(code), `Missing guarded migration element: ${code}`);
  check(!/\bDROP\s+TABLE\b|\bTRUNCATE\b|\bDELETE\s+FROM\b/i.test(sql), 'Destructive SQL found');
  check(!/\bUPDATE\s+ORG\./i.test(sql), 'Existing ORG data may not be rewritten');
  check(service.includes('lockUnit(input.unitId())') && service.includes('validateAssignmentUniqueness'), 'Contact business validation absent');
  check(!service.includes('CIF.CONTACT_POINT'), 'CIF must remain independent');
  check(api.includes('@PostMapping') && api.includes('@PutMapping') && api.includes('@GetMapping'), 'Contact read/write API absent');
  check(ui.includes('OrganizationUnitPickerComponent') && ui.includes('PersianDateInputComponent'), 'Usable selectors missing');
  check(ui.includes('contactValue') && !ui.includes('formControlName="contactPointId"'), 'Raw numeric ID reintroduced');
  check(legacy.includes('"organization-unit-contact-points".equals(resource)'), 'Generic legacy contact writes not gated');
  check(routes.includes("path: 'organization/unit-contacts'") && routes.includes("redirectTo: 'organization/unit-contacts'"), 'Dedicated route/redirect missing');
  check(menu.includes("resource: 'unit-contacts'") && !menu.includes("resource: 'organization-unit-locations'"), 'Unwanted old menu entry');
  check(title.includes('نشانی و موقعیت شعب و واحدها') && !title.includes('مکان و نشانی تجمیعی واحدهای سازمانی'), 'Location title unchanged');
  check(cmd.includes('verify-org-contacts-r1.mjs') && sh.includes('verify-org-contacts-r1.mjs'), 'Contacts verification not wired into both build scripts');
  console.log('ORG_CONTACTS_RELEASE_STATIC_VERIFY_PASS');
} catch (e) {
  console.error('ORG_CONTACTS_RELEASE_STATIC_VERIFY_FAIL:',e.message);
  process.exitCode=1;
}
