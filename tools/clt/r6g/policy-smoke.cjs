/* CLT-R6G source-level safety check. Java/JUnit and live Oracle need the real Windows build. */
'use strict';
const fs=require('fs'),path=require('path'),assert=require('assert');
const root=path.resolve(process.argv[2]||'.');let passed=0;
function read(rel){return fs.readFileSync(path.join(root,rel),'utf8')}
function test(name, fn){fn();passed++;}
const b='backend/src/main/java/com/behsazan/corebanking/collateral/';
const local=b+'localuat/';
const properties=read(local+'CltLocalUatProperties.java');
const config=read(local+'CltLocalUatSecurityConfiguration.java');
const filter=read(local+'CltLocalUatAuthenticationFilter.java');
const reader=read(b+'reference/CollateralReferenceReaderPool.java');
const referenceConfig=read(b+'reference/CollateralReferenceConfiguration.java');
const jwtReference=read(b+'reference/CollateralSecurityConfiguration.java');
const jwtRegistry=read(b+'registry/CollateralRegistrySecurityConfiguration.java');
const registry=read(b+'registry/CollateralRegistryController.java');
const reporting=read(b+'reporting/CollateralReportingController.java');
const runtime=read('backend/src/main/resources/clt/local-uat.properties');
const r6d=read('frontend/src/app/features/collateral/clt-r6d-readonly-diagnostics-policy.ts');
const r6e=read('frontend/src/app/features/collateral/clt-r6e-report-contract.ts');
for (const [name,expected,source] of [
 ['local-mode-disabled-default','CLT_LOCAL_UAT_READONLY_ENABLED:false',runtime],
 ['local-mode-flag','CLT_LOCAL_UAT_READONLY_ENABLED',runtime],
 ['server-fixed-branch','CLT_LOCAL_UAT_BRANCH_CODE',runtime],
 ['startup-loopback-bind','server.address',properties],
 ['startup-loopback-IP','127.0.0.1',properties],
 ['requires-two-read-flags','registry.readsEnabled()',properties],
 ['optional-branch-validated','branchCode.matches(',properties],
 ['reference-writes-prohibited','core-banking.collateral.reference.writes-enabled',properties],
 ['intro-writes-prohibited','core-banking.collateral.introduction.draft-writes-enabled',properties],
 ['sponsor-writes-prohibited','core-banking.collateral.sponsor.request-writes-enabled',properties],
 ['insurance-writes-prohibited','core-banking.collateral.insurance.request-writes-enabled',properties],
 ['valuation-writes-prohibited','core-banking.collateral.valuation.request-writes-enabled',properties],
 ['allocation-writes-prohibited','core-banking.collateral.allocation.intake.request-writes-enabled',properties],
 ['unallocation-writes-prohibited','core-banking.collateral.unallocation.intake.request-writes-enabled',properties],
 ['disable-original-reference-chain','havingValue = "false", matchIfMissing = true',jwtReference],
 ['disable-original-registry-chain','havingValue = "false", matchIfMissing = true',jwtRegistry],
 ['read-side-only-source-reference','new CollateralReferenceReaderPool(applicationDataSource)',referenceConfig],
 ['reference-shared-db-never-closed','if (pool != null) pool.close()',reader],
 ['dev-security-conditional','havingValue = "true"',config],
 ['dev-security-csrf-explicit','SessionCreationPolicy.STATELESS',config],
 ['dev-security-loopback-remote','request.getRemoteAddr()',filter],
 ['dev-security-loopback-local','request.getLocalAddr()',filter],
 ['dev-security-host','request.getServerName()',filter],
 ['proxy-prevention','X-Forwarded-For',filter],
 ['no-external-jwt-issuer','clt-server-internal-local-uat-only',filter],
 ['server-side-resolved-branch','branchResolver.resolve()',filter],
 ['fixed-principal-marker','clt_local_uat',filter],
 ['read-scope','SCOPE_clt.collateral.read',filter],
 ['ref-read-scope','SCOPE_clt.reference.read',filter],
 ['report-metadata','LOCAL_UAT_FIXED_BRANCH',reporting],
 ['readiness-metadata','LOCAL_UAT_FIXED_BRANCH',registry],
 ['r6d-contract-metadata','LOCAL_UAT_FIXED_BRANCH',r6d],
 ['r6e-contract-metadata','LOCAL_UAT_FIXED_BRANCH',r6e],
 ['default-jwt-still-rs256','SignatureAlgorithm.RS256',jwtReference],
 ])test(name,()=>assert(source.includes(expected), name));
for (const [name,source] of [['reference-chain',config.slice(config.indexOf('cltLocalReferenceSecurity'),config.indexOf('cltLocalRegistrySecurity'))], ['registry-chain',config.slice(config.indexOf('cltLocalRegistrySecurity'))]]){
 test(name+'-only-get',()=>assert(!/\.requestMatchers\(HttpMethod\.(POST|PUT|PATCH|DELETE)/.test(source)));
 test(name+'-deny-all-default',()=>assert(source.includes('.anyRequest().denyAll()')));
 test(name+'-read-scope',()=>assert(source.includes('.hasAuthority("SCOPE_clt.')));
}
test('fixed-registry-allowlist-not-broad-catchall',()=>assert(!config.includes('.requestMatchers(HttpMethod.GET, "/api/v1/collaterals", "/api/v1/collaterals/**")')));
test('no-unbounded-dev-db-read-write',()=>assert(!reader.includes('jdbc.update(')&&!reader.includes('execute(')));
test('filter-does-not-consume-header-branch',()=>assert(!/getHeader\("(X-Branch-Code|X-User-Id)"\)/.test(filter)));
test('guard-no-iam-dependency',()=>assert(!/\.iam\.|IamSecurity|IamProperties/.test(properties+config+filter)));
test('frontend-r6d-fail-close',()=>assert(r6d.includes("body['registryWritesEnabled']===false")&&r6d.includes("body['allocationAuthorization']===false")));
test('frontend-r6e-fail-close',()=>assert(r6e.includes("value['allocationAuthorization']!==false")));
const disabledRoutes=['introduction-reviews/pending','introduction-reviews/mine', 'introduction-drafts/*','requests/mine','intake/journal','unallocation-requests/journal'];
for(const item of disabledRoutes)test('disabled-route-'+item,()=>assert(!config.includes('"/api/v1/collaterals/'+item+'"') && !config.includes('"/api/v1/collaterals/*/'+item+'"')));
console.log('CLT_R6G_STATIC_SAFETY_PASS='+passed+'; DEFAULT_DISABLED=YES; LOCAL_ONLY=YES; HTTP_GET_ONLY=YES; IAM_CHANGED=NO; ORACLE_WRITES=NONE');
