#!/usr/bin/env node
'use strict';
/** Executable R6 source contracts only; never marks analyst or Oracle UAT accepted. */
const fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'../../..');
const back='backend/src/main/java/com/behsazan/corebanking/collateral/uat/';
const front='frontend/src/app/features/collateral/';
function read(p){const filename=path.join(root,p);if(!fs.existsSync(filename))throw Error('R6_MISSING_SOURCE '+p);return fs.readFileSync(filename,'utf8');}
function check(ok,why){if(!ok)throw Error('CLT_R6_CONTRACT_FAIL '+why);checks++;}
function has(str,tokens){for(const t of tokens)check(str.includes(t),'MISSING_'+t)}
let checks=0;
const policy=read(back+'CollateralR6ReadinessPolicy.java');
const endpoint=read(back+'CollateralR6ReadinessController.java');
const cat=read(front+'clt-r6-business-uat-contract.ts');
const service=read(front+'clt-r6-business-uat.service.ts');
const ui=read(front+'clt-r6-business-uat.component.ts');
const html=read(front+'clt-r6-business-uat.component.html');
const session=read(front+'clt-r6-business-uat-session.service.ts');
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
check(!/jdbc\.(update|execute|batchUpdate)\(/.test(endpoint),'R6_GET_MUST_NOT_WRITE');
has(policy,['"UNKNOWN"','"PRESENT"','"MISSING"','List.copyOf(rows)','Collections.unmodifiableMap(items)']);
has(cat,['CLT_R6_SCENARIOS','cltR6ImportBusinessEvidence','cltR6BusinessEvidenceExport',
 'bankAcceptance:false','oracleAttestation:false','Object.hasOwn(result','r[\'note\'].length>280',
 'p[\'bankAcceptance\']!==false','acceptanceAttested)throw']);
has(service,["this.http.get<unknown>(this.endpoint)",'/api/v1/collaterals/uat/business-readiness']);
check(!service.includes('.post('),'R6_SERVICE_READONLY');
has(ui,['CltR6BusinessUatSessionService','this.session.results','readonly allScenarios',
 'void this.refresh()','timeout({first:12000})','cltR6ValidateDatabaseReadiness',
 'this.navigateTo.emit','results.update','this.probe.set(null)','exportEvidence()']);
has(session,["@Injectable({providedIn:'root'})",'readonly results=signal']);
has(html,['data-clt-r6-business-uat','data-clt-buc','data-clt-scenario',
 'manualCompleteBucs()','databaseStatus(item.id)','باز کردن صفحه عملیاتی',
 'خروجی نسخه‌دار UAT','<details class="r6-collapse">']);
has(wb,['data-clt-r6-business-tab','<app-clt-r6-business-uat','(navigateTo)="navigateBusiness($event)"']);
has(wbts,['CltR6BusinessUatComponent','navigateBusiness(request:CltNavigationRequest)']);
has(registry,['<app-collateral-timeline-panel [collateral]="detail" />']);
has(registryts,['CollateralTimelinePanelComponent']);
has(timeline,['cltAssertTimeline(response,50)','this.data.set(null)','String(this.collateral[\'COLLATERAL_ID\']','cltValidNumber(number)']);
has(timelineservice,['historyByRegistration(number:string,page=0,size=50)']);
has(nav,["'summary'|'amendment'|'sponsors'","['summary','amendment','sponsors'"]);
has(security,['"/api/v1/collaterals", "/api/v1/collaterals/**"','SCOPE_clt.collateral.read']);
has(local,['HttpMethod.GET','"/api/v1/collaterals/**"']);
console.log('CLT_R6_BUSINESS_SOURCE_CONTRACT_PASS='+checks+'/'+checks+';UAT_ATTESTATION=NONE;ORACLE_DML=NONE');
