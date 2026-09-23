const base=(process.env.CORE_BANKING_BASE_URL||'http://127.0.0.1:8091').replace(/\/$/,'');
const assert=(c,m)=>{if(!c)throw new Error(m)};
const token=(label)=>`phase11d-${label}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
async function req(method,url,body,operationKey){
  const headers={'content-type':'application/json','X-User-Id':'phase11d.qa'};
  if(operationKey){headers['X-Idempotency-Key']=operationKey;headers['X-Correlation-Id']=operationKey;}
  const r=await fetch(base+url,{method,headers,body:body===undefined?undefined:JSON.stringify(body)});
  const text=await r.text();let data;try{data=text?JSON.parse(text):null}catch{data=text}
  if(!r.ok)throw new Error(`${method} ${url} -> ${r.status}: ${typeof data==='string'?data:JSON.stringify(data)}`);
  return data;
}
const n=v=>Number(v??0);
try{
  const health=await req('GET','/actuator/health');assert(health.status==='UP','Application health is not UP');
  const search=await req('GET','/api/v1/deposit-accounts?status=ACTIVE&offset=0&limit=50');assert(search.items?.length,'No ACTIVE deposit account found for Phase 11D runtime test');
  const requested=process.argv[2]?Number(process.argv[2]):null;
  let detail=null;
  for(const row of search.items){
    if(requested && row.accountId!==requested)continue;
    const d=await req('GET',`/api/v1/deposit-accounts/${row.accountId}`);
    const blocking=d.holds?.some(h=>h.holdStatusCode==='ACTIVE'&&['FULL','DEBIT_ONLY','CREDIT_ONLY'].includes(h.holdTypeCode));
    if(!blocking && n(d.balance?.ledgerBalance)>=0 && n(d.balance?.availableBalance)>=0){detail=d;break;}
  }
  assert(detail,requested?`Requested ACTIVE account ${requested} is not suitable for 11D posting probe`:'No suitable ACTIVE account without blocking holds found');
  const id=detail.account.accountId,currency=detail.balance.currencyCode;
  const initialLedger=n(detail.balance.ledgerBalance),initialBlocked=n(detail.balance.blockedAmount),initialPending=n(detail.balance.pendingDebitAmount),initialAvailable=n(detail.balance.availableBalance);
  const amount=100,holdAmount=25,reserveAmount=25;

  const creditKey=token('credit');
  const creditBody={debitCreditCode:'CREDIT',amount,currencyCode:currency,postingReference:`P11D-C-${id}-${Date.now()}`,sourceEntityType:'PHASE11D_RUNTIME_E2E',sourceEntityId:id};
  let r=await req('POST',`/api/v1/deposit-accounts/${id}/balance/postings`,creditBody,creditKey);
  const creditEntry=r.subledgerEntryId;assert(creditEntry,'Credit subledger entry id missing');
  assert(n(r.state.balance.ledgerBalance)===initialLedger+amount,'Credit did not increase ledger balance atomically');
  assert(n(r.state.balance.pendingDebitAmount)===initialPending,'Credit changed pending debit unexpectedly');
  const replay=await req('POST',`/api/v1/deposit-accounts/${id}/balance/postings`,creditBody,creditKey);
  assert(replay.idempotentReplay===true,'Posting idempotency replay not detected');
  assert(replay.subledgerEntryId===creditEntry,'Idempotent replay returned different subledger entry');

  r=await req('POST',`/api/v1/deposit-accounts/${id}/holds`,{
    holdTypeCode:'PARTIAL',holdAmount,currencyCode:currency,holdReasonCode:'CUSTOMER_REQUEST',sourceReference:'PHASE11D_E2E',
    originSystemCode:'CORE_BANKING',originModuleCode:'ACCOUNT_SERVICING',originExecutionModeCode:'USER',releasePolicyCode:'ORIGIN_OR_AUTHORITY'
  },token('hold'));
  const holdId=r.accountHoldId;assert(holdId,'Partial hold id missing');
  assert(n(r.account.balance.ledgerBalance)===initialLedger+amount,'Partial hold changed ledger balance');
  assert(n(r.account.balance.blockedAmount)===initialBlocked+holdAmount,'Partial hold did not increase blocked amount');
  assert(n(r.account.balance.availableBalance)===Math.max(0,n(replay.state.balance.availableBalance)-holdAmount),'Partial hold did not reduce available balance');
  r=await req('POST',`/api/v1/deposit-accounts/${id}/holds/${holdId}/release`,{
    reasonCode:'CUSTOMER_REQUEST_FULFILLED',actionSourceSystemCode:'CORE_BANKING',actionSourceModuleCode:'ACCOUNT_SERVICING',actionExecutionModeCode:'USER'
  },token('hold-release'));
  assert(n(r.account.balance.ledgerBalance)===initialLedger+amount,'Partial hold release changed ledger balance');
  assert(n(r.account.balance.blockedAmount)===initialBlocked,'Partial hold release did not restore blocked amount');
  assert(n(r.account.balance.availableBalance)===n(replay.state.balance.availableBalance),'Partial hold release did not restore available balance');

  const reservationKey=token('reserve');
  r=await req('POST',`/api/v1/deposit-accounts/${id}/balance/reservations`,{
    reservationReference:`P11D-R-${id}-${Date.now()}`,reservationTypeCode:'OTHER',amount:reserveAmount,currencyCode:currency,
    ownerEntityType:'PHASE11D_RUNTIME_E2E',ownerEntityId:id
  },reservationKey);
  const reservationId=r.balanceReservationId;assert(reservationId,'Reservation id missing');
  assert(n(r.state.balance.ledgerBalance)===initialLedger+amount,'Reservation changed ledger balance');
  assert(n(r.state.balance.pendingDebitAmount)===initialPending+reserveAmount,'Reservation did not increase pending debit');
  const availableAfterReserve=n(r.state.balance.availableBalance);
  assert(availableAfterReserve===Math.max(0,n(replay.state.balance.availableBalance)-reserveAmount),'Reservation did not reduce available balance');

  r=await req('POST',`/api/v1/deposit-accounts/${id}/balance/reservations/${reservationId}/release`,{reasonCode:'RUNTIME_RELEASE'},token('release'));
  assert(n(r.state.balance.ledgerBalance)===initialLedger+amount,'Reservation release changed ledger balance');
  assert(n(r.state.balance.pendingDebitAmount)===initialPending,'Reservation release did not restore pending debit');

  const debitBody={debitCreditCode:'DEBIT',amount,currencyCode:currency,postingReference:`P11D-D-${id}-${Date.now()}`,sourceEntityType:'PHASE11D_RUNTIME_E2E',sourceEntityId:id};
  r=await req('POST',`/api/v1/deposit-accounts/${id}/balance/postings`,debitBody,token('debit'));
  const debitEntry=r.subledgerEntryId;assert(debitEntry,'Debit subledger entry id missing');
  assert(n(r.state.balance.ledgerBalance)===initialLedger,'Debit did not restore original ledger balance');
  assert(n(r.state.balance.pendingDebitAmount)===initialPending,'Final pending debit differs from initial');
  assert(n(r.state.balance.blockedAmount)===initialBlocked,'Final blocked amount differs from initial');
  assert(n(r.state.balance.availableBalance)===initialAvailable,'Net-zero posting probe did not restore available balance');
  assert(r.state.subledgerEntries.some(e=>e.subledgerEntryId===creditEntry),'Credit entry missing from subledger view');
  assert(r.state.subledgerEntries.some(e=>e.subledgerEntryId===debitEntry),'Debit entry missing from subledger view');
  assert(r.state.reservations.some(x=>x.balanceReservationId===reservationId&&x.reservationStatusCode==='RELEASED'),'Reservation release not visible');
  assert(r.state.balance.availableBalance>=0,'Available balance became negative');

  console.log(`PHASE11D_RUNTIME_E2E_ACCOUNT=${id}`);
  console.log(`PHASE11D_RUNTIME_E2E_CREDIT_ENTRY=${creditEntry}`);
  console.log(`PHASE11D_RUNTIME_E2E_HOLD=${holdId}`);
  console.log(`PHASE11D_RUNTIME_E2E_RESERVATION=${reservationId}`);
  console.log(`PHASE11D_RUNTIME_E2E_DEBIT_ENTRY=${debitEntry}`);
  console.log('PHASE11D_RUNTIME_E2E_PASS');
}catch(e){console.error('PHASE11D_RUNTIME_E2E_FAIL');console.error(e?.stack||e);process.exit(1)}
