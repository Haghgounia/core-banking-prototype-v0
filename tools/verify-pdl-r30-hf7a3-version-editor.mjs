import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import {createRequire} from 'node:module';
import {spawnSync} from 'node:child_process';
const root = path.resolve(import.meta.dirname, '..');
const require = createRequire(import.meta.url);
let ts = null;
const globalNpm = spawnSync('npm', ['root', '-g'], {encoding: 'utf8', timeout: 8000});
for (const candidate of [path.join(root, 'frontend/node_modules/typescript'), path.join(root, 'node_modules/typescript'), ...(globalNpm.status===0 ? [path.join(globalNpm.stdout.trim(), 'typescript')] : [])]) {
  try { ts = require(candidate); break; } catch { /* fresh source without npm dependencies */ }
}
const view = fs.readFileSync(path.join(root, 'frontend/src/app/features/product-builder/product-workspace.component.html'), 'utf8');
const source = fs.readFileSync(path.join(root, 'frontend/src/app/features/product-builder/product-workspace.component.ts'), 'utf8');
const styles = fs.readFileSync(path.join(root, 'frontend/src/app/features/product-builder/product-workspace.component.scss'), 'utf8');
const gov = fs.readFileSync(path.join(root, 'backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductRuleGovernanceService.java'), 'utf8');
let passed = 0;
let failed = 0;
function check(name, actual) {if(actual){console.log('PASS | '+name);passed++;}else{console.error('FAIL | '+name);failed++;}}
check('modal owns an accessible failure message',view.includes('class="version-save-error" role="alert"')&&view.includes('{{ versionSaveError() }}'));
check('form retains editor and selection after governance denial', source.includes('this.versionSaveError.set(') && /if \(\(opening \|\| approvalWithOpen\) && !readiness\.canOpen\) \{[\s\S]*?return;\s*\}/.test(source));
check('design-only action explicitly closes origination and servicing',view.includes('(click)="closeOriginationForDesign()"')&&source.includes("this.versionForm.controls.SERVICING_STATUS_CODE.setValue('CLOSED')"));
check('runtime warning is shown for new origination or active servicing',view.includes("versionForm.controls.ORIGINATION_STATUS_CODE.value === 'OPEN'")&&view.includes("versionForm.controls.SERVICING_STATUS_CODE.value === 'ACTIVE'"));
check('approved, rejected, expired are not create-new defaults',view.includes('value="APPROVED" [disabled]="editingVersionId() === null"'));
check('business source retains fail-closed loan policy',gov.includes('if ("LOAN".equals(domain)) {')&&gov.includes('canOpen = false;'));
check('CSS keeps readable modal guidance',styles.includes('.version-save-error')&&styles.includes('.version-operational-warning'));
if (!ts) {
  console.log('SKIP | dynamic TypeScript checks (npm dependencies not installed yet)');
  console.log('PDL_R30_HF7A3_VERSION_EDITOR_STATIC_PASS='+passed+' FAIL='+failed);
  process.exit(failed ? 1 : 0);
}
const result = ts.transpileModule(source,{fileName:'product-workspace.component.ts',reportDiagnostics:true,compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}});
check('full Product Workspace TypeScript parser',!(result.diagnostics?.length));
const src = ts.createSourceFile('component.ts', source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS);
const clazz=src.statements.find(statement=>ts.isClassDeclaration(statement)&&statement.name?.text==='ProductWorkspaceComponent');
if (!clazz) throw new Error('ProductWorkspaceComponent class not found');
const methods=['saveVersion','closeOriginationForDesign'].map(name=>{
  const method=clazz.members.find(member=>ts.isMethodDeclaration(member)&&member.name.getText(src)===name);
  if (!method) throw new Error('Expected '+name+' method missing');
  return method.getText(src);
});
const wrapper='class VersionEditorFixture {'+methods.join('\n')+'}\nmodule.exports = VersionEditorFixture;';
const js=ts.transpileModule(wrapper,{fileName:'version-save.ts',compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;
const context={module:{exports:{}},exports:{},console};
vm.runInNewContext(js,context);
const Editor=context.module.exports;
function sig(value) {const f=()=>value;f.set=(v)=>{value=v;};return f;}
function scenario({previousStatus='DRAFT', previousOrig='CLOSED',targetStatus='APPROVED',targetOrig='OPEN',targetServ='ACTIVE',canApprove=true,canOpen=false,editing=17,throws=false,confirm=true}={}) {
 const saved={PRODUCT_VERSION_ID:17,PRODUCT_ID:7,VERSION_STATUS_CODE:previousStatus,ORIGINATION_STATUS_CODE:previousOrig,SERVICING_STATUS_CODE:'CLOSED'};
 const draft={PRODUCT_ID:7,PRODUCT_VERSION_ID:17,VERSION_NO:1,VALID_FROM:'2026-10-14',VALID_TO:null,VERSION_STATUS_CODE:targetStatus,ORIGINATION_STATUS_CODE:targetOrig,SERVICING_STATUS_CODE:targetServ,CHANGE_REASON:null,IS_CURRENT:false};
 const fake=new Editor();const calls=[];
 fake.productId=sig(7);fake.editingVersionId=sig(editing);fake.selectedVersionId=sig(17);
 fake.versions=sig([saved]);fake.versionEditorOpen=sig(true);fake.currentStep=sig(2);
 fake.versionSaveError=sig(null);fake.error=sig(null);fake.saving=sig(false);
 fake.productNotice=sig(null);fake.governanceReport=sig(null);
 fake.versionForm={invalid:false, markAllAsTouched(){}, getRawValue(){return {...draft}},controls:{
  ORIGINATION_STATUS_CODE:{setValue(v){draft.ORIGINATION_STATUS_CODE=v}},
  SERVICING_STATUS_CODE:{setValue(v){draft.SERVICING_STATUS_CODE=v}}}};
 fake.isDeposit=sig(false);
 fake.message=e=>e.message;
 fake.service={
  async ruleGovernance(id){calls.push(['governance',id]);return {canApprove,canOpen,blockers:['LOAN_POLICY_NOT_CONNECTED_TO_RUNTIME']};},
  async update(table,id,payload){calls.push(['update',table,id,payload]);if(throws)throw Error('API rejected write');return {...saved,...payload,PRODUCT_VERSION_ID:id};},
  async create(table,payload){calls.push(['create',table,payload]);if(throws)throw Error('API rejected write');return {...payload,PRODUCT_VERSION_ID:19};}}
 fake.load=async()=>{calls.push(['load']);if(confirm){const op=calls.find(x=>x[0]==='update'||x[0]==='create');if(op){const payload=op.at(-1);fake.versions.set([{...saved,...payload,PRODUCT_VERSION_ID:editing===null?19:editing}]);}}};
 return {fake,calls,draft};
}
{
 const x=scenario();await x.fake.saveVersion();
 check('loan APPROVED + OPEN denied without writing to server',x.calls.every(c=>!['update','create'].includes(c[0])));
 check('denial preserves editor, selected statuses and current step',x.fake.versionEditorOpen()===true&&x.fake.currentStep()===2&&x.draft.ORIGINATION_STATUS_CODE==='OPEN'&&x.fake.versionSaveError()?.length>0);
 x.fake.closeOriginationForDesign();
 check('explicit action adjusts both operational statuses',x.draft.ORIGINATION_STATUS_CODE==='CLOSED'&&x.draft.SERVICING_STATUS_CODE==='CLOSED'&&x.fake.versionSaveError()===null);
 await x.fake.saveVersion();
 check('design approval persists while canOpen is still false',x.calls.some(c=>c[0]==='update')&&x.fake.versionEditorOpen()===false&&x.fake.productNotice()?.length>0);
}
{
 const x=scenario({canApprove:false,targetOrig:'CLOSED',targetServ:'CLOSED'});await x.fake.saveVersion();
 check('missing design rules still block approval',x.calls.every(c=>c[0]!=='update')&&!!x.fake.versionSaveError());
}
{
 const x=scenario({targetStatus:'DRAFT',targetOrig:'OPEN'});await x.fake.saveVersion();
 check('opening is denied even without design approval',x.calls.every(c=>c[0]!=='update')&&!!x.fake.versionSaveError());
}
{
 const x=scenario({targetStatus:'APPROVED',targetOrig:'CLOSED',targetServ:'CLOSED',throws:true});await x.fake.saveVersion();
 check('backend write failures stay inside editor',x.calls.some(c=>c[0]==='update')&&x.fake.versionEditorOpen()&&x.fake.versionSaveError()?.includes('API rejected write'));
}
{
 const x=scenario({editing:null,targetStatus:'APPROVED',targetOrig:'CLOSED'});await x.fake.saveVersion();
 check('new versions cannot skip DRAFT',x.calls.every(c=>c[0]!=='create')&&!!x.fake.versionSaveError());
}
{
 const x=scenario({editing:null,targetStatus:'DRAFT',targetOrig:'CLOSED',targetServ:'CLOSED'});await x.fake.saveVersion();
 check('new DRAFT can be created without canOpen',x.calls.some(c=>c[0]==='create')&&x.fake.versionEditorOpen()===false);
}
{
 const x=scenario({confirm:false,targetStatus:'APPROVED',targetOrig:'CLOSED',targetServ:'CLOSED'});await x.fake.saveVersion();
 check('missing refreshed version never reports success',x.fake.versionSaveError()?.length>0&&x.fake.versionEditorOpen()===true);
}
console.log('PDL_R30_HF7A3_VERSION_EDITOR_PASS='+passed+' FAIL='+failed);
if(failed) process.exit(1);
