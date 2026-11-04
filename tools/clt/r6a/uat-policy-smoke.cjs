'use strict';
const fs=require('fs'),path=require('path'),crypto=require('crypto'),vm=require('vm');
const target=path.resolve(process.argv[2]||path.join(__dirname,'../../..'));
const catalog=require('./uat-catalog-runtime.cjs');
const ts=fs.readFileSync(path.join(target,'frontend/src/app/features/collateral/collateral-r6a-catalog.ts'),'utf8');
const runtime=fs.readFileSync(path.join(target,'tools/clt/r6a/uat-catalog-runtime.cjs'),'utf8');
const html=fs.readFileSync(path.join(target,'tools/clt/r6a/CLT-R6A-UAT-OFFLINE.html'),'utf8');
const sha=s=>crypto.createHash('sha256').update(s).digest('hex');
let passed=0;
function ok(value,message){if(!value)throw Error('CLT_R6A_TEST_FAIL:'+message);passed++;}
function j(x){return JSON.stringify(x);}
try{
 ok(runtime.includes('source SHA256 '+sha(ts)),'GENERATED_RUNTIME_SOURCE_HASH');
 ok(catalog.CLT_UAT_CASES.length===10,'UAT_EXPECTED_10');
 ok(catalog.CLT_UI_COVERAGE.length===15,'VIEWS_EXPECTED_15');
 const keys=new Set();for(let i=1;i<=10;i++){
  const c=catalog.CLT_UAT_CASES[i-1],id='UAT-'+String(i).padStart(2,'0');
  ok(c.id===id,'UAT_ID_SEQUENCE_'+id);
  ok(c.title.length>3&&c.expected.length>8,'UAT_EXPECTATION_'+id);
  ok(c.bucs.every(x=>/^BUC-\d\d$/.test(x))&&c.sucs.every(x=>/^SUC-\d\d$/.test(x)), 'UAT_TRACE_IDS_'+id);
  ok(c.source==='Collateral_UI_Specification_FA.html','UAT_SOURCE_'+id);
 }
 for(const v of catalog.CLT_UI_COVERAGE){ok(!keys.has(v.key),'DUPLICATE_VIEW_'+v.key);keys.add(v.key);
  ok(['CONNECTED_COMPONENT','PARTIAL','GAP'].includes(v.state),'VIEW_STATE_'+v.key);}
 ok(keys.has('dashboard')&&keys.has('recentEvents')&&keys.has('hqDashboard'),'REPORTING_GAPS_REGISTERED');
 ok(catalog.CLT_UI_COVERAGE.filter(x=>x.state==='GAP').length>=3,'NO_FAKE_REPORTING_COMPLETION');
 ok(catalog.CLT_UAT_STATUSES.length===4,'FOUR_STATUSES');
 ok(catalog.CLT_UAT_CASES.every(x=>x.id&&x.uiTarget),'ALL_CASES_NAVIGABLE_TO_RELATED_WORKSPACE');
 const zero=catalog.cltUatCounts({});ok(j(zero)===j({NOT_RUN:10,PASS:0,FAIL:0,BLOCKED:0}),'DEFAULT_NOT_RUN');
 const changed={ 'UAT-01':{status:'PASS',note:'Test'},'UAT-02':{status:'FAIL',note:'Issue'},'UAT-03':{status:'BLOCKED',note:'No route'},'UAT-04':{status:'BAD',note:'x'}};
 const c=catalog.cltUatCounts(changed);
 ok(c.PASS===1&&c.FAIL===1&&c.BLOCKED===1&&c.NOT_RUN===7,'COUNTS_AND_INVALID_STATUS');
 ok(catalog.cltNormalizeUatStatus('NOPE')==='NOT_RUN','UNKNOWN_STATUS_SAFE');
 ok(catalog.cltNormalizeUatStatus('PASS')==='PASS','KNOWN_STATUS');
 ok(catalog.cltCleanUatNote(' a\x00b\n')==='a b','CONTROL_CHAR_SANITIZATION');
 ok(catalog.cltCleanUatNote('x'.repeat(400)).length===280,'MAX_NOTE_LENGTH');
 const exportData=JSON.parse(catalog.cltUatExport(changed,'2026-11-04T00:00:00.000Z'));
 ok(exportData.cases.length===10&&exportData.roadmap==='CLT-ROADMAP-R1.0','EXPORT_SCHEMA');
 ok(exportData.oracleAttestation===false&&exportData.runtimeAttestation===false&&exportData.analystApproval===false,'NO_FAKE_UAT_ATTESTATION');
 ok(exportData.cases[3].status==='NOT_RUN','INVALID_EXPORT_STATUS');
 ok(exportData.cases[0].note==='Test','EXPORT_NOTE');
 const match=html.match(/<script id="metadata" type="application\/json">([^<]+)<\/script>/);
 ok(!!match,'OFFLINE_JSON_EMBEDDED');
 const embedded=JSON.parse(match[1]);
 ok(j(embedded.cases.map(x=>x.id))===j(catalog.CLT_UAT_CASES.map(x=>x.id)),'OFFLINE_CASE_IDENTITIES');
 ok(j(embedded.views.map(x=>x.key))===j(catalog.CLT_UI_COVERAGE.map(x=>x.key)),'OFFLINE_VIEW_IDENTITIES');
 ok(html.includes('localOnly')&&html.includes('NOT_RUN')&&html.includes('FINANCIAL')===false,'OFFLINE_NO_FINANCIAL_CLAIM');
 const scriptMatches=[...html.matchAll(/<script(?: [^>]*)?>([\s\S]*?)<\/script>/g)];
 ok(scriptMatches.length>=2,'OFFLINE_SCRIPT_PRESENT');
 new vm.Script(scriptMatches.at(-1)[1]);ok(true,'OFFLINE_SCRIPT_PARSE');
 const registryHtml=fs.readFileSync(path.join(target,'frontend/src/app/features/collateral/collateral-registry.component.html'),'utf8');
 const registryTs=fs.readFileSync(path.join(target,'frontend/src/app/features/collateral/collateral-registry.component.ts'),'utf8');
 const workbench=fs.readFileSync(path.join(target,'frontend/src/app/features/collateral/collateral-uat-workbench.component.ts'),'utf8');
 ok(registryTs.includes('detailSection')&&registryHtml.includes('detailSection()'), 'DETAIL_LAZY_TABS');
 ok(registryHtml.includes('mode() !== \'uat\'')&&registryHtml.includes('(navigateTo)="navigateFromUat($event)"'),'UAT_PERSISTENT_HOST');
 ok(registryHtml.includes('app-collateral-final-release-panel')&&registryHtml.includes('app-collateral-unallocation-panel'),'R5_PANELS_PRESERVED');
 ok(workbench.includes('exportJson()')&&workbench.includes('cltUatExport'),'UAT_EXPORT_IMPLEMENTED');
 ok(!/(?:https?:\/\/|\blocalStorage\b|\bsessionStorage\b)/.test(workbench),'NO_REMOTE_OR_BROWSER_STORAGE_IN_UAT');
 const routes=fs.readFileSync(path.join(target,'frontend/src/app/app.routes.ts'),'utf8');
 const integrated=/path:\s*['"]collateral['"]/.test(routes) && /path:\s*['"]collateral\/reference-data['"]/.test(routes);
 console.log('CLT_R6A_UAT_POLICY_SMOKE_PASS='+passed+'; REFERENCE_UAT=10; UI_VIEWS=15; DEFAULT_RESULTS=NOT_RUN');
 console.log('CLT_R6A_ROUTE_STATUS='+(integrated?'ROUTES_PRESENT_REQUIRES_RUNTIME_QA':'NOT_REGISTERED; UAT_ANGULAR_ENTRY=BLOCKED_NEEDS_APPROVAL'));
}catch(e){console.error(e.message);process.exitCode=1;}
