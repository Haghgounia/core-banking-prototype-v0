'use strict';
/* R6C: execute actual TypeScript policies (not a rewritten JS approximation). */
const fs=require('fs'),path=require('path');
const target=process.argv[2];
if(!target){console.error('CLT_R6C_UI_SMOKE_FAIL:TARGET_REQUIRED');process.exit(2)}
let ts;
try{ts=require(require.resolve('typescript',{paths:[path.join(target,'frontend'),process.cwd(),__dirname]}))}
catch{console.error('CLT_R6C_UI_SMOKE_FAIL:TYPESCRIPT_REQUIRED_IN_FRONTEND_NODE_MODULES');process.exit(1)}
const dir=path.join(target,'frontend/src/app/features/collateral');
function load(name, imports={}){
 const file=path.join(dir,name+'.ts');
 const result=ts.transpileModule(fs.readFileSync(file,'utf8'),{fileName:file,reportDiagnostics:true,compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS,experimentalDecorators:true}});
 const errors=(result.diagnostics||[]).filter(d=>d.category===ts.DiagnosticCategory.Error);
 if(errors.length)throw Error('TS_PARSE:'+name+':'+errors.map(d=>ts.flattenDiagnosticMessageText(d.messageText,' ')).join(';'));
 const m={exports:{}};
 const importer=(id)=>{if(Object.hasOwn(imports,id))return imports[id];throw Error('UNEXPECTED_IMPORT:'+name+':'+id)};
 new Function('module','exports','require',result.outputText)(m,m.exports,importer);
 return m.exports;
}
let checks=0;
function ok(condition,label){checks++;if(!condition)throw Error('CHECK_'+checks+':'+label)}
try{
 const p=load('clt-r6c-ui-policy');
 const cases=[
  ['۱۲۳۴۵','12345',true],['١٢٣٤','1234',true],['1234567890123456789','1234567890123456789',true],
  ['۱23۴۵','12345',true],['  ۱۲  ','12',true],['1','1',true],
  ['۰','0',false],['0','0',false],['01','01',false],['۱ ۲','1 2',false],
  ['1.0','1.0',false],['-1','-1',false],['0x12','0x12',false],['۱۲۳٫۴','123٫4',false],
  ['12345678901234567890','12345678901234567890',false],['','',false],['  ','',false],
  ['12\n','12',true],['۱۲۳abc','123abc',false],['۱۲۳٬۴۵','123٬45',false]
 ];
 for(const [value,normalized,valid] of cases){ok(p.cltNormalizedNumber(value)===normalized,'normalization '+value);ok(p.cltValidNumber(value)===valid,'validity '+value)}
 for(const [value,want] of [[null,'—'],['','—'],['0','0'],['1000','1٬000'],['1234567890123456789','1٬234٬567٬890٬123٬456٬789'],['12345.1234','12٬345.1234'],['-12000.5','-12٬000.5'],[1000000,'1٬000٬000'],[Number.MAX_SAFE_INTEGER+1,'خطای دقت عددی API'],['12,345','12,345']])ok(p.cltFormatAmount(value)===want,'amount '+value);
 const routeEntries=Object.entries({list:'/collateral/registry',direct:'/collateral/lookup',introduction:'/collateral/introduction',uat:'/collateral/uat'});
 for(const [name,route] of routeEntries)ok(p.CLT_ROUTE_FOR_MODE[name]===route,'route '+name);
 for(const name of ['summary','sponsors','insurance','valuation','capacity','allocation','unallocation','release','history'])ok(p.cltDetailSection(name)===name,'section '+name);
 for(const name of ['INVALID','__proto__','','unallocation/other'])ok(p.cltDetailSection(name)==='summary','unknown section');
 for(const [name,sect] of Object.entries({insurance:'insurance',valuation:'valuation',release:'release',history:'history',allocation:'allocation',capacityOverview:'capacity'}))ok(p.cltSectionForView(name)===sect,'view '+name);
 for(const [id,sect] of Object.entries({'UAT-01':'sponsors','UAT-04':'allocation','UAT-05':'valuation','UAT-09':'sponsors'}))ok(p.cltSectionForUatCase(id)===sect,'uat '+id);
 ok(p.cltUatNeedsEvidence('PASS',''),'missing pass evidence');ok(p.cltUatNeedsEvidence('BLOCKED','   '),'missing block evidence');
 ok(!p.cltUatNeedsEvidence('NOT_RUN',''),'not-run no evidence');ok(!p.cltUatNeedsEvidence('FAIL','HTTP 403'),'fail with evidence');
 const catalog=load('collateral-r6a-catalog');
 ok(catalog.CLT_UAT_CASES.length===10,'ten cases');ok(catalog.CLT_UI_COVERAGE.length===15,'15 views');
 ok(new Set(catalog.CLT_UAT_CASES.map(x=>x.id)).size===10,'unique UAT');
 ok(new Set(catalog.CLT_UI_COVERAGE.map(x=>x.key)).size===15,'unique views');
 const empty={};let counts=catalog.cltUatCounts(empty);
 ok(counts.NOT_RUN===10,'default all not-run');ok(counts.PASS===0,'default zero pass');
 const draft={'UAT-01':{status:'PASS',note:'آزمون صرفا در مرورگر اجرا شد'},'UAT-02':{status:'BLOCKED',note:'بدون داده آزمون'}};
 const serialized=catalog.cltUatExport(draft,'2026-11-04T11:00:00Z');
 ok(JSON.parse(serialized).oracleAttestation===false,'oracle not attested');
 ok(JSON.parse(serialized).runtimeAttestation===false,'runtime not attested');
 ok(JSON.parse(serialized).analystApproval===false,'approval not attested');
 ok(JSON.parse(serialized).cases.length===10,'export cases');
 const restored=catalog.cltUatImport(serialized);
 ok(restored['UAT-01'].status==='PASS','import status');ok(restored['UAT-02'].note==='بدون داده آزمون','import notes');
 ok(catalog.cltUatCounts(restored).NOT_RUN===8,'unrun preserved');
 for(const changed of [serialized.replace('CLT-R6A-UAT-EVIDENCE-1','FAKE-APPROVED'),serialized.replace('"UAT-01"','"UNKNOWN"'),'{invalid json']){
  let rejected=false;try{catalog.cltUatImport(changed)}catch{rejected=true}ok(rejected,'malformed import rejected');
 }
 const srcRegistry=fs.readFileSync(path.join(dir,'collateral-registry.component.ts'),'utf8');
 ok(srcRegistry.includes('this.api.list(page, this.pageSize, this.appliedListFilter())') ||
   srcRegistry.includes('this.api.list(page, this.pageSize, this.appliedListFilter(), this.groupCode())'),
   'real list API search wired; extended R6I server-side group filter remains bound');
 ok(srcRegistry.includes('this.router.navigateByUrl(destination)'),'browser route navigation');
 ok(srcRegistry.includes('this.requestSequence++'),'request invalidation');
 ok(srcRegistry.includes('this.requestedSection.set(cltDetailSection('),'deep-link section guarded');
 const srcUat=fs.readFileSync(path.join(dir,'collateral-uat-workbench.component.ts'),'utf8');
 ok(srcUat.includes('inject(CltUatSessionService)'),'in-memory UAT retention');
 ok(!/sessionStorage|localStorage|fetch\(/.test(srcUat),'no UAT persistence or network');
 const ui=fs.readFileSync(path.join(dir,'collateral-registry.component.html'),'utf8');
 ok(ui.includes('role="search"'),'search accessible');ok(ui.includes('displayAmount('),'money format');
 ok(ui.includes('requestedSectionLabel()'),'deep link UX');
 const reports=fs.readFileSync(path.join(dir,'collateral-reports.component.ts'),'utf8');
 ok(!reports.includes('تکمیل IAM'),'reports focus CLT');
 ok(reports.includes("timeZone:'Asia/Tehran'"),'report timestamp Teheran');
 console.log('CLT_R6C_UI_POLICY_SMOKE_PASS='+checks+'; UAT_CASES=10; REFERENCE_VIEWS=15; NO_FINANCIAL_WRITES=YES');
}catch(e){console.error('CLT_R6C_UI_POLICY_SMOKE_FAIL:'+e.message);process.exitCode=1}
