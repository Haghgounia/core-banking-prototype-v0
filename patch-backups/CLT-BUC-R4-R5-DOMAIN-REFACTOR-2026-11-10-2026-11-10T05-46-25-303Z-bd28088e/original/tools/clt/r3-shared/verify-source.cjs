'use strict';
const fs = require('fs'), path=require('path');
const base=path.resolve(__dirname,'../../..');
const src=p=>fs.readFileSync(path.join(base,p),'utf8');
let n=0;
function ok(label,test){if(!test)throw Error('CLT_R3_SHARED_CONTRACT_FAIL='+label); n++; console.log('PASS | '+label);}
const root='backend/src/main/java/com/behsazan/corebanking/collateral/';
const policy=src(root+'prototype/CltR3SharedDatasourcePolicy.java');
ok('loopback required',policy.includes('"127.0.0.1".equals(bindAddress)'));
ok('non-local mode remains separated',policy.includes('if (!prototypeEnabled) return false;'));
for(const kind of ['Sponsor','Insurance','Valuation']) {
 const lower=kind.toLowerCase();
 const pool=src(root+lower+'/Collateral'+kind+'WriterPool.java');
 const config=src(root+lower+'/Collateral'+kind+'Configuration.java');
 ok(kind+' common app datasource constructor',pool.includes('Collateral'+kind+'WriterPool(javax.sql.DataSource applicationDataSource)'));
 ok(kind+' shared transaction manager',pool.includes('new DataSourceTransactionManager(applicationDataSource)'));
 ok(kind+' non-close shared datasource',pool.includes('if ('+(kind==='Valuation'?'datasource':'pool')+' != null) '+(kind==='Valuation'?'datasource':'pool')+'.close()'));
 ok(kind+' still accepts historical explicit writer',pool.includes('Collateral'+kind+'WriterPool(Collateral'+kind+'Properties properties)'));
 ok(kind+' environment prototype gate',config.includes('CltR3SharedDatasourcePolicy.useApplicationDatasource('));
 ok(kind+' scoped opt-in property',config.includes('name = "request-writes-enabled"'));
 ok(kind+' common datasource injection',config.includes('DataSource applicationDataSource'));
 ok(kind+' return of shared writer',config.includes('new Collateral'+kind+'WriterPool(applicationDataSource)'));
 ok(kind+' no Spring IAM integration changes',!config.includes('JwtDecoder'));
}
const config=src('config/clt-unified-local.yml');
for (const [kind,prefix] of [['sponsor','SPONSOR'],['insurance','INSURANCE'],['valuation','VALUATION']]) {
 ok(kind+' stage writes unified config',config.includes('CLT_'+prefix+'_REQUEST_WRITES_ENABLED:${CLT_UNIFIED_ENABLED:true}'));
}
ok('no database passwords embedded in overlay',!config.includes('jdbc-password:'));
const component=src('frontend/src/app/features/collateral/collateral-r3-draft-worklist.component.ts');
const html=src('frontend/src/app/features/collateral/collateral-r3-draft-worklist.component.html');
for (const kind of ['sponsor','insurance','valuation']) {
 ok(kind+' stage worklist API',component.includes('this.'+(kind==='sponsor'?'sponsors':kind==='insurance'?'insurances':'valuations')+'.mine(nextPage)'));
 const parent=src('frontend/src/app/features/collateral/collateral-'+kind+'-panel.component.ts');
 const parentHtml=src('frontend/src/app/features/collateral/collateral-'+kind+'-panel.component.html');
 ok(kind+' worklist imported',parent.includes('CollateralR3DraftWorklistComponent'));
 ok(kind+' worklist embedded',parentHtml.includes('kind="'+kind+'"'));
 ok(kind+' only when Stage approved',parentHtml.includes('stagingSchemaVerified === true'));
 ok(kind+' post create refresh',parent.includes('this.r3RequestsRefresh.update(value => value + 1)'));
}
ok('user/branch scope comes from API',html.includes('کاربر و شعبه جاری'));
ok('no generic ad hoc HTTP request in worklist',!component.includes('HttpClient'));
ok('worklist explicit not final',html.includes('پیش‌نویس؛ نهایی‌نشده'));
ok('insurance preview remains read-only',src('frontend/src/app/features/collateral/collateral-insurance-panel.component.html').includes('@if (!analystPreview)'));
ok('insurance typed mine',src('frontend/src/app/features/collateral/collateral-insurance.service.ts').includes('/insurance-policies/requests/mine'));
ok('valuation typed mine',src('frontend/src/app/features/collateral/collateral-valuation.service.ts').includes('/valuations/requests/mine'));
ok('roadmap is same versioned R0-R7',src('docs/collateral/CLT-ROADMAP-R1.0-FA.md').includes('R0 تا R7'));
ok('roadmap records R3 checkpoint',src('docs/collateral/CLT-ROADMAP-R1.0-FA.md').includes('CLT-R3-SHARED-DATASOURCE-INTAKE-2026-11-09'));
ok('gap register keeps unapproved changes',src('docs/collateral/CLT-GAP-REGISTER-R1.0-FA.md').includes('SQLهای موجود صرفاً Candidate/Fail-Closed'));
console.log('CLT_R3_SHARED_SOURCE_PASS='+n+'/'+n);
