import fs from 'node:fs';
import path from 'node:path';
import {randomUUID} from 'node:crypto';
import {fileURLToPath} from 'node:url';

const base=(process.env.CORE_BANKING_BASE_URL||'http://localhost:8091').replace(/\/$/,'');
const partyId=Number(process.env.PHASE10F_PARTY_ID||'1');
const openingAmount=Number(process.env.PHASE10F_OPENING_AMOUNT||'1000000');
const here=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(here,'..');
const outPath=process.argv[2]
  ? path.resolve(process.argv[2])
  : path.join(root,'docs','examples','phase10f-e2e-fixture.runtime.json');

const families=[
  {family:'QARD_SAVINGS', code:'P10F_QARD_SAVINGS', name:'Phase10F Qard Savings', purpose:'SAVING'},
  {family:'CURRENT_ACCOUNT', code:'P10F_CURRENT_ACCOUNT', name:'Phase10F Current Account', purpose:'DAILY_BANKING'},
  {family:'SHORT_TERM_DEPOSIT', code:'P10F_SHORT_TERM', name:'Phase10F Short Term Deposit', purpose:'INVESTMENT'},
  {family:'LONG_TERM_DEPOSIT', code:'P10F_LONG_TERM', name:'Phase10F Long Term Deposit', purpose:'INVESTMENT'}
];

const headers={
  'content-type':'application/json',
  'X-User-Id':'phase10f.qa',
  'X-User-Name':'phase10f.qa',
  'X-Correlation-Id':`phase10f-prepare-${Date.now()}`
};
const assert=(condition,message)=>{if(!condition)throw new Error(message)};
const upper=value=>value==null?'':String(value).trim().toUpperCase();
const today=()=>new Date().toISOString().slice(0,10);
const plusHours=hours=>new Date(Date.now()+hours*3600_000).toISOString();

async function request(method,url,body){
  const response=await fetch(base+url,{method,headers,body:body===undefined?undefined:JSON.stringify(body)});
  const text=await response.text();
  let data; try{data=text?JSON.parse(text):null}catch{data=text}
  if(!response.ok){
    throw new Error(`${method} ${url} -> ${response.status}: ${typeof data==='string'?data:JSON.stringify(data)}`);
  }
  return data;
}

async function descriptor(table){
  return request('GET',`/api/v1/product-builder/tables/${encodeURIComponent(table)}/descriptor`);
}

function column(desc,name){
  const c=(desc?.columns||[]).find(x=>upper(x.name)===upper(name));
  assert(c,`${desc?.tableName||'PDL'} missing required column ${name}`);
  return c;
}

function optionCodes(col){
  return (col?.options||[]).map(o=>upper(o.code ?? o.value)).filter(Boolean);
}

function chooseRuntimeValue(desc,name,preferred,{allowNull=true}={}){
  const col=column(desc,name);
  const options=optionCodes(col);
  for(const value of preferred){
    if(options.length===0 || options.includes(upper(value))) return value;
  }
  if(allowNull && col.nullable) return null;
  throw new Error(
    `${desc.tableName}.${name} cannot satisfy Phase10F runtime contract. `+
    `Allowed=${options.length?options.join(','):'<metadata options unavailable>'}, nullable=${col.nullable}`
  );
}

function chooseNonRuntimeValue(desc,name,preferred){
  const col=column(desc,name);
  const options=optionCodes(col);
  for(const value of preferred){
    if(options.length===0 || options.includes(upper(value))) return value;
  }
  if(col.nullable) return null;
  if(options.length) return options[0];
  // Omit by returning undefined when a DB default can own the value.
  if(col.defaultValue!=null && String(col.defaultValue).trim()!=='') return undefined;
  throw new Error(`${desc.tableName}.${name} has no safe Phase10F value`);
}

function hasDatabaseDefault(col){
  const value=col?.defaultValue;
  return value!=null && String(value).trim()!=='' && upper(value)!=='NULL';
}

