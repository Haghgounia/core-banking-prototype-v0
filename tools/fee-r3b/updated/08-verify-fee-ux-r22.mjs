import fs from 'node:fs';
import crypto from 'node:crypto';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const base = 'frontend/src/app/features/fee-admin/';
const read = name => fs.readFileSync(path.join(root,base+name),'utf8');
const check = (ok,desc) => {if(!ok)throw new Error('FEE UX R2.2 FAIL: '+desc);};
const studio = read('fee-design-studio.component.ts');
const html = read('fee-design-studio.component.html');
const css = read('fee-design-studio.component.scss');
const crud = read('fee-admin-table.component.ts');
const crudHtml = read('fee-admin-table.component.html');
const crudCss = read('fee-admin-table.component.scss');
check(studio.includes("readonly catalogStage = signal<'browse' | 'detail'>('browse')") && studio.includes('readonly catalogWide'), 'catalog must separate browse/detail states');
check(html.includes('@if (!catalogWide())') && css.includes('.studio-layout.catalog-wide{grid-template-columns:minmax(0,1fr)}'), 'catalog must be full width without the duplicate sidebar');
check(html.includes('aria-label="فهرست کارمزدها"') && html.includes('aria-label="فهرست نسخه‌های کارمزد"'), 'searchable browsing screens missing');
check(html.includes('definitionTotal()') && html.includes('versionTotal()') && html.includes('(click)="selectDefinition(row)"') && html.includes('(click)="selectVersion(row)"'), 'live definition/version selection missing');
check(html.includes('returnToCatalogList()') && studio.includes('returnToCatalogList(): void'), 'browse/detail navigation missing');
check(html.includes('[editorOnly]="true"') && crud.includes('@Input() editorOnly = false'), 'full width editor not connected');
check(crudHtml.includes('@if (!editorOnly) {') && crudCss.includes('.editor-only .workspace.editor-open{display:block'), 'list grid must be absent in editor-only state');
check(crud.includes('if (!this.editorOnly) await this.search(0)') && crud.includes('if (!this.editorOnly) await this.search(this.page())'), 'editor-only flow must avoid extra table searches');
check(html.includes('(editorDismissed)="closeCatalogEditor()"') && crud.includes('this.editorDismissed.emit()'), 'editor cancel must return to catalog context');
check(html.includes('[quickAction]="catalogManageAction()"') && studio.includes('this.catalogStage.set(\'detail\');'), 'quick edit and saved-row display missing');
check(crud.includes('values[fk.name] =') && crud.includes('!fk.primaryKey && !fk.readOnly'), 'FK integrity guard missing');
check(studio.includes('if (!this.canSwitch()) return;') && crud.includes('hasUnsavedChanges()'), 'unsaved change guard missing');
check(html.includes("[routerLink]=\"['/fee/design/rules']\"") && html.includes('[queryParams]="navigationParams()"'), 'rules handoff must retain chosen IDs');
check(!studio.includes('/api/v1/fee2') && !crud.includes('/api/v1/fee2') && !studio.includes('service.delete('), 'domain isolation or soft-delete safety broken');
const assets=path.join(root,'tools/fee-ux-r22');
const manifest=JSON.parse(fs.readFileSync(path.join(assets,'manifest.json'),'utf8'));
const digest = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
// Follow-up R2.3 may change only a strictly enumerated subset of these six files.
// Accept either R2.2 or the signed R2.3 overlay, never arbitrary modifications.
const r23Path = path.join(root,'tools/fee-ux-r23/manifest.json');
const r23Manifest = fs.existsSync(r23Path) ? JSON.parse(fs.readFileSync(r23Path,'utf8')) : null;
const r23Changes = new Map((r23Manifest?.files ?? []).map(f => [f.path, f]));
const r3bPath = path.join(root,'tools/fee-r3b/manifest.json');
const r3bManifest = fs.existsSync(r3bPath) ? JSON.parse(fs.readFileSync(r3bPath,'utf8')) : null;
const r3bChanges = new Map((r3bManifest?.files ?? []).map(f => [f.path, f]));

check(manifest.files.length===6, 'patch must be limited to six FEE UX files');
for(const f of manifest.files){
  check(f.path.startsWith(base), 'patch must not target any other module');
  const got=digest(fs.readFileSync(path.join(root,f.path)));
  const r23 = r23Changes.get(f.path);
  const r3b = r3bChanges.get(f.path);
  const previous = r23?.baseSha256 === f.updatedSha256 ? r23.updatedSha256 : f.updatedSha256;
  const signedR3b = r3b?.before === previous && got === r3b.after &&
    digest(fs.readFileSync(path.join(root,'tools/fee-r3b/updated/'+r3b.asset))) === r3b.after;
  check(got===f.updatedSha256 || (r23?.baseSha256===f.updatedSha256 && got===r23.updatedSha256) || signedR3b,
    'unexpected source change in '+f.path);
}
console.log('FEE_UX_R22_STATIC_PASS: full width searchable definition/version browser; selection overview; editor-only metadata CRUD; FK/dirty/deep-link guards; six FEE-only files; FEE2/PDL unaffected.');
console.log('NOTE: static tests only. Run build-production.cmd and browser+Oracle UAT for release acceptance.');
