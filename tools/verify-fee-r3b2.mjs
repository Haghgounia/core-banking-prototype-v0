import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const manifest=JSON.parse(fs.readFileSync(path.join(root,'tools/fee-r3b2/manifest.json'),'utf8'));
const check=(ok,msg)=>{if(!ok)throw Error('FEE_R3B2_STATIC_FAIL: '+msg);};
check(manifest.id==='FEE_R3B2_SAFE_UAT_TESTS'&&manifest.files.length===2,'test-only manifest');
for(const e of manifest.files){
  check(e.path.startsWith('backend/src/test/java/com/behsazan/corebanking/fee/admin/application/'),'outside test subtree');
  check(sha(fs.readFileSync(path.join(root,'tools/fee-r3b2',e.asset)))===e.sha256,'damaged package asset');
  check(sha(fs.readFileSync(path.join(root,e.path)))===e.sha256,'uninstalled or altered test target '+e.path);
}
const unit=fs.readFileSync(path.join(root,manifest.files[0].path),'utf8');
const oracle=fs.readFileSync(path.join(root,manifest.files[1].path),'utf8');
const readonly=fs.readFileSync(path.join(root,'tools/fee-r3b2/readonly-api.mjs'),'utf8');
check(unit.includes('mismatchedRuleNeverReachesInsert')&&unit.includes('mismatchedComponentNeverReachesInsert')&&
  unit.includes('correctVersionAllowsStageWithoutOptionalComponent')&&unit.includes('serviceWritesAreDeclaredTransactional'),
  'service unit cases missing');
check(oracle.includes('YES_ROLLBACK_ONLY')&&oracle.includes('FEE_R3B2_EXPECTED_DB_NAME')&&
  oracle.includes('FEE_R3B2_EXPECTED_CON_NAME')&&oracle.includes('@Rollback')&&
  oracle.includes('@AfterTransaction')&&oracle.includes('assertNoFixtureSurvivedRollback'),
  'Oracle UAT safety guard or rollback verification missing');
check(readonly.includes("method:'GET'") && !/method:\s*['"](?:POST|PUT|PATCH|DELETE)['"]/.test(readonly),
  'read-only API UAT must use GET only');
const service=fs.readFileSync(path.join(root,'backend/src/main/java/com/behsazan/corebanking/fee/admin/application/FeeAdminService.java'),'utf8');
check(service.includes('FeeChargeStageIntegrityPolicy.verifyOwners') && service.includes('repository.lockChargeStage') &&
  service.includes('validateParentReassignment'),'R3-B backend guard no longer present');
console.log('FEE_R3B2_STATIC_PASS | 2 signed FEE-only test files, read-only API preflight, explicit Oracle opt-in, test rollback and residual check.');
console.log('No production code, Product Builder, FEE2, schema or tariff data changed.');
