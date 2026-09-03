import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');

const service = read('backend/src/main/java/com/behsazan/corebanking/calendar/reference/application/CalendarReferenceService.java');
const repo = read('backend/src/main/java/com/behsazan/corebanking/calendar/reference/oracle/CalendarReferenceRepository.java');
const registry = read('backend/src/main/java/com/behsazan/corebanking/calendar/reference/application/CalendarReferenceRegistry.java');
const ts = read('frontend/src/app/features/calendar-reference/calendar-reference-page.component.ts');
const html = read('frontend/src/app/features/calendar-reference/calendar-reference-page.component.html');
const css = read('frontend/src/app/features/calendar-reference/calendar-reference-page.component.scss');

const checks = [
  [service.includes('searchCalendarDays') && service.includes('searchBusinessCalendarDays'), 'service routes day/business-day to enriched searches'],
  [service.includes('searchOccasionRules') && service.includes('searchOccasionOccurrences') && service.includes('searchCalendarDayOccasions'), 'service routes occasion grids to enriched searches'],
  [repo.includes("P.CALENDAR_SYSTEM_CODE='SOLAR_HIJRI_IR'") && repo.includes("H.CALENDAR_SYSTEM_CODE='HIJRI_CIVIL'"), 'calendar day projects Solar Hijri and Hijri dates'],
  [repo.includes('BC.CALENDAR_NAME_FA') && repo.includes('W.WEEKDAY_NAME_FA') && repo.includes('IS_BANK_HOLIDAY'), 'business calendar day projects Persian business context'],
  [repo.includes('O.OCCASION_NAME_FA') && repo.includes('C.CATEGORY_NAME_FA') && repo.includes('START_SOLAR_DATE'), 'occasion occurrence projects Persian occasion/date context'],
  [repo.includes('CALENDAR_DAY_OCCASION CDO') && repo.includes('DISPLAY_PRIORITY') && repo.includes('PRIMARY_OCCASION_FLAG'), 'day-occasion enriched projection'],
  [ts.includes('calendarDayPage') && ts.includes('businessCalendarDayPage') && ts.includes('occasionRulePage') && ts.includes('occasionOccurrencePage') && ts.includes('calendarDayOccasionPage'), 'special grid page detection'],
  [ts.includes('occurrenceSolarRange') && ts.includes('statusSourceLabel') && ts.includes('sourceAuthorityLabel'), 'Persian range/source helpers'],
  [html.includes('calendarDayDisplayedColumns') && html.includes('businessCalendarDayDisplayedColumns') && html.includes('occasionOccurrenceDisplayedColumns'), 'special grid renderers'],
  [html.includes('تاریخ شمسی') && html.includes('روز هفته') && html.includes('تعطیل بانکی') && html.includes('دسته مناسبت'), 'Persian business labels in grids'],
  [html.includes("row['occasionName']") && html.includes("row['businessCalendarName']"), 'business names replace raw lookup ids in grids'],
  [css.includes('.primary-cell'), 'primary business cell style'],
  [registry.includes('option("FIXED_DATE", "تاریخ ثابت")') && registry.includes('option("GENERATED", "تولیدشده")'), 'controlled codes keep Persian labels'],
  [registry.includes('قطعی بودن الگوریتم') && registry.includes('روزهای مرجع تقویم') && registry.includes('اصلاح وضعیت روز کاری'), 'legacy English UI labels were replaced with Persian business labels']
];

const failed = checks.filter(([ok]) => !ok).map(([, label]) => label);
if (failed.length) throw new Error(`CAL form usability verification failed: ${failed.join(', ')}`);
console.log('CAL form usability verification OK: canonical days, business days, occasion rules/occurrences and day-occasion grids expose Persian business context.');
