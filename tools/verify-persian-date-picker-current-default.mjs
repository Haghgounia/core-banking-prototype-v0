import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const file = path.join(root, 'frontend', 'src', 'app', 'shared', 'ui', 'persian-date-input.component.ts');
const source = fs.readFileSync(file, 'utf8');
const checks = [
  [source.includes('this.seedTodaySelection();'), 'empty/current selection is seeded from today'],
  [source.includes('[selected]="year === selectedYear()"'), 'year option explicitly tracks selectedYear'],
  [source.includes('[selected]="month.value === selectedMonth()"'), 'month option explicitly tracks selectedMonth'],
  [source.includes('[selected]="day === selectedDay()"'), 'day option explicitly tracks selectedDay'],
  [source.includes("const existing = this.gregorianToPersian(this.value().slice(0, 10));"), 'existing persisted value keeps precedence'],
  [source.includes('this.clampToBounds();'), 'date bounds remain enforced']
];
const failures = checks.filter(([ok]) => !ok).map(([, label]) => label);
if (failures.length) {
  console.error('Persian date-picker default verification FAILED:');
  for (const item of failures) console.error(`- ${item}`);
  process.exit(1);
}
console.log(`Persian date-picker default verification OK: ${checks.length} checks.`);
