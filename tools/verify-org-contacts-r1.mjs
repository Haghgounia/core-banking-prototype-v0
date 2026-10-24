import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const assert = (ok, message) => { if (!ok) throw new Error(message); };
try {
  const script = read('database/oracle/org/contacts-r1/01-ORG-CONTACT-MASTER-OPT-IN.sql');
  const service = read('backend/src/main/java/com/behsazan/corebanking/organization/contact/OrganizationContactService.java');
  const controller = read('backend/src/main/java/com/behsazan/corebanking/organization/contact/OrganizationContactController.java');
  const ui = read('frontend/src/app/features/organization/organization-unit-contacts.component.ts');
  const legacy = read('backend/src/main/java/com/behsazan/corebanking/referencedata/management/application/ReferenceService.java');
  const routes = read('frontend/src/app/app.routes.ts');
  const menu = read('frontend/src/app/features/organization/organization-menu.component.ts');
  const sourceTitle = read('frontend/src/app/features/organization/organization-unit-locations-canonical.component.ts');
  assert(script.includes('DEFINE APPLY_CHANGES = NO') && script.includes("v_apply = 'NO'"), 'schema opt-in preview missing');
  assert(!/\bDROP\s+TABLE\b|\bTRUNCATE\s+TABLE\b|\bDELETE\s+FROM\b|\bUPDATE\s+ORG\./i.test(script), 'unexpected destructive SQL');
  for (const token of ['CREATE TABLE ORG.CONTACT_POINTS','FK_OUCP_CONTACT_MASTER','FK_SPCP_CONTACT_MASTER','EXISTING_ASSOCIATIONS','NO LEGACY/CIF DATA WAS CHANGED'])
    assert(script.includes(token), `schema guard missing ${token}`);
  assert(service.includes('lockUnit(input.unitId())') && service.includes('validateAssignmentUniqueness(input'), 'transaction conflict guard missing');
  assert(service.includes('ORG.CONTACT_POINTS') && service.includes('ORG.ORGANIZATION_UNIT_CONTACT_POINTS'), 'canonical ORG sources missing');
  assert(!service.includes('CIF.CONTACT_POINT'), 'CIF coupling introduced');
  assert(service.includes('SELECT_ROWS') && service.includes('INSERT INTO ORG.CONTACT_POINTS'), 'missing contact value persistence');
  assert(controller.includes('@PostMapping') && controller.includes('@PutMapping') && controller.includes('@GetMapping'), 'API operations missing');
  assert(ui.includes('OrganizationUnitPickerComponent') && ui.includes('PersianDateInputComponent'), 'unit picker or Persian date inputs missing');
  assert(ui.includes('contactValue') && !ui.includes('formControlName="contactPointId"'), 'contact user interface not corrected');
  assert(legacy.includes('"organization-unit-contact-points".equals(resource)'), 'legacy generic write gate missing');
  assert(routes.includes("path: 'organization/unit-contacts'") && routes.includes("redirectTo: 'organization/unit-contacts'"), 'canonical contact route missing');
  assert(menu.includes("resource: 'unit-contacts'"), 'new contact quick link missing');
  assert(sourceTitle.includes('نشانی و موقعیت شعب و واحدها') && !sourceTitle.includes('مکان و نشانی تجمیعی واحدهای سازمانی'), 'location title not renamed');
  console.log('ORG_CONTACTS_R1_STATIC_VERIFY_PASS');
} catch (error) {
  console.error('ORG_CONTACTS_R1_STATIC_VERIFY_FAIL:', error.message);
  process.exitCode = 1;
}
