import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const servicePath = path.join(root, 'backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderService.java');
const buildCmdPath = path.join(root, 'build-production.cmd');
const buildShPath = path.join(root, 'build-production.sh');

const service = fs.readFileSync(servicePath, 'utf8');
const buildCmd = fs.readFileSync(buildCmdPath, 'utf8');
const buildSh = fs.readFileSync(buildShPath, 'utf8');

let pass = 0;
let fail = 0;
const check = (condition, message) => {
  if (condition) {
    pass += 1;
    console.log(`PASS | ${message}`);
  } else {
    fail += 1;
    console.error(`FAIL | ${message}`);
  }
};

check(
  service.includes('List<Map<String, Object>> normalized = requestedCriteria.stream()'),
  'eligibility criteria normalization keeps List<Map<String,Object>> contract'
);
check(
  service.includes('.<Map<String, Object>>map(row -> new LinkedHashMap<>(row))'),
  'stream mapping widens LinkedHashMap copies to Map<String,Object> before toList'
);
check(
  !service.includes('.map(LinkedHashMap::new)'),
  'invariant List<LinkedHashMap> inference regression is absent'
);
check(
  service.includes('.peek(row -> row.put("ELIGIBILITY_RULE_ID", ruleId))'),
  'rule id injection remains on normalized criterion maps'
);
check(
  service.includes('.sorted((left, right) -> Integer.compare(criterionSort(left), criterionSort(right)))'),
  'criterion ordering remains unchanged'
);
check(
  buildCmd.includes('verify-pdl-r10u-hf3-java-generic.mjs'),
  'Windows production build runs HF3 generic compile regression guard'
);
check(
  buildSh.includes('verify-pdl-r10u-hf3-java-generic.mjs'),
  'Unix production build runs HF3 generic compile regression guard'
);

console.log('------------------------------------------------------------');
console.log(`PDL_R10U_HF3_JAVA_GENERIC_PASS=${pass}`);
console.log(`PDL_R10U_HF3_JAVA_GENERIC_FAIL=${fail}`);
if (fail) process.exit(1);
console.log('PDL_R10U_HF3_JAVA_GENERIC_STATIC_PASS');
