import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const p = value => path.join(root, value);
const testPrefix = 'backend/src/test/java/com/behsazan/corebanking/productbuilder/application/';
const app = 'backend/src/main/java/com/behsazan/corebanking/productbuilder/';
const mustExist = [
  testPrefix + 'ProductGovernanceStopOnlyR19Test.java',
  testPrefix + 'ProductRuleEvidenceR19Test.java',
  testPrefix + 'ProductRuleGovernanceR19Test.java',
  app + 'application/ProductRuleEvidenceEvaluator.java',
  app + 'application/ProductRuleGovernanceService.java',
  app + 'application/ProductGovernanceWriteGuard.java',
  app + 'web/ProductBuilderController.java',
  'tools/verify-pb-r19.mjs',
  'tools/verify-rgl-lite-r20.mjs'
];
const criticalSignatures = [
  [app + 'application/ProductGovernanceWriteGuard.java', 'assertNonDraftVersionStopOnly'],
  [app + 'application/ProductGovernanceWriteGuard.java', 'Set.of("CLOSED", "SUSPENDED")'],
  [app + 'web/ProductBuilderController.java', '/versions/{versionId}/rule-governance'],
  ['frontend/src/app/features/product-builder/product-workspace.component.html', 'governanceSourceFa(rule.source)'],
];
let errors = 0, warnings = 0;
for (const file of mustExist) {
  if (fs.existsSync(p(file))) console.log('PASS FILE ' + file);
  else { console.error('FAIL MISSING ' + file); errors++; }
}
for (const [file, text] of criticalSignatures) {
  if (!fs.existsSync(p(file))) continue;
  if (fs.readFileSync(p(file), 'utf8').includes(text)) console.log('PASS CONTRACT ' + file + ' :: ' + text);
  else { console.error('WARN R19 contract not found ' + file + ' :: ' + text); warnings++; }
}
console.log(`PB_R20_HF2_BASELINE_ERRORS=${errors} WARNINGS=${warnings}`);
if (errors || warnings) process.exitCode = 1;
