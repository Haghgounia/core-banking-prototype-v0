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
  check(legacy.includes('havingValue = "legacy", matchIfMissing = true'), 'Legacy must remain the default');
  check(canonical.includes('havingValue = "canonical"'), 'Canonical mode needs explicit configuration');
  check(api.includes('OrganizationContactOperations'), 'Controller must use exactly one implementation');
  check(models.includes('String storageMode, boolean writesEnabled'), 'Readiness contract is incomplete');
  check(canonical.includes('if (!allowWrites) throw OrganizationContactException.writesDisabled()'), 'Explicit write guard missing');
  check(canonical.includes('equal(columns, 15)') && canonical.includes('equal(unitFk, 1)') && canonical.includes('equal(roleFk, 1)'), 'Fail-closed schema check missing');
  check(canonical.includes('return new ContactRow(id, id,'), 'Compatibility identity alias missing');
  for (const bad of ['INSERT INTO ORG.ORGANIZATION_UNIT_CONTACT_POINTS', 'UPDATE ORG.ORGANIZATION_UNIT_CONTACT_POINTS', 'INSERT INTO ORG.CONTACT_POINTS_V2', 'UPDATE ORG.CONTACT_POINTS_V2'])
    check(!canonical.includes(bad), `Forbidden canonical write target: ${bad}`);
  check(canonical.includes('ROW_VERSION=ROW_VERSION+1') && canonical.includes('AND ROW_VERSION=:expectedVersion'), 'Optimistic concurrency missing');
  check(canonical.includes('FOR UPDATE OF c.CONTACT_ROLE_ID') && canonical.includes('lockUnit(input.unitId())'), 'Writer locking missing');
  check(canonical.includes("'MANUAL', :actor") && canonical.includes('UPDATED_AT=SYSTIMESTAMP'), 'Auditing missing');
  check(reference.includes('"point-of-service-contact-points".equals(resource)'), 'Service point generic write guard missing');
  check(descriptor.includes('entries.removeIf') && descriptor.includes('"canonical".equalsIgnoreCase(contactsStorageMode)'), 'Retired descriptors not gated');
  check(!menu.includes("resources: ['contact-roles', 'organization-unit-contact-points']"), 'Old generic contact item still visible');
  check(!menu.includes("'point-of-service-contact-points']"), 'Unsafe POS contact item still visible');
  check(ui.includes("this.storageMode.set(state.storageMode === 'CANONICAL'") && ui.includes('this.writesEnabled.set(state.writesEnabled !== false)'), 'Angular mode/read-only gating missing');
  check(ui.includes('row.contactPointId !== row.organizationUnitContactPointId'), 'Canonical identity check missing in UI');
  check(ui.includes('!this.writesEnabled()'), 'Angular write guard missing');
  check(route.includes("path: 'organization/unit-contacts'"), 'Public contact route altered');
  for (const s of [conf, config]) {
    check(s.includes('storage-mode: ${ORG_CONTACT_STORAGE_MODE:legacy}'), 'Safe default storage-mode missing');
    check(s.includes('canonical-writes-enabled: ${ORG_CONTACT_CANONICAL_WRITES_ENABLED:false}'), 'Safe default write flag missing');
  }
  check(!/\b(DROP\s+TABLE|TRUNCATE\s+TABLE|ALTER\s+TABLE)\b/i.test(canonical), 'Unexpected DDL in canonical service');
  console.log('ORG_CONTACT_R3B2_STATIC_VERIFY_PASS');
} catch (error) {
  console.error('ORG_CONTACT_R3B2_STATIC_VERIFY_FAIL:', error.message);
  process.exitCode = 1;
}
