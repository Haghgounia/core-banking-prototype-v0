const base=(process.env.CORE_BANKING_BASE_URL||'http://127.0.0.1:8091').replace(/\/$/,'');
const text=v=>v==null?'':String(v).trim();
const upper=v=>text(v).toUpperCase();
const today=()=>new Date().toISOString().slice(0,10);
const assert=(ok,msg)=>{if(!ok)throw new Error(msg)};
const SPECS=[
 {code:'UAT-QS-STD-001',family:'QARD_SAVINGS',channels:3,customers:2,media:3,terms:0},
 {code:'UAT-QS-DIG-001',family:'QARD_SAVINGS',channels:2,customers:1,media:2,terms:0},
 {code:'UAT-QS-ORG-001',family:'QARD_SAVINGS',channels:2,customers:1,media:2,terms:0},
 {code:'UAT-CA-PER-001',family:'CURRENT_ACCOUNT',channels:1,customers:1,media:4,terms:0},
 {code:'UAT-CA-ORG-001',family:'CURRENT_ACCOUNT',channels:1,customers:1,media:3,terms:0},
 {code:'UAT-CA-NOCHEQUE-001',family:'CURRENT_ACCOUNT',channels:2,customers:1,media:3,terms:0},
 {code:'UAT-ST-STD-001',family:'SHORT_TERM_DEPOSIT',channels:3,customers:2,media:1,terms:3},
 {code:'UAT-ST-DIG-001',family:'SHORT_TERM_DEPOSIT',channels:2,customers:1,media:2,terms:3},
 {code:'UAT-ST-HIGH-001',family:'SHORT_TERM_DEPOSIT',channels:1,customers:2,media:0,terms:2},
 {code:'UAT-LT-STD-001',family:'LONG_TERM_DEPOSIT',channels:1,customers:2,media:0,terms:3},
 {code:'UAT-LT-PAYOUT-001',family:'LONG_TERM_DEPOSIT',channels:1,customers:1,media:0,terms:2},
 {code:'UAT-LT-RENEW-001',family:'LONG_TERM_DEPOSIT',channels:1,customers:2,media:0,terms:3}
];
const NEGATIVE_EXPECTATIONS={
 'UAT-QS-STD-001':[{versionNo:1,kind:'EXPIRED'}],
 'UAT-CA-ORG-001':[{versionNo:2,kind:'FUTURE'}],
 'UAT-ST-DIG-001':[{versionNo:2,kind:'SUSPENDED'}],
 'UAT-LT-RENEW-001':[{versionNo:1,kind:'EXPIRED'}]
};
async function req(path){const r=await fetch(base+path);const t=await r.text();let data;try{data=t?JSON.parse(t):null}catch{data=t}if(!r.ok)throw new Error(`GET ${path} -> ${r.status}: ${typeof data==='string'?data:JSON.stringify(data)}`);return data}
async function descriptor(table){return req(`/api/v1/product-builder/tables/${encodeURIComponent(table)}/descriptor`)}
async function rows(table,filterColumn,filterValue){const out=[];let page=0,total=1;while(out.length<total){const q=new URLSearchParams({page:String(page),size:'200'});if(filterColumn)q.set('filterColumn',filterColumn);if(filterValue!=null)q.set('filterValue',String(filterValue));const p=await req(`/api/v1/product-builder/tables/${encodeURIComponent(table)}/rows?${q}`);out.push(...(p.items||[]));total=Number(p.totalElements||0);if(!(p.items||[]).length)break;page++}return out}
function cols(desc){return new Set((desc.columns||[]).map(c=>upper(c.name)))}
function col(desc,names){const c=cols(desc);return names.find(n=>c.has(n))||null}
function truthy(v){return v==null||v===''||v===true||v===1||['1','Y','YES','TRUE','ACTIVE'].includes(upper(v))}
function activeRow(r){if(Object.hasOwn(r,'IS_ACTIVE')&&!truthy(r.IS_ACTIVE))return false;if(Object.hasOwn(r,'IS_CURRENT')&&!truthy(r.IS_CURRENT))return false;const from=text(r.VALID_FROM||r.EFFECTIVE_FROM_DATE).slice(0,10),to=text(r.VALID_TO||r.EFFECTIVE_TO_DATE).slice(0,10),now=today();return (!from||from<=now)&&(!to||to>=now)}
function openingEligibleVersion(v){const vs=upper(v.VERSION_STATUS_CODE),os=upper(v.ORIGINATION_STATUS_CODE),rs=upper(v.RECORD_STATUS_CODE),from=text(v.VALID_FROM).slice(0,10),to=text(v.VALID_TO).slice(0,10),now=today();return (!vs||['ACTIVE','APPROVED'].includes(vs))&&(!os||os==='OPEN')&&(!rs||rs==='ACTIVE')&&(!from||from<=now)&&(!to||to>=now)}
function negativeMatches(v,kind){const now=today(),from=text(v.VALID_FROM).slice(0,10),to=text(v.VALID_TO).slice(0,10);if(kind==='EXPIRED')return !!to&&to<now;if(kind==='FUTURE')return !!from&&from>now;if(kind==='SUSPENDED')return upper(v.ORIGINATION_STATUS_CODE)!=='OPEN'||!openingEligibleVersion(v);return !openingEligibleVersion(v)}
async function activeVersionRows(table,desc,versionId){const versionCol=col(desc,['PRODUCT_VERSION_ID','DEPOSIT_PRODUCT_VERSION_ID']);assert(versionCol,`${table}: PRODUCT_VERSION_ID column missing`);return (await rows(table,versionCol,versionId)).filter(activeRow)}

