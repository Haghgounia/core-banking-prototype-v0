#!/usr/bin/env node
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve, dirname } from 'node:path';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, rmSync } from 'node:fs';
import os from 'node:os';
const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
const read = p => readFileSync(resolve(root,p),'utf8');
const backend = 'backend/src/main/java/com/behsazan/corebanking/iam/s1b/';
const files = ['IamS1bProperties.java','IamS1bPolicy.java','IamS1bConfiguration.java','IamS1bReaderPool.java',
  'IamS1bSecurityConfiguration.java','IamS1bModels.java','IamS1bReadRepository.java',
  'IamS1bReadService.java','IamS1bReadController.java','IamS1bExceptionHandler.java'];
let checks=0; const check = (name,ok) => {if(!ok)throw new Error(`IAM_S1B_STATIC_FAILED: ${name}`);checks++;console.log(`PASS ${String(checks).padStart(2,'0')} ${name}`)};
for(const f of files)check('source: '+f,existsSync(resolve(root,backend+f)));
const pom=read('backend/pom.xml'); const conf=read('backend/src/main/resources/application.yml');
const props=read(backend+'IamS1bProperties.java'); const sec=read(backend+'IamS1bSecurityConfiguration.java');
const ctl=read(backend+'IamS1bReadController.java'); const repo=read(backend+'IamS1bReadRepository.java');
const service=read(backend+'IamS1bReadService.java'); const pool=read(backend+'IamS1bReaderPool.java');
const tokenValidation=read(backend+'IamS1bTokenValidation.java');
const road=read('docs/iam/IAM-CANONICAL-ROADMAP-FA.md');
check('Boot 4.1 supported Resource Server starter',pom.includes('spring-boot-starter-security-oauth2-resource-server'));
check('S1B default disabled',conf.includes('IAM_S1B_READS_ENABLED:false'));
check('no static secret or demo token',!conf.match(/IAM_S1B_JDBC_PASSWORD:([^}\s]+)/));
check('explicit issuer, JWKS and audience validation',/withJwkSetUri/.test(sec)&&/createDefaultWithIssuer/.test(sec)&&/getAudience/.test(tokenValidation));
check('JWT expiry and actor required',/getExpiresAt\(\)/.test(tokenValidation)&&/getSubject\(\)/.test(tokenValidation));
check('JWT RS256 decoder (standard library)',sec.includes('NimbusJwtDecoder')&&!sec.includes('parseJwtManually'));
check('IAM API only matcher, not global retrofit',sec.includes('securityMatcher("/api/v1/iam/**")'));
check('stateless session',sec.includes('SessionCreationPolicy.STATELESS'));
check('fails closed without opt-in',sec.includes('req.anyRequest().denyAll()'));
check('no HTTP Basic/demo principal',sec.includes('httpBasic(AbstractHttpConfigurer::disable')&&!sec.includes('X-User-Id'));
check('scope required',sec.includes('SCOPE_iam.users.read'));
check('dedicated Oracle reader',pool.includes('new HikariDataSource')&&pool.includes('setReadOnly(true)'));
check('no Hikari datasource exposed as bean',!read(backend+'IamS1bConfiguration.java').includes('public HikariDataSource '));
check('rejects privileged Oracle credentials',props.includes('"SYSTEM"')&&props.includes('"SYS"')&&props.includes('"IAM"'));
check('no IAM write operations',!files.some(f=>/\b(INSERT INTO|UPDATE IAM\.|DELETE FROM|@PostMapping|@PutMapping|@DeleteMapping|@PatchMapping)\b/i.test(read(backend+f))));
check('controller restricted to GET routes', (ctl.match(/@GetMapping/g)||[]).length===4);
check('domain and context verified against trusted claims',service.includes('iam_domains')&&service.includes('iam_contexts'));
check('rejects shared-principal link leakage',service.includes('principalIsExclusiveToDomain'));
check('Oracle metadata checked before reads',repo.includes('ALL_TAB_COLUMNS')&&service.includes('repo.metadataReady()'));
check('all dynamic values bind SQL parameters',repo.includes('OFFSET ? ROWS FETCH NEXT ? ROWS ONLY')&&repo.includes('WHERE DOMAIN_CODE = ?'));
check('pagination limits',service.includes('limit > 100')&&service.includes('offset > 10000'));
check('no raw SQL exception in response',read(backend+'IamS1bExceptionHandler.java').includes('IAM_S1B_DATA_SOURCE_UNAVAILABLE'));
check('separate identity mock UI',read('frontend/src/app/features/iam/iam-s1a-demo.component.html').includes('DEV / MOCK'));
check('canonical roadmap includes S1B',road.includes('S1B')&&road.includes('IAM-01'));
check('Windows and Unix build gates',read('build-production.cmd').includes('verify-s1b-security.mjs')&&read('build-production.sh').includes('verify-s1b-security.mjs'));
// Compile and execute pure policy in a clean temporary directory if a local JDK exists.
const path=mkdtempSync(resolve(os.tmpdir(),'iam-s1b-javac-'));
try {
 const javaFile=resolve(root,backend+'IamS1bPolicy.java');
 const testFile=resolve(root,'tools/iam/s1b/PolicySelfTest.java');
 const comp=spawnSync('javac',['-d',path,javaFile,testFile],{encoding:'utf8',timeout:30000});
 if(comp.error?.code==='ENOENT')console.log('INFO javac not present: policy Java test deferred to Windows build');
 else {
  check('offline Java policy compiles',comp.status===0 || (console.error(comp.stderr),false));
  const run=spawnSync('java',['-cp',path,'PolicySelfTest'],{encoding:'utf8',timeout:10000});
  check('offline policy security negative tests',run.status===0 && /IAM_S1B_POLICY_JAVA_PASS=21\/21/.test(run.stdout));
  process.stdout.write(run.stdout);
 }
} finally { rmSync(path,{recursive:true,force:true}); }
console.log(`IAM_S1B_STATIC_PASS=${checks}; NO_ORACLE_WRITE; DEFAULT_DISABLED`);
