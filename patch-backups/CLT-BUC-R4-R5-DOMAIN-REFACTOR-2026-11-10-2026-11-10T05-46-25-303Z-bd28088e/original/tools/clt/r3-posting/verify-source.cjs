'use strict';
const fs=require('fs');const path=require('path');
const root=path.resolve(process.argv[2] || process.cwd());
const t=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const source='backend/src/main/java/com/behsazan/corebanking/collateral/workflow/';
const ui='frontend/src/app/features/collateral/';
const svc=t(source+'CollateralR3PostingReadinessService.java');
const policy=t(source+'CollateralR3PostingReadinessPolicy.java');
const shared=t(source+'CollateralR3StageFingerprint.java');
const workflow=t(source+'CollateralR3WorkflowService.java');
const controller=t(source+'CollateralR3WorkflowController.java');
const frontendService=t(ui+'collateral-r3-workflow.service.ts');
const component=t(ui+'collateral-r3-draft-worklist.component.ts');
const template=t(ui+'collateral-r3-draft-worklist.component.html');
let checks=0;
const check=(condition,message)=>{if(!condition)throw Error('R3_POSTING_CONTRACT_FAILED:'+message);checks++;};
for(const kind of ['SPONSOR','INSURANCE','VALUATION']){
 check(policy.includes('case '+kind+' ->'), 'domain '+kind);
 check(svc.includes('case '+kind+' ->'), 'physical evidence '+kind);
}
check(svc.includes('c.BRANCH_CODE=r.BRANCH_CODE') && svc.includes('r.BRANCH_CODE = ?'), 'principal branch restriction');
check(svc.includes('SCOPE_clt.collateral.read'),'requires read scope');
check(svc.includes('sharedDataSource') && !svc.includes('DriverManager') && !svc.includes('new Hikari'), 'shared datasource');
check(svc.includes('DECISION_REASON') && svc.includes('EVENT_ACTOR<>r.MAKER_SUBJECT'), 'audited independent review');
check(svc.includes('CREATED_AUDIT_COUNT') && svc.includes('SUBMITTED_AUDIT_COUNT')
 && svc.includes('INDEPENDENT_REVIEW_AUDIT_COUNT'),'three event types');
check(svc.includes('CollateralR3StageFingerprint.digest'), 're-read oracle stage fingerprint');
check(workflow.includes('CollateralR3StageFingerprint.digest'), 'same digest in reviewer flow');
check(shared.includes('case SPONSOR ->')&&shared.includes('case INSURANCE ->')&&shared.includes('case VALUATION ->'), 'identical three canonical domains');
check(!/jdbc\.(update|execute|batchUpdate)\(/.test(svc),'read-only posting service');
check(!/(@PostMapping|@PutMapping|@DeleteMapping)/.test(controller.slice(controller.indexOf('Non-mutating posting preflight'),controller.indexOf('@PostMapping'))), 'new route GET only');
check(policy.includes('false, false, false'), 'no final posting or policy grant');
check(policy.includes('R3_APPROVED_POSTING_SCHEMA_TRANSACTION_AUDIT_AND_ROLLBACK'), 'explicit approval gates');
check(controller.includes('@GetMapping("/{kind}/{requestId}/posting-readiness")'), 'posting contract route');
check(frontendService.includes('posting-readiness') && frontendService.includes('finalPostingEligible: false'), 'angular typed client');
check(component.includes('previewPosting') && component.includes('this.workflow.postingReadiness'), 'angular component wire');
check(template.includes('پیش‌بررسی ثبت قطعی') && template.includes('فاقد ثبت')===false, 'presentation text present');
check(template.includes('مجوز ثبت نهایی') && template.includes('صادر نشده'), 'no misleading UI');
check(fs.existsSync(path.join(root,'docs/collateral/CLT-R3-POSTING-DECISION-MATRIX-FA.md')), 'decision matrix');
check(fs.existsSync(path.join(root,'docs/collateral/CLT-R3-POSTING-PREFLIGHT-CHECKPOINT-2026-11-09.md')), 'checkpoint');
// TS syntax-only (actual Angular dependency compilation is a separately documented gate).
const ts=require('/opt/nvm/versions/node/v22.16.0/lib/node_modules/typescript/lib/typescript.js');
for (const file of ['collateral-r3-workflow.service.ts','collateral-r3-draft-worklist.component.ts']) {
 const obj=ts.createSourceFile(file,t(ui+file),ts.ScriptTarget.Latest,true,ts.ScriptKind.TS);
 check(obj.parseDiagnostics.length===0,'TypeScript parse '+file);
}
console.log('CLT_R3_POSTING_SOURCE_CONTRACT_PASS='+checks+'/'+checks);
