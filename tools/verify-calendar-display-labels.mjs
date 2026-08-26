import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const files = [
  'frontend/src/app/features/reference-hub/reference-hub.component.html',
  'frontend/src/app/features/calendar-reference/calendar-reference-menu.component.html',
  'frontend/src/app/features/calendar-reference/calendar-reference-page.component.html',
  'frontend/src/app/features/calendar-reference/calendar-dataset-import.component.html',
  'frontend/src/app/features/calendar2-reference/calendar2-reference-menu.component.html',
  'frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.html',
  'frontend/src/app/features/calendar2-reference/calendar2-dataset-import.component.html',
  'frontend/src/app/features/calendar2-reference/calendar2-reference-menu.component.ts',
  'frontend/src/app/features/calendar2-reference/calendar2-dataset-import.component.ts',
  'frontend/src/app/features/system-specification/system-specification.data.ts',
  'backend/src/main/java/com/behsazan/corebanking/calendar/reference/application/CalendarReferenceRegistry.java',
  'backend/src/main/java/com/behsazan/corebanking/calendar2/reference/application/Calendar2ReferenceRegistry.java',
  'backend/src/main/java/com/behsazan/corebanking/system/modelcomparison/ConfiguredDatabaseSchemas.java'
];

const joined = files.map(rel => fs.readFileSync(path.join(root, rel), 'utf8')).join('\n');
const hub = fs.readFileSync(path.join(root, 'frontend/src/app/features/reference-hub/reference-hub.component.html'), 'utf8');
const calMenu = fs.readFileSync(path.join(root, 'frontend/src/app/features/calendar-reference/calendar-reference-menu.component.html'), 'utf8');
const cal2Menu = fs.readFileSync(path.join(root, 'frontend/src/app/features/calendar2-reference/calendar2-reference-menu.component.html'), 'utf8');

const forbidden = ['چهارصدساله', 'تقویم سازمانی', 'تقویم BIAN', 'BIAN Calendar 400Y', 'Import بسته 400Y'];
const found = forbidden.filter(value => joined.includes(value));
if (found.length) throw new Error(`Legacy calendar display labels remain: ${found.join(', ')}`);
if (!hub.includes('<h3>تقویم یک</h3>') || !hub.includes('<h3>تقویم دو</h3>')) {
  throw new Error('Reference Data hub must expose exact labels «تقویم یک» and «تقویم دو».');
}
if (!calMenu.includes('<h1 class="page-title">تقویم یک</h1>')) throw new Error('CAL menu title is not «تقویم یک».');
if (!cal2Menu.includes('<h1 class="page-title">تقویم دو</h1>')) throw new Error('CAL2 menu title is not «تقویم دو».');
console.log('Calendar display-label verification OK: CAL=«تقویم یک», CAL2=«تقویم دو», no four-hundred-year title remains in live forms.');
