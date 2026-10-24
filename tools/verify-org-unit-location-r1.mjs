import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const fail = (message) => { console.error(`ORG_UNIT_LOCATION_R1_VERIFY_FAIL: ${message}`); process.exit(1); };
const read = (file) => {
  const full = path.join(root, file);
  if (!fs.existsSync(full)) fail(`file missing: ${file}`);
  return fs.readFileSync(full, 'utf8');
};
const assert = (condition, message) => { if (!condition) fail(message); };
const base = 'database/oracle/org/unit-location-consolidation/';
const [preview, add, backfill, verify] = [
  '01-ORG-UNIT-LOCATIONS-PREVIEW-READONLY.sql',
  '02-ORG-UNIT-LOCATIONS-ADD-COLUMNS-OPT-IN.sql',
  '03-ORG-UNIT-LOCATIONS-BACKFILL-OPT-IN.sql',
  '04-ORG-UNIT-LOCATIONS-VERIFY-READONLY.sql'
].map(name => read(base + name));
const provider = read('backend/src/main/java/com/behsazan/corebanking/organization/descriptor/OrganizationDescriptorProvider.java');
for (const token of [
  '"ORGANIZATION_UNIT_LOCATIONS"', '"SEQ_ORGANIZATION_UNIT_LOCATIONS"',
  '"LOCATIONS"', '"POSTAL_ADDRESSES"',
  'lookup("locationId", "LOCATION_ID", "مکان", "locations", true, true)'
]) assert(provider.includes(token), `legacy descriptor compatibility missing: ${token}`);
console.log('PASS legacy descriptors and location references retained during R1');

const backend = read('backend/src/main/java/com/behsazan/corebanking/organization/location/application/OrganizationUnitLocationReviewService.java');
const controller = read('backend/src/main/java/com/behsazan/corebanking/organization/location/web/OrganizationUnitLocationReviewController.java');
for (const token of ['ORGANIZATION_UNIT_LOCATIONS UL', 'LEFT JOIN %1$s.LOCATIONS L',
  'LEFT JOIN %1$s.POSTAL_ADDRESSES P', 'P.POSTAL_ADDRESS_ID', 'POSTAL_ADDRESS_COUNT',
  'SERVICE_POINTS', 'NamedParameterJdbcTemplate', '@Transactional(readOnly = true)']) {
  assert(backend.includes(token), `read-only union projection incomplete: ${token}`);
}
assert(controller.includes('@GetMapping') && !/\@(PostMapping|PutMapping|DeleteMapping|PatchMapping)/.test(controller), 'review controller should be GET-only');
assert(!/jdbc\.update\s*\(|executeUpdate\s*\(/.test(backend), 'review service contains unexpected write operations');
console.log('PASS no-write combined legacy projection preserves all address variants');

const routes = read('frontend/src/app/app.routes.ts');
const component = read('frontend/src/app/features/organization/organization-unit-locations-review.component.ts');
const menu = read('frontend/src/app/features/organization/organization-menu.component.html');
assert(routes.includes("path: 'organization/unit-locations-review'") &&
  routes.indexOf("path: 'organization/unit-locations-review'") < routes.indexOf("path: 'organization/:resource'"), 'specific review route must precede the generic resource route');
for (const token of ['reviewMode', 'legacyPostalAddressId', 'postalAddressCount > 1',
  'organizationUnitLocationId']) {
  assert(component.includes(token), `review UI misses: ${token}`);
}
for (const retired of ['locations','postal-addresses','organization-unit-locations']) {
  assert(!component.includes(`routerLink="/organization/${retired}"`), `${retired} old editor entry advertised in source review`);
}
assert(!menu.includes('routerLink="/organization/unit-locations-review"') &&
  menu.includes('routerLink="/organization/unit-locations-canonical"'),
  'menu must lead to canonical viewer, not legacy view');
console.log('PASS read-only source view retained, legacy editors hidden from navigation, canonical route advertised');

assert(preview.includes('EXACT COUNTS') && preview.includes('LISTAGG(') && preview.includes('SERVICE_POINTS') &&
  preview.includes('OPEN_PRIMARY_ROWS'), 'read-only preview misses count/ambiguity/point-service evidence');
for (const sql of [add, backfill]) {
  assert(/DEFINE\s+APPLY_CHANGES\s*=\s*NO/i.test(sql), 'opt-in SQL must default to NO');
  const executable = sql.split('\n').filter(line => !/^\s*--/.test(line)).join('\n');
  assert(!/\bDROP\s+(TABLE|SEQUENCE|INDEX)\b|\bTRUNCATE\s+TABLE\b|\bCASCADE\s+CONSTRAINTS\b/i.test(executable), 'forbidden destructive SQL operation found');
  assert(!/^\s*COMMIT\s*;/im.test(executable), 'auto commit is forbidden in SQL scripts');
}
const expectedColumns = [
  'GEO_ENTITY_ID','LATITUDE','LONGITUDE','COORDINATE_REFERENCE_SYSTEM_CODE',
  'LOCATION_DESCRIPTION_FA','LOCATION_DESCRIPTION_EN','ADDRESS_TYPE_CODE',
  'MAIN_STREET_FA','MAIN_STREET_EN','SECONDARY_STREET_FA','SECONDARY_STREET_EN',
  'SQUARE_CROSSROAD_FA','SQUARE_CROSSROAD_EN','BUILDING_NO','POSTAL_CODE',
  'ADDRESS_FA','ADDRESS_EN','ADDITIONAL_ADDRESS_FA','ADDITIONAL_ADDRESS_EN',
  'LEGACY_POSTAL_ADDRESS_ID','RELOCATION_REASON_CODE','DECISION_REFERENCE',
  'DECISION_DATE','LEGACY_BACKFILL_FLAG','LEGACY_BACKFILLED_AT','ROW_VERSION'
];
for (const field of expectedColumns) {
  assert(add.includes(field) && backfill.includes(`'${field}'`), `staging column missing: ${field}`);
}
assert(add.includes('ADD (') && add.includes('v_existing NOT IN (0,26)') &&
  !/ALTER\s+TABLE\s+ORG\.LOCATIONS/i.test(add), 'staging DDL changes legacy data or lacks partial-install guard');
console.log('PASS 26 additive columns, no destructive DDL and opt-in guard');

for (const token of [
  'v_ambiguous+v_orphan+v_conflict+v_active_triggers>0', 'P.LOCATION_ID=UL.LOCATION_ID',
  'WHERE DST.LEGACY_BACKFILL_FLAG=0', 'LEGACY_POSTAL_ADDRESS_ID',
  'SAVEPOINT ORG_UNIT_LOCATION_R1_BACKFILL', 'ROLLBACK TO ORG_UNIT_LOCATION_R1_BACKFILL',
  'NO AUTO COMMIT', 'MERGE INTO ORG.ORGANIZATION_UNIT_LOCATIONS'
]) assert(backfill.includes(token), `loss prevention gate missing: ${token}`);
assert(!/\bINSERT\s+INTO\s+ORG\.LOCATIONS/i.test(backfill), 'must not regenerate legacy IDs');
assert(verify.includes('MISMATCHED_ROWS') && verify.includes('DECODE(UL.POSTAL_CODE, P.POSTAL_CODE') &&
  verify.includes('LEGACY_SERVICE_POINTS'), 'post-apply exact value checking incomplete');
console.log('PASS lossless snapshot backfill (zero conflicting rows) and exact-value verification');
console.log('ORG_UNIT_LOCATION_R1_VERIFY_PASS');
