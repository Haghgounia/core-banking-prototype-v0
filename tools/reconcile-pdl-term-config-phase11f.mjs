const base=(process.env.CORE_BANKING_BASE_URL||'http://127.0.0.1:8091').replace(/\/$/,'');
const actor=process.env.PDL_TERM_REPAIR_ACTOR||'phase11f.pdl.repair';
const apply=process.argv.includes('--apply');
const today=()=>new Date().toISOString().slice(0,10);
const text=(v)=>v==null?'':String(v).trim();
const upper=(v)=>text(v).toUpperCase();
const number=(v)=>{const n=Number(v);return Number.isFinite(n)?n:null};
const truthyFlag=(v)=>v==null||v===''||v===true||v===1||upper(v)==='Y'||upper(v)==='ACTIVE';

async function req(method,path,body){
  const headers={'Content-Type':'application/json','X-User-Name':actor};
  const r=await fetch(base+path,{method,headers,body:body===undefined?undefined:JSON.stringify(body)});
  const t=await r.text();
  if(!r.ok)throw new Error(`${method} ${path} -> ${r.status}: ${t}`);
  return t?JSON.parse(t):null;
}
async function descriptor(table){return req('GET',`/api/v1/product-builder/tables/${encodeURIComponent(table)}/descriptor`)}
async function rows(table,filterColumn,filterValue){
  const q=new URLSearchParams({page:'0',size:'200'});
  if(filterColumn)q.set('filterColumn',filterColumn);
  if(filterValue!=null)q.set('filterValue',String(filterValue));
  return req('GET',`/api/v1/product-builder/tables/${encodeURIComponent(table)}/rows?${q}`);
}
async function create(table,payload){return req('POST',`/api/v1/product-builder/tables/${encodeURIComponent(table)}/rows`,payload)}
async function lookup(table,column){return req('GET',`/api/v1/product-builder/tables/${encodeURIComponent(table)}/lookups/${encodeURIComponent(column)}?limit=50`)}
async function referenceLookup(resource){
  try{return await req('GET',`/api/v1/reference/${encodeURIComponent(resource)}/lookup?limit=200`)}
  catch(e){console.log(`PHASE11F_PDL_REPAIR_REFERENCE_LOOKUP_SKIP resource=${resource} reason=${text(e?.message).split('\n')[0]}`);return []}
}

function columns(desc){return new Map((desc.columns||[]).map(c=>[upper(c.name),c]))}
function columnName(desc,candidates){const c=columns(desc);return candidates.find(x=>c.has(x))||null}
function hasDefault(c){return c.defaultValue!=null&&text(c.defaultValue)!==''&&upper(c.defaultValue)!=='NULL'}
function activeNow(row){
  if(Object.hasOwn(row,'IS_ACTIVE')&&!truthyFlag(row.IS_ACTIVE))return false;
  if(Object.hasOwn(row,'IS_CURRENT')&&!truthyFlag(row.IS_CURRENT))return false;
  const now=today();
  const from=text(row.VALID_FROM||row.EFFECTIVE_FROM_DATE).slice(0,10);
  const to=text(row.VALID_TO||row.EFFECTIVE_TO_DATE).slice(0,10);
  return (!from||from<=now)&&(!to||to>=now);
}
function openingEligibleVersion(row){
  const status=upper(row.VERSION_STATUS_CODE),origination=upper(row.ORIGINATION_STATUS_CODE),recordStatus=upper(row.RECORD_STATUS_CODE);
  if(status&&!['ACTIVE','APPROVED'].includes(status))return false;
  if(origination&&origination!=='OPEN')return false;
  if(recordStatus&&recordStatus!=='ACTIVE')return false;
  return activeNow(row);
}
function saneOption(o){
  const v=upper(o?.code??o?.value);
  return !!v&&!v.includes(' AND ')&&!v.includes(' OR ')&&!v.includes(' IN (')&&!/[()]/.test(v);
}
function saneOptions(c){return (c?.options||[]).filter(saneOption)}
function optionValue(c,preferences=[]){
  const opts=saneOptions(c);
  for(const pref of preferences){
    const hit=opts.find(o=>upper(o.code)===upper(pref)||upper(o.value)===upper(pref));
    if(hit)return hit.value;
  }
  return opts.length?opts[0].value:undefined;
}
function requiredColumns(desc){return (desc.columns||[]).filter(c=>!c.primaryKey&&!c.readOnly&&!c.nullable&&!hasDefault(c))}
function describeRequired(desc){return requiredColumns(desc).map(c=>`${c.name}:${c.dataType}${c.foreignKey?`->${c.parentTable}`:''}${saneOptions(c).length?` options=[${saneOptions(c).map(o=>o.code||o.value).join(',')}]`:''}`).join('; ')}
function dataConflict(error){const m=text(error?.message);return m.includes('"errorCode":"DATA_CONFLICT"')||m.includes('errorCode=DATA_CONFLICT')}
function uniqueUpper(values){return [...new Set(values.map(upper).filter(Boolean))]}

