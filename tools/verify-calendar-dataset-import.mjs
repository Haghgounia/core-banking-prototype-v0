import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const exists = rel => fs.existsSync(path.join(root, rel));

const controller = read('backend/src/main/java/com/behsazan/corebanking/calendar/datasetimport/web/CalendarDatasetImportController.java');
const service = read('backend/src/main/java/com/behsazan/corebanking/calendar/datasetimport/application/CalendarDatasetImportService.java');
const repository = read('backend/src/main/java/com/behsazan/corebanking/calendar/datasetimport/oracle/CalendarDatasetImportRepository.java');
const routes = read('frontend/src/app/app.routes.ts');
const page = read('frontend/src/app/features/calendar-reference/calendar-dataset-import.component.html');
const component = read('frontend/src/app/features/calendar-reference/calendar-dataset-import.component.ts');
const menu = read('frontend/src/app/features/calendar-reference/calendar-reference-menu.component.html');
const config = read('config/application.yml');
const resourceConfig = read('backend/src/main/resources/application.yml');

const checks = [
  [controller.includes('/api/v1/calendar/dataset-import'), 'dataset import API base path'],
  [controller.includes('MULTIPART_FORM_DATA_VALUE'), 'multipart endpoint'],
  [service.includes('@Transactional'), 'single transaction boundary'],
  [service.includes('repository.loadCalendarDays(calendarDayFile)') && service.includes('repository.loadCalendarDates(calendarDateFile)'), 'day then date insert order'],
  [!service.includes('repository.lockDatasetTables()'), 'no table-lock control'],
  [!service.includes('seedReady()') && !service.includes('datasetEmpty()'), 'no seed/empty checks'],
  [!service.includes('dayRows * 3L') && !service.includes('countDays()') && !service.includes('countDates()'), 'no row-count verification'],
  [!service.includes('repository.verify()') && !service.includes('validateVerification'), 'no final dataset validation'],
  [!service.includes('SHA-256') && !service.includes('sha256('), 'no dataset hash validation'],
  [repository.includes('BATCH_SIZE = 1000'), '1000-row JDBC batch'],
  [repository.includes('reader.readLine(); // header is intentionally skipped; no header validation'), 'header skipped without validation'],
  [repository.includes('INSERT INTO ') && repository.includes('CALENDAR_DAY') && repository.includes('CALENDAR_DATE'), 'calendar day/date inserts'],
  [routes.includes("path: 'calendar/reference-data/import'"), 'frontend import route'],
  [menu.includes('Import Dataset چهارصدساله از CSV'), 'calendar menu import card'],
  [page.includes('بدون اعتبارسنجی Dataset') && page.includes('پس از پایان موفق هر دو Insert، تراکنش Commit می‌شود'), 'raw import UI'],
  [!component.includes('refreshStatus()') && !component.includes('seedReady') && !component.includes('datasetEmpty'), 'UI does not count/block before import'],
  [config.includes('max-file-size: 64MB') && config.includes('max-request-size: 96MB'), 'runtime multipart technical limits'],
  [resourceConfig.includes('max-file-size: 64MB') && resourceConfig.includes('max-request-size: 96MB'), 'packaged multipart technical limits'],
  [exists('backend/src/test/java/com/behsazan/corebanking/calendar/datasetimport/application/CalendarDatasetCsvParserTest.java'), 'CSV parser regression test remains available']
];

const failed = checks.filter(([ok]) => !ok).map(([, label]) => label);
if (failed.length) throw new Error(`Calendar raw import verification failed: ${failed.join(', ')}`);
console.log(`Calendar raw import verification OK: ${checks.length} checks, no dataset/count validation in import path; commit occurs after both insert streams complete.`);
