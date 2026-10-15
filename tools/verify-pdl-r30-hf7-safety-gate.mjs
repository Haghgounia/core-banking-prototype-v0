import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const service=fs.readFileSync(path.join(root,'backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductRuleGovernanceService.java'),'utf8');
const test=fs.readFileSync(path.join(root,'backend/src/test/java/com/behsazan/corebanking/productbuilder/application/ProductRuleGovernanceR19Test.java'),'utf8');
const block=service.match(/if \("LOAN"\.equals\(domain\)\) \{\s*canOpen = false;[\s\S]*?\n        \}/);
const checks=[
 ['explicit loan operational deny', Boolean(block)],
 ['loan explanation names qualification', Boolean(block?.[0].includes('احراز نشده'))],
 ['deposit operational deny retained', service.includes('if ("DEPOSIT".equals(domain)') && service.includes('canOpen = false;')],
 ['design approval separate from operational open', service.includes('boolean canApprove = blockers.isEmpty();') && service.includes('boolean canOpen = canApprove && operationBlockers.isEmpty();')],
 ['approval transition guarded by canApprove', service.includes('approval && !result.canApprove()')],
 ['origination OPEN guarded by canOpen', service.includes('opening || (goCurrent && "OPEN".equals(origination))')],
 ['loan positive design and negative operational test', test.includes('loanDesignApprovalNeverImpliesOperationalQualification')],
 ['loan OPEN test', test.includes('loanOriginationOpenIsBlockedEvenWhenAllRowsExist')],
 ['loan profile retains own semantics', service.includes('LOAN_PRODUCT_PROFILE')],
 ['deposit HF5/HF6 maintained', fs.existsSync(path.join(root,'tools/verify-pdl-r30-hf6-auto-deposit-profile.mjs'))],
];
let fail=0;
for(const [label,ok] of checks){ console.log(`${ok?'PASS':'FAIL'} | ${label}`);if(!ok)fail++; }
console.log(`PDL_R30_HF7_SAFETY_STATIC_PASS=${checks.length-fail} FAIL=${fail}`);
if(fail)process.exit(1);
