import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const repo = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/oracle/Calendar2ReferenceRepository.java');
const svc = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/application/Calendar2ReferenceService.java');
const registry = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/application/Calendar2ReferenceRegistry.java');
const ts = read('frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.ts');
const html = read('frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.html');
const qa = read('docs/CAL2-0.3.75-FIX83-BUSINESS-CALENDAR-DAY-RENDERER-QA.md');

const checks = [
  [svc.includes('searchBusinessCalendarDays'), 'service must route business-calendar-days to enriched search'],
  [repo.includes('PCD.YEAR_NO AS SOLAR_YEAR') && repo.includes('BUSINESS_CALENDAR_NAME') && repo.includes('SOURCE_NAME'), 'repository must project Persian date/calendar/source context'],
  [repo.includes('businessCalendarDaySort') && repo.includes('W.IR_DISPLAY_ORDER'), 'business-calendar-day sorting must support business columns'],
  [registry.includes('option("UNCLASSIFIED", "طبقه‌بندی‌نشده")'), 'UNCLASSIFIED must have Persian label'],
  [registry.includes('option("PENDING_RULE_EVALUATION", "در انتظار اعمال قواعد")'), 'reason code must be Persian select'],
  [ts.includes("businessCalendarDayPage") && ts.includes("descriptor?.tableName === 'BUSINESS_CALENDAR_DAY'") && ts.includes("businessCalendarSolarDate") && ts.includes("businessDayReasonLabel"), 'frontend renderer detection/helper contract missing'],
  [html.includes('تاریخ شمسی') && html.includes('روز هفته') && html.includes('businessCalendarDayDisplayedColumns'), 'business calendar day grid Persian columns missing'],
  [qa.includes('BUSINESS_CALENDAR_DAY') && qa.includes('tableName'), 'QA document incomplete']
];

const failed = checks.filter(([ok]) => !ok).map(([,message]) => message);
if (failed.length) {
  console.error('CAL2 business-calendar-day Persian grid verification FAILED:');
  failed.forEach(message => console.error(`- ${message}`));
  process.exit(1);
}
console.log('CAL2 business-calendar-day Persian grid verification OK.');
