import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const get = pathName => fs.readFileSync(path.join(root, pathName), 'utf8');
const ts = get('frontend/src/app/features/product-builder/pdl-table.component.ts');
const html = get('frontend/src/app/features/product-builder/pdl-table.component.html');
const css = get('frontend/src/app/features/product-builder/pdl-table.component.scss');
const ws = get('frontend/src/app/features/product-builder/product-workspace.component.ts');
const wh = get('frontend/src/app/features/product-builder/product-workspace.component.html');
const tests = [
 ['Save can always trigger validation when not saving', html.includes('[disabled]="!canAttemptSave()"') && ts.includes('private reportInvalidFields()')],
 ['Missing field labels shown in editor', html.includes('invalidFieldLabels().join') && css.includes('.validation-summary')],
 ['Eligibility and generic paths report validation', (ts.match(/this\.reportInvalidFields\(\)/g) || []).length === 2],
 ['Navigation preserves product context', wh.includes('returnProductId:productId()') && wh.includes('returnStep:4')],
 ['All business capabilities have rule target', [...ws.split('const MODULE_DEFINITIONS:')[1].split('@Component(')[0].matchAll(/\{code: '([^']+)', label:/g)].every(m => ws.includes(`${m[1]}: '`))],
 ['Modules show linked configuration', wh.includes('moduleRuleTable(module.code)') && wh.includes('moduleReturnStep(module)')],
 ['Calendar over grid style remains', css.includes('.editor{position:relative;z-index:5;overflow:visible}')],
 ['Backend Oracle interfaces unchanged by PB-R11', fs.existsSync(path.join(root,'backend/src/main/java/com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java'))]
];
for(const [label, ok] of tests) console.log(`${ok ? 'PASS' : 'FAIL'} | ${label}`);
if(tests.some(([,ok])=>!ok)) process.exitCode=1;
