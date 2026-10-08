import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const ts = read('frontend/src/app/features/product-builder/product-workspace.component.ts');
const html = read('frontend/src/app/features/product-builder/product-workspace.component.html');
const refs = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlReferenceOptionService.java');
const migration = read('database/oracle/dps/migrations/0.11.0-r10u-hf5-default-currency-code-reconciliation.sql');
const win = read('build-production.cmd');
const unix = read('build-production.sh');

const checks = [
  ['backend keeps default currency governed by DPS reference', refs.includes('Map.entry("DEFAULT_CURRENCY_CODE", "dps-default-currencies")')],
  ['workspace loads PRODUCT descriptor for governed currency options', ts.includes("this.service.descriptor('PRODUCT')") && ts.includes("item.name === 'DEFAULT_CURRENCY_CODE'")],
  ['workspace rejects malformed non-ISO currency reference values', ts.includes('/^[A-Z]{3}$/') && ts.includes('داده مرجع ارزهای پیش‌فرض دارای کد کسب‌وکاری معتبر ISO نیست')],
  ['workspace save is blocked when governed currency reference is unavailable', ts.includes('if (!this.currencyReferenceReady())') && ts.includes('فهرست مرجع ارزهای پیش‌فرض معتبر و قابل استفاده نیست')],
  ['workspace currency selector is reference-driven', html.includes('@for (currency of currencyOptions(); track currency.code)') && html.includes('[value]="currencyOptionValue(currency)"')],
  ['hard-coded workspace IRR USD EUR options are removed', !html.includes('<mat-option value="IRR">ریال ایران</mat-option>') && !html.includes('<mat-option value="USD">دلار آمریکا</mat-option>') && !html.includes('<mat-option value="EUR">یورو</mat-option>')],
  ['ProblemDetail detail is surfaced instead of unexpected-error fallback', ts.includes('error instanceof HttpErrorResponse') && ts.includes('problem.detail')],
  ['migration reconciles numeric currency business codes to ISO codes', migration.includes("WHEN '1' THEN 'IRR'") && migration.includes("WHEN '2' THEN 'USD'") && migration.includes("WHEN '3' THEN 'EUR'") && migration.includes("WHEN '4' THEN 'AED'")],
  ['migration preserves surrogate identity and updates business CODE only', migration.includes('UPDATE DPS.REF_DEFAULT_CURRENCY_CODE') && !migration.includes('UPDATE DPS.REF_DEFAULT_CURRENCY_CODE\n     SET DEFAULT_CURRENCY_ID')],
  ['migration validates the four observed Persian currency rows', ['ریال ایران','دلار آمریکا','یورو','درهم امارات متحده عربی'].every(v => migration.includes(v))],
  ['migration reconciles legacy PDL product currency values', migration.includes('UPDATE PDL.PRODUCT') && migration.includes('DEFAULT_CURRENCY_CODE')],
  ['Windows production build runs HF5 currency-reference guard', win.includes('verify-pdl-r10u-hf5-currency-reference.mjs')],
  ['Unix production build runs HF5 currency-reference guard', unix.includes('verify-pdl-r10u-hf5-currency-reference.mjs')]
];

let pass = 0;
for (const [label, ok] of checks) {
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${label}`);
  if (ok) pass++;
}
console.log('------------------------------------------------------------');
console.log(`PDL_R10U_HF5_CURRENCY_REFERENCE_PASS=${pass}`);
console.log(`PDL_R10U_HF5_CURRENCY_REFERENCE_FAIL=${checks.length - pass}`);
if (pass !== checks.length) process.exit(1);
console.log('PDL_R10U_HF5_CURRENCY_REFERENCE_STATIC_PASS');
