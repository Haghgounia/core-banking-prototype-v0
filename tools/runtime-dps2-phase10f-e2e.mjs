import fs from 'node:fs';
const base=(process.env.CORE_BANKING_BASE_URL||'http://localhost:8091').replace(/\/$/,'');
const fixturePath=process.argv[2];
if(!fixturePath){console.error('Usage: node tools/runtime-dps2-phase10f-e2e.mjs <fixture.json>');process.exit(2)}
const fixture=JSON.parse(fs.readFileSync(fixturePath,'utf8'));
const requiredFamilies=['QARD_SAVINGS','CURRENT_ACCOUNT','SHORT_TERM_DEPOSIT','LONG_TERM_DEPOSIT'];
const headers={'content-type':'application/json','X-User-Id':'phase10f.qa','X-Correlation-Id':`phase10f-${Date.now()}`};
const assert=(c,m)=>{if(!c)throw new Error(m)};
function assertNoPlaceholders(value,path='fixture'){
  if(typeof value==='string') assert(!/(TODO|PLACEHOLDER|REPLACE_ME|CHANGEME|DUMMY)/i.test(value),`${path} contains placeholder evidence`);
  else if(Array.isArray(value)) value.forEach((v,i)=>assertNoPlaceholders(v,`${path}[${i}]`));
  else if(value&&typeof value==='object') Object.entries(value).forEach(([k,v])=>assertNoPlaceholders(v,`${path}.${k}`));
}
async function request(method,url,body){
  const r=await fetch(base+url,{method,headers,body:body===undefined?undefined:JSON.stringify(body)});
  const text=await r.text();let data;try{data=text?JSON.parse(text):null}catch{data=text}
  if(!r.ok) throw new Error(`${method} ${url} -> ${r.status}: ${typeof data==='string'?data:JSON.stringify(data)}`);
  return data;
}
async function runCase(c){
  assert(requiredFamilies.includes(c.family),`Unsupported family ${c.family}`);
  assertNoPlaceholders(c);
  const aggregate=c.aggregate;
  assert(aggregate?.DEPOSIT_OPENING_REQUEST?.PRODUCT_VERSION_ID,'PRODUCT_VERSION_ID required');
  console.log(`\n[${c.family}] validate`);
  const validation=await request('POST','/api/v1/deposit-opening/requests/validate',aggregate);assert(validation.valid===true,'Runtime validation did not pass');
  console.log(`[${c.family}] persist`);
  const created=await request('POST','/api/v1/deposit-opening/requests',aggregate);const id=created.openingRequestId;assert(id,'openingRequestId missing');
  console.log(`[${c.family}] create account`);
  let account=await request('POST',`/api/v1/deposit-opening/requests/${id}/account`);
  assert(account.accountStatusCode==='PENDING_ACTIVATION','Expected PENDING_ACTIVATION');
  console.log(`[${c.family}] settlement`);
  await request('POST',`/api/v1/deposit-opening/requests/${id}/account/settlement`,c.settlement);
  console.log(`[${c.family}] readiness`);
  const ready=await request('POST',`/api/v1/deposit-opening/requests/${id}/account/readiness`,{EVIDENCE:c.readinessEvidence||[]});
  assert(ready.activationStatusCode==='READY',`Expected READY, got ${ready.activationStatusCode}`);
  console.log(`[${c.family}] activate`);
  account=await request('POST',`/api/v1/deposit-opening/requests/${id}/account/activate`);
  assert(account.accountStatusCode==='ACTIVE','Expected ACTIVE');
  if(c.closeExpectedRecordVersion){
    console.log(`[${c.family}] close`);
    const closed=await request('POST',`/api/v1/deposit-accounts/${account.accountId}/close`,{expectedRecordVersion:c.closeExpectedRecordVersion});
    assert(closed.accountStatusCode==='CLOSED','Expected CLOSED');
  }
  console.log(`[${c.family}] PASS request=${id} account=${account.accountId}`);
  return {family:c.family,openingRequestId:id,accountId:account.accountId,status:'PASS'};
}

try{
  const health=await request('GET','/actuator/health');assert(health.status==='UP','Application health is not UP');
  assert(Array.isArray(fixture.cases),'fixture.cases must be an array');
  const families=new Set(fixture.cases.map(x=>x.family));requiredFamilies.forEach(f=>assert(families.has(f),`Missing family fixture: ${f}`));
  const results=[];for(const c of fixture.cases)results.push(await runCase(c));
  console.log('\nPHASE10F_RUNTIME_E2E_PASS');console.table(results);
}catch(e){console.error('\nPHASE10F_RUNTIME_E2E_FAIL');console.error(e?.stack||e);process.exit(1)}
