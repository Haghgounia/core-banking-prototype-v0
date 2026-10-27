import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const get=p=>fs.readFileSync(path.join(root,p),'utf8');
const file={
  model:get('backend/src/main/java/com/behsazan/corebanking/fee3/domain/Fee3Models.java'),
  controller:get('backend/src/main/java/com/behsazan/corebanking/fee3/web/Fee3Controller.java'),
  service:get('backend/src/main/java/com/behsazan/corebanking/fee3/application/Fee3Service.java'),
  policy:get('backend/src/main/java/com/behsazan/corebanking/fee3/application/Fee3DraftPolicy.java'),
  repository:get('backend/src/main/java/com/behsazan/corebanking/fee3/oracle/Fee3Repository.java'),
  api:get('frontend/src/app/features/fee3/fee3-oracle-api.service.ts'),
  mapper:get('frontend/src/app/features/fee3/fee3-oracle-mapping.ts'),
  component:get('frontend/src/app/features/fee3/fee3-workspace.component.ts'),
  html:get('frontend/src/app/features/fee3/fee3-workspace.component.html'),
  types:get('frontend/src/app/features/fee3/fee3.models.ts'),
  state:get('frontend/src/app/features/fee3/fee3-state.service.ts'),
  sample:get('docs/install/FEE3-BE-R1-SAMPLE-DRAFT.json'),
};
let pass=0;
function check(name,ok){console.log((ok?'PASS':'FAIL')+' | '+name);if(!ok)process.exitCode=1;else pass++;}
const has=(section,needle)=>file[section].includes(needle);
for(const s of ['serviceCurrency','feeCurrency','uiContext'])check('typed UI contract '+s,has('model',s)&&has('mapper',s)&&has('api',s));
for(const [name,token] of Object.entries({
  'readiness':'/readiness','catalog':'/items','item aggregate':'/items/{code}',
  'write draft':'/drafts','update draft':'/drafts/{itemId}/{versionId}',
  'reference operations':'/references/operations','reference formulas':'/references/formulas',
  'reference sources':'/references/sources','reference codes':'/references/codes/{group}'
}))check('API '+name,has('controller',`"${token}"`));
for(const [name,source,token] of [
  ['no additional DDL','repository','CREATE TABLE'],['preserved fixed schema','repository','FCE_TARIFF_VERSION'],
  ['UI contract embedded in existing JSON','service','_fee3UiV1'],
  ['editable expression in existing rule JSON','mapper','_fceEditableExpressionV1'],
  ['server currency matches scope','policy','scope.currency().equals(version.serviceCurrency())'],
  ['server rejects non IRR','policy','!"IRR".equals(version.feeCurrency())'],
  ['server validates discount dates','policy','checkFinancialDates'],
  ['server validates guarantee portfolio','policy','checkConditionalContext'],
  ['server scopes UI keys','policy','UI_CONTEXT_KEYS'],
  ['no saved token in API service','api','X-FEE3-UAT-Token'],
  ['read after write','component','loadOracleItem(receipt.calcCode)'],
  ['write guard on flag','component','oracleReadiness?.draftWritesEnabled'],
  ['no silent fallback','component',"oracleStatus='offline'"],
  ['live catalogue replaces fixture','component','pages.map(fee3SummaryToItem)'],
  ['live reference groups loaded','component','groups.map(g=>firstValueFrom(this.oracle.codes(g)))'],
  ['live formulas loaded','component','this.formulas=formulas.map'],
  ['read only seeded items','component','F3-UAT-'],
  ['optimistic concurrency mapping','mapper','expectedVersionRecordVersion'],
  ['source document lookup','mapper','documentNo===v.sourceCode'],
  ['no fabricated test result','mapper','result:null,status:null'],
  ['server UAT token required','service','MessageDigest.isEqual'],
  ['no seed overwrite','service','FEE3_SEED_WRITE_DENIED'],
  ['no publication route','controller','/publish'],
  ['R8 money directive retained','html','fee3Money'],
  ['seven step UI retained','component','visibleStageIndices=[0,1,2,3,4,6,7]'],
  ['UAT auth separate from wizard','html','fee3-uat-dialog'],
  ['UAT token typed password','html','type="password"'],
  ['sample updated','sample','uiContext'],
]){
  check(name,name==='no additional DDL'||name==='no publication route'?!has(source,token):has(source,token));
}
check('R1 unchanged mandatory lock',has('repository','FOR UPDATE'));
check('R1 unchanged Oracle audit',has('repository','UAT_DRAFT_UPDATED'));
check('no external PostJournal',!Object.values(file).join('\n').includes('PostJournal'));
check('write token never localStorage',!file.component.includes('localStorage')&&!file.api.includes('localStorage'));
check('write token cleared immediately after submit',has('component',"this.oracleSaving=true;this.uatTokenInput='';"));
check('browser UAT only on HTTPS or loopback',has('component',"location.protocol!=='https:'"));
for(const ext of ['cmd','sh'])check('release build wires R2 '+ext,get('build-production.'+ext).includes('fee3-backend')&&get('build-production.'+ext).includes('verify-r2.mjs'));
console.log(`FEE3_BE_R2_STATIC_PASS=${pass} FAIL=${process.exitCode?1:0}`);
