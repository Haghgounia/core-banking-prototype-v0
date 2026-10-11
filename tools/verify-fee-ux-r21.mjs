import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');
const check = (value, what) => { if (!value) throw new Error(`FEE UX R2.1 FAIL: ${what}`); };
const base = 'frontend/src/app/features/fee-admin/';
const studio = read(base + 'fee-design-studio.component.ts');
const template = read(base + 'fee-design-studio.component.html');
const styles = read(base + 'fee-design-studio.component.scss');
const crud = read(base + 'fee-admin-table.component.ts');
const routes = read('frontend/src/app/app.routes.ts');
const versionDdl = read('database/oracle/fee/baseline-1.0/ddl/FEE.FEE_DEFINITION_VERSION_20260829_142423_665.oracle.sql');
const definitionDdl = read('database/oracle/fee/baseline-1.0/ddl/FEE.FEE_DEFINITION_20260829_142423_665.oracle.sql');

check(template.includes("mode==='catalog' && (activeTab()==='definition' || activeTab()==='version')"), 'catalog must use a non-duplicating overview');
check(template.includes('selection-overview') && template.includes('catalog-onboarding'), 'selected definition/version summaries and empty states missing');
check(template.includes('catalogEditorContext()') && template.includes('[quickAction]="catalogManageAction()"'), 'on-demand contextual CRUD is not connected');
check(studio.includes('readonly catalogManageAction') && studio.includes('openCatalogEditor(action:'), 'catalog editor must be opt-in');
check(crud.includes("@Input() quickAction: 'create' | 'edit'") && crud.includes("if (this.quickAction === 'create') await this.create()"), 'one-click new record launch missing');
check(crud.includes("this.quickAction === 'edit' && this.quickRow") && crud.includes('await this.open(this.quickRow)'), 'one-click edit launch missing');
check(studio.includes("filterColumn:'FEE_DEFINITION_ID'") && studio.includes("tableName:'FEE_DEFINITION_VERSION'"), 'version must remain scoped to selected definition');
check(crud.includes('!fk.primaryKey && !fk.readOnly'), 'PK must never be rewritten by contextual filter');
check(template.includes("[class.active]=\"definitionId() === asId(row,'FEE_DEFINITION_ID')\"") && template.includes('selector-status'), 'selection must be visually evident');
check(studio.includes('definitionOutsidePage') && studio.includes('versionOutsidePage') && template.includes('selection-pinned'), 'selected objects must remain visible after search/pagination');
check(template.includes('(click)="focusDefinitionSearch()"') && template.includes("{definitionId:definitionId(),tab:'version'}"), 'direct-rule empty-state recovery missing');
check(studio.includes("this.route.snapshot.queryParamMap.get('tab')") && studio.includes('void this.syncUrl()'), 'tab deep links and context persistence missing');
check(studio.includes('if (!this.canSwitch()) return;') && crud.includes('hasUnsavedChanges()'), 'unsaved edit guard absent');
check(template.includes('[routerLink]="[\'/fee/design/rules\']"') && template.includes('[queryParams]="navigationParams()"'), 'definition/version navigation must pass context');
check(styles.includes('.selection-overview') && styles.includes('-webkit-line-clamp:2'), 'compact detail panel and long-text clamping missing');
for (const column of ['FEE_DEFINITION_ID','FEE_CODE','NAME_FA','CATEGORY_CODE','CLASSIFICATION_CODE','IS_ACTIVE']) {
  check(definitionDdl.includes('  '+column+' '), `definition view uses missing field ${column}`);
}
for (const column of ['FEE_DEFINITION_VERSION_ID','FEE_DEFINITION_ID','POLICY_VERSION_ID','VERSION_NO','STATUS_CODE','EFFECTIVE_FROM','EFFECTIVE_TO']) {
  check(versionDdl.includes('  '+column+' '), `version view uses missing field ${column}`);
}
check(routes.includes("path: 'fee/design/catalog'") && routes.includes("path: 'fee/design/rules'"), 'FEE design routes changed');
check(routes.includes("path: 'fee2/studio'"), 'FEE2 route removed');
check(!studio.includes('/api/v1/fee2') && !crud.includes('/api/v1/fee2'), 'FEE2 dependency forbidden');
console.log('FEE_UX_R21_STATIC_PASS: one canonical selector, context-safe definition/version summaries, on-demand CRUD, quick-create/edit, deep-linked selection, guarded dirty edits, FEE/FEE2 isolated.');
console.log('NOTE: Static source verification only. Run build-production.cmd and browser/Oracle UAT on the deployment machine.');
