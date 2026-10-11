import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const relative = 'backend/src/test/java/com/behsazan/corebanking/productbuilder/application/LoanProductFinalUxR25Test.java';
const source = fs.readFileSync(path.join(root, relative), 'utf8');
const checks = [
  ['first lambda uses a final map', source.includes('final Map<String, Object> missingUnit = repayment();')],
  ['duration test creates its own map', source.includes('missingUnit.put("MIN_TERM_DURATION", 2);')],
  ['duration lambda captures the final map', source.includes('validator.validate("LOAN_PRODUCT_REPAYMENT_RULE", missingUnit)')],
  ['partial prepayment test uses separate final map', source.includes('final Map<String, Object> partial = repayment();')],
  ['partial prepayment test changes correct flag', source.includes('partial.put("PARTIAL_PREPAYMENT_ALLOWED", 1);')],
  ['partial prepayment lambda remains', source.includes('validator.validate("LOAN_PRODUCT_REPAYMENT_RULE", partial)')],
  ['mutable reassignment removed', !source.includes('invalid = repayment();')],
  ['civil partnership test retained', source.includes('civilPartnershipIsNotEveryParticipatoryContract()')],
  ['contribution validation test retained', source.includes('invalidContributionFractionsAndOrderAreRejected()')],
  ['collateral priority test retained', source.includes('collateralRulePriorityZeroMeansUnspecifiedAndOneMeansFirst()')]
];
let passed = 0;
for (const [title, ok] of checks) {
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${title}`);
  if (ok) passed++;
}
console.log(`PB_R25_HF1_STATIC_PASS=${passed} FAIL=${checks.length-passed}`);
if (passed !== checks.length) process.exitCode = 1;
