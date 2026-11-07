'use strict';
// Executed after CLT local read-only startup; bounded GET only. No simulated rows.
const fs=require('node:fs');
const path=require('node:path');
const root=path.resolve(process.argv[2]||path.resolve(__dirname,'../../..'));
const U='http://127.0.0.1:8091';
const P='/api/v1/collateral/reference/INSURANCE_COMPANY';
const say=s=>process.stdout.write(s+'\n');
const fail=(s)=>{throw new Error('CLT_R3C_'+s);};
async function get(page){
 const u=U+P+'?page='+page+'&size=50';
 const res=await fetch(u,{method:'GET',redirect:'manual',headers:{'Accept':'application/json'},signal:AbortSignal.timeout(9000)});
 if(res.status!==200)fail('REFERENCE_GET_FAILED; HTTP='+res.status+'; PAGE='+page);
 let val;try{val=await res.json();}catch{fail('REFERENCE_INVALID_JSON');}
 if(!val||!Array.isArray(val.items)||!Number.isSafeInteger(val.totalElements)||val.totalElements<0||val.page!==page||val.size!==50)fail('REFERENCE_SCHEMA_INVALID');
 return val;
}
async function run(){
 const rows=[]; let expected=null;
 for(let page=0;page<100;page++){
   const d=await get(page);
   if(expected===null){expected=d.totalElements;if(expected>5000)fail('REFERENCE_TOO_LARGE_FOR_UAT');}
   if(d.totalElements!==expected)fail('REFERENCE_CHANGED_DURING_READ');
   rows.push(...d.items);
   if(rows.length>=expected)break;
   if(!d.items.length)fail('REFERENCE_EMPTY_INTERMEDIATE_PAGE');
 }
 if(rows.length!==expected)fail('REFERENCE_PAGE_COUNT_MISMATCH; ACTUAL='+rows.length+'; EXPECTED='+expected);
 const ids=new Set(), codes=new Set();let active=0,inactive=0,other=0,temp=0;
 for(const r of rows){
   const id=String(r.INSURANCE_COMPANY_ID??'');
   const code=String(r.INSURANCE_COMPANY_CODE??'');
   const name=String(r.INSURANCE_COMPANY_NAME??'');
   const status=r.STATUS;
   if(!/^[0-9]+$/.test(id)||!name.trim()||!code.trim()||ids.has(id)||codes.has(code))fail('REFERENCE_ROW_INVALID_OR_DUPLICATED');
   ids.add(id);codes.add(code);
   if(status==='ACTIVE')active++;else if(status==='INACTIVE')inactive++;else other++;
   if(/^TMP-INS-1403-[0-9]{3}$/.test(code))temp++;
 }
 const status=rows.length===42 && active===0 && inactive===42 && temp===42?'CANDIDATE_42_VISIBLE'
   : rows.length===0?'DATA_NOT_COMMITTED_OR_NOT_PRESENT':'ACTUAL_ORACLE_DATA';
 const result={checkedAt:new Date().toISOString(),total:rows.length,active,inactive,other,temporaryCodes:temp,
  source:'ORACLE_CLT_READ_ONLY',dataGate:status,oracleDml:false,analystUat:'NOT_RUN'};
 try{const logs=path.join(root,'logs');fs.mkdirSync(logs,{recursive:true});fs.writeFileSync(path.join(logs,'clt-r3c-insurer-result.json'),JSON.stringify(result,null,2)+'\n');}catch{}
 say('CLT_R3C_ORACLE_CATALOG_PASS; TOTAL='+rows.length+'; ACTIVE='+active+'; INACTIVE='+inactive+'; TMP='+temp+'; DATA_GATE='+status);
 say('CLT_R3C_UAT_URL='+U+'/#/collateral/reference-data/INSURANCE_COMPANY');
 say('CLT_R3C_ANALYST_HANDOFF_READY; UAT=NOT_RUN; DB_WRITES=NONE');
 return result;
}
if(require.main===module)run().catch(e=>{console.error(String(e?.message||e));process.exitCode=1;});
module.exports={run};
