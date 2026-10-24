import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
function read(file){return fs.readFileSync(path.join(root,file),'utf8');}
function check(name,fn){try{fn();console.log('PASS',name);}catch(e){console.error('FAIL',name,e.message);process.exitCode=1;}}
const menu=read('frontend/src/app/features/organization/organization-menu.component.ts');
const menuHtml=read('frontend/src/app/features/organization/organization-menu.component.html');
const review=read('backend/src/main/java/com/behsazan/corebanking/organization/location/application/OrganizationUnitLocationCanonicalReviewService.java');
const manage=read('backend/src/main/java/com/behsazan/corebanking/organization/location/manage/OrganizationUnitLocationService.java');
const contact=read('backend/src/main/java/com/behsazan/corebanking/organization/contact/OrganizationContactService.java');
const edit=read('frontend/src/app/features/organization/organization-unit-location-editor.component.ts');
const view=read('frontend/src/app/features/organization/organization-unit-locations-canonical.component.ts');
const script=read('database/oracle/org/locations-r2b/01-ORG-UNIT-LOCATIONS-R2B-OPT-IN.sql');
const forensic=read('database/oracle/org/contacts-r2/01-ORG-LEGACY-BRANCH-PHONES-READONLY.sql');
check('01. New menu labels match agreed wording',()=>{
 assert.ok(menu.includes('راه‌های ارتباطی شعب و واحدها'));
 assert.ok(menuHtml.includes('نشانی و موقعیت شعب و واحدها'));
 assert.ok(view.includes('نشانی و موقعیت شعب و واحدها'));
});
check('02. Old three forms not present in primary ORG menu',()=>{
 assert.ok(!menu.includes("resources: ['locations', 'postal-addresses'"));
 assert.ok(!menu.includes("resources: ['organization-unit-locations'"));
});
check('03. Preview first, never auto-enable Oracle DDL',()=>{
 assert.match(script,/DEFINE APPLY_CHANGES = NO/);
 assert.match(script,/IF v_apply='NO' THEN/);
 assert.ok(!/^\s*(?:UPDATE|DELETE|TRUNCATE|MERGE)\s+ORG\./im.test(script));
});
check('04. Legacy data write protection',()=>{
 assert.ok(!/\b(?:UPDATE|DELETE\s+FROM|INSERT\s+INTO)\s+ORG\.(LOCATIONS|POSTAL_ADDRESSES)\b/i.test(manage));
 assert.ok(!/\b(?:UPDATE|DELETE\s+FROM|INSERT\s+INTO)\s+ORG\.(LOCATIONS|POSTAL_ADDRESSES)\b/i.test(script));
});
check('05. Legacy location FK only changed to nullable and never dropped',()=>{
 assert.ok(script.includes('MODIFY (LOCATION_ID NULL)'));
 assert.ok(!/DROP\s+(?:CONSTRAINT|COLUMN|TABLE)/i.test(script));
});
check('06. Canonical CRUD uses same transaction for audit and data',()=>{
 assert.ok(manage.includes('public LocationDetails create'));
 assert.ok(manage.includes('public LocationDetails update'));
 assert.match(manage,/@Transactional\s+public LocationDetails create/);
 assert.match(manage,/@Transactional\s+public LocationDetails update/);
 assert.ok(manage.includes('audit(id,unitId,"CREATE"'));
 assert.ok(manage.includes('audit(id,body.organizationUnitId(),"UPDATE"'));
});
check('07. Concurrency and primary date overlaps are checked',()=>{
 assert.ok(manage.includes('ROW_VERSION=:expectedVersion'));
 assert.ok(manage.includes('FOR UPDATE'));
 assert.ok(manage.includes('ensureNoPrimaryOverlap'));
});
check('08. Canonical review is compatible before R2B DDL',()=>{
 assert.ok(review.includes('r2bColumnsReady()'));
 assert.ok(review.includes('managedColumnsPresent ? "UL.CANONICAL_MANAGED_FLAG" : "0"'));
 assert.ok(review.includes("'NATIVE_RECORD'"));
});
check('09. Contact phone values were not erased by R1 contact CRUD',()=>{
 assert.ok(!/\b(?:DELETE|TRUNCATE)\s+(?:FROM\s+)?ORG\./i.test(contact));
 assert.ok(forensic.includes('NO DDL. NO DML. NO COMMIT'));
});
check('10. UI does not write before Oracle R2B readiness',()=>{
 assert.ok(view.includes("managementReady()===null") && view.includes("if(!this.managementReady())"));
 assert.ok(view.includes("/api/v1/organization/unit-locations/readiness"));
 assert.ok(edit.includes('decisionReference'));
});
check('11. Data origin and 1327 sharing protected',()=>{
 assert.ok(manage.includes('legacy LOCATION_ID=1327'));
 assert.ok(manage.includes('LOCATION_ID,') && manage.includes('VALUES ('));
 assert.ok(manage.includes('CANONICAL_MANAGED_FLAG=1'));
});
if(process.exitCode) process.exit(process.exitCode);
console.log('ORG_R2B_STATIC_CHECKS_COMPLETE. PASS=11. NO ORACLE WRITES.');
