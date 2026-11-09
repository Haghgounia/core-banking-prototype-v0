const fs = require('fs');
const path = require('path');
const root = path.resolve(process.argv[2] || '.');
const dir = path.join(root,'backend/src/main/java/com/behsazan/corebanking/collateral/lifecycle');
function load(name) { return fs.readFileSync(path.join(dir,name),'utf8'); }
const policy=load('CollateralR3LifecycleEvidencePolicy.java');
const service=load('CollateralR3LifecycleEvidenceService.java');
const controller=load('CollateralR3LifecycleEvidenceController.java');
const checks = [
  [service.includes('DataSource applicationDataSource'), 'SHARED_APPLICATION_DATASOURCE'],
  [service.includes('new JdbcTemplate(applicationDataSource)'), 'JDBC_SHARED'],
  [service.includes('SCOPE_clt.collateral.read'), 'READ_SCOPE'],
  [service.includes('introduction.actor(authentication)'), 'BRANCH_ACTOR'],
  [service.includes('c.BRANCH_CODE = ?'), 'BRANCH_SQL'],
  [service.includes('Collat') && service.includes('CollateralRegistryPolicy.id(collateralId)'), 'ID_SAFE'],
  [service.includes('Collat') && service.includes('CollateralRegistryPolicy.branch(actor.branch())'), 'BRANCH_SAFE'],
  [service.includes('FROM CLT.COLLATERAL c'), 'PHYSICAL_COLLATERAL'],
  [service.includes('FROM CLT.COLLATERAL_SPONSOR s'), 'SPONSOR_RECORDS'],
  [service.includes('FROM CLT.COLLATERAL_INSURANCE i'), 'INSURANCE_RECORDS'],
  [service.includes('FROM CLT.COLLATERAL_VALUATION_HISTORY vh'), 'VALUATION_RECORDS'],
  [service.includes('LEFT JOIN CLT.COLLATERAL_VALUATION_HISTORY v'), 'CURRENT_POINTER_EVIDENCE'],
  [service.includes('COUNT(*)') && service.includes('BRANCH_CODE'), 'NO_UNBOUNDED_ALL_ROWS'],
  [service.includes('Asia/Tehran'), 'ORACLE_LOCAL_CLOCK'],
  [service.includes('rows.size() != 1'), 'NOT_FOUND_OR_FORBIDDEN'],
  [service.includes('registry.readsEnabled()') && service.includes('reference.readsEnabled()'), 'READ_FLAGS'],
  [controller.includes('@GetMapping("/{collateralId}/r3/lifecycle-evidence")'), 'GET_ONLY_ROUTE'],
  [controller.includes('ConditionalOnProperty'), 'PROTOTYPE_GATE'],
  [!controller.includes('@PostMapping') && !controller.includes('@PutMapping'), 'NO_WRITE_ENDPOINT'],
  [!(/\b(INSERT|UPDATE|DELETE|MERGE|CREATE TABLE|ALTER TABLE|DROP TABLE)\b/.test(service) ), 'NO_DML_DDL'],
  [policy.includes('false, false,'), 'FINAL_ALLOCATION_FALSE'],
  [policy.includes('G-11_MIN_VALID_COUNT_NOT_DEFINED'), 'SPONSOR_GAP'],
  [policy.includes('G-06_POLICY_LIFECYCLE_NOT_APPROVED'), 'INSURANCE_GAP'],
  [policy.includes('G-18_REVALUATION_INTERVAL_UNIT_NOT_MODELED'), 'REVALUATION_GAP'],
  [policy.includes('"FINAL".equals(facts.currentValuationStatus())'), 'PHYSICAL_FINAL_CANONICAL'],
  [policy.includes('RECORDED_SPONSORS_ARE_NOT_CONFIRMED_VALID_SPONSORS'), 'NO_VALID_SPONSOR_INFERENCE'],
  [policy.includes('RECORDED_POLICIES_ARE_NOT_APPROVED_COVERAGE'), 'NO_VALID_INSURANCE_INFERENCE'],
  [!policy.includes('LocalDate.now('), 'NO_APP_SERVER_CLOCK'],
  [!service.includes('new DriverManagerDataSource') && !service.includes('DriverManager.getConnection'), 'NO_EXTRA_JDBC_POOL'],
  [!service.includes('"SELECT " + ') && !service.includes('" WHERE " + '), 'NO_DYNAMIC_SQL_IDENTIFIERS'],
];
let passed=0;
for (const [ok,key] of checks) {
  if (!ok) throw new Error('CLT_R3_LIFECYCLE_STATIC_FAIL='+key);
  passed++;
}
console.log('CLT_R3_LIFECYCLE_SOURCE_CONTRACT_PASS='+passed+'/'+passed);