async function requiredFallback(table,c,context){
  const name=upper(c.name),type=upper(c.dataType);
  if(c.foreignKey){
    const options=await lookup(table,c.name);
    if(options.length)return options[0].value;
    throw new Error(`NO_LOOKUP_VALUE_FOR_REQUIRED_FK:${table}.${name}->${c.parentTable}`);
  }
  if(saneOptions(c).length){
    const prefs=[];
    if(name.includes('STATUS'))prefs.push('ACTIVE','APPROVED','CONFIGURED','VALID','OPEN');
    if(name.includes('UNIT'))prefs.push('DAY','MONTH','YEAR');
    if(name.startsWith('IS_')||name.endsWith('_FLAG')||name.includes('ALLOWED'))prefs.push('1','0');
    const v=optionValue(c,prefs);if(v!==undefined)return v;
  }
  if(type.startsWith('NUMBER')){
    if(name.includes('MAX'))return 120;
    if(name.includes('MIN'))return 1;
    if(name.includes('PERCENT'))return 100;
    return 1;
  }
  if(type==='DATE')return today();
  if(type.startsWith('TIMESTAMP'))return `${today()}T00:00:00`;
  if(name.includes('STATUS'))return 'ACTIVE';
  if(name.includes('UNIT'))return 'DAY';
  if(name.endsWith('_CODE'))return context.code||'PHASE11F';
  return 'PHASE11F';
}

async function buildPayload(table,desc,explicit,context={}){
  const cmap=columns(desc);const payload={};
  for(const [k,v] of Object.entries(explicit))if(cmap.has(upper(k)))payload[upper(k)]=v;
  for(const c of requiredColumns(desc)){
    const name=upper(c.name);if(Object.hasOwn(payload,name))continue;
    payload[name]=await requiredFallback(table,c,context);
  }
  return payload;
}

function allowedTermFromRow(row,desc){
  const idColumn=columnName(desc,['ALLOWED_TERM_ID'])||upper(desc.primaryKeyColumn);
  const allowedTermId=number(row[idColumn]);
  let termValue=null,termUnitCode='';
  const valueColumn=columnName(desc,['TERM_VALUE','DURATION_VALUE','TERM_LENGTH_VALUE']);
  const unitColumn=columnName(desc,['TERM_UNIT_CODE','DURATION_UNIT_CODE','TERM_LENGTH_UNIT_CODE']);
  if(valueColumn&&unitColumn){termValue=number(row[valueColumn]);termUnitCode=upper(row[unitColumn])}
  else if(Object.hasOwn(row,'TERM_DAYS')){termValue=number(row.TERM_DAYS);termUnitCode='DAY'}
  else if(Object.hasOwn(row,'TERM_MONTHS')){termValue=number(row.TERM_MONTHS);termUnitCode='MONTH'}
  else if(Object.hasOwn(row,'TERM_YEARS')){termValue=number(row.TERM_YEARS);termUnitCode='YEAR'}
  const codeColumn=columnName(desc,['TERM_CODE','ALLOWED_TERM_CODE','CODE']);
  const termCode=codeColumn?text(row[codeColumn]):'';
  return {allowedTermId,termCode,termValue,termUnitCode,usable:allowedTermId>0&&termValue>0&&['DAY','MONTH','YEAR'].includes(termUnitCode)&&!!termCode};
}

