import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const exists = rel => fs.existsSync(path.join(root, rel));

const registry = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/application/Calendar2ReferenceRegistry.java');
const referenceService = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/application/Calendar2ReferenceService.java');
const controller = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/web/Calendar2ReferenceController.java');
const recurrenceController = read('backend/src/main/java/com/behsazan/corebanking/calendar2/eventrecurrence/web/Calendar2EventRecurrenceController.java');
const recurrenceService = read('backend/src/main/java/com/behsazan/corebanking/calendar2/eventrecurrence/application/Calendar2EventRecurrenceService.java');
const recurrenceRepository = read('backend/src/main/java/com/behsazan/corebanking/calendar2/eventrecurrence/oracle/Calendar2EventRecurrenceRepository.java');
const importController = read('backend/src/main/java/com/behsazan/corebanking/calendar2/datasetimport/web/Calendar2DatasetImportController.java');
const importService = read('backend/src/main/java/com/behsazan/corebanking/calendar2/datasetimport/application/Calendar2DatasetImportService.java');
const importRepository = read('backend/src/main/java/com/behsazan/corebanking/calendar2/datasetimport/oracle/Calendar2DatasetImportRepository.java');
const routes = read('frontend/src/app/app.routes.ts');
const hub = read('frontend/src/app/features/reference-hub/reference-hub.component.html');
const menu = read('frontend/src/app/features/calendar2-reference/calendar2-reference-menu.component.html');
const page = read('frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.ts');
const pageHtml = read('frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.html');
const uiService = read('frontend/src/app/features/calendar2-reference/calendar2-reference.service.ts');
const config = read('config/application.yml');
const resourceConfig = read('backend/src/main/resources/application.yml');
const schemas = read('backend/src/main/java/com/behsazan/corebanking/system/modelcomparison/ConfiguredDatabaseSchemas.java');
const ddl = read('database/oracle/cal2/01-create-cal2-tables.sql');
const specification = read('frontend/src/app/features/system-specification/system-specification.data.ts');

const expectedTables = [
  'CALENDAR_SYSTEM','SOURCE_AUTHORITY','DATASET_VERSION','CALENDAR_VARIANT','CALENDAR_MONTH','WEEKDAY',
  'CANONICAL_DAY','CALENDAR_DATE','EVENT_TYPE','EVENT','EVENT_RECURRENCE_RULE','EVENT_OCCURRENCE','BUSINESS_CALENDAR',
  'BUSINESS_CALENDAR_DAY','VALIDATION_RUN','VALIDATION_RESULT'
];

const checks = [
  [expectedTables.every(t => registry.includes(`\"${t}\"`)), '16 CAL2 descriptors'],
  [expectedTables.every(t => ddl.includes(`CAL2.${t}`)), '16 CAL2 DDL tables'],
  [controller.includes('/api/v1/calendar2/reference'), 'CAL2 reference API'],
  [registry.includes('event-recurrence-rules') && registry.includes('ANNUAL_FIXED_DATE') && registry.includes('ONE_TIME_DATE'), 'recurrence-rule descriptor'],
  [referenceService.includes('validateEventRecurrenceRule') && referenceService.includes('rebuildRecurrenceRuleIfNeeded'), 'recurrence-rule validation and automatic materialization on save'],
  [recurrenceController.includes('/api/v1/calendar2/event-recurrence') && recurrenceController.includes('/rebuild-all'), 'recurrence generation API'],
  [recurrenceService.includes('@Transactional') && recurrenceService.includes('deleteGenerated') && recurrenceService.includes('insertGenerated'), 'transactional recurrence rebuild'],
  [recurrenceRepository.includes("OCCURRENCE_SOURCE = 'GENERATED'") && recurrenceRepository.includes('NOT EXISTS') && recurrenceRepository.includes('CALENDAR_VARIANT_ID'), 'generated occurrence materialization without overwriting existing occurrence'],
  [ddl.includes('EVENT_RULE_ID') && ddl.includes('OCCURRENCE_SOURCE') && ddl.includes('CK_CAL2_EO_SOURCE'), 'occurrence provenance columns'],
  [exists('database/oracle/cal2/migrations/0.3.45-fix56-event-recurrence-rule.sql'), 'existing-CAL2 migration'],
  [page.includes('rebuildEventOccurrences') && page.includes('normalizedPayload') && pageHtml.includes('بازسازی همه قواعد فعال'), 'automatic rule generation UI'],
  [uiService.includes('/api/v1/calendar2/event-recurrence/rebuild'), 'frontend recurrence API client'],
  [importController.includes('/api/v1/calendar2/dataset') && importController.includes('MULTIPART_FORM_DATA_VALUE'), 'CAL2 ZIP import API'],
  [importService.includes('@Transactional'), 'single CAL2 import transaction'],
  [importService.includes('01_calendar_system.csv') && importService.includes('15_validation_result.csv'), '15-file dataset package contract'],
  [importRepository.includes('BATCH_SIZE = 1000'), '1000-row JDBC batch'],
  [routes.includes("path: 'calendar2/reference-data'") && routes.includes("path: 'calendar2/reference-data/import'") && routes.includes("path: 'calendar2/reference-data/:resource'"), 'CAL2 frontend routes'],
  [hub.includes('تقویم دو'), 'CAL2 Reference Data card'],
  [menu.includes('Schema ‏CAL2') && menu.includes('Import بسته تقویم دو'), 'CAL2 independent menu'],
  [config.includes('calendar2: CAL2') && resourceConfig.includes('calendar2: CAL2'), 'CAL2 schema configuration'],
  [schemas.includes('schemas.calendar2') && schemas.includes('تقویم دو (CAL2)'), 'CAL2 system tools schema option'],
  [exists('database/oracle/cal2/00-create-cal2-schema.sql') && exists('database/oracle/cal2/02-grant-cal2-to-application-user.sql'), 'CAL2 schema/grant scripts'],
  [(ddl.match(/^COMMENT ON COLUMN CAL2\./gm) ?? []).length === 158, '158 CAL2 Persian column comments'],
  [!(/REFERENCES\s+CAL\./i.test(ddl)) && !registry.includes('schemaName = "CAL"') && !controller.includes('/api/v1/calendar/reference'), 'CAL2 runtime/DDL isolated from CAL'],
  [specification.includes('referenceForms: 201') && specification.includes('calendar2ReferenceForms: 16'), 'system specification includes CAL2 counts']
];

const failed = checks.filter(([ok]) => !ok).map(([, label]) => label);
if (failed.length) throw new Error(`CAL2 verification failed: ${failed.join(', ')}`);
console.log(`CAL2 verification OK: ${expectedTables.length} independent tables/forms, recurring-event materialization, ZIP JDBC import, separate CAL2 schema and routes.`);
