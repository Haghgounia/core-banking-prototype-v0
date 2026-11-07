'use strict';
// CLT-R6K: Windows-only, single-machine read-only UAT boot. No PowerShell needed.
// It never changes application.yml, IAM, or database state and never grants POST.
const fs = require('node:fs');
const path = require('node:path');
const net = require('node:net');
const cp = require('node:child_process');
const { pathToFileURL } = require('node:url');
const MARKER = 'CLT_R6K';
const PORT = 8091;
const HOST = '127.0.0.1';
const REQUIRED_CODES = Object.freeze([
  ...Array.from({ length:21 },(_,i)=>String(10+i)),
  '41','42','43','44','50','60','70'
]);
const WRITE_FLAGS = Object.freeze([
  'CLT_REFERENCE_WRITES_ENABLED',
  'CLT_INTRODUCTION_DRAFT_WRITES_ENABLED',
  'CLT_INTRODUCTION_REVIEW_WORKFLOW_ENABLED',
  'CLT_SPONSOR_REQUEST_WRITES_ENABLED',
  'CLT_INSURANCE_REQUEST_WRITES_ENABLED',
  'CLT_VALUATION_REQUEST_WRITES_ENABLED',
  'CLT_ALLOCATION_INTAKE_WRITES_ENABLED',
  'CLT_CAPACITY_LOCK_PROBE_ENABLED',
  'CLT_UNALLOCATION_INTAKE_WRITES_ENABLED'
]);
function say(line) { process.stdout.write(line + '\n'); }
function error(code, details = '') { throw new Error(`${MARKER}_${code}${details ? ': ' + details : ''}`); }
function pathWithin(root, ...items) {
  const target = path.resolve(root, ...items);
  if (target !== root && !target.startsWith(root + path.sep)) error('PATH_ESCAPE');
  return target;
}
function makeSafeEnvironment(base = process.env) {
  const env = { ...base,
    CLT_LOCAL_UAT_READONLY_ENABLED: 'true',
    CLT_REFERENCE_READS_ENABLED: 'true',
    CLT_REGISTRY_READS_ENABLED: 'true',
    SERVER_ADDRESS: HOST
  };
  delete env.CLT_LOCAL_UAT_BRANCH_CODE;
  for (const name of WRITE_FLAGS) env[name] = 'false';
  return env;
}
function javaArgs(jar, config) {
  return [
    '-DCLT_LOCAL_UAT_READONLY_ENABLED=true', '-jar', jar,
    '--server.address=' + HOST,
    '--server.port=' + PORT,
    '--core-banking.collateral.reference.reads-enabled=true',
    '--core-banking.collateral.registry.reads-enabled=true',
    '--spring.config.additional-location=optional:' + pathToFileURL(config).href
  ];
}
function validateGroups(value) {
  if (!Array.isArray(value) || value.length < 28) error('ORACLE_GROUP_COUNT_MISMATCH', 'EXPECTED_MIN=28; ACTUAL=' + (Array.isArray(value) ? value.length : 'INVALID_JSON'));
  const seen = new Set();
  for (const row of value) {
    if (!row || row.STATUS !== 'ACTIVE' || !/^\d{1,5}$/.test(String(row.COLLATERAL_GROUP_CODE ?? '')) ||
        typeof row.COLLATERAL_GROUP_NAME !== 'string' || !row.COLLATERAL_GROUP_NAME.trim()) error('GROUP_PAYLOAD_INVALID');
    const code = String(row.COLLATERAL_GROUP_CODE);
    if (seen.has(code)) error('GROUP_DUPLICATE_CODE', code);
    seen.add(code);
  }
  for (const code of REQUIRED_CODES) {
    if (!seen.has(code)) error('GROUP_REQUIRED_CODE_MISSING', code);
  }
  return value.length;
}
function checkSource(root) {
  const version = pathWithin(root, 'VERSION');
  const jar = pathWithin(root, 'app', 'core-banking-prototype.jar');
  const build = pathWithin(root, 'app', 'BUILD-VERSION');
  const dirty = pathWithin(root, 'app', 'BUILD-DIRTY');
  const config = pathWithin(root, 'config', 'application.yml');
  for (const p of [version, build, config, jar]) {
    if (!fs.existsSync(p)) error('PREREQUISITE_MISSING', p);
  }
  if (fs.existsSync(dirty)) error('DIRTY_BUILD_DENIED', 'Run the normal production build once');
  if (fs.readFileSync(version, 'utf8').trim() !== '0.11.0' ||
      fs.readFileSync(build, 'utf8').trim() !== '0.11.0') error('VERSION_MISMATCH');
  if (fs.statSync(jar).size < 1024 * 1024) error('RUNTIME_JAR_INVALID');
  for (const [rel, content] of [
    ['backend/src/main/java/com/behsazan/corebanking/collateral/localuat/CltLocalUatSecurityConfiguration.java', '/api/v1/collaterals/groups'],
    ['backend/src/main/java/com/behsazan/corebanking/collateral/registry/CollateralRegistryController.java', 'CLT_R6I_ACTIVE_GROUPS_20261104'],
    ['frontend/src/app/features/collateral/clt-r6j-registry-reference.ts', 'COLLATERAL_GROUP']
  ]) {
    const f = pathWithin(root, ...rel.split('/'));
    if (!fs.existsSync(f) || !fs.readFileSync(f, 'utf8').includes(content)) error('R6J_BASELINE_MISSING', rel);
  }
  return { jar, config };
}
async function ensurePortFree() {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.on('error', e => reject(new Error(`${MARKER}_PORT_BUSY; ${e.code}; STOP_PREVIOUS_PROTOTYPE_FIRST`)));
    server.listen(PORT, HOST, () => server.close(resolve));
  });
}
function readTail(file, lines = 15) {
  try { return fs.readFileSync(file, 'utf8').split(/\r?\n/).slice(-lines).join('\n'); }
  catch { return 'LOG_UNAVAILABLE'; }
}
async function fetchJson(route, headers = {}, timeout = 5000) {
  const url = `http://${HOST}:${PORT}${route}`;
  const response = await fetch(url, { method:'GET', headers, redirect:'manual', signal:AbortSignal.timeout(timeout) });
  const body = await response.text();
  if (response.status !== 200) error('API_REJECTED', `ROUTE=${route}; HTTP=${response.status}; BODY=${body.slice(0,120).replace(/[\r\n]+/g,' ')}`);
  try { return JSON.parse(body); } catch { error('API_INVALID_JSON', route); }
}
async function start(root) {
  say(MARKER + '_START; MODE=LOCAL_READ_ONLY; NODE=' + process.version);
  if (process.platform !== 'win32') error('WINDOWS_REQUIRED');
  if (Number(process.versions.node.split('.')[0]) < 20) error('NODE20_REQUIRED');
  const { jar, config } = checkSource(root);
  await ensurePortFree();
  say(MARKER + '_PREFLIGHT_PASS; PORT=FREE; VERSION=0.11.0; BUILD=REUSED');
  const logs = pathWithin(root,'logs');
  fs.mkdirSync(logs,{recursive:true});
  const output = pathWithin(logs, 'clt-r6k-stdout.log');
  const errors = pathWithin(logs, 'clt-r6k-stderr.log');
  const outFd = fs.openSync(output,'w');
  const errFd = fs.openSync(errors,'w');
  let child;
  try {
    const java = process.env.JAVA_HOME
      ? path.join(process.env.JAVA_HOME, 'bin', 'java.exe') : 'java.exe';
    child = cp.spawn(java, javaArgs(jar,config), {
      cwd:root, env:makeSafeEnvironment(), detached:true, windowsHide:true,
      stdio:['ignore',outFd,errFd]
    });
  } finally { fs.closeSync(outFd); fs.closeSync(errFd); }
  if (!child.pid) {
    child.on('error', () => {});
    error('JAVA_SPAWN_FAILED', 'java.exe could not be started; check JAVA_HOME or PATH');
  }
  // The Java child may exit very early; always register lifecycle handlers and check the server.
  let alive=true;
  let processError=null;
  child.on('exit', () => { alive=false; });
  child.on('error', e => { processError=e; alive=false; });
  child.unref();
  say(MARKER + '_BOOTING; PID=' + child.pid + '; NO_REBUILD=YES; DB_WRITES=NONE');
  let healthOk=false;
  for(let n=0; n<100 && alive; n++) {
    try {
      const response=await fetch(`http://${HOST}:${PORT}/actuator/health`,{signal:AbortSignal.timeout(1500)});
      if (response.status===200) { healthOk=true; await response.body?.cancel(); break; }
      await response.body?.cancel();
    } catch {}
    await new Promise(resolve=>setTimeout(resolve,700));
  }
  if (!healthOk) {
    if (alive) child.kill();
    error('STARTUP_FAILED', `JAVA=${processError?.message||'no healthy listener'}; STDOUT=${readTail(output)}; STDERR=${readTail(errors)}`);
  }
  const stdout=fs.readFileSync(output,'utf8');
  if (!stdout.includes('CLT_R6H_LOCAL_BOOTSTRAP_ACTIVE')) {
    if (alive) child.kill();
    error('LOCAL_MODE_NOT_ACTIVE', `STDOUT=${readTail(output)}; STDERR=${readTail(errors)}`);
  }
  try {
    const groups=await fetchJson('/api/v1/collaterals/groups', { Accept:'application/json' });
    const count=validateGroups(groups);
    // Reproduce headers added by Angular/Chrome for an API call; no custom privileged header.
    const browserGroups=await fetchJson('/api/v1/collaterals/groups', {
      Accept:'application/json, text/plain, */*',
      'User-Agent':'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/130.0.0.0 Safari/537.36',
      'Sec-Fetch-Mode':'cors', 'Sec-Fetch-Site':'same-origin', 'Sec-Fetch-Dest':'empty',
      Referer:`http://${HOST}:${PORT}/#/collateral/reference-data/COLLATERAL_GROUP`
    });
    validateGroups(browserGroups);
    const types=await fetchJson('/api/v1/collaterals/types', { Accept:'application/json' });
    if (!Array.isArray(types)) error('TYPE_PAYLOAD_INVALID');
    say(MARKER + `_RUNTIME_PASS; HEALTH=200; API_GROUPS=200; BROWSER_HEADERS=200; GROUPS=${count}; TYPES=${types.length}`);
    say(MARKER + '_LOCAL_READONLY_ATTESTED; IAM_UNCHANGED=YES; FINANCIAL_WRITES=NONE');
    say(`OPEN_LOCAL_URL=http://${HOST}:${PORT}/#/collateral/reference-data/COLLATERAL_GROUP`);
    const receipt=pathWithin(root,'tools','clt','r6k','runtime-receipt.json');
    fs.mkdirSync(path.dirname(receipt),{recursive:true});
    fs.writeFileSync(receipt,JSON.stringify({delivery:'CLT-R6K',at:new Date().toISOString(),pid:child.pid,
      healthHttp:200, groupsHttp:200, browserHeaderHttp:200, groupCount:count, typeCount:types.length,
      localOnly:true, getOnly:true, financialWrites:false, oracleDataWrite:false, analystUat:'NOT_RUN'},null,2)+'\n');
  } catch (e) {
    // Never leave an unverified background listener as a false successful start.
    if (alive) child.kill();
    error('STARTUP_UAT_GATE_FAILED', `${e.message}; STDERR=${readTail(errors)}`);
  }
}
if (require.main===module) {
  const root=path.resolve(process.argv[2] || path.resolve(__dirname,'../../..'));
  start(root).catch(e=> { process.stderr.write(MARKER+'_FAILED: '+e.message+'\n');process.exitCode=1; });
}
module.exports={makeSafeEnvironment,javaArgs,validateGroups,fetchJson,checkSource,ensurePortFree,WRITE_FLAGS,REQUIRED_CODES};