try{
 const health=await req('/actuator/health');assert(health?.status==='UP','Application health is not UP');
 const tables=['PRODUCT','PRODUCT_VERSION','PRODUCT_CHANNEL_RULE','PRODUCT_ELIGIBILITY_RULE','DEPOSIT_PRODUCT_OPENING_RULE','DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA','DEPOSIT_PRODUCT_TERM_RULE','DEPOSIT_PRODUCT_ALLOWED_TERM','PRODUCT_PRICING_RULE','PRODUCT_PRICING_COMPONENT','PRODUCT_RATE_TIER','DEPOSIT_PROFIT_PAYMENT_RULE'];
 const d={};for(const t of tables)d[t]=await descriptor(t);
 let pass=0;
 for(const spec of SPECS){
   const products=await rows('PRODUCT','PRODUCT_CODE',spec.code);assert(products.length===1,`${spec.code}: expected one PRODUCT, found ${products.length}`);const product=products[0];assert(upper(product.PRODUCT_CLASS_CODE)==='DEPOSIT',`${spec.code}: PRODUCT_CLASS_CODE is not DEPOSIT`);assert(upper(product.PRODUCT_FAMILY_CODE)===spec.family,`${spec.code}: family mismatch`);const pid=Number(product.PRODUCT_ID);assert(pid>0,`${spec.code}: PRODUCT_ID missing`);
   const versions=await rows('PRODUCT_VERSION','PRODUCT_ID',pid),openable=versions.filter(openingEligibleVersion);assert(openable.length===1,`${spec.code}: expected exactly one opening-eligible version, found ${openable.length}`);const version=openable[0],vid=Number(version.PRODUCT_VERSION_ID);assert(vid>0,`${spec.code}: PRODUCT_VERSION_ID missing`);
   for(const neg of NEGATIVE_EXPECTATIONS[spec.code]||[]){const v=versions.find(x=>Number(x.VERSION_NO)===neg.versionNo);assert(v,`${spec.code}: negative version ${neg.versionNo} missing`);assert(negativeMatches(v,neg.kind),`${spec.code}: version ${neg.versionNo} is not ${neg.kind}`);assert(!openingEligibleVersion(v),`${spec.code}: negative version ${neg.versionNo} is incorrectly openable`)}
   const channels=await activeVersionRows('PRODUCT_CHANNEL_RULE',d.PRODUCT_CHANNEL_RULE,vid);assert(channels.length>=spec.channels,`${spec.code}: channel policy ${channels.length}/${spec.channels}`);
   const eligibility=await activeVersionRows('PRODUCT_ELIGIBILITY_RULE',d.PRODUCT_ELIGIBILITY_RULE,vid);assert(eligibility.length>=1,`${spec.code}: eligibility policy missing`);
   const opening=await activeVersionRows('DEPOSIT_PRODUCT_OPENING_RULE',d.DEPOSIT_PRODUCT_OPENING_RULE,vid);assert(opening.length>=1,`${spec.code}: opening rule missing`);
   const withdrawal=await activeVersionRows('DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA',d.DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA,vid);assert(withdrawal.length>=spec.media,`${spec.code}: withdrawal media ${withdrawal.length}/${spec.media}`);
   if(spec.terms>0){
     const termRules=await activeVersionRows('DEPOSIT_PRODUCT_TERM_RULE',d.DEPOSIT_PRODUCT_TERM_RULE,vid);assert(termRules.length>=1,`${spec.code}: term rule missing`);const termRuleId=Number(termRules[0].TERM_RULE_ID??termRules[0][d.DEPOSIT_PRODUCT_TERM_RULE.primaryKeyColumn]);assert(termRuleId>0,`${spec.code}: TERM_RULE_ID missing`);const allowed=(await rows('DEPOSIT_PRODUCT_ALLOWED_TERM','TERM_RULE_ID',termRuleId)).filter(activeRow);assert(allowed.length>=spec.terms,`${spec.code}: allowed terms ${allowed.length}/${spec.terms}`);
     const pricing=await activeVersionRows('PRODUCT_PRICING_RULE',d.PRODUCT_PRICING_RULE,vid);assert(pricing.length>=1,`${spec.code}: pricing rule missing`);const payment=await activeVersionRows('DEPOSIT_PROFIT_PAYMENT_RULE',d.DEPOSIT_PROFIT_PAYMENT_RULE,vid);assert(payment.length>=1,`${spec.code}: profit payment rule missing`);
   }
   console.log(`UAT_PRODUCT_RUNTIME_OK code=${spec.code} productId=${pid} versionId=${vid} channels=${channels.length} eligibility=${eligibility.length} media=${withdrawal.length} terms=${spec.terms}`);pass++;
 }
 assert(pass===12,`Expected 12 verified products, got ${pass}`);
 console.log('------------------------------------------------------------');
 console.log(`DPS2_UAT_PRODUCT_PACK_V1_RUNTIME_PRODUCTS=${pass}`);
 console.log('DPS2_UAT_PRODUCT_PACK_V1_RUNTIME_PASS');
}catch(e){console.error('DPS2_UAT_PRODUCT_PACK_V1_RUNTIME_FAIL');console.error(e?.stack||e);process.exit(1)}
