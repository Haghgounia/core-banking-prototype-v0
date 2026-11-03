'use strict';
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const ROOT=path.resolve(__dirname,'../../..');
const REL='backend/src/main/java/com/behsazan/corebanking/iam/s1b/';
const FILES=Object.freeze([
 REL+'IamS1bSecurityConfiguration.java',
 REL+'IamS1bReadService.java',
 REL+'IamS1bReadController.java',
 REL+'IamS1bTokenValidation.java',
 'backend/src/test/java/com/behsazan/corebanking/iam/s1b/IamS1bRs256Fixture.java',
 'backend/src/test/java/com/behsazan/corebanking/iam/s1b/IamS1bSignedHttpIntegrationTest.java',
 'tools/iam/s1b/verify-s1b-ro3-contract.mjs',
 'backend/pom.xml'
]);
const PROOF='backend/src/main/resources/iam-s1b-ro3-proof.json';
const RELEASE='IAM-R1.9-S1B-RO3';
function hash(root=ROOT){const d=crypto.createHash('sha256');for(const rel of FILES){
 const buf=fs.readFileSync(path.join(root,rel));d.update(rel+'\0'+buf.length+'\0');d.update(buf);d.update('\0');
}return d.digest('hex');}
function create(){return {release:RELEASE,sourceSha256:hash(),mode:'default-disabled',
 labTest:'ephemeral-jdk-rsa-2048-and-spring-http',requiresLiveIdpAcceptance:true,
 requiresLiveOracleAcceptance:true,oracleWrites:false};}
module.exports={ROOT,REL,FILES,PROOF,RELEASE,hash,create};