async function maturityActionCandidates(ruleDesc){
  const col=columns(ruleDesc).get('MATURITY_ACTION_CODE');
  const descriptorValues=saneOptions(col).map(o=>o.code??o.value);
  const referenceValues=(await referenceLookup('dps2-maturity-action')).map(o=>o.code);
  return uniqueUpper([
    ...descriptorValues,
    ...referenceValues,
    'RENEW_PRINCIPAL','RENEW_PRINCIPAL_PAY_PROFIT','CLOSE_AND_SETTLE','PAY_TO_ACCOUNT','WAIT_INSTRUCTION'
  ]);
}

function termUnitCandidates(allowedDesc){
  const name=columnName(allowedDesc,['TERM_UNIT_CODE','DURATION_UNIT_CODE','TERM_LENGTH_UNIT_CODE']);
  const col=name?columns(allowedDesc).get(name):null;
  const descriptorValues=saneOptions(col).map(o=>o.code??o.value);
  return uniqueUpper([...descriptorValues,'DAY','MONTH','YEAR']).filter(v=>['DAY','MONTH','YEAR'].includes(v));
}

async function createTermRule(versionId,versionColumn,ruleDesc){
  const actions=await maturityActionCandidates(ruleDesc);
  console.log(`PHASE11F_PDL_REPAIR_MATURITY_CANDIDATES=${actions.join(',')}`);
  let lastError=null;
  for(const action of actions){
    const renewable=action.startsWith('RENEW_')?1:0;
    const explicit={
      [versionColumn]:versionId,
      IS_ACTIVE:1,IS_CURRENT:1,VALID_FROM:today(),EFFECTIVE_FROM_DATE:today(),
      RECORD_STATUS_CODE:'ACTIVE',RULE_STATUS_CODE:'ACTIVE',STATUS_CODE:'ACTIVE',
      IS_RENEWABLE:renewable,MATURITY_ACTION_CODE:action,GRACE_PERIOD_DAYS:0
    };
    const payload=await buildPayload('DEPOSIT_PRODUCT_TERM_RULE',ruleDesc,explicit,{code:action});
    console.log(`PHASE11F_PDL_REPAIR_TERM_RULE_PLAN productVersionId=${versionId} maturityAction=${action} payload=${JSON.stringify(payload)}`);
    if(!apply)return null;
    try{return await create('DEPOSIT_PRODUCT_TERM_RULE',payload)}
    catch(e){
      lastError=e;
      if(dataConflict(e)){
        console.log(`PHASE11F_PDL_REPAIR_TERM_RULE_REJECTED productVersionId=${versionId} maturityAction=${action} reason=DATA_CONFLICT`);
        continue;
      }
      throw new Error(`${e.message}\nREQUIRED_COLUMNS=${describeRequired(ruleDesc)}\nPAYLOAD=${JSON.stringify(payload)}`);
    }
  }
  throw new Error(`${lastError?.message||'TERM_RULE_CANDIDATES_EXHAUSTED'}\nREQUIRED_COLUMNS=${describeRequired(ruleDesc)}\nMATURITY_CANDIDATES=${actions.join(',')}`);
}

