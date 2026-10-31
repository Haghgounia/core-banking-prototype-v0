// ORG R5-C2 source-only contract. No Oracle, data writes, or network calls.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
let n=0;
const test=(condition,why)=>{if(!condition)throw new Error(why);n++;};
try {
 const service=read('backend/src/main/java/com/behsazan/corebanking/organization/contact/OrganizationContactService.java');
 const migration=read('backend/src/main/java/com/behsazan/corebanking/organization/contact/OrganizationContactMigrationReadinessController.java');
 const models=read('backend/src/main/java/com/behsazan/corebanking/organization/contact/OrganizationContactModels.java');
 const descriptor=read('backend/src/main/java/com/behsazan/corebanking/organization/descriptor/OrganizationDescriptorProvider.java');
 const form=read('frontend/src/app/features/organization/organization-unit-contacts.component.ts');
 const sql=read('database/oracle/org/contact-points-retirement-r5c2/01-ORG-R5C2-OUCP-DETACH-OPT-IN.sql');
 const verify=read('database/oracle/org/contact-points-retirement-r5c2/02-ORG-R5C2-OUCP-DETACH-VERIFY-READONLY.sql');
 const rename=read('database/oracle/org/contact-points-retirement-r5c2/03-ORG-R5C3-CONTACT_POINTS-RENAME-OPT-IN.sql');
 const renameVerify=read('database/oracle/org/contact-points-retirement-r5c2/04-ORG-R5C3-RENAME-VERIFY-READONLY.sql');
 const restore=read('database/oracle/org/contact-points-retirement-r5c2/05-ORG-R5C3-MASTER-RESTORE-OPT-IN.sql');
 test(!/\b(?:FROM|JOIN|INTO|UPDATE) ORG\.CONTACT_POINTS(?:\s|$)/m.test(service),'Direct master SQL remains in live service');
 test(service.includes('FROM ORG.ORGANIZATION_UNIT_CONTACT_POINTS a') && service.includes('a.CONTACT_VALUE'),'Read path is not OUCP snapshot');
 test(service.includes('INSERT INTO ORG.ORGANIZATION_UNIT_CONTACT_POINTS') && service.includes('CONTACT_TYPE_CODE, CONTACT_VALUE, DISPLAY_NAME_FA'),'Independent create missing');
 test(service.includes('UPDATE ORG.ORGANIZATION_UNIT_CONTACT_POINTS') && !service.includes('SET CONTACT_POINT_ID='),'Independent update missing / historic ID overwritten');
 test(service.includes('ROW_VERSION=ROW_VERSION+1') && service.includes('AND ROW_VERSION=:expectedVersion'),'Optimistic concurrency missing');
 test(service.includes('lockUnit(input.unitId())') && service.includes('FOR UPDATE OF a.CONTACT_ROLE_ID'),'Row lock missing');
 test(service.includes('requireWriteAccess()') && service.includes('unit-writes-enabled:false'),'Explicit write gate missing');
 test(service.includes('equal(unitFk, 0)') && service.includes('equal(serviceFk, 0)') && service.includes('equal(nullableLegacy, 1)'),'Oracle detach gate missing');
 test(!service.includes('ORG.SEQ_CONTACT_POINTS') && service.includes('SEQ_ORGANIZATION_UNIT_CONTACT_POINTS'),'Legacy sequence remains active');
 test(models.includes('Long contactPointId') && form.includes('contactPointId: number | null'),'Nullable historical ID contract missing');
 test(form.includes("storageMode?: 'LEGACY' | 'CANONICAL' | 'UNIT'") && form.includes('ready() && !writesEnabled()'),'Read-only UNIT view contract missing');
 test(!/\b(?:FROM|JOIN|INTO|UPDATE) ORG\.CONTACT_POINTS(?:\s|$)/m.test(migration),'Migration endpoint still selects old master');
 test(descriptor.includes('"organization-unit-contact-points".equals(item.resource())'),'Generic OUCP descriptor still exposed');
 for(const cfg of ['backend/src/main/resources/application.yml','config/application.yml'])
  test(read(cfg).includes('unit-writes-enabled: ${ORG_CONTACT_UNIT_WRITES_ENABLED:false}'),'Write mode not fail-closed: '+cfg);
 for(const name of ['APPLY_CHANGES','CONFIRM_DBA_BACKUP','CONFIRM_APP_STOPPED','CONFIRM_APP_READ_UAT','CONFIRM_EXTERNAL_CONSUMERS'])
  test(sql.includes(`DEFINE ${name} = NO`),'Detach default flag unsafe: '+name);
 test(sql.includes('DROP CONSTRAINT FK_OUCP_CONTACT_MASTER') && sql.includes('DROP CONSTRAINT FK_SPCP_CONTACT_MASTER'),'Both known master FKs must be detached');
 test(sql.includes('MODIFY (CONTACT_POINT_ID NULL)') && !/\bDELETE\s+FROM\b/i.test(sql),'Historical master ID is not kept optional');
 test(sql.includes('v_unknown_fk') && sql.includes('v_drift') && sql.includes('v_snap_required'),'Oracle detach must fail closed on bad data/dependencies');
 test(sql.includes('ORG_R5C2_OUCP_DETACH_PASS') && verify.includes('HISTORICAL_CONTACT_POINT_ID_NULLABLE'),'Detach post-verify missing');
 for(const name of ['APPLY_CHANGES','CONFIRM_SOURCE_CUTOVER','CONFIRM_APP_UAT','CONFIRM_APP_STOPPED','CONFIRM_DBA_BACKUP','CONFIRM_EXTERNAL_CONSUMERS'])
  test(rename.includes(`DEFINE ${name} = NO`),'Rename default flag unsafe: '+name);
 test(rename.includes('ORG_R5C3_CONTACT_POINTS_RENAME_PASS') && rename.includes('DEPRECATED_CONTACT_POINTS'),'Rename pass contract missing');
 test(rename.includes('v_in_fk') && rename.includes('v_stored') && rename.includes('v_jobs') && rename.includes('v_obj_after'),'Rename dependency/object screens missing');
 test(renameVerify.includes('UNMODIFIED_V2_TABLE') && restore.includes('DEFINE APPLY_CHANGES = NO'),'Verify/restore missing');
 for(const name of ['build-production.cmd','build-production.sh'])
  test(read(name).includes('verify-org-contact-r5c2.mjs'),'New verifier not wired: '+name);
 const jt=read('backend/src/test/java/com/behsazan/corebanking/organization/contact/OrganizationUnitContactR5C2IndependenceTest.java');
 test(jt.includes('postDetachReadinessIsIndependentOfArchivedMaster') && jt.includes('newWritesAreBlockedBeforeOracleDetach'),'Java tests absent');
 console.log('ORG_CONTACT_R5C2_STATIC_PASS='+n);
} catch(e) {console.error('ORG_CONTACT_R5C2_STATIC_FAIL:',e.message);process.exitCode=1;}
