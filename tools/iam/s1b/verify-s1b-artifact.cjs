#!/usr/bin/env node
'use strict';
const path=require('node:path');
const {openJar}=require('../s1a/iam-runtime-contract.cjs');
const {ROOT,JAVA,SOURCES,RELEASE,hash}=require('./s1b-backend-proof.cjs');
const jar=path.resolve(process.argv[2] || path.join(ROOT,'app/core-banking-prototype.jar'));
try {
  const zip=openJar(jar);
  try {
    const prefix='BOOT-INF/classes/';
    const proof=JSON.parse(zip.extract(prefix+'iam-s1b-source-proof.json').toString('utf8'));
    if(proof.release!==RELEASE||proof.sourceSha256!==hash()||proof.writesEnabled!==false||proof.defaultEnabled!==false)
      throw new Error('IAM_S1B_BUILD_PROOF_STALE');
    for(const file of SOURCES) {
      const cls=prefix+'com/behsazan/corebanking/iam/s1b/'+file.replace(/\.java$/,'.class');
      if(!zip.entries.has(cls))throw new Error('IAM_S1B_MISSING_CLASS: '+file);
    }
    const libs=Array.from(zip.entries.keys());
    for(const lib of ['spring-security-oauth2-resource-server-','spring-security-oauth2-jose-'])
      if(!libs.some(x=>x.startsWith('BOOT-INF/lib/'+lib)))throw new Error('IAM_S1B_SECURITY_LIBRARY_MISSING: '+lib);
    console.log('IAM_S1B_JAR_PASS: source checksum matched; 10 IAM classes and JWT verifier dependencies present; default disabled');
  } finally {zip.close();}
}catch(e){console.error('IAM_S1B_JAR_FAILED: '+e.message);process.exitCode=1;}
