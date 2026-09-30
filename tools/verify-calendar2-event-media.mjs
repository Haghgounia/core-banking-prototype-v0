import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const exists = rel => fs.existsSync(path.join(root, rel));

const ddl = read('database/oracle/cal2/01-create-cal2-tables.sql');
const migration = read('database/oracle/cal2/migrations/0.11.0-event-media-history.sql');
const repository = read('backend/src/main/java/com/behsazan/corebanking/calendar2/eventmedia/oracle/Calendar2EventMediaRepository.java');
const service = read('backend/src/main/java/com/behsazan/corebanking/calendar2/eventmedia/application/Calendar2EventMediaService.java');
const controller = read('backend/src/main/java/com/behsazan/corebanking/calendar2/eventmedia/web/Calendar2EventMediaController.java');
const page = read('frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.ts');
const pageHtml = read('frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.html');
const uiService = read('frontend/src/app/features/calendar2-reference/calendar2-reference.service.ts');
const dashboard = read('frontend/src/app/features/dashboard/dashboard.component.ts');
const dashboardHtml = read('frontend/src/app/features/dashboard/dashboard.component.html');

const checks = [
  [ddl.includes('CREATE TABLE CAL2.EVENT_MEDIA') && ddl.includes('IMAGE_CONTENT           BLOB NOT NULL'), 'EVENT_MEDIA BLOB table'],
  [ddl.includes('UQ_CAL2_EVENT_MEDIA_ACTIVE') && ddl.includes("CASE WHEN ACTIVE_FLAG='Y' THEN EVENT_ID END"), 'one active media per event'],
  [exists('database/oracle/cal2/migrations/0.11.0-event-media-history.sql') && migration.includes('Safe to rerun'), 'rerunnable EVENT_MEDIA migration'],
  [migration.includes('SEQ_CAL2_EVENT_MEDIA') && migration.includes('ALL_SEQUENCES'), 'EVENT_MEDIA sequence migration'],
  [repository.includes("DEACTIVATED_AT=SYSTIMESTAMP") && repository.includes("WHERE EVENT_ID=:eventId AND ACTIVE_FLAG='Y'"), 'previous active image deactivation'],
  [repository.includes("D.CANONICAL_DATE=TRUNC(SYSDATE)") && repository.includes('EVENT_OCCURRENCE'), 'today occurrence lookup'],
  [service.includes('MAX_IMAGE_BYTES') && service.includes('image/jpeg') && service.includes('image/png') && service.includes('image/webp'), 'safe image validation'],
  [controller.includes('/api/v1/calendar2/event-media') && controller.includes('/today') && controller.includes('/content'), 'event media APIs'],
  [page.includes('eventMediaHistory') && page.includes('uploadEventMedia') && pageHtml.includes('تاریخچه تصاویر') && pageHtml.includes('ثبت تصویر جدید'), 'admin event image management UI'],
  [uiService.includes('eventMediaHistory') && uiService.includes('todayEventMedia') && uiService.includes('FormData'), 'frontend event-media API client'],
  [dashboard.includes('todayOccasionMedia') && dashboardHtml.includes('مناسبت امروز') && dashboardHtml.includes('occasionImageUrl'), 'dashboard today occasion image']
];

const failed = checks.filter(([ok]) => !ok).map(([, label]) => label);
if (failed.length) throw new Error(`CAL2 event-media verification failed: ${failed.join(', ')}`);
console.log(`CAL2 event-media verification OK: ${checks.length}/${checks.length} checks passed.`);
