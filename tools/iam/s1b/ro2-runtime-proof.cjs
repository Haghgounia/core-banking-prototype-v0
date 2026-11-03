'use strict';
const crypto=require('node:crypto');
const fs=require('node:fs');
const path=require('node:path');
const ROOT=path.resolve(__dirname,'../../..');
const JAVA='backend/src/main/java/com/behsazan/corebanking/iam/s1b/';
const CLASSES=Object.freeze(['IamS1bTokenClaimPolicy.java','IamS1bOracleReadPolicy.java','IamS1bTokenValidation.java']);
const CONTRACT=Object.freeze(['tools/iam/s1b/verify-s1b-ro2-security.mjs','tools/iam/s1b/verify-s1b-ro2-live.cjs']);
const PROOF='backend/src/main/resources/iam-s1b-ro2-proof.json';
const RELEASE='IAM-R1.8-S1B-RO2';
const SOURCES=[...CLASSES.map(f=>JAVA+f), ...CONTRACT];
function hash(root=ROOT){const h=crypto.createHash('sha256');for(const rel of SOURCES){
 const bytes=fs.readFileSync(path.join(root,rel));h.update(rel+'\0'+bytes.length+'\0');h.update(bytes);h.update('\0');
}return h.digest('hex');}
function create(){return {release:RELEASE,sourceSha256:hash(),mode:'default-disabled',
  allowedMethod:'GET',algorithm:'RS256',requiresVerifiedIssuer:true,requiresApprovedOracleReader:true,oracleWrites:false};}
module.exports={ROOT,JAVA,CLASSES,CONTRACT,PROOF,RELEASE,hash,create};
