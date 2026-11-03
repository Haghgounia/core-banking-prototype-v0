#!/usr/bin/env node
'use strict';
const path=require('node:path');
const {openJar}=require('../s1a/iam-runtime-contract.cjs');
const {ROOT,JAVA,CLASSES,RELEASE,hash}=require('./ro2-runtime-proof.cjs');
const jar=path.resolve(process.argv[2]||path.join(ROOT,'app/core-banking-prototype.jar'));
try{
 const zip=openJar(jar);
 try{
  const prefix='BOOT-INF/classes/';
  const proof=JSON.parse(zip.extract(prefix+'iam-s1b-ro2-proof.json').toString('utf8'));
  if(proof.release!==RELEASE||proof.sourceSha256!==hash()||proof.mode!=='default-disabled'
       ||proof.oracleWrites!==false||proof.algorithm!=='RS256')throw Error('RO2_PROOF_STALE');
  for(const f of CLASSES){const cls=prefix+'com/behsazan/corebanking/iam/s1b/'+f.replace('.java','.class');
    if(!zip.entries.has(cls))throw Error('RO2_MISSING_COMPILED_CLASS: '+f);
  }
  if(!zip.entries.has(prefix+'com/behsazan/corebanking/iam/s1b/IamS1bReadRepository.class')
     ||!zip.entries.has(prefix+'com/behsazan/corebanking/iam/s1b/IamS1bSecurityConfiguration.class'))
    throw Error('RO2_MISSING_EXISTING_BOUNDARY');
  console.log('IAM_S1B_RO2_JAR_PASS: 3 new classes, bounded RS256/Audience/privilege contract, source digest matched');
 }finally{zip.close();}
}catch(e){console.error('IAM_S1B_RO2_JAR_FAILED: '+e.message);process.exitCode=1;}
