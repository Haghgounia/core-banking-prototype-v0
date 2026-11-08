'use strict';
// Run from any cwd. Static boundary checks only; no database mutation.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(process.argv[2]||path.join(__dirname,'../../..'));
const text=p=>fs.readFileSync(path.join(root,p),'utf8');
const prefix='frontend/src/app/features/collateral/';
const html=text(prefix+'clt-reference-workspace.component.html');
const ts=text(prefix+'clt-reference-workspace.component.ts');
const css=text(prefix+'clt-reference-workspace.component.scss');
const svc=text(prefix+'clt-reference.service.ts');
const helper=text(prefix+'clt-reference-presentation.ts');
const catalog=JSON.parse(text('backend/src/main/resources/clt/reference-catalog.json'));
const uiCatalogSource=text(prefix+'clt-reference-catalog.ts');
const uiStart=uiCatalogSource.indexOf('= [',uiCatalogSource.indexOf('export const CLT_REFERENCE_TABLES'))+2;
const uiEnd=uiCatalogSource.indexOf('];',uiStart)+1;
const uiCatalog=JSON.parse(uiCatalogSource.slice(uiStart,uiEnd));
const originalRoadmap=text('docs/collateral/CLT-ROADMAP-R1.0-FA.md');
let passed=0;
function check(name,fn){try{fn();passed++;console.log('PASS | '+name);}catch(e){console.error('FAIL | '+name+': '+e.message);process.exitCode=1;}}
check('fixed 17 physical reference tables and 204 exact source columns',()=>{
 assert.equal(catalog.tables.length,17);assert.equal(new Set(catalog.tables.map(x=>x.table)).size,17);
 assert.equal(catalog.tables.reduce((n,t)=>n+t.columns.length,0),204);
});
check('full UI and backend catalog parity in 17/17 table column contracts',()=>{
 assert.equal(uiCatalog.length,catalog.tables.length);
 for(const table of catalog.tables){const u=uiCatalog.find(t=>t.table===table.table);
  assert.ok(u,table.table);assert.equal(u.primaryKey,table.primaryKey);
  assert.deepEqual(u.columns.map(c=>[c.name,c.dataType,c.required,c.primaryKey]),
    table.columns.map(c=>[c.name,c.dataType,c.required,c.primaryKey]),table.table);
 }
});
check('every table has system and business fields',()=>{
 for(const t of catalog.tables){for(const n of [t.primaryKey,'CREATED_AT','CREATED_BY','RECORD_VERSION'])
 assert.ok(t.columns.some(c=>c.name===n),t.table+' '+n);
 assert.ok(t.columns.some(c=>!c.primaryKey&&!['CREATED_AT','UPDATED_AT','CREATED_BY','UPDATED_BY','RECORD_VERSION','STATUS'].includes(c.name)),t.table);
 }
});
check('generic business details hide system identifiers by default',()=>{
 for(const s of ['businessDetailColumns()','auditDetailColumns()','اطلاعات سیستمی و حسابرسی','جزئیات {{table.title}}','data-clt-business-status'])assert.ok(html.includes(s),s);
 assert.ok(ts.includes('cltIsBusinessDetailField')&&ts.includes('cltPersianAuditDateTime'));
});
check('timestamp presentation retains wall time and seconds, not API conversion',()=>{
 assert.ok(helper.includes("timeZone: 'UTC'")&&helper.includes("second: '2-digit'"));
 assert.ok(!svc.includes('toISOString()'));
});
check('typed CRUD and full-width three-column editor',()=>{
 for(const s of ['@case (\'status\')','@case (\'flag\')','@case (\'number\')','@case (\'lookup\')','mat-error','saveDirect()'])assert.ok(html.includes(s),s);
 assert.match(css,/clt-editor-fields\s*\{[^}]*repeat\(3/);
 assert.match(css,/clt-ref-workspace\.editor-active\s*\{[^}]*grid-template-columns:1fr/);
});
check('commit read-back used to verify success of create and update',()=>{
 assert.ok(svc.includes('record(table: string, id: string)'));
 assert.ok(ts.includes('this.api.record(table.table, id)'));
 assert.ok(ts.includes('cltVerifySavedReference')&&ts.includes('RECORD_VERSION'));
});
check('delete confirmation dialog verifies not found before success',()=>{
 for(const s of ['deleteConfirmDialog','confirmDelete()','deleteError()','onDeleteDialogClosed()'])assert.ok(html.includes(s),s);
 assert.ok(ts.includes('deleteDirect(')&&ts.includes('this.api.deleteRecord(')&&ts.includes('this.api.record(table.table, id)'));
});
check('no dangerous change to business financial writes or IAM',()=>{
 assert.ok(originalRoadmap.includes('R0..R7'));
 const security=text('backend/src/main/java/com/behsazan/corebanking/collateral/prototype/CltUnifiedPrototypeSecurityConfiguration.java');
 assert.match(security,/\.anyRequest\(\)\.denyAll\(\)/);
});
check('all legacy R3 source verifier markers retained',()=>{
 for(const s of ['data-clt-unified-crud','mat-table','mat-paginator','mat-sort-header','matTooltip="حذف"'])assert.ok(html.includes(s),s);
 assert.ok(ts.includes('deleteDirect('));
});
check('roadmap phase boundaries and G-16 G-17 explicitly remain open',()=>{
 const status=text('docs/collateral/CLT-R1-CLOSURE-CHECKPOINT-2026-11-08-FA.md');
 for(const s of ['R0','R1','R2','R3','R4','R5','R6','R7','G-16','G-17','NOT_VERIFIED'])assert.ok(status.includes(s),s);
});
if(process.exitCode){console.error('CLT_R1_CLOSURE_STATIC_FAILED='+passed+'/11');process.exit(1);}
console.log('CLT_R1_CLOSURE_STATIC_PASS='+passed+'/11; LIVE_ORACLE_CRUD=NOT_VERIFIED; BROWSER_UAT=NOT_RUN');