function requiredControlValue(col){
  const name=upper(col.name);
  const options=optionCodes(col);
  const optionValue=(preferred)=>{
    for(const value of preferred){
      const idx=options.indexOf(upper(value));
      if(idx>=0) return col.options[idx]?.value ?? col.options[idx]?.code ?? value;
    }
    return undefined;
  };

  if(name==='IS_DELETED') return optionValue([0,'0','N','NO','FALSE']) ?? 0;
  if(name==='IS_ACTIVE') return optionValue([1,'1','Y','YES','TRUE']) ?? 1;
  if(name==='IS_CURRENT') return optionValue([1,'1','Y','YES','TRUE']) ?? 1;
  if(name.endsWith('_FLAG')) return optionValue([0,'0','N','NO','FALSE']) ?? 0;

  // For a required column backed by an explicit CHECK/lookup option list,
  // choosing the first metadata-provided value is safer than inventing a literal.
  if(options.length && col.options?.length){
    return col.options[0].value ?? col.options[0].code;
  }
  return undefined;
}

function completeRequiredPayload(desc,payload){
  const completed={...payload};
  const systemManaged=new Set(['CREATED_AT','CREATED_BY','UPDATED_AT','UPDATED_BY','RECORD_VERSION','MIGRATED_AT']);
  for(const col of desc?.columns||[]){
    const name=upper(col.name);
    if(col.primaryKey || col.nullable || systemManaged.has(name) || hasDatabaseDefault(col)) continue;
    if(Object.prototype.hasOwnProperty.call(completed,col.name) && completed[col.name]!==null && String(completed[col.name]).trim()!=='') continue;
    const value=requiredControlValue(col);
    if(value===undefined){
      throw new Error(`${desc.tableName}.${col.name} is required by Oracle metadata and the qualification tool has no safe automatic value. `+
        `Add an explicit Phase10F seed value for this column.`);
    }
    completed[col.name]=value;
    console.log(`[metadata] ${desc.tableName}.${col.name}=${value}`);
  }
  return completed;
}

async function rows(table,{filterColumn,filterValue,text,size=200}={}){
  const params=new URLSearchParams({page:'0',size:String(size)});
  if(filterColumn)params.set('filterColumn',filterColumn);
  if(filterValue!==undefined&&filterValue!==null)params.set('filterValue',String(filterValue));
  if(text)params.set('text',text);
  return request('GET',`/api/v1/product-builder/tables/${encodeURIComponent(table)}/rows?${params}`);
}

async function create(table,values){
  return request('POST',`/api/v1/product-builder/tables/${encodeURIComponent(table)}/rows`,values);
}

async function update(table,id,values){
  return request('PUT',`/api/v1/product-builder/tables/${encodeURIComponent(table)}/rows/${id}`,values);
}

function versionEligible(row){
  if(!row)return false;
  const versionStatus=upper(row.VERSION_STATUS_CODE);
  const origination=upper(row.ORIGINATION_STATUS_CODE);
  const recordStatus=upper(row.RECORD_STATUS_CODE);
  const date=today();
  return (!versionStatus || ['ACTIVE','APPROVED'].includes(versionStatus))
    && (!origination || origination==='OPEN')
    && (!recordStatus || recordStatus==='ACTIVE')
    && (!row.VALID_FROM || String(row.VALID_FROM).slice(0,10)<=date)
    && (!row.VALID_TO || String(row.VALID_TO).slice(0,10)>=date);
}

