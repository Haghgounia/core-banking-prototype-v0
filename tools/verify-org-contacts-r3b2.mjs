// R3-B2 code-only acceptance checks; no database access, writes or network.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const check = (condition, message) => { if (!condition) throw new Error(message); };
try {
  const legacy = read('backend/src/main/java/com/behsazan/corebanking/organization/contact/OrganizationContactService.java');
  const canonical = read('backend/src/main/java/com/behsazan/corebanking/organization/contact/CanonicalOrganizationContactService.java');
  const api = read('backend/src/main/java/com/behsazan/corebanking/organization/contact/OrganizationContactController.java');
  const models = read('backend/src/main/java/com/behsazan/corebanking/organization/contact/OrganizationContactModels.java');
  const descriptor = read('backend/src/main/java/com/behsazan/corebanking/organization/descriptor/OrganizationDescriptorProvider.java');
  const reference = read('backend/src/main/java/com/behsazan/corebanking/referencedata/management/application/ReferenceService.java');
  const ui = read('frontend/src/app/features/organization/organization-unit-contacts.component.ts');
  const menu = read('frontend/src/app/features/organization/organization-menu.component.ts');
  const conf = read('backend/src/main/resources/application.yml');
  const config = read('config/application.yml');
  const route = read('frontend/src/app/app.routes.ts');
  check(legacy.includes('havingValue = "legacy", matchIfMissing = true'), 'Default contact mode changed');
  check(legacy.includes('INSERT INTO ORG.ORGANIZATION_UNIT_CONTACT_POINTS') &&
    legacy.includes('UPDATE ORG.ORGANIZATION_UNIT_CONTACT_POINTS'), 'OUCP operational DML removed');
  check(canonical.includes('havingValue = "canonical"'), 'Historical configuration alias missing');
  check(canonical.includes('implements OrganizationContactOperations') &&
    canonical.includes('new OrganizationContactService(jdbc, false)'), 'Canonical alias must use OUCP and its write gate');
  check(!canonical.includes('CONTACT_POINTS_V2') && !canonical.includes('canonical-writes-enabled:false'),
    'Canonical alias still exposes historical V2 access or bypass flag');
  check(api.includes('OrganizationContactOperations'), 'Controller contract missing');
  check(models.includes('String storageMode, boolean writesEnabled'), 'Readiness contract incomplete');
  check(legacy.includes('equal(unitFk, 0)') && legacy.includes('equal(serviceFk, 0)') &&
    legacy.includes('equal(nullableLegacy, 1)'), 'Detach gate removed');
  check(legacy.includes('ROW_VERSION=ROW_VERSION+1') &&
    legacy.includes('AND ROW_VERSION=:expectedVersion'), 'Optimistic concurrency missing');
  check(legacy.includes('FOR UPDATE OF a.CONTACT_ROLE_ID') &&
    legacy.includes('lockUnit(input.unitId())'), 'Writer lock missing');
  check(legacy.includes('requireWriteAccess()'), 'Explicit OUCP write guard missing');
  check(reference.includes('"point-of-service-contact-points".equals(resource)'), 'Service-point generic write guard missing');
  check(descriptor.includes('entries.removeIf'), 'Retired descriptors not gated');
  check(!menu.includes("resources: ['contact-roles', 'organization-unit-contact-points']"), 'Old generic contact item still visible');
  check(!menu.includes("'point-of-service-contact-points']"), 'Unsafe POS contact item still visible');
  check(ui.includes('this.writesEnabled.set(state.writesEnabled !== false)'), 'Angular read-only gating missing');
  check(ui.includes('row.contactPointId !== row.organizationUnitContactPointId'), 'Historical identity-display compatibility missing');
  check(ui.includes('!this.writesEnabled()'), 'Angular write guard missing');
  check(route.includes("path: 'organization/unit-contacts'"), 'Public contact route altered');
  for (const s of [conf, config]) {
    check(s.includes('storage-mode: ${ORG_CONTACT_STORAGE_MODE:legacy}'), 'Safe default storage-mode missing');
    check(s.includes('unit-writes-enabled: ${ORG_CONTACT_UNIT_WRITES_ENABLED:false}'), 'Safe unit write opt-in missing');
  }
  check(!/\b(DROP\s+TABLE|TRUNCATE\s+TABLE|ALTER\s+TABLE)\b/i.test(canonical), 'Unexpected DDL in alias service');
  console.log('ORG_CONTACT_R3B2_STATIC_VERIFY_PASS');
} catch (error) {
  console.error('ORG_CONTACT_R3B2_STATIC_VERIFY_FAIL:', error.message);
  process.exitCode = 1;
}
