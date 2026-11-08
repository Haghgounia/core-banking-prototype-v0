'use strict';
// CLT UI R3: source/design contract verifier. Does not contact Oracle or mutate data.
const fs=require('node:fs');const path=require('node:path');const assert=require('node:assert/strict');
const root=path.resolve(process.argv[2] || path.join(__dirname,'../../..'));
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const folder='frontend/src/app/features/collateral/';
let count=0;
function check(name,fn){try{fn();count++;console.log('PASS | '+name);}catch(e){console.error('FAIL | '+name+': '+e.message);process.exitCode=1;}}
const catalog=JSON.parse(read('backend/src/main/resources/clt/reference-catalog.json'));
const html=read(folder+'clt-reference-workspace.component.html');
const ts=read(folder+'clt-reference-workspace.component.ts');
const service=read(folder+'clt-reference.service.ts');
const backend=read('backend/src/main/java/com/behsazan/corebanking/collateral/reference/CollateralReferenceRepository.java');
const controller=read('backend/src/main/java/com/behsazan/corebanking/collateral/reference/CollateralReferenceController.java');
const panels=['clt-reference-workspace','collateral-registry','collateral-introduction','collateral-sponsor-panel',
  'collateral-insurance-panel','collateral-valuation-panel','collateral-capacity-panel','collateral-allocation-request-panel',
  'collateral-unallocation-panel','collateral-final-release-panel','collateral-reports','collateral-uat-workbench',
  'clt-r6d-readonly-diagnostics','clt-r6e-detail-diagnostics'];
