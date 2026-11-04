'use strict';
/** Execute ACTUAL pure TS policies from installed R6D source; no Angular runtime and NO HTTP calls. */
const fs=require('fs'),path=require('path'),assert=require('assert'),vm=require('vm');
const root=path.resolve(process.argv[2]||'.');
let ts;
try {ts=require(path.join(root,'frontend/node_modules/typescript'));}
catch {try{ts=require('typescript')}catch {throw Error('TYPESCRIPT_DEPENDENCY_MISSING_RUN_PROJECT_BUILD_FIRST')}}
const src='frontend/src/app/features/collateral/';let passed=0;
function test(fn){fn();passed++;}
function load(rel){
 const file=path.join(root,src,rel),source=fs.readFileSync(file,'utf8');
 const result=ts.transpileModule(source,{fileName:file,reportDiagnostics:true,compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}});
 const errors=(result.diagnostics||[]).filter(x=>x.category===ts.DiagnosticCategory.Error);
 if(errors.length)throw Error('TYPESCRIPT_PARSE_ERROR:'+rel+':'+errors.map(x=>ts.flattenDiagnosticMessageText(x.messageText,' ')).join(';'));
 const module={exports:{}};vm.runInNewContext(result.outputText,{module,exports:module.exports,require,Number,JSON,Set,Array,String,Error,Object,RegExp}, {filename:rel,timeout:1000});
 return module.exports;
}
try{
 const d=load('clt-r6d-readonly-diagnostics-policy.ts'),r=load('clt-r6d-registry-contract.ts');
 const probe={
 'registry-readiness':{registryReadEnabled:true,registryWritesEnabled:false,authorizationMode:'JWT_BRANCH_ONLY'},
 'registry-page':{items:[],totalElements:0,page:0,size:1,source:'ORACLE_CLT_OWN_BRANCH'},
 'report-overview':{source:'ORACLE_CLT_READ_ONLY',scope:'JWT_COLLATERAL_BRANCH',branchCode:'TEST_BRANCH',totalCollaterals:0,byStatus:[],activeExposures:[],allocationAuthorization:false},
 'reference-summary':{source:'ORACLE_CLT_REFERENCE',scope:'GLOBAL_TECHNICAL_CATALOG',activeTypes:0,activeGroups:0,hqAttestation:false}
 };
 const keys=d.CLT_READONLY_PROBES.map(x=>x.key);
 test(()=>assert.strictEqual(keys.join(','),'registry-readiness,registry-page,report-overview,reference-summary'));
 test(()=>assert.strictEqual(new Set(keys).size,4));
 for(const key of keys){
  test(()=>assert.strictEqual(d.cltProbeVerdict(key,200,probe[key]).state,'PASS'));
  for(const st of [401,403])test(()=>assert.strictEqual(d.cltProbeVerdict(key,st,{}).state,'BLOCKED_AUTH'));
  for(const [st,expected] of [[503,'READ_DISABLED'],[404,'MISSING_ROUTE'],[0,'UNAVAILABLE'],[408,'TIMEOUT'],[504,'TIMEOUT'],[500,'SERVER_ERROR']])
   test(()=>assert.strictEqual(d.cltProbeVerdict(key,st,null).state,expected));
  test(()=>assert.strictEqual(d.cltProbeVerdict(key,200,{}).state,'CONTRACT_DRIFT'));
  test(()=>assert.strictEqual(d.cltProbeVerdict(key,204,probe[key]).state,'SERVER_ERROR'));
 }
 for(const [key,bad] of [
  ['registry-readiness',{...probe['registry-readiness'],registryWritesEnabled:true}],
  ['registry-readiness',{...probe['registry-readiness'],registryReadEnabled:false}],
  ['registry-page',{...probe['registry-page'],source:'MOCK'}],
  ['registry-page',{...probe['registry-page'],page:1}],
  ['registry-page',{...probe['registry-page'],totalElements:-1}],
  ['report-overview',{...probe['report-overview'],allocationAuthorization:true}],
  ['report-overview',{...probe['report-overview'],scope:'BANK_WIDE'}],
  ['reference-summary',{...probe['reference-summary'],hqAttestation:true}],
  ['reference-summary',{...probe['reference-summary'],activeGroups:-1}]
 ])test(()=>assert.strictEqual(d.cltProbeVerdict(key,200,bad).state,'CONTRACT_DRIFT'));
 const evidence=keys.map(key=>({key,label:'not exported',httpStatus:200,state:'PASS',message:'hidden',secret:'do-not-export'}));
 const exported=d.cltProbeExport(evidence,'2026-11-04T10:00:00.000Z');
 test(()=>assert.strictEqual(JSON.parse(exported).probes.length,4));
 for(const token of ['not exported','do-not-export','hidden','TEST_BRANCH','Authorization','token','subject'])
  test(()=>assert.strictEqual(exported.includes(token),false));
 test(()=>assert.strictEqual(JSON.parse(exported).financialWrites,false));
 test(()=>assert.strictEqual(JSON.parse(exported).oracleAttestation,false));
 test(()=>assert.strictEqual(JSON.parse(exported).uatApproval,false));
 const good={source:'ORACLE_CLT_OWN_BRANCH',items:[{COLLATERAL_ID:'123',COLLATERAL_NUMBER:'9999999999999999999'}],totalElements:1,page:0,size:20};
 test(()=>assert.doesNotThrow(()=>r.cltAssertRegistryPage(good,0,20)));
 test(()=>assert.doesNotThrow(()=>r.cltAssertRegistryPage({...good,items:[],totalElements:0},0,20)));
 for(const bad of [
  {...good,source:'LOCAL_DEMO'}, {...good,page:1}, {...good,size:21}, {...good,totalElements:-1},
  {...good,totalElements:2**53}, {...good,items:[...good.items,...good.items],totalElements:1},
  {...good,items:[{COLLATERAL_ID:123,COLLATERAL_NUMBER:'100'}]},
  {...good,items:[{COLLATERAL_ID:'0',COLLATERAL_NUMBER:'100'}]},
  {...good,items:[{COLLATERAL_ID:'10000000000000000000',COLLATERAL_NUMBER:'100'}]},
  {...good,items:[{COLLATERAL_ID:'123',COLLATERAL_NUMBER:9999999999999999999}]},
  {...good,items:[{}]}, {...good,items:null}, null, [],
  {...good,items:[{...good.items[0]},...Array.from({length:20},()=>good.items[0])],totalElements:21}
 ])test(()=>assert.throws(()=>r.cltAssertRegistryPage(bad,0,20),/CLT_R6D_RESPONSE_CONTRACT_INVALID/));
 test(()=>assert.doesNotThrow(()=>r.cltAssertRegistryDetail(good.items[0],'COLLATERAL_ID','123')));
 test(()=>assert.doesNotThrow(()=>r.cltAssertRegistryDetail(good.items[0],'COLLATERAL_NUMBER','9999999999999999999')));
 for(const bad of [
  [{...good.items[0]},'COLLATERAL_ID','987'],
  [{...good.items[0],COLLATERAL_ID:123},'COLLATERAL_ID','123'],
  [{...good.items[0],COLLATERAL_NUMBER:'0'},'COLLATERAL_ID','123'],
  [null,'COLLATERAL_ID','123'],
  [{...good.items[0],COLLATERAL_NUMBER:'10000000000000000000'},'COLLATERAL_ID','123']
 ])test(()=>assert.throws(()=>r.cltAssertRegistryDetail(...bad),/CLT_R6D_RESPONSE_CONTRACT_INVALID/));
 const source=fs.readFileSync(path.join(root,src,'collateral-registry.component.ts'),'utf8');
 for(const necessary of ['this.invalidatePending();','this.route.queryParamMap','cltAssertRegistryPage(result,page,this.pageSize)','cltAssertRegistryDetail(found,\'COLLATERAL_NUMBER\',number)','cltAssertRegistryDetail(found,\'COLLATERAL_ID\',id)'])
  test(()=>assert(source.includes(necessary),'REGISTRY_WIRING:'+necessary));
 const uat=fs.readFileSync(path.join(root,src,'collateral-uat-workbench.component.html'),'utf8');
 test(()=>assert(uat.includes('<app-clt-r6d-readonly-diagnostics />')));
 const diag=fs.readFileSync(path.join(root,src,'clt-r6d-readonly-diagnostics.component.ts'),'utf8');
 test(()=>assert(!/\.(post|put|patch|delete)\s*\(/i.test(diag),'DIAGNOSTIC_WRITER_FORBIDDEN'));
 test(()=>assert(!/localStorage|sessionStorage|Authorization\s*:/i.test(diag),'UNEXPECTED_AUTH_OR_STORAGE'));
 for(const file of ['clt-r6d-readonly-diagnostics.component.ts','collateral-registry.component.ts','collateral-uat-workbench.component.ts']){
  const input=fs.readFileSync(path.join(root,src,file),'utf8');
  const result=ts.transpileModule(input,{reportDiagnostics:true,fileName:file,compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ES2022,experimentalDecorators:true}});
  test(()=>assert(!(result.diagnostics||[]).some(x=>x.category===ts.DiagnosticCategory.Error),'SYNTAX:'+file));
 }
 console.log('CLT_R6D_CONTRACT_AND_DIAGNOSTICS_POLICY_PASS='+passed+'; FINANCIAL_WRITES=NONE; ORACLE_EXECUTION=NONE');
}catch(e){console.error('CLT_R6D_POLICY_FAIL:'+e.stack);process.exitCode=1}
