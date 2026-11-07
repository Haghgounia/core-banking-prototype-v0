'use strict';
// CLT-R3C: business-facing catalog contract; verify existing read-only APIs and UI.
const fs=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
const root=path.resolve(process.argv[2]||path.resolve(__dirname,'../../..'));
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const source='frontend/src/app/features/collateral/';
const ref=read(source+'clt-reference-workspace.component.ts');
const html=read(source+'clt-reference-workspace.component.html');
const ins=read(source+'collateral-insurance-panel.component.ts');
const insHtml=read(source+'collateral-insurance-panel.component.html');
const nav=read('frontend/src/app/layout/app-shell.component.html');
const backend=read('backend/src/main/java/com/behsazan/corebanking/collateral/insurance/CollateralInsuranceRepository.java');
const repo=read('backend/src/main/java/com/behsazan/corebanking/collateral/reference/CollateralReferenceRepository.java');
const catalog=read('backend/src/main/java/com/behsazan/corebanking/collateral/reference/CollateralReferenceCatalog.java');
const guard=read('backend/src/main/java/com/behsazan/corebanking/collateral/localuat/CltLocalUatSecurityConfiguration.java');
const doc=read('docs/collateral/CLT-R3C-INSURANCE-CATALOG-UAT-FA.md');
let count=0;
function check(n,fn){try{fn();count++;console.log('PASS | '+n);}catch(e){console.error('FAIL | '+n+' | '+e.message);process.exitCode=1;}}
check('R6M modal remains native',()=>{assert.match(html,/<dialog #recordDetailDialog data-clt-dialog/);assert.match(ref,/dialog\.showModal\(\)/);});
check('R6M search and paging preserved',()=>{assert.match(html,/data-clt-run-search/);assert.match(html,/data-clt-clear-search/);assert.match(html,/load\(page\(\)\+1\)/);});
check('insurer catalog is Oracle not technical seed',()=>{assert.match(ref,/readonly insurerCatalog = computed/);assert.match(ref,/mode !== 'oracle'/);assert.match(html,/data-clt-r3c-insurer-source/);});
check('candidate insurer cannot use generic proposal UI',()=>assert.match(ref,/!this\.registryActiveCatalog\(\) && !this\.insurerCatalog\(\)/));
check('temporary codes explicitly non-official',()=>assert.match(html,/TMP-INS-1403/));
check('inactive versus licensing distinguished',()=>assert.match(html,/تأیید اعتبار پروانه/));
check('insurer page requires real Oracle record status',()=>{assert.match(repo,/FROM " \+ source \+ filter/);assert.match(repo,/new Rows\(rows, total == null/);assert.match(catalog,/"INSURANCE_COMPANY"/);});
check('insurer operational select is ACTIVE only',()=>assert.match(backend,/FROM CLT\.INSURANCE_COMPANY WHERE STATUS = 'ACTIVE'/));
check('insurer selector has zero ACTIVE message',()=>assert.match(insHtml,/data-clt-r3c-no-active-insurers/));
check('insurer selector licensing warning',()=>assert.match(insHtml,/data-clt-r3c-license-gate/));
check('insurer catalog linked from insurance panel',()=>{assert.match(insHtml,/routerLink="\/collateral\/reference-data\/INSURANCE_COMPANY"/);assert.match(ins,/imports: \[FormsModule, RouterLink, PersianDateInputComponent\]/);});
check('insurer catalog linked from collateral navigation',()=>assert.match(nav,/data-clt-r3c-menu/));
check('CLT-only secured GET allowed with explicit scope',()=>{assert.match(guard,/\/api\/v1\/collateral\/reference\/\*/);assert.match(guard,/SCOPE_clt\.reference\.read/);});
check('R3C docs mark data and licence gates',()=>{assert.match(doc,/INS-DATA-01/);assert.match(doc,/G-04/);assert.match(doc,/G-09/);});
check('source files contain no DDL or DML migration',()=>{for(const s of [html,insHtml,ref,ins,nav])assert.doesNotMatch(s,/\bINSERT\s+INTO\s+CLT\.|\bUPDATE\s+CLT\.|\bDELETE\s+FROM\s+CLT\./i);});
// Parse updated TypeScript in-process if TS exists locally; Angular production build also verifies templates.
let ts=null;
try{ts=require(require.resolve('typescript',{paths:[path.join(root,'frontend'),process.cwd(),__dirname]}));}catch{}
if(ts){for(const r of ['clt-reference-workspace.component.ts','collateral-insurance-panel.component.ts']){
 check('TypeScript syntax '+r,()=>{const f=path.join(root,source,r);const d=ts.transpileModule(fs.readFileSync(f,'utf8'),{fileName:f,reportDiagnostics:true,compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ES2022,experimentalDecorators:true}}).diagnostics||[];assert.equal(d.filter(x=>x.category===ts.DiagnosticCategory.Error).length,0);});
}}else console.log('CLT_R3C_TYPESCRIPT_PARSER_UNAVAILABLE; ANGULAR_BUILD_MUST_VALIDATE');
if(process.exitCode)process.exit(1);
console.log('CLT_R3C_STATIC_PASS; CHECKS='+count+'; ORACLE_WRITE=NONE; BASE_R3C_UI_CONTRACT=PASS');
