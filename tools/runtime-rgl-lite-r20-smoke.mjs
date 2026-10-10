/** R20 Lite runtime check - READ ONLY, never inserts/updates Oracle data. */
const origin=(process.env.CORE_BANKING_BASE_URL || 'http://127.0.0.1:8091').replace(/\/$/,'');
const endpoint='/api/v1/product-builder';
let checks=0;
async function read(path){
 const res=await fetch(origin+endpoint+path,{headers:{'Accept':'application/json'}});
 if(!res.ok)throw new Error('HTTP '+res.status+' '+path+': '+(await res.text()).slice(0,300));
 return res.json();
}
try{
 const catalog=await read('/catalog');
 if(!Array.isArray(catalog.packages)) throw new Error('Malformed product-builder catalog');checks++;
 const policies=await read('/rule-policies');
 if(!Array.isArray(policies)) throw new Error('Policy API not an array');checks++;
 for(const policy of policies){
  if(!Number.isSafeInteger(policy.id)||!['DRAFT','APPROVED'].includes(policy.state))throw new Error('Malformed policy '+policy.id);
  const details=await read('/rule-policies/'+policy.id+'/controls');
  if(!Array.isArray(details) || details.length!==policy.controlCount)throw new Error('Details mismatch for '+policy.id);
  const unique=new Set(details.map(c=>c.code));
  if(unique.size!==details.length)throw new Error('Duplicate control for policy '+policy.id);
  checks+=2;
 }
 console.log('RGL_R20_RUNTIME_READONLY_PASS checks='+checks+' policies='+policies.length);
 if(!policies.length)console.log('INFO: no policies created yet; mutation/approval not tested by this read-only check.');
}catch(e){
 console.error('RGL_R20_RUNTIME_READONLY_FAIL '+e.message);
 process.exitCode=1;
}