async function createAllowedTerm(ruleId,allowedDesc,versionId){
  const units=termUnitCandidates(allowedDesc);
  let lastError=null;
  for(const unit of units){
    const termValue=unit==='DAY'?30:1;
    const termCode=`P11F_${versionId}_${termValue}${unit==='DAY'?'D':unit==='MONTH'?'M':'Y'}`;
    const explicit={
      TERM_RULE_ID:ruleId,TERM_CODE:termCode,ALLOWED_TERM_CODE:termCode,CODE:termCode,
      TERM_VALUE:termValue,DURATION_VALUE:termValue,TERM_LENGTH_VALUE:termValue,
      TERM_UNIT_CODE:unit,DURATION_UNIT_CODE:unit,TERM_LENGTH_UNIT_CODE:unit,
      TERM_DAYS:unit==='DAY'?termValue:undefined,TERM_MONTHS:unit==='MONTH'?termValue:undefined,TERM_YEARS:unit==='YEAR'?termValue:undefined,
      IS_DEFAULT:1,DISPLAY_ORDER:1,IS_ACTIVE:1,IS_CURRENT:1,VALID_FROM:today(),EFFECTIVE_FROM_DATE:today(),RECORD_STATUS_CODE:'ACTIVE',STATUS_CODE:'ACTIVE'
    };
    const payload=await buildPayload('DEPOSIT_PRODUCT_ALLOWED_TERM',allowedDesc,explicit,{code:termCode});
    for(const k of Object.keys(payload))if(payload[k]===undefined)delete payload[k];
    console.log(`PHASE11F_PDL_REPAIR_ALLOWED_TERM_PLAN productVersionId=${versionId} termRuleId=${ruleId} unit=${unit} payload=${JSON.stringify(payload)}`);
    if(!apply)return null;
    try{return await create('DEPOSIT_PRODUCT_ALLOWED_TERM',payload)}
    catch(e){
      lastError=e;
      if(dataConflict(e)){
        console.log(`PHASE11F_PDL_REPAIR_ALLOWED_TERM_REJECTED productVersionId=${versionId} termRuleId=${ruleId} unit=${unit} reason=DATA_CONFLICT`);
        continue;
      }
      throw new Error(`${e.message}\nREQUIRED_COLUMNS=${describeRequired(allowedDesc)}\nPAYLOAD=${JSON.stringify(payload)}`);
    }
  }
  throw new Error(`${lastError?.message||'ALLOWED_TERM_CANDIDATES_EXHAUSTED'}\nREQUIRED_COLUMNS=${describeRequired(allowedDesc)}\nTERM_UNIT_CANDIDATES=${units.join(',')}`);
}

async function ensureAllowedTerm(ruleId,allowedDesc,versionId){
  const existing=(await rows('DEPOSIT_PRODUCT_ALLOWED_TERM','TERM_RULE_ID',ruleId)).items||[];
  for(const row of existing.filter(activeNow)){
    const term=allowedTermFromRow(row,allowedDesc);
    if(term.usable){console.log(`PHASE11F_PDL_REPAIR_ALLOWED_TERM_OK productVersionId=${versionId} termRuleId=${ruleId} allowedTermId=${term.allowedTermId} code=${term.termCode} value=${term.termValue} unit=${term.termUnitCode}`);return term;}
  }
  const created=await createAllowedTerm(ruleId,allowedDesc,versionId);
  if(!apply)return null;
  const term=allowedTermFromRow(created,allowedDesc);
  if(!term.usable)throw new Error(`CREATED_ALLOWED_TERM_NOT_USABLE:${JSON.stringify(created)}`);
  console.log(`PHASE11F_PDL_REPAIR_ALLOWED_TERM_CREATED productVersionId=${versionId} termRuleId=${ruleId} allowedTermId=${term.allowedTermId} code=${term.termCode} value=${term.termValue} unit=${term.termUnitCode}`);
  return term;
}

async function ensureTermConfiguration(version,ruleDesc,allowedDesc){
  const versionId=number(version.PRODUCT_VERSION_ID);if(!(versionId>0))return {changed:false,usable:false};
  const versionColumn=columnName(ruleDesc,['PRODUCT_VERSION_ID','DEPOSIT_PRODUCT_VERSION_ID']);
  if(!versionColumn)throw new Error('PDL_TERM_RULE_PRODUCT_VERSION_COLUMN_NOT_FOUND');
  const existing=(await rows('DEPOSIT_PRODUCT_TERM_RULE',versionColumn,versionId)).items||[];
  for(const rule of existing.filter(activeNow)){
    const ruleId=number(rule.TERM_RULE_ID??rule[ruleDesc.primaryKeyColumn]);if(!(ruleId>0))continue;
    const term=await ensureAllowedTerm(ruleId,allowedDesc,versionId);
    if(term?.usable||!apply)return {changed:!term,usable:!!term};
  }
  const created=await createTermRule(versionId,versionColumn,ruleDesc);
  if(!apply)return {changed:false,usable:false};
  const ruleId=number(created.TERM_RULE_ID??created[ruleDesc.primaryKeyColumn]);
  if(!(ruleId>0))throw new Error(`CREATED_TERM_RULE_ID_MISSING:${JSON.stringify(created)}`);
  console.log(`PHASE11F_PDL_REPAIR_TERM_RULE_CREATED productVersionId=${versionId} termRuleId=${ruleId} maturityAction=${upper(created.MATURITY_ACTION_CODE)}`);
  const term=await ensureAllowedTerm(ruleId,allowedDesc,versionId);
  return {changed:true,usable:!!term?.usable};
}

