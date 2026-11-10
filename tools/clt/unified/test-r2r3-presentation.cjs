'use strict';
// Pure presentation policy tests. Resolve TypeScript from project dependencies on Windows and from developer tooling on Linux.
const assert=require('node:assert/strict'), fs=require('node:fs'), path=require('node:path'), vm=require('node:vm');
const root=path.resolve(__dirname,'../../..');
let ts;
for(const modulePath of [path.join(root,'frontend','node_modules','typescript'),'typescript','/opt/nvm/versions/node/v22.16.0/lib/node_modules/typescript']){
  try { ts=require(modulePath); break; } catch(e) { if(e.code!=='MODULE_NOT_FOUND') throw e; }
}
if(!ts){console.error('CLT_R2_R3_TYPESCRIPT_MISSING_AFTER_BUILD');process.exit(1);}
const file=path.join(root,'frontend/src/app/features/collateral/collateral-presentation.ts');
const source=fs.readFileSync(file,'utf8');
const out=ts.transpileModule(source,{fileName:file,reportDiagnostics:true,compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}});
assert.equal(out.diagnostics?.length||0,0);
const moduleExports={};vm.runInNewContext(out.outputText,{exports:moduleExports,Intl,Date,Number,RegExp,String,Array});
const {cltR2PersianDate:date,cltR2StatusName:status,cltPersistedDraftMatches:match}=moduleExports;
let tested=0;const test=(label,fn)=>{fn();tested++;console.log('PASS | '+label);};
test('Gregorian to Persian calendar exact date',()=>assert.match(date('2026-11-08'),/۱۴۰۵/));
test('Date never invents time',()=>assert.ok(!date('2026-11-08').includes(':')));
test('Timestamp keeps seconds unchanged',()=>assert.ok(date('2026-11-08T11:24:17.467349').endsWith('11:24:17')));
test('Null date is explicit',()=>assert.equal(date(null),'—'));
test('Invalid date rejected',()=>assert.equal(date('2026-02-31'),'تاریخ نامعتبر'));
test('Raw SQL date cannot leak',()=>assert.equal(date('0 OR 1=1'),'تاریخ نامعتبر'));
test('No unlabeled status business claim',()=>assert.match(status(''),/نامشخص/));
test('Real status title preserved',()=>assert.equal(status('در جریان'),'در جریان'));
const draft={draftId:'123',partyId:'45',collateralTypeId:'8',recordVersion:'1'};
test('True persisted draft verified',()=>assert.equal(match({...draft,status:'DRAFT'},draft),true));
test('Another draft id never matches',()=>assert.equal(match({...draft,draftId:'124',status:'DRAFT'},draft),false));
test('Wrong owner never matches',()=>assert.equal(match({...draft,partyId:'46',status:'DRAFT'},draft),false));
test('Wrong collateral type never matches',()=>assert.equal(match({...draft,collateralTypeId:'9',status:'DRAFT'},draft),false));
test('Changed version never matches',()=>assert.equal(match({...draft,recordVersion:'2',status:'DRAFT'},draft),false));
test('Finalized is not a draft',()=>assert.equal(match({...draft,status:'REGISTERED'},draft),false));
test('Null response never proves persistence',()=>assert.equal(match(null,draft),false));
console.log('CLT_R2_R3_PURE_TEST_PASS='+tested+'/'+tested);
