'use strict';
// Durable contract for domain naming, legacy routes and analyst Stage (not financial booking).
const fs=require('fs'),path=require('path');
const root=path.resolve(process.argv[2]||process.cwd());
const rd=p=>fs.readFileSync(path.join(root,p),'utf8');
const has=p=>fs.existsSync(path.join(root,p));
const base='backend/src/main/java/com/behsazan/corebanking/collateral/';
const web='frontend/src/app/features/collateral/';
let n=0;
const assert=(expr,what)=>{if(!expr)throw new Error('CLT_DOMAIN_USECASE_SOURCE_FAIL:'+what);n++};
const java=[];
for(const dir of ['backend/src/main/java/com/behsazan/corebanking/collateral','backend/src/test/java/com/behsazan/corebanking/collateral']) {
 const walk=d=>{for(const file of fs.readdirSync(path.join(root,d),{withFileTypes:true})) {
  const p=d+'/'+file.name;
  if(file.isDirectory())walk(p);else if(p.endsWith('.java'))java.push(p);
 }};walk(dir);
}
assert(java.length>=180,'java source exists');
assert(java.every(p=>!/(?:CollateralR3|CltR3)/.test(path.basename(p))),'permanent java classes no R3 name');
assert(java.every(p=>!/(?:\bCollateralR3|\bCltR3)/.test(rd(p))),'java imports no retired R3 classes');
for(const name of ['CollateralWorkflowService','CollateralWorkflowController','CollateralValuationCommitService',
 'CollateralSponsorCommitService','CollateralInsuranceCommitService'])
 assert(java.some(p=>p.endsWith('/'+name+'.java')),'domain java '+name);
const ctrl=rd(base+'workflow/CollateralWorkflowController.java');
assert(ctrl.includes('/api/v1/collaterals/workflow') && ctrl.includes('/api/v1/collaterals/r3-workflow'),'canonical and legacy single workflow controller');
assert(!has(base+'workflow/CollateralR3WorkflowController.java'),'legacy controller deleted');
const lifecycle=rd(base+'lifecycle/CollateralLifecycleEvidenceController.java');
assert(lifecycle.includes('/lifecycle-evidence')&&lifecycle.includes('/r3/lifecycle-evidence'),'lifecycle route aliases');
const proto=rd(base+'prototype/CltAnalystStageIdentity.java');
for(const token of ['CLT_UNIFIED_LOCAL_DEV','clt_local_prototype','clt-unified-local-internal','financialPostingAuthorized() { return false','consumerAttested() { return false'])
 assert(proto.includes(token),'internal local actor '+token);
const allocation=rd(base+'allocation/CollateralAllocationIntakeService.java');
const aCtrl=rd(base+'allocation/CollateralAllocationIntakeController.java');
const unallocation=rd(base+'unallocation/CollateralUnallocationIntakeService.java');
const uCtrl=rd(base+'unallocation/CollateralUnallocationIntakeController.java');
assert(allocation.includes('submitAnalyst(')&&aCtrl.includes('@PostMapping("/analyst")'),'R4 analyst Stage route');
assert(unallocation.includes('submitAnalyst(')&&unallocation.includes('cancelAnalyst('),'R5 analyst Stage and cancel');
assert(uCtrl.includes('unallocation/analyst-requests')&&uCtrl.includes('analyst-requests/{requestId}/cancel'),'R5 UI routes');
assert(!allocation.includes('UPDATE CLT.COLLATERAL_ALLOCATION')&&!unallocation.includes('UPDATE CLT.COLLATERAL_ALLOCATION'),'no financial allocation write');
const aCfg=rd(base+'allocation/CollateralAllocationIntakeConfiguration.java');
const uCfg=rd(base+'unallocation/CollateralUnallocationIntakeConfiguration.java');
assert(aCfg.includes('applicationDataSource')&&aCfg.includes('prototype.enabled'),'R4 shared datasource opt in');
assert(uCfg.includes('applicationDataSource')&&uCfg.includes('prototype.enabled'),'R5 shared datasource opt in');
const aPool=rd(base+'allocation/CollateralAllocationIntakeWriterPool.java');
const uPool=rd(base+'unallocation/CollateralUnallocationIntakeWriterPool.java');
assert(aPool.includes('if (ownsSource)')&&uPool.includes('if(ownsSource)'),'shared datasource never closed');
const security=rd(base+'prototype/CltUnifiedPrototypeSecurityConfiguration.java');
assert(security.includes('/api/v1/collaterals/workflow/*/*/transition')&&security.includes('anyRequest().denyAll()'),'workflow alias local chain');
assert(security.includes('unallocation/analyst-requests')&&security.includes('allocation-requests/intake/analyst'),'new POST Stage allowlist');
for(const f of ['collateral-workflow.service.ts','collateral-draft-worklist.component.ts','collateral-draft-worklist.component.html',
 'collateral-insurance-cancellation.service.ts','collateral-insurance-cancellation.component.ts']) assert(has(web+f),'domain frontend '+f);
for(const f of ['collateral-r3-workflow.service.ts','collateral-r3-draft-worklist.component.ts',
 'collateral-r3-insurance-cancellation.component.ts']) assert(!has(web+f),'retired frontend absent '+f);
const feService=rd(web+'collateral-workflow.service.ts');
assert(feService.includes("'/api/v1/collaterals/workflow'"),'frontend canonical path');
const allocUI=rd(web+'collateral-allocation-request-panel.component.ts');
const unallocUI=rd(web+'collateral-unallocation-panel.component.ts');
assert(allocUI.includes('stageAnalystRequest()')&&allocUI.includes('stagingReceipt'),'R4 UI Stage and receipt');
assert(unallocUI.includes('stageAnalyst')&&unallocUI.includes('cancelAnalyst'),'R5 UI Stage and cancel');
const repo=rd(base+'allocation/CollateralAllocationIntakeRepository.java');
assert(repo.includes('branchCode')&&repo.includes('BRANCH_CODE'),'R4 branch replay scope');
const doc=rd('docs/collateral/CLT-USECASE-DELIVERY-CHECKPOINT-2026-11-10.md');
assert(doc.includes('BUC')&&doc.includes('Stage-only')&&doc.includes('END-TO-END ACCEPTED'),'scope explicit');
for (const f of ['config/clt-unified-local.yml','config/clt-unified-local.properties']) {
 if(has(f))assert(!rd(f).includes('allocation.intake.request-writes-enabled: true')&&!rd(f).includes('unallocation.intake.request-writes-enabled: true'),'no force-enable');
}
console.log('CLT_DOMAIN_USECASE_SOURCE_PASS='+n+'/'+n+'; NO_FINANCIAL_POSTING=true');
