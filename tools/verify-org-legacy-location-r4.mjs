import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const provider=read('backend/src/main/java/com/behsazan/corebanking/organization/descriptor/OrganizationDescriptorProvider.java');
const route=read('frontend/src/app/app.routes.ts');
const ui=read('frontend/src/app/features/reference-data/presentation/reference-page.component.ts');
const controllers=['OrganizationUnitLocationReviewController.java','OrganizationUnitLocationCanonicalReviewController.java']
    .map(n=>read('backend/src/main/java/com/behsazan/corebanking/organization/location/web/'+n));
const sqlBase='database/oracle/org/location-retirement-r4/';
const a=read(sqlBase+'01-ORG-R4-SERVICE-POINT-CANONICAL-CUTOVER-OPT-IN.sql');
const av=read(sqlBase+'02-ORG-R4-SERVICE-POINT-VERIFY-READONLY.sql');
const b=read(sqlBase+'03-ORG-R4-LEGACY-RENAME-PREVIEW-OPT-IN.sql');
const bv=read(sqlBase+'04-ORG-R4-LEGACY-RENAME-VERIFY-READONLY.sql');
const restore=read(sqlBase+'05-ORG-R4-LEGACY-RENAME-RESTORE-OPT-IN.sql');
const status=read('backend/src/main/java/com/behsazan/corebanking/organization/location/servicepoint/OrganizationServicePointLocationReadinessController.java');
const tests=read('backend/src/test/java/com/behsazan/corebanking/organization/descriptor/OrganizationR4LocationDescriptorTest.java');
const testsPassed=[];
const check=(label,fn)=>{try{fn();console.log('PASS | '+label);testsPassed.push(label);}catch(e){console.error('FAIL | '+label+' | '+e.message);process.exitCode=1;}};
check('R4.1 Location source descriptors withdrawn, canonical lookup retained',()=>{
  assert.ok(!provider.includes('                locations(),'));
  assert.ok(!provider.includes('                postalAddresses(),'));
  assert.ok(!provider.includes('return descriptor("locations"'));
  assert.ok(!provider.includes('return descriptor("postal-addresses"'));
  assert.ok(provider.includes('"organization-unit-locations"'));
  assert.ok(provider.includes('"organizationUnitLocationId", "addressFa"'));
  assert.ok(provider.includes('lookup("organizationUnitLocationId", "ORGANIZATION_UNIT_LOCATION_ID"'));
  assert.ok(!provider.includes('lookup("locationId", "LOCATION_ID"'));
});
check('R4.2 Operational forms/routes no longer navigate legacy source tables',()=>{
  for(const resource of ['locations','postal-addresses','organization-unit-locations'])
    assert.ok(route.includes(`path: 'organization/${resource}', redirectTo: 'organization/unit-locations-canonical'`));
  assert.ok(route.includes("path: 'organization/unit-locations-review', redirectTo: 'organization/unit-locations-canonical'"));
  assert.ok(ui.includes("'points-of-service': ['pointOfServiceTypeId', 'organizationUnitLocationId', 'statusCode']"));
});
check('R4.3 Historical comparison API defaults off, not a runtime dependency',()=>{
  for(const ctrl of controllers) assert.ok(ctrl.includes('@ConditionalOnProperty(') && ctrl.includes('legacy-review-enabled'));
  for(const app of ['config/application.yml','backend/src/main/resources/application.yml'])
     assert.ok(read(app).includes('legacy-review-enabled: ${ORG_LEGACY_LOCATION_REVIEW_ENABLED:false}'));
});
check('R4.4 Stage A requires zero service-point rows and never modifies legacy source tables',()=>{
  assert.match(a,/DEFINE\s+APPLY_CHANGES\s*=\s*NO/);
  for(const token of ['v_pos_rows<>0','v_legacy_rows<>0','v_legacy_unit_fk<>0','v_blockers<>0',
    'MODIFY (LOCATION_ID NULL)','FK_SP_R4_CANONICAL_LOCATION','CK_SP_R4_NO_LEGACY','REQUIRE_CANONICAL_LOCATION_ID'])
       assert.ok(a.includes(token),`Missing: ${token}`);
  assert.ok(!/ALTER\s+TABLE\s+ORG\.(LOCATIONS|POSTAL_ADDRESSES)\b/i.test(a));
});
check('R4.5 Stage B is manual, data-parity gated, and never deletes sources',()=>{
  for(const flag of ['APPLY_CHANGES','CONFIRM_RUNTIME_CUTOVER','CONFIRM_EXTERNAL_CONSUMERS'])
     assert.ok(new RegExp(`DEFINE\\s+${flag}\\s*=\\s*NO`).test(b));
  for(const token of ['v_not_covered_l<>0','v_not_covered_p<>0','v_missing_source<>0','v_drift<>0',
     'v_unknown_fk<>0','v_stored<>0','v_synonyms<>0','v_triggers<>0',
     'rename_table(\'POSTAL_ADDRESSES\'','rename_table(\'LOCATIONS\''])
     assert.ok(b.includes(token),`Missing: ${token}`);
  assert.equal((b.match(/DECODE\(UL\.[A-Z_]+,[LP]\.[A-Z_]+,0,1\)/g)??[]).length,20);
  assert.ok(!/\b(?:DELETE\s+FROM|INSERT\s+INTO|MERGE\s+INTO|UPDATE\s+ORG|DROP\s+TABLE|DROP\s+SEQUENCE|TRUNCATE\s+TABLE)\b/i.test(b.replace(/^\s*--[^\n]*$/gm,'')));
});
check('R4.6 Post-verification and manual restore preserve metadata and rows',()=>{
  assert.ok(av.includes('SERVICE_POINT_ROWS') && av.includes('ORG_R4A_SERVICE_POINT_VERIFY_READONLY_COMPLETE'));
  assert.ok(bv.includes('DEPRECATED_LOCATIONS') && bv.includes('HISTORICAL_LOCATION_ID_ROWS'));
  assert.match(restore,/DEFINE\s+APPLY_RESTORE\s*=\s*NO/);
  assert.ok(restore.includes('ORG_R4B_RESTORE_PREVIEW_COMPLETE'));
});
check('R4.7 Runtime readiness and JUnit keep contract verifiable',()=>{
  assert.ok(status.includes('@GetMapping') && status.includes('legacyForeignKeyDetached') && status.includes('ready) {}'));
  assert.ok(!status.includes('FROM ORG.LOCATIONS') && !status.includes('FROM ORG.POSTAL_ADDRESSES'));
  assert.ok(tests.includes('assertEquals(23, entries.size(),') && tests.includes('ORGANIZATION_UNIT_LOCATION_ID'));
});
check('R4.8 Both production build scripts invoke R4 QA',()=>{
  for(const name of ['build-production.cmd','build-production.sh'])
    assert.ok(read(name).includes('verify-org-legacy-location-r4.mjs'));
});
if(!process.exitCode)console.log('ORG_LEGACY_LOCATION_R4_STATIC_PASS='+testsPassed.length+' | NO ORACLE EXECUTION');
if(process.exitCode)process.exit(1);
