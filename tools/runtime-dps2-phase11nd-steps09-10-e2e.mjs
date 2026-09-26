const base=(process.env.CORE_BANKING_BASE_URL||'http://127.0.0.1:8091').replace(/\/$/,'');
const uid=()=>`${Date.now()}-${Math.random().toString(16).slice(2,10)}`;
const today=()=>new Date().toISOString().slice(0,10);
async function req(method,path,body,actor='deposit.operator',idem=`11nd-0910-${uid()}`){
  const h={'Content-Type':'application/json','X-User-Id':actor,'X-Correlation-Id':idem};
  if(method!=='GET')h['X-Idempotency-Key']=idem;
  const r=await fetch(base+path,{method,headers:h,body:body===undefined?undefined:JSON.stringify(body)});
  const txt=await r.text();let data=null;try{data=txt?JSON.parse(txt):null}catch{data=txt}
  if(!r.ok)throw new Error(`${method} ${path} -> ${r.status}: ${txt}`);return data;
}
async function main(){
  const accounts=(await req('GET','/api/v1/deposit-accounts?status=ACTIVE&limit=100')).items||[];
  if(!accounts.length)throw new Error('PHASE11ND_STEPS09_10_NEEDS_ACTIVE_ACCOUNT');
  let account=null,detail=null,parties=[];
  for(const a of accounts){
    const d=await req('GET',`/api/v1/deposit-accounts/${a.accountId}`);
    const ps=(d.parties||[]).filter(x=>x.statusCode==='ACTIVE');
    if(ps.length){account=a;detail=d;parties=ps;break;}
  }
  if(!account)throw new Error('PHASE11ND_STEPS09_10_NEEDS_ACTIVE_ACCOUNT_PARTY');
  const aid=account.accountId,party=parties[0],s=String(Date.now()).slice(-8);

  // Step 09 - Deposit Account Services only.
  let w=await req('POST',`/api/v1/deposit-accounts/${aid}/inquiries`,{inquiryTypeCode:'ACCOUNT_SUMMARY',channelCode:'API',requestedByPartyId:party.partyId});
  const inq=w.services.inquiries[0]; if(!inq||inq.resultStatusCode!=='SUCCESS')throw new Error('STEP09_INQUIRY_TRACE_MISSING');
  w=await req('POST',`/api/v1/deposit-accounts/${aid}/confirmations`,{confirmationTypeCode:'BALANCE_STATUS',deliveryChannelCode:'BRANCH'});
  const conf=w.services.confirmations[0]; if(!conf||conf.currencyCode!==account.currencyCode)throw new Error('STEP09_CONFIRMATION_CURRENCY_MISMATCH');
  const email=(detail.contacts||[]).find(x=>x.contactTypeCode==='EMAIL'&&!x.validTo); const ch=email?'EMAIL':'PORTAL',target=email?email.contactValue:'PORTAL';
  w=await req('POST',`/api/v1/deposit-accounts/${aid}/notification-preferences`,{eventCode:`RUNTIME_${s}`,channelCode:ch,deliveryTarget:target,enabled:true,validFrom:today(),validTo:null});
  if(!(w.services.notificationPreferences||[]).some(x=>x.eventCode===`RUNTIME_${s}`&&x.enabled))throw new Error('STEP09_NOTIFICATION_PREFERENCE_MISSING');
  w=await req('POST',`/api/v1/deposit-accounts/${aid}/notification-events`,{eventCode:`RUNTIME_${s}`,eventReference:`EV-${s}`,channelCode:ch,deliveryTarget:target,providerReference:`PROV-${s}`});
  if(!(w.services.notificationEvents||[]).some(x=>x.eventReference===`EV-${s}`&&x.deliveryStatusCode==='DELIVERED'))throw new Error('STEP09_NOTIFICATION_EVENT_MISSING');
  w=await req('POST',`/api/v1/deposit-accounts/${aid}/api-access`,{clientReference:`RT-${s}`,apiScopeCode:'ACCOUNT_READ',authorizedPartyId:party.partyId,consentReference:`CONS-${s}`,validFrom:null,validTo:null});
  const api=(w.services.apiAccesses||[]).find(x=>x.clientReference===`RT-${s}`&&x.statusCode==='ACTIVE'); if(!api)throw new Error('STEP09_API_ACCESS_MISSING');
  w=await req('POST',`/api/v1/deposit-accounts/${aid}/api-access/${api.accountApiAccessId}/revoke`,{});
  if(!(w.services.apiAccesses||[]).some(x=>x.accountApiAccessId===api.accountApiAccessId&&x.statusCode==='REVOKED'))throw new Error('STEP09_API_ACCESS_REVOKE_MISSING');

  // Step 10 - Party Access & Payment Instruments only.
  w=await req('POST',`/api/v1/deposit-accounts/${aid}/signature-rules`,{signatureRuleCode:`RT${s}`.slice(0,30),minSignatureCount:1,validFrom:today(),validTo:null});
  const sig=(w.partyAccess.signatureRules||[])[0]; if(!sig)throw new Error('STEP10_SIGNATURE_RULE_MISSING');
  let delegationMarker='DEFERRED_NO_SECOND_ACTIVE_ACCOUNT_PARTY';
  if(parties.length>1){
    w=await req('POST',`/api/v1/deposit-accounts/${aid}/delegations`,{grantorPartyId:parties[0].partyId,delegatePartyId:parties[1].partyId,delegationTypeCode:'POA',authorityScopeCode:'ALL_OPERATIONS',maxAmount:null,documentReference:`POA-${s}`,validFrom:today(),validTo:null});
    const d=(w.partyAccess.delegations||[]).find(x=>x.documentReference===`POA-${s}`&&x.statusCode==='ACTIVE');
    if(!d)throw new Error('STEP10_DELEGATION_MISSING'); delegationMarker=String(d.accountDelegationId);
  }
  w=await req('POST',`/api/v1/deposit-accounts/${aid}/authorized-users`,{partyId:party.partyId,accessRoleCode:`RUNTIME_${s}`.slice(0,40),channelScopeCode:'API',validFrom:today(),validTo:null});
  const au=(w.partyAccess.authorizedUsers||[])[0]; if(!au||au.partyId!==party.partyId)throw new Error('STEP10_AUTHORIZED_USER_MISSING');
  w=await req('POST',`/api/v1/deposit-accounts/${aid}/beneficiaries`,{beneficiaryPartyId:party.partyId,beneficiaryReference:null,beneficiaryTypeCode:`RT${s}`.slice(0,30),sharePercent:100,validFrom:today(),validTo:null});
  const ben=(w.partyAccess.beneficiaries||[])[0]; if(!ben)throw new Error('STEP10_BENEFICIARY_MISSING');
  w=await req('POST',`/api/v1/deposit-accounts/${aid}/payment-instruments`,{instrumentTypeCode:'DEBIT_CARD',instrumentReference:`PI-${s}`,holderPartyId:party.partyId,issuedAt:null,expiresAt:null});
  const pi=(w.partyAccess.paymentInstruments||[])[0]; if(!pi||pi.holderPartyId!==party.partyId)throw new Error('STEP10_PAYMENT_INSTRUMENT_MISSING');

  console.log(`PHASE11ND_RUNTIME_STEPS09_10_ACCOUNT=${aid}`);
  console.log(`PHASE11ND_RUNTIME_STEP09_INQUIRY=${inq.accountInquiryId}`);
  console.log(`PHASE11ND_RUNTIME_STEP09_CONFIRMATION=${conf.accountConfirmationId}`);
  console.log(`PHASE11ND_RUNTIME_STEP09_API_ACCESS=${api.accountApiAccessId}`);
  console.log(`PHASE11ND_RUNTIME_STEP10_SIGNATURE_RULE=${sig.accountSignatureRuleId}`);
  console.log(`PHASE11ND_RUNTIME_STEP10_DELEGATION=${delegationMarker}`);
  console.log(`PHASE11ND_RUNTIME_STEP10_AUTHORIZED_USER=${au.accountAuthorizedUserId}`);
  console.log(`PHASE11ND_RUNTIME_STEP10_BENEFICIARY=${ben.accountBeneficiaryId}`);
  console.log(`PHASE11ND_RUNTIME_STEP10_PAYMENT_INSTRUMENT=${pi.accountPaymentInstrumentId}`);
  console.log('PHASE11ND_RUNTIME_STEPS09_10_PASS');
}
main().catch(e=>{console.error('PHASE11ND_RUNTIME_STEPS09_10_FAIL');console.error(e);process.exit(1)});
