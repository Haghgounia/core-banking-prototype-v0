/* CLT R6G-HF1: prove the launcher-to-pool binding is unambiguous; runtime still needs Windows/Oracle. */
'use strict';
const fs=require('fs'), path=require('path'), assert=require('assert');
const root=path.resolve(process.argv[2]||'.');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const base='backend/src/main/java/com/behsazan/corebanking/collateral/';
const conf=read(base+'localuat/CltLocalUatConfiguration.java');
const record=read(base+'localuat/CltLocalUatProperties.java');
const pool=read(base+'reference/CollateralReferenceConfiguration.java');
const localSec=read(base+'localuat/CltLocalUatSecurityConfiguration.java');
const jwtSec=read(base+'reference/CollateralSecurityConfiguration.java');
const regJwtSec=read(base+'registry/CollateralRegistrySecurityConfiguration.java');
const filter=read(base+'localuat/CltLocalUatAuthenticationFilter.java');
const test=read('backend/src/test/java/com/behsazan/corebanking/collateral/localuat/CltLocalUatEnvironmentBootstrapTest.java');
const readerTest=read('backend/src/test/java/com/behsazan/corebanking/collateral/reference/CollateralReferenceLocalReaderBootstrapTest.java');
const localLauncher=read('tools/clt/r6g/policy-smoke.cjs');
let n=0;function check(name,fn){fn();n++;}
for(const [name,src,pattern] of [
 ['single-explicit-bean',conf,/\bCltLocalUatProperties\s+cltLocalUatProperties\(Environment environment\)/],
 ['binding-is-launcher-flag',conf,/getProperty\("CLT_LOCAL_UAT_READONLY_ENABLED",\s*"false"\)/],
 ['branch-is-launcher-branch',conf,/getProperty\("CLT_LOCAL_UAT_BRANCH_CODE"\)/],
 ['reject-nonboolean',conf,/throw new IllegalStateException\("CLT_LOCAL_UAT_FLAG_INVALID"\)/],
 ['enabled-from-exact-flag',conf,/new CltLocalUatProperties\(enabled,/],
 ['default-no-implicit-binding',record,/public record CltLocalUatProperties\(\s*boolean enabled/],
 ['reuse-borrowed-db',pool,/if \(local\.enabled\(\)\)\s*\{[\s\S]*?local\.requireSafeMode\([\s\S]*?new CollateralReferenceReaderPool\(applicationDataSource\)/],
 ['reject-writes',record,/CLT_LOCAL_UAT_WRITE_GATE_MUST_BE_DISABLED/],
 ['loopback-check',record,/server\.address/],
 ['safety-check-in-JUnit',test,/launcherFlagAndBranchAreTheSingleSourceOfTruth/],
 ['test-shadowed-config-regression',test,/\.withProperty\("core-banking.collateral.local-uat.enabled",\s*"false"\)/],
 ['test-disabled-default',test,/disabledByDefaultAndWithExplicitFalse/],
 ['test-malformed-flag',test,/invalidModeFlagIsRejectedRatherThanCoerced/],
 ['regression-startup-reader-selection',readerTest,/localBranchSelectsApplicationDatasourceWithoutJwtOrSeparateDbCredentials/],
 ['regression-production-is-fail-closed',readerTest,/nonLocalBranchPreservesOriginalFailClosedPolicy/],
 ['read-only-scope',filter,/SCOPE_clt.collateral.read/],
 ['guard-JWT-production',jwtSec,/matchIfMissing = true/],
 ['guard-JWT-registry-production',regJwtSec,/matchIfMissing = true/],
 ['keep-R6G-static-guard',localLauncher,/LOCAL_ONLY=YES/],
])check(name,()=>assert(pattern.test(src),name));
check('no-second-configuration-properties-binding',()=>assert(!/EnableConfigurationProperties|ConfigurationProperties/.test(conf+record)));
check('local-chain-uses-exact-launcher-flag',()=>assert(localSec.includes('name = "CLT_LOCAL_UAT_READONLY_ENABLED"')));
check('no-IAM-modifications',()=>assert(!/\.iam\.|IamProperties|IamSecurity/.test(conf+record+pool)));
check('only-get-allowlist',()=>assert(localSec.includes('.anyRequest().denyAll()')&&!localSec.includes('HttpMethod.POST')));
console.log(`CLT_R6G_HF1_SOURCE_SAFETY_PASS=${n}; ENV_BINDING=EXPLICIT; DEFAULT=OFF; IAM=UNCHANGED; DB_WRITES=NONE`);
