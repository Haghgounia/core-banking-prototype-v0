'use strict';
// Extra deny-by-default regressions: dangerous paths are not included in local unified security.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'../../..');
const read=x=>fs.readFileSync(path.join(root,x),'utf8');
const b='backend/src/main/java/com/behsazan/corebanking/collateral/';
const chain=read(b+'prototype/CltUnifiedPrototypeSecurityConfiguration.java');
const config=read('config/clt-unified-local.yml');
let ok=0;const check=(label,value)=>{assert.ok(value,label);console.log('PASS | '+label);ok++};
for(const route of ['/api/v1/collaterals','/api/v1/collaterals/*/final-release/execute',
  '/api/v1/collaterals/*/allocation-requests/intake','/api/v1/collaterals/*/allocations/*/release',
  '/api/v1/collaterals/introduction-reviews/*/decision']) {
 check('No new POST grant for '+route, !chain.includes(`HttpMethod.POST, "${route}"`));
}
check('Unified chain default deny still applies',chain.includes('.anyRequest().denyAll()'));
check('Local shared writer cannot listen on wildcard bind',chain.includes('CLT_UNIFIED_REQUIRES_LOOPBACK_BINDING'));
check('No external JDBC password added',!config.includes('jdbc-url')&&!config.includes('jdbc-password'));
check('Draft-only feature remains separately switchable',config.includes('CLT_DRAFT_WRITES_ENABLED'));
check('Review workflow remains off in local mode',config.includes('review-workflow-enabled: false'));
console.log('CLT_R2_R3_NEGATIVE_SECURITY_PASS='+ok+'/'+ok);
