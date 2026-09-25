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

function rateFrom(row,descriptor){const preferred=['RATE_VALUE','ANNUAL_RATE','PROFIT_RATE','INTEREST_RATE','PERCENTAGE_RATE','RATE_PERCENT','FIXED_RATE','BASE_RATE','COMPONENT_VALUE','VALUE'];for(const name of preferred){const v=number(row?.[name]);if(v!=null&&v>0)return v}for(const c of descriptor?.columns||[]){const name=text(c.name).toUpperCase();if(name.endsWith('_ID')||!text(c.dataType).toUpperCase().startsWith('NUMBER'))continue;if(/RATE|PERCENT/.test(name)){const v=number(row?.[name]);if(v!=null&&v>0)return v}}return null}
function firstCode(rows,names,fallback){for(const row of rows){for(const name of names){const v=text(row?.[name]).toUpperCase();if(v)return v}}return fallback}
async function resolveProfitConfiguration(productVersionId){
  const [ruleDescriptor,componentDescriptor,tierDescriptor,paymentDescriptor]=await Promise.all([pdlDescriptor('PRODUCT_PRICING_RULE'),pdlDescriptor('PRODUCT_PRICING_COMPONENT'),pdlDescriptor('PRODUCT_RATE_TIER'),pdlDescriptor('DEPOSIT_PROFIT_PAYMENT_RULE')]);
  const versionColumn=columnName(ruleDescriptor,['PRODUCT_VERSION_ID']);if(!versionColumn)throw new Error('PDL_PRICING_RULE_PRODUCT_VERSION_COLUMN_NOT_FOUND');
  const rules=((await pdlRows('PRODUCT_PRICING_RULE',versionColumn,productVersionId)).items||[]).filter(activeNow);if(!rules.length)throw new Error(`NO_ACTIVE_PDL_PRICING_RULE_FOR_PRODUCT_VERSION=${productVersionId}`);
  const paymentRows=((await pdlRows('DEPOSIT_PROFIT_PAYMENT_RULE','PRODUCT_VERSION_ID',productVersionId)).items||[]).filter(activeNow);if(!paymentRows.length)throw new Error(`NO_ACTIVE_PDL_PROFIT_PAYMENT_RULE_FOR_PRODUCT_VERSION=${productVersionId}`);
  const payment=paymentRows[0];const profitPaymentRuleId=number(payment.PROFIT_PAYMENT_RULE_ID??payment[paymentDescriptor.primaryKeyColumn]);if(!(profitPaymentRuleId>0))throw new Error(`PDL_PROFIT_PAYMENT_RULE_ID_MISSING_FOR_PRODUCT_VERSION=${productVersionId}`);
  for(const rule of rules){const pricingRuleId=number(rule.PRICING_RULE_ID??rule[ruleDescriptor.primaryKeyColumn]);if(!(pricingRuleId>0))continue;const components=((await pdlRows('PRODUCT_PRICING_COMPONENT','PRICING_RULE_ID',pricingRuleId)).items||[]).filter(activeNow);for(const component of components){const pricingComponentId=number(component.PRICING_COMPONENT_ID??component[componentDescriptor.primaryKeyColumn]);if(!(pricingComponentId>0))continue;const tiers=((await pdlRows('PRODUCT_RATE_TIER','PRICING_COMPONENT_ID',pricingComponentId)).items||[]).filter(activeNow);const tier=tiers[0]||null;const rateTierId=tier?number(tier.RATE_TIER_ID??tier[tierDescriptor.primaryKeyColumn]):null;const rateValue=rateFrom(tier||{},tierDescriptor)||rateFrom(component,componentDescriptor)||rateFrom(rule,ruleDescriptor);if(!(rateValue>0))continue;return {pricingRuleId,pricingComponentId,rateTierId:rateTierId>0?rateTierId:null,profitPaymentRuleId,rateValue,calculationMethodCode:firstCode([tier,component,rule].filter(Boolean),['CALCULATION_METHOD_CODE','PRICING_METHOD_CODE'],'PERCENTAGE'),dayCountBasisCode:firstCode([tier,component,rule].filter(Boolean),['DAY_COUNT_BASIS_CODE'],'ACT_365'),accrualFrequencyCode:firstCode([component,rule].filter(Boolean),['ACCRUAL_FREQUENCY_CODE'],'DAILY'),paymentFrequencyCode:firstCode([payment],['PAYMENT_FREQUENCY_CODE'],'MATURITY'),paymentDayRuleCode:firstCode([payment],['PAYMENT_DAY_RULE_CODE'],'MATURITY_DATE'),firstPaymentRuleCode:firstCode([payment],['FIRST_PAYMENT_RULE_CODE'],'MATURITY_ONLY'),holidayAdjustmentCode:firstCode([payment],['HOLIDAY_ADJUSTMENT_CODE'],'NEXT_BUSINESS_DAY'),paymentDestinationCode:firstCode([payment],['PAYMENT_DESTINATION_CODE'],'SAME_DEPOSIT')}}}
  throw new Error(`NO_USABLE_PDL_PRICING_COMPONENT_RATE_FOR_PRODUCT_VERSION=${productVersionId}`);
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
        const profitConfig=await resolveProfitConfiguration(productVersionId);
        console.log(`PHASE11F_RUNTIME_E2E_PDL_TEMPLATE_PRODUCT_VERSION=${productVersionId} family=${text(product.PRODUCT_FAMILY_CODE)}`);
        return {template:{productVersionId,primaryPartyId,productFamilyCode:text(product.PRODUCT_FAMILY_CODE)},allowedTerm,profitConfig};
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
    try{const allowedTerm=await resolveAllowedTerm(productVersionId);return {template,allowedTerm,profitConfig:await resolveProfitConfiguration(productVersionId)}}
    catch(e){console.log(`PHASE11F_RUNTIME_E2E_SKIP_TEMPLATE_PRODUCT_VERSION=${productVersionId} reason=${String(e.message).slice(0,220)}`)}
  }
  const donor=templates.find(t=>number(t.primaryPartyId)>0);
  if(donor){
    const governed=await governedPdlTermTemplate(number(donor.primaryPartyId));
    if(governed)return governed;
  }
  throw new Error('NO_OPEN_TERM_PRODUCT_VERSION_WITH_GOVERNED_ALLOWED_TERM_OR_PROFIT_CONFIGURATION; run tools/reconcile-pdl-term-config-phase11f.mjs --apply then tools/reconcile-pdl-profit-config-phase11g.mjs --apply');
}
function minusTerm(end,value,unit){
  const d=new Date(`${end}T00:00:00.000Z`);
  switch(text(unit).toUpperCase()){
    case 'DAY': d.setUTCDate(d.getUTCDate()-value);break;
    case 'MONTH': d.setUTCMonth(d.getUTCMonth()-value);break;
    case 'YEAR': d.setUTCFullYear(d.getUTCFullYear()-value);break;
    default: throw new Error(`UNSUPPORTED_ALLOWED_TERM_UNIT:${unit}`);
  }
  return d.toISOString().slice(0,10);
}
function plusDaysIso(day,n){const d=new Date(`${day}T00:00:00.000Z`);d.setUTCDate(d.getUTCDate()+n);return d.toISOString().slice(0,10)}
async function bootstrapNcTerm({label,start,maturity,maturityAction='RENEW_PRINCIPAL',settlementRef=null,paymentDestination='SAME_DEPOSIT',profitDestinationRef=null}){
  const {template,allowedTerm,profitConfig}=await bootstrapTemplate();
  const run=uid();const amount=1000;const evidence=`N11C-${label}-${run}`;const actor='phase11nc.qa';
  const calculated=maturity||addTerm(start,allowedTerm.termValue,allowedTerm.termUnitCode);
  const destinationSelected=paymentDestination==='CUSTOMER_SELECTED_ACCOUNT'?1:0;
  const aggregate={
    DEPOSIT_OPENING_REQUEST:{REQUEST_NO:`N11C-${label}-${run}`.slice(0,40),IDEMPOTENCY_KEY:`N11C-${label}-${run}`.slice(0,80),PRODUCT_VERSION_ID:Number(template.productVersionId),REQUEST_TYPE_CODE:'CUSTOMER_REQUEST',OWNERSHIP_TYPE_CODE:'INDIVIDUAL',CURRENCY_CODE:'IRR',OPENING_CHANNEL_CODE:'BRANCH',ORG_UNIT_CODE:'001',REQUESTED_OPENING_DATE:today(),OPENING_AMOUNT:amount,SOURCE_OF_FUNDS_CODE:'SAVINGS',PURPOSE_CODE:'INVESTMENT',CUSTOMER_RISK_LEVEL_CODE:'LOW',RISK_ASSESSMENT_REFERENCE:`${evidence}-RISK`.slice(0,120),EXPECTED_ACTIVITY_REFERENCE:`${evidence}-ACTIVITY`.slice(0,120),ACTIVATION_STATUS_CODE:'NOT_CREATED',REQUEST_STATUS_CODE:'APPROVED'},
    DEPOSIT_OPENING_PARTY:[{PARTY_ID:Number(template.primaryPartyId),ROLE_CODE:'OWNER',IS_PRIMARY:1,OWNERSHIP_PERCENT:100,SEQUENCE_NO:1}],
    DEPOSIT_OPENING_TERM:{ALLOWED_TERM_ID:allowedTerm.allowedTermId,TERM_CODE:allowedTerm.termCode,TERM_VALUE:allowedTerm.termValue,TERM_UNIT_CODE:allowedTerm.termUnitCode,START_DATE:start,MATURITY_DATE:calculated,AUTO_RENEW_FLAG:0},
    DEPOSIT_OPENING_MATURITY_INSTRUCTION:{MATURITY_ACTION_CODE:maturityAction,SETTLEMENT_ACCOUNT_REFERENCE:settlementRef,INSTRUCTION_SOURCE_CODE:'CUSTOMER'},
    DEPOSIT_OPENING_PROFIT_INSTRUCTION:{PRICING_RULE_ID:profitConfig.pricingRuleId,PRICING_COMPONENT_ID:profitConfig.pricingComponentId,RATE_TIER_ID:profitConfig.rateTierId,PROFIT_PAYMENT_RULE_ID:profitConfig.profitPaymentRuleId,RATE_VALUE:profitConfig.rateValue,CALCULATION_METHOD_CODE:profitConfig.calculationMethodCode,DAY_COUNT_BASIS_CODE:profitConfig.dayCountBasisCode,ACCRUAL_FREQUENCY_CODE:profitConfig.accrualFrequencyCode,PAYMENT_FREQUENCY_CODE:profitConfig.paymentFrequencyCode,PAYMENT_DAY_RULE_CODE:profitConfig.paymentDayRuleCode,FIRST_PAYMENT_RULE_CODE:profitConfig.firstPaymentRuleCode,HOLIDAY_ADJUSTMENT_CODE:profitConfig.holidayAdjustmentCode,PAYMENT_DESTINATION_CODE:paymentDestination,DESTINATION_ACCOUNT_REFERENCE:profitDestinationRef,DESTINATION_SELECTED_BY_CUSTOMER:destinationSelected},
    DEPOSIT_OPENING_OBLIGATION:[{OPENING_OBLIGATION_ID:1,OBLIGATION_TYPE_CODE:'INITIAL_BALANCE',SOURCE_SYSTEM_CODE:'PHASE11NC',SOURCE_REFERENCE:`${evidence}-INITIAL`.slice(0,120),DESCRIPTION:'Phase 11N-C canonical closure runtime account',GROSS_AMOUNT:amount,WAIVED_AMOUNT:0,FINAL_AMOUNT:amount,CURRENCY_CODE:'IRR',MANDATORY_FOR_ACTIVATION_FLAG:1,SETTLEMENT_STATUS_CODE:'PENDING'}],
    DEPOSIT_OPENING_FUNDING:[{OPENING_FUNDING_ID:1,FUNDING_METHOD_CODE:'CASH',FUNDING_AMOUNT:amount,SOURCE_PARTY_ID:Number(template.primaryPartyId),SOURCE_REFERENCE:`${evidence}-FUNDING`.slice(0,120),FUNDING_PURPOSE_CODE:'INITIAL_BALANCE',SOURCE_OWNERSHIP_VERIFIED_FLAG:1,SOURCE_VERIFICATION_REFERENCE:`${evidence}-SOURCE-VERIFIED`.slice(0,120),CASH_MANAGEMENT_TXN_REF:`${evidence}-CASH-MGMT`.slice(0,100),FUNDING_STATUS_CODE:'PENDING'}],
    DEPOSIT_OPENING_CHECK:createGateChecks.map(([code,type,phase],i)=>({CHECK_CODE:code,CHECK_TYPE_CODE:type,ATTEMPT_NO:1,CHECK_PHASE_CODE:phase,BLOCKING_SCOPE_CODE:'ACCOUNT_CREATION',REQUIRED_FLAG:1,RECHECK_REQUIRED_FLAG:0,RESULT_STATUS_CODE:'PASS',RESULT_REFERENCE:`${evidence}-CG-${String(i+1).padStart(2,'0')}`.slice(0,120),SOURCE_EVALUATION_REFERENCE:'PHASE11NC_RUNTIME_BOOTSTRAP'})),
    DEPOSIT_OPENING_TERMS_ACCEPTANCE:{TERMS_VERSION_CODE:'PHASE11NC-2026.09',ACCEPTED_BY_PARTY_ID:Number(template.primaryPartyId),ACCEPTANCE_SOURCE_CODE:'API',CHANNEL_CODE:'BRANCH',ACCEPTANCE_STATUS_CODE:'ACCEPTED',EVIDENCE_REFERENCE:`${evidence}-TERMS`.slice(0,120)},
    DEPOSIT_OPENING_DECISION:{DECISION_CODE:'APPROVE',DECISION_REASON_CODE:'ALL_CHECKS_PASSED',DECISION_NOTE:'Phase 11N-C runtime bootstrap',DECIDED_BY:actor}
  };
  const validation=await req('POST','/api/v1/deposit-opening/requests/validate',aggregate,actor);if(validation.valid!==true)throw new Error(`NC_BOOTSTRAP_VALIDATION_FAILED:${label}:${JSON.stringify(validation)}`);
  const created=await req('POST','/api/v1/deposit-opening/requests',aggregate,actor);const oid=created.openingRequestId;if(!oid)throw new Error(`NC_BOOTSTRAP_OPENING_ID_MISSING:${label}`);
  let account=await req('POST',`/api/v1/deposit-opening/requests/${oid}/account`,undefined,actor);if(account.accountStatusCode!=='PENDING_ACTIVATION')throw new Error(`NC_BOOTSTRAP_ACCOUNT_NOT_PENDING:${label}`);
  await req('POST',`/api/v1/deposit-opening/requests/${oid}/account/settlement`,{SETTLEMENT_REFERENCE:`${evidence}-SETTLE`.slice(0,120)},actor);
  const readinessEvidence=['CBI_SIAH_REGISTRATION','FINAL_COMPLIANCE_RECHECK','RESTRICTIONS_READY'].map(code=>({CHECK_CODE:code,RESULT_STATUS_CODE:'PASS',RESULT_REFERENCE:`${evidence}-${code}`.slice(0,120),VALID_UNTIL:plusHours(2),SOURCE_EVALUATION_REFERENCE:'PHASE11NC_RUNTIME_BOOTSTRAP'}));
  const ready=await req('POST',`/api/v1/deposit-opening/requests/${oid}/account/readiness`,{EVIDENCE:readinessEvidence},actor);if(ready.activationStatusCode!=='READY')throw new Error(`NC_BOOTSTRAP_NOT_READY:${label}:${ready.activationStatusCode}`);
  account=await req('POST',`/api/v1/deposit-opening/requests/${oid}/account/activate`,undefined,actor);if(account.accountStatusCode!=='ACTIVE')throw new Error(`NC_BOOTSTRAP_NOT_ACTIVE:${label}`);
  const detail=await req('GET',`/api/v1/deposit-accounts/${account.accountId}`);const term=await req('GET',`/api/v1/deposit-accounts/${account.accountId}/term-operations`);const profit=await req('GET',`/api/v1/deposit-accounts/${account.accountId}/profit`);
  if(Number(detail.balance.availableBalance)<=1||Number(term.contract.principalAmount)<=1)throw new Error(`NC_BOOTSTRAP_NOT_FUNDED:${label}`);
  console.log(`PHASE11NC_RUNTIME_BOOTSTRAP_${label}=${account.accountId}:${term.contract.maturityDate}`);
  return {account:{...template,accountId:account.accountId,accountNo:detail.account?.accountNo||account.accountNo||detail.accountNo},detail,term,profit,allowedTerm};
}
try{
  const tpl=await bootstrapTemplate();
  const futureStart=plusDaysIso(today(),-1);const futureMaturity=addTerm(futureStart,tpl.allowedTerm.termValue,tpl.allowedTerm.termUnitCode);
  if(futureMaturity<=today())throw new Error(`PHASE11NC_NEEDS_TERM_LONGER_THAN_ONE_DAY_FOR_RUNTIME: resolved ${tpl.allowedTerm.termValue} ${tpl.allowedTerm.termUnitCode}`);
  const target=await bootstrapNcTerm({label:'TARGET',start:futureStart,maturity:futureMaturity,maturityAction:'RENEW_PRINCIPAL',paymentDestination:'SAME_DEPOSIT'});
  const targetRef=target.account.accountNo;if(!targetRef)throw new Error('PHASE11NC_TARGET_ACCOUNT_REFERENCE_MISSING');

  const source=await bootstrapNcTerm({label:'SOURCE',start:futureStart,maturity:futureMaturity,maturityAction:'RENEW_PRINCIPAL',paymentDestination:'CUSTOMER_SELECTED_ACCOUNT',profitDestinationRef:targetRef});
  const sid=source.account.accountId;
  const accrued=await req('POST',`/api/v1/deposit-accounts/${sid}/profit/accruals`,{throughDate:today()},'phase11nc.qa','11nc-accrue-'+uid());if(!(accrued.entityId>0))throw new Error('PHASE11NC_ACCRUAL_MISSING');
  const profitPosted=await req('POST',`/api/v1/deposit-accounts/${sid}/profit/postings`,{postingDate:today()},'phase11nc.qa','11nc-profit-'+uid());if(!(profitPosted.entityId>0))throw new Error('PHASE11NC_EXTERNAL_PROFIT_TX_MISSING');
  const profitView=await req('GET',`/api/v1/deposit-accounts/${sid}/profit`);const payment=profitView.payments.find(x=>x.paymentStatusCode==='PAID'&&x.postingReference===`TX-${profitPosted.entityId}`);if(!payment)throw new Error('PHASE11NC_EXTERNAL_PROFIT_PAYMENT_TRACE_MISSING');
  console.log(`PHASE11NC_RUNTIME_EXTERNAL_PROFIT=${payment.profitPaymentId}:TX-${profitPosted.entityId}`);

  const beforeTerm=await req('GET',`/api/v1/deposit-accounts/${sid}/term-operations`);const beforePrincipal=Number(beforeTerm.contract.principalAmount);const pw=await req('POST',`/api/v1/deposit-accounts/${sid}/term-operations/partial-withdrawals`,{amount:1,approverUserId:'deposit.approver',orgUnitCode:'HQ'},'phase11nc.qa','11nc-pw-rq-'+uid());
  await req('POST',`/api/v1/deposit-accounts/${sid}/term-operations/partial-withdrawals/${pw.entityId}/approve`,{},'deposit.approver','11nc-pw-ap-'+uid());
  await req('POST',`/api/v1/deposit-accounts/${sid}/term-operations/partial-withdrawals/${pw.entityId}/execute`,{},'phase11nc.qa','11nc-pw-ex-'+uid());
  const afterPw=await req('GET',`/api/v1/deposit-accounts/${sid}/term-operations`);const pwRow=afterPw.partialWithdrawals.find(x=>Number(x.partialWithdrawalId)===Number(pw.entityId));if(!pwRow||pwRow.statusCode!=='EXECUTED'||!(pwRow.transactionId>0)||Math.abs(beforePrincipal-Number(afterPw.contract.principalAmount)-1)>0.0001)throw new Error('PHASE11NC_PARTIAL_WITHDRAWAL_TRACE_INVALID');
  console.log(`PHASE11NC_RUNTIME_PARTIAL_WITHDRAWAL=${pw.entityId}:TX-${pwRow.transactionId}`);

  const dueStart=minusTerm(today(),tpl.allowedTerm.termValue,tpl.allowedTerm.termUnitCode);
  const due=await bootstrapNcTerm({label:'MATURITY',start:dueStart,maturity:today(),maturityAction:'PAY_TO_ACCOUNT',settlementRef:targetRef,paymentDestination:'SAME_DEPOSIT'});const did=due.account.accountId;
  const maturity=await req('POST',`/api/v1/deposit-accounts/${did}/term-operations/maturity/execute`,{},'phase11nc.qa','11nc-maturity-'+uid());
  const maturityView=await req('GET',`/api/v1/deposit-accounts/${did}/term-operations`);const me=maturityView.maturityEvents.find(x=>Number(x.maturityEventId)===Number(maturity.entityId));if(!me||me.actionResultCode!=='TRANSFERRED'||!(me.settlementTransactionId>0))throw new Error('PHASE11NC_MATURITY_TRACE_INVALID');
  const ms=maturityView.settlements.find(x=>x.settlementTypeCode==='MATURITY'&&Number(x.transactionId)===Number(me.settlementTransactionId)&&x.statusCode==='POSTED');if(!ms)throw new Error('PHASE11NC_MATURITY_SETTLEMENT_TRACE_MISSING');
  console.log(`PHASE11NC_RUNTIME_MATURITY=${me.maturityEventId}:TX-${me.settlementTransactionId}`);

  const earlyRq=await req('POST',`/api/v1/deposit-accounts/${sid}/term-operations/early-terminations`,{terminationDate:today(),settlementAccountReference:targetRef,earlyTerminationRate:1,approverUserId:'deposit.approver',orgUnitCode:'HQ'},'phase11nc.qa','11nc-early-rq-'+uid());
  let earlyView=await req('GET',`/api/v1/deposit-accounts/${sid}/term-operations`);let er=earlyView.earlyTerminations.find(x=>Number(x.earlyTerminationId)===Number(earlyRq.entityId));if(!er||!(er.closureId>0))throw new Error('PHASE11NC_EARLY_CLOSURE_TRACE_MISSING');
  await req('POST',`/api/v1/deposit-accounts/${sid}/closures/${er.closureId}/approve`,{},'deposit.approver','11nc-early-cl-ap-'+uid());
  await req('POST',`/api/v1/deposit-accounts/${sid}/term-operations/early-terminations/${earlyRq.entityId}/execute`,{},'phase11nc.qa','11nc-early-ex-'+uid());
  earlyView=await req('GET',`/api/v1/deposit-accounts/${sid}/term-operations`);er=earlyView.earlyTerminations.find(x=>Number(x.earlyTerminationId)===Number(earlyRq.entityId));if(!er||er.statusCode!=='EXECUTED'||!(er.settlementTransactionId>0)||er.closureStatusCode!=='EXECUTED')throw new Error('PHASE11NC_EARLY_TERMINATION_TRACE_INVALID');
  const es=earlyView.settlements.find(x=>x.settlementTypeCode==='EARLY_TERMINATION'&&Number(x.transactionId)===Number(er.settlementTransactionId)&&x.statusCode==='POSTED');if(!es)throw new Error('PHASE11NC_EARLY_SETTLEMENT_TRACE_MISSING');
  console.log(`PHASE11NC_RUNTIME_EARLY_TERMINATION=${er.earlyTerminationId}:TX-${er.settlementTransactionId}:CLOSURE-${er.closureId}`);
  console.log(`PHASE11NC_RUNTIME_SOURCE_ACCOUNT=${sid}`);console.log(`PHASE11NC_RUNTIME_TARGET_ACCOUNT=${target.account.accountId}`);console.log(`PHASE11NC_RUNTIME_MATURITY_ACCOUNT=${did}`);
  console.log('PHASE11NC_RUNTIME_E2E_PASS');
}catch(e){console.error('PHASE11NC_RUNTIME_E2E_FAIL');console.error(e);process.exit(1)}
