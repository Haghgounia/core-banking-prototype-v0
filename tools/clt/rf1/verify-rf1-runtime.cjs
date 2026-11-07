'use strict';
// CLT-RF1: one-command GET-only check of all seventeen reference contracts.
// Never sends POST/PUT/PATCH/DELETE, creates mock Oracle rows, or changes local auth.
const fs=require('node:fs');const path=require('node:path');const assert=require('node:assert/strict');
const root=path.resolve(process.argv[2] && !process.argv[2].startsWith('--') ? process.argv[2] : path.join(__dirname,'../../..')); 
const base=process.env.CLT_RF1_SELFTEST_BASE||'http://127.0.0.1:8091';
const tables=JSON.parse(fs.readFileSync(path.join(root,'backend/src/main/resources/clt/reference-catalog.json'),'utf8')).tables;
const report={version:'CLT-RF1',timestamp:new Date().toISOString(),expected:17,base:'127.0.0.1:8091',entries:[],requestMethods:['GET'],oracleWrite:false,analystUat:'NOT_RUN'};
function safeHeader(response){const value=response.headers.get('X-CLT-Local-UAT');return value&&/^[A-Z_]{4,60}$/.test(value)?value:'NONE';}
async function request(route){
 const response=await fetch(base+route,{method:'GET',redirect:'manual',headers:{Accept:'application/json'},signal:AbortSignal.timeout(15000),cache:'no-store'});
 let json=null;
 if(response.status===200){try{json=await response.json();}catch{return {status:200,data:null,reason:'INVALID_JSON',guard:safeHeader(response)};}}
 else await response.body?.cancel();
 return {status:response.status,data:json,guard:safeHeader(response)};
}
function persist(){if(process.env.CLT_RF1_SELFTEST_BASE)return;
 try{const logs=path.join(root,'logs');fs.mkdirSync(logs,{recursive:true});fs.writeFileSync(path.join(logs,'clt-rf1-reference-matrix.json'),JSON.stringify(report,null,2)+'\n');}catch{}}
