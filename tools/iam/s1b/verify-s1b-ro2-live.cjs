#!/usr/bin/env node
'use strict';
/** Loopback-only, token-safe IdP/Oracle acceptance. No tokens are emitted to stdout or files. */
const http=require('node:http');
const crypto=require('node:crypto');
const base=new URL(process.argv[2]||'http://127.0.0.1:8091');
const mode=process.argv[3]||'disabled';
const allowedHosts=['127.0.0.1','localhost','[::1]'];
if(base.protocol!=='http:'||!allowedHosts.includes(base.hostname)||!['disabled','enabled','enabled-db-unavailable'].includes(mode)){
  console.error('IAM_RO2_LIVE_FAILED: loopback HTTP and recognized mode required');process.exit(1);
}
const valid=process.env.IAM_RO2_TEST_VALID_TOKEN||'';
const domain=process.env.IAM_RO2_TEST_DOMAIN||'';
const optional=[['IAM_RO2_TEST_WRONG_AUD_TOKEN',401],['IAM_RO2_TEST_WRONG_ISSUER_TOKEN',401],
                ['IAM_RO2_TEST_EXPIRED_TOKEN',401],['IAM_RO2_TEST_NO_SCOPE_TOKEN',403]];
if(mode!=='disabled' && (!valid||valid.length>16000||!/^[A-Z][A-Z0-9_]{1,49}$/.test(domain))){
  console.error('IAM_RO2_LIVE_FAILED: enabled mode needs test domain and an IdP-issued valid token in environment');process.exit(1);
}
function request(p,{method='GET',token='',headers={}}={}) {
  return new Promise((resolve,reject)=>{
    const url=new URL(p,base), opts={method,headers:{'Accept':'application/json',...headers}};
    if(token)opts.headers.Authorization='Bearer '+token;
    const req=http.request(url,opts,res=>{
      let size=0;
      res.on('data',chunk=>{size+=chunk.length;if(size>262144)req.destroy(Error('response too large'));});
      res.on('end',()=>resolve(res.statusCode));
      res.on('error',reject);
    });
    req.setTimeout(8000,()=>req.destroy(Error('request timeout')));
    req.on('error',reject);req.end();
  });
}
let checks=0;
async function expectStatus(name,p,expected,options){
 const status=await request(p,options);
 if(status!==expected)throw Error(name+': HTTP '+status+' (expected '+expected+')');
 console.log('IAM_RO2_HTTP_PASS '+name+' HTTP='+status);checks++;
}
function tamperJwt(jwt){
 const parts=jwt.split('.');
 if(parts.length!==3||parts[2].length<12)throw Error('valid token must be a signed compact JWT');
 // Deliberately change a *middle* signature character (not padding bits) so signature verification must fail.
 const p=parts[2],i=Math.floor(p.length/2),c=p[i]==='A'?'B':'A';
 parts[2]=p.slice(0,i)+c+p.slice(i+1);return parts.join('.');
}
(async()=>{
 const d='/api/v1/iam/readiness?domainCode=DEMO_WF';
 const a='/api/v1/iam/accounts?domainCode=DEMO_WF';
 if(mode==='disabled') {
  await expectStatus('disabled_readiness',d,403);
  await expectStatus('disabled_accounts',a,403);
  await expectStatus('forged_actor_header_denied',d,403,{headers:{'X-User-Id':'iam-admin','X-Iam-Role':'admin'}});
  await expectStatus('garbage_bearer_denied',d,403,{token:'not-a-real-jwt'});
  await expectStatus('no_write_while_disabled',a,403,{method:'POST'});
  console.log('IAM_S1B_RO2_DEFAULT_DISABLED_PASS='+checks+'/'+checks+'; NO_TOKEN; NO_ORACLE_IO');return;
 }
 const target='/api/v1/iam/readiness?domainCode='+encodeURIComponent(domain);
 await expectStatus('missing_bearer_401',target,401);
 await expectStatus('garbage_bearer_401',target,401,{token:'invalid.jwt.token'});
 await expectStatus('tampered_rs256_signature_401',target,401,{token:tamperJwt(valid)});
 await expectStatus('forged_header_no_bearer_401',target,401,{headers:{'X-User-Id':'admin','X-Iam-Role':'admin'}});
 const randomDenied='ZZ_RO2_DENIED_'+crypto.randomBytes(7).toString('hex').toUpperCase();
 await expectStatus('domain_not_granted_403','/api/v1/iam/readiness?domainCode='+randomDenied,403,{token:valid});
 await expectStatus('non_get_method_denied_403','/api/v1/iam/accounts?domainCode='+domain,403,{method:'POST',token:valid});
 for(const [name,expected] of optional){const token=process.env[name];
  if(token){if(token.length>16000)throw Error(name+' excessively long');
    await expectStatus(name.toLowerCase(),'/api/v1/iam/readiness?domainCode='+domain,expected,{token});
  } else console.log('IAM_RO2_OPTIONAL_CASE_PENDING '+name);
 }
 const expected=mode==='enabled'?200:503;
 await expectStatus('authorized_readiness',target,expected,{token:valid});
 if(expected===200)
  await expectStatus('authorized_accounts','/api/v1/iam/accounts?domainCode='+domain+'&limit=2',200,{token:valid});
 console.log('IAM_S1B_RO2_IDP_ORACLE_HTTP_PASS='+checks+'/'+checks+'; MODE='+mode+'; TOKEN_SUPPRESSED');
})().catch(error=>{console.error('IAM_S1B_RO2_HTTP_FAILED: '+error.message);process.exitCode=1;});
