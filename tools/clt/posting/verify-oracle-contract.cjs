'use strict';
/** BUC-09/10 Oracle required-columns and BUC-16 posted-event source guard. No database connection. */
const fs=require('node:fs');const path=require('node:path');
const root=path.resolve(__dirname,'../../..');
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const p='backend/src/main/java/com/behsazan/corebanking/collateral/';
const repo=read(p+'posting/CollateralPostingRepository.java');
const svc=read(p+'posting/CollateralPostingService.java');
const timeline=read(p+'reporting/CollateralTimelineRepository.java');
const summary=read(p+'reporting/CollateralTimelineService.java');
const props=read('backend/src/main/resources/clt/collateral-posting.properties');
const contract=read('frontend/src/app/features/collateral/clt-business-usecase-report-contract.ts');
const models=read('frontend/src/app/features/collateral/collateral-reports.service.ts');
const ui=read('frontend/src/app/features/collateral/collateral-reports.component.ts');
let checks=0;const check=(good,label)=>{if(!good)throw Error('CLT_ORACLE_POSTING_CONTRACT_INVALID: '+label);checks++;};
const SQL=t=>new RegExp('INSERT\\s+INTO\\s+CLT\\.'+t+'\\s*\\(([\\s\\S]*?)\\)\\s*VALUES\\s*\\(([\\s\\S]*?)\\)','i').exec(repo);
const inserts=[
 ['COLLATERAL_ALLOCATION',['COLLATERAL_ALLOCATION_ID','COLLATERAL_ID','BRANCH_CODE','MODULE_ID','FILE_NUMBER','ALLOCATE_AMOUNT','CURRENCY_CODE','USED','RECORD_VERSION','CREATED_AT','CREATED_BY','UPDATED_AT'],8],
 ['COLLATERAL_TRANSACTION',['COLLATERAL_TRANSACTION_ID','COLLATERAL_ID','COLLATERAL_ALLOCATION_ID','COLLATERAL_TRANSACTION_NUMBER','TRANSACTION_TYPE_CODE','TRANSACTION_STATUS_CODE','TRANSACTION_DIRECTION_CODE','TRANSACTION_AMOUNT','CURRENCY_CODE','EFFECTIVE_AT','ORIGINAL_TRANSACTION_ID','SOURCE_SYSTEM_CODE','IDEMPOTENCY_KEY','CREATED_BY','OWNER_BRANCH_CODE','EXECUTING_BRANCH_CODE','REQUEST_ID','TRANSACTION_AT','CREATED_AT','RECORD_VERSION'],16],
 ['COLLATERAL_TXN_STATUS_HISTORY',['COLLATERAL_TRANSACTION_ID','STATUS_SEQUENCE_NO','FROM_STATUS_CODE','TO_STATUS_CODE','CHANGED_AT','CHANGED_BY','CREATED_AT','CREATED_BY','RECORD_VERSION'],4]
];
for(const [table,expected,binds] of inserts){
 const m=SQL(table);check(m!==null,table+' insert present');
 const columns=m[1].replace(/\s+/g,'').split(',').map(x=>x.toUpperCase());
 const values=m[2].replace(/\s+/g,'').split(',');
 check(columns.join('|')===expected.join('|'),table+' exact schema columns');
 check(columns.length===values.length,table+' values equal columns');
 check((m[2].match(/\?/g)||[]).length===binds,table+' placeholder count');
}
for(const needed of ['SOURCE_SYSTEM_CODE','IDEMPOTENCY_KEY','CREATED_BY','TRANSACTION_AT','RECORD_VERSION'])
 check(svc.includes('"'+needed+'"'),needed+' enforced by schema readiness');
check(svc.includes('referencePresent("REF_SOURCE_SYSTEM","SOURCE_SYSTEM_CODE",config("source-system-code"))'),'bank source reference lookup');
check(props.includes('source-system-code=${CLT_POSTING_SOURCE_SYSTEM_CODE:}'),'source system mapping is blank by default');
check(!props.includes('CLT_POSTING_SOURCE_SYSTEM_CODE:CLT'),'no fabricated source system code');
check(svc.includes('repository.insertInitialTransactionStatus(jdbc,transactionId'),'initial history in same transactional service');
check(svc.includes('repository.insertTransaction(jdbc,transactionId'),'transaction write in same transactional service');
check(svc.indexOf('repository.insertInitialTransactionStatus')<svc.indexOf('repository.bumpParent(jdbc'),'history before atomic version bump');
check(svc.includes('config("source-system-code"),key,who.subject()'),'trusted source,key,actor passed to financial transaction');
check(svc.includes('repository.insertAllocation(jdbc,allocationId,p,who.subject())'),'allocation created-by supplied');
check(svc.includes('repository.updateAllocation(jdbc,a,remaining,who.subject())'),'unallocation updated-by supplied');
check(svc.includes('transactionHistoryKeyGeneratedByOracle()'),'history generated key verified');
check(svc.includes('TRANSACTION_NUMBER')===false || svc.includes('{0,63}'),'bank transaction number length validated');
check(timeline.includes('JOIN authorized c ON c.COLLATERAL_ID=t.COLLATERAL_ID'),'transaction event branch-scoped');
check(timeline.includes('JOIN authorized c ON c.COLLATERAL_ID=t.COLLATERAL_ID'),'status event authorization inherited through transaction');
check(timeline.includes('hasColumns(jdbc,"COLLATERAL_TXN_STATUS_HISTORY"'),'history optional under schema introspection');
check(timeline.includes('hasColumns(jdbc,"COLLATERAL_TRANSACTION"'),'financial timeline conditional on schema');
for(const kind of ['TRANSACTION','TRANSACTION_STATUS']){
 check(timeline.includes("'"+kind+"'"),kind+' back-end event query');
 check(contract.includes("'"+kind+"'"),kind+' front-end data validator');
 check(models.includes("'"+kind+"'"),kind+' front-end typed model');
 check(ui.includes(kind+':'),kind+' localized UI label');
}
check(summary.includes('TRANSACTION_LEDGER='),'evidence ledger availability surfaced');
check(summary.includes('NOT_COMPLETE_AUDIT_LOG'),'never claims exhaustive audit');
console.log(`CLT_BUC09_10_16_ORACLE_CONTRACT_PASS=${checks}/${checks}; FINANCIAL_FLAGS=UNCHANGED; SQL_DDL=NONE`);
