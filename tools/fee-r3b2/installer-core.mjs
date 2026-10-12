import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const dir=path.join(root,'tools/fee-r3b2');
const manifest=JSON.parse(fs.readFileSync(path.join(dir,'manifest.json'),'utf8'));
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
function fail(message){throw new Error('FEE_R3B2_INSTALL_ABORT: '+message+' (no production source overwritten)');}
export function run(action,{checkOnly=false}={}){
  if(!['install','uninstall'].includes(action))fail('invalid action');
  if(manifest.id!=='FEE_R3B2_SAFE_UAT_TESTS'||manifest.files.length!==2)fail('invalid manifest');
  if(!fs.existsSync(path.join(root,'backend/src/main/java/com/behsazan/corebanking/fee/admin/application/FeeChargeStageIntegrityPolicy.java')))
    fail('R3-B predecessor is required');
  if(!fs.existsSync(path.join(root,'tools/fee-r3b1/manifest.json')))
    fail('R3-B.1 predecessor is required');
  const tasks=[];
  for(const entry of manifest.files){
    if(!/^backend\/src\/test\/java\/com\/behsazan\/corebanking\/fee\/admin\/application\/FeeChargeStageR3B2(?:ServiceContract|OracleUat)Test\.java$/.test(entry.path))
      fail('file target outside FEE-only test subtree');
    const bytes=fs.readFileSync(path.join(dir,entry.asset));
    if(sha(bytes)!==entry.sha256)fail('asset altered '+entry.asset);
    const target=path.join(root,entry.path);
    const exists=fs.existsSync(target);
    if(exists && sha(fs.readFileSync(target))!==entry.sha256)fail('existing test collision '+entry.path);
    tasks.push({target,exists,bytes});
  }
  if(checkOnly){console.log('FEE_R3B2_'+action.toUpperCase()+'_PRECHECK_PASS | test-only; no writes');return;}
  const changed=[];
  try {
    if(action==='install'){
      for(const t of tasks) if(!t.exists){
        fs.mkdirSync(path.dirname(t.target),{recursive:true});
        const tmp=t.target+'.r3b2.tmp';
        try {fs.writeFileSync(tmp,t.bytes,{flag:'wx'});fs.renameSync(tmp,t.target);}
        finally {if(fs.existsSync(tmp))fs.unlinkSync(tmp);}
        changed.push(t);
      }
    } else {
      for(const t of tasks) if(t.exists){fs.unlinkSync(t.target);changed.push(t);}
    }
  } catch(error) {
    for(const t of changed.reverse()){
      try {
        if(action==='install' && fs.existsSync(t.target) && sha(fs.readFileSync(t.target))===sha(t.bytes)) fs.unlinkSync(t.target);
        if(action==='uninstall' && !fs.existsSync(t.target)) fs.writeFileSync(t.target,t.bytes,{flag:'wx'});
      } catch(rollbackError) { console.error('FEE_R3B2_TEST_FILE_ROLLBACK_FAILED',t.target,rollbackError.message); }
    }
    throw error;
  }
  console.log('FEE_R3B2_'+action.toUpperCase()+'_PASS | '+changed.length+
    ' test-only files; no changes to product code, routes, backend production, FEE2 or Oracle');
}
