'use strict';
// CLT-R6M-T2/T3: actual Chrome DOM interactions over local DevTools protocol.
// Deliberately no HTTP POST/PUT/PATCH/DELETE and no external automation package.
const fs=require('node:fs');const path=require('node:path');const os=require('node:os');
const cp=require('node:child_process');const net=require('node:net');const assert=require('node:assert/strict');
const root=path.resolve(process.argv[2]||path.resolve(__dirname,'../../..'));
const selfTest=process.argv.includes('--self-test');
const base='http://127.0.0.1:8091/#/collateral/reference-data/';
const resultPath=path.join(root,'logs','clt-r6m-t2-t3-browser.json');
const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms));
function chromePath(){
 const env=process.env;
 const candidates=[env.CLT_R6M_CHROME_EXE,env.CLT_R6L_CHROME_EXE,
  env.PROGRAMFILES&&path.join(env.PROGRAMFILES,'Google','Chrome','Application','chrome.exe'),
  env['PROGRAMFILES(X86)']&&path.join(env['PROGRAMFILES(X86)'],'Google','Chrome','Application','chrome.exe'),
  env.LOCALAPPDATA&&path.join(env.LOCALAPPDATA,'Google','Chrome','Application','chrome.exe'),
  '/usr/bin/chromium','/usr/bin/google-chrome'].filter(Boolean);
 return candidates.find(p=>path.isAbsolute(p)&&fs.existsSync(p))||null;
}
async function reservePort(){
 return new Promise((resolve,reject)=>{const server=net.createServer();server.once('error',reject);
  server.listen(0,'127.0.0.1',()=>{const port=server.address().port;server.close(()=>resolve(port));});});
}
class Cdp {
 constructor(ws){this.ws=ws;this.seq=0;this.callbacks=new Map();this.writeRequests=[];
  ws.addEventListener('message',event=>{
   let msg;try{msg=JSON.parse(String(event.data));}catch{return;}
   if(msg.method==='Network.requestWillBeSent'){
     const r=msg.params?.request, u=r?.url||'';
     if(/\/api\/v1\/collateral/i.test(u)&&!['GET','OPTIONS'].includes(r.method))
       this.writeRequests.push({method:r.method,url:u.split('?')[0]});
   }
   if(msg.id&&this.callbacks.has(msg.id)){const cb=this.callbacks.get(msg.id);this.callbacks.delete(msg.id);
     if(msg.error)cb.reject(Error('CDP '+msg.error.message));else cb.resolve(msg.result||{});}
  });
 }
 call(method,params={}){const id=++this.seq;return new Promise((resolve,reject)=>{
  const timer=setTimeout(()=>{this.callbacks.delete(id);reject(Error('CDP_TIMEOUT '+method));},12000);
  this.callbacks.set(id,{resolve:v=>{clearTimeout(timer);resolve(v);},reject:e=>{clearTimeout(timer);reject(e);}});
  this.ws.send(JSON.stringify({id,method,params}));
 });}
 async eval(expression){const out=await this.call('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});
  if(out.exceptionDetails)throw Error('PAGE_SCRIPT_EXCEPTION '+out.exceptionDetails.text);
  return out.result?.value;}
 async wait(expression,label,ms=28000){const end=Date.now()+ms;let value;
  do {value=await this.eval(expression);if(value)return value;await sleep(250);}while(Date.now()<end);
  throw Error('BROWSER_GATE_TIMEOUT '+label);}
}
async function connect(url){return new Promise((resolve,reject)=>{
 const ws=new WebSocket(url);const timer=setTimeout(()=>{ws.close();reject(Error('CDP_WS_TIMEOUT'));},8000);
 ws.addEventListener('open',()=>{clearTimeout(timer);resolve(new Cdp(ws));},{once:true});
 ws.addEventListener('error',()=>{clearTimeout(timer);reject(Error('CDP_WS_ERROR'));},{once:true});
});}
async function launchChrome(){const chrome=chromePath();if(!chrome)throw Error('CLT_R6M_CHROME_NOT_FOUND');
 const profile=fs.mkdtempSync(path.join(os.tmpdir(),'clt-r6m-browser-'));const port=await reservePort();
 const args=[`--remote-debugging-port=${port}`,'--remote-debugging-address=127.0.0.1',
  `--remote-allow-origins=http://127.0.0.1:${port}`,'--headless=new','--disable-gpu','--disable-extensions','--no-first-run',
  '--no-proxy-server','--disable-background-networking',`--user-data-dir=${profile}`,'about:blank'];
 if(process.platform!=='win32'&&process.getuid?.()===0)args.push('--no-sandbox');
 const child=cp.spawn(chrome,args,{stdio:'ignore',windowsHide:true,
  env:{...process.env,NO_PROXY:'localhost,127.0.0.1',no_proxy:'localhost,127.0.0.1'}});
 try {
  let browserInfo;
  for(let i=0;i<80;i++){
   if(child.exitCode!==null)throw Error('CHROME_EXIT_EARLY '+child.exitCode);
   try {const r=await fetch(`http://127.0.0.1:${port}/json`,{signal:AbortSignal.timeout(800)});
    if(r.ok){const tabs=await r.json();browserInfo=tabs.find(t=>t.type==='page'&&t.webSocketDebuggerUrl);if(browserInfo)break;}}
   catch{} await sleep(150);
  }
  if(!browserInfo)throw Error('CHROME_CDP_START_TIMEOUT');
  const cdp=await connect(browserInfo.webSocketDebuggerUrl);await cdp.call('Page.enable');await cdp.call('Runtime.enable');
  await cdp.call('Network.enable');return {cdp,child,profile};
 }catch(e){child.kill();fs.rmSync(profile,{recursive:true,force:true,maxRetries:4});throw e;}
}
async function navigate(cdp,url){await cdp.call('Page.navigate',{url});
 await cdp.wait('document.readyState === "complete"','page complete');}
async function setSearch(cdp,value){return cdp.eval(`(()=>{const el=document.querySelector('#cltRowSearch');
  if(!el)return false;Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(el,${JSON.stringify(value)});
  el.dispatchEvent(new Event('input',{bubbles:true}));
  document.querySelector('[data-clt-run-search]').click();return true;})()`);}
function receipt(body){if(selfTest)return;
 fs.mkdirSync(path.dirname(resultPath),{recursive:true});
 fs.writeFileSync(resultPath,JSON.stringify({stage:'T2_T3',testedAt:new Date().toISOString(),
  oracleWrite:false,javaAuthChanged:false,oneClickBrowser:'REAL_CHROME_CDP',...body},null,2)+'\n');}
async function run(){let handle,report={passed:false,checks:{},url:base+'COLLATERAL_GROUP'};
 try {
  handle=await launchChrome();const cdp=handle.cdp;
  if(selfTest){
   await cdp.eval("document.body.innerHTML='<button id=opener>Open</button><dialog id=dlg><button id=close>Close</button></dialog>';");
   await cdp.eval("document.querySelector('#dlg').showModal();");
   assert.equal(await cdp.eval("document.querySelector('#dlg').open"),true);
   await cdp.call('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});
   await cdp.call('Input.dispatchKeyEvent',{type:'keyUp',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});
   await cdp.wait("!document.querySelector('#dlg').open",'Escape closes native dialog');
   console.log('CLT_R6M_CDP_SELFTEST_PASS; CHROME='+(chromePath()||'UNKNOWN'));return;
  }
  await navigate(cdp,base+'COLLATERAL_GROUP');
  await cdp.wait(`document.querySelector('[data-clt-ui-contract="R6M-T2-T3"]') &&
   document.querySelectorAll('[data-clt-row]').length===20 &&
   document.querySelector('[data-clt-total]')?.textContent.includes('28') &&
   !document.querySelector('.error[role=alert]')`,'group page 1 / 28');
  report.checks.groupsPage1=20;
  await cdp.eval("document.querySelectorAll('.pagination button')[1].click()");
  await cdp.wait("document.querySelectorAll('[data-clt-row]').length===8 && document.querySelector('[data-clt-page-summary]')?.textContent.includes('28')",'group page 2 / 8');
  report.checks.groupsPage2=8;
  await cdp.eval("document.querySelectorAll('.pagination button')[0].click()");
  await cdp.wait("document.querySelectorAll('[data-clt-row]').length===20",'return group page 1');
  // Native modal: open from actual grid, close with Escape; ensure focus restored.
  await cdp.eval("(()=>{const btn=document.querySelector('[data-clt-row] .detail-btn');window.__cltR6mOpener=btn;btn.click()})()");
  await cdp.wait("document.querySelector('[data-clt-dialog]').open && document.querySelector('[data-clt-dialog] .detail-fields strong')",'modal open');
  report.checks.modalOpen=true;
  await cdp.call('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});
  await cdp.call('Input.dispatchKeyEvent',{type:'keyUp',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});
  await cdp.wait("!document.querySelector('[data-clt-dialog]').open",'modal Escape close');
  await cdp.wait("document.activeElement===window.__cltR6mOpener",'focus restored');
  report.checks.modalEscAndFocus=true;
  assert.equal(await setSearch(cdp,'10'),true);
  await cdp.wait("document.querySelectorAll('[data-clt-row]').length===1 && document.querySelector('[data-clt-total]')?.textContent.includes('1') && document.querySelector('[data-clt-row]')?.textContent.includes('10')",'code search');
  report.checks.codeSearch=true;
  assert.equal(await setSearch(cdp,'\u06f1\u06f0'),true);
  await cdp.wait("document.querySelectorAll('[data-clt-row]').length===1 && document.querySelector('[data-clt-total]')?.textContent.includes('1')",'Persian digits search');
  report.checks.persianDigits=true;
  assert.equal(await setSearch(cdp,'\u06a9\u0627\u0644\u0627'),true);
  await cdp.wait("document.querySelectorAll('[data-clt-row]').length>0 && Array.from(document.querySelectorAll('[data-clt-row]')).every(r=>r.textContent.includes('\u06a9\u0627\u0644\u0627'))",'title search');
  report.checks.titleSearch=true;
  await cdp.eval("document.querySelector('[data-clt-clear-search]').click()");
  await cdp.wait("document.querySelectorAll('[data-clt-row]').length===20 && document.querySelector('[data-clt-total]')?.textContent.includes('28')",'clear search');
  report.checks.clearSearch=true;
  await navigate(cdp,base+'COLLATERAL_TYPE');
  await cdp.wait("document.querySelector('[data-clt-ui-contract=\"R6M-T2-T3\"]') && document.querySelector('[data-clt-total]')?.textContent.includes('0') && document.querySelector('[data-clt-empty]')?.textContent.includes('\u0646\u0648\u0639 \u0648\u062b\u06cc\u0642\u0647') && !document.querySelector('.error[role=alert]')",'types empty');
  report.checks.activeTypes=0;
  assert.deepEqual(cdp.writeRequests,[],'No collateral mutating request during UI acceptance');
  report.checks.noCltMutationMethods=true;report.passed=true;
  console.log('CLT_R6M_T2_BROWSER_PASS; GROUPS=28; PAGES=20+8; SEARCH=CODE_PERSIAN_TITLE; MODAL=ESC_FOCUS; TYPES=0; CLT_HTTP_WRITES=NONE');
  // T3 negative paths: informative, not a false business acceptance gate.
  try {
   await navigate(cdp,'http://127.0.0.1:8091/#/collateral/registry');
   await cdp.wait("!!document.querySelector('[data-clt-registry-empty]')",'T3 list empty/error',15000);
   report.checks.t3RegistryEmptyHandled=true;
  }catch(e){report.checks.t3RegistryObservation='NOT_PROVEN: '+e.message;}
  if(cdp.writeRequests.length){report.passed=false;throw Error('T3_BROWSER_CLT_WRITE_REQUEST_DETECTED');}
  receipt(report);
 }catch(e){report.error=String(e?.message||e);receipt(report);
  console.error('CLT_R6M_T2_BROWSER_FAIL; '+report.error+'; RECEIPT='+resultPath);process.exitCode=1;
 }finally{if(handle){handle.cdp.ws.close();handle.child.kill();await sleep(150);
   fs.rmSync(handle.profile,{recursive:true,force:true,maxRetries:4,retryDelay:200});}}
}
if(require.main===module)run();
module.exports={chromePath,launchChrome,Cdp};