try{
  console.log(`PHASE11F_PDL_REPAIR_MODE=${apply?'APPLY':'DRY_RUN'}`);
  const [ruleDesc,allowedDesc]=await Promise.all([descriptor('DEPOSIT_PRODUCT_TERM_RULE'),descriptor('DEPOSIT_PRODUCT_ALLOWED_TERM')]);
  console.log(`PHASE11F_PDL_TERM_RULE_REQUIRED=${describeRequired(ruleDesc)}`);
  console.log(`PHASE11F_PDL_ALLOWED_TERM_REQUIRED=${describeRequired(allowedDesc)}`);
  const products=(await rows('PRODUCT')).items||[];
  const termProducts=products.filter(p=>upper(p.PRODUCT_CLASS_CODE)==='DEPOSIT'&&['SHORT_TERM_DEPOSIT','LONG_TERM_DEPOSIT'].includes(upper(p.PRODUCT_FAMILY_CODE)));
  let candidates=0,usable=0,plannedOrChanged=0;
  for(const product of termProducts){
    const productId=number(product.PRODUCT_ID);if(!(productId>0))continue;
    const versions=(await rows('PRODUCT_VERSION','PRODUCT_ID',productId)).items||[];
    for(const version of versions.filter(openingEligibleVersion)){
      candidates++;
      const versionId=number(version.PRODUCT_VERSION_ID);
      console.log(`PHASE11F_PDL_REPAIR_TARGET productId=${productId} productVersionId=${versionId} family=${upper(product.PRODUCT_FAMILY_CODE)} status=${upper(version.VERSION_STATUS_CODE)} origination=${upper(version.ORIGINATION_STATUS_CODE)}`);
      const result=await ensureTermConfiguration(version,ruleDesc,allowedDesc);
      if(result.usable)usable++;
      if(result.changed||!apply)plannedOrChanged++;
    }
  }
  if(!candidates)throw new Error('NO_OPEN_TERM_PRODUCT_VERSION_FOUND');
  if(!apply){console.log(`PHASE11F_PDL_REPAIR_DRY_RUN_COMPLETE candidates=${candidates}`);console.log('Re-run with --apply to create only missing governed TERM configuration.');process.exit(0);}
  // Post-condition: at least one opening-eligible term version must now expose a usable governed allowed term.
  let verified=0;
  for(const product of termProducts){
    const productId=number(product.PRODUCT_ID);if(!(productId>0))continue;
    const versions=(await rows('PRODUCT_VERSION','PRODUCT_ID',productId)).items||[];
    for(const version of versions.filter(openingEligibleVersion)){
      const versionId=number(version.PRODUCT_VERSION_ID);const versionColumn=columnName(ruleDesc,['PRODUCT_VERSION_ID','DEPOSIT_PRODUCT_VERSION_ID']);
      const rules=(await rows('DEPOSIT_PRODUCT_TERM_RULE',versionColumn,versionId)).items||[];
      for(const rule of rules.filter(activeNow)){
        const ruleId=number(rule.TERM_RULE_ID??rule[ruleDesc.primaryKeyColumn]);if(!(ruleId>0))continue;
        const allowed=(await rows('DEPOSIT_PRODUCT_ALLOWED_TERM','TERM_RULE_ID',ruleId)).items||[];
        if(allowed.filter(activeNow).some(r=>allowedTermFromRow(r,allowedDesc).usable)){verified++;console.log(`PHASE11F_PDL_REPAIR_VERIFIED productVersionId=${versionId} termRuleId=${ruleId}`);break;}
      }
    }
  }
  if(!verified)throw new Error('PDL_TERM_REPAIR_POSTCONDITION_FAILED');
  console.log(`PHASE11F_PDL_REPAIR_PASS candidates=${candidates} verified=${verified}`);
}catch(e){console.error('PHASE11F_PDL_REPAIR_FAIL');console.error(e);process.exit(1)}
