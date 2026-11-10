#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../../..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
let checks=0;
const yes=(test,why)=>{if(!test)throw Error('CLT_R3_WORKFLOW_SOURCE_FAIL '+why);checks++};
const service=read('backend/src/main/java/com/behsazan/corebanking/collateral/workflow/CollateralR3WorkflowService.java');
const controller=read('backend/src/main/java/com/behsazan/corebanking/collateral/workflow/CollateralR3WorkflowController.java');
const policy=read('backend/src/main/java/com/behsazan/corebanking/collateral/workflow/CollateralR3WorkflowPolicy.java');
const security=read('backend/src/main/java/com/behsazan/corebanking/collateral/prototype/CltUnifiedPrototypeSecurityConfiguration.java');
const ui=read('frontend/src/app/features/collateral/collateral-r3-draft-worklist.component.ts');
const html=read('frontend/src/app/features/collateral/collateral-r3-draft-worklist.component.html');
const client=read('frontend/src/app/features/collateral/collateral-r3-workflow.service.ts');
const ddl=read('database/oracle/clt/r3workflow/01_R3_STAGE_TRANSITIONS_CANDIDATE_NOT_APPROVED.sql');
const roadmap=read('docs/collateral/CLT-ROADMAP-R1.0-FA.md');
const gaps=read('docs/collateral/CLT-GAP-REGISTER-R1.0-FA.md');
yes(service.includes('ConditionalOnProperty')&&service.includes('havingValue = "true"'),'prototype_only');
yes(controller.includes('ConditionalOnProperty')&&controller.includes('havingValue = "true"'),'controller_prototype_only');
for(const kind of ['SPONSOR','INSURANCE','VALUATION']) yes(policy.includes(kind+'('),'domain_'+kind);
for(const action of ['SUBMIT','WITHDRAW','REVIEW','REJECT']) yes(policy.includes('case '+action),'action_'+action);
yes(policy.includes('if (!reviewerAuthorized || isMaker)'),'independent_reviewer');
yes(policy.includes('version(String value)'),'optimistic_version_type');
yes(service.includes('SELECT * FROM '),'oracle_payload_readback');
yes(service.includes('payloadDigest(kind, id, collateralId)'),'recomputed_hash');
yes(service.includes("EVENT_CODE = 'REQUEST_CREATED'"),'creation_audit_guard');
yes(service.includes('FOR UPDATE'),'row_lock');
yes(service.includes('RECORD_VERSION = RECORD_VERSION + 1'),'optimistic_update');
yes(service.includes('DECISION_REASON, EVENT_AT'),'reason_audit_event');
yes(service.includes('CollateralR3WorkflowPolicy.requiresCurrentDomainValidation(action)') &&
    service.includes('validateForStageDecision(kind, collateralId, id, action, auth)'),
    'submit_and_review_revalidation');
yes(policy.includes('action == Action.SUBMIT || action == Action.REVIEW'),
    'positive_review_revalidates_current_domain');
for(const domain of ['sponsor','insurance','valuation']) {
  const domainService=read('backend/src/main/java/com/behsazan/corebanking/collateral/'+domain+'/Collateral'+domain[0].toUpperCase()+domain.slice(1)+'Service.java');
  yes(domainService.includes('screenForReview(') &&
      domainService.includes('"SCOPE_clt.collateral.review"'),
      'independent_review_scope_'+domain);
  yes(service.includes(domain+'.screenForReview(') && service.includes(domain+'.validate('),
      'review_submits_are_separated_'+domain);
}
yes(service.includes('CLT_R3_WORKFLOW_REVIEW_REVALIDATION_BLOCKED'), 'review_fails_closed');
yes(service.includes('Collatera'+'lSponsorService'),'sponsor_revalidation');
yes(service.includes('CollateralInsuranceService'),'insurance_revalidation');
yes(service.includes('CollateralValuationService'),'valuation_revalidation');
yes(service.includes("STATUS_CODE = 'SUBMITTED' AND MAKER_SUBJECT <> ?"),'review_queue_isolation');
yes(service.includes('new DataSourceTransactionManager(applicationDataSource)'),'shared_transaction');
yes(service.includes('ALL_CONSTRAINTS')&&service.includes('SEARCH_CONDITION_VC'),'schema_gate_requires_migration');
for(const p of ['sponsors','insurance-policies','valuations']) yes(security.includes('"/api/v1/collaterals/*/'+p+'/requests"'),'local_allow_'+p);
yes(security.includes('"/api/v1/collaterals/r3-workflow/*/*/transition"'),'local_stage_action');
yes(client.includes('expectedVersion')&&client.includes('reason'),'client_stage_version_reason');
yes(html.includes('ارسال برای بررسی')&&html.includes('بازبینی شد (غیرقطعی)'),'ui_actions');
yes(ui.includes('workflowEnabled.set(readiness.stageTransitionsEnabled)'),'ui_readiness');
yes(ui.includes("this.reviewerAvailable.set(readiness.independentReviewerAvailable)"),'ui_reviewer_only');
yes(ddl.includes("RAISE_APPLICATION_ERROR(-20953")&&ddl.indexOf('RAISE_APPLICATION_ERROR')<ddl.indexOf('ALTER TABLE'),'candidate_guard');
yes((ddl.match(/ADD DECISION_REASON/g)||[]).length===3,'three_event_reasons');
yes((ddl.match(/DROP CONSTRAINT/g)||[]).length===6,'six_checks_replaced');
yes(roadmap.includes('CLT-R3-STAGE-WORKFLOW-CHECKPOINT-2026-11-09.md'),'roadmap_checkpoint');
yes(gaps.includes('R3 Stage Workflow'),'gap_register');
for(const name of ['sponsor','insurance','valuation']) {
 const api=read('frontend/src/app/features/collateral/collateral-'+name+'.service.ts');
 yes(api.includes("'SUBMITTED' | 'REVIEWED' | 'REJECTED' | 'WITHDRAWN'"),'status_contract_'+name);
}
yes(!/(?:INSERT\s+INTO|UPDATE|DELETE\s+FROM)\s+CLT\.COLLATERAL_(?:SPONSOR\b|INSURANCE\b|VALUATION_HISTORY\b)/i.test(service),'no_final_domain_dml');
console.log('CLT_R3_WORKFLOW_SOURCE_PASS='+checks+'/'+checks);
