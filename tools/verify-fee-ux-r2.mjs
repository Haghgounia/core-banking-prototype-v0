import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const check = (assertion, reason) => {if (!assertion) throw new Error(`FEE UX R2 FAIL: ${reason}`);};
const studio = read('frontend/src/app/features/fee-admin/fee-design-studio.component.ts');
const html = read('frontend/src/app/features/fee-admin/fee-design-studio.component.html');
const editor = read('frontend/src/app/features/fee-admin/fee-admin-table.component.ts');
const editorHtml = read('frontend/src/app/features/fee-admin/fee-admin-table.component.html');
const routes = read('frontend/src/app/app.routes.ts');
const service = read('frontend/src/app/features/fee-admin/fee-admin.service.ts');
const home = read('frontend/src/app/features/fee-admin/fee-admin-home.component.ts');
const workspaceHtml = read('frontend/src/app/features/fee-admin/fee-admin-workspace.component.html');
const ddlDir = path.join(root, 'database/oracle/fee/baseline-1.0/ddl');
const d = new Map(fs.readdirSync(ddlDir).filter(f => /FEE\.FEE_[A-Z0-9_]+_.*\.oracle\.sql$/.test(f))
    .map(f => [f.match(/^FEE\.(FEE_[A-Z0-9_]+)_\d{8}_/)[1], fs.readFileSync(path.join(ddlDir, f),'utf8')]));
const expected = [
  ['FEE_DEFINITION',null],['FEE_DEFINITION_VERSION','FEE_DEFINITION_ID'],
  ['FEE_FEATURE',null],['FEE_PRODUCT_FEATURE','FEE_DEFINITION_VERSION_ID'],
  ['FEE_APPLICABILITY_RULE','FEE_DEFINITION_VERSION_ID'],['FEE_APPLICABILITY_CONDITION','APPLICABILITY_RULE_ID'],
  ['FEE_CALCULATION_RULE','FEE_DEFINITION_VERSION_ID'],['FEE_INPUT_DEFINITION','CALCULATION_RULE_ID'],
  ['FEE_CALCULATION_TIER','CALCULATION_RULE_ID'],['FEE_RULE_COMPONENT','CALCULATION_RULE_ID'],
  ['FEE_CHARGE_COMPONENT','FEE_DEFINITION_VERSION_ID']
];
check(expected.length===11,'expected 11 contextual entities');
for (const [table,fk] of expected) {
  check(d.has(table), `DDL for ${table} missing`);
  check(studio.includes(`table:'${table}'`), `designer does not expose ${table}`);
  if (fk) {
    check(new RegExp(`\\b${fk} NUMBER\\(`).test(d.get(table)), `${table}.${fk} not declared in Oracle baseline`);
    check(studio.includes(`filterColumn:'${fk}'`), `${table}.${fk} context filter missing`);
  }
}
check(studio.includes('const CATALOG_TABS') && studio.includes('const RULE_TABS'), 'two independent studio modes required');
check(studio.includes("this.service.rows('FEE_DEFINITION'") && studio.includes("this.service.rows('FEE_DEFINITION_VERSION'"), 'live Oracle-backed definition/version selectors required');
check(studio.includes("this.asId(row,'FEE_DEFINITION_ID') !== definitionId"), 'foreign-version context integrity required');
check(studio.includes("case 'version'") && studio.includes("case 'calculation'"), 'dependent editor must enforce required selection');
check(html.includes('<app-fee-admin-table') && html.includes('[context]="ctx"') && html.includes('(rowWritten)="rowWritten($event)"'), 'in-place CRUD must reuse FEE table engine');
check(editor.includes('@Input() context: FeeEmbeddedContext') && editor.includes('values[fk.name] ='), 'contextual FK write protection required');
check(editor.includes('hasUnsavedChanges') && studio.includes('canLeave()') && routes.includes('canDeactivate'), 'unsaved edit protection required');
check(editor.includes('canHardDelete(): boolean { return !this.embedded'), 'no hard delete in contextual studio');
check(editorHtml.includes('compact-strategy') && editorHtml.includes('createCalculation($event.value)'), 'strategy quick create missing');
check(routes.includes("path: 'fee/design/catalog'") && routes.includes("path: 'fee/design/rules'"), 'studio routes not registered');
check(routes.includes("path: 'fee/tables/:table'") && routes.includes("path: 'fee/references'") && routes.includes("path: 'fee/simulator'"), 'existing FEE pages removed');
check(routes.includes("path: 'fee2/studio'") && routes.includes("path: 'fee2/tables/:table'"), 'FEE2 routes removed');
check(home.includes("workspace.key === 'catalog'") && home.includes("workspace.key === 'calculation'"), 'home navigation not wired');
check(workspaceHtml.includes('ورود به طراح'), 'legacy workspaces not linked to studio');
check(service.includes('row(table: string, id: number)') && service.includes('/rows/${id}'), 'row deep-link restoration API missing');
check(!/from\s+['"][^'"]*fee2/i.test(studio+editor), 'FEE2 code dependency prohibited');
check(!studio.includes('/api/v1/fee2') && !editor.includes('/api/v1/fee2'), 'FEE2 API forbidden');
check(!studio.includes('service.delete('), 'studio must not delete fee configurations');
console.log('FEE_UX_R2_STATIC_PASS: 11 schema-verified forms (4 catalog, 7 rules), context-filtered CRUD, strict FK scope, guarded unsaved editing, independent FEE2 and legacy routes preserved.');
console.log('NOTE: Static source checks only; Angular compile, Oracle and browser UAT are required before release acceptance.');
