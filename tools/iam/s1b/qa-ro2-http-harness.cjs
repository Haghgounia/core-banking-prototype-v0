#!/usr/bin/env node
'use strict';
// Local test *of the acceptance harness*, not a substitute for Spring+IdP/Oracle runtime proof.
const http=require('node:http'), cp=require('node:child_process'), path=require('node:path');
const live=path.join(__dirname,'verify-s1b-ro2-live.cjs');
const fakeToken='eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.BBBBBBBBBBBBBBBBBBBBBB';
const server=http.createServer((req,res)=>{
 const auth=req.headers.authorization||'';
 const u=new URL(req.url,'http://localhost');
 let code=403;
 if(u.pathname.startsWith('/api/v1/iam/')){
  if(process.env.MODE==='disabled')code=403;
  else if(auth!=='Bearer '+fakeToken)code=401;
  else if(req.method!=='GET'||u.searchParams.get('domainCode')!=='DEMO_WF')code=403;
  else code=process.env.MODE==='enabled'?200:503;
 }
 res.writeHead(code,{'content-type':'application/json'});res.end('{}');
});
server.listen(0,'127.0.0.1',async()=>{
 const port=server.address().port, base=`http://127.0.0.1:${port}`;
 for(const mode of ['disabled','enabled','enabled-db-unavailable']){
  process.env.MODE=mode;
  const env={...process.env,IAM_RO2_TEST_DOMAIN:'DEMO_WF',IAM_RO2_TEST_VALID_TOKEN:fakeToken};
  const exit=await new Promise((resolve,reject)=>{
    const child=cp.spawn(process.execPath,[live,base,mode],{env,stdio:['ignore','pipe','pipe']});
    let stdout='',stderr='';child.stdout.on('data',d=>stdout+=d);child.stderr.on('data',d=>stderr+=d);
    child.on('error',reject);child.on('close',code=>{if(code!==0)console.error(stdout,stderr);resolve(code);});
  });
  if(exit!==0){process.exitCode=1;break;}
  console.log('IAM_RO2_HARNESS_SELFTEST_PASS mode='+mode);
 }
 server.close();
});
