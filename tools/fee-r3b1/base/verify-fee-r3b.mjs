import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import crypto from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const fail=why=>{throw new Error('FEE_R3B_VERIFY_FAIL: '+why);};
const check=(yes,why)=>{if(!yes)fail(why);};
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const hash=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
const sha=rel=>hash(fs.readFileSync(path.join(root,rel)));
const base='frontend/src/app/features/fee-admin/';
const backend='backend/src/main/java/com/behsazan/corebanking/fee/admin/';
const manifest=JSON.parse(read('tools/fee-r3b/manifest.json'));
check(manifest.id==='FEE_R3B_STAGE_INTEGRITY','patch identity');
check(manifest.files.length===10 && manifest.newFiles.length===2,'manifest targets must be complete');
for(const entry of manifest.files){
  check(entry.path.startsWith(base) || entry.path.startsWith(backend) ||
    ['tools/verify-fee-r3a.mjs','tools/verify-fee-ux-r22.mjs','tools/verify-fee-ux-r23.mjs'].includes(entry.path),
    'forbidden module target '+entry.path);
  check(sha(entry.path)===entry.after && sha('tools/fee-r3b/updated/'+entry.asset)===entry.after &&
    sha('tools/fee-r3b/base/'+entry.asset)===entry.before,'altered signed patch or live source '+entry.path);
}
for(const entry of manifest.newFiles){
  check(entry.path.startsWith(backend) || entry.path.startsWith('backend/src/test/java/com/behsazan/corebanking/fee/'),
    'new file outside FEE: '+entry.path);
  check(sha(entry.path)===entry.sha256 && sha('tools/fee-r3b/updated/'+entry.asset)===entry.sha256,
    'new FEE file mismatch '+entry.path);
}
const policy=read(backend+'application/FeeChargeStageIntegrityPolicy.java');
const service=read(backend+'application/FeeAdminService.java');
const repo=read(backend+'oracle/FeeAdminRepository.java');
const ctrl=read(backend+'web/FeeAdminController.java');
const component=read(base+'fee-collection-studio.component.ts');
const html=read(base+'fee-collection-studio.component.html');
const editor=read(base+'fee-admin-table.component.ts');
const frontendService=read(base+'fee-admin.service.ts');
check(service.includes('if (isStage(table)) validateStage(values, null);') &&
  service.includes('repository.lockChargeStage(id)') &&
  service.includes('validateStage(values, persisted);') &&
  service.includes('FeeChargeStageIntegrityPolicy.verifyOwners'),
  'the shared generic create/update API must guard stage links');
check(service.includes('validateParentReassignment') && repo.includes('stageReferencesParent'),
  'parent version mutation must not break existing stages');
check(repo.includes('FOR UPDATE') && repo.includes('lockStageParentVersion') && repo.includes('lockChargeStage') &&
  repo.includes('T.FEE_DEFINITION_VERSION_ID = :versionId'),
  'locked parent and scoped lookup queries are required');
check(ctrl.includes('@RequestParam(required = false) Long versionId') &&
  frontendService.includes("params = params.set('versionId', versionId)"),
  'version-scoped lookup contract missing');
check(editor.includes("this.tableName() === 'FEE_CHARGE_STAGE'") &&
  editor.includes("column.name === 'FEE_DEFINITION_VERSION_ID'") &&
  editor.includes("'CALCULATION_RULE_ID', 'CHARGE_COMPONENT_ID'") &&
  editor.includes('stageDependency ? stageVersion! : undefined') &&
  editor.includes('serial === this.lookupSerial'),
  'dependent FK dropdowns must be scoped and reloaded safely');
check(component.includes("table:'FEE_CHARGE_STAGE'") && component.includes('editableInStudio:true') &&
  component.includes("this.service.lookup('FEE_CHARGE_STAGE'") &&
  html.includes('کنترل یکپارچگی مرحله اخذ فعال است') &&
  html.includes('افزودن مرحله'),
  'R3-B stage create/edit must be context-bound and report guard status');
check(policy.includes('longValueExact') && policy.includes('verifyParentMove') &&
  policy.includes('verifyOwners'), 'strict positive IDs and owner checks missing');
const ddlDir='database/oracle/fee/baseline-1.0/ddl/';
const stageDdl=read(ddlDir+fs.readdirSync(path.join(root,ddlDir)).find(f=>f.startsWith('FEE.FEE_CHARGE_STAGE_')));
check(stageDdl.includes('CALCULATION_RULE_ID NUMBER(19,0) NOT NULL') &&
  stageDdl.includes('CHARGE_COMPONENT_ID NUMBER(19,0)') &&
  stageDdl.includes('FOREIGN KEY (CALCULATION_RULE_ID) REFERENCES FEE.FEE_CALCULATION_RULE'),
  'the FEE-only Oracle stage contract changed');
check(!service.includes('com.behsazan.corebanking.fee2') &&
  !service.includes('com.behsazan.corebanking.productbuilder') &&
  !component.includes('/api/v1/fee2'), 'forbidden cross-domain dependency');
console.log('FEE_R3B_STATIC_PASS | 10 signed modifications, 2 new FEE files, shared write guard, version-scoped lookup, no cross-domain changes.');
const temp=fs.mkdtempSync(path.join(os.tmpdir(),'fee-r3b-policy-'));
try{
  const sources=[
    backend+'application/FeeAdminValidationException.java',
    backend+'application/FeeChargeStageIntegrityPolicy.java',
    'tools/fee-r3b/StandalonePolicyCheck.java'
  ].map(rel=>path.join(root,rel));
  const javac=spawnSync('javac',['-encoding','UTF-8','-d',temp,...sources],{encoding:'utf8'});
  if(javac.error?.code==='ENOENT')console.log('FEE_R3B_POLICY_RUNTIME_SKIPPED | javac unavailable; full build must run on Windows.');
  else {
    check(javac.status===0,'policy javac failed: '+javac.stderr);
    const result=spawnSync('java',['-cp',temp,'com.behsazan.corebanking.fee.admin.application.StandalonePolicyCheck'],{encoding:'utf8'});
    check(result.status===0 && result.stdout.includes('FEE_R3B_POLICY_RUNTIME_PASS'),
      'policy behavioral test failed: '+result.stderr+' '+result.stdout);
    console.log(result.stdout.trim());
  }
}finally{fs.rmSync(temp,{recursive:true,force:true});}
console.log('R3-B is not a substitute for full Angular/Java build and Oracle integration UAT.');
