import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const manifest=JSON.parse(fs.readFileSync(path.join(root,'tools/fee-r3a/manifest.json'),'utf8'));
const assetDir=path.join(root,'tools/fee-r3a');
const routesFile='frontend/src/app/app.routes.ts';
const designImport="import type {FeeDesignStudioComponent} from './features/fee-admin/fee-design-studio.component';";
const workspaceImport="import type {FeeAdminWorkspaceComponent} from './features/fee-admin/fee-admin-workspace.component';";
const oldRoute="{path: 'fee/workspaces/:workspace', loadComponent: () => import('./features/fee-admin/fee-admin-workspace.component').then(m => m.FeeAdminWorkspaceComponent)},";
const newRoute="{path: 'fee/workspaces/:workspace', canDeactivate: [(c: FeeAdminWorkspaceComponent) => c.canLeave()], loadComponent: () => import('./features/fee-admin/fee-admin-workspace.component').then(m => m.FeeAdminWorkspaceComponent)},";
const fail=(what)=>{throw new Error(`FEE_R3A_INSTALL_ABORT: ${what}; no project files should be overwritten by a conflicting release.`);};
const hash=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
const file=rel=>path.join(root,rel);
const read=rel=>fs.existsSync(file(rel))?fs.readFileSync(file(rel)):null;
const asset=(side,name)=>fs.readFileSync(path.join(assetDir,side,name));
const assert=(yes,msg)=>{if(!yes)fail(msg);};

// Do not package the shared app.routes.ts file. Transform only the exact FEE workspace route.
function transformRoutes(source,direction){
  let text=source.toString('utf8');
  if(direction==='install'){
    assert(text.includes(designImport),'FEE routes import anchor missing');
    assert(text.split(oldRoute).length===2,'FEE workspace route signature changed');
    assert(!text.includes(newRoute) && !text.includes(workspaceImport),'already modified or conflicting FEE route');
    text=text.replace(designImport,designImport+'\n'+workspaceImport).replace(oldRoute,newRoute);
  }else{
    assert(text.includes(designImport),'FEE routes import anchor missing');
    assert(text.split(newRoute).length===2,'guarded FEE workspace route signature changed');
    assert(text.split(workspaceImport).length===2,'FEE workspace type import missing');
    text=text.replace(newRoute,oldRoute).replace(workspaceImport+'\n','');
  }
  return Buffer.from(text,'utf8');
}
function analyze(mode){
  const isInstall=mode==='install';
  const all=[];
  for(const entry of manifest.files){
    assert(entry.path.startsWith('frontend/src/app/features/fee-admin/'),'unauthorized target '+entry.path);
    const before=asset('base',entry.name);const after=asset('updated',entry.name);
    assert(hash(before)===entry.before && hash(after)===entry.after,'payload hash mismatch '+entry.name);
    const current=read(entry.path);
    assert(current!==null,'missing FEE workspace source '+entry.path);
    const h=hash(current);
    assert(h===entry.before || h===entry.after,'FEE workspace source changed concurrently: '+entry.path);
    all.push({rel:entry.path,original:current,newBytes:isInstall?after:before,phase:h===entry.before?'base':'updated'});
  }
  for(const entry of manifest.newFiles){
    assert(entry.path.startsWith('frontend/src/app/features/fee-admin/'),'unauthorized new component '+entry.path);
    const bytes=asset('updated',entry.name);
    assert(hash(bytes)===entry.sha256,'new component payload mismatch '+entry.name);
    const current=read(entry.path);
    assert(current===null || hash(current)===entry.sha256,'new component path collision '+entry.path);
    all.push({rel:entry.path,original:current,newBytes:isInstall?bytes:null,phase:current===null?'base':'updated'});
  }
  const currentRoute=read(routesFile);
  assert(currentRoute!==null,'app.routes.ts missing');
  const code=currentRoute.toString('utf8');
  const routePhase=code.includes(newRoute)&&code.includes(workspaceImport)&&!code.includes(oldRoute)?'updated':
    code.includes(oldRoute)&&!code.includes(newRoute)&&!code.includes(workspaceImport)?'base':null;
  assert(routePhase!==null,'FEE workspace route changed concurrently');
  // R3 uses R2.3 independent metadata editor; require that API contract to exist.
  const crud=read('frontend/src/app/features/fee-admin/fee-admin-table.component.ts')?.toString('utf8')??'';
  assert(crud.includes('@Input() editorOnly = false') && crud.includes('hasUnsavedChanges()') && crud.includes('isFeeFeatureSelector'),
    'FEE R2.3 prerequisite missing; first install R2.2 and R2.3');
  const phaseSet=new Set([...all.map(x=>x.phase),routePhase]);
  assert(phaseSet.size===1,'incomplete previous installation or mixed-version files');
  const phase=[...phaseSet][0];
  const changes=all.map(x=>({rel:x.rel,original:x.original,newBytes:x.newBytes}));
  changes.push({rel:routesFile,original:currentRoute,newBytes:routePhase===(isInstall?'updated':'base')?currentRoute:transformRoutes(currentRoute,isInstall?'install':'uninstall')});
  return {phase,changes};
}
function atomic(rel,bytes){
  const dest=file(rel);
  if(bytes===null){if(fs.existsSync(dest))fs.unlinkSync(dest);return;}
  fs.mkdirSync(path.dirname(dest),{recursive:true});
  const tmp=dest+'.fee-r3a-'+process.pid+'.tmp';
  try{fs.writeFileSync(tmp,bytes);fs.renameSync(tmp,dest);}finally{if(fs.existsSync(tmp))fs.unlinkSync(tmp);}
}
export function run(mode,{checkOnly=false}={}){
  assert(mode==='install'||mode==='uninstall','bad mode');
  const analysis=analyze(mode);const desired=mode==='install'?'updated':'base';
  if(analysis.phase===desired){
    console.log('FEE_R3A_'+mode.toUpperCase()+'_ALREADY_'+desired.toUpperCase()+' | no files changed');
    return;
  }
  if(checkOnly){
    console.log('FEE_R3A_'+mode.toUpperCase()+'_PRECHECK_PASS | only FEE workspace and its route anchor affected | no writes');
    return;
  }
  const completed=[];
  try{
    for(const change of analysis.changes){
      const now=read(change.rel);
      assert((now===null&&change.original===null)||(now!==null&&change.original!==null&&now.equals(change.original)),
        'source changed during installation '+change.rel);
      atomic(change.rel,change.newBytes);completed.push(change);
    }
  }catch(e){
    for(const change of completed.reverse()){
      try{atomic(change.rel,change.original);}catch(rollbackError){console.error('FEE_R3A_RESTORE_ERROR',change.rel,rollbackError);}
    }
    throw e;
  }
  console.log('FEE_R3A_'+mode.toUpperCase()+'_PASS | '+completed.length+' scoped changes; FEE2 and Product Builder untouched');
}
