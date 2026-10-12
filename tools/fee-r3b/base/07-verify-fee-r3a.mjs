import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const digest=rel=>crypto.createHash('sha256').update(fs.readFileSync(path.join(root,rel))).digest('hex');
const check=(ok,why)=>{if(!ok)throw new Error('FEE_R3A_VERIFY_FAIL: '+why);};
const dir='frontend/src/app/features/fee-admin/';
const component=read(dir+'fee-collection-studio.component.ts');
const template=read(dir+'fee-collection-studio.component.html');
const styles=read(dir+'fee-collection-studio.component.scss');
const workspace=read(dir+'fee-admin-workspace.component.ts');
const workHtml=read(dir+'fee-admin-workspace.component.html');
const routes=read('frontend/src/app/app.routes.ts');
const backend=read('backend/src/main/java/com/behsazan/corebanking/fee/admin/oracle/FeeAdminRepository.java');
const manifest=JSON.parse(read('tools/fee-r3a/manifest.json'));
check(manifest.id==='FEE_R3A_COLLECTION_STUDIO','manifest id');
for(const entry of manifest.files){
  check(entry.path.startsWith(dir),'must not overwrite other module');
  check(digest(entry.path)===entry.after,'changed FEE source mismatch '+entry.path);
  check(digest('tools/fee-r3a/base/'+entry.name)===entry.before,'rollback source mismatch '+entry.name);
  check(digest('tools/fee-r3a/updated/'+entry.name)===entry.after,'new patch payload mismatch '+entry.name);
}
for(const entry of manifest.newFiles){
  check(entry.path.startsWith(dir),'new file outside FEE');
  check(digest(entry.path)===entry.sha256 && digest('tools/fee-r3a/updated/'+entry.name)===entry.sha256,
    'new component mismatch '+entry.name);
}
const tables=['FEE_CHARGE_STAGE','FEE_TIMING_RULE','FEE_CURRENCY_RULE','FEE_COLLECTION_RULE','FEE_POSTING_RULE'];
for(const name of tables){
  check(component.includes("table:'"+name+"'"),'missing business tab for '+name);
  const files=fs.readdirSync(path.join(root,'database/oracle/fee/baseline-1.0/ddl'));
  const sqlFile=files.find(f=>f.startsWith('FEE.'+name+'_') && f.endsWith('.oracle.sql'));
  check(!!sqlFile,'missing authoritative baseline DDL '+name);
  const sql=read('database/oracle/fee/baseline-1.0/ddl/'+sqlFile);
  check(sql.includes('FOREIGN KEY (FEE_DEFINITION_VERSION_ID) REFERENCES FEE.FEE_DEFINITION_VERSION'),
    'version FK missing '+name);
}
const stageSql=read('database/oracle/fee/baseline-1.0/ddl/'+fs.readdirSync(path.join(root,'database/oracle/fee/baseline-1.0/ddl')).find(f=>f.startsWith('FEE.FEE_CHARGE_STAGE_')));
check(stageSql.includes('FOREIGN KEY (CALCULATION_RULE_ID) REFERENCES FEE.FEE_CALCULATION_RULE'),
  'stage calculation FK missing');
check(component.includes("editableInStudio:false") && template.includes("tab().key==='stages'") &&
  template.includes('تا زمانی که تطابق نسخه قاعده محاسبه'),
  'stage must be read-only in focused R3-A until cross-version check exists');
check(component.includes("filterColumn:'FEE_DEFINITION_VERSION_ID'") && component.includes("filterValue:version"),
  'all five tab reads must use the selected version filter');
check(component.includes("this.rowId(r,'FEE_DEFINITION_ID')!==definition") &&
  component.includes("this.rowId(r,'FEE_DEFINITION_VERSION_ID')!==version"),
  'unexpected cross-definition/version rows must be rejected');
check(component.includes("this.rowId(row,'FEE_DEFINITION_ID')!==definition") &&
  component.includes("this.rowId(row,'FEE_DEFINITION_VERSION_ID')!==this.versionId()"),
  'picked row or deep link must belong to selection');
check(template.includes('[editorOnly]="true"') && template.includes('[context]="ctx"') &&
  template.includes('[quickAction]="editorMode()"') &&
  template.includes('(rowWritten)="rowWritten($event)"'),
  'contextual metadata CRUD must use on-demand full-width editor');
check(component.includes('this.editor?.hasUnsavedChanges()') &&
  routes.includes("canDeactivate: [(c: FeeAdminWorkspaceComponent) => c.canLeave()]") &&
  workspace.includes('this.collectionStudio?.canLeave() ?? true'),
  'unsaved-change route guard missing');
check(routes.includes("path: 'fee/design/catalog'") && routes.includes("path: 'fee/design/rules'") &&
  routes.includes("path: 'fee2/studio'"), 'existing routes were disturbed');
check(workHtml.includes("@if (work.key === 'collection')") && workHtml.includes('<app-fee-collection-studio />') &&
  workHtml.includes("@else {\n    <section class=\"work-steps\""),
  'collection must use focused studio instead of five technical cards');
check(backend.includes('filterValue') && backend.includes('where.add("T." + identifier(column.name()) + " = :filterValue")'),
  'backend does not support scoped FEE filtering');
check(!component.includes('/api/v1/fee2') && !component.includes('/api/v1/product') &&
  !template.includes('app-fee2') && !routes.includes('fee2/studio-not-found'),
  'FEE2/Product Builder independence');
check(styles.includes('.collection-tabs') && styles.includes('.context-options'),'responsive styles missing');
console.log('FEE_R3A_STATIC_PASS');
console.log('Five version FK scopes, 4 controlled CRUD tabs, charge-stage review guard, dirty-route guard, existing FEE2/PDL routes preserved.');
console.log('Static checks only; run build-production.cmd and browser/Oracle UAT.');