async function ensureProductAndVersion(productDesc,versionDesc,spec){
  let page=await rows('PRODUCT',{filterColumn:'PRODUCT_CODE',filterValue:spec.code});
  let product=page.items?.[0];
  let productPayload={
    PRODUCT_CODE:spec.code,
    PRODUCT_NAME:spec.name,
    PRODUCT_CLASS_CODE:'DEPOSIT',
    BALANCE_NATURE_CODE:'LIABILITY',
    PRODUCT_FAMILY_CODE:spec.family,
    DEFAULT_CURRENCY_CODE:'IRR',
    DESCRIPTION:'Phase 10F runtime qualification seed',
    PRODUCT_STATUS_CODE:chooseNonRuntimeValue(productDesc,'PRODUCT_STATUS_CODE',['ACTIVE','DRAFT'])
  };
  Object.keys(productPayload).forEach(k=>productPayload[k]===undefined&&delete productPayload[k]);
  productPayload=completeRequiredPayload(productDesc,productPayload);

  if(!product){
    console.log(`[${spec.family}] creating PDL.PRODUCT ${spec.code}`);
    product=await create('PRODUCT',productPayload);
  }else{
    const needsRepair=upper(product.PRODUCT_CLASS_CODE)!=='DEPOSIT'
      || upper(product.PRODUCT_FAMILY_CODE)!==spec.family
      || upper(product.DEFAULT_CURRENCY_CODE)!=='IRR';
    if(needsRepair){
      console.log(`[${spec.family}] repairing dedicated qualification product ${spec.code}`);
      product=await update('PRODUCT',Number(product.PRODUCT_ID),productPayload);
    }else{
      console.log(`[${spec.family}] reusing PDL.PRODUCT id=${product.PRODUCT_ID}`);
    }
  }

  assert(Number(product.PRODUCT_ID)>0,`PRODUCT_ID missing for ${spec.family}`);
  page=await rows('PRODUCT_VERSION',{filterColumn:'PRODUCT_ID',filterValue:Number(product.PRODUCT_ID)});
  let version=(page.items||[]).filter(versionEligible).sort((a,b)=>Number(b.VERSION_NO||0)-Number(a.VERSION_NO||0))[0];
  if(!version){
    const maxVersion=(page.items||[]).reduce((m,v)=>Math.max(m,Number(v.VERSION_NO)||0),0);
    let payload={
      PRODUCT_ID:Number(product.PRODUCT_ID),
      SOURCE_VERSION_ID:null,
      VERSION_NO:maxVersion+1,
      VALID_FROM:today(),
      VALID_TO:null,
      VERSION_STATUS_CODE:chooseRuntimeValue(versionDesc,'VERSION_STATUS_CODE',['ACTIVE','APPROVED']),
      IS_CURRENT:1,
      ORIGINATION_STATUS_CODE:chooseRuntimeValue(versionDesc,'ORIGINATION_STATUS_CODE',['OPEN']),
      SERVICING_STATUS_CODE:chooseNonRuntimeValue(versionDesc,'SERVICING_STATUS_CODE',['ACTIVE','OPEN','ENABLED','CLOSED','DISABLED']),
      CHANGE_REASON:'Phase 10F runtime qualification seed',
      RECORD_STATUS_CODE:chooseRuntimeValue(versionDesc,'RECORD_STATUS_CODE',['ACTIVE'])
    };
    Object.keys(payload).forEach(k=>payload[k]===undefined&&delete payload[k]);
    payload=completeRequiredPayload(versionDesc,payload);
    console.log(`[${spec.family}] creating runtime-eligible PDL.PRODUCT_VERSION`);
    version=await create('PRODUCT_VERSION',payload);
  }else{
    console.log(`[${spec.family}] reusing PRODUCT_VERSION id=${version.PRODUCT_VERSION_ID}`);
  }
  assert(versionEligible(version),`PRODUCT_VERSION ${version.PRODUCT_VERSION_ID} is not runtime eligible`);
  return {product,version};
}

const createGateChecks=[
  ['CUSTOMER_IDENTITY','COMPLIANCE','PRE_ACCOUNT_CREATION'],
  ['MOBILE_OWNERSHIP','COMPLIANCE','PRE_ACCOUNT_CREATION'],
  ['LEGAL_CAPACITY','COMPLIANCE','PRE_ACCOUNT_CREATION'],
  ['KYC_CDD','COMPLIANCE','PRE_ACCOUNT_CREATION'],
  ['PEP_SANCTIONS','COMPLIANCE','PRE_ACCOUNT_CREATION'],
  ['CUSTOMER_RISK','COMPLIANCE','PRE_APPROVAL'],
  ['EXPECTED_ACTIVITY','COMPLIANCE','PRE_APPROVAL'],
  ['ACCOUNT_COUNT_STATUS','INQUIRY','PRE_ACCOUNT_CREATION'],
  ['PRODUCT_ELIGIBILITY','PRODUCT_RULE','PRE_ACCOUNT_CREATION'],
  ['DOCUMENTS','DOCUMENT','PRE_ACCOUNT_CREATION'],
  ['INQUIRIES','INQUIRY','PRE_ACCOUNT_CREATION'],
  ['SIGNATORY_AUTHORITY','AUTHORITY','PRE_ACCOUNT_CREATION'],
  ['TERMS_ACCEPTANCE','CONSENT','PRE_ACCOUNT_CREATION'],
  ['SHARIA_CONTRACT','PRODUCT_RULE','PRE_ACCOUNT_CREATION'],
  ['TAX_PROFILE','INQUIRY','PRE_ACCOUNT_CREATION'],
  ['DUPLICATE_REQUEST','OPERATIONAL','PRE_ACCOUNT_CREATION']
];

