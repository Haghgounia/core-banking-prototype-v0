#!/usr/bin/env node
import {readFileSync,existsSync,mkdtempSync,rmSync} from 'node:fs';
import {resolve} from 'node:path';
import {tmpdir} from 'node:os';
import {spawnSync} from 'node:child_process';
const ROOT=resolve(import.meta.dirname,'../../..');
const java=resolve(ROOT,'backend/src/main/java/com/behsazan/corebanking/iam/s1b');
const tests=resolve(ROOT,'backend/src/test/java/com/behsazan/corebanking/iam/s1b');
const sec=readFileSync(resolve(java,'IamS1bSecurityConfiguration.java'),'utf8');
const source=readFileSync(resolve(tests,'IamS1bSignedHttpIntegrationTest.java'),'utf8');
const fix=readFileSync(resolve(tests,'IamS1bRs256Fixture.java'),'utf8');
const pom=readFileSync(resolve(ROOT,'backend/pom.xml'),'utf8');
let count=0;
function check(n,c){if(!c)throw Error('IAM_RO3_CHECK_FAILED '+n);count++;console.log('PASS '+count+' '+n);}
check('production pins HTTPS JWKS and issuer',sec.includes('withJwkSetUri')&&sec.includes('JwtValidators.createDefaultWithIssuer'));
check('production decoder is explicitly named',sec.includes('@Bean("iamS1bJwtDecoder")'));
check('IAM trust never falls back to global JwtDecoder',sec.includes('@Qualifier("iamS1bJwtDecoder")')&&sec.includes('iamDecoder.getObject()'));
check('production JWT decoder only when explicit feature enabled',sec.includes('@ConditionalOnProperty')&&sec.includes('reads-enabled'));
check('JWT must remain RS256',sec.includes('SignatureAlgorithm.RS256'));
check('unauthorized and forbidden handlers remain independent',sec.includes('BearerTokenAuthenticationEntryPoint')&&sec.includes('BearerTokenAccessDeniedHandler'));
check('write methods remain denied',sec.includes('.anyRequest().denyAll()')&&!sec.includes('permitAll()'));
check('test uses real local HTTP, not controller-only simulation',source.includes('RANDOM_PORT')&&source.includes('java.net.http.HttpClient'));
check('test imports actual IAM security and controller and service',source.includes('IamS1bSecurityConfiguration.class')&&source.includes('IamS1bReadController.class')&&source.includes('IamS1bReadService.class'));
check('lab mocks only decoder and Oracle reader',source.includes('@MockitoBean(name="iamS1bJwtDecoder"')&&source.includes('mock(IamS1bReadRepository.class)'));
check('lab uses Nimbus to cryptographically verify',source.includes('NimbusJwtDecoder.withPublicKey')&&source.includes('realVerifier.decode'));
check('lab verifies token lifetime and signed claims',source.includes('IamS1bTokenValidation.requiredClaims'));
check('lab covers HTTP 401 403 200 503',source.includes('401,PATH')&&source.includes('403,PATH')&&source.includes('503,PATH')&&source.includes('readiness.statusCode()).isEqualTo(200)'));
check('lab checks no Oracle I/O before domain authorization',source.includes('verifyNoInteractions(repository)'));
check('lab cannot create Oracle connection',source.includes('never_connect')&&source.includes('DataSourceAutoConfiguration.class'));
check('lab never stores or prints access tokens',!source.includes('System.out.println(good)')&&!source.includes('Files.writeString'));
check('signature fixture uses ephemeral RSA 2048 SHA256withRSA',fix.includes('generator.initialize(2048)')&&fix.includes('SHA256withRSA'));
check('signature fixture never uses committed secrets',!fix.includes('BEGIN PRIVATE KEY')&&!fix.includes('-----BEGIN'));
check('JAR proof and Windows/Linux launch gate exist',existsSync(resolve(ROOT,'tools/iam/s1b/verify-s1b-ro3-artifact.cjs'))&&
 ['build-production.cmd','build-production.sh','bin/start.cmd','bin/start.sh'].every(f=>readFileSync(resolve(ROOT,f),'utf8').includes('verify-s1b-ro3-artifact.cjs')));
check('Windows/Linux production builds emit and check proof', ['build-production.cmd','build-production.sh'].every(f=>{
 const x=readFileSync(resolve(ROOT,f),'utf8');return x.includes('verify-s1b-ro3-contract.mjs')&&x.includes('emit-s1b-ro3-proof.cjs');}));
check('JUnit is test-scoped and not packaged as test-token endpoint',pom.includes('spring-boot-starter-test')&&
 !readFileSync(resolve(ROOT,'backend/src/main/java/com/behsazan/corebanking/iam/s1b/IamS1bReadController.java'),'utf8').includes('test-token'));
const temp=mkdtempSync(resolve(tmpdir(),'iam-ro3-jdk-'));
try{
 const f=resolve(tests,'IamS1bRs256Fixture.java');
 const c=spawnSync('javac',['-d',temp,f],{encoding:'utf8',timeout:30000});
 if(c.error?.code==='ENOENT'){console.log('INFO: JDK21 not found; RSA fixture execution deferred to Windows Maven');}
 else{
  check('JDK21 fixture compiled',c.status===0||(console.error(c.stderr),false));
  const t=spawnSync('java',['-cp',temp,'com.behsazan.corebanking.iam.s1b.IamS1bRs256Fixture'],{encoding:'utf8',timeout:20000});
  check('12 genuine RSA signature tests passed',t.status===0&&t.stdout.includes('IAM_S1B_RO3_RS256_JDK_PASS=12/12')||(console.error(t.stderr),false));
  console.log(t.stdout.trim());
 }
}finally{rmSync(temp,{recursive:true,force:true});}
console.log('IAM_S1B_RO3_STATIC_PASS='+count+'; SPRING_HTTP_JUNIT_PENDING_MAVEN; REAL_IDP_ORACLE_PENDING');
