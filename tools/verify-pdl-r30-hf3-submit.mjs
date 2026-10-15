/** R30-HF3: non-writing regression for the current-version confirmation form. */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const dir = 'frontend/src/app/features/product-builder/';
const ui = read(dir + 'product-workspace.component.html');
const ts = read(dir + 'product-workspace.component.ts');
const api = read(dir + 'product-builder.service.ts');
const backend = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderService.java');
const cmd = read('build-production.cmd');
const sh = read('build-production.sh');
let passed = 0;
let failed = 0;
function assert(name, result) {
  const ok = Boolean(result);
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${name}`);
  if (ok) passed++; else failed++;
}
const match = ui.match(/<form\b[^>]*class="current-version-confirm"[^>]*>[\s\S]*?<\/form>/);
const form = match?.[0] ?? '';
const actionBegin = ts.indexOf('async makeApprovedVersionCurrent(): Promise<void>');
const actionEnd = ts.indexOf('sourceVersionOptions():', actionBegin);
const action = ts.slice(actionBegin, actionEnd);
assert('Current-version confirmation form exists', !!match);
assert('Reactive FormGroupDirective is attached to the form', /<form\b[^>]*\[formGroup\]="currentVersionForm"/.test(form));
assert('Submitting through Angular ngSubmit', /\(ngSubmit\)="makeApprovedVersionCurrent\(\)"/.test(form));
assert('Reason text area registered to form group', /<textarea\b[^>]*formControlName="reason"/.test(form));
assert('Matching FormGroup owns existing validated reason control', /currentVersionForm = new FormGroup\(\{reason: this\.currentVersionReason\}\)/.test(ts));
assert('Form retains reason required and length validators', ts.includes('Validators.required, Validators.minLength(5), Validators.maxLength(500)'));
assert('Native unbound ngSubmit cannot recur', !/<form\b[^>]*class="current-version-confirm"(?![^>]*\[formGroup\])[^>]*\(ngSubmit\)=/.test(ui));
assert('Submit button explicitly has submit type', /<button\b[^>]*type="submit"[^>]*\[disabled\]="saving\(\) \|\| currentVersionReason.invalid"/.test(form));
assert('No form action or browser navigation is configured', !/<form\b[^>]*\baction=/.test(form) && !/location\.(assign|reload|replace)/.test(action));
assert('Angular component invokes dedicated HTTP action', action.includes('this.service.makeApprovedVersionCurrent(') && api.includes('/make-current'));
assert('Backend keeps transaction for make-current', backend.includes('@Transactional\n    public Map<String, Object> makeApprovedVersionCurrent('));
assert('After API success page refreshes workspace data', action.includes('await this.load();'));
assert('Success requires a verified persisted IS_CURRENT flag', action.includes("Number(v['IS_CURRENT']) === 1") && action.includes('if (!persisted)'));
assert('Action stays at version step', action.includes('this.currentStep.set(2)'));
assert('Form remains open when API rejects request', action.indexOf('this.currentVersionActionOpen.set(false);') < action.indexOf('} catch (e)'));
assert('Errors are shown adjacent to confirmation controls', form.includes('currentVersionError()') && form.includes('role="alert"') && action.includes('this.currentVersionError.set(detail)'));
assert('No generic IS_CURRENT write from frontend', !action.includes("this.service.update('PRODUCT_VERSION'"));
assert('No new Oracle migration for HF3', !fs.existsSync(path.join(root, 'database/oracle/pdl/migrations/0.11.0-r30-hf3-submit.sql')));
assert('Windows build invokes HF3 regression', cmd.includes('verify-pdl-r30-hf3-submit.mjs'));
assert('Linux build invokes HF3 regression', sh.includes('verify-pdl-r30-hf3-submit.mjs'));
let tsCompiler;
for (const manifest of [path.join(root, 'frontend/package.json'), '/opt/nvm/versions/node/v22.16.0/lib/node_modules/typescript/package.json']) {
  try { tsCompiler = createRequire(manifest)('typescript'); break; } catch {}
}
if (tsCompiler) {
  const parsed = tsCompiler.createSourceFile('product-workspace.component.ts', ts, tsCompiler.ScriptTarget.Latest, true, tsCompiler.ScriptKind.TS);
  assert('TypeScript syntax parses without errors', parsed.parseDiagnostics.length === 0);
}
console.log(`PDL_R30_HF3_STATIC_PASS=${passed} FAIL=${failed}`);
if (failed) process.exit(1);
