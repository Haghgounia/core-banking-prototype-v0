const base=(process.env.CORE_BANKING_BASE_URL||'http://127.0.0.1:8091').replace(/\/$/,'');
const uid=()=>`${Date.now()}-${Math.random().toString(16).slice(2)}`;
const today=()=>new Date().toISOString().slice(0,10);
const plusHours=(hours)=>new Date(Date.now()+hours*3600_000).toISOString();
const text=(v)=>v==null?'':String(v).trim();
const number=(v)=>{const n=Number(v);return Number.isFinite(n)?n:null};
const truthyFlag=(v)=>v==null||v===''||v===true||v===1||text(v).toUpperCase()==='Y'||text(v).toUpperCase()==='ACTIVE';
async function req(method,path,body,actor='deposit.operator',idem){const h={'Content-Type':'application/json','X-User-Id':actor};if(idem){h['X-Idempotency-Key']=idem;h['X-Correlation-Id']=idem}const r=await fetch(base+path,{method,headers:h,body:body===undefined?undefined:JSON.stringify(body)});const t=await r.text();if(!r.ok)throw new Error(`${method} ${path} -> ${r.status}: ${t}`);return t?JSON.parse(t):null;}
const createGateChecks=[
  ['CUSTOMER_IDENTITY','COMPLIANCE','PRE_ACCOUNT_CREATION'],['MOBILE_OWNERSHIP','COMPLIANCE','PRE_ACCOUNT_CREATION'],
  ['LEGAL_CAPACITY','COMPLIANCE','PRE_ACCOUNT_CREATION'],['KYC_CDD','COMPLIANCE','PRE_ACCOUNT_CREATION'],
  ['PEP_SANCTIONS','COMPLIANCE','PRE_ACCOUNT_CREATION'],['CUSTOMER_RISK','COMPLIANCE','PRE_APPROVAL'],
  ['EXPECTED_ACTIVITY','COMPLIANCE','PRE_APPROVAL'],['ACCOUNT_COUNT_STATUS','INQUIRY','PRE_ACCOUNT_CREATION'],
  ['PRODUCT_ELIGIBILITY','PRODUCT_RULE','PRE_ACCOUNT_CREATION'],['DOCUMENTS','DOCUMENT','PRE_ACCOUNT_CREATION'],
  ['INQUIRIES','INQUIRY','PRE_ACCOUNT_CREATION'],['SIGNATORY_AUTHORITY','AUTHORITY','PRE_ACCOUNT_CREATION'],
  ['TERMS_ACCEPTANCE','CONSENT','PRE_ACCOUNT_CREATION'],['SHARIA_CONTRACT','PRODUCT_RULE','PRE_ACCOUNT_CREATION'],
  ['TAX_PROFILE','INQUIRY','PRE_ACCOUNT_CREATION'],['DUPLICATE_REQUEST','OPERATIONAL','PRE_ACCOUNT_CREATION']
];
function addTerm(start,value,unit){
  const d=new Date(`${start}T00:00:00.000Z`);
  switch(text(unit).toUpperCase()){
    case 'DAY': d.setUTCDate(d.getUTCDate()+value);break;
    case 'MONTH': d.setUTCMonth(d.getUTCMonth()+value);break;
    case 'YEAR': d.setUTCFullYear(d.getUTCFullYear()+value);break;
    default: throw new Error(`UNSUPPORTED_ALLOWED_TERM_UNIT:${unit}`);
  }
  return d.toISOString().slice(0,10);
}
function activeNow(row){
  if(Object.hasOwn(row,'IS_ACTIVE')&&!truthyFlag(row.IS_ACTIVE))return false;
  if(Object.hasOwn(row,'IS_CURRENT')&&!truthyFlag(row.IS_CURRENT))return false;
  const now=today();
  const from=text(row.VALID_FROM||row.EFFECTIVE_FROM_DATE).slice(0,10);
  const to=text(row.VALID_TO||row.EFFECTIVE_TO_DATE).slice(0,10);
  return (!from||from<=now)&&(!to||to>=now);
}
async function pdlDescriptor(table){return req('GET',`/api/v1/product-builder/tables/${encodeURIComponent(table)}/descriptor`)}
async function pdlRows(table,filterColumn,filterValue){
  const q=new URLSearchParams({page:'0',size:'200'});if(filterColumn)q.set('filterColumn',filterColumn);if(filterValue!=null)q.set('filterValue',String(filterValue));
  return req('GET',`/api/v1/product-builder/tables/${encodeURIComponent(table)}/rows?${q.toString()}`);
}
function columnName(descriptor,candidates){const names=new Set((descriptor.columns||[]).map(c=>text(c.name).toUpperCase()));return candidates.find(c=>names.has(c))||null}
function allowedTermFromRow(row,descriptor){
  const idColumn=columnName(descriptor,['ALLOWED_TERM_ID'])||text(descriptor.primaryKeyColumn).toUpperCase();
  const allowedTermId=number(row[idColumn]);
  let termValue=null,termUnitCode='';
  const valueColumn=columnName(descriptor,['TERM_VALUE','DURATION_VALUE','TERM_LENGTH_VALUE']);
  const unitColumn=columnName(descriptor,['TERM_UNIT_CODE','DURATION_UNIT_CODE','TERM_LENGTH_UNIT_CODE']);
  if(valueColumn&&unitColumn){termValue=number(row[valueColumn]);termUnitCode=text(row[unitColumn]).toUpperCase()}
  else if(Object.hasOwn(row,'TERM_DAYS')){termValue=number(row.TERM_DAYS);termUnitCode='DAY'}
  else if(Object.hasOwn(row,'TERM_MONTHS')){termValue=number(row.TERM_MONTHS);termUnitCode='MONTH'}
  else if(Object.hasOwn(row,'TERM_YEARS')){termValue=number(row.TERM_YEARS);termUnitCode='YEAR'}
  const codeColumn=columnName(descriptor,['TERM_CODE','ALLOWED_TERM_CODE','CODE']);
  const termCode=codeColumn?text(row[codeColumn]):'';
  if(!(allowedTermId>0))throw new Error(`PDL_ALLOWED_TERM_ID_MISSING:${JSON.stringify(row)}`);
  if(!(termValue>0)||!['DAY','MONTH','YEAR'].includes(termUnitCode))throw new Error(`PDL_ALLOWED_TERM_DURATION_UNRESOLVED:${JSON.stringify(row)}`);
  if(!termCode)throw new Error(`PDL_ALLOWED_TERM_CODE_MISSING:${JSON.stringify(row)}`);
  return {allowedTermId,termCode,termValue,termUnitCode};
}
async function resolveAllowedTerm(productVersionId){
  const ruleDescriptor=await pdlDescriptor('DEPOSIT_PRODUCT_TERM_RULE');
  const versionColumn=columnName(ruleDescriptor,['PRODUCT_VERSION_ID','DEPOSIT_PRODUCT_VERSION_ID']);
  if(!versionColumn)throw new Error('PDL_TERM_RULE_PRODUCT_VERSION_COLUMN_NOT_FOUND');
  const rules=(await pdlRows('DEPOSIT_PRODUCT_TERM_RULE',versionColumn,productVersionId)).items||[];
  const activeRules=rules.filter(activeNow);
  if(!activeRules.length)throw new Error(`NO_ACTIVE_PDL_TERM_RULE_FOR_PRODUCT_VERSION=${productVersionId}`);
  const allowedDescriptor=await pdlDescriptor('DEPOSIT_PRODUCT_ALLOWED_TERM');
  for(const rule of activeRules){
    const termRuleId=number(rule.TERM_RULE_ID??rule[ruleDescriptor.primaryKeyColumn]);
    if(!(termRuleId>0))continue;
    const allowed=(await pdlRows('DEPOSIT_PRODUCT_ALLOWED_TERM','TERM_RULE_ID',termRuleId)).items||[];
    for(const row of allowed.filter(activeNow)){
      try{return allowedTermFromRow(row,allowedDescriptor)}catch(e){console.log(`PHASE11F_RUNTIME_E2E_SKIP_ALLOWED_TERM=${text(row.ALLOWED_TERM_ID||row[allowedDescriptor.primaryKeyColumn])} reason=${String(e.message).slice(0,180)}`)}
    }
  }
  throw new Error(`NO_USABLE_PDL_ALLOWED_TERM_FOR_PRODUCT_VERSION=${productVersionId}`);
}
async function candidateAccounts(){
  const all=[];
  for(const family of ['SHORT_TERM_DEPOSIT','LONG_TERM_DEPOSIT']){
    const x=await req('GET',`/api/v1/deposit-accounts?status=ACTIVE&productFamilyCode=${family}&limit=50`);
    for(const a of x.items||[])all.push(a);
  }
  return all;
}
async function findFundedTermCandidate(){
  for(const a of await candidateAccounts()){
    try{
      const d=await req('GET',`/api/v1/deposit-accounts/${a.accountId}`);
      const x=await req('GET',`/api/v1/deposit-accounts/${a.accountId}/term-operations`);
      if(Number(d.balance.availableBalance)>1&&Number(x.contract.principalAmount)>1)return {account:a,detail:d,term:x};
    }catch(e){console.log(`PHASE11F_RUNTIME_E2E_SKIP_ACCOUNT=${a.accountId} reason=${String(e.message).slice(0,180)}`)}
  }
  return null;
}
function openingEligibleVersion(row){
  const status=text(row.VERSION_STATUS_CODE).toUpperCase();
  const origination=text(row.ORIGINATION_STATUS_CODE).toUpperCase();
  const recordStatus=text(row.RECORD_STATUS_CODE).toUpperCase();
  if(status&&!['ACTIVE','APPROVED'].includes(status))return false;
  if(origination&&origination!=='OPEN')return false;
  if(recordStatus&&recordStatus!=='ACTIVE')return false;
  return activeNow(row);
}
async function governedPdlTermTemplate(primaryPartyId){
  const products=(await pdlRows('PRODUCT')).items||[];
  const termProducts=products.filter(p=>text(p.PRODUCT_CLASS_CODE).toUpperCase()==='DEPOSIT'&&['SHORT_TERM_DEPOSIT','LONG_TERM_DEPOSIT'].includes(text(p.PRODUCT_FAMILY_CODE).toUpperCase()));
  for(const product of termProducts){
    const productId=number(product.PRODUCT_ID);if(!(productId>0))continue;
    const versions=(await pdlRows('PRODUCT_VERSION','PRODUCT_ID',productId)).items||[];
    for(const version of versions.filter(openingEligibleVersion)){
      const productVersionId=number(version.PRODUCT_VERSION_ID);if(!(productVersionId>0))continue;
      try{
        const allowedTerm=await resolveAllowedTerm(productVersionId);
        console.log(`PHASE11F_RUNTIME_E2E_PDL_TEMPLATE_PRODUCT_VERSION=${productVersionId} family=${text(product.PRODUCT_FAMILY_CODE)}`);
        return {template:{productVersionId,primaryPartyId,productFamilyCode:text(product.PRODUCT_FAMILY_CODE)},allowedTerm};
      }catch(e){
        console.log(`PHASE11F_RUNTIME_E2E_SKIP_PDL_PRODUCT_VERSION=${productVersionId} reason=${String(e.message).slice(0,220)}`);
      }
    }
  }
  return null;
}
async function bootstrapTemplate(){
  const templates=await candidateAccounts();
  const seen=new Set();
  for(const template of templates){
    const productVersionId=number(template.productVersionId),primaryPartyId=number(template.primaryPartyId);
    if(!(productVersionId>0)||!(primaryPartyId>0)||seen.has(productVersionId))continue;
    seen.add(productVersionId);
    try{return {template,allowedTerm:await resolveAllowedTerm(productVersionId)}}
    catch(e){console.log(`PHASE11F_RUNTIME_E2E_SKIP_TEMPLATE_PRODUCT_VERSION=${productVersionId} reason=${String(e.message).slice(0,220)}`)}
  }
  const donor=templates.find(t=>number(t.primaryPartyId)>0);
  if(donor){
    const governed=await governedPdlTermTemplate(number(donor.primaryPartyId));
    if(governed)return governed;
  }
  throw new Error('NO_OPEN_TERM_PRODUCT_VERSION_WITH_GOVERNED_ALLOWED_TERM; run tools/reconcile-pdl-term-config-phase11f.mjs --apply');
}
async function bootstrapTermAccount(){
  const {template,allowedTerm}=await bootstrapTemplate();
  const run=uid();const amount=1000;const evidence=`P11F-${run}`;const actor='phase11f.qa';const start=today();const maturity=addTerm(start,allowedTerm.termValue,allowedTerm.termUnitCode);
  console.log(`PHASE11F_RUNTIME_E2E_ALLOWED_TERM=${allowedTerm.allowedTermId} code=${allowedTerm.termCode} value=${allowedTerm.termValue} unit=${allowedTerm.termUnitCode}`);
  const aggregate={
    DEPOSIT_OPENING_REQUEST:{REQUEST_NO:`P11F-${run}`.slice(0,40),IDEMPOTENCY_KEY:`P11F-${run}`.slice(0,80),PRODUCT_VERSION_ID:Number(template.productVersionId),REQUEST_TYPE_CODE:'CUSTOMER_REQUEST',OWNERSHIP_TYPE_CODE:'INDIVIDUAL',CURRENCY_CODE:'IRR',OPENING_CHANNEL_CODE:'BRANCH',ORG_UNIT_CODE:'001',REQUESTED_OPENING_DATE:start,OPENING_AMOUNT:amount,SOURCE_OF_FUNDS_CODE:'SAVINGS',PURPOSE_CODE:'INVESTMENT',CUSTOMER_RISK_LEVEL_CODE:'LOW',RISK_ASSESSMENT_REFERENCE:`${evidence}-RISK`.slice(0,120),EXPECTED_ACTIVITY_REFERENCE:`${evidence}-ACTIVITY`.slice(0,120),ACTIVATION_STATUS_CODE:'NOT_CREATED',REQUEST_STATUS_CODE:'APPROVED'},
    DEPOSIT_OPENING_PARTY:[{PARTY_ID:Number(template.primaryPartyId),ROLE_CODE:'OWNER',IS_PRIMARY:1,OWNERSHIP_PERCENT:100,SEQUENCE_NO:1}],
    DEPOSIT_OPENING_TERM:{ALLOWED_TERM_ID:allowedTerm.allowedTermId,TERM_CODE:allowedTerm.termCode,TERM_VALUE:allowedTerm.termValue,TERM_UNIT_CODE:allowedTerm.termUnitCode,START_DATE:start,MATURITY_DATE:maturity,AUTO_RENEW_FLAG:0},
    DEPOSIT_OPENING_MATURITY_INSTRUCTION:{MATURITY_ACTION_CODE:'RENEW_PRINCIPAL',INSTRUCTION_SOURCE_CODE:'CUSTOMER'},
    DEPOSIT_OPENING_OBLIGATION:[{OPENING_OBLIGATION_ID:1,OBLIGATION_TYPE_CODE:'INITIAL_BALANCE',SOURCE_SYSTEM_CODE:'PHASE11F',SOURCE_REFERENCE:`${evidence}-INITIAL`.slice(0,120),DESCRIPTION:'Phase 11F funded term runtime account',GROSS_AMOUNT:amount,WAIVED_AMOUNT:0,FINAL_AMOUNT:amount,CURRENCY_CODE:'IRR',MANDATORY_FOR_ACTIVATION_FLAG:1,SETTLEMENT_STATUS_CODE:'PENDING'}],
    DEPOSIT_OPENING_FUNDING:[{OPENING_FUNDING_ID:1,FUNDING_METHOD_CODE:'CASH',FUNDING_AMOUNT:amount,SOURCE_PARTY_ID:Number(template.primaryPartyId),SOURCE_REFERENCE:`${evidence}-FUNDING`.slice(0,120),FUNDING_PURPOSE_CODE:'INITIAL_BALANCE',SOURCE_OWNERSHIP_VERIFIED_FLAG:1,SOURCE_VERIFICATION_REFERENCE:`${evidence}-SOURCE-VERIFIED`.slice(0,120),CASH_MANAGEMENT_TXN_REF:`${evidence}-CASH-MGMT`.slice(0,100),FUNDING_STATUS_CODE:'PENDING'}],
    DEPOSIT_OPENING_CHECK:createGateChecks.map(([code,type,phase],i)=>({CHECK_CODE:code,CHECK_TYPE_CODE:type,ATTEMPT_NO:1,CHECK_PHASE_CODE:phase,BLOCKING_SCOPE_CODE:'ACCOUNT_CREATION',REQUIRED_FLAG:1,RECHECK_REQUIRED_FLAG:0,RESULT_STATUS_CODE:'PASS',RESULT_REFERENCE:`${evidence}-CG-${String(i+1).padStart(2,'0')}`.slice(0,120),SOURCE_EVALUATION_REFERENCE:'PHASE11F_RUNTIME_BOOTSTRAP'})),
    DEPOSIT_OPENING_TERMS_ACCEPTANCE:{TERMS_VERSION_CODE:'PHASE11F-2026.09',ACCEPTED_BY_PARTY_ID:Number(template.primaryPartyId),ACCEPTANCE_SOURCE_CODE:'API',CHANNEL_CODE:'BRANCH',ACCEPTANCE_STATUS_CODE:'ACCEPTED',EVIDENCE_REFERENCE:`${evidence}-TERMS`.slice(0,120)},
    DEPOSIT_OPENING_DECISION:{DECISION_CODE:'APPROVE',DECISION_REASON_CODE:'ALL_CHECKS_PASSED',DECISION_NOTE:'Phase 11F runtime bootstrap',DECIDED_BY:actor}
  };
  const validation=await req('POST','/api/v1/deposit-opening/requests/validate',aggregate,actor);if(validation.valid!==true)throw new Error(`TERM_BOOTSTRAP_VALIDATION_FAILED:${JSON.stringify(validation)}`);
  const created=await req('POST','/api/v1/deposit-opening/requests',aggregate,actor);const oid=created.openingRequestId;if(!oid)throw new Error('TERM_BOOTSTRAP_OPENING_ID_MISSING');
  let account=await req('POST',`/api/v1/deposit-opening/requests/${oid}/account`,undefined,actor);if(account.accountStatusCode!=='PENDING_ACTIVATION')throw new Error('TERM_BOOTSTRAP_ACCOUNT_NOT_PENDING');
  await req('POST',`/api/v1/deposit-opening/requests/${oid}/account/settlement`,{SETTLEMENT_REFERENCE:`${evidence}-SETTLE`.slice(0,120)},actor);
  const readinessEvidence=['CBI_SIAH_REGISTRATION','FINAL_COMPLIANCE_RECHECK','RESTRICTIONS_READY'].map(code=>({CHECK_CODE:code,RESULT_STATUS_CODE:'PASS',RESULT_REFERENCE:`${evidence}-${code}`.slice(0,120),VALID_UNTIL:plusHours(2),SOURCE_EVALUATION_REFERENCE:'PHASE11F_RUNTIME_BOOTSTRAP'}));
  const ready=await req('POST',`/api/v1/deposit-opening/requests/${oid}/account/readiness`,{EVIDENCE:readinessEvidence},actor);if(ready.activationStatusCode!=='READY')throw new Error(`TERM_BOOTSTRAP_NOT_READY:${ready.activationStatusCode}`);
  account=await req('POST',`/api/v1/deposit-opening/requests/${oid}/account/activate`,undefined,actor);if(account.accountStatusCode!=='ACTIVE')throw new Error('TERM_BOOTSTRAP_NOT_ACTIVE');
  const detail=await req('GET',`/api/v1/deposit-accounts/${account.accountId}`);const term=await req('GET',`/api/v1/deposit-accounts/${account.accountId}/term-operations`);
  if(Number(detail.balance.availableBalance)<=1||Number(term.contract.principalAmount)<=1)throw new Error('TERM_BOOTSTRAP_NOT_FUNDED');
  console.log(`PHASE11F_RUNTIME_E2E_BOOTSTRAP_ACCOUNT=${account.accountId}`);
  return {account:{...template,accountId:account.accountId},detail,term};
}
try{
  let selected=await findFundedTermCandidate();if(!selected)selected=await bootstrapTermAccount();
  const {account,term}=selected;const id=account.accountId;console.log(`PHASE11F_RUNTIME_E2E_ACCOUNT=${id}`);
  const maturity=term.contract.maturityDate;const instructionKey='11f-ins-'+uid();
  await req('PUT',`/api/v1/deposit-accounts/${id}/term-operations/maturity-instruction`,{instructionCode:'WAIT_INSTRUCTION',instructionSourceCode:'CUSTOMER',settlementAccountId:null,settlementAccountReference:null,expectedRecordVersion:term.contract.recordVersion},'deposit.operator',instructionKey);
  const afterInstruction=await req('GET',`/api/v1/deposit-accounts/${id}/term-operations`);if(afterInstruction.contract.maturityDate!==maturity)throw new Error('MATURITY_CHANGED_BY_INSTRUCTION');
  const beforePrincipal=Number(afterInstruction.contract.principalAmount),beforeLedger=Number((await req('GET',`/api/v1/deposit-accounts/${id}`)).balance.ledgerBalance);const amount=1;
  const requestKey='11f-pw-rq-'+uid();const rq=await req('POST',`/api/v1/deposit-accounts/${id}/term-operations/partial-withdrawals`,{amount,approverUserId:'deposit.approver',orgUnitCode:'HQ'},'deposit.operator',requestKey);const pwId=rq.entityId;console.log(`PHASE11F_RUNTIME_E2E_PARTIAL=${pwId}`);
  await req('POST',`/api/v1/deposit-accounts/${id}/term-operations/partial-withdrawals/${pwId}/approve`,{},'deposit.approver','11f-pw-ap-'+uid());
  await req('POST',`/api/v1/deposit-accounts/${id}/term-operations/partial-withdrawals/${pwId}/execute`,{},'deposit.operator','11f-pw-ex-'+uid());
  const finalTerm=await req('GET',`/api/v1/deposit-accounts/${id}/term-operations`);const finalDetail=await req('GET',`/api/v1/deposit-accounts/${id}`);const principalDelta=beforePrincipal-Number(finalTerm.contract.principalAmount),ledgerDelta=beforeLedger-Number(finalDetail.balance.ledgerBalance);if(principalDelta!==amount)throw new Error(`PRINCIPAL_DELTA_MISMATCH:${principalDelta}`);if(ledgerDelta!==amount)throw new Error(`LEDGER_DELTA_MISMATCH:${ledgerDelta}`);
  const row=finalTerm.partialWithdrawals.find(x=>x.partialWithdrawalId===pwId);if(!row||row.statusCode!=='EXECUTED'||row.approvalStatusCode!=='APPROVED')throw new Error('PARTIAL_WORKFLOW_NOT_EXECUTED');
  console.log('PHASE11F_RUNTIME_E2E_PASS');
}catch(e){console.error('PHASE11F_RUNTIME_E2E_FAIL');console.error(e);process.exit(1)}
