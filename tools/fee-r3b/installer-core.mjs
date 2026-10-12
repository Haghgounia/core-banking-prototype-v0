import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const assetDir=path.join(root,'tools/fee-r3b');
const manifest=JSON.parse(fs.readFileSync(path.join(assetDir,'manifest.json'),'utf8'));
const sha=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
const file=p=>path.join(root,p);
const read=p=>fs.existsSync(file(p))?fs.readFileSync(file(p)):null;
const check=(ok,msg)=>{if(!ok)throw new Error('FEE_R3B_INSTALL_ABORT: '+msg+'; nothing will be overwritten on conflict.');};
const allowed=p=>p.startsWith('frontend/src/app/features/fee-admin/') ||
  p.startsWith('backend/src/main/java/com/behsazan/corebanking/fee/admin/') ||
  p.startsWith('backend/src/test/java/com/behsazan/corebanking/fee/') ||
  ['tools/verify-fee-r3a.mjs','tools/verify-fee-ux-r22.mjs','tools/verify-fee-ux-r23.mjs'].includes(p);
function examine(mode){
  const changed=[];
  const phases=[];
  check(manifest.id==='FEE_R3B_STAGE_INTEGRITY','manifest identity mismatch');
  for(const entry of manifest.files){
    check(allowed(entry.path),'unauthorized target '+entry.path);
    const before=fs.readFileSync(path.join(assetDir,'base',entry.asset));
    const after=fs.readFileSync(path.join(assetDir,'updated',entry.asset));
    check(sha(before)===entry.before && sha(after)===entry.after,'invalid patch asset '+entry.path);
    const current=read(entry.path);
    check(current!==null,'missing prerequisite R3-A/R2.3 source '+entry.path);
    const now=sha(current);
    check(now===entry.before || now===entry.after,'source modified by another release '+entry.path);
    phases.push(now===entry.before?'base':'updated');
    changed.push({path:entry.path,current,desired:mode==='install'?after:before});
  }
  for(const entry of manifest.newFiles){
    check(allowed(entry.path),'unauthorized new file '+entry.path);
    const after=fs.readFileSync(path.join(assetDir,'updated',entry.asset));
    check(sha(after)===entry.sha256,'invalid new file '+entry.path);
    const current=read(entry.path);
    check(current===null || sha(current)===entry.sha256,'new file collides with existing code '+entry.path);
    phases.push(current===null?'base':'updated');
    changed.push({path:entry.path,current,desired:mode==='install'?after:null});
  }
  check(new Set(phases).size===1,'mixed or partially applied R3-B installation');
  // Newer Product Builder releases may have modified shared routes; never overwrite them.
  const routes=read('frontend/src/app/app.routes.ts')?.toString('utf8')??'';
  check(routes.includes("path: 'fee/workspaces/:workspace'") &&
    routes.includes('canDeactivate: [(c: FeeAdminWorkspaceComponent) => c.canLeave()]'),
    'R3-A guarded workspace route not installed');
  const phase=phases[0];
  return {phase,changed};
}
function atomic(p,bytes){
  const target=file(p);
  if(bytes===null){if(fs.existsSync(target))fs.unlinkSync(target);return;}
  fs.mkdirSync(path.dirname(target),{recursive:true});
  const tmp=target+'.r3b-'+process.pid+'.tmp';
  try{fs.writeFileSync(tmp,bytes);fs.renameSync(tmp,target);}finally{if(fs.existsSync(tmp))fs.unlinkSync(tmp);}
}
export function run(mode,{checkOnly=false}={}){
  check(['install','uninstall'].includes(mode),'unknown action');
  const {phase,changed}=examine(mode);
  const desired=mode==='install'?'updated':'base';
  if(phase===desired){console.log('FEE_R3B_'+mode.toUpperCase()+'_ALREADY_'+desired.toUpperCase()+' | no changes');return;}
  if(checkOnly){console.log('FEE_R3B_'+mode.toUpperCase()+'_PRECHECK_PASS | '+changed.length+' FEE-only targets; no files changed');return;}
  const completed=[];
  try{
    for(const change of changed){
      const current=read(change.path);
      check((current===null&&change.current===null)||(current!==null&&change.current!==null&&current.equals(change.current)),
        'source changed during install '+change.path);
      atomic(change.path,change.desired);completed.push(change);
    }
  }catch(e){
    for(const c of completed.reverse()){
      try{atomic(c.path,c.current);}catch(err){console.error('FEE_R3B_ROLLBACK_FAILED',c.path,err);}
    }
    throw e;
  }
  console.log('FEE_R3B_'+mode.toUpperCase()+'_PASS | '+completed.length+' changes; Product Builder, FEE2, routing and Oracle untouched');
}
