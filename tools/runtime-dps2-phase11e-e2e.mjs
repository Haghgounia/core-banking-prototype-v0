const base=(process.env.CORE_BANKING_BASE_URL||'http://127.0.0.1:8091').replace(/\/$/,'');
const uid=()=>globalThis.crypto?.randomUUID?.()??`${Date.now()}-${Math.random().toString(16).slice(2)}`;
async function req(method,path,body,user='deposit.operator'){
 const id=uid(); const h={'Content-Type':'application/json','X-User-Id':user,'X-Correlation-Id':id,'X-Idempotency-Key':id};
 const r=await fetch(base+path,{method,headers:h,body:body===undefined?undefined:JSON.stringify(body)});const t=await r.text();let data;try{data=t?JSON.parse(t):null}catch{data=t}
 if(!r.ok)throw new Error(`${method} ${path} -> ${r.status}: ${t}`);return data;
}
try{
 const search=await req('GET','/api/v1/deposit-accounts');
 const candidates=(search.items||[]).filter(x=>x.accountStatusCode==='ACTIVE');
 if(!candidates.length)throw new Error('No ACTIVE deposit account available for Phase 11E E2E.');
 let account=null,workflow=null;
 for(const c of candidates){const w=await req('GET',`/api/v1/deposit-accounts/${c.accountId}/closure-workflow`);if(!(w.closures||[]).some(x=>['REQUESTED','APPROVED'].includes(x.closureStatusCode))&&!(w.reopenings||[]).some(x=>['REQUESTED','APPROVED'].includes(x.reopenStatusCode))){account=c;workflow=w;break;}}
 if(!account)throw new Error('No ACTIVE account without open closure/reopening workflow.');
 const accountId=account.accountId;
 const closure=await req('POST',`/api/v1/deposit-accounts/${accountId}/closures`,{closureTypeCode:'CUSTOMER_REQUEST',reasonCode:'NO_LONGER_NEEDED',settlementAccountReference:`E2E-SETTLEMENT-${uid().slice(0,8)}`,approverUserId:'deposit.approver',orgUnitCode:'HQ'});
 const closureId=closure.closure.accountClosureId;
 if(closure.closure.closureStatusCode!=='REQUESTED')throw new Error('Closure did not enter REQUESTED.');
 if((closure.closure.checks||[]).some(x=>x.resultStatusCode!=='PASS'))throw new Error('Closure pre-checks did not all PASS.');
 const approved=await req('POST',`/api/v1/deposit-accounts/${accountId}/closures/${closureId}/approve`,{},'deposit.approver');
 if(approved.closure.closureStatusCode!=='APPROVED'||approved.closure.approvalStatusCode!=='APPROVED')throw new Error('Closure approval failed.');
 const executed=await req('POST',`/api/v1/deposit-accounts/${accountId}/closures/${closureId}/execute`,{});
 if(executed.closure.closureStatusCode!=='EXECUTED')throw new Error('Closure did not execute.');
 let details=await req('GET',`/api/v1/deposit-accounts/${accountId}`);
 if(details.account.accountStatusCode!=='CLOSED')throw new Error(`Expected CLOSED, got ${details.account.accountStatusCode}`);
 if(Number(details.balance.ledgerBalance)!==0||Number(details.balance.availableBalance)!==0)throw new Error('Closed account balance is not zero.');
 const reopen=await req('POST',`/api/v1/deposit-accounts/${accountId}/reopenings`,{reopenReasonCode:'CUSTOMER_REQUEST',approverUserId:'deposit.approver',orgUnitCode:'HQ'});
 const reopeningId=reopen.reopening.accountReopeningId;
 const reopenApproved=await req('POST',`/api/v1/deposit-accounts/${accountId}/reopenings/${reopeningId}/approve`,{},'deposit.approver');
 if(reopenApproved.reopening.reopenStatusCode!=='APPROVED')throw new Error('Reopening approval failed.');
 const reopenExecuted=await req('POST',`/api/v1/deposit-accounts/${accountId}/reopenings/${reopeningId}/execute`,{});
 if(reopenExecuted.reopening.reopenStatusCode!=='EXECUTED')throw new Error('Reopening did not execute.');
 details=await req('GET',`/api/v1/deposit-accounts/${accountId}`);
 if(details.account.accountStatusCode!=='ACTIVE')throw new Error(`Expected ACTIVE after reopen, got ${details.account.accountStatusCode}`);
 const types=(details.lifecycleEvents||[]).map(x=>x.eventTypeCode);
 if(!types.includes('CLOSE')||!types.includes('REOPEN'))throw new Error('CLOSE/REOPEN lifecycle events not found.');
 console.log(`PHASE11E_RUNTIME_E2E_ACCOUNT=${accountId}`);
 console.log(`PHASE11E_RUNTIME_E2E_CLOSURE=${closureId}`);
 console.log(`PHASE11E_RUNTIME_E2E_REOPENING=${reopeningId}`);
 console.log('PHASE11E_RUNTIME_E2E_PASS');
}catch(e){console.error('PHASE11E_RUNTIME_E2E_FAIL');console.error(e);process.exit(1)}
