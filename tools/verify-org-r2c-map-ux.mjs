import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
let pass=0;
function check(name,fn){try{fn();console.log('PASS |',name);pass++;}catch(err){console.error('FAIL |',name,err.message);process.exitCode=1;}}
const routes=read('frontend/src/app/app.routes.ts');
const menu=read('frontend/src/app/features/organization/organization-menu.component.ts');
const menuHtml=read('frontend/src/app/features/organization/organization-menu.component.html');
const map=read('frontend/src/app/features/organization/organization-branch-map.component.ts');
const picker=read('frontend/src/app/features/organization/organization-coordinate-map-picker.component.ts');
const tiles=read('frontend/src/app/features/organization/organization-map-tile-source.ts');
const mapApi=read('backend/src/main/java/com/behsazan/corebanking/organization/location/map/OrganizationBranchMapController.java');
const mapSvc=read('backend/src/main/java/com/behsazan/corebanking/organization/location/map/OrganizationBranchMapService.java');
const loc=read('frontend/src/app/features/organization/organization-unit-locations-canonical.component.ts');
const locManage=read('backend/src/main/java/com/behsazan/corebanking/organization/location/manage/OrganizationUnitLocationService.java');
const contact=read('frontend/src/app/features/organization/organization-unit-contacts.component.ts');
const contactSvc=read('backend/src/main/java/com/behsazan/corebanking/organization/contact/OrganizationContactService.java');
const sql=read('database/oracle/org/locations-r2b/01-ORG-UNIT-LOCATIONS-R2B-OPT-IN.sql');
check('01 Current bank-user names',()=>{
 assert.ok(loc.includes('نشانی و موقعیت شعب و واحدها'));
 assert.ok(contact.includes('راه‌های ارتباطی شعب و واحدها'));
 assert.ok(menu.includes('نقشه استقرار شعب و واحدها'));
});
check('02 No historical migration internals in operational location/contact screens',()=>{
 for(const term of ['R1','R2-A','تجمیع','مبدأ قدیمی','جدول یکپارچه ORG']){
   assert.ok(!loc.includes(term),`location contains ${term}`);
   assert.ok(!contact.includes(term),`contact contains ${term}`);
 }
 assert.ok(!loc.includes('sourceStatus')&&!loc.includes('legacyPostalAddressId'));
});
check('03 Real create/update remains guarded by R2B readiness',()=>{
 assert.ok(loc.includes('managementReady()')&&loc.includes('startCreate()')&&loc.includes('startEdit('));
 assert.ok(locManage.includes('requireReady()')&&locManage.includes('ensureNoPrimaryOverlap'));
 assert.ok(locManage.includes('audit(id,unitId,"CREATE"')&&locManage.includes('audit(id,body.organizationUnitId(),"UPDATE"'));
 assert.ok(sql.includes('DEFINE APPLY_CHANGES = NO'));
});
check('04 Contact remains source-faithful and user can add verified data',()=>{
 assert.ok(contact.includes('loadRows(unitId)')&&contact.includes('contactValue'));
 assert.ok(!contactSvc.includes('CIF.CONTACT_POINT'));
 assert.ok(!/\bDELETE\s+FROM\s+ORG\./i.test(contactSvc));
 assert.ok(contact.includes('unitId')&&contact.includes('queryParamMap'));
});
check('05 Map route, menu and deep links are wired',()=>{
 assert.ok(routes.includes("path: 'organization/branch-map'"));
 assert.ok(menu.includes("resource: 'branch-map'"));
 assert.ok(menuHtml.includes('routerLink="/organization/branch-map"'));
 assert.ok(map.includes("'/organization/unit-contacts'")&&map.includes("'/organization/unit-locations-canonical'"));
 assert.ok(map.includes('selectedContacts.set(rows.filter('), 'map details must load validated persisted unit contacts');
});
check('06 Real geographic parent filters (no fake city => rural hierarchy)',()=>{
 for(const term of ['GEO.PROVINCES','GEO.COUNTIES','GEO.DISTRICTS','GEO.CITIES','GEO.RURAL_DISTRICTS'])assert.ok(mapSvc.includes(term));
 assert.ok(mapSvc.includes('r.DISTRICT_ID')&&mapSvc.includes('c.DISTRICT_ID'));
 assert.ok(map.includes('changeProvince(')&&map.includes('changeCounty(')&&map.includes('changeCity(')&&map.includes('changeRural('));
});
check('07 Map comes from bounded read-only Oracle data',()=>{
 assert.ok(mapSvc.includes('MAX_ROWS=6000')&&mapSvc.includes('FETCH FIRST :maxRows ROWS ONLY'));
 assert.ok(mapSvc.includes('@Transactional(readOnly=true)'));
 assert.ok(mapApi.includes('@GetMapping("/points")')&&mapApi.includes('@GetMapping("/geo-options")'));
 assert.ok(!/@(?:PostMapping|PutMapping|PatchMapping|DeleteMapping)/.test(mapApi));
 assert.ok(!/\b(?:DELETE\s+FROM|UPDATE\s+ORG\.|INSERT\s+INTO|TRUNCATE|MERGE\s+INTO)\b/i.test(mapSvc));
});
check('08 Only valid geocoordinates plotted; no fabricated pin',()=>{
 assert.ok(mapSvc.includes('LATITUDE BETWEEN 24 AND 41 AND LONGITUDE BETWEEN 44 AND 64'));
 assert.ok(map.includes('withoutCoordinates')&&map.includes('tileError.set(true)'));
 assert.ok(map.includes('organizationMapTileUrl(')&&map.includes('openstreetmap.org/copyright'));
 assert.ok(picker.includes('organizationMapTileUrl(')&&picker.includes('openstreetmap.org/copyright'));
 assert.ok(tiles.includes('https://tile.openstreetmap.org/'));
 for(const src of [map,picker,tiles])assert.ok(!src.includes('basemaps.cartocdn.com')&&!src.includes('carto.com/attributions'));
});
check('09 Map markers are clustered and interactive',()=>{
 assert.ok(map.includes('this.markers.set(')&&map.includes('Math.floor(x/42)'));
 assert.ok(map.includes('((x/scale*360)%360+360)%360-180'), 'longitude inverse Mercator must preserve Iran longitude');
 for(const name of ['zoomBy(', 'iranView(', 'pointerDown(', 'pointerMove(', 'focus('])assert.ok(map.includes(name));
});
check('10 Never delete or rewrite official old location tables',()=>{
 for(const source of [locManage,sql])assert.ok(!/\b(?:DELETE\s+FROM|TRUNCATE\s+TABLE|UPDATE)\s+ORG\.(?:LOCATIONS|POSTAL_ADDRESSES)\b/i.test(source));
 assert.ok(locManage.includes('ROW_VERSION=:expectedVersion'));
});
if(process.exitCode)process.exit(process.exitCode);
console.log(`ORG_R2C_MAP_UX_VERIFY_PASS=${pass}`);
