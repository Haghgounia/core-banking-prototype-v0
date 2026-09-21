import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

const here=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(here,'..');
const requireRuntime=process.argv.includes('--require-runtime');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const exists=p=>fs.existsSync(path.join(root,p));
const checks=[];
const ok=(name,cond,detail='')=>checks.push({name,pass:!!cond,detail});

const version=read('VERSION').trim();
const pom=read('backend/pom.xml');
const pkg=JSON.parse(read('frontend/package.json'));
const runtimeRepo=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/readiness/oracle/DepositOpeningRuntimeRepository.java');
const aggSvc=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/application/DepositOpeningAggregateService.java');
const wizard=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts');
const fundRepo=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/oracle/DepositOpeningOperationalRepository.java');
const runtimeHarness=read('tools/runtime-dps2-phase10f-e2e.mjs');
const phase7=read('database/oracle/dps2/migrations/0.7.0-phase7-opening-e2e-hardening.sql');
const phase10=read('database/oracle/dps2/migrations/0.10.0-phase10-opening-operational-v5-foundation.sql');

ok('VERSION is exactly 0.10.0',version==='0.10.0',version);
ok('Frontend version matches 0.10.0',pkg.version===version,pkg.version);
ok('Backend Maven source is qualified 0.10.0-SNAPSHOT',pom.includes(`<version>${version}-SNAPSHOT</version>`));

const requiredMigrations=[
  'database/oracle/dps2/migrations/0.10.0-phase10-opening-operational-v5-foundation.sql',
  'database/oracle/dps2/migrations/0.10.0-phase10a-constraint-reconciliation-repair.sql',
  'database/oracle/dps2/migrations/0.10.0-phase10b-opening-operational-runtime-alignment.sql',
  'database/oracle/dps2/migrations/0.10.0-phase10f-created-at-default-reconciliation.sql',
  'database/oracle/dps2/migrations/0.10.0-phase10f-created-account-reference-reconciliation.sql',
  'database/oracle/dps2/migrations/0.10.0-phase10f-fund-alloc-column-reconciliation.sql',
  'database/oracle/dps2/migrations/0.10.0-phase10f-fund-alloc-legacy-column-cleanup.sql',
  'database/oracle/dps2/migrations/0.10.0-phase10f-check-result-status-reconciliation.sql'
];
for(const f of requiredMigrations) ok(`Required migration present: ${path.basename(f)}`,exists(f));

const allowlist=['REF_DEP_OPEN_FUND_PURPOSE','REF_DEP_OPEN_OBLIGATION_TYPE','REF_DEP_OPEN_SETTLEMENT_STATUS','REF_DEP_OPEN_CHECK_PHASE','REF_DEP_OPEN_BLOCKING_SCOPE','REF_DEP_OPEN_JOINT_BASIS','REF_DEP_OPEN_ACTIVATION_STATUS'];
ok('Phase10F runtime reference allow-list is complete',allowlist.every(x=>runtimeRepo.includes(`"${x}"`)));
ok('CASH funding requires CASH_MANAGEMENT_TXN_REF in backend',aggSvc.includes('"CASH".equalsIgnoreCase(value.fundingMethodCode())')&&aggSvc.includes('DEPOSIT_OPENING_FUNDING.CASH_MANAGEMENT_TXN_REF'));
ok('Angular sends CASH management transaction reference',wizard.includes("CASH_MANAGEMENT_TXN_REF:x.method==='CASH'?(x.sourceReference||null):null"));
ok('Operational repository uses canonical ALLOCATED_AMOUNT',fundRepo.includes('ALLOCATED_AMOUNT')&&!fundRepo.includes('ALLOCATION_AMOUNT'));
ok('Phase7 unique guard is semantic, not index-name dependent',phase7.includes('unique_guard_exists')&&phase7.includes("i.uniqueness = 'UNIQUE'"));
ok('Phase10 index reconciliation is semantic',phase10.includes('index_covers_leading_column')||phase10.includes('index coverage'));
ok('Runtime harness refreshes controlled evidence validity at readiness',runtimeHarness.includes('Date.now()+2*3600_000')&&runtimeHarness.includes('readinessEvidence'));
ok('Final DB verifier is present',exists('database/oracle/dps2/verification/0.10.0-final-verifier.sql'));
ok('Final closure docs are present',exists('release/FINAL-MIGRATION-MANIFEST-0.10.0.md')&&exists('release/FINAL-FREEZE-0.10.0.md'));

