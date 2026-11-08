// System UI V1: deterministic regression gate. Read-only; does not inspect Oracle.
'use strict';
const fs=require('node:fs');const path=require('node:path');
const root=path.resolve(__dirname,'../..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const screenDir=path.join(root,'frontend/src/app/features');
function walk(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name)):[path.join(dir,e.name)]);}
const templates=walk(screenDir).filter(p=>p.endsWith('.component.html'));
const tests=[];const expect=(name,ok)=>tests.push({name,ok:Boolean(ok)});
expect('86 feature screens (source 1219 baseline)',templates.length===86);
expect('one global AppShell template',fs.existsSync(path.join(root,'frontend/src/app/layout/app-shell.component.html')));
const badRoot=[];
for(const file of templates){
 const s=fs.readFileSync(file,'utf8');
 if(!/<(?:section|main|div)\b[^>]*(?:class\.ui-standard-v1|\bui-standard-v1\b)/s.test(s)) badRoot.push(path.relative(root,file));
}
expect('all feature templates use standard scope',badRoot.length===0);
const style=read('frontend/src/styles.scss');
expect('global responsive token style installed',style.includes('SYSTEM UI STANDARD v1') && style.includes('.ui-standard-v1'));
expect('theme light/dark retained',style.includes('html.theme-dark') && style.includes('html.theme-light'));
expect('keyboard focus contract',style.includes(':focus-visible') && style.includes('outline: 2px solid var(--app-primary)'));
expect('reduced-motion contract',style.includes('prefers-reduced-motion'));
expect('no blanket forced CRUD actions',!style.includes('display: block !important; /* force-delete') );
const reference=read('frontend/src/app/features/reference-data/presentation/reference-page.component.html');
expect('reference list keeps Material sortable table and pagination',reference.includes('<table mat-table matSort') && reference.includes('<mat-paginator'));
expect('reference create/edit/delete actions retained',reference.includes('(click)="create()"') && reference.includes('(click)="edit(row)"') && reference.includes('(click)="remove(row)"'));
expect('reference date/time uses shared persian picker',reference.includes('[withTime]="true"') && !reference.includes('type="date"') && !reference.includes('type="datetime-local"'));
for(const rel of [
 'frontend/src/app/features/cif-reference/party-reference-page.component.html',
 'frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.html',
 'frontend/src/app/features/fee/fee-management.component.html',
 'frontend/src/app/features/fee2/studio/fee2-studio-create-dialog.component.html',
 'frontend/src/app/features/product-builder/rule-policy-lite.component.html']){
 const s=read(rel);
 expect('typed date '+rel,!s.includes('type="date"') && !s.includes('type="datetime-local"') && s.includes('<app-persian-date-input'));
}
const date=read('frontend/src/app/shared/ui/persian-date-input.component.ts');
expect('shared date has theme tokens without changing date payload',date.includes('var(--app-surface)') && date.includes('var(--app-primary)') && date.includes('this.onChange(value)'));
for(const rel of [
 'frontend/src/app/features/four-deposits/deposit-opening-wizard.component.scss',
 'frontend/src/app/features/four-deposits/deposit-account-operations.component.scss']) {
 const s=read(rel);expect('independent deposit UI inherits shared tokens '+rel,s.includes('System UI Standard v1')&&s.includes('--surface:var(--app-surface)'));
}
const clt=read('docs/collateral/CLT-ROADMAP-R1.0-FA.md');
expect('canonical R0-R7 roadmap preserved', ['**R0**','**R1**','**R2**','**R3**','**R4**','**R5**','**R6**','**R7**'].every(t=>clt.includes(t)));
const updated=read('docs/collateral/CLT-ROADMAP-R1.0-STATUS-2026-11-08-SYSTEM-UI.md');
expect('collateral checkpoint references system UI cross-cutting change',updated.includes('CLT-ROADMAP-R1.0')&&updated.includes('R0')&&updated.includes('R7'));
const bad=tests.filter(t=>!t.ok);
for(const t of tests)console.log((t.ok?'PASS':'FAIL')+' | '+t.name);
if(bad.length){console.error('SYSTEM_UI_V1_SOURCE_FAIL='+bad.length);console.error('Unscoped roots:',badRoot);process.exitCode=1;}
else console.log('SYSTEM_UI_V1_SOURCE_PASS='+tests.length+'; ANGULAR_BUILD=NOT_PROVEN; BROWSER_UAT=NOT_RUN; DOMAIN_RULES=UNCHANGED');
