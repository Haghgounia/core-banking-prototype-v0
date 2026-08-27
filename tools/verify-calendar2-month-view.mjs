import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const exists = rel => fs.existsSync(path.join(root, rel));

const controllerPath = 'backend/src/main/java/com/behsazan/corebanking/calendar2/monthview/web/Calendar2MonthViewController.java';
const servicePath = 'backend/src/main/java/com/behsazan/corebanking/calendar2/monthview/application/Calendar2MonthViewService.java';
const repositoryPath = 'backend/src/main/java/com/behsazan/corebanking/calendar2/monthview/oracle/Calendar2MonthViewRepository.java';
const domainPath = 'backend/src/main/java/com/behsazan/corebanking/calendar2/monthview/domain/Calendar2MonthViewModels.java';
const componentPath = 'frontend/src/app/features/calendar2-reference/calendar2-month-view.component.ts';
const templatePath = 'frontend/src/app/features/calendar2-reference/calendar2-month-view.component.html';
const stylePath = 'frontend/src/app/features/calendar2-reference/calendar2-month-view.component.scss';

for (const file of [controllerPath, servicePath, repositoryPath, domainPath, componentPath, templatePath, stylePath]) {
  if (!exists(file)) throw new Error(`CAL2 month-view file missing: ${file}`);
}

const controller = read(controllerPath);
const service = read(servicePath);
const repository = read(repositoryPath);
const domain = read(domainPath);
const component = read(componentPath);
const template = read(templatePath);
const styles = read(stylePath);
const uiService = read('frontend/src/app/features/calendar2-reference/calendar2-reference.service.ts');
const models = read('frontend/src/app/features/calendar2-reference/calendar2-reference.models.ts');
const routes = read('frontend/src/app/app.routes.ts');
const menu = read('frontend/src/app/features/calendar2-reference/calendar2-reference-menu.component.html');

const checks = [
  [controller.includes('/api/v1/calendar2/month-view') && controller.includes('@RequestParam(defaultValue = "PERSIAN")'), 'month-view API with Persian default'],
  [service.includes('SUPPORTED_CALENDARS') && service.includes('PERSIAN') && service.includes('GREGORIAN') && service.includes('ISLAMIC'), 'three supported calendar systems'],
  [service.includes('isoWeekdayNo() == 5') && service.includes('holidayDayCount'), 'Iran Friday/weekend and holiday summary'],
  [repository.includes('EVENT_OCCURRENCE') && repository.includes('EVENT_TYPE') && repository.includes('CALENDAR_DATE') && repository.includes('CANONICAL_DAY'), 'calendar/event read-model joins'],
  [repository.includes('PERSIAN_VARIANT_ID') && repository.includes('GREGORIAN_VARIANT_ID') && repository.includes('ISLAMIC_VARIANT_ID'), 'cross-calendar date projection'],
  [!repository.includes('CAL.') && !controller.includes('/calendar/reference'), 'CAL2 isolation'],
  [domain.includes('MonthViewResponse') && domain.includes('DayView') && domain.includes('EventView'), 'month-view domain contract'],
  [routes.includes("path: 'calendar2/month-view'"), 'Angular month-view route'],
  [menu.includes('routerLink="/calendar2/month-view"') && menu.includes('مشاهده تقویم ماهانه'), 'CAL2 menu entry'],
  [uiService.includes("'/api/v1/calendar2/month-view'") && uiService.includes('monthView('), 'Angular month-view API client'],
  [models.includes('Calendar2MonthViewResponse') && models.includes('Calendar2MonthDay') && models.includes('Calendar2MonthEvent'), 'Angular month-view models'],
  [component.includes("['شنبه', 'یکشنبه', 'دوشنبه', 'سه‌شنبه', 'چهارشنبه', 'پنجشنبه', 'جمعه']") && component.includes('previousMonth') && component.includes('nextMonth') && component.includes('goToday'), 'Saturday-first month navigation'],
  [template.includes('نوع تقویم') && template.includes('فقط تعطیلی‌ها') && template.includes('هجری شمسی') && template.includes('میلادی') && template.includes('هجری قمری'), 'calendar controls and cross-calendar details'],
  [template.includes('مناسبت‌ها و رویدادها') && template.includes('روزهای تعطیل') && template.includes('جمعه‌ها'), 'event/holiday presentation'],
  [styles.includes('grid-template-columns: repeat(7') && styles.includes('.day-details') && styles.includes('.day-cell.holiday'), 'seven-column responsive calendar styling']
];

const failed = checks.filter(([ok]) => !ok).map(([, label]) => label);
if (failed.length) throw new Error(`CAL2 month-view verification failed: ${failed.join(', ')}`);
console.log('CAL2 month-view verification OK: Persian-default monthly calendar, three-calendar switching, event/holiday projection, Saturday-first grid and day details are wired.');
