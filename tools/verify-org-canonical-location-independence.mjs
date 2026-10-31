import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const api=read('backend/src/main/java/com/behsazan/corebanking/organization/location/manage/OrganizationUnitLocationController.java');
const browse=read('backend/src/main/java/com/behsazan/corebanking/organization/location/manage/OrganizationUnitLocationBrowseService.java');
const service=read('backend/src/main/java/com/behsazan/corebanking/organization/location/manage/OrganizationUnitLocationService.java');
const models=read('backend/src/main/java/com/behsazan/corebanking/organization/location/manage/OrganizationUnitLocationModels.java');
const ui=read('frontend/src/app/features/organization/organization-unit-locations-canonical.component.ts');
const generic=read('backend/src/main/java/com/behsazan/corebanking/referencedata/management/application/ReferenceService.java');
const sql=read('database/oracle/org/canonical-location-independence/01-ORG-UNIT-LOCATION-DETACH-OPT-IN.sql');
const verifySql=read('database/oracle/org/canonical-location-independence/02-ORG-UNIT-LOCATION-INDEPENDENCE-VERIFY-READONLY.sql');
const checks=[
['R3.1 Operational UI reads canonical-only browse API',()=>{
  assert.ok(ui.includes("'/api/v1/organization/unit-locations/browse'"));
  assert.ok(!ui.includes("'/api/v1/organization/unit-location-canonical-review'"));
  assert.ok(api.includes('@GetMapping("/browse")'));
}],
['R3.2 Operational browse has no legacy source join or DML',()=>{
  assert.ok(browse.includes('FROM %s.ORGANIZATION_UNIT_LOCATIONS UL'));
  assert.ok(browse.includes('JOIN %s.ORGANIZATION_UNITS OU'));
  assert.ok(!/\bJOIN\s+[^\n]*\.(?:LOCATIONS|POSTAL_ADDRESSES)\b/i.test(browse));
  assert.ok(!/\b(?:INSERT\s+INTO|UPDATE|DELETE\s+FROM|MERGE\s+INTO)\s+(?:ORG|%s)\./i.test(browse));
  assert.ok(browse.includes('CANONICAL_SINGLE_TABLE'));
}],
['R3.3 Write readiness fails closed while old FK remains',()=>{
  assert.ok(service.includes('AS LEGACY_LOCATION_FK'));
  assert.ok(service.includes('boolean detached=rs.getInt("LEGACY_LOCATION_FK")==0'));
  assert.ok(service.includes('n&&m&&u&&a&&g&&detached'));
  assert.ok(models.includes('legacyLocationFkDetached'));
}],
['R3.4 Writes are canonical-only and generic bypass remains blocked',()=>{
  assert.ok(service.includes('INSERT INTO ORG.ORGANIZATION_UNIT_LOCATIONS'));
  assert.ok(service.includes(':id,:unitId,NULL,:role'));
  for (const r of ['locations','postal-addresses','organization-unit-locations'])
    assert.ok(generic.includes('"'+r+'".equals(resource)'));
}],
['R3.5 DBA opt-in default and non-destructive legacy retention',()=>{
  assert.match(sql,/DEFINE\s+APPLY_CHANGES\s*=\s*NO/);
  assert.match(sql,/IF v_apply='NO' THEN[\s\S]*?RETURN;[\s\S]*?END IF;/);
  assert.ok(sql.includes('DROP CONSTRAINT FK_ORGANIZATION_UNIT_LOCATIONS_LOCATION_ID'));
  assert.ok(sql.includes('MODIFY (LOCATION_ID NULL)'));
  assert.ok(!/\b(?:UPDATE\s+ORG|DELETE\s+FROM\s+ORG|MERGE\s+INTO\s+ORG|INSERT\s+INTO\s+ORG|TRUNCATE\s+TABLE|DROP\s+TABLE)\b/i.test(sql));
  assert.ok(!/ALTER\s+TABLE\s+ORG\.(?:LOCATIONS|POSTAL_ADDRESSES|SERVICE_POINTS)\b/i.test(sql));
  assert.ok(sql.includes('v_unbackfilled') && sql.includes('v_incomplete') && sql.includes('v_missing_location'));
  assert.ok(sql.includes('ORG_CANONICAL_LOCATION_DETACH_PASS'));
}],
['R3.6 Read-only post-verification and explicit service point blocker',()=>{
  assert.ok(verifySql.includes('SERVICE_POINT_LEGACY_LINKS'));
  assert.ok(verifySql.includes('MANAGED_WITHOUT_AUDIT'));
  assert.ok(!/\b(?:UPDATE\s+ORG|DELETE\s+FROM\s+ORG|MERGE\s+INTO\s+ORG|INSERT\s+INTO\s+ORG|TRUNCATE\s+TABLE|DROP\s+TABLE)\b/i.test(verifySql));
}],
];
let failed=0;
for (const [label,fn] of checks) {
  try{fn(); console.log('PASS | '+label);}catch(e){failed++;console.error('FAIL | '+label+' | '+e.message);}
}
if(failed){console.error('ORG_CANONICAL_LOCATION_INDEPENDENCE_VERIFY_FAIL='+failed);process.exit(1);}
console.log('ORG_CANONICAL_LOCATION_INDEPENDENCE_VERIFY_PASS=6');
