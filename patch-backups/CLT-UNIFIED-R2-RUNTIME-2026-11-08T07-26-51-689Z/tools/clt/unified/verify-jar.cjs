'use strict';
const fs=require('fs');
const {execFileSync}=require('child_process');
const jarPath=process.argv[2];
if(!jarPath || !fs.existsSync(jarPath)){console.error('CLT_UNIFIED_JAR_MISSING');process.exit(1)}
const entries=new Set(execFileSync('jar',['tf',jarPath],{encoding:'utf8',maxBuffer:16*1024*1024}).split(/\r?\n/));
const checks=[
 ['api-controller', 'BOOT-INF/classes/com/behsazan/corebanking/collateral/reference/CltUnifiedReferenceController.class'],
 ['oracle-shared-writer', 'BOOT-INF/classes/com/behsazan/corebanking/collateral/reference/CltUnifiedReferenceRepository.class'],
 ['localhost-security','BOOT-INF/classes/com/behsazan/corebanking/collateral/prototype/CltUnifiedPrototypeSecurityConfiguration.class'],
 ['frontend-ui','BOOT-INF/classes/static/index.html']
];
for(const [name,entry] of checks){if(!entries.has(entry)){console.error('CLT_UNIFIED_JAR_PROOF_FAIL '+name+' '+entry);process.exit(1)};console.log('PASS | '+name)}
console.log('CLT_UNIFIED_OFFICIAL_JAR_PROOFS_PASS=4/4');
