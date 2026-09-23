const base=(process.env.CORE_BANKING_BASE_URL||'http://127.0.0.1:8091').replace(/\/$/,'');
const assert=(c,m)=>{if(!c)throw new Error(m)};
const key=(label)=>`phase11c-${label}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
async function req(method,url,body,operationKey){
  const headers={'content-type':'application/json','X-User-Id':'phase11c.qa'};
  if(operationKey){headers['X-Idempotency-Key']=operationKey;headers['X-Correlation-Id']=operationKey;}
  const r=await fetch(base+url,{method,headers,body:body===undefined?undefined:JSON.stringify(body)});
  const text=await r.text();let data;try{data=text?JSON.parse(text):null}catch{data=text}
  if(!r.ok)throw new Error(`${method} ${url} -> ${r.status}: ${typeof data==='string'?data:JSON.stringify(data)}`);return data;
}
try{
  const health=await req('GET','/actuator/health');assert(health.status==='UP','Application health is not UP');
  const search=await req('GET','/api/v1/deposit-accounts?status=ACTIVE&offset=0&limit=20');assert(search.items?.length,'No ACTIVE deposit account found for Phase 11C runtime test');
  const requested=process.argv[2]?Number(process.argv[2]):null;const row=requested?search.items.find(x=>x.accountId===requested):search.items[0];assert(row,`Requested ACTIVE account ${requested} not found`);const id=row.accountId;
  let d=await req('GET',`/api/v1/deposit-accounts/${id}`);assert(d.account.accountStatusCode==='ACTIVE','Start status must be ACTIVE');

  let r=await req('POST',`/api/v1/deposit-accounts/${id}/lifecycle/suspend`,{expectedRecordVersion:d.account.recordVersion,reasonCode:'OPERATIONAL_CONTROL'},key('suspend'));d=r.account;assert(d.account.accountStatusCode==='SUSPENDED','SUSPEND did not produce SUSPENDED');
  r=await req('POST',`/api/v1/deposit-accounts/${id}/lifecycle/reactivate`,{expectedRecordVersion:d.account.recordVersion,reasonCode:'NO_LONGER_NEEDED'},key('reactivate1'));d=r.account;assert(d.account.accountStatusCode==='ACTIVE','REACTIVATE from SUSPENDED did not produce ACTIVE');

  r=await req('POST',`/api/v1/deposit-accounts/${id}/holds`,{holdTypeCode:'FULL',holdReasonCode:'CUSTOMER_REQUEST',sourceReference:'PHASE11C_E2E',originSystemCode:'CORE_BANKING',originModuleCode:'ACCOUNT_SERVICING',originExecutionModeCode:'USER',releasePolicyCode:'ORIGIN_OR_AUTHORITY'},key('hold'));d=r.account;const hid=r.accountHoldId;assert(hid,'Hold id missing');assert(d.holds.some(x=>x.accountHoldId===hid&&x.holdStatusCode==='ACTIVE'),'Created hold is not ACTIVE');assert(d.account.accountStatusCode==='ACTIVE','Hold changed account status');
  r=await req('POST',`/api/v1/deposit-accounts/${id}/holds/${hid}/release`,{reasonCode:'CUSTOMER_REQUEST_FULFILLED',actionSourceSystemCode:'CORE_BANKING',actionSourceModuleCode:'ACCOUNT_SERVICING',actionExecutionModeCode:'USER'},key('release'));d=r.account;assert(d.holds.some(x=>x.accountHoldId===hid&&x.holdStatusCode==='RELEASED'),'Hold release did not produce RELEASED');assert(d.account.accountStatusCode==='ACTIVE','Hold release changed account status');

  r=await req('POST',`/api/v1/deposit-accounts/${id}/lifecycle/mark-dormant`,{expectedRecordVersion:d.account.recordVersion,reasonCode:'INACTIVITY_POLICY'},key('dormant'));d=r.account;assert(d.account.accountStatusCode==='DORMANT','MARK_DORMANT did not produce DORMANT');assert(d.account.dormancyDate,'Dormancy date was not set');
  r=await req('POST',`/api/v1/deposit-accounts/${id}/lifecycle/reactivate`,{expectedRecordVersion:d.account.recordVersion,reasonCode:'NO_LONGER_NEEDED'},key('reactivate2'));d=r.account;assert(d.account.accountStatusCode==='ACTIVE','REACTIVATE from DORMANT did not produce ACTIVE');assert(!d.account.dormancyDate,'Dormancy date was not cleared on reactivation');

  assert(d.statusHistory.length>=4,'Status history entries are missing');assert(d.holdHistory.some(x=>x.accountHoldId===hid&&x.actionCode==='CREATE'),'Hold CREATE history missing');assert(d.holdHistory.some(x=>x.accountHoldId===hid&&x.actionCode==='RELEASE'),'Hold RELEASE history missing');
  console.log(`PHASE11C_RUNTIME_E2E_ACCOUNT=${id}`);console.log(`PHASE11C_RUNTIME_E2E_HOLD=${hid}`);console.log('PHASE11C_RUNTIME_E2E_PASS');
}catch(e){console.error('PHASE11C_RUNTIME_E2E_FAIL');console.error(e?.stack||e);process.exit(1)}
