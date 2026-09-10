import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');

const ddl = read('database/oracle/cal2/01-create-cal2-tables.sql');
const migration = read('database/oracle/cal2/migrations/0.3.85-fix93-business-calendar-schedule-policy.sql');
const registry = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/application/Calendar2ReferenceRegistry.java');
const referenceService = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/application/Calendar2ReferenceService.java');
const referenceRepo = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/oracle/Calendar2ReferenceRepository.java');
const resolverController = read('backend/src/main/java/com/behsazan/corebanking/calendar2/businesscalendar/web/Calendar2BusinessCalendarController.java');
const resolverService = read('backend/src/main/java/com/behsazan/corebanking/calendar2/businesscalendar/application/Calendar2BusinessCalendarService.java');
const resolverRepo = read('backend/src/main/java/com/behsazan/corebanking/calendar2/businesscalendar/oracle/Calendar2BusinessCalendarRepository.java');
const uiService = read('frontend/src/app/features/calendar2-reference/calendar2-reference.service.ts');
const uiTs = read('frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.ts');
const uiHtml = read('frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.html');

const persianFormatter = new Intl.DateTimeFormat('fa-IR-u-ca-persian-nu-latn', {calendar: 'persian', year: 'numeric', month: '2-digit', day: '2-digit', timeZone: 'UTC'});
const samplePersianDate = persianFormatter.format(new Date(Date.UTC(2027, 2, 1, 12)));

const checks = [
  [ddl.includes('CREATE TABLE CAL2.BUSINESS_CALENDAR_SCHEDULE (') && ddl.includes('CREATE TABLE CAL2.BUSINESS_CALENDAR_SCHEDULE_DAY (') && ddl.includes('CREATE TABLE CAL2.BUSINESS_CALENDAR_EXCEPTION ('), 'baseline has range schedule, weekly pattern and single-day exception tables'],
  [ddl.includes('STAFF_START_TIME         TIMESTAMP(6)') && ddl.includes('RESOLUTION_SOURCE') && ddl.includes('SCHEDULE_ID') && ddl.includes('EXCEPTION_ID'), 'resolved BUSINESS_CALENDAR_DAY carries staff hours and provenance'],
  [migration.includes("Safe to rerun") && migration.includes("ALL_TABLES") && migration.includes("ALL_TAB_COLUMNS") && migration.includes("IX_CAL2_BCD_SCHEDULE") && migration.includes("IX_CAL2_BCD_EXCEPTION"), 'FIX93 migration is rerunnable and creates provenance indexes'],
  [registry.includes('"business-calendar-schedules"') && registry.includes('"business-calendar-schedule-days"') && registry.includes('"business-calendar-exceptions"') && registry.includes('true, true, "DRAFT"'), 'CAL2 metadata exposes all three policy forms and new schedules start as DRAFT'],
  [registry.includes('"BUSINESS_CALENDAR_DAY", false, false, false, false'), 'resolved day table is read-only in the reference UI'],
  [referenceService.includes('initializeBusinessCalendarScheduleIfNeeded') && referenceService.includes('validateBusinessCalendarScheduleActivationIfNeeded') && referenceRepo.includes('initializeBusinessCalendarScheduleDays') && referenceRepo.includes('countIncompleteBusinessCalendarScheduleDays') && referenceRepo.includes("W.IR_DISPLAY_ORDER = 7 THEN 'CLOSED'"), 'new schedule gets seven weekly rows, Friday defaults closed, and activation requires a complete pattern'],
  [referenceService.includes('validateClock') && referenceService.includes('clockRange') && referenceService.includes('normalizeBusinessCalendarPolicy'), 'weekly/exception hours are validated and CLOSED normalizes operational flags'],
  [resolverController.includes('@PostMapping("/rebuild")') && resolverController.includes('businessCalendarId'), 'business-calendar rebuild REST API exists'],
  [resolverService.includes('@Transactional') && resolverService.includes('plusYears(5)') && resolverService.includes('incompleteActiveScheduleDays') && resolverService.includes('resolutionCounts'), 'resolver is transactional, range-bounded, blocks incomplete active weekly schedules and reports resolution counts'],
  [resolverRepo.includes('NUMTODSINTERVAL') && resolverRepo.includes('MERGE INTO %s T') && resolverRepo.includes("WHEN X.EXCEPTION_ID IS NOT NULL") && resolverRepo.includes("WHEN X.HOLIDAY_DAY_ID IS NOT NULL") && resolverRepo.includes("WHEN X.SCHEDULE_ID IS NOT NULL"), 'resolver materializes NLS-independent times and exception > holiday > schedule > default precedence'],
  [resolverRepo.includes("ROW_NUMBER() OVER") && resolverRepo.includes('S.PRIORITY_NO DESC') && resolverRepo.includes('S.EFFECTIVE_FROM DESC'), 'overlapping active schedules are deterministic by priority and effective date'],
  [uiService.includes('rebuildBusinessCalendar') && uiTs.includes('rebuildBusinessCalendar()') && uiHtml.includes('بازسازی خروجی روزهای تقویم کاری'), 'frontend exposes rebuild workflow'],
  [uiHtml.includes('برنامه بازه‌ای، Source of Truth ساعات کاری است') && uiHtml.includes('استثنا فقط همان روز را Override می‌کند') && uiHtml.includes('ویرایش مستقیم روزبه‌روز مجاز نیست'), 'UI communicates policy/source-of-truth rules'],
  [uiHtml.includes('<app-persian-date-input') && uiTs.includes("if (field.type === 'DATE') return this.persianDateCell(value)") && uiTs.includes("fa-IR-u-ca-persian-nu-latn"), 'schedule/exception DATE editors and generic grids display Solar Hijri while preserving ISO API values'],
  [samplePersianDate === '1405/12/10', `Gregorian 2027-03-01 resolves to Solar Hijri ${samplePersianDate}`]
];

let ok = true;
for (const [pass, label] of checks) {
  console.log(`${pass ? '[OK]' : '[FAIL]'} ${label}`);
  ok &&= pass;
}
if (!ok) process.exit(1);
console.log(`CAL2 business-calendar schedule/exception guard passed (${checks.length}/${checks.length}).`);
