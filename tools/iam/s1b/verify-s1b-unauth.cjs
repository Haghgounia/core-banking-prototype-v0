#!/usr/bin/env node
'use strict';
// Negative HTTP smoke test: do NOT include any privileged token or query sensitive data.
const http=require('node:http');
const base=process.argv[2]||'http://127.0.0.1:8091';
const address=new URL(base);
if(!['127.0.0.1','localhost','[::1]'].includes(address.hostname)||address.protocol!=='http:'){
  console.error('IAM_S1B_UNAUTH_FAILED: local loopback only');process.exit(1);
}
function probe(path){return new Promise((resolve,reject)=>{
  const req=http.get(new URL(path,address),{timeout:7000,headers:{'Accept':'application/problem+json'}},res=>{
    const status=res.statusCode;res.resume();res.on('end',()=>resolve(status));
  });req.on('timeout',()=>req.destroy(new Error('TIMEOUT')));req.on('error',reject);
});}
(async()=>{
  for(const p of ['/api/v1/iam/readiness?domainCode=DEMO_WF','/api/v1/iam/accounts?domainCode=DEMO_WF']){
    const status=await probe(p);
    if(![401,403,404].includes(status))throw new Error(`IAM_S1B_UNAUTH_HTTP_${status}: ${p}`);
    console.log('IAM_S1B_UNAUTH_DENIED HTTP='+status+' '+p);
  }
  console.log('IAM_S1B_UNAUTH_HTTP_PASS=2/2; NO_BEARER; NO_ORACLE_WRITE');
})().catch(e=>{console.error('IAM_S1B_UNAUTH_HTTP_FAILED: '+e.message);process.exitCode=1;});
