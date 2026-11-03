#!/usr/bin/env node
'use strict';
const path=require('node:path');
const {openJar}=require('../s1a/iam-runtime-contract.cjs');
const {ROOT,REL,RELEASE,hash}=require('./ro3-runtime-proof.cjs');
const jar=path.resolve(process.argv[2]||path.join(ROOT,'app/core-banking-prototype.jar'));
try{
 const zip=openJar(jar);
 try{
  const prefix='BOOT-INF/classes/';
  const proof=JSON.parse(zip.extract(prefix+'iam-s1b-ro3-proof.json').toString('utf8'));
  if(proof.release!==RELEASE||proof.sourceSha256!==hash()||proof.mode!=='default-disabled'
     ||proof.requiresLiveIdpAcceptance!==true||proof.requiresLiveOracleAcceptance!==true||proof.oracleWrites!==false)
   throw Error('IAM_RO3_BUILD_PROOF_STALE');
  for(const cls of ['IamS1bSecurityConfiguration','IamS1bReadController','IamS1bReadService','IamS1bTokenValidation']){
   if(!zip.entries.has(prefix+'com/behsazan/corebanking/iam/s1b/'+cls+'.class'))
    throw Error('IAM_RO3_JAR_CLASS_MISSING: '+cls);
  }
  console.log('IAM_S1B_RO3_JAR_PASS: signed HTTP lab + source digest; no live IdP/Oracle claim');
 }finally{zip.close();}
}catch(e){console.error('IAM_S1B_RO3_JAR_FAILED: '+e.message);process.exitCode=1;}
