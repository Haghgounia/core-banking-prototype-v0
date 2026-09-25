const base=(process.env.CORE_BANKING_BASE_URL||'http://127.0.0.1:8091').replace(/\/$/,'');
const uid=()=>`${Date.now()}-${Math.random().toString(16).slice(2)}`;
async function req(method,path,body,actor='deposit.operator',idem){const h={'Content-Type':'application/json','X-User-Id':actor};if(idem){h['X-Idempotency-Key']=idem;h['X-Correlation-Id']=idem}const r=await fetch(base+path,{method,headers:h,body:body===undefined?undefined:JSON.stringify(body)});const t=await r.text();if(!r.ok){const e=new Error(`${method} ${path} -> ${r.status}: ${t}`);e.status=r.status;e.responseText=t;throw e}return t?JSON.parse(t):null;}
async function controls(id){return req('GET',`/api/v1/deposit-accounts/${id}/servicing-controls`)}
async function detail(id){return req('GET',`/api/v1/deposit-accounts/${id}`)}
async function main(){
  const pending=(await req('GET','/api/v1/deposit-accounts?status=PENDING_ACTIVATION&limit=100')).items||[];
  if(!pending.length)throw new Error('PHASE11J_NO_PENDING_ACCOUNT_FOR_ACTIVATION_QUALIFICATION');
  const aid=pending[0].accountId;
  const key=`11j-activation-hotfix-${uid()}`;
  const run=await req('POST',`/api/v1/deposit-accounts/${aid}/activation-runs`,{triggerCode:'MANUAL_RECHECK'},'deposit.operator',key);
  const c=await controls(aid);
  if(!c.latestActivationRun||c.latestActivationRun.activationRunId!==run.activationRunId||(c.latestActivationRun.checks||[]).length===0)throw new Error('ACTIVATION_RUN_EVIDENCE_FAIL');
  if(c.latestActivationRun.ready){
    await req('POST',`/api/v1/deposit-accounts/${aid}/activation-runs/${run.activationRunId}/execute`,{},'deposit.operator',`11j-activate-hotfix-${uid()}`);
    const d=await detail(aid);if(d.account.accountStatusCode!=='ACTIVE')throw new Error('ACTIVATION_EXECUTE_FAIL');
    console.log(`PHASE11J_RUNTIME_ACTIVATION=${run.activationRunId}:EXECUTED`);
  }else{
    console.log(`PHASE11J_RUNTIME_ACTIVATION=${run.activationRunId}:BLOCKED_BY_SOURCE_READINESS`);
  }
  console.log(`PHASE11J_RUNTIME_ACTIVATION_ACCOUNT=${aid}`);
  console.log('PHASE11J_RUNTIME_ACTIVATION_PASS');
}
main().catch(e=>{console.error('PHASE11J_RUNTIME_ACTIVATION_FAIL');console.error(e);process.exit(1)});
