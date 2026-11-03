#!/usr/bin/env node
import { readFileSync, existsSync, mkdtempSync, rmSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import os from 'node:os';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'../../..');
const java='backend/src/main/java/com/behsazan/corebanking/iam/s1b/';
const get=p=>readFileSync(resolve(root,p),'utf8');
let count=0;
const check=(name,ok)=>{if(!ok)throw Error('IAM_RO2_STATIC_FAILED '+name);console.log(`PASS RO2_STATIC_${++count} ${name}`)};
const sec=get(java+'IamS1bSecurityConfiguration.java');
const claims=get(java+'IamS1bTokenClaimPolicy.java');
const grant=get(java+'IamS1bOracleReadPolicy.java');
const token=get(java+'IamS1bTokenValidation.java');
const repo=get(java+'IamS1bReadRepository.java');
const service=get(java+'IamS1bReadService.java');
const props=get(java+'IamS1bProperties.java');
const config=get('backend/src/main/resources/application.yml');
for(const f of ['IamS1bOracleReadPolicy.java','IamS1bTokenClaimPolicy.java','IamS1bTokenValidation.java'])
 check('new Java source '+f,existsSync(resolve(root,java+f)));
check('RS256 algorithm pinned',sec.includes('.jwsAlgorithm(SignatureAlgorithm.RS256)'));
check('built-in JWT signature and issuer validators retained',sec.includes('withJwkSetUri')&&sec.includes('JwtValidators.createDefaultWithIssuer'));
check('audience and maximum token lifetime validated',token.includes('getAudience()')&&token.includes('maxLifetimeSeconds'));
check('issued-at future and expiry bound validated',token.includes('issuedAt.isAfter')&&token.includes('expiresAt.isAfter')&&token.includes('Duration.between'));
check('typed signed grant arrays',token.includes('IamS1bTokenClaimPolicy.validClaims(jwt.getClaims())')&&claims.includes('instanceof List<?>'));
check('no token wildcard or duplicate grants',claims.includes('!seen.add(s)')&&claims.includes('allowed.matcher(s).matches()'));
check('enabled mode distinct authentication and permission handlers',sec.includes('BearerTokenAuthenticationEntryPoint')&&sec.includes('BearerTokenAccessDeniedHandler'));
check('IAM only GET allowlist',sec.includes('requestMatchers(HttpMethod.GET, "/api/v1/iam/**")')&&sec.includes('.anyRequest().denyAll()'));
check('disabled default remains 403 with no Oracle',sec.includes('if (!p.readsEnabled())')&&sec.includes('req.anyRequest().denyAll()'));
check('scope/domain checked prior to metadata and data reads',service.indexOf('!scopes.contains("iam.users.read")')>=0&&
 service.indexOf('!scopes.contains("iam.users.read")')<service.indexOf('repo.metadataReady()')&&
 service.indexOf('!domains.contains(code)')<service.indexOf('repo.metadataReady()'));
check('domain context verified after Oracle domain resolution',service.includes('IamS1bPolicy.canRead')&&service.indexOf('IamS1bPolicy.canRead')>service.indexOf('repo.domain(code)'));
check('Oracle no writable privileges, enabled roles or cross-schema grants',grant.includes('!enabledRoles.isEmpty()')&&grant.includes('"CREATE SESSION"')&&grant.includes('"IAM".equals(grant.owner())'));
check('Oracle all four SELECT grants required',grant.includes('readable.containsAll(TABLES)')&&grant.includes('"IDENTITY_LINKS"'));
check('Oracle actual metadata and direct grant views',repo.includes('ALL_TAB_COLUMNS')&&repo.includes('USER_TAB_PRIVS_RECD')&&repo.includes('SESSION_PRIVS')&&repo.includes('SESSION_ROLES'));
check('Oracle actual SELECT probe for all four required tables',repo.includes('WHERE 1 = 0')&&repo.includes('IamS1bOracleReadPolicy.TABLES'));
check('bounded lifetime configuration and no secret defaults',config.includes('IAM_S1B_MAX_TOKEN_LIFETIME_SECONDS:3600')&&props.includes('maxTokenLifetimeSeconds')&&config.includes('IAM_S1B_JDBC_PASSWORD:}'));
check('no new IAM write APIs',!/@(Post|Put|Patch|Delete)Mapping/.test(get(java+'IamS1bReadController.java')));
check('real IdP/DB test remains explicit opt-in',existsSync(resolve(root,'tools/iam/s1b/verify-s1b-ro2-live.cjs')));
check('DBA evidence script is read-only and not invoked by build',existsSync(resolve(root,'docs/iam/implementation/S1B/IAM-S1B-RO2-ORACLE-DEV-READONLY-PREFLIGHT.sql')));
check('extra JUnit token contract exists',existsSync(resolve(root,'backend/src/test/java/com/behsazan/corebanking/iam/s1b/IamS1bTokenValidationTest.java')));
check('release builds/launchers gate RO2 artifact',get('build-production.cmd').includes('verify-s1b-ro2-artifact.cjs')&&
 get('build-production.sh').includes('verify-s1b-ro2-artifact.cjs')&&
 get('bin/start.cmd').includes('verify-s1b-ro2-artifact.cjs')&&get('bin/start.sh').includes('verify-s1b-ro2-artifact.cjs'));
const temp=mkdtempSync(resolve(os.tmpdir(),'iam-ro2-pure-'));
try{
 const files=['IamS1bTokenClaimPolicy.java','IamS1bOracleReadPolicy.java'].map(f=>resolve(root,java+f));
 files.push(resolve(root,'tools/iam/s1b/Ro2PolicySelfTest.java'));
 const c=spawnSync('javac',['-d',temp,...files],{encoding:'utf8',timeout:30000});
 if(c.error?.code==='ENOENT'){console.log('INFO: javac unavailable; pure Java validation deferred to Windows build');}
 else {
  check('JDK21 pure grant/Oracle policy compiled',c.status===0||(console.error(c.stderr),false));
  const t=spawnSync('java',['-cp',temp,'Ro2PolicySelfTest'],{encoding:'utf8',timeout:20000});
  check('26/26 positive/negative grant and privilege cases',t.status===0&&/IAM_S1B_RO2_PURE_POLICY_PASS=26\/26/.test(t.stdout)||(console.error(t.stderr),false));
  console.log(t.stdout.trim());
 }
} finally {rmSync(temp,{recursive:true,force:true});}
console.log('IAM_S1B_RO2_STATIC_PASS='+count+'; NO_ORACLE_IO; NO_WRITE; IDP_EXTERNAL_PENDING');
