/* R6H: cumulative local readonly safety and no-prompt branch contract. */
'use strict';
const fs=require('fs'),path=require('path'),assert=require('node:assert/strict');
const root=path.resolve(process.argv[2]||'.');
function load(p){return fs.readFileSync(path.join(root,p),'utf8');}
const base='backend/src/main/java/com/behsazan/corebanking/collateral/';
const local=load(base+'localuat/CltLocalUatConfiguration.java');
const props=load(base+'localuat/CltLocalUatProperties.java');
const resolver=load(base+'localuat/CltLocalUatBranchResolver.java');
const filter=load(base+'localuat/CltLocalUatAuthenticationFilter.java');
const chains=load(base+'localuat/CltLocalUatSecurityConfiguration.java');
const rconf=load(base+'reference/CollateralReferenceConfiguration.java');
const rpool=load(base+'reference/CollateralReferenceReaderPool.java');
const referenceJwt=load(base+'reference/CollateralSecurityConfiguration.java');
const registryJwt=load(base+'registry/CollateralRegistrySecurityConfiguration.java');
const run=load('tools/clt/r6h/RUN-CLT-LOCAL-UAT.ps1');
const test=load('backend/src/test/java/com/behsazan/corebanking/collateral/localuat/CltLocalUatBranchResolverTest.java');
const readerBootstrapTest=load('backend/src/test/java/com/behsazan/corebanking/collateral/reference/CollateralReferenceLocalReaderBootstrapTest.java');
let n=0;function check(name,cond){assert.ok(cond,name);n++;}
for(const [name,s,sub] of [
 ['marker',local,'CLT_R6H_AUTO_BRANCH_20261104'],
 ['single-mode-env',local,'getProperty("CLT_LOCAL_UAT_READONLY_ENABLED"'],
 ['backend-branch-auto',resolver,'SELECT MIN(BRANCH_CODE) FROM CLT.COLLATERAL'],
 ['no-cross-branch-on-empty',resolver,'CLT_NO_DATA'],
 ['cached',resolver,'cachedBranch'],
 ['read-only-query-template',resolver,'queryForObject(DISCOVERY_SQL, String.class)'],
 ['branch-failure-503',resolver,'HttpStatus.SERVICE_UNAVAILABLE'],
 ['no-request-branch',filter,'branchResolver.resolve()'],
 ['branch-internal-only',filter,'.claim("branch_code", serverBranch)'],
 ['strict-loopback',filter,'request.getRemoteAddr()'],
 ['strict-forwarded',filter,'X-Forwarded-For'],
 ['read-reference-scope',filter,'SCOPE_clt.reference.read'],
 ['read-registry-scope',filter,'SCOPE_clt.collateral.read'],
 ['deny-all-http',chains,'.anyRequest().denyAll()'],
 ['registry-scope',chains,'cltLocalRegistrySecurity'],
 ['jwt-production-still-rs256',referenceJwt,'SignatureAlgorithm.RS256'],
 ['jwt-production-disabled-in-local',registryJwt,'matchIfMissing = true'],
 ['borrowed-datasource',rconf,'new CollateralReferenceReaderPool(applicationDataSource)'],
 ['borrowed-not-closed',rpool,'if (pool != null) pool.close()'],
 ['boot-mode-mismatch-guard',rconf,'CLT_R6H_LOCAL_MODE_BINDING_MISMATCH'],
 ['bind-loopback-command',run,'--server.address=127.0.0.1'],
 ['bind-local-flag-jvm',run,'-DCLT_LOCAL_UAT_READONLY_ENABLED=true'],
 ['override-write-flags-false',run,'CLT_REFERENCE_WRITES_ENABLED'],
 ['branch-auto-enforced',run,'CLT_LOCAL_UAT_BRANCH_CODE'],
 ['unit-cache',test,'discoversBranchOnceAndCachesForSubsequentReadRequests'],
 ['unit-empty',test,'noDataYieldsSafeNonexistentBranchNotCrossBranchVisibility'],
 ['unit-oracle-fail',test,'unresolvedOracleReturnsServiceUnavailableNotUnscopedRead'],
 ['verify-jar',run,'CLT_R6H_AUTO_BRANCH_20261104'],
 ['rollback-on-error',run,'BUILD-DIRTY'],
]) check(name,s.includes(sub));
check('no-input-prompt', !/Read-Host|set\s+\/p\s+.*BRANCH|Branch code:/i.test(run));
check('no-write-HTTP-allowlist', !/requestMatchers\(HttpMethod\.(POST|PUT|PATCH|DELETE)/.test(chains));
check('no-get-wildcard-allow', !chains.includes('requestMatchers(HttpMethod.GET, "/api/v1/collaterals/**"'));
check('no-IAM-source', !/\.iam\.|IamSecurityConfiguration/.test(local+props+resolver+filter+chains));
check('no-client-header-branch', !/getHeader\("(X-Branch-Code|X-User-Id)"\)/.test(filter));
check('always-explicit-local-only', chains.includes('havingValue = "true"') && referenceJwt.includes('havingValue = "false"'));
check('production-default-closed', props.includes('CLT_LOCAL_UAT_NOT_ENABLED') && local.includes('"false"'));
check('skip-synthetic-permission-for-nonGET', filter.includes('!"GET".equals(request.getMethod())'));
check('branch-regex-fail-closed', props.includes('branchCode.matches("[A-Za-z0-9_-]{1,30}")'));
check('junit-fixture-launcher-flag', readerBootstrapTest.includes('.withProperty("CLT_LOCAL_UAT_READONLY_ENABLED", "true")'));
check('junit-negative-no-magic-enable', readerBootstrapTest.includes('missingLauncherFlagCannotEnableSyntheticLocalPrincipal'));
check('junit-negative-no-disabled-override', readerBootstrapTest.includes('enabledLauncherFlagCannotBeOverriddenByDisabledLocalObject'));
check('production-binding-still-fail-closed', rconf.includes('CLT_R6H_LOCAL_MODE_BINDING_MISMATCH'));
console.log('CLT_R6H_SOURCE_SAFETY_PASS='+n+'; BRANCH=AUTO_SERVER; GET_ONLY=YES; LOOPBACK=YES; IAM_CHANGED=NO');