function aggregateFor(spec,productVersionId,runToken){
  const amount=openingAmount;
  const familyToken={
    QARD_SAVINGS:'QS',CURRENT_ACCOUNT:'CA',SHORT_TERM_DEPOSIT:'ST',LONG_TERM_DEPOSIT:'LT'
  }[spec.family];
  const requestNo=`P10F-${familyToken}-${runToken}`.slice(0,40);
  const key=`P10F-${spec.family}-${runToken}`.slice(0,80);
  const evidencePrefix=`P10F-${familyToken}-${runToken}`;
  return {
    DEPOSIT_OPENING_REQUEST:{
      REQUEST_NO:requestNo,
      IDEMPOTENCY_KEY:key,
      PRODUCT_VERSION_ID:Number(productVersionId),
      REQUEST_TYPE_CODE:'CUSTOMER_REQUEST',
      OWNERSHIP_TYPE_CODE:'INDIVIDUAL',
      CURRENCY_CODE:'IRR',
      OPENING_CHANNEL_CODE:'BRANCH',
      ORG_UNIT_CODE:'001',
      REQUESTED_OPENING_DATE:today(),
      OPENING_AMOUNT:amount,
      SOURCE_OF_FUNDS_CODE:'SAVINGS',
      PURPOSE_CODE:spec.purpose,
      CUSTOMER_RISK_LEVEL_CODE:'LOW',
      RISK_ASSESSMENT_REFERENCE:`${evidencePrefix}-RISK`,
      EXPECTED_ACTIVITY_REFERENCE:`${evidencePrefix}-ACTIVITY`,
      ACTIVATION_STATUS_CODE:'NOT_CREATED',
      REQUEST_STATUS_CODE:'APPROVED'
    },
    DEPOSIT_OPENING_PARTY:[{
      PARTY_ID:partyId,
      ROLE_CODE:'OWNER',
      IS_PRIMARY:1,
      OWNERSHIP_PERCENT:100,
      SEQUENCE_NO:1
    }],
    DEPOSIT_OPENING_OBLIGATION:[{
      OPENING_OBLIGATION_ID:1,
      OBLIGATION_TYPE_CODE:'INITIAL_BALANCE',
      SOURCE_SYSTEM_CODE:'PHASE10F',
      SOURCE_REFERENCE:`${evidencePrefix}-INITIAL-BALANCE`,
      DESCRIPTION:'Phase 10F opening balance',
      GROSS_AMOUNT:amount,
      WAIVED_AMOUNT:0,
      FINAL_AMOUNT:amount,
      CURRENCY_CODE:'IRR',
      MANDATORY_FOR_ACTIVATION_FLAG:1,
      SETTLEMENT_STATUS_CODE:'PENDING'
    }],
    DEPOSIT_OPENING_FUNDING:[{
      OPENING_FUNDING_ID:1,
      FUNDING_METHOD_CODE:'CASH',
      FUNDING_AMOUNT:amount,
      SOURCE_PARTY_ID:partyId,
      SOURCE_REFERENCE:`${evidencePrefix}-FUNDING`,
      FUNDING_PURPOSE_CODE:'INITIAL_BALANCE',
      SOURCE_OWNERSHIP_VERIFIED_FLAG:1,
      SOURCE_VERIFICATION_REFERENCE:`${evidencePrefix}-SOURCE-VERIFIED`,
      CASH_MANAGEMENT_TXN_REF:`${evidencePrefix}-CASH-MGMT`,
      FUNDING_STATUS_CODE:'PENDING'
    }],
    DEPOSIT_OPENING_CHECK:createGateChecks.map(([code,type,phase],index)=>({
      CHECK_CODE:code,
      CHECK_TYPE_CODE:type,
      ATTEMPT_NO:1,
      CHECK_PHASE_CODE:phase,
      BLOCKING_SCOPE_CODE:'ACCOUNT_CREATION',
      REQUIRED_FLAG:1,
      RECHECK_REQUIRED_FLAG:0,
      RESULT_STATUS_CODE:'PASS',
      RESULT_REFERENCE:`${evidencePrefix}-CG-${String(index+1).padStart(2,'0')}`,
      SOURCE_EVALUATION_REFERENCE:'PHASE10F_RUNTIME_QUALIFICATION'
    })),
    DEPOSIT_OPENING_TERMS_ACCEPTANCE:{
      TERMS_VERSION_CODE:'PHASE10F-2026.09',
      ACCEPTED_BY_PARTY_ID:partyId,
      ACCEPTANCE_SOURCE_CODE:'API',
      CHANNEL_CODE:'BRANCH',
      ACCEPTANCE_STATUS_CODE:'ACCEPTED',
      EVIDENCE_REFERENCE:`${evidencePrefix}-TERMS`
    },
    DEPOSIT_OPENING_DECISION:{
      DECISION_CODE:'APPROVE',
      DECISION_REASON_CODE:'ALL_CHECKS_PASSED',
      DECISION_NOTE:'Phase 10F runtime qualification',
      DECIDED_BY:'phase10f.qa'
    }
  };
}