const verifierScripts=[
  'tools/verify-dps2-deposit-opening-phase7.mjs',
  'tools/verify-dps2-account-schema-reconciliation-092.mjs',
  'tools/verify-dps2-reference-fk-reconciliation-093.mjs',
  'tools/verify-dps2-opening-operational-v5-phase10.mjs',
  'tools/verify-dps2-opening-operational-v5-phase10b.mjs',
  'tools/verify-dps2-opening-operational-v5-phase10d.mjs',
  'tools/verify-dps2-opening-v5-phase10e10f.mjs',
  'tools/verify-dps2-phase10f-fund-alloc-contract.mjs',
  'tools/verify-runtime-artifact-contract.mjs',
  'tools/verify-release-layout.mjs'
].filter(exists);

for(const script of verifierScripts){
  const r=spawnSync(process.execPath,[path.join(root,script)],{cwd:root,encoding:'utf8'});
  ok(`Static verifier: ${path.basename(script)}`,r.status===0,(r.stdout+r.stderr).trim().split(/\r?\n/).slice(-1)[0]||'');
}

const hashManifestPath=path.join(root,'release','FINAL-BASELINE-HASHES-0.10.0.sha256');
if(fs.existsSync(hashManifestPath)){
  const lines=fs.readFileSync(hashManifestPath,'utf8').split(/\r?\n/).map(x=>x.trim()).filter(Boolean);
  let hashFailures=0;
  for(const line of lines){
    const m=line.match(/^([0-9a-f]{64})\s+(.+)$/i);
    if(!m){ hashFailures++; continue; }
    const [,expected,relRaw]=m;
    const rel=relRaw.replace(/^\*?/,'');
    const full=path.join(root,rel);
    if(!fs.existsSync(full)){ hashFailures++; continue; }
    const actual=crypto.createHash('sha256').update(fs.readFileSync(full)).digest('hex');
    if(actual.toLowerCase()!==expected.toLowerCase()) hashFailures++;
  }
  ok('Frozen SHA-256 source baseline matches',hashFailures===0,`files=${lines.length}, mismatches=${hashFailures}`);
}else{
  ok('Frozen SHA-256 source baseline exists',false,'release/FINAL-BASELINE-HASHES-0.10.0.sha256 missing');
}

const jar=path.join(root,'app','core-banking-prototype.jar');
const buildVersion=path.join(root,'app','BUILD-VERSION');
if(requireRuntime){
  ok('Runtime JAR exists',fs.existsSync(jar));
  ok('BUILD-VERSION exists',fs.existsSync(buildVersion));
  if(fs.existsSync(buildVersion)) ok('BUILD-VERSION equals 0.10.0',fs.readFileSync(buildVersion,'utf8').trim()===version,fs.readFileSync(buildVersion,'utf8').trim());
  if(fs.existsSync(jar)){
    const h=crypto.createHash('sha256').update(fs.readFileSync(jar)).digest('hex');
    const stat=fs.statSync(jar);
    console.log(`INFO JAR_SHA256=${h}`);
    console.log(`INFO JAR_SIZE=${stat.size}`);
  }
  const logPath=path.join(root,'logs','core-banking-prototype.log');
  if(fs.existsSync(logPath)){
    const log=fs.readFileSync(logPath,'utf8');
    const starts=[...log.matchAll(/Starting CoreBankingApplication v([^\s]+)/g)].map(m=>m[1]);
    const latest=starts.at(-1)||'';
    ok('Latest startup log is 0.10.0-SNAPSHOT',latest==='0.10.0-SNAPSHOT',latest||'<not found>');
  } else {
    ok('Runtime log exists for version audit',false,'logs/core-banking-prototype.log not found');
  }
}

let failed=0;
for(const c of checks){
  console.log(`${c.pass?'PASS':'FAIL'} | ${c.name}${c.detail?` | ${c.detail}`:''}`);
  if(!c.pass) failed++;
}
console.log('------------------------------------------------------------');
console.log(`FINAL_SOURCE_VERIFIER_PASS=${checks.length-failed}`);
console.log(`FINAL_SOURCE_VERIFIER_FAIL=${failed}`);
if(failed) process.exit(1);
console.log('FINAL_SOURCE_BASELINE_PASS');
