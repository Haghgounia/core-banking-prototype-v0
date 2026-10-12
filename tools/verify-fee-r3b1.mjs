import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const sha=rel=>crypto.createHash('sha256').update(fs.readFileSync(path.join(root,rel))).digest('hex');
const check=(condition,reason)=>{if(!condition)throw Error('FEE_R3B1_VERIFY_FAIL: '+reason);};
const rootSrc='frontend/src/app/features/fee-admin/';
const manifest=JSON.parse(read('tools/fee-r3b1/manifest.json'));
check(manifest.id==='FEE_R3B1_COMPONENT_EMPTY_STATE'&&manifest.files.length===7,'signed manifest scope');
for(const entry of manifest.files){
  check(entry.path.startsWith(rootSrc) || entry.path.startsWith('tools/verify-fee-'), 'module isolation '+entry.path);
  check(sha(entry.path)===entry.after && sha('tools/fee-r3b1/base/'+entry.asset)===entry.before &&
    sha('tools/fee-r3b1/updated/'+entry.asset)===entry.after,'changed source or broken signed assets '+entry.path);
}
const ts=read(rootSrc+'fee-admin-table.component.ts');
const html=read(rootSrc+'fee-admin-table.component.html');
const studio=read(rootSrc+'fee-collection-studio.component.html');
const backend=read('backend/src/main/java/com/behsazan/corebanking/fee/admin/application/FeeAdminService.java');
const repo=read('backend/src/main/java/com/behsazan/corebanking/fee/admin/oracle/FeeAdminRepository.java');
const ddl=read('database/oracle/fee/baseline-1.0/ddl/'+fs.readdirSync(path.join(root,'database/oracle/fee/baseline-1.0/ddl')).find(x=>x.startsWith('FEE.FEE_CHARGE_STAGE_')));
check(ddl.includes('CHARGE_COMPONENT_ID NUMBER(19,0),') && ddl.includes('CALCULATION_RULE_ID NUMBER(19,0) NOT NULL'),
  'optional component / required calculation rule model');
check(ts.includes("column.name === 'CHARGE_COMPONENT_ID'") && ts.includes('this.editorOnly') &&
  ts.includes("column.name === 'CALCULATION_RULE_ID') return 'قاعده محاسبه'") &&
  ts.includes("return 'مؤلفه کارمزد'"),'new labels must be only in scoped business editor');
check(html.includes('بدون مؤلفه') && html.includes('lookupsLoading()') &&
  html.includes("lookupFailures().includes('CHARGE_COMPONENT_ID')") &&
  html.includes('مؤلفه‌ای در فهرست یافت نشد') &&
  html.includes("tab: 'charge'"),'three-state optional component UI / designer handoff');
check(ts.includes('failed.push(column.name)') && ts.includes('this.lookupFailures.set(failed)') &&
  ts.includes('this.lookupsLoading.set(false)') &&
  ts.includes("stageDependency ? stageVersion! : undefined"),
  'lookup error must not be confused with empty successful response; version filtering retained');
check(studio.includes('تعریف مرحله اخذ جدید') && studio.includes('ویرایش مرحله اخذ') &&
  studio.includes('[relatedDefinitionId]="definitionId()"'),'stage title + version-preserving link');
check(backend.includes('FeeChargeStageIntegrityPolicy.verifyOwners') && repo.includes('T.FEE_DEFINITION_VERSION_ID = :versionId'),
  'critical R3-B server checks retained');
check(!ts.includes('/api/v1/fee2')&&!ts.includes('/api/v1/productbuilder')&&!studio.includes('/api/v1/fee2'),
  'FEE2 / Product Builder isolation');
console.log('FEE_R3B1_STATIC_PASS | stage title, scoped FK labels, null option, empty/loading/error states, secure R3-B backend guard retained.');
console.log('Build, browser verification, live Oracle lookup/save tests still required.');
