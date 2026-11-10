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

// Stable BUC/SUC evidence journal: no phase-coded classes, no automatic Oracle DDL,
// no financial posting scope and no bank-approval masquerade.
const evidencePolicy=read(back+'CollateralBusinessUatEvidencePolicy.java');
const evidenceService=read(back+'CollateralBusinessUatEvidenceService.java');
const evidenceController=read(back+'CollateralBusinessUatEvidenceController.java');
const oracleContract=read(front+'collateral-business-uat-oracle-contract.ts');
const sql=read('database/oracle/clt/business-uat/01_CLT_BUC_UAT_SCHEMA_CANDIDATE_NOT_APPROVED.sql');
const readSql=read('database/oracle/clt/business-uat/00_CLT_BUC_UAT_READONLY_DISCOVERY.sql');
const registrySecurity=read('backend/src/main/java/com/behsazan/corebanking/collateral/registry/CollateralRegistrySecurityConfiguration.java');
has(evidencePolicy,['BUC-(0[1-9]|1[0-6])-[PN]','public static List<Scenario> normalize',
 'public static List<Scenario> fillMissing','CLT-BUC-SUC-UAT-ORACLE-1','CLT_UAT_DUPLICATE_SCENARIO',
 'CLT_UAT_NOTE_TOO_LONG','"NOT_RUN", "PASS", "FAIL", "BLOCKED"']);
check(!evidencePolicy.includes('CLT-R6-BUC-EVIDENCE-1'),'CAPABILITY_NAME_NOT_PHASE_TAG');
has(evidenceService,['@Service','public class CollateralBusinessUatEvidenceService','DataSource dataSource',
 'new DataSourceTransactionManager(dataSource)','new TransactionTemplate','FOR UPDATE WAIT 2',
 'SCOPE_clt.collateral.uat.write','CltAnalystStageIdentity.require(auth)',
 'CLT_BUC_UAT_EVENT_SEQ.NEXTVAL','CLT_UAT_EXPECTED_REVISION_CONFLICT',
 'CLT_UAT_ORACLE_WRITE_FAILED','false,false,scenarios','bankAcceptance()',
 'oracleAttestation()','CLT_BUC_UAT_RESULT','CLT_BUC_UAT_EVENT',
 'UPDATE CLT.CLT_BUC_UAT_RUN SET REVISION=?',
 'DELETE FROM CLT.CLT_BUC_UAT_RESULT','INSERT INTO CLT.CLT_BUC_UAT_EVENT',
 'ALL_TAB_COLUMNS','ALL_SEQUENCES','SCOPE_clt.collateral.read']);

check(!evidenceService.includes('final class CollateralBusinessUatEvidenceService'),'SPRING_SERVICE_MUST_ALLOW_PROXY');
check(!evidenceService.includes('CLT.COLLATERAL_ALLOCATION'),'ANALYST_JOURNAL_CANNOT_WRITE_ALLOCATION');
check(!evidenceService.includes('CLT.COLLATERAL_TRANSACTION'),'ANALYST_JOURNAL_CANNOT_WRITE_FINANCIAL_TXN');
has(evidenceController,['@RestController','@GetMapping("/business-evidence")',
 '@PutMapping("/business-evidence")','@GetMapping("/business-evidence/history")',
 'service.get(auth)','service.put(auth,data)','service.history(auth)']);
has(oracleContract,['CLT-BUC-SUC-UAT-ORACLE-1','collateralValidateUatOracleState',
 'collateralUatOracleScenarios','collateralValidateUatOracleEvents',
 'bankAcceptance:false','oracleAttestation:false','new Set(CLT_BUC_SCENARIOS.map(s=>s.id))']);
has(service,['saveEvidence(request:CollateralUatOracleWrite)','this.http.put<unknown>',
 'this.http.get<unknown>(this.evidenceEndpoint)']);
has(ui,['loadOracleEvidence(force:boolean)','saveOracleEvidence()',
 'oracleDirty.set(this.results()!==snapshot)','e.status===409',
 'collateralValidateUatOracleState','oracleEvidenceMessage','businessExitCheck']);
has(html,['data-clt-business-uat-oracle','ذخیره نتایج در Oracle',
 'بازیابی نتایج از Oracle','تاریخچه تغییرات','businessExitCheck()']);
has(registrySecurity,['HttpMethod.PUT, "/api/v1/collaterals/uat/business-evidence"',
 'SCOPE_clt.collateral.uat.write']);
has(local,['HttpMethod.PUT, "/api/v1/collaterals/uat/business-evidence"']);
has(sql,['RAISE_APPLICATION_ERROR(-20961','CLT_BUC_UAT_CANDIDATE_NOT_APPROVED_NO_AUTO_DDL',
 '-- CREATE TABLE CLT.CLT_BUC_UAT_RUN', '-- CREATE TABLE CLT.CLT_BUC_UAT_RESULT',
 '-- CREATE TABLE CLT.CLT_BUC_UAT_EVENT','-- CREATE SEQUENCE CLT.CLT_BUC_UAT_EVENT_SEQ']);
has(readSql,['ALL_TAB_COLUMNS'.toLowerCase(),'all_sequences']);
check(sql.split('\n').filter(line=>/^\s*(CREATE|ALTER|DROP|GRANT)\b/i.test(line)).length===0,
  'DDL_MUST_BE_COMMENTED_IN_CANDIDATE');
console.log('CLT_BUC_SUC_UAT_ORACLE_SOURCE_CONTRACT_PASS=YES;CHECKS='+checks+';MIGRATION=NONE;BANK_ATTESTATION=NONE');
