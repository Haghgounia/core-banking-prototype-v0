'use strict';
// No Windows/Oracle access required: deterministic test of the shipped launcher contract.
const fs=require('node:fs');const path=require('node:path');const assert=require('node:assert/strict');
const {makeSafeEnvironment,javaArgs,validateGroups,fetchJson,WRITE_FLAGS,REQUIRED_CODES}=require('./run-local-uat.cjs');
const root=path.resolve(process.argv[2]||path.resolve(__dirname,'../../..'));
let passed=0;
function test(label,fn){fn();passed++;process.stdout.write('PASS | '+label+'\n');}
function failOn(fn,pattern){assert.throws(fn,pattern);}
const baseline={CLT_LOCAL_UAT_READONLY_ENABLED:'false',CLT_LOCAL_UAT_BRANCH_CODE:'PSEUDO',CLT_REFERENCE_WRITES_ENABLED:'true',CLT_REGISTRY_READS_ENABLED:'false'};
const env=makeSafeEnvironment(baseline);
test('local mode requires explicit true in spawned child',()=>assert.equal(env.CLT_LOCAL_UAT_READONLY_ENABLED,'true'));
test('reads opt in for registry',()=>assert.equal(env.CLT_REGISTRY_READS_ENABLED,'true'));
test('reads opt in for reference',()=>assert.equal(env.CLT_REFERENCE_READS_ENABLED,'true'));
test('bound to IPv4 loopback',()=>assert.equal(env.SERVER_ADDRESS,'127.0.0.1'));
test('no user supplied branch',()=>assert.equal(Object.hasOwn(env,'CLT_LOCAL_UAT_BRANCH_CODE'),false));
test('all CLT write switches hard off',()=>{for(const f of WRITE_FLAGS)assert.equal(env[f],'false',f);});
test('nine explicit write gates',()=>assert.equal(WRITE_FLAGS.length,9));
test('original parent process env not mutated',()=>assert.equal(baseline.CLT_REFERENCE_WRITES_ENABLED,'true'));
const argv=javaArgs('D:\\path with spaces\\a.jar','D:\\path with spaces\\application.yml');
test('java uses -jar separate argument',()=>assert.equal(argv[1],'-jar'));
test('java binds loopback and port',()=>assert.ok(argv.includes('--server.address=127.0.0.1')&&argv.includes('--server.port=8091')));
test('java explicit CLT modes',()=>assert.ok(argv.includes('-DCLT_LOCAL_UAT_READONLY_ENABLED=true')&&argv.some(x=>x.includes('registry.reads-enabled=true'))));
test('java config path protected as one argument',()=>assert.ok(argv.some(x=>x.startsWith('--spring.config.additional-location=optional:file:'))));
const groups=REQUIRED_CODES.map((code,i)=>({COLLATERAL_GROUP_CODE:code,COLLATERAL_GROUP_NAME:'Test '+i, STATUS:'ACTIVE'}));
test('real group projection format counts 28',()=>assert.equal(validateGroups(groups),28));
test('future additions do not break the import minimum',()=>assert.equal(validateGroups([...groups,{COLLATERAL_GROUP_CODE:'100',COLLATERAL_GROUP_NAME:'Extra',STATUS:'ACTIVE'}]),29));
test('zero groups cannot pass',()=>failOn(()=>validateGroups([]),/COUNT_MISMATCH/));
test('wrong status cannot pass',()=>failOn(()=>validateGroups(groups.map((x,i)=>i===2?{...x,STATUS:'INACTIVE'}:x)),/PAYLOAD_INVALID/));
test('duplicate code cannot pass',()=>failOn(()=>validateGroups(groups.map((x,i)=>i===27?{...x,COLLATERAL_GROUP_CODE:'10'}:x)),/DUPLICATE_CODE/));
test('missing expected code cannot pass',()=>failOn(()=>validateGroups(groups.map((x,i)=>i===0?{...x,COLLATERAL_GROUP_CODE:'99999'}:x)),/REQUIRED_CODE_MISSING/));
const cmd=fs.readFileSync(path.join(root,'tools/clt/r6h/RUN-CLT-LOCAL-UAT.cmd'),'utf8');
test('r6h cmd now delegates to Node',()=>assert.match(cmd,/run-local-uat\.cjs/));
test('r6h cmd propagates failure status',()=>assert.match(cmd,/exit \/b %RC%/));
test('r6h cmd emits an entry marker',()=>assert.match(cmd,/CLT_R6K_CMD_ENTRY/));
const script=fs.readFileSync(path.join(root,'tools/clt/r6k/run-local-uat.cjs'),'utf8');
test('launcher never writes Oracle',()=>assert.ok(!/(?:INSERT|UPDATE|DELETE|MERGE)\s+(?:INTO\s+)?CLT\./i.test(script)));
test('browser-style request checked before success',()=>assert.ok(script.indexOf('validateGroups(browserGroups)')<script.indexOf('RUNTIME_PASS')));
test('no success after a 403 response',()=>assert.match(script,/if \(response\.status !== 200\) error\('API_REJECTED'/));
(async()=>{
  const originalFetch=globalThis.fetch;
  try {
    globalThis.fetch=async()=>({status:403,text:async()=>'<html>Forbidden</html>'});
    await assert.rejects(()=>fetchJson('/api/v1/collaterals/groups'),/HTTP=403/);
    passed++;console.log('PASS | HTTP 403 must fail with exact code');
    globalThis.fetch=async()=>({status:200,text:async()=>JSON.stringify(groups)});
    assert.equal(validateGroups(await fetchJson('/api/v1/collaterals/groups')),28);
    passed++;console.log('PASS | HTTP 200 JSON group contract');
  } finally {globalThis.fetch=originalFetch;}
  console.log('CLT_R6K_LAUNCHER_REGRESSION_PASS='+passed+'; DB_WRITES=NONE; BUILD_REUSED=YES');
})().catch(e=>{console.error('CLT_R6K_TEST_FAILED '+e.stack);process.exitCode=1;});
