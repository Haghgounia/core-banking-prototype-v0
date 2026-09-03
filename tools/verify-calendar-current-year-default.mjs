import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = (...parts) => fs.readFileSync(path.join(root, ...parts), 'utf8');
const pageTs = read('frontend','src','app','features','calendar-reference','calendar-reference-page.component.ts');
const pageHtml = read('frontend','src','app','features','calendar-reference','calendar-reference-page.component.html');
const serviceTs = read('frontend','src','app','features','calendar-reference','calendar-reference.service.ts');
const controller = read('backend','src','main','java','com','behsazan','corebanking','calendar','reference','web','CalendarReferenceController.java');
const service = read('backend','src','main','java','com','behsazan','corebanking','calendar','reference','application','CalendarReferenceService.java');
const repo = read('backend','src','main','java','com','behsazan','corebanking','calendar','reference','oracle','CalendarReferenceRepository.java');

const requiredResources = [
  'calendar-days', 'calendar-dates', 'business-calendar-days', 'calendar-exceptions',
  'occasion-occurrences', 'calendar-day-occasions', 'hijri-date-overrides'
];
for (const resource of requiredResources) {
  if (!pageTs.includes(`'${resource}'`)) throw new Error(`Missing current-year default resource: ${resource}`);
}
const checks = [
  [pageTs.includes('solarYearControl'), 'frontend solarYearControl'],
  [pageTs.includes('context.currentYear'), 'current Solar Hijri year is applied by default'],
  [pageTs.includes('showAllSolarYears'), 'all-years escape remains available'],
  [pageHtml.includes('سال هجری شمسی'), 'year filter is visible'],
  [pageHtml.includes('سال جاری'), 'current-year shortcut is visible'],
  [pageHtml.includes('همه سال‌ها'), 'all-years shortcut is visible'],
  [serviceTs.includes("solarYearContext()"), 'frontend context endpoint'],
  [serviceTs.includes("params.set('solarYear'"), 'frontend sends solarYear'],
  [controller.includes('@GetMapping("/solar-year-context")'), 'backend context endpoint'],
  [controller.includes('@RequestParam(required = false) Integer solarYear'), 'backend year parameter'],
  [service.includes('repository.searchCalendarDays(text, solarYear'), 'service forwards year to calendar days'],
  [service.includes('repository.searchOccasionOccurrences(text, solarYear'), 'service forwards year to occurrences'],
  [repo.includes("P.CALENDAR_SYSTEM_CODE='SOLAR_HIJRI_IR'"), 'Solar Hijri dataset is authoritative'],
  [repo.includes('P.YEAR_NO=:solarYear'), 'custom high-volume forms filter server-side'],
  [repo.includes("SY.YEAR_NO=:solarYear"), 'generic DAY_ID forms filter server-side']
];
for (const [ok, label] of checks) if (!ok) throw new Error(`Calendar current-year verification failed: ${label}`);
console.log(`CAL current Solar Hijri year default verification OK: ${requiredResources.length} date-bound forms.`);
