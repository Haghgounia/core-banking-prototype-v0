import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const exists = rel => fs.existsSync(path.join(root, rel));

const controllerPath = 'backend/src/main/java/com/behsazan/corebanking/calendar/monthview/web/CalendarMonthViewController.java';
const servicePath = 'backend/src/main/java/com/behsazan/corebanking/calendar/monthview/application/CalendarMonthViewService.java';
const repositoryPath = 'backend/src/main/java/com/behsazan/corebanking/calendar/monthview/oracle/CalendarMonthViewRepository.java';
const domainPath = 'backend/src/main/java/com/behsazan/corebanking/calendar/monthview/domain/CalendarMonthViewModels.java';
const componentPath = 'frontend/src/app/features/calendar-reference/calendar-month-view.component.ts';
const templatePath = 'frontend/src/app/features/calendar-reference/calendar-month-view.component.html';
const stylePath = 'frontend/src/app/features/calendar-reference/calendar-month-view.component.scss';

for (const file of [controllerPath, servicePath, repositoryPath, domainPath, componentPath, templatePath, stylePath]) {
  if (!exists(file)) throw new Error(`CAL month-view file missing: ${file}`);
}

const controller = read(controllerPath);
const service = read(servicePath);
const repository = read(repositoryPath);
const domain = read(domainPath);
const component = read(componentPath);
const template = read(templatePath);
const styles = read(stylePath);
const uiService = read('frontend/src/app/features/calendar-reference/calendar-reference.service.ts');
const models = read('frontend/src/app/features/calendar-reference/calendar-reference.models.ts');
const routes = read('frontend/src/app/app.routes.ts');
const menu = read('frontend/src/app/features/calendar-reference/calendar-reference-menu.component.html');
const breadcrumb = read('frontend/src/app/shared/ui/app-breadcrumb.component.ts');

const checks = [
  [controller.includes('/api/v1/calendar/month-view') && controller.includes('@RequestParam(defaultValue = "PERSIAN")'), 'CAL month-view API with Persian default'],
  [service.includes('SUPPORTED_CALENDARS') && service.includes('PERSIAN') && service.includes('GREGORIAN') && service.includes('ISLAMIC'), 'three calendar systems'],
  [service.includes('SOLAR_HIJRI_IR') && service.includes('HIJRI_CIVIL') && service.includes('GREGORIAN'), 'public calendar codes map to CAL systems'],
  [service.includes('isoWeekdayNo() == 5') && service.includes('holidayDayCount'), 'Iran Friday/weekend and holiday summary'],
  [repository.includes('CALENDAR_DAY_OCCASION') && repository.includes('OCCASION_OCCURRENCE') && repository.includes('OCCASION_CATEGORY'), 'occasion projection comes from CAL'],
  [repository.includes('BUSINESS_CALENDAR_DAY') && repository.includes('IS_BANK_HOLIDAY'), 'bank holiday projection comes from CAL business calendar'],
  [repository.includes("CALENDAR_SYSTEM_CODE = 'SOLAR_HIJRI_IR'") && repository.includes("CALENDAR_SYSTEM_CODE = 'GREGORIAN'") && repository.includes("CALENDAR_SYSTEM_CODE = 'HIJRI_CIVIL'"), 'cross-calendar date projection'],
  [!repository.includes('CAL2.'), 'CAL month view is isolated from CAL2 at runtime'],
  [domain.includes('MonthViewResponse') && domain.includes('DayView') && domain.includes('EventView'), 'month-view domain contract'],
  [routes.includes("path: 'calendar/month-view'"), 'Angular CAL month-view route'],
  [menu.includes('routerLink="/calendar/month-view"') && menu.includes('مشاهده تقویم ماهانه'), 'CAL menu month-view entry'],
  [uiService.includes("'/api/v1/calendar/month-view'") && uiService.includes('monthView('), 'Angular CAL month-view API client'],
  [models.includes('CalendarMonthViewResponse') && models.includes('CalendarMonthDay') && models.includes('CalendarMonthEvent'), 'Angular CAL month-view models'],
  [component.includes("['شنبه', 'یکشنبه', 'دوشنبه', 'سه‌شنبه', 'چهارشنبه', 'پنجشنبه', 'جمعه']") && component.includes('previousMonth') && component.includes('nextMonth') && component.includes('goToday'), 'Saturday-first navigation'],
  [template.includes('نوع تقویم') && template.includes('فقط تعطیلی‌ها') && template.includes('مناسبت‌ها و رویدادها'), 'calendar controls/event details'],
  [styles.includes('grid-template-columns: repeat(7') && styles.includes('.day-details') && styles.includes('.day-cell.holiday'), 'seven-column calendar styling'],
  [breadcrumb.includes("'/calendar/month-view': 'نمای ماهانه تقویم یک'"), 'global breadcrumb title for CAL month view']
];

const failed = checks.filter(([ok]) => !ok).map(([, label]) => label);
if (failed.length) throw new Error(`CAL month-view verification failed: ${failed.join(', ')}`);
console.log('CAL month-view verification OK: Persian-default monthly calendar, three-calendar switching, CAL occasion/holiday projection and Saturday-first grid are wired.');
