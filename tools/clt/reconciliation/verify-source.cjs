const fs=require('fs');
const path=require('path');
const root=path.resolve(__dirname,'../../..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const service=read('backend/src/main/java/com/behsazan/corebanking/collateral/reconciliation/CollateralOracleReconciliationService.java');
const controller=read('backend/src/main/java/com/behsazan/corebanking/collateral/reconciliation/CollateralOracleReconciliationController.java');
const policy=read('backend/src/main/java/com/behsazan/corebanking/collateral/reconciliation/CollateralOracleReconciliationPolicy.java');
const ui=read('frontend/src/app/features/collateral/collateral-business-uat.component.ts');
const html=read('frontend/src/app/features/collateral/collateral-business-uat.component.html');
const api=read('frontend/src/app/features/collateral/collateral-business-uat.service.ts');
const sql=read('database/oracle/clt/reconciliation/00_CLT_ORACLE_REFERENCE_READONLY.sql');
let checks=0;
function test(ok,name){if(!ok)throw Error('CLT_P1_STATIC_FAIL_'+name);checks++;}
test(service.includes('DataSource dataSource')&&service.includes('new JdbcTemplate(dataSource)'), 'SHARED_DATASOURCE');
test(service.includes('setQueryTimeout(3)'), 'BOUNDED_PROBE');
test(controller.includes('JwtAuthenticationToken')&&controller.includes('SCOPE_clt.collateral.read'), 'SIGNED_READ_SCOPE');
test(controller.includes('CollateralRegistryPolicy.branch(value)'), 'SERVER_BRANCH_VALIDATION');
test(service.includes('WHERE BRANCH_CODE = ?')&&service.includes('Long.class, branch'), 'BRANCH_BOUND');
test(policy.includes('REFERENCE_TABLES.size() != 17'), 'FIXED_17');
test(service.includes('FROM ALL_TABLES')&&service.includes('FROM ALL_SEQUENCES'), 'ORACLE_DICTIONARY');
test(service.includes('FROM ALL_TAB_COLUMNS')&&service.includes('FROM ALL_CONSTRAINTS'), 'NUMBER_SCHEMA_CHECK');
test(service.includes('DATA_DEFAULT')&&service.includes('PENDING_APPROVAL'), 'G16_STATUS_CHECK');
test(policy.includes('OBSERVED_EMPTY')&&policy.includes('UNKNOWN'), 'ZERO_VERSUS_UNKNOWN');
test(service.includes('boolean bankAccepted')&&service.includes(' false,'), 'NO_BANK_APPROVAL_CLAIM');
test(html.includes('data-clt-oracle-reconciliation')&&html.includes('report.references'), 'EXISTING_ANALYST_UI');
test(api.includes('reconciliationEndpoint')&&api.includes('http.get<unknown>'), 'GET_ONLY_API');
test(ui.includes('collateralValidateOracleReconciliation')&&ui.includes('exportReconciliation'), 'CONTRACT_AND_EXPORT');
for(const [name,content] of [['Java service',service],['Java controller',controller],['SQL',sql]]){
 const sanitized=content.replace(/\/\*[\s\S]*?\*\//g,'').replace(/\/\/[^\n]*/g,'').replace(/^\s*--[^\n]*/gm,'');
 test(!/\b(INSERT\s+INTO|UPDATE\s+\w+\s+SET|DELETE\s+FROM|MERGE\s+INTO|CREATE\s+TABLE|ALTER\s+TABLE|DROP\s+TABLE|TRUNCATE\s+TABLE|COMMIT\s*;|EXECUTE\s+IMMEDIATE)\b/i.test(sanitized),name+' READONLY');
}
for(const fname of ['CLT-ROADMAP-R1.0-FA.md','CLT-GAP-REGISTER-R1.0-FA.md','CLT-EXECUTION-BASELINE-2026-11-10-FA.md']){
 test(read('docs/collateral/'+fname).includes('CLT-EXECUTION-BASELINE-2026-11-10'),fname+' SYNC');
}
for(const old of ['collateral-insurance-panel.component.html','collateral-sponsor-panel.component.html','collateral-valuation-panel.component.html']){
 test(fs.existsSync(path.join(root,'frontend/src/app/features/collateral',old)),'PRESERVE_'+old);
}
console.log('CLT_P1_RECONCILIATION_STATIC_PASS='+checks+'/'+checks);
