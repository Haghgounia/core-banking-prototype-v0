#!/usr/bin/env node
'use strict';
/** Executable business UAT source contracts only; never marks analyst or Oracle UAT accepted. */
const fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'../../..');
const back='backend/src/main/java/com/behsazan/corebanking/collateral/uat/';
const front='frontend/src/app/features/collateral/';
function read(p){const filename=path.join(root,p);if(!fs.existsSync(filename))throw Error('BUC_UAT_MISSING_SOURCE '+p);return fs.readFileSync(filename,'utf8');}
function check(ok,why){if(!ok)throw Error('CLT_BUC_UAT_CONTRACT_FAIL '+why);checks++;}
function has(str,tokens){for(const t of tokens)check(str.includes(t),'MISSING_'+t)}
let checks=0;
const policy=read(back+'CollateralBusinessReadinessPolicy.java');
const endpoint=read(back+'CollateralBusinessReadinessController.java');
const cat=read(front+'collateral-business-uat-contract.ts');
const service=read(front+'collateral-business-uat.service.ts');
const ui=read(front+'collateral-business-uat.component.ts');
const html=read(front+'collateral-business-uat.component.html');
const session=read(front+'collateral-business-uat-session.service.ts');
const wb=read(front+'collateral-uat-workbench.component.html');
const wbts=read(front+'collateral-uat-workbench.component.ts');
const registry=read(front+'collateral-registry.component.html');
const registryts=read(front+'collateral-registry.component.ts');
const timeline=read(front+'collateral-timeline-panel.component.ts');
const timelineservice=read(front+'collateral-reports.service.ts');
const nav=read(front+'clt-r6c-ui-policy.ts');
const security=read('backend/src/main/java/com/behsazan/corebanking/collateral/registry/CollateralRegistrySecurityConfiguration.java');
const local=read('backend/src/main/java/com/behsazan/corebanking/collateral/prototype/CltUnifiedPrototypeSecurityConfiguration.java');
for(let i=1;i<=16;i++){
 const id='BUC-'+String(i).padStart(2,'0');
 check(policy.includes('items.put("'+id+'"'),'JAVA_BUC_'+id);
 check(cat.includes('"id": "'+id+'"'),'TS_BUC_'+id);
}
for(let i=1;i<=20;i++){
 const id='SUC-'+String(i).padStart(2,'0');
 check(cat.includes('"'+id+'"'),'SUC_'+id);
}
has(endpoint,['@GetMapping("/business-readiness")','/api/v1/collaterals/uat','ALL_TABLES',"OWNER='CLT'",
 'requireReadPrincipal(auth)','SCOPE_clt.collateral.read','CollateralRegistryPolicy.branch(value)',
 'catch (DataAccessException ex)','metadataAvailable = false','false, local',
 'core-banking.collateral.posting.allocation-enabled','core-banking.collateral.posting.unallocation-enabled']);
check(!/jdbc\.(update|execute|batchUpdate)\(/.test(endpoint),'BUC_GET_MUST_NOT_WRITE');
has(policy,['"UNKNOWN"','"PRESENT"','"MISSING"','List.copyOf(rows)','Collections.unmodifiableMap(items)']);
has(cat,['CLT_BUC_SCENARIOS','collateralImportBusinessEvidence','collateralExportBusinessEvidence',
 'bankAcceptance:false','oracleAttestation:false','Object.hasOwn(result','rec[\'note\'].length>280',
 'p[\'bankAcceptance\']!==false','acceptanceAttested)throw']);
has(service,["this.http.get<unknown>(this.endpoint)",'/api/v1/collaterals/uat/business-readiness']);
check(!service.includes('.post('),'BUC_SERVICE_READONLY');
has(ui,['CollateralBusinessUatSessionService','this.session.results','readonly allScenarios',
 'void this.refresh()','timeout({first:12000})','collateralValidateBusinessReadiness',
 'this.navigateTo.emit','results.update','this.probe.set(null)','exportEvidence()','exportGaps()','collateralBusinessUatGaps']);
has(session,["@Injectable({providedIn:'root'})",'readonly results=signal']);
has(html,['data-clt-business-uat','data-clt-buc','data-clt-scenario',
 'manualCompleteBucs()','databaseStatus(item.id)','باز کردن صفحه عملیاتی',
 'خروجی نسخه‌دار UAT','<details class="uat-collapse">']);
has(wb,['data-clt-business-uat-tab','<app-collateral-business-uat','(navigateTo)="navigateBusiness($event)"']);
has(wbts,['CollateralBusinessUatComponent','navigateBusiness(request:CltNavigationRequest)']);
has(registry,['<app-collateral-timeline-panel [collateral]="detail" />']);
has(registryts,['CollateralTimelinePanelComponent']);
has(timeline,['cltAssertTimeline(response,50)','this.data.set(null)','String(this.collateral[\'COLLATERAL_ID\']','cltValidNumber(number)']);
has(timelineservice,['historyByRegistration(number:string,page=0,size=50)']);
has(nav,["'summary'|'amendment'|'sponsors'","['summary','amendment','sponsors'"]);
has(security,['"/api/v1/collaterals", "/api/v1/collaterals/**"','SCOPE_clt.collateral.read']);
has(local,['HttpMethod.GET','"/api/v1/collaterals/**"']);
console.log('CLT_BUC_UAT_SOURCE_CONTRACT_PASS='+checks+'/'+checks+';UAT_ATTESTATION=NONE;ORACLE_DML=NONE');

has(cat,['CLT-BUC-SUC-UAT-EVIDENCE-2','CLT-BUC-SUC-UAT-GAPS-1','CLT-R6-BUC-EVIDENCE-1',
 "legacy&&typeof rec['id']", 'acceptanceAttested', 'collateralExportBusinessGaps']);
check(!fs.existsSync(path.join(root,back+'CollateralR6ReadinessController.java')),'STALE_PHASE_CONTROLLER_REMOVED');
check(!fs.existsSync(path.join(root,front+'clt-r6-business-uat.component.ts')),'STALE_PHASE_COMPONENT_REMOVED');
console.log('CLT_BUC_SUC_NAMING_AND_GAPS_PASS=YES; CHECKS='+checks);
