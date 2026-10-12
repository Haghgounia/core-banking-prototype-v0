import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p => fs.readFileSync(path.join(root,p),'utf8');
const sha = p => crypto.createHash('sha256').update(fs.readFileSync(path.join(root,p))).digest('hex');
const check = (condition, message) => {if (!condition) throw new Error('FEE_UX_R23_VERIFY_FAIL: '+message);};
const src = 'frontend/src/app/features/fee-admin/';
const tableTs = read(src+'fee-admin-table.component.ts');
const tableHtml = read(src+'fee-admin-table.component.html');
const tableCss = read(src+'fee-admin-table.component.scss');
const studio = read(src+'fee-design-studio.component.html');
const feeDdl = read('database/oracle/fee/baseline-1.0/ddl/FEE.FEE_DEFINITION_20260829_142423_665.oracle.sql');

check(tableTs.includes("import {MatAutocompleteModule}"), 'searchable Feature selector must use Angular Material');
check(tableHtml.includes('[matAutocomplete]="featureOptions"') && tableHtml.includes('chooseFeeFeature($event.option.value)'), 'feature selection must use an actual FEE_FEATURE option');
check(tableTs.includes("this.form.controls['FEE_FEATURE_ID']") && tableTs.includes('fk.setValue(option.value)'), 'persist FK ID rather than feature label');
check(tableTs.includes('fk.setValue(null)') && tableTs.includes('fk.markAsDirty()'), 'invalidating the selected FK on free text input is required');
check(tableTs.includes("column.name === 'FEE_FEATURE_ID' && column.foreignKey"), 'feature selector restricted to a real FK');
check(feeDdl.includes('FOREIGN KEY (FEE_FEATURE_ID) REFERENCES FEE.FEE_FEATURE'), 'FEE_DEFINITION must reference FEE_FEATURE');
check(feeDdl.includes('CATEGORY_CODE VARCHAR2') && feeDdl.includes('CLASSIFICATION_CODE VARCHAR2') &&
  !feeDdl.includes('FOREIGN KEY (CATEGORY_CODE)') && !feeDdl.includes('FOREIGN KEY (CLASSIFICATION_CODE)'),
  'category and classification are not FK-backed codes in the baseline model');
check(tableHtml.includes('<datalist [id]="definitionCodeListId(column)"') && tableTs.includes("this.service.rows('FEE_DEFINITION'"),
  'code suggestions must come from existing FEE data rather than hardcoded enumerations');
check(tableTs.includes('!column.foreignKey && !column.options.length') && tableHtml.includes('ورود کد جدید نیز مجاز است'),
  'suggestions must not override a future FK/domain or disallow free text');
check(tableHtml.includes('@if (!editorOnly) {') && tableHtml.includes('class="editor-advanced-toggle"') &&
  !tableHtml.includes('class="editor card">\n        <header>'), 'remove duplicate business editor header and move technical actions');
check(studio.includes('تعریف کارمزد جدید') && studio.includes('ویرایش تعریف کارمزد') && studio.includes('ثبت نسخه جدید کارمزد'),
  'business title must reflect table and action');
check(tableHtml.includes('requestCloseEditor()') && tableHtml.includes('role="alert"') &&
  tableTs.includes('this.form.markAllAsTouched()') && tableTs.includes('row !== null || value !== null'),
  'required validation on database-default fields and cancel guards must remain');
check(tableHtml.includes('@if (!editorOnly) {') && tableCss.includes('.editor-only .editor-actions'), 'compact layout isolation');
const patch=JSON.parse(read('tools/fee-ux-r23/manifest.json'));
const r3bPath = path.join(root,'tools/fee-r3b/manifest.json');
const r3bManifest = fs.existsSync(r3bPath) ? JSON.parse(fs.readFileSync(r3bPath,'utf8')) : null;
const r3bChanges = new Map((r3bManifest?.files ?? []).map(f => [f.path, f]));
const r3b1Path = path.join(root,'tools/fee-r3b1/manifest.json');
const r3b1Manifest = fs.existsSync(r3b1Path) ? JSON.parse(fs.readFileSync(r3b1Path,'utf8')) : null;
const r3b1Changes = new Map((r3b1Manifest?.files ?? []).map(f => [f.path,f]));

check(patch.files.length === 5, 'exactly four UI files plus one verifier must be targeted');
for (const entry of patch.files) {
  check(entry.path.startsWith(src) || entry.path==='tools/verify-fee-ux-r22.mjs', 'forbidden module target '+entry.path);
  const overlay = r3bChanges.get(entry.path);
  const allowedOverlay = overlay?.before === entry.updatedSha256 && sha(entry.path) === overlay.after &&
    sha('tools/fee-r3b/updated/'+overlay.asset) === overlay.after;
  const newer = r3b1Changes.get(entry.path);
  const allowedR3b1 = overlay?.before === entry.updatedSha256 && newer?.before === overlay.after &&
    sha(entry.path) === newer.after &&
    sha('tools/fee-r3b/updated/'+overlay.asset) === overlay.after &&
    sha('tools/fee-r3b1/base/'+newer.asset) === newer.before &&
    sha('tools/fee-r3b1/updated/'+newer.asset) === newer.after;
  // Some R2.3 HTML files are changed for the first time by R3-B.1; they do not
  // pass through R3-B's TS/backend overlay. Verify that direct signed chain too.
  const directR3b1 = newer?.before === entry.updatedSha256 &&
    sha(entry.path) === newer.after &&
    sha('tools/fee-r3b1/base/'+newer.asset) === newer.before &&
    sha('tools/fee-r3b1/updated/'+newer.asset) === newer.after;
  check(sha(entry.path) === entry.updatedSha256 || allowedOverlay || allowedR3b1 || directR3b1, 'source SHA mismatch '+entry.path);
  check(sha('tools/fee-ux-r23/updated/'+entry.filename) === entry.updatedSha256, 'tampered package '+entry.path);
  check(sha('tools/fee-ux-r23/base/'+entry.filename) === entry.baseSha256, 'tampered rollback payload '+entry.path);
}
check(!tableTs.includes('/api/v1/fee2') && !tableTs.includes('/api/v1/productbuilder'), 'FEE/FEE2/PDL independence');
console.log('FEE_UX_R23_STATIC_PASS: searchable FEE_FEATURE FK, non-binding database-derived code suggestions, single business header, explicit required messages, FEE-only five-target hash-checked patch.');
console.log('NOTE: Static verification only; run build-production.cmd and catalog browser/Oracle UAT.');
