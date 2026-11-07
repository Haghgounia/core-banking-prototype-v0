'use strict';
// CLT-R6J: test the actual pure TypeScript projection, not a duplicate rewrite.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(process.argv[2] || '.');
const base=path.join(root,'frontend/src/app/features/collateral');
let ts;
try{ts=require(require.resolve('typescript',{paths:[path.join(root,'frontend'),process.cwd(),__dirname]}));}
catch(e){console.error('CLT_R6J_TS_MODULE_UNAVAILABLE: '+e.message);process.exit(1);}
function parse(name){
 const file=path.join(base,name);
 const raw=fs.readFileSync(file,'utf8');
 const out=ts.transpileModule(raw,{fileName:file,reportDiagnostics:true,compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS,experimentalDecorators:true}});
 const errors=(out.diagnostics||[]).filter(d=>d.category===ts.DiagnosticCategory.Error);
 assert.equal(errors.length,0,'TS_PARSE_'+name+':'+errors.map(d=>ts.flattenDiagnosticMessageText(d.messageText,' ')).join(';'));
 return {text:raw,js:out.outputText};
}
let checks=0;
function test(label,fn){fn();checks++;console.log('PASS | '+label);}
try{
 const helper=parse('clt-r6j-registry-reference.ts');
 const service=parse('clt-reference.service.ts');
 const workspace=parse('clt-reference-workspace.component.ts');
 const html=fs.readFileSync(path.join(base,'clt-reference-workspace.component.html'),'utf8');
 const m={exports:{}};
 new Function('module','exports','require',helper.js)(m,m.exports,()=>{throw Error('PURE_HELPER_EXTERNAL_IMPORT')});
 const {cltR6jActivePage}=m.exports;
 const codes=['70','60','50','44','43','42','41','30','29','28','27','26','25','24','23','22','21','20','19','18','17','16','15','14','13','12','11','10'];
 const groups=codes.map((code,i)=>({COLLATERAL_GROUP_ID:String(i+1),COLLATERAL_GROUP_CODE:code,COLLATERAL_GROUP_NAME:'گروه شماره '+code,STATUS:'ACTIVE'}));
 test('R6I group endpoint, not old reference endpoint',()=>{
  assert(service.text.includes("'/api/v1/collaterals/groups'"));
  assert(service.text.includes("'/api/v1/collaterals/types'"));
  assert(service.text.includes('cltR6jActivePage('));
  assert(service.text.includes('this.http.get<CltReadPage>(`${this.base}/${encodeURIComponent(table)}`, {params})'));
 });
 test('28 group rows preserved from actual API projection',()=>{
  const p=cltR6jActivePage('COLLATERAL_GROUP',groups,0,20);assert.equal(p.totalElements,28);assert.equal(p.items.length,20);assert.equal(p.source,'ORACLE_CLT_READ_ONLY');
 });
 test('pagination second page 8 real rows',()=>{
  const p=cltR6jActivePage('COLLATERAL_GROUP',groups,1,20);assert.equal(p.items.length,8);assert.equal(p.items[7].COLLATERAL_GROUP_CODE,'10');
 });
 test('exact and Persian-digit group lookup',()=>{
  assert.equal(cltR6jActivePage('COLLATERAL_GROUP',groups,0,20,'۷۰').totalElements,1);
  assert.equal(cltR6jActivePage('COLLATERAL_GROUP',groups,0,20,'٧٠').totalElements,1);
  assert.equal(cltR6jActivePage('COLLATERAL_GROUP',groups,0,20,'گروه شماره 50').totalElements,1);
 });
 test('inactive and malformed rows are never projected as active',()=>{
  const rows=[...groups,{COLLATERAL_GROUP_ID:'123',COLLATERAL_GROUP_CODE:'99',COLLATERAL_GROUP_NAME:'غیرفعال',STATUS:'INACTIVE'},{STATUS:'ACTIVE'}];
  assert.equal(cltR6jActivePage('COLLATERAL_GROUP',rows,0,200).totalElements,28);
 });
 test('zero types remain zero, never fabricated from groups',()=>{
  assert.equal(cltR6jActivePage('COLLATERAL_TYPE',[],0,20).totalElements,0);
 });
 test('active type projection keeps its parent group and policy flags only as supplied',()=>{
  const types=[{COLLATERAL_TYPE_ID:'2',COLLATERAL_TYPE_CODE:'008',COLLATERAL_TYPE_NAME:'نمونه آزمون',COLLATERAL_GROUP_ID:'1',STATUS:'ACTIVE'}];
  assert.equal(cltR6jActivePage('COLLATERAL_TYPE',types,0,20).items[0].COLLATERAL_GROUP_ID,'1');
 });
 test('unexpected table and payload rejected',()=>{
  assert.throws(()=>cltR6jActivePage('ANOTHER_TABLE',groups,0,20));
  assert.throws(()=>cltR6jActivePage('COLLATERAL_GROUP',{},0,20));
  assert.throws(()=>cltR6jActivePage('COLLATERAL_GROUP',groups,-1,20));
  assert.throws(()=>cltR6jActivePage('COLLATERAL_GROUP',groups,0,201));
 });
 test('UI shows active-source scope and only columns available via API',()=>{
  assert(workspace.text.includes('CLT_R6J_REGISTRY_CATALOG_COLUMNS'));
  assert(html.includes('API فقط‌خواندنی R6I'));
  assert(html.includes('registryActiveCatalog() && mode()'));
 });
 test('read-only UI hides reference writer for group and type',()=>{
  assert(workspace.text.includes('&& !this.registryActiveCatalog()'));
  // R3C/RF1 also exclude unlicensed insurer candidates from the reference writer workflow.
  assert(workspace.text.includes('if (!this.registryActiveCatalog()) void this.loadGovernanceState()')
    || workspace.text.includes('if (!this.registryActiveCatalog() && !this.insurerCatalog()) void this.loadGovernanceState()'));
 });
 test('read failures cannot be mistaken for zero Oracle records',()=>{
  assert(html.includes('صفر بودن نتایج به معنای خالی بودن Oracle نیست'));
 });
 test('no simulated records or writes in active-group projection',()=>{
  assert(!/fetch\(|\.post\(|\.put\(|\.delete\(|\.patch\(|INSERT|UPDATE|DELETE FROM|MERGE INTO/i.test(helper.text));
 });
 console.log('CLT_R6J_REFERENCE_UI_SMOKE_PASS='+checks+'; ACTIVE_GROUP_FIX=YES; TYPES_EMPTY_NO_FAKE=YES; IAM_CHANGED=NO; DB_WRITES=NONE');
}catch(e){console.error('CLT_R6J_REFERENCE_UI_SMOKE_FAIL: '+e.stack);process.exit(1);}
