import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
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

const referencePageTs = read('frontend/src/app/features/reference-data/presentation/reference-page.component.ts');
const referencePageHtml = read('frontend/src/app/features/reference-data/presentation/reference-page.component.html');
for (const token of ['organizationUnitsPage', 'organizationUnitTypeFilterControl', 'organizationUnitTypeId']) {
  if (!referencePageTs.includes(token)) fail(`ORGANIZATION_UNITS type filter logic missing: ${token}`);
}
for (const token of ['نوع واحد', 'همه انواع واحد', "lookupOptions()['organizationUnitTypeId']"]) {
  if (!referencePageHtml.includes(token)) fail(`ORGANIZATION_UNITS type filter UI missing: ${token}`);
}
ok('ORGANIZATION_UNITS unit-type filter');

const expectedOrgFilters = {
  'organizations': ['statusCode'],
  'organization-units': ['organizationId', 'statusCode'],
  'organization-unit-relationship-types': ['hierarchicalFlag'],
  'organization-unit-relationships': ['sourceOrganizationUnitId', 'targetOrganizationUnitId', 'organizationUnitRelationshipTypeId'],
  'organization-unit-relationship-rules': ['organizationUnitRelationshipTypeId', 'sourceOrganizationUnitTypeId', 'targetOrganizationUnitTypeId'],
  'organization-unit-lifecycle-events': ['organizationUnitLifecycleEventTypeId', 'successorOrganizationUnitId'],
  'postal-addresses': ['addressTypeCode'],
  'organization-unit-locations': ['locationId', 'locationRoleCode'],
  'organization-unit-geo-coverages': ['coverageTypeCode'],
  'operating-schedules': ['scheduleTypeCode'],
  'operating-schedule-intervals': ['dayOfWeekCode', 'shiftNo'],
  'organization-unit-operating-schedules': ['operatingScheduleId', 'scheduleRoleCode'],
  'organization-unit-operating-exceptions': ['exceptionTypeCode'],
  'organization-unit-operating-exception-intervals': ['shiftNo'],
  'employee-assignments': ['employeeAssignmentRoleId', 'primaryFlag'],
  'organization-unit-service-capabilities': ['serviceCapabilityId', 'statusCode'],
  'foreign-exchange-service-profiles': ['recordIncomeFlag', 'statusCode'],
  'points-of-service': ['pointOfServiceTypeId', 'locationId', 'statusCode'],
  'organization-unit-point-of-service-assignments': ['pointOfServiceId', 'assignmentRoleCode'],
  'point-of-service-contact-points': ['contactRoleId', 'primaryFlag'],
  'point-of-service-operating-schedules': ['operatingScheduleId'],
  'point-of-service-operating-exceptions': ['exceptionTypeCode'],
  'point-of-service-operating-exception-intervals': ['shiftNo'],
  'self-service-terminals': ['selfServiceTerminalTypeId', 'statusCode'],
  'self-service-terminal-assignments': ['organizationUnitId', 'assignmentRoleCode'],
  'organization-unit-contact-points': ['contactRoleId', 'primaryFlag']
};
for (const [resource, fields] of Object.entries(expectedOrgFilters)) {
  const expectedMapping = `'${resource}': [${fields.map(field => `'${field}'`).join(', ')}]`;
  if (!referencePageTs.includes(expectedMapping)) fail(`ORG specialized filter mapping mismatch for ${resource}`);
}
for (const token of ['organizationFilterFields', 'collectAdvancedFilters', 'applyAdvancedFilters']) {
  if (!referencePageTs.includes(token)) fail(`ORG filter orchestration missing: ${token}`);
}
for (const token of ["field.type === 'BOOLEAN' && store.descriptor()?.category === 'ORGANIZATION'", '[value]="true"', '[value]="false"']) {
  if (!referencePageHtml.includes(token)) fail(`ORG boolean filter UI missing: ${token}`);
}
ok('contextual filters across ORG forms');

const repository = read('backend/src/main/java/com/behsazan/corebanking/referencedata/management/oracle/OracleReferenceRepository.java');
if (!repository.includes('optionalField("createdAt")') || !repository.includes('SYSTIMESTAMP')) fail('generic insert does not support createdAt/SYSTIMESTAMP');
ok('generic repository createdAt support');

for (const token of [
  '"ORGANIZATION".equals(descriptor.category())',
  'appendOrganizationLookupSearch(descriptor, search)',
  'field.type() != FieldType.LOOKUP',
  'EXISTS (SELECT 1 FROM ',
  'lookupDescriptor.codeApiName()',
  'lookupDescriptor.nameApiName()'
]) {
  if (!repository.includes(token)) fail(`ORG lookup-label text search missing: ${token}`);
}
ok('ORG text search includes displayed lookup code/name labels');

for (const token of [
  "field.lookupResource === 'organization-units'",
  'organizationUnitGridValue(value, code, label)',
  "row[`${field.apiName}__unitCode`]",
  "row[`${field.apiName}__unitName`]",
  "descriptor.parent?.resource === 'organization-units'",
  "row['parentId']",
  "row['parentUnitCode']",
  "field.apiName === 'unitCode'"
]) {
  if (!referencePageTs.includes(token)) fail(`ORG unit id/code grid rendering missing: ${token}`);
}
if (!referencePageHtml.includes('{{ parentCell(row) }}')) fail('ORG parent unit grid cell does not use id/code renderer');
for (const token of [
  'organizationUnitGridDisplayColumns(selected)',
  '"organization-units".equals(field.lookupResource())',
  '__unitCode',
  '__unitName',
  'AS \\\"parentId\\\"',
  'AS \\\"parentUnitCode\\\"',
  'mapSearchFields(rs, selected, descriptor)'
]) {
  if (!repository.includes(token)) fail(`ORG unit id/code backend projection missing: ${token}`);
}
ok('organization-unit id and unit code are shown together across ORG grids');

const service = read('backend/src/main/java/com/behsazan/corebanking/referencedata/management/application/ReferenceService.java');
for (const pair of [['effectiveFrom','effectiveTo'], ['openingDate','closingDate'], ['installationDate','decommissionDate']]) {
  if (!service.includes(`"${pair[0]}"`) || !service.includes(`"${pair[1]}"`)) fail(`date validation missing for ${pair[0]}/${pair[1]}`);
}
ok('cross-field date validation');

console.log('ORG_FORMS_VERIFY_PASS');