function readinessEvidence(runToken,family){
  const validUntil=plusHours(2);
  return ['CBI_SIAH_REGISTRATION','FINAL_COMPLIANCE_RECHECK','RESTRICTIONS_READY'].map(code=>({
    CHECK_CODE:code,
    RESULT_STATUS_CODE:'PASS',
    RESULT_REFERENCE:`P10F-${family}-${runToken}-${code}`.slice(0,120),
    VALID_UNTIL:validUntil,
    SOURCE_EVALUATION_REFERENCE:'PHASE10F_CONTROLLED_EVIDENCE'
  }));
}

async function main(){
  assert(Number.isInteger(partyId)&&partyId>0,'PHASE10F_PARTY_ID must be a positive integer');
  assert(Number.isFinite(openingAmount)&&openingAmount>0,'PHASE10F_OPENING_AMOUNT must be > 0');
  const health=await request('GET','/actuator/health');
  assert(health?.status==='UP','Application health is not UP');

  const readiness=await request('GET','/api/v1/deposit-opening/readiness');
  assert(readiness?.status==='READY_WITH_WARNINGS'||readiness?.status==='READY',`Deposit opening readiness is ${readiness?.status}`);

  const productDesc=await descriptor('PRODUCT');
  const versionDesc=await descriptor('PRODUCT_VERSION');
  ['PRODUCT_ID','PRODUCT_CODE','PRODUCT_NAME','PRODUCT_CLASS_CODE','BALANCE_NATURE_CODE','PRODUCT_FAMILY_CODE','DEFAULT_CURRENCY_CODE','PRODUCT_STATUS_CODE']
    .forEach(name=>column(productDesc,name));
  ['PRODUCT_VERSION_ID','PRODUCT_ID','VERSION_NO','VALID_FROM','VERSION_STATUS_CODE','IS_CURRENT','ORIGINATION_STATUS_CODE','SERVICING_STATUS_CODE','RECORD_STATUS_CODE']
    .forEach(name=>column(versionDesc,name));

  const runToken=`${Date.now().toString(36)}-${randomUUID().slice(0,8)}`;
  const cases=[];
  for(const spec of families){
    const {version}=await ensureProductAndVersion(productDesc,versionDesc,spec);
    const aggregate=aggregateFor(spec,version.PRODUCT_VERSION_ID,runToken);
    console.log(`[${spec.family}] validating final aggregate`);
    const validation=await request('POST','/api/v1/deposit-opening/requests/validate',aggregate);
    assert(validation?.valid===true,`Aggregate validation failed for ${spec.family}`);
    assert(upper(validation.productFamilyCode)===spec.family,
      `PDL family mismatch for ${spec.family}: runtime returned ${validation.productFamilyCode}`);
    cases.push({
      family:spec.family,
      aggregate,
      settlement:{SETTLEMENT_REFERENCE:`P10F-${spec.family}-${runToken}-SETTLE`.slice(0,120)},
      readinessEvidence:readinessEvidence(runToken,spec.family)
    });
  }

  const fixture={generatedAt:new Date().toISOString(),baseUrl:base,partyId,cases};
  fs.mkdirSync(path.dirname(outPath),{recursive:true});
  fs.writeFileSync(outPath,JSON.stringify(fixture,null,2)+'\n','utf8');
  console.log(`\nPHASE10F_FIXTURE_READY ${outPath}`);
  for(const c of cases){
    console.log(`${c.family}: PRODUCT_VERSION_ID=${c.aggregate.DEPOSIT_OPENING_REQUEST.PRODUCT_VERSION_ID}`);
  }
  console.log(`\nNext: node tools/runtime-dps2-phase10f-e2e.mjs "${path.relative(root,outPath)}"`);
}

main().catch(error=>{
  console.error('\nPHASE10F_FIXTURE_PREPARE_FAIL');
  console.error(error?.stack||error);
  process.exit(1);
});
