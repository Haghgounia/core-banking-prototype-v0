import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const registry = read('backend/src/main/java/com/behsazan/corebanking/calendar/reference/application/CalendarReferenceRegistry.java');
const routes = read('frontend/src/app/app.routes.ts');
const hub = read('frontend/src/app/features/reference-hub/reference-hub.component.html');
const config = read('config/application.yml');
const page = read('frontend/src/app/features/calendar-reference/calendar-reference-page.component.html');

const expectedTables = [
  'CALENDAR_SYSTEM','CALENDAR_ALGORITHM','WEEKDAY','CALENDAR_MONTH','CALENDAR_DAY','CALENDAR_DATE','HIJRI_DATE_OVERRIDE',
  'BUSINESS_CALENDAR','BUSINESS_CALENDAR_DAY','CALENDAR_EXCEPTION','BUSINESS_DAY_CONVENTION',
  'OCCASION_CATEGORY','OCCASION','OCCASION_RULE','OCCASION_OCCURRENCE','CALENDAR_DAY_OCCASION'
];
const missing = expectedTables.filter(table => !registry.includes(`\"${table}\"`));
if (missing.length) throw new Error(`Calendar descriptor missing physical tables: ${missing.join(', ')}`);
if (!registry.includes('calendar-days') || !registry.includes('calendar-dates')) throw new Error('Generated chronology resources missing.');
if (!routes.includes("path: 'calendar/reference-data'")) throw new Error('Calendar reference menu route missing.');
if (!routes.includes("path: 'calendar/reference-data/:resource'")) throw new Error('Calendar reference page route missing.');
if (!hub.includes('اطلاعات پایه تقویم سازمانی')) throw new Error('Calendar domain card missing from Reference Data hub.');
if (!config.includes('calendar: CAL')) throw new Error('CAL schema configuration missing.');
if (!page.includes('Dataset محاسباتی/Canonical')) throw new Error('Read-only chronology protection notice missing.');
console.log(`Calendar reference verification OK: ${expectedTables.length} CAL tables, separate Reference Data menu, protected generated dataset.`);
