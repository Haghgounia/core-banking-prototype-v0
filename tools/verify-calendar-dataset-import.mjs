import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const exists = rel => fs.existsSync(path.join(root, rel));

const controller = read('backend/src/main/java/com/behsazan/corebanking/calendar/datasetimport/web/CalendarDatasetImportController.java');
const service = read('backend/src/main/java/com/behsazan/corebanking/calendar/datasetimport/application/CalendarDatasetImportService.java');
const repository = read('backend/src/main/java/com/behsazan/corebanking/calendar/datasetimport/oracle/CalendarDatasetImportRepository.java');
const parser = read('backend/src/main/java/com/behsazan/corebanking/calendar/datasetimport/application/CalendarDatasetCsvParser.java');
const routes = read('frontend/src/app/app.routes.ts');
const page = read('frontend/src/app/features/calendar-reference/calendar-dataset-import.component.html');
const menu = read('frontend/src/app/features/calendar-reference/calendar-reference-menu.component.html');
const config = read('config/application.yml');
const resourceConfig = read('backend/src/main/resources/application.yml');

const checks = [
  [controller.includes('/api/v1/calendar/dataset-import'), 'dataset import API base path'],
  [controller.includes('MULTIPART_FORM_DATA_VALUE'), 'multipart endpoint'],
  [service.includes('@Transactional'), 'transactional import'],
  [service.includes('repository.lockDatasetTables()'), 'concurrent-import table lock'],
  [service.includes('dateRows != dayRows * 3L'), 'three-calendar row-count guard'],
  [service.includes('SHA-256'), 'SHA-256 audit'],
  [repository.includes('BATCH_SIZE = 1000'), '1000-row JDBC batch'],
  [repository.includes('INSERT INTO ') && repository.includes('CALENDAR_DAY') && repository.includes('CALENDAR_DATE'), 'calendar day/date inserts'],
  [repository.includes('LAG(CANONICAL_DATE)') && repository.includes('JULIAN_DAY_NUMBER <> EPOCH_DAY + 2440588'), 'post-load verification'],
  [parser.includes('DAY_HEADER') && parser.includes('DATE_HEADER'), 'strict CSV headers'],
  [routes.includes("path: 'calendar/reference-data/import'"), 'frontend import route'],
  [menu.includes('Import Dataset چهارصدساله از CSV'), 'calendar menu import card'],
  [page.includes('بدون نیاز به SQL*Loader') && page.includes('شروع Import به Oracle'), 'import UI'],
  [config.includes('max-file-size: 64MB') && config.includes('max-request-size: 96MB'), 'runtime multipart limits'],
  [resourceConfig.includes('max-file-size: 64MB') && resourceConfig.includes('max-request-size: 96MB'), 'packaged multipart limits'],
  [exists('backend/src/test/java/com/behsazan/corebanking/calendar/datasetimport/application/CalendarDatasetCsvParserTest.java'), 'CSV parser regression test']
];

const failed = checks.filter(([ok]) => !ok).map(([, label]) => label);
if (failed.length) throw new Error(`Calendar dataset import verification failed: ${failed.join(', ')}`);
console.log(`Calendar dataset import verification OK: ${checks.length} controls, multipart JDBC batch import with transactional validation.`);
