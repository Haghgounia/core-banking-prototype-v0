'use strict';
// CLT-R6M: source-based regression of T2 and negative T3. No Oracle/HTTP writes.
const fs=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
const root=path.resolve(process.argv[2]||path.resolve(__dirname,'../../..'));
const get=p=>fs.readFileSync(path.join(root,p),'utf8');
const directory='frontend/src/app/features/collateral/';
const comp=get(directory+'clt-reference-workspace.component.ts');
const html=get(directory+'clt-reference-workspace.component.html');
const css=get(directory+'clt-reference-workspace.component.scss');
const service=get(directory+'clt-reference.service.ts');
const registry=get(directory+'collateral-registry.component.html');
let ts;
try{ts=require(require.resolve('typescript',{paths:[path.join(root,'frontend'),process.cwd(),__dirname]}));}
catch(e){console.error('CLT_R6M_TYPESCRIPT_MISSING; NPM_INSTALL_REQUIRED');process.exit(1);}
let count=0;
function check(label,action){try{action();count++;console.log('PASS | '+label);}catch(e){console.error('FAIL | '+label+' | '+e.message);process.exitCode=1;}}
for(const n of ['clt-reference-workspace.component.ts','clt-reference.service.ts','clt-r6j-registry-reference.ts','clt-r6c-ui-policy.ts']){
 check('TypeScript syntax '+n,()=>{
  const file=path.join(root,directory,n);const result=ts.transpileModule(fs.readFileSync(file,'utf8'),{fileName:file,reportDiagnostics:true,
   compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS,experimentalDecorators:true}});
  assert.equal((result.diagnostics||[]).filter(x=>x.category===ts.DiagnosticCategory.Error).length,0);
 });
}
const codes=['10','11','12','13','14','15','16','17','18','19','20','21','22','23','24','25','26','27','28','29','30','41','42','43','44','50','60','70'];
const file=path.join(root,directory,'clt-r6j-registry-reference.ts');
const out=ts.transpileModule(fs.readFileSync(file,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}});
const moduleShim={exports:{}};new Function('module','exports',out.outputText)(moduleShim,moduleShim.exports);
const page=moduleShim.exports.cltR6jActivePage;
const rows=codes.map((code,i)=>({COLLATERAL_GROUP_ID:String(i+1),COLLATERAL_GROUP_CODE:code,COLLATERAL_GROUP_NAME:code==='10'?'\u06a9\u0627\u0644\u0627':`\u06af\u0631\u0648\u0647 ${code}`,STATUS:'ACTIVE'}));
check('28 source rows across 20 and 8 pages',()=>{assert.equal(page('COLLATERAL_GROUP',rows,0,20).items.length,20);assert.equal(page('COLLATERAL_GROUP',rows,1,20).items.length,8);});
check('search by ASCII and Persian code',()=>{assert.equal(page('COLLATERAL_GROUP',rows,0,20,'10').totalElements,1);assert.equal(page('COLLATERAL_GROUP',rows,0,20,'\u06f1\u06f0').totalElements,1);});
check('search by Persian title',()=>assert.equal(page('COLLATERAL_GROUP',rows,0,20,'\u06a9\u0627\u0644\u0627').totalElements,1));
check('zero active types remain empty',()=>assert.equal(page('COLLATERAL_TYPE',[],0,20).totalElements,0));
check('search draft separated from applied filter',()=>{assert.match(comp,/readonly appliedSearch = signal\(''\)/);assert.match(comp,/this\.appliedSearch\.set\(this\.searchText\(\)\.trim\(\)\)/);assert.match(comp,/this\.searchEnabled\(\) \? this\.appliedSearch\(\) : ''/);});
check('page navigation and clear search persist',()=>{assert.match(html,/data-clt-run-search/);assert.match(html,/data-clt-clear-search/);assert.match(html,/\(keyup\.enter\)="runSearch\(\)"/);assert.match(html,/load\(page\(\)\+1\)/);});
check('native accessible modal replaces inline detail',()=>{assert.match(html,/<dialog #recordDetailDialog data-clt-dialog/);assert.match(html,/aria-modal="true"/);assert.match(html,/aria-labelledby="cltRecordDialogTitle"/);assert.doesNotMatch(html,/<aside class="detail-panel">/);assert.match(comp,/dialog\.showModal\(\)/);assert.match(comp,/dialog\.close\(\)/);});
check('keyboard cancel and focus restoration use native dialog',()=>{assert.match(html,/\(close\)="onRecordDialogClosed\(\)"/);assert.match(comp,/this\.recordOpener\?\.focus\(\)/);});
check('dialog detail remains read only',()=>{assert.match(html,/data-clt-close-dialog/);assert.doesNotMatch(html.match(/<dialog[\s\S]*?<\/dialog>/)[0],/submitProposal\(/);assert.match(html,/class="record-technical"/);assert.match(css,/\.clt-record-dialog::backdrop/);});
check('type-empty distinguished from HTTP error',()=>{assert.match(html,/data-clt-empty/);assert.match(html,/@else if \(error\(\)\)/);assert.match(html,/table\.table === 'COLLATERAL_TYPE' && !appliedSearch\(\)/);});
check('T3 registry empty/error are distinct',()=>{assert.match(registry,/data-clt-registry-empty/);assert.match(registry,/@else if\(error\(\)\)/);assert.match(registry,/@else/);});
check('authorized CLT GET only; no added business writes',()=>{assert.match(service,/\/api\/v1\/collaterals\/groups/);assert.match(service,/\/api\/v1\/collaterals\/types/);assert.doesNotMatch(comp,/\bfetch\(|\bINSERT\b|\bMERGE INTO\b/);});
check('R6L T1 browser regression build marker retained',()=>assert.match(html,/data-clt-reference-build="R6L-T1"/));
check('R6M T2/T3 build marker',()=>assert.match(html,/data-clt-ui-contract="R6M-T2-T3"/));
check('versioned coverage and release docs',()=>{assert.match(get('docs/collateral/CLT-ROADMAP-R1.0-FA.md'),/CLT-R6M/);assert.match(get('docs/collateral/CLT-STEP01-FEATURE-COVERAGE-DATA-GATES-FA.md'),/T2/);assert.match(get('release/RELEASE-NOTES-0.11.0.md'),/CLT-R6M/);});
const numberFile=path.join(root,directory,'clt-r6c-ui-policy.ts');
const numberJs=ts.transpileModule(fs.readFileSync(numberFile,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}});
const numberModule={exports:{}};new Function('module','exports',numberJs.outputText)(numberModule,numberModule.exports);
const valid=numberModule.exports.cltValidNumber;
check('T3 invalid regulatory number is rejected before any GET',()=>{
 assert.equal(valid(''),false);assert.equal(valid('0'),false);assert.equal(valid('-1'),false);
 assert.equal(valid('12345678901234567890'),false);assert.equal(valid('abc'),false);
 assert.equal(valid('\u06f1\u06f2\u06f3'),true);assert.equal(valid('1234567890123456789'),true);
});
check('T3 legacy registration remains read-only/disabled',()=>{
 const text=get(directory+'collateral-introduction.component.html');
 assert.match(text,/filteredTypes\(\)\.length===0/);
 assert.match(text,/readiness\(\)/);
 assert.match(text,/types\(\)\.length === 0/);
});
if(process.exitCode){console.error('CLT_R6M_STATIC_FAIL');process.exit(1);}
console.log('CLT_R6M_STATIC_PASS; CHECKS='+count+'; NO_ORACLE_WRITE=YES; T2_MATURITY=SOURCE_QA');
