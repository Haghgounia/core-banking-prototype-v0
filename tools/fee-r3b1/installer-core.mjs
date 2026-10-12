import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const assets = path.join(root, 'tools/fee-r3b1');
const manifest = JSON.parse(fs.readFileSync(path.join(assets,'manifest.json'),'utf8'));
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const file = rel => path.join(root,rel);
const read = rel => fs.existsSync(file(rel)) ? fs.readFileSync(file(rel)) : null;
const abort = message => {throw new Error('FEE_R3B1_INSTALL_ABORT: '+message+'; no file was intentionally overwritten.');};
const assert = (condition,message) => {if(!condition)abort(message);};

function examine(action) {
  assert(manifest.id==='FEE_R3B1_COMPONENT_EMPTY_STATE','unexpected patch identity');
  assert(manifest.files.length===7 && manifest.newFiles.length===0,'unexpected number of targets');
  assert(fs.existsSync(file('backend/src/main/java/com/behsazan/corebanking/fee/admin/application/FeeChargeStageIntegrityPolicy.java')),
    'R3-B server-side integrity policy is a prerequisite');
  const previous = JSON.parse(fs.readFileSync(file('tools/fee-r3b/manifest.json'),'utf8'));
  assert(previous.id==='FEE_R3B_STAGE_INTEGRITY','R3-B predecessor not installed');
  const edits=[];
  const states=[];
  for(const entry of manifest.files) {
    assert(entry.path.startsWith('frontend/src/app/features/fee-admin/') ||
      ['tools/verify-fee-r3b.mjs','tools/verify-fee-r3a.mjs','tools/verify-fee-ux-r22.mjs','tools/verify-fee-ux-r23.mjs'].includes(entry.path),
      'forbidden target '+entry.path);
    const before=fs.readFileSync(path.join(assets,'base',entry.asset));
    const after=fs.readFileSync(path.join(assets,'updated',entry.asset));
    assert(hash(before)===entry.before && hash(after)===entry.after,'damaged archive asset '+entry.path);
    const current=read(entry.path);
    assert(current!==null,'missing required file '+entry.path);
    const got=hash(current);
    assert(got===entry.before || got===entry.after,'conflicting concurrent changes in '+entry.path);
    states.push(got===entry.before?'base':'updated');
    edits.push({path:entry.path,original:current,desired:action==='install'?after:before});
  }
  assert(new Set(states).size===1,'partial or mixed R3-B.1 install state');
  // Strictly record that the server guard remains R3-B. Do not modify backend on any action.
  for(const target of previous.files.filter(f=>f.path.startsWith('backend/src/'))) {
    assert(hash(read(target.path))===target.after,'R3-B backend guard has changed: '+target.path);
  }
  return {state:states[0],edits};
}
function atomic(rel,bytes) {
  const target=file(rel); const temp=target+'.r3b1-'+process.pid+'.tmp';
  try {fs.writeFileSync(temp,bytes);fs.renameSync(temp,target);}
  finally {if(fs.existsSync(temp))fs.unlinkSync(temp);}
}
export function run(action,{checkOnly=false}={}) {
  assert(['install','uninstall'].includes(action),'unknown operation');
  const {state,edits}=examine(action);
  const desiredState=action==='install'?'updated':'base';
  if(state===desiredState) {console.log('FEE_R3B1_'+action.toUpperCase()+'_ALREADY_'+state.toUpperCase()+' | no changes');return;}
  if(checkOnly) {console.log('FEE_R3B1_'+action.toUpperCase()+'_PRECHECK_PASS | '+edits.length+' FEE UI/verifier files; no writes');return;}
  const completed=[];
  try {
    for(const edit of edits) {
      const current=read(edit.path);
      assert(current!==null && current.equals(edit.original),'file changed during installation '+edit.path);
      atomic(edit.path,edit.desired);
      completed.push(edit);
    }
  }catch(error){
    for(const edit of completed.reverse()) {
      try {atomic(edit.path,edit.original);}catch(e){console.error('FEE_R3B1_ROLLBACK_FAILED',edit.path,e);}
    }
    throw error;
  }
  console.log('FEE_R3B1_'+action.toUpperCase()+'_PASS | '+completed.length+' targeted changes; backend, Oracle, routes, Product Builder and FEE2 untouched');
}