check('17 unchanged independent reference tables',()=>{assert.equal(catalog.tables.length,17);assert.equal(new Set(catalog.tables.map(t=>t.table)).size,17);});
check('14 collateral screen templates present',()=>{for(const p of panels)assert.ok(read(folder+p+'.component.html'));});
check('shared app token styling across all operational screens',()=>{
  for(const p of panels.filter(p=>p!=='clt-reference-workspace'))assert.match(read(folder+p+'.component.scss'),/@include cltUi\.standard;/);
  assert.match(read(folder+'clt-reference-workspace.component.scss'),/--app-surface/);
});
check('reference has Material list, sort and pagination',()=>{
  for(const n of ['mat-table','mat-sort-header','mat-paginator','mat-icon-button','mat-form-field','mat-select']) assert.ok(html.includes(n),n);
});
check('create action on top of reference screen',()=>{
  assert.ok(html.indexOf('startDirectCreate()')<html.indexOf('data-clt-unified-crud'));
  assert.match(html,/افزودن رکورد/);
});
check('reference actions use icons and no DB name',()=>{
  for(const n of ['matTooltip="مشاهده"','matTooltip="ویرایش"','matTooltip="حذف"'])assert.ok(html.includes(n));
  assert.ok(!html.includes('ویرایش Oracle')&&!html.includes('حذف Oracle'));
});
check('all reference status filters paginated on server',()=>{
  assert.match(controller,/@RequestParam\(required = false\) String status/);
  assert.match(backend,/predicates\.add\("STATUS = \?"\)/);
  assert.match(ts,/status: this\.statusFilter\(\)/);
  assert.match(service,/params = params\.set\(key, value\)/);
});
check('sort column allowlist blocks SQL injection',()=>{
  assert.match(backend,/table\.columns\(\)\.stream\(\)\.noneMatch\(c -> c\.name\(\)\.equals\(sortColumn\)\)/);
  assert.match(backend,/List\.of\("ASC", "DESC"\)/);
});
check('parent FK selectors and dedicated relation filters',()=>{
  for(const n of ['COLLATERAL_TYPE_ID','COLLATERAL_ATTRIBUTE_DEFINITION_ID','COLLATERAL_GROUP_ID'])assert.ok(ts.includes(n));
  assert.ok(html.includes('lookupValues(column.name)')&&html.includes('filterTypeId()'));
});
check('typed flags/numbers/status fields and visible inline validation',()=>{
  assert.match(ts,/fieldKind\(column: CltColumn\)/);
  assert.ok(html.includes("@case ('flag')")&&html.includes("@case ('number')")&&html.includes("@case ('status')"));
  assert.ok(html.includes('mat-error'));
});
check('CHAR(1) flags, field lengths and attribute data type use typed controls',()=>{
  assert.match(ts,/CHAR\\\(1/);
  assert.match(ts,/fieldMaxLength\(column: CltColumn\)/);
  assert.match(ts,/REF_COLLATERAL_ATTRIBUTE_DATA_TYPE/);
  assert.ok(html.includes('[attr.maxlength]="fieldMaxLength(column)"'));
});
check('currency selector uses active CLT reference and no free code',()=>{
  const intro=read(folder+'collateral-introduction.component.html');
  const introTs=read(folder+'collateral-introduction.component.ts');
  assert.ok(intro.includes('currencyOptions()')&&!intro.includes('کد ارز (ISO 4217)'));
  assert.match(introTs,/rows\('REF_CURRENCY'.*status:'ACTIVE'/);
});
check('technology names removed from row CRUD controls',()=>{
  for(const value of ['CRUD محلی Oracle','ویرایش Oracle','حذف Oracle','ثبت جدید در Oracle'])assert.ok(!html.includes(value),value);
});
check('functional non-finalization rules unchanged',()=>{
  const sec=read('backend/src/main/java/com/behsazan/corebanking/collateral/prototype/CltUnifiedPrototypeSecurityConfiguration.java');
  assert.ok(sec.includes('.anyRequest().denyAll()'));
  assert.ok(!controller.includes('@PostMapping'));
});
check('existing Oracle CRUD endpoints preserved',()=>{
  const crud=read('backend/src/main/java/com/behsazan/corebanking/collateral/reference/CltUnifiedReferenceController.java');
  for(const n of ['@PostMapping("/{table}")','@PutMapping("/{table}/{id}")','@DeleteMapping("/{table}/{id}")'])assert.ok(crud.includes(n));
});
check('no endpoint or credentials embedded in styling',()=>{assert.doesNotMatch(read(folder+'_clt-ui-standard.scss'),/jdbc:|password:|username:|Bearer /i);});
check('all operational forms retained with shared theming',()=>{for(const p of panels.filter(x=>!x.includes('diagnostics')&&!x.startsWith('clt-reference')))assert.match(read(folder+p+'.component.html'),/class=/);});
const typeNames=['clt-reference-workspace.component.ts','clt-reference.service.ts','collateral-introduction.component.ts'];
try {
  const resolve=require.resolve('typescript',{paths:[path.join(root,'frontend'),'/opt/nvm/versions/node/v22.16.0/lib/node_modules',process.cwd()]});
  const typescript=require(resolve);
  for(const f of typeNames)check('TypeScript parse '+f,()=>{
    const file=path.join(root,folder,f);const result=typescript.transpileModule(fs.readFileSync(file,'utf8'),{
      fileName:file,reportDiagnostics:true,compilerOptions:{target:typescript.ScriptTarget.ES2022,module:typescript.ModuleKind.ES2022,experimentalDecorators:true}});
    const errs=(result.diagnostics||[]).filter(d=>d.category===typescript.DiagnosticCategory.Error);
    assert.equal(errs.length,0,errs.map(d=>typescript.flattenDiagnosticMessageText(d.messageText,' ')).join(';'));
  });
} catch(e) {console.log('INFO | TypeScript parser unavailable: '+e.message);}
if(process.exitCode){console.error('CLT_UI_R3_SOURCE_FAILED='+count);process.exit(1);}
console.log('CLT_UI_R3_SOURCE_PASS='+count+'; ANGULAR_BUILD=NOT_RUN; ORACLE_WRITE=NOT_RUN; BROWSER_UAT=NOT_RUN');