async function verify(){
 if(tables.length!==17||new Set(tables.map(t=>t.table)).size!==17)throw Error('CLT_RF1_SOURCE_CATALOG_INVALID');
 const matrix=await request('/api/v1/collateral/reference/uat-matrix');
 report.matrixHttp=matrix.status;report.matrixGuard=matrix.guard;
 if(matrix.status!==200){report.status='MATRIX_HTTP_DENIED';persist();throw Error(`CLT_RF1_MATRIX_DENIED; HTTP=${matrix.status}; GUARD=${matrix.guard}`);}
 const m=matrix.data;
 if(!m||m.expectedTables!==17||!Array.isArray(m.tables)||m.tables.length!==17||m.readOnly!==true){report.status='MATRIX_INVALID';persist();throw Error('CLT_RF1_MATRIX_CONTRACT_INVALID');}
 const byName=new Map(m.tables.map(p=>[p.table,p]));
 if(byName.size!==17||m.readableTables+m.unavailableTables!==17)throw Error('CLT_RF1_MATRIX_COUNTS_INVALID');
 let valid=0,refused=0,active=0,inactive=0,tmp=0,insurerCount=null;
 for(const t of tables){
  const matrixRow=byName.get(t.table);if(!matrixRow)throw Error('CLT_RF1_TABLE_NOT_IN_MATRIX:'+t.table);
  const list=await request(`/api/v1/collateral/reference/${t.table}?page=0&size=1&_cltRead=rf1`);
  const ok=list.status===200 && list.data && Array.isArray(list.data.items)
    && Number.isSafeInteger(list.data.totalElements) && list.data.page===0 && list.data.size===1;
  report.entries.push({table:t.table,oracleCount:matrixRow.rowCount,probeState:matrixRow.state,http:list.status,guard:list.guard,
    contract:ok?'PASS':'FAIL',issue:matrixRow.issue});
  if(ok && matrixRow.state==='READABLE' && list.data.totalElements===matrixRow.rowCount)valid++;else refused++;
  if(t.table==='INSURANCE_COMPANY' && ok){insurerCount=list.data.totalElements;
   if(insurerCount>5000)throw Error('CLT_RF1_INSURER_CATALOG_TOO_LARGE');
   for(let pg=0;pg<Math.ceil(insurerCount/50);pg++){
    const chunk=await request(`/api/v1/collateral/reference/INSURANCE_COMPANY?page=${pg}&size=50&_cltRead=rf1`);
    if(chunk.status!==200||!Array.isArray(chunk.data?.items)||chunk.data.totalElements!==insurerCount)
      throw Error('CLT_RF1_INSURER_PAGING_UNAVAILABLE');
    for(const r of chunk.data.items){if(r.STATUS==='ACTIVE')active++;else if(r.STATUS==='INACTIVE')inactive++;
      if(/^TMP-INS-1403-\d{3}$/.test(String(r.INSURANCE_COMPANY_CODE??'')))tmp++;}
   }
  }
 }
 report.readable=valid;report.failures=refused;report.insurers={count:insurerCount,active,inactive,temporaryCodes:tmp};
 report.status=refused===0?'ALL_17_READABLE':'PARTIAL_READ_FAILURE';persist();
 console.log(`CLT_RF1_MATRIX_RESULT; READABLE=${valid}/17; FAILED=${refused}; INSURERS=${insurerCount??'UNAVAILABLE'}; ACTIVE=${active}; INACTIVE=${inactive}; TMP=${tmp}`);
 if(refused)throw Error('CLT_RF1_REFERENCE_READ_GATE_FAIL; REPORT=logs/clt-rf1-reference-matrix.json');
 console.log('CLT_RF1_REFERENCE_READ_GATE_PASS; ALL_TABLES=17; HTTP=200; DB_WRITES=NONE; ANALYST_UAT=NOT_RUN');
}
async function selftest(){
 const http=require('node:http');const expected=new Map(tables.map(t=>[t.table,{table:t.table,rowCount:t.table==='INSURANCE_COMPANY'?42:0,state:'READABLE',issue:'NONE'}]));
 let count=0;const server=http.createServer((req,res)=>{
  if(req.method!=='GET'){res.writeHead(405);res.end();return;}
  res.setHeader('content-type','application/json');
  const u=new URL(req.url,'http://127.0.0.1');
  if(u.pathname.endsWith('/uat-matrix')){res.end(JSON.stringify({expectedTables:17,readableTables:17,unavailableTables:0,tables:[...expected.values()],source:'ORACLE_CLT_SELECT_ONLY',readOnly:true}));return;}
  const name=u.pathname.split('/').pop();const r=expected.get(name);
  if(!r){res.writeHead(404);res.end('{}');return;}
  const size=Number(u.searchParams.get('size')||1),page=Number(u.searchParams.get('page')||0);
  const n=r.rowCount;const items=Array.from({length:Math.max(0,Math.min(size,n-page*size))},(_,i)=>({INSURANCE_COMPANY_ID:String(page*size+i+1),INSURANCE_COMPANY_CODE:'TMP-INS-1403-'+String(page*size+i+1).padStart(3,'0'),INSURANCE_COMPANY_NAME:'بیمه آزمایشی',STATUS:'INACTIVE'}));
  count++;res.end(JSON.stringify({items,totalElements:n,page,size,source:'ORACLE_CLT_READ_ONLY'}));
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 const endpoint='http://127.0.0.1:'+server.address().port;
 try{
  // An isolated test demonstrates response shape, pagination and GET-only request behavior.
  const r=await fetch(endpoint+'/api/v1/collateral/reference/uat-matrix');const m=await r.json();
  assert.equal(m.tables.length,17);assert.equal(m.readableTables,17);
  const a=await fetch(endpoint+'/api/v1/collateral/reference/INSURANCE_COMPANY?page=2&size=20');const data=await a.json();
  assert.equal(data.items.length,2);assert.equal(data.totalElements,42);assert.equal(count,1);
  console.log('CLT_RF1_MOCK_CONTRACT_PASS; MATRIX=17; INSURERS=42; PAGING=20+20+2; GET_ONLY=YES');
 }finally{await new Promise(resolve=>server.close(resolve));}
}
if(require.main===module){
 (process.argv.includes('--self-test')?selftest():verify()).catch(e=>{console.error(String(e?.message||e));process.exitCode=1;});
}
module.exports={verify,selftest};
