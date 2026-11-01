// ORG R5-B canonical read-only pilot and R6 relationship-rule retirement contract.
// No Oracle connection and no network calls. Build-safe checks only.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = name => fs.readFileSync(path.join(root, name), 'utf8');
const assert = (cond, msg) => { if (!cond) throw new Error(msg); };
let n = 0;
const check = (cond, msg) => { assert(cond,msg); n++; };
try {
  const d=read('backend/src/main/java/com/behsazan/corebanking/organization/descriptor/OrganizationDescriptorProvider.java');
  const refs=read('backend/src/main/java/com/behsazan/corebanking/referencedata/management/application/ReferenceService.java');
  const route=read('frontend/src/app/app.routes.ts');
  const ui=read('frontend/src/app/features/reference-data/presentation/reference-page.component.ts');
  const contacts=read('backend/src/main/java/com/behsazan/corebanking/organization/contact/CanonicalOrganizationContactService.java');
  const form=read('frontend/src/app/features/organization/organization-unit-contacts.component.ts');
  const launcher=read('bin/start-org-contacts-canonical-readonly.cmd');
  const smoke=read('tools/verify-org-contact-r5b-runtime.ps1');
  const sql=read('database/oracle/org/relationship-rules-retirement-r6/01-ORG-R6-RELATIONSHIP-RULES-RENAME-OPT-IN.sql');
  const verify=read('database/oracle/org/relationship-rules-retirement-r6/02-ORG-R6-RELATIONSHIP-RULES-VERIFY-READONLY.sql');
  const restore=read('database/oracle/org/relationship-rules-retirement-r6/03-ORG-R6-RELATIONSHIP-RULES-RESTORE-OPT-IN.sql');
  check(!d.includes('organizationUnitRelationshipRules()') && !d.includes('schemaName, "ORGANIZATION_UNIT_RELATIONSHIP_RULES"'), 'Rule table descriptor still published');
  check(d.includes('organizationUnitRelationships()') && d.includes('organizationUnitRelationshipTypes()'), 'Active relationship/type descriptors removed');
  check(refs.includes('if ("organization-unit-relationship-rules".equals(resource))'), 'Legacy URL write guard missing');
  check(route.includes("path: 'organization/organization-unit-relationship-rules', redirectTo: 'organization'") &&
    route.indexOf("path: 'organization/organization-unit-relationship-rules'")<route.indexOf("path: 'organization/:resource'"), 'Legacy route not redirected before generic route');
  check(!ui.includes("'organization-unit-relationship-rules':"), 'Retired advanced filters remain');
  check(contacts.includes('implements OrganizationContactOperations') && contacts.includes('havingValue = "canonical"') && !contacts.includes('CONTACT_POINTS_V2'), 'Historical canonical mode must alias OUCP');
  check(contacts.includes('new OrganizationContactService(jdbc, false)'), 'Canonical unit write fail-close removed');
  check(form.includes("ready() && !writesEnabled()") && form.includes('[disabled]="saving() || !writesEnabled()"'), 'Read-only Angular UI not guarded');
  check(launcher.includes('set "ORG_CONTACT_STORAGE_MODE=canonical"') && launcher.includes('set "ORG_CONTACT_CANONICAL_WRITES_ENABLED=false"'), 'Read-only launcher not fail closed');
  check(smoke.includes("$state.storageMode -ne 'CANONICAL'"), 'Historic R5-B smoke evidence was unexpectedly rewritten');
  check(sql.includes('DEFINE APPLY_CHANGES = NO') && sql.includes('DEFINE CONFIRM_APP_UAT = NO') &&
    sql.includes('DEFINE CONFIRM_DBA_BACKUP = NO') && sql.includes('DEFINE CONFIRM_EXTERNAL_CONSUMERS = NO') && sql.includes('DEFINE CONFIRM_APP_STOPPED = NO'), 'R6 manual gates are not disabled by default');
  check(sql.includes('DEPRECATED_ORGANIZATION_UNIT_RELATIONSHIP_RULES') &&
    sql.includes('EXECUTE IMMEDIATE') && sql.includes('ALTER TABLE ORG.'), 'R6 rename logic missing');
  check(sql.includes('v_in_fk') && sql.includes('v_stored') && sql.includes('v_synonyms') &&
    sql.includes('v_triggers') && sql.includes('v_source_refs') && sql.includes('v_jobs'), 'R6 dependency screening incomplete');
  check(sql.includes('v_object_after<>v_object_before') && sql.includes('v_rows_after<>v_rows_before'), 'R6 post-rename preservation checks missing');
  check(sql.includes('ORG_R6_RULES_PREVIEW_COMPLETE') && sql.includes('ORG_R6_RULES_DEPRECATION_PASS'), 'R6 proof markers missing');
  check(verify.includes('ORG_R6_RULES_VERIFY_PASS') && verify.includes('ACTIVE_RELATION_TABLES='), 'R6 read-only verify missing');
  check(restore.includes('DEFINE APPLY_CHANGES = NO') && restore.includes('ORG_R6_RULES_RESTORE_PREVIEW_COMPLETE'), 'R6 restore requires preview');
  const uncommented = sql.replace(/^\s*--.*$/gm,'').replace(/^\s*PROMPT.*$/gm,'');
  check(!/\b(?:DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM|INSERT\s+INTO|MERGE\s+INTO|COMMIT\s*;)\b/i.test(uncommented), 'R6 contains destructive DDL or DML');
  const test=read('backend/src/test/java/com/behsazan/corebanking/organization/descriptor/OrganizationR6RelationshipRulesRetirementTest.java');
  check(test.includes('assertEquals(23, descriptors.size())') && test.includes('ORGANIZATION_UNIT_RELATIONSHIPS'), 'R6 Java descriptor test missing');
  const cmd=read('build-production.cmd'), sh=read('build-production.sh');
  check(cmd.includes('verify-org-r5b-r6.mjs') && sh.includes('verify-org-r5b-r6.mjs'), 'R5B R6 checks not wired to both builds');
  console.log(`ORG_R5B_R6_STATIC_PASS=${n}`);
} catch (e) {
  console.error('ORG_R5B_R6_STATIC_FAIL:',e.message);
  process.exitCode=1;
}
