'use strict';
// BUC-15 source contract only. Does not simulate Oracle, a bank approval, or a full application build.
const fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'../../..');
let count=0;
function contains(file, needles){
  const full=path.join(root,file);
  if(!fs.existsSync(full))throw Error('BUC15_MISSING '+file);
  const s=fs.readFileSync(full,'utf8');
  for(const value of needles){
    if(!s.includes(value))throw Error('BUC15_CONTRACT_MISSING '+file+' | '+value);
    ++count;
  }
}
try{
 const pkg='backend/src/main/java/com/behsazan/corebanking/collateral/crossunit/';
 contains(pkg+'CollateralCrossUnitService.java',[
   'DataSourceTransactionManager(source)','requireRequests()','requireDecisions()',
   'CLT_CROSS_UNIT_LOCAL_BANK_DECISION_DENIED','SCOPE_clt.crossunit.approve',
   'COLLATERAL_TYPE t','USE_IN_OTHER_BRANCHES','GRANT_STATUS=\'APPROVED\'',
   'g.CONSUMER_BRANCH=?','g.SOURCE_RECORD_VERSION=c.RECORD_VERSION',
   'g.VALID_UNTIL_UTC>SYS_EXTRACT_UTC(SYSTIMESTAMP)',
   'c.VALID_TO>=TRUNC(SYSDATE)','s.IS_ACTIVE_STATUS_FLAG=\'Y\'',
   'CLT_CROSS_UNIT_NOT_FOUND_OR_NOT_AUTHORIZED',
   'CltAnalystStageIdentity.require(auth)',
   'consumers.authenticatedClient(','sponsors.sponsorDebtorMatches(',
   'capacity.evidenceForVerifiedBranch(', 'RECORDED_NOT_ALLOCATED',
   'CLT.CLT_CROSS_UNIT_GRANT_EVENT','CLT.CLT_CROSS_UNIT_INTENT_DEBTOR',
   'CLT_CROSS_UNIT_FX_POLICY_NOT_APPROVED','G02_BANK_PERMIT_FOR_ATOMIC_ALLOCATION_REQUIRED'
 ]);
 contains(pkg+'CollateralCrossUnitController.java',[
  '/api/v1/collaterals/cross-unit','/by-registration/{registrationNumber}',
  '/owner/{collateralId}/grants','/intents/analyst','/intents/consumer',
  '/intents'
 ]);
 contains(pkg+'CollateralCrossUnitPolicy.java',[
  'typeEnabled && statusActive','"APPROVED".equals(grantStatus)',
  'consumer.equals(grantedConsumer)','grantVersion.equals(currentVersion)',
  'MessageDigest.isEqual','CLT_CROSS_UNIT_MAKER_CHECKER_MUST_DIFFER'
 ]);
 contains('backend/src/main/java/com/behsazan/corebanking/collateral/prototype/CltUnifiedPrototypeSecurityConfiguration.java',[
  '/api/v1/collaterals/cross-unit/owner/*/grants',
  '/api/v1/collaterals/cross-unit/intents/analyst',
  '.anyRequest().denyAll()'
 ]);
 contains('frontend/src/app/app.routes.ts',["path: 'collateral/cross-unit'",'CollateralCrossUnitWorkbenchComponent']);
 contains('frontend/src/app/features/collateral/collateral-registry.component.ts',[
  'CollateralCrossUnitWorkbenchComponent',"key:'crossunit'"
 ]);
 contains('frontend/src/app/features/collateral/collateral-registry.component.html',[
  'app-collateral-cross-unit-workbench','routerLink="/collateral/cross-unit"'
 ]);
 contains('frontend/src/app/features/collateral/collateral-cross-unit-workbench.component.ts',[
  'this.api.propose','this.api.inquiry','this.api.stage','this.api.intents',
  'this.api.decide','this.api.revoke','globalThis.crypto?.randomUUID?.()'
 ]);
 contains('frontend/src/app/features/collateral/collateral-cross-unit-workbench.component.html',[
  'ثبت درخواست مجوز (مرحله‌ای)','استعلام مجوز','ثبت درخواست مرحله‌ای استفاده',
  'ثبت تخصیص مالی: خیر'
 ]);
 contains('frontend/src/app/features/collateral/collateral-cross-unit.service.ts',[
  'Idempotency-Key','/by-registration/','/intents/analyst'
 ]);
 contains('database/oracle/clt/crossunit/01_BUC15_CROSS_UNIT_SCHEMA_CANDIDATE_NOT_APPROVED.sql',[
  'RAISE_APPLICATION_ERROR(-20945', '/*', 'CREATE TABLE CLT.CLT_CROSS_UNIT_GRANT (',
  'CREATE UNIQUE INDEX CLT.UQ_CLT_CU_GRANT_ACTIVE',
  'CREATE TABLE CLT.CLT_CROSS_UNIT_INTENT_DEBTOR ('
 ]);
 contains('docs/collateral/CLT-BUC15-CROSS-UNIT-USECASE-2026-11-10.md',[
  'BUC-15','BR-10','G-13','پیش‌فرض','false'
 ]);
 console.log('CLT_BUC15_SOURCE_CONTRACT_PASS='+count+'/'+count+'; FINANCIAL_POSTING=NOT_AUTHORIZED; ORACLE_DDL=NOT_EXECUTED');
} catch(e){console.error('CLT_BUC15_QA_FAIL='+e.message);process.exit(1);}
