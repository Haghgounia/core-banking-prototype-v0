'use strict';
// CLT-R6L-T1: real Angular DOM acceptance in isolated Chrome, not an API-only smoke.
// Uses Windows Chrome CLI; no npm packages, no browser extensions, no proxy.
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const cp = require('node:child_process');
const ROOT = path.resolve(process.argv[2] || path.resolve(__dirname,'../../..'));
const URL = 'http://127.0.0.1:8091/#/collateral/reference-data/COLLATERAL_GROUP';
const LOG_DIR = path.join(ROOT, 'logs');
const RECEIPT = path.join(LOG_DIR, 'clt-r6l-t1-browser-result.json');
const BASE = 'CLT_R6L';
function chromePath(env = process.env) {
  const candidates = [
    env.CLT_R6L_CHROME_EXE,
    env['PROGRAMFILES'] && path.join(env.PROGRAMFILES, 'Google','Chrome','Application','chrome.exe'),
    env['PROGRAMFILES(X86)'] && path.join(env['PROGRAMFILES(X86)'], 'Google','Chrome','Application','chrome.exe'),
    env.LOCALAPPDATA && path.join(env.LOCALAPPDATA,'Google','Chrome','Application','chrome.exe')
  ].filter(Boolean);
  return candidates.find(x => path.isAbsolute(x) && fs.existsSync(x)) || null;
}
function analyzeHtml(dom) {
  if (typeof dom !== 'string' || !dom.includes('data-clt-reference-build="R6L-T1"')) {
    return {pass:false, code:'R6L_UI_VERSION_MISSING', rows:0, total:null};
  }
  const body = dom.match(/<tbody\b[^>]*>([\s\S]*?)<\/tbody>/i)?.[1] || '';
  const rows = [...body.matchAll(/<tr\b/gi)].length;
  const summary = dom.match(/<div\b[^>]*class="[^"]*data-panel-title[^"]*"[^>]*>([\s\S]*?)<\/div>/i)?.[1] || '';
  const total = /28\s*رکورد(?:\s|<)/.test(summary) ? 28 : null;
  const accessDenied = /خواندن گروه‌ها\/انواع وثیقه رد شد|HTTP\s*403|HTTP\s*401|ارتباط با سرویس CLT برقرار نشد/.test(body+dom.slice(0,200000));
  const referenceLabelFound = body.includes('کالا'); // known Oracle source group code 10
  return {pass:rows===20&&total===28&&referenceLabelFound&&!accessDenied, code:accessDenied?'R6L_UI_ACCESS_DENIED':(rows!==20||total!==28||!referenceLabelFound?'R6L_UI_DATA_OR_GRID_MISMATCH':'PASS'), rows,total};
}
function makeReceipt(data) {
  fs.mkdirSync(LOG_DIR, {recursive:true});
  const report={stage:'T1', testedAt:new Date().toISOString(),
    browser:'CLEAN_CHROME_LOCAL_ONLY', url:URL, ...data,
    oracleWrite:false, iamChanged:false, analystUat:data.pass?'READY_FOR_ANALYST_REVIEW':'BLOCKED'};
  fs.writeFileSync(RECEIPT, JSON.stringify(report,null,2)+'\n');
  return report;
}
function verifyBrowser() {
  const chrome=chromePath();
  if (!chrome) throw Error('CLT_R6L_CHROME_NOT_FOUND; Chrome for the user is required for Browser E2E.');
  const profile=fs.mkdtempSync(path.join(os.tmpdir(),'clt-r6l-t1-'));
  try {
    const args=[
      '--headless=new','--disable-gpu','--disable-extensions','--disable-background-networking',
      '--no-first-run','--no-default-browser-check','--no-proxy-server',
      `--user-data-dir=${profile}`,'--virtual-time-budget=14000','--dump-dom',URL
    ];
    const result=cp.spawnSync(chrome,args,{encoding:'utf8',maxBuffer:10*1024*1024,timeout:60000,
      windowsHide:true,env:{...process.env,NO_PROXY:'127.0.0.1,localhost',no_proxy:'127.0.0.1,localhost'}});
    const check=analyzeHtml(result.stdout||'');
    const output=makeReceipt({pass:result.status===0&&check.pass,code:result.error?.code||check.code,
      rowsOnFirstPage:check.rows,totalRows:check.total,exitStatus:result.status,
      stderrTail:String(result.stderr||'').slice(-700).replace(/Bearer\s+[^\s]+/ig,'Bearer [REDACTED]')});
    if(!output.pass) throw Error(`${BASE}_BROWSER_E2E_FAIL; CODE=${output.code}; PAGE_ROWS=${check.rows}; TOTAL=${check.total ?? 'UNKNOWN'}; RECEIPT=${RECEIPT}`);
    console.log(`${BASE}_BROWSER_E2E_PASS; PAGE_1_ROWS=20; TOTAL=28; FRESH_CHROME=YES; NO_PROXY=YES`);
    return true;
  } finally {fs.rmSync(profile,{recursive:true,force:true,maxRetries:4,retryDelay:250});}
}
if (require.main === module) {
  try { verifyBrowser(); } catch(e) {
    console.error(String(e.message||e));process.exitCode=1;
  }
}
module.exports={analyzeHtml,chromePath,verifyBrowser};
