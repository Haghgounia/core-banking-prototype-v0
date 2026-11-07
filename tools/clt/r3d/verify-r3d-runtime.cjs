'use strict';
// R3D: GET-only data-gate smoke, not analyst UAT, and never a business acceptance proof.
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const root=path.resolve(process.argv[2]||path.join(__dirname,'../../..'));
const report={schema:'CLT-R3D-GET-ONLY-v1',at:new Date().toISOString(),oracleWrite:false,
 analystUat:'NOT_RUN',methods:['GET'],checks:{}};
async function getJson(route){
 const response=await fetch((process.env.CLT_R3D_SELFTEST_BASE || 'http://127.0.0.1:8091')+route,{method:'GET',headers:{Accept:'application/json'},
  cache:'no-store',redirect:'manual',signal:AbortSignal.timeout(12000)});
 if(response.status!==200){await response.body?.cancel();throw Error('CLT_R3D_REFERENCE_HTTP_'+response.status+' '+route);}
 try{return await response.json();}catch{throw Error('CLT_R3D_REFERENCE_JSON_INVALID '+route);}
}
function page(data,name){
 if(!data||!Array.isArray(data.items)||!Number.isSafeInteger(data.totalElements)||data.totalElements<0)
  throw Error('CLT_R3D_REFERENCE_CONTRACT_INVALID '+name);
 return data;
}
async function verify(){
 const [insurers,insuranceTypes,collateralTypes]=await Promise.all([
  getJson('/api/v1/collateral/reference/INSURANCE_COMPANY?page=0&size=50&_cltRead=r3d'),
  getJson('/api/v1/collateral/reference/COLLATERAL_INSURANCE_TYPE?page=0&size=50&_cltRead=r3d'),
  getJson('/api/v1/collaterals/types?_cltRead=r3d')
 ]);
 page(insurers,'INSURANCE_COMPANY');page(insuranceTypes,'COLLATERAL_INSURANCE_TYPE');
 if(!Array.isArray(collateralTypes))throw Error('CLT_R3D_COLLATERAL_TYPES_INVALID');
 const active=insurers.items.length===insurers.totalElements
  ? insurers.items.filter(r=>r.STATUS==='ACTIVE').length:null;
 const activeInsTypes=insuranceTypes.items.length===insuranceTypes.totalElements
  ? insuranceTypes.items.filter(r=>r.STATUS==='ACTIVE').length:null;
 const temporary=insurers.items.length===insurers.totalElements
  ? insurers.items.filter(r=>/^TMP-INS-1403-\d{3}$/.test(String(r.INSURANCE_COMPANY_CODE??''))).length:null;
 report.checks={insurerCatalog:{total:insurers.totalElements,active,temporary},
  insuranceTypeCatalog:{total:insuranceTypes.totalElements,active:activeInsTypes},
  activeCollateralTypeCount:collateralTypes.length};
 if(!process.env.CLT_R3D_SELFTEST_BASE){
  const dir=path.join(root,'logs');fs.mkdirSync(dir,{recursive:true});
  fs.writeFileSync(path.join(dir,'clt-r3d-insurance-uat-gates.json'),JSON.stringify(report,null,2)+'\n');
 }
 console.log(`CLT_R3D_DATA_GATE; INSURERS=${insurers.totalElements}; INSURERS_ACTIVE=${active===null?'UNPROVEN':active}; TMP=${temporary===null?'UNPROVEN':temporary}; INSURANCE_TYPES=${insuranceTypes.totalElements}; ACTIVE_COLLATERAL_TYPES=${collateralTypes.length}`);
 console.log('CLT_R3D_GET_ONLY_RUNTIME_PASS; API_READS=3; ORACLE_WRITES=NONE; ANALYST_UAT=NOT_RUN');
}
async function selftest(){
 const expected={insurerCount:42,companyRows:Array.from({length:42},(_,i)=>({INSURANCE_COMPANY_ID:String(i+1),STATUS:'INACTIVE',INSURANCE_COMPANY_CODE:`TMP-INS-1403-${String(i+1).padStart(3,'0')}`}))};
 const server=http.createServer((req,res)=>{
  if(req.method!=='GET'){res.writeHead(405);res.end();return;}
  res.setHeader('Content-Type','application/json');const url=new URL(req.url,'http://127.0.0.1');
  if(url.pathname.endsWith('INSURANCE_COMPANY')){res.end(JSON.stringify({items:expected.companyRows,totalElements:42,page:0,size:50}));return;}
  if(url.pathname.endsWith('COLLATERAL_INSURANCE_TYPE')){res.end(JSON.stringify({items:[],totalElements:0,page:0,size:50}));return;}
  if(url.pathname.endsWith('/types')){res.end('[]');return;}
  res.writeHead(403);res.end('{}');
 });
 await new Promise(ok=>server.listen(0,'127.0.0.1',ok));
 const {port}=server.address();process.env.CLT_R3D_SELFTEST_BASE=`http://127.0.0.1:${port}`;
 try {await verify();console.log('CLT_R3D_RUNTIME_SELFTEST_PASS; NO_DATA_FABRICATION=YES');}finally{server.close();}
}
if(require.main===module){(process.argv.includes('--self-test')?selftest():verify())
 .catch(e=>{console.error('CLT_R3D_GET_ONLY_RUNTIME_FAIL; '+String(e?.message||e));process.exitCode=1;});}
module.exports={verify};
