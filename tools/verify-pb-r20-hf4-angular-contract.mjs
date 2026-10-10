import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read=(name)=>fs.readFileSync(path.join(root,name),'utf8');
const model=read('frontend/src/app/features/product-builder/product-builder.models.ts');
const html=read('frontend/src/app/features/product-builder/product-workspace.component.html');
const backend=read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/RuleGovernanceLiteService.java');
const checks=[
  ['API report union includes module-conditional state', /readonly\s+presencePolicy\s*:\s*'REQUIRED'\s*\|\s*'WHEN_ENABLED'\s*\|\s*'OPTIONAL'/.test(model)],
  ['policy edit union keeps module-conditional state', /presence\s*:\s*'REQUIRED'\s*\|\s*'WHEN_ENABLED'\s*\|\s*'OPTIONAL'/.test(model)],
  ['governance matrix recognizes module-conditional state', /rule\.presencePolicy\s*===\s*'WHEN_ENABLED'/.test(html)],
  ['backend accepts module-conditional state', /PRESENCES\s*=\s*Set\.of\(\s*"REQUIRED",\s*"WHEN_ENABLED",\s*"OPTIONAL"\s*\)/.test(backend)]
];
let failures=0;
for(const [label,ok] of checks){console.log(`${ok?'PASS':'FAIL'} | ${label}`);if(!ok) failures++;}
console.log(`PB_R20_HF4_STATIC_PASS=${checks.length-failures} FAIL=${failures}`);
if(failures)process.exitCode=1;
