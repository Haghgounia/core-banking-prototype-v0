import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const base=(process.env.CORE_BANKING_BASE_URL||'http://127.0.0.1:8091').replace(/\/$/,'');
const actor='phase11nb.product-version.qa';
const fixturePath=path.join(root,'docs/examples/phase10f-e2e-fixture.runtime.json');
const headers={'Content-Type':'application/json','X-User-Id':actor,'X-User-Name':actor,'X-Correlation-Id':`11nb-pv-${Date.now()}`};
const today=()=>new Date().toISOString().slice(0,10);
const upper=v=>v==null?'':String(v).trim().toUpperCase();
const assert=(c,m)=>{if(!c)throw new Error(m)};

async function req(method,url,body){
  const r=await fetch(base+url,{method,headers,body:body===undefined?undefined:JSON.stringify(body)});
  const t=await r.text();let d;try{d=t?JSON.parse(t):null}catch{d=t}
  if(!r.ok)throw new Error(`${method} ${url} -> ${r.status}: ${typeof d==='string'?d:JSON.stringify(d)}`);
  return d;
}
async function row(table,id){return req('GET',`/api/v1/product-builder/tables/${encodeURIComponent(table)}/rows/${id}`)}
async function rows(table,filterColumn,filterValue){const q=new URLSearchParams({page:'0',size:'200',filterColumn,filterValue:String(filterValue)});return req('GET',`/api/v1/product-builder/tables/${encodeURIComponent(table)}/rows?${q}`)}
async function descriptor(table){return req('GET',`/api/v1/product-builder/tables/${encodeURIComponent(table)}/descriptor`)}
async function create(table,body){return req('POST',`/api/v1/product-builder/tables/${encodeURIComponent(table)}/rows`,body)}
function optionCodes(col){return (col?.options||[]).map(o=>upper(o.code??o.value)).filter(Boolean)}
function col(desc,name){const c=(desc?.columns||[]).find(x=>upper(x.name)===upper(name));assert(c,`${desc?.tableName||'PRODUCT_VERSION'} missing ${name}`);return c}
function choose(desc,name,preferred){const c=col(desc,name),opts=optionCodes(c);for(const v of preferred){if(!opts.length||opts.includes(upper(v)))return v}throw new Error(`PHASE11NB_PRODUCT_VERSION_METADATA_UNSUPPORTED:${name}:${opts.join(',')}`)}
function eligible(v,sourceId){
  if(Number(v.PRODUCT_VERSION_ID)===sourceId)return false;
  const date=today();
  return ['ACTIVE','APPROVED'].includes(upper(v.VERSION_STATUS_CODE))
    && upper(v.SERVICING_STATUS_CODE)==='ACTIVE'
    && upper(v.RECORD_STATUS_CODE)==='ACTIVE'
    && (!v.VALID_FROM||String(v.VALID_FROM).slice(0,10)<=date)
    && (!v.VALID_TO||String(v.VALID_TO).slice(0,10)>=date);
}

async function main(){
  const health=await req('GET','/actuator/health');assert(health?.status==='UP','PHASE11NB_RUNTIME_NOT_UP');
  assert(fs.existsSync(fixturePath),'PHASE11NB_PHASE10F_FIXTURE_MISSING');
  const fixture=JSON.parse(fs.readFileSync(fixturePath,'utf8'));
  const c=(fixture.cases||[]).find(x=>x.family==='QARD_SAVINGS')||fixture.cases?.[0];
  const sourceId=Number(c?.aggregate?.DEPOSIT_OPENING_REQUEST?.PRODUCT_VERSION_ID||0);
  assert(sourceId>0,'PHASE11NB_SOURCE_PRODUCT_VERSION_MISSING');
  const source=await row('PRODUCT_VERSION',sourceId);
  const productId=Number(source.PRODUCT_ID||0);assert(productId>0,'PHASE11NB_SOURCE_PRODUCT_MISSING');
  const product=await row('PRODUCT',productId);
  const productCode=upper(product.PRODUCT_CODE);
  if(!productCode.startsWith('P10F_')){
    throw new Error(`PHASE11NB_REAL_PRODUCT_CONFIG_REQUIRED:${productCode||productId}: automatic qualification version creation is restricted to dedicated P10F_* products`);
  }
  const page=await rows('PRODUCT_VERSION','PRODUCT_ID',productId);
  const versions=page.items||[];
  const existing=versions.filter(v=>eligible(v,sourceId)).sort((a,b)=>Number(b.PRODUCT_VERSION_ID)-Number(a.PRODUCT_VERSION_ID))[0];
  if(existing){
    console.log(`PHASE11NB_QUALIFICATION_PRODUCT_VERSION_KEEP=${productId}:${sourceId}->${existing.PRODUCT_VERSION_ID}`);
    return;
  }
  const desc=await descriptor('PRODUCT_VERSION');
  const maxNo=versions.reduce((m,v)=>Math.max(m,Number(v.VERSION_NO)||0),0);
  const payload={
    PRODUCT_ID:productId,
    SOURCE_VERSION_ID:sourceId,
    VERSION_NO:maxNo+1,
    VALID_FROM:today(),
    VALID_TO:null,
    VERSION_STATUS_CODE:choose(desc,'VERSION_STATUS_CODE',['ACTIVE','APPROVED']),
    IS_CURRENT:0,
    ORIGINATION_STATUS_CODE:choose(desc,'ORIGINATION_STATUS_CODE',['CLOSED']),
    SERVICING_STATUS_CODE:choose(desc,'SERVICING_STATUS_CODE',['ACTIVE']),
    CHANGE_REASON:'Phase 11N-B qualification-only servicing migration target',
    RECORD_STATUS_CODE:choose(desc,'RECORD_STATUS_CODE',['ACTIVE'])
  };
  const created=await create('PRODUCT_VERSION',payload);
  const targetId=Number(created.PRODUCT_VERSION_ID||0);assert(targetId>0,'PHASE11NB_ALT_PRODUCT_VERSION_CREATE_FAIL');
  assert(Number(created.PRODUCT_ID)===productId,'PHASE11NB_ALT_PRODUCT_VERSION_PRODUCT_MISMATCH');
  assert(Number(created.IS_CURRENT||0)===0,'PHASE11NB_ALT_PRODUCT_VERSION_MUST_BE_NONCURRENT');
  assert(upper(created.ORIGINATION_STATUS_CODE)==='CLOSED','PHASE11NB_ALT_PRODUCT_VERSION_ORIGINATION_MUST_BE_CLOSED');
  assert(upper(created.SERVICING_STATUS_CODE)==='ACTIVE','PHASE11NB_ALT_PRODUCT_VERSION_SERVICING_MUST_BE_ACTIVE');
  console.log(`PHASE11NB_QUALIFICATION_PRODUCT_VERSION_CREATED=${productId}:${sourceId}->${targetId}`);
  console.log('PHASE11NB_QUALIFICATION_PRODUCT_VERSION_READY');
}
main().catch(e=>{console.error('PHASE11NB_QUALIFICATION_PRODUCT_VERSION_PREPARE_FAIL');console.error(e?.stack||e);process.exit(1)});
