#!/usr/bin/env node
// Static ORG->CAL2 ownership regression. No Oracle connection or writes.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p => fs.readFileSync(path.join(root,p), 'utf8');
const fail = reason => { throw new Error(`ORG_CAL2_CONSOLIDATION_VERIFY_FAIL: ${reason}`); };
const retired = [
  ['operating-schedules','OPERATING_SCHEDULES','SEQ_OPERATING_SCHEDULES'],
  ['operating-schedule-intervals','OPERATING_SCHEDULE_INTERVALS','SEQ_OPERATING_SCHEDULE_INTERVALS'],
  ['organization-unit-operating-schedules','ORGANIZATION_UNIT_OPERATING_SCHEDULES','SEQ_ORGANIZATION_UNIT_OPERATING_SCHEDULES'],
  ['organization-unit-operating-exceptions','ORGANIZATION_UNIT_OPERATING_EXCEPTIONS','SEQ_ORGANIZATION_UNIT_OPERATING_EXCEPTIONS'],
  ['organization-unit-operating-exception-intervals','ORGANIZATION_UNIT_OPERATING_EXCEPTION_INTERVALS','SEQ_ORGANIZATION_UNIT_OPERATING_EXCEPTION_INTERVALS'],
  ['point-of-service-operating-schedules','SERVICE_POINT_OPERATING_SCHEDULES','SEQ_POINT_OF_SERVICE_OPERATING_SCHEDULES'],
  ['point-of-service-operating-exceptions','SERVICE_POINT_OPERATING_EXCEPTIONS','SEQ_POINT_OF_SERVICE_OPERATING_EXCEPTIONS'],
  ['point-of-service-operating-exception-intervals','SERVICE_POINT_OPERATING_EXCEPTION_INTERVALS','SEQ_POINT_OF_SERVICE_OPERATING_EXCEPTION_INTERVALS']
];
try {
  const provider = read('backend/src/main/java/com/behsazan/corebanking/organization/descriptor/OrganizationDescriptorProvider.java');
  const menu = read('frontend/src/app/features/organization/organization-menu.component.ts');
  const menuHtml = read('frontend/src/app/features/organization/organization-menu.component.html');
  const filters = read('frontend/src/app/features/reference-data/presentation/reference-page.component.ts');
  const routes = read('frontend/src/app/app.routes.ts');
  const cal2 = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/application/Calendar2ReferenceRegistry.java');
  const preview = read('database/oracle/org/calendar2-consolidation/01-ORG-CAL2-PREVIEW-READONLY.sql');
  const drop = read('database/oracle/org/calendar2-consolidation/02-ORG-CAL2-DROP-EMPTY-OPT-IN.sql');
  const post = read('database/oracle/org/calendar2-consolidation/03-ORG-CAL2-VERIFY-READONLY.sql');
  for (const [resource,table,seq] of retired) {
    if (provider.includes(`"${resource}"`) || provider.includes(`"${table}"`)) fail(`still advertised by backend: ${resource}`);
    if (menu.includes(`'${resource}'`)) fail(`still in ORG menu: ${resource}`);
    if (filters.includes(`'${resource}':`)) fail(`stale ORG filters: ${resource}`);
    if (!routes.includes(`path: 'organization/${resource}', redirectTo: 'calendar2/reference-data'`)) fail(`missing safe redirect: ${resource}`);
    if (!preview.includes(table) || !drop.includes(table) || !post.includes(table)) fail(`Oracle target missing: ${table}`);
    if (!drop.includes(seq) || !post.includes(seq)) fail(`Oracle sequence missing: ${seq}`);
  }
  if (provider.includes('SCHEDULE_TYPES') || provider.includes('SCHEDULE_ROLES') || provider.includes('EXCEPTION_TYPES')) fail('orphan ORG working-hours vocabulary');
  if (!menuHtml.includes('routerLink="/calendar2/reference-data"')) fail('CAL2 navigation missing');
  if (!/DEFINE APPLY_CHANGES = NO/.test(drop)) fail('DDL not opt-in');
  for (const token of ["IF v_mode='NO' THEN", "IF v_bad>0 THEN", "SELECT COUNT(*) FROM ORG.", "EXTERNAL_INBOUND_FK", "DEPENDENT_PROGRAM_OR_VIEW", "DEPENDENT_SYNONYMS", "TARGET_TABLE_TRIGGERS", "EXTERNAL_SEQUENCE_DEPENDENCIES", "CAL2_CANONICAL_TABLE_COUNT", "DROP TABLE ORG.", "DROP SEQUENCE ORG."]) {
    if (!drop.includes(token)) fail(`missing Oracle drop guard: ${token}`);
  }
  if (/\bPURGE\b|\bCASCADE\s+CONSTRAINTS\b/i.test(drop.replace(/^--[^\n]*$/gm,''))) fail('unsafe Oracle DROP mode');
  if (/\bDROP\s+TABLE\b|\bDELETE\s+FROM\b|\bINSERT\s+INTO\b|\bTRUNCATE\s+TABLE\b|\bUPDATE\s+[A-Z_]+\b/i.test(preview.replace(/^--[^\n]*$/gm,''))) fail('preview must be read-only');
  if (!preview.includes("EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ORG.'")) fail('exact COUNT probe missing');
  if (!post.includes('ORG_CAL2_POST_VERIFY_PASS')) fail('post-verify marker missing');
  for (const canonical of ['BUSINESS_CALENDAR','BUSINESS_CALENDAR_SCHEDULE','BUSINESS_CALENDAR_SCHEDULE_DAY','BUSINESS_CALENDAR_EXCEPTION','BUSINESS_CALENDAR_DAY']) {
    if (!cal2.includes(`"${canonical}"`)) fail(`CAL2 catalog missing: ${canonical}`);
  }
  for (const preserved of ['"ORGANIZATIONS"','"ORGANIZATION_UNITS"','"SERVICE_POINTS"','"SERVICE_CAPABILITIES"','"SELF_SERVICE_TERMINALS"']) {
    if (!provider.includes(preserved)) fail(`ORG master accidentally removed: ${preserved}`);
  }
  const resources = [...provider.matchAll(/return\s+descriptor\(\s*"([^"]+)"[\s\S]*?schemaName,\s*"([A-Z0-9_]+)"/g)];
  if (resources.length !== 27) fail(`expected 27 active ORG descriptors, got ${resources.length}`);
  const groupBlock = menu.split('readonly groups:')[1]?.split('readonly gradeTools')[0] ?? '';
  if ([...groupBlock.matchAll(/title: '[^']+', description:/g)].length !== 6) fail('expected six ORG menu groups');
  if (!read('build-production.cmd').includes('verify-org-cal2-consolidation.mjs') || !read('build-production.sh').includes('verify-org-cal2-consolidation.mjs')) fail('not wired into both builds');
  console.log('ORG_CAL2_CONSOLIDATION_STATIC_PASS: 8 retired forms/tables; 27 ORG descriptors; 6 menu groups; 5 CAL2 master tables; guarded Oracle DDL. NO WRITES.');
} catch (e) {
  console.error(e.message);
  process.exitCode=1;
}
