// R5-C4B/R5-C5: single operational OUCP storage source contract (static, no Oracle).
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
let passed=0;
const test=(ok,msg)=>{if(!ok)throw Error(msg);passed++;};
try {
  const source='backend/src/main/java/com/behsazan/corebanking/organization/contact/';
  const main=read(source+'OrganizationContactService.java');
  const alias=read(source+'CanonicalOrganizationContactService.java');
  const migration=read(source+'OrganizationContactMigrationReadinessController.java');
  const controller=read(source+'OrganizationContactController.java');
  const html=read('frontend/src/app/features/organization/organization-unit-contacts.component.ts');
  const r5c5=read('database/oracle/org/contact-points-retirement-r5c4/02-ORG-R5C5-CONTACT_POINTS_V2-RENAME-OPT-IN.sql');
  const verify=read('database/oracle/org/contact-points-retirement-r5c4/03-ORG-R5C5-V2-RENAME-VERIFY-READONLY.sql');
  const r5c2tests=read('backend/src/test/java/com/behsazan/corebanking/organization/contact/OrganizationUnitContactR5C2IndependenceTest.java');
  const compatibilityTests=read('backend/src/test/java/com/behsazan/corebanking/organization/contact/OrganizationContactR5V2TargetTest.java');
  const config=read('backend/src/main/resources/application.yml');
  const cmd=read('build-production.cmd'), sh=read('build-production.sh');
  test(main.includes('havingValue = "legacy", matchIfMissing = true'), 'OUCP default bean missing');
  test(main.includes('INSERT INTO ORG.ORGANIZATION_UNIT_CONTACT_POINTS'), 'OUCP create missing');
  test(main.includes('UPDATE ORG.ORGANIZATION_UNIT_CONTACT_POINTS'), 'OUCP update missing');
  test(main.includes('FROM ORG.ORGANIZATION_UNIT_CONTACT_POINTS a'), 'OUCP read missing');
  test(main.includes('ROW_VERSION=ROW_VERSION+1') && main.includes('AND ROW_VERSION=:expectedVersion'), 'Optimistic concurrency missing');
  test(main.includes('lockUnit(input.unitId())') && main.includes('FOR UPDATE OF a.CONTACT_ROLE_ID'), 'Serial write lock missing');
  test(main.includes('equal(unitFk, 0)') && main.includes('equal(serviceFk, 0)') && main.includes('equal(nullableLegacy, 1)'), 'Oracle detach gate missing');
  test(main.includes('unit-writes-enabled:false') && main.includes('requireWriteAccess()'), 'Explicit write approval missing');
  test(alias.includes('havingValue = "canonical"'), 'Legacy canonical setting no longer recognized');
  test(alias.includes('implements OrganizationContactOperations'), 'Historical canonical setting does not use OUCP');
  test(alias.includes('new OrganizationContactService(jdbc, false)'), 'Historical canonical alias MUST be read-only');
  test(!alias.includes('ORG.CONTACT_POINTS_V2') && !alias.includes('canonical-writes-enabled:false'), 'Alias still has V2 SQL/legacy write gate');
  test(migration.includes('HttpStatus.GONE') && migration.includes('ORG_V2_MIGRATION_ENDPOINT_RETIRED'), 'Historical migration API not retired');
  test(!migration.includes('NamedParameterJdbcTemplate') && !migration.includes('CONTACT_POINTS_V2'), 'Migration API still accesses Oracle V2');
  test(controller.includes('OrganizationContactOperations'), 'Operational API injection changed');
  test(html.includes('this.writesEnabled.set(state.writesEnabled !== false)'), 'UI write gate changed');
  test(config.includes('unit-writes-enabled: ${ORG_CONTACT_UNIT_WRITES_ENABLED:false}'), 'Fail-closed OUCP write setting lost');
  test(r5c2tests.includes('postDetachReadinessIsIndependentOfArchivedMaster'), 'R5-C2 independent baseline test missing');
  test(compatibilityTests.includes('oldCanonicalSettingIsReadOnlyRegardlessOfUnitWriteApproval'), 'Legacy alias safety test missing');
  test(compatibilityTests.includes('retiredMigrationEndpointReturnsGoneWithoutDatabaseAccess'), '410 regression test missing');
  for (const flag of ['APPLY_CHANGES','CONFIRM_SOURCE_CUTOVER','CONFIRM_APP_UAT',
       'CONFIRM_APP_STOPPED','CONFIRM_DBA_BACKUP','CONFIRM_EXTERNAL_CONSUMERS'])
    test(r5c5.includes(`DEFINE ${flag} = NO`), 'Unsafe R5-C5 default: '+flag);
  test(r5c5.includes('ALTER TABLE ORG.CONTACT_POINTS_V2 RENAME TO DEPRECATED_CONTACT_POINTS_V2'), 'R5-C5 rename absent');
  test(r5c5.includes("TABLE_SCHEMA='ORG'") && !r5c5.includes("FROM ALL_TAB_PRIVS\n   WHERE OWNER="), 'Oracle grants dictionary invalid');
  test(r5c5.includes('v_obj_after<>v_obj_before') && r5c5.includes('v_rows_after<>v_rows_before'), 'R5-C5 row/object preservation missing');
  test(r5c5.includes('v_fk<>0') && r5c5.includes('v_source_refs<>0') && r5c5.includes('v_programs<>0'), 'R5-C5 dependency screening incomplete');
  test(!/\b(?:DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM|INSERT\s+INTO|MERGE\s+INTO)\b/i.test(r5c5), 'Destructive SQL in opt-in rename');
  test(verify.includes('ORG_R5C5_V2_RENAME_VERIFY_PASS') && verify.includes('v_obj<>121823'), 'V2 archival verify incomplete');
  test(cmd.includes('verify-org-contact-r5c4b.mjs') && sh.includes('verify-org-contact-r5c4b.mjs'), 'Future builds lack R5-C4B guard');
  test(read('tools/smoke-org-contact-r5c4b.mjs').includes('ORG_R5C4B_RUNTIME_SMOKE_PASS'), 'Post-build runtime smoke absent');
  const mainDir=path.join(root,'backend/src/main/java');
  const files=[];
  function visit(dir){for(const entry of fs.readdirSync(dir,{withFileTypes:true})){
    if(entry.isDirectory())visit(path.join(dir,entry.name));
    else if(entry.isFile()&&entry.name.endsWith('.java'))files.push(path.join(dir,entry.name));
  }}
  visit(mainDir);
  const re=/\b(?:FROM|JOIN|INTO|UPDATE|TABLE_NAME\s*=\s*')\s*(?:ORG\.)?CONTACT_POINTS_V2\b/i;
  const matched=files.filter(f=>re.test(fs.readFileSync(f,'utf8')));
  test(matched.length===0,'Runtime Java SQL still references V2: '+matched.map(f=>path.relative(root,f)).join(','));
  console.log('ORG_R5C4B_STATIC_PASS='+passed+'; V2_RUNTIME_SQL_REFERENCES=0');
} catch(e){console.error('ORG_R5C4B_STATIC_FAIL='+e.message);process.exitCode=1;}
