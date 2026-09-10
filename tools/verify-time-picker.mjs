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
  [shared.includes("selector: 'app-time-input'") && shared.includes('MatMenuModule') && shared.includes('class="clock-face"'), 'shared app-time-input renders a custom overlay clock dial'],
  [!shared.includes('MatTimepickerModule') && !shared.includes('<mat-timepicker'), 'legacy list/native Material timepicker implementation is removed'],
  [shared.includes("phase = signal<'hour' | 'minute'>") && shared.includes("this.phase.set('minute')"), 'clock picker has separate hour and minute selection phases'],
  [shared.includes('innerValues = [13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 0]') && shared.includes('Array.from({length: 12}'), 'clock dial provides a full 24-hour outer/inner ring'],
  [shared.includes("interval: string | number = '5m'") && shared.includes('intervalMinutes()') && shared.includes('minute < 60; minute += step'), 'minute dial honors configurable interval with a 5-minute default'],
  [shared.includes("min = '00:00'") && shared.includes("max = '23:59'") && shared.includes('draftOutOfRange()'), 'picker enforces the complete default 24-hour range and disables invalid confirmation'],
  [shared.includes('NG_VALUE_ACCESSOR') && shared.includes('NG_VALIDATORS') && shared.includes('implements ControlValueAccessor, Validator'), 'clock picker integrates with Reactive Forms and validation'],
  [shared.includes("padStart(2, '0')") && shared.includes('asciiDigits') && shared.includes('normalizeApiValue'), 'picker keeps API values normalized as HH:mm and accepts Persian/Arabic digits'],
  [shared.includes('var(--app-primary)') && shared.includes('var(--app-surface)') && shared.includes('var(--app-text)'), 'clock UI uses application theme tokens for light/dark compatibility'],
  [shared.includes('role="dialog"') && shared.includes('aria-label="باز کردن ساعت گرافیکی"') && shared.includes('aria-label]="dialAriaLabel'), 'clock interaction has dialog/button accessibility labels'],
  [nativeTimeInputs.length === 0, `no browser-native type=time inputs remain (${nativeTimeInputs.length})`],
  [cal2Model.includes("'TIME'"), 'CAL2 frontend metadata has a first-class TIME field type'],
  [javaModels.includes('DATE, TIME, TIMESTAMP'), 'CAL2 backend metadata has a first-class TIME field type'],
  [javaRegistry.includes('FieldType.TIME') && javaRegistry.includes('time("staffStartTime"') && javaRegistry.includes('time("customerCloseTime"'), 'weekly schedule and exception clocks are declared as TIME metadata'],
  [javaRepo.includes('case TEXT, TIME, SELECT') && javaRepo.includes('case BOOLEAN, TEXT, TIME, SELECT'), 'CAL2 repository reads/writes TIME as normalized VARCHAR HH:mm'],
  [javaService.includes('field.type() == FieldType.TIME') && javaService.includes('ساعات حضور کارکنان') && javaService.includes('ساعات خدمت‌رسانی به مشتری'), 'CAL2 service validates TIME syntax and start/end ranges'],
  [cal2Ts.includes('TimeInputComponent') && cal2Html.includes("@case ('TIME')") && cal2Html.includes('<app-time-input'), 'CAL2 generic editor renders TIME through shared clock picker'],
  [cal1Ts.includes('TimeInputComponent') && cal1Html.includes('<app-time-input'), 'legacy CAL TIME fields use the same shared clock picker'],
  [cifTs.includes('TimeInputComponent') && (cifHtml.match(/<app-time-input/g) ?? []).length >= 3, 'CIF communication/preference time fields use the same shared clock picker']
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
console.log(`Shared clock-style time-picker guard passed (${checks.length}/${checks.length}).`);
