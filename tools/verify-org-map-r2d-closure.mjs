import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const map=read('backend/src/main/java/com/behsazan/corebanking/organization/location/map/OrganizationBranchMapService.java');
const controller=read('backend/src/main/java/com/behsazan/corebanking/organization/location/map/OrganizationBranchMapController.java');
const location=read('backend/src/main/java/com/behsazan/corebanking/organization/location/manage/OrganizationUnitLocationService.java');
const models=read('backend/src/main/java/com/behsazan/corebanking/organization/location/manage/OrganizationUnitLocationModels.java');
const frontendMap=read('frontend/src/app/features/organization/organization-branch-map.component.ts');
const editor=read('frontend/src/app/features/organization/organization-unit-location-editor.component.ts');
const ddl=read('database/oracle/org/map-r2d/01-ORG-MAP-EXPLICIT-GEO-TYPE-OPT-IN.sql');
const postverify=read('database/oracle/org/map-r2d/02-ORG-MAP-GEO-TYPE-POSTVERIFY-READONLY.sql');
let ok=0;let failed=0;
function check(name,fn){try{fn();ok++;console.log('PASS |',name);}catch(error){failed++;console.error('FAIL |',name,'-',error.message);}}
check('Historical city-only technical references are scoped to proven import rows',()=>{
  assert.match(map,/LEGACY_BACKFILL_FLAG=1 THEN 'CITY'/);
  assert.match(map,/GEO_ENTITY_TYPE_CODE IN \('CITY','RURAL_DISTRICT'\)/);
  assert.match(map,/GEO_ENTITY_TYPE_CODE_EFFECTIVE='CITY'/);
  assert.match(map,/GEO_ENTITY_TYPE_CODE_EFFECTIVE='RURAL_DISTRICT'/);
  assert.ok(!map.includes('r.IS_ACTIVE=1 AND c.CITY_ID IS NULL'));
});
check('Rural and city GEO joins are independent and typed',()=>{
  assert.match(map,/LEFT JOIN GEO\.CITIES c ON c\.CITY_ID=l\.GEO_ENTITY_ID/);
  assert.match(map,/LEFT JOIN GEO\.RURAL_DISTRICTS r ON r\.RURAL_DISTRICT_ID=l\.GEO_ENTITY_ID/);
  assert.match(map,/l\.GEO_ENTITY_TYPE_CODE_EFFECTIVE='CITY'/);
  assert.match(map,/l\.GEO_ENTITY_TYPE_CODE_EFFECTIVE='RURAL_DISTRICT'/);
});
check('No data loss when coordinates are missing',()=>{
  assert.ok(map.includes('FROM ENRICHED'));
  assert.ok(map.includes('NVL(SUM(CASE WHEN LATITUDE BETWEEN 24 AND 41'));
  assert.ok(!map.includes('AND LATITUDE BETWEEN 24 AND 41 AND LONGITUDE BETWEEN 44 AND 64\n                  ORDER BY'));
  assert.ok(frontendMap.includes('hasCoordinates(point)'));
  assert.ok(frontendMap.includes('فاقد مختصات قابل نمایش'));
});
check('City and rural selectors report independent current active placement counts',()=>{
  assert.ok(map.includes('COUNT(DISTINCT CASE WHEN u.STATUS_CODE=\'ACTIVE\''));
  assert.ok(map.includes('AND ("+typeExpr+")=\'"+kind+"\''));
  assert.ok(frontendMap.includes('item.unitCount ?? 0'));
});
check('No free-form GEO ID in the new location editor',()=>{
  assert.ok(!editor.includes('type="number" formControlName="geoEntityId"'));
  for(const token of ['انتخاب استان','انتخاب شهرستان','انتخاب شهر','انتخاب دهستان',
     'geoEntityTypeCode','geo-context','chooseGeoEntity','changeCounty','changeProvince'])assert.ok(editor.includes(token),token);
  assert.ok(controller.includes('@GetMapping("/geo-context")'));
});
check('Backend validates selected GEO type against correct active reference',()=>{
  assert.match(location,/GEO\.CITIES WHERE CITY_ID=:id AND IS_ACTIVE=1/);
  assert.match(location,/GEO\.RURAL_DISTRICTS WHERE RURAL_DISTRICT_ID=:id AND IS_ACTIVE=1/);
  assert.ok(location.includes('GEO_ENTITY_TYPE_CODE=:geoType'));
  assert.ok(location.includes('GEO_ENTITY_ID,GEO_ENTITY_TYPE_CODE'));
  assert.ok(models.includes('String geoEntityTypeCode'));
});
check('Pre-DDL read-only map still works; location writer fails closed',()=>{
  assert.ok(map.includes('geoTypeColumnInstalled()'));
  assert.ok(location.includes('n&&m&&u&&a&&g'));
  assert.ok(location.includes('requireReady()'));
});
check('DDL is explicitly opt-in and non-destructive, without bulk data change',()=>{
  assert.match(ddl,/DEFINE APPLY_CHANGES = NO/);
  assert.match(ddl,/IF v_apply='NO' THEN/);
  assert.ok(ddl.includes('CHECK (GEO_ENTITY_TYPE_CODE IN (\'CITY\',\'RURAL_DISTRICT\'))'));
  assert.ok(!/\b(?:UPDATE|DELETE|MERGE|TRUNCATE|DROP)\b\s+(?:TABLE\s+|FROM\s+|INTO\s+)?ORG\./i.test(ddl));
  assert.ok(!/^\s*COMMIT\s*;/im.test(ddl));
  assert.ok(postverify.includes('ORG_MAP_R2D_POSTVERIFY_PASS. READONLY.'));
});
check('Legacy R1 location rows and contact tables are never rewritten by map',()=>{
  assert.ok(!/\b(?:INSERT\s+INTO|UPDATE\s+ORG\.|DELETE\s+FROM|MERGE\s+INTO)\b/i.test(map));
  assert.ok(!/\b(?:INSERT\s+INTO|UPDATE\s+ORG\.|DELETE\s+FROM|MERGE\s+INTO)\b/i.test(controller));
  assert.ok(!location.includes('UPDATE ORG.LOCATIONS'));
  assert.ok(!location.includes('UPDATE ORG.POSTAL_ADDRESSES'));
});
check('Pagination and zoom use actual visible coordinates only',()=>{
  assert.ok(frontendMap.includes('this.fitToFilteredResults(data.points);'));
  assert.ok(frontendMap.includes('this.hasCoordinates(p)'));
  assert.ok(frontendMap.includes('this.result.set(data)'));
  assert.ok(map.includes('FETCH FIRST :maxRows ROWS ONLY'));
});
const req=createRequire(import.meta.url);
const candidates=[path.join(root,'frontend/node_modules/typescript/lib/typescript.js'),
  path.resolve(path.dirname(process.execPath),'../lib/node_modules/typescript/lib/typescript.js'),
  ...(process.env.APPDATA?[path.join(process.env.APPDATA,'npm/node_modules/typescript/lib/typescript.js')]:[])];
const tsPath=candidates.find(fs.existsSync);
if(tsPath){
  const ts=req(tsPath);
  check('All changed Angular TypeScript modules parse',()=>{
    for(const f of ['organization-branch-map.component.ts','organization-unit-location-editor.component.ts']){
      const result=ts.transpileModule(read(`frontend/src/app/features/organization/${f}`),{
        fileName:f,reportDiagnostics:true,compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ESNext,experimentalDecorators:true}
      });
      assert.equal(result.diagnostics?.length??0,0,f);
    }
  });
}else console.log('INFO | Global TypeScript unavailable. TypeScript syntax compilation requires local npm packages.');
if(failed){console.error(`ORG_MAP_R2D_CLOSURE_FAIL=${failed} PASS=${ok}`);process.exit(1);}
console.log(`ORG_MAP_R2D_CLOSURE_STATIC_PASS=${ok}`);
