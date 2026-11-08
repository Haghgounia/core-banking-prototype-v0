'use strict';
// Pure TypeScript presentation tests; no Angular/Oracle dependency, no data mutations.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const root=path.resolve(process.argv[2]||path.join(__dirname,'../../..'));
const file=path.join(root,'frontend/src/app/features/collateral/clt-reference-presentation.ts');
let ts;try{ts=require(require.resolve('typescript',{paths:[path.join(root,'frontend'),process.cwd(),...((process.env.NODE_PATH||'').split(path.delimiter).filter(Boolean))]}));}
catch(_e){console.error('CLT_R1_PRESENTATION_TEST_NOT_RUN=NO_TYPESCRIPT_COMPILER');process.exit(process.argv.includes('--require-typescript')?1:0);}
const source=fs.readFileSync(file,'utf8');
const compiler=ts.transpileModule(source,{fileName:file,reportDiagnostics:true,
 compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}});
if((compiler.diagnostics||[]).some(x=>x.category===ts.DiagnosticCategory.Error))throw Error('TYPESCRIPT_PARSER_FAILED');
const compiledExports={};vm.runInNewContext(compiler.outputText,{exports:compiledExports,Intl,Date,Set,Number,String,Object,RegExp});
let p=0;function test(name,fn){fn();p++;console.log('PASS | '+name);}
test('Persian date is not an ISO leak',()=>{const actual=compiledExports.cltPersianAuditDateTime('2026-11-08T11:24:17.467349');
 assert.ok(actual.includes('11')&&actual.includes('24')&&actual.includes('17'),actual);
 assert.ok(!actual.includes('2026')&&!actual.includes('467349'),actual);
});
test('null and invalid dates show placeholder',()=>{assert.equal(compiledExports.cltPersianAuditDateTime(null),'—');
 assert.equal(compiledExports.cltPersianAuditDateTime('2026-02-30T03:04:05'),'—');
 assert.equal(compiledExports.cltPersianAuditDateTime('unsafe raw timestamp'),'—');
});
test('system fields hidden and status separate',()=>{assert.equal(compiledExports.cltIsBusinessDetailField('CREATED_BY','INSURANCE_COMPANY_ID'),false);
 assert.equal(compiledExports.cltIsBusinessDetailField('INSURANCE_COMPANY_ID','INSURANCE_COMPANY_ID'),false);
 assert.equal(compiledExports.cltIsBusinessDetailField('STATUS','INSURANCE_COMPANY_ID'),false);
 assert.equal(compiledExports.cltIsBusinessDetailField('INSURANCE_COMPANY_NAME','INSURANCE_COMPANY_ID'),true);
});
test('business label compact and audit identified',()=>{
 assert.equal(compiledExports.cltReferenceFieldLabel('STATUS','long verbose text'),'وضعیت');
 assert.equal(compiledExports.cltReferenceFieldLabel('UPDATED_AT','raw'), 'تاریخ و ساعت آخرین ویرایش');
});
test('create read-back requires exact persisted record',()=>{
 const fields={STATUS:'ACTIVE',INSURANCE_COMPANY_NAME:'بیمه تستی'};
 assert.equal(compiledExports.cltVerifySavedReference('INSURANCE_COMPANY_ID','43',null,fields,
 {INSURANCE_COMPANY_ID:'43',RECORD_VERSION:'1',...fields}),true);
 assert.equal(compiledExports.cltVerifySavedReference('INSURANCE_COMPANY_ID','43',null,fields,
 {INSURANCE_COMPANY_ID:'44',RECORD_VERSION:'1',...fields}),false);
});
test('update read-back requires incremented RECORD_VERSION',()=>{
 assert.equal(compiledExports.cltVerifySavedReference('INSURANCE_COMPANY_ID','43',4,{STATUS:'INACTIVE'},
 {INSURANCE_COMPANY_ID:'43',RECORD_VERSION:'4',STATUS:'INACTIVE'}),false);
 assert.equal(compiledExports.cltVerifySavedReference('INSURANCE_COMPANY_ID','43',4,{STATUS:'INACTIVE'},
 {INSURANCE_COMPANY_ID:'43',RECORD_VERSION:'5',STATUS:'INACTIVE'}),true);
});
test('numeric decimal normalizes without unsafe float rounding',()=>{
 assert.equal(compiledExports.cltVerifySavedReference('COLLATERAL_TYPE_ID','900000000000000001',1,{DISPLAY_ORDER:'0001.00'},
 {COLLATERAL_TYPE_ID:'900000000000000001',RECORD_VERSION:'2',DISPLAY_ORDER:'1'}),true);
 assert.equal(compiledExports.cltVerifySavedReference('COLLATERAL_TYPE_ID','900000000000000001',1,{DISPLAY_ORDER:'999999999999999998'},
 {COLLATERAL_TYPE_ID:'900000000000000001',RECORD_VERSION:'2',DISPLAY_ORDER:'999999999999999999'}),false);
});
test('nullable field clear is verified, not assumed',()=>{
 assert.equal(compiledExports.cltVerifySavedReference('REF_CURRENCY_ID','1',1,{CURRENCY_NUMERIC_CODE:null},
 {REF_CURRENCY_ID:'1',RECORD_VERSION:'2',CURRENCY_NUMERIC_CODE:null}),true);
 assert.equal(compiledExports.cltVerifySavedReference('REF_CURRENCY_ID','1',1,{CURRENCY_NUMERIC_CODE:null},
 {REF_CURRENCY_ID:'1',RECORD_VERSION:'2',CURRENCY_NUMERIC_CODE:'364'}),false);
});
console.log('CLT_R1_PRESENTATION_TEST_PASS='+p+'/'+p);
