import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const check = (ok, msg) => { if (!ok) throw Error(msg); };
try {
  const menu = read('frontend/src/app/features/organization/organization-menu.component.ts');
  const html = read('frontend/src/app/features/organization/organization-menu.component.html');
  const review = read('frontend/src/app/features/organization/organization-unit-locations-review.component.ts');
  const canonical = read('frontend/src/app/features/organization/organization-unit-locations-canonical.component.ts');
  const routes = read('frontend/src/app/app.routes.ts');
  const provider = read('backend/src/main/java/com/behsazan/corebanking/organization/descriptor/OrganizationDescriptorProvider.java');
  const sql = read('database/oracle/org/contact-points/01-ORG-BRANCH-CONTACTS-PREVIEW-READONLY.sql');
  const resources = [...menu.matchAll(/resources:\s*\[([\s\S]*?)\]/g)].flatMap(m => [...m[1].matchAll(/'([^']+)'/g)].map(x => x[1]));
  const legacy = ['locations', 'postal-addresses', 'organization-unit-locations'];
  for (const resource of legacy) {
    check(!resources.includes(resource), `${resource} still in visible ORG menu`);
    check(!new RegExp(`resource: '${resource}'`).test(menu), `${resource} still in ORG quick links`);
    check(provider.includes(`"${resource}"`), `${resource} legacy backend descriptor was removed`);
    check(!html.includes(`routerLink="/organization/${resource}"`), `${resource} link in menu template`);
    check(!review.includes(`routerLink="/organization/${resource}"`), `${resource} link in legacy source inspection page`);
  }
  check(resources.includes('contact-roles') && resources.includes('organization-unit-contact-points'), 'unit contacts and roles missing');
  check(resources.includes('point-of-service-contact-points'), 'service point contacts disappeared');
  check(menu.includes('canonicalLocation: true') && menu.includes('canonicalVisible(group)') &&
    html.includes('routerLink="/organization/unit-locations-canonical"'), 'canonical location menu incomplete');
  check(menu.includes("resource: 'unit-contacts'") && routes.includes("path: 'organization/unit-contacts'") && routes.includes("path: 'organization/organization-unit-contact-points'"), 'business contact form route missing');
  check(!html.includes('routerLink="/organization/unit-locations-review"'), 'old three-table review should not be a parallel primary menu item');
  check(routes.includes("path: 'organization/unit-locations-review'") &&
    routes.includes("path: 'organization/unit-locations-review'") &&
    routes.includes("path: 'organization/unit-locations-canonical'"), 'read-only technical comparison route was removed');
  check(!canonical.includes('مکان و نشانی تجمیعی واحدهای سازمانی') && canonical.includes('نشانی و موقعیت شعب و واحدها'), 'canonical location title not updated');
  check(resources.length===20, `expected 20 visible generic ORG resources; found ${resources.length}`);
  console.log('PASS user menu: 3 retired location forms hidden; 1 canonical entry; dedicated contacts route');
  console.log('PASS legacy data descriptors, service point contact resource, technical view preserved');

  const sourceWithoutComments = sql.replace(/^\s*--.*$/gm, '').replace(/^\s*PROMPT .*$/gm, '');
  check(!/\b(?:INSERT\s+INTO|UPDATE\s+\S+\s+SET|DELETE\s+FROM|TRUNCATE\s+TABLE|ALTER\s+TABLE|CREATE\s+TABLE|DROP\s+TABLE|MERGE\s+INTO|COMMIT\s*;|ROLLBACK\s*;)\b/i.test(sourceWithoutComments), 'contact baseline contains DDL or DML');
  for (const token of [
    'ORG.ORGANIZATION_UNIT_CONTACT_POINTS', 'ORG.CONTACT_ROLES', 'ORG.SERVICE_POINT_CONTACT_POINTS',
    'ORG.ORGANIZATION_UNIT_TYPES', 'ALL_CONS_COLUMNS', 'ALL_TAB_COLUMNS',
    'MATCHED_DISTINCT_IDS_IN_CIF=', 'CIF_CONTACT_POINT_SELECT_NOT_AVAILABLE'
  ]) check(sql.includes(token), `missing contact diagnostic check ${token}`);
  check(!sql.includes('SELECT CONTACT_VALUE'), 'contact baseline must not expose customer contact values');
  console.log('PASS contacts: read-only baseline, role/coverage/primary/owner checks, no personal contact values');
  console.log('ORG_MENU_CONTACT_R0_VERIFY_PASS');
} catch (error) {
  console.error('ORG_MENU_CONTACT_R0_VERIFY_FAIL:', error.message);
  process.exitCode=1;
}
