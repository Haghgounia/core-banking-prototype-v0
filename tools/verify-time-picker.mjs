import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');

const shared = read('frontend/src/app/shared/ui/time-input.component.ts');
const cal2Model = read('frontend/src/app/features/calendar2-reference/calendar2-reference.models.ts');
const cal2Ts = read('frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.ts');
const cal2Html = read('frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.html');
const cal1Ts = read('frontend/src/app/features/calendar-reference/calendar-reference-page.component.ts');
const cal1Html = read('frontend/src/app/features/calendar-reference/calendar-reference-page.component.html');
const cifTs = read('frontend/src/app/features/cif/party-consents-preferences.component.ts');
const cifHtml = read('frontend/src/app/features/cif/party-consents-preferences.component.html');
const javaModels = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/domain/Calendar2ReferenceModels.java');
const javaRegistry = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/application/Calendar2ReferenceRegistry.java');
const javaService = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/application/Calendar2ReferenceService.java');
const javaRepo = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/oracle/Calendar2ReferenceRepository.java');

const frontendRoot = path.join(root, 'frontend/src/app');
function walk(dir) {
  return fs.readdirSync(dir, {withFileTypes: true}).flatMap(entry => {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) return walk(full);
    return /\.(html|ts)$/.test(entry.name) ? [full] : [];
  });
}
const nativeTimeInputs = walk(frontendRoot).filter(file => /type\s*=\s*["']time["']/.test(fs.readFileSync(file, 'utf8')));

const checks = [
  [shared.includes("MatTimepickerModule") && shared.includes("selector: 'app-time-input'"), 'shared app-time-input uses Angular Material Timepicker'],
  [shared.includes("provideNativeDateAdapter()") && shared.includes("MAT_DATE_LOCALE") && shared.includes("'en-GB'"), 'timepicker has local 24-hour native date adapter configuration'],
  [shared.includes('NG_VALUE_ACCESSOR') && shared.includes('NG_VALIDATORS') && shared.includes('implements ControlValueAccessor, Validator'), 'timepicker integrates with Reactive Forms and validation'],
  [shared.includes("interval: string | number = '5m'") && shared.includes("min = '00:00'") && shared.includes("max = '23:59'"), 'timepicker defaults to 5-minute options over the complete 24-hour range'],
  [shared.includes('toApiValue') && shared.includes("padStart(2, '0')") && shared.includes('asciiDigits'), 'timepicker keeps API value normalized as HH:mm and accepts Persian/Arabic digits when preloaded'],
  [nativeTimeInputs.length === 0, `no browser-native type=time inputs remain (${nativeTimeInputs.length})`],
  [cal2Model.includes("'TIME'"), 'CAL2 frontend metadata has a first-class TIME field type'],
  [javaModels.includes('DATE, TIME, TIMESTAMP'), 'CAL2 backend metadata has a first-class TIME field type'],
  [javaRegistry.includes('FieldType.TIME') && javaRegistry.includes('time("staffStartTime"') && javaRegistry.includes('time("customerCloseTime"'), 'weekly schedule and exception clocks are declared as TIME metadata'],
  [javaRepo.includes('case TEXT, TIME, SELECT') && javaRepo.includes('case BOOLEAN, TEXT, TIME, SELECT'), 'CAL2 repository reads/writes TIME as normalized VARCHAR HH:mm'],
  [javaService.includes('field.type() == FieldType.TIME') && javaService.includes('ساعات حضور کارکنان') && javaService.includes('ساعات خدمت‌رسانی به مشتری'), 'CAL2 service validates TIME syntax and start/end ranges'],
  [cal2Ts.includes('TimeInputComponent') && cal2Html.includes("@case ('TIME')") && cal2Html.includes('<app-time-input'), 'CAL2 generic editor renders TIME through shared picker'],
  [cal1Ts.includes('TimeInputComponent') && cal1Html.includes('<app-time-input'), 'legacy CAL TIME fields use the same shared picker'],
  [cifTs.includes('TimeInputComponent') && (cifHtml.match(/<app-time-input/g) ?? []).length >= 3, 'CIF communication/preference time fields use the same shared picker']
];

let ok = true;
for (const [pass, label] of checks) {
  console.log(`${pass ? '[OK]' : '[FAIL]'} ${label}`);
  ok &&= pass;
}
if (!ok) {
  if (nativeTimeInputs.length) console.error('Native time inputs:', nativeTimeInputs.map(file => path.relative(root, file)).join(', '));
  process.exit(1);
}
console.log(`Shared time-picker guard passed (${checks.length}/${checks.length}).`);
