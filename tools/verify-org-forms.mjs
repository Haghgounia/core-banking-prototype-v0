import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const fail = (message) => { console.error(`ORG_FORMS_VERIFY_FAIL: ${message}`); process.exit(1); };
const ok = (message) => console.log(`PASS ${message}`);
const read = (relative) => {
  const file = path.join(root, relative);
  if (!fs.existsSync(file)) fail(`missing file: ${relative}`);
  return fs.readFileSync(file, 'utf8');
};

const expected = [
  'contact-roles',
  'employee-assignments',
  'employee-assignment-roles',
  'foreign-exchange-service-profiles',
  'locations',
  'operating-schedules',
  'operating-schedule-intervals',
  'organizations',
  'organization-units',
  'organization-unit-classifications',
  'organization-unit-contact-points',
  'organization-unit-geo-coverages',
  'organization-unit-lifecycle-events',
  'organization-unit-lifecycle-event-types',
  'organization-unit-locations',
  'organization-unit-operating-exceptions',
  'organization-unit-operating-exception-intervals',
  'organization-unit-operating-schedules',
  'organization-unit-point-of-service-assignments',
  'organization-unit-relationships',
  'organization-unit-relationship-rules',
  'organization-unit-relationship-types',
  'organization-unit-service-capabilities',
  'organization-unit-types',
  'points-of-service',
  'point-of-service-contact-points',
  'point-of-service-operating-exceptions',
  'point-of-service-operating-exception-intervals',
  'point-of-service-operating-schedules',
  'point-of-service-types',
  'postal-addresses',
  'self-service-terminals',
  'self-service-terminal-assignments',
  'self-service-terminal-types',
  'service-capabilities'
].sort();

const provider = read('backend/src/main/java/com/behsazan/corebanking/organization/descriptor/OrganizationDescriptorProvider.java');
const actual = [...provider.matchAll(/return\s+descriptor\(\s*"([^"]+)"[\s\S]*?schemaName,\s*"([A-Z0-9_]+)"/g)]
  .map(([, resource, table]) => ({resource, table}));
if (actual.length !== 35) fail(`expected 35 ORG descriptors, found ${actual.length}`);
const actualResources = actual.map(x => x.resource).sort();
if (JSON.stringify(actualResources) !== JSON.stringify(expected)) {
  const missing = expected.filter(x => !actualResources.includes(x));
  const extra = actualResources.filter(x => !expected.includes(x));
  fail(`descriptor resources mismatch; missing=${missing.join(',') || '-'} extra=${extra.join(',') || '-'}`);
}
for (const {resource, table} of actual) {
  const expectedTable = resource.replaceAll('-', '_').toUpperCase();
  if (table !== expectedTable) fail(`${resource}: expected table ${expectedTable}, found ${table}`);
}
ok('35 ORG table descriptors and resource/table mappings');

for (const token of ['createdAt', 'CREATED_AT', 'createdBy', 'CREATED_BY', 'updatedAt', 'UPDATED_AT', 'updatedBy', 'UPDATED_BY']) {
  if (!provider.includes(token)) fail(`ORGANIZATION_UNITS audit field missing from provider: ${token}`);
}
ok('ORGANIZATION_UNITS audit fields');

const yaml = read('backend/src/main/resources/application.yml');
if (!/^\s*organization:\s*ORG\s*$/m.test(yaml)) fail('application.yml does not contain organization: ORG');
ok('ORG schema configuration');

const routes = read('frontend/src/app/app.routes.ts');
if (!routes.includes("path: 'organization'")) fail('missing /organization route');
if (!routes.includes("path: 'organization/:resource'")) fail('missing /organization/:resource route');
ok('Angular ORG routes');

const shell = read('frontend/src/app/layout/app-shell.component.html');
if (!shell.includes('routerLink="/organization"') || !shell.includes('ساختار سازمانی و شبکه شعب')) fail('ORG sidebar entry missing');
ok('sidebar entry');

const menu = read('frontend/src/app/features/organization/organization-menu.component.ts');
const resourceBlocks = [...menu.matchAll(/resources:\s*\[([\s\S]*?)\]/g)].map(m => m[1]);
const menuResourceMatches = resourceBlocks.flatMap(block => [...block.matchAll(/['"]([^'"]+)['"]/g)].map(m => m[1]));
for (const resource of expected) {
  const count = menuResourceMatches.filter(x => x === resource).length;
  if (count !== 1) fail(`menu resource ${resource} appears ${count} times; expected 1`);
}
if (menuResourceMatches.length !== 35) fail(`menu expected 35 resources, found ${menuResourceMatches.length}`);
ok('menu contains all 35 ORG forms exactly once');

const repository = read('backend/src/main/java/com/behsazan/corebanking/referencedata/management/oracle/OracleReferenceRepository.java');
if (!repository.includes('optionalField("createdAt")') || !repository.includes('SYSTIMESTAMP')) fail('generic insert does not support createdAt/SYSTIMESTAMP');
ok('generic repository createdAt support');

const service = read('backend/src/main/java/com/behsazan/corebanking/referencedata/management/application/ReferenceService.java');
for (const pair of [['effectiveFrom','effectiveTo'], ['openingDate','closingDate'], ['installationDate','decommissionDate']]) {
  if (!service.includes(`"${pair[0]}"`) || !service.includes(`"${pair[1]}"`)) fail(`date validation missing for ${pair[0]}/${pair[1]}`);
}
ok('cross-field date validation');

console.log('ORG_FORMS_VERIFY_PASS');
