'use strict';
// Tests the actual browser-only pure TypeScript evidence contract. No API, no Oracle.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../../..');
const candidates=[path.join(root,'frontend/node_modules/typescript'),
  path.resolve(path.dirname(process.execPath),'../lib/node_modules/typescript')];
let ts=null;
for(const p of candidates)try{ts=require(p);break;}catch{}
if(!ts){console.error('CLT_R1_FOLLOWUP_TYPESCRIPT_MISSING: run the official Angular build before this test');process.exit(2);}
const src=fs.readFileSync(path.join(root,'frontend/src/app/features/collateral/clt-r1-uat-contract.ts'),'utf8');
const compiled=ts.transpileModule(src,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022},reportDiagnostics:true});
if((compiled.diagnostics||[]).some(d=>d.category===ts.DiagnosticCategory.Error))throw Error('TypeScript transpile error');
const mod={exports:{}};
new Function('module','exports',compiled.outputText)(mod,mod.exports);
const C=mod.exports;
let passed=0, failed=0;
function test(label,fn){try{fn();passed++;console.log('PASS | '+label);}catch(e){failed++;console.error('FAIL | '+label+': '+e.message);}}
const good={source:'ORACLE_CLT_READ_ONLY',page:0,size:1,totalElements:12,items:[{PK:'43',RECORD_VERSION:'1'}]};
const empty={source:'ORACLE_CLT_READ_ONLY',page:0,size:1,totalElements:0,items:[]};
const bad=(field,value)=>({...good,[field]:value});
const reject=fn=>assert.throws(fn);
const validManual={create:'PASS',update:'PASS',delete:'PASS',rules:'PASS',note:'record 35 tested'};
const ref=['T0','T1','T2','T3','T4','T5','T6','T7','T8','T9','T10','T11','T12','T13','T14','T15','T16'];
const entry=table=>({table,...C.CLT_R1_NOT_RUN});
const report={schema:C.CLT_R1_EXPORT_SCHEMA,tables:ref.map(entry)};
test('17 metadata tables are independently represented',()=>assert.equal(ref.length,17));
test('valid real list is accepted',()=>assert.deepEqual(C.cltR1PageEvidence(good,'PK'),{count:12,id:'43',recordVersion:1}));
test('readable empty table is not rejected',()=>assert.deepEqual(C.cltR1PageEvidence(empty,'PK'),{count:0,id:'',recordVersion:0}));
test('fake list source refused',()=>reject(()=>C.cltR1PageEvidence(bad('source','CACHE'),'PK')));
test('negative count refused',()=>reject(()=>C.cltR1PageEvidence(bad('totalElements',-1),'PK')));
test('zero count with a row refused',()=>reject(()=>C.cltR1PageEvidence(bad('totalElements',0),'PK')));
test('nonempty count without row refused',()=>reject(()=>C.cltR1PageEvidence(bad('items',[]),'PK')));
test('wrong page refused',()=>reject(()=>C.cltR1PageEvidence(bad('page',1),'PK')));
test('more than one row refused',()=>reject(()=>C.cltR1PageEvidence(bad('items',[good.items[0],good.items[0]]),'PK')));
test('invalid row id refused',()=>reject(()=>C.cltR1PageEvidence(bad('items',[{PK:'0',RECORD_VERSION:1}]),'PK')));
test('zero version refused',()=>reject(()=>C.cltR1PageEvidence(bad('items',[{PK:'12',RECORD_VERSION:0}]),'PK')));
test('detail PK and version equality accepted',()=>assert.equal(C.cltR1DetailMatches({PK:'43',RECORD_VERSION:1},'PK','43',1),true));
test('stale detail version refused',()=>assert.equal(C.cltR1DetailMatches({PK:'43',RECORD_VERSION:2},'PK','43',1),false));
test('mismatched detail ID refused',()=>assert.equal(C.cltR1DetailMatches({PK:'44',RECORD_VERSION:1},'PK','43',1),false));
test('manual PASS requires all four checks',()=>assert.equal(C.cltR1Complete({state:'READABLE',count:1,httpStatus:null},validManual),true));
test('manual PASS cannot override missing Oracle read',()=>assert.equal(C.cltR1Complete({state:'FAILED',count:null,httpStatus:500},validManual),false));
test('blank evidence prevents falsely closing table',()=>assert.equal(C.cltR1Complete({state:'READABLE',count:1,httpStatus:null},{...validManual,note:''}),false));
test('an unrun scenario prevents acceptance',()=>assert.equal(C.cltR1Complete({state:'READABLE',count:1,httpStatus:null},{...validManual,rules:'NOT_RUN'}),false));
test('17 manual rows import from valid report',()=>assert.equal(Object.keys(C.cltR1ImportManual(JSON.stringify(report),ref)).length,17));
test('import rejects missing table',()=>reject(()=>C.cltR1ImportManual(JSON.stringify({...report,tables:report.tables.slice(1)}),ref)));
test('import rejects duplicate table',()=>reject(()=>C.cltR1ImportManual(JSON.stringify({...report,tables:[...report.tables.slice(1),report.tables[1]]}),ref)));
test('import rejects invalid manual decision',()=>reject(()=>C.cltR1ImportManual(JSON.stringify({...report,tables:[{...report.tables[0],create:'MUTATED'},...report.tables.slice(1)]}),ref)));
test('import refuses wrong report schema',()=>reject(()=>C.cltR1ImportManual(JSON.stringify({...report,schema:'R6'}),ref)));
test('import strips control characters',()=>assert.equal(C.cltR1CleanNote('x\n\t y'),'x   y'));
console.log(`CLT_R1_FOLLOWUP_PURE_${failed?'FAIL':'PASS'}=${passed} FAIL=${failed}`);
process.exitCode=failed?1:0;
