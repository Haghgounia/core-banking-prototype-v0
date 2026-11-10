const fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'../../..');
function read(p){return fs.readFileSync(path.join(root,p),'utf8')}
const base='backend/src/main/java/com/behsazan/corebanking/collateral/';
const svc=read(base+'release/CollateralReleasePostingService.java');
const ctl=read(base+'release/CollateralReleasePostingController.java');
const pol=read(base+'release/CollateralReleasePostingPolicy.java');
const sec=read(base+'registry/CollateralRegistrySecurityConfiguration.java');
const ui=read('frontend/src/app/features/collateral/collateral-final-release-panel.component.html');
const ts=read('frontend/src/app/features/collateral/collateral-final-release-panel.component.ts');
const api=read('frontend/src/app/features/collateral/collateral-final-release.service.ts');
const sql=read('database/oracle/clt/releasepost/01_RELEASE_PERMIT_SCHEMA_CANDIDATE_NOT_APPROVED.sql');
let passed=0;function test(name,yes){if(!yes)throw Error('FAIL '+name);passed++}
const required=[
 ['permit gated',svc.includes('CLT_FINAL_RELEASE_PERMIT')],
 ['default off',svc.includes('posting-enabled",Boolean.class,false')],
 ['target config absent by default',svc.includes('target-status-code","")')],
 ['source config absent by default',svc.includes('source-status-code","")')],
 ['JDBC common datasource',svc.includes('DataSource dataSource, Environment env')],
 ['transaction manager',svc.includes('new DataSourceTransactionManager(dataSource)')],
 ['transaction execute',svc.includes('tx.execute(status->{')],
 ['locked collateral row',svc.includes('FOR UPDATE OF c.RECORD_VERSION')],
 ['locked permit row',svc.includes('lock?" FOR UPDATE"')],
 ['CAS version',svc.includes('AND BRANCH_CODE=? AND RECORD_VERSION=?')],
 ['status history inserted',svc.includes('INSERT INTO CLT.COLLATERAL_STATUS_HISTORY')],
 ['history pointer',svc.includes('PREVIOUS_STATUS_HISTORY_ID')],
 ['generated history key',svc.includes('GeneratedKeyHolder')],
 ['source event',svc.includes('BUC11_FINAL_RELEASE')],
 ['permit consumed',svc.includes("PERMIT_STATUS='POSTED'")],
 ['replay exact UUID',svc.includes('key.equals(p.key())')],
 ['replay poster',svc.includes('who.subject().equals(p.poster())')],
 ['replay version',svc.includes('expected.equals(p.baseVersion())')],
 ['active allocation gate',svc.includes("USED='YES'")],
 ['unknown used gate',svc.includes("USED IS NULL OR USED NOT IN ('YES','NO')")],
 ['foreign branch gate',svc.includes('COUNT(CASE WHEN BRANCH_CODE<>?')],
 ['nonterminal txn gate',svc.includes('TRANSACTION_STATUS_CODE NOT IN')],
 ['terminal approved ref',svc.includes("IS_TERMINAL_STATUS_FLAG='Y'")],
 ['scope review server',svc.includes('SCOPE_clt.collateral.review')],
 ['scope review security matcher',sec.includes('"/api/v1/collaterals/*/final-release/commit"') && sec.includes('hasAuthority("SCOPE_clt.collateral.review")')],
 ['secure branch',svc.includes('CollateralRegistryPolicy.branch(branch)')],
 ['deny prototype',svc.includes('RELEASE_PROTOTYPE_FINANCIAL_WRITE_BLOCKED')],
 ['no browser legal attestations',ctl.includes('FinalizeCommand')&&ctl.includes('Idempotency-Key')&&!ctl.includes('legalClearanceRef')],
 ['no DML on allocations',!/UPDATE\s+CLT\.COLLATERAL_ALLOCATION/i.test(svc)],
 ['no delete',!/\bDELETE\s+FROM\b/i.test(svc)],
 ['independent maker checker poster',pol.includes('f.poster().equals(f.checker())')],
 ['bank legal clearance',pol.includes('f.legalClearanceRef()')],
 ['consumer clearance',pol.includes('f.consumerClearanceRef()')],
 ['unallocation clearance',pol.includes('f.unallocationRef()')],
 ['expiry',pol.includes('f.expiresAt().isAfter(f.databaseNow())')],
 ['chain continuity',pol.includes('RELEASE_STATUS_HISTORY_CHAIN_INCONSISTENT')],
 ['schema constraints validated',svc.includes("VALIDATED='VALIDATED'")],
 ['signed request strong etag',svc.includes('CollateralReleasePostingPolicy.etag(ifMatch)')],
 ['idempotency uuid',svc.includes('CollateralReleasePostingPolicy.key(idem)')],
 ['commit route',ctl.includes('@PostMapping("/commit")')],
 ['readonly permit listing',ctl.includes('@GetMapping("/approved-permits")')],
 ['status readiness',ctl.includes('@GetMapping("/posting-readiness")')],
 ['client receipt model',api.includes('FinalReleaseReceipt')],
 ['client idempotent commit',api.includes('Idempotency-Key')],
 ['UI release action',ui.includes('finalizeRelease()')],
 ['UI status result',ui.includes('finalReceipt(); as receipt')],
 ['UI conditional enable',ui.includes('!p.postingEnabled')],
 ['UI replay stable',ts.includes('this.finalKey=key')],
 ['UI bank permit selection',ts.includes('setSelectedPermit')],
 ['no direct client approval refs',!ts.includes('approvePermit(')],
 ['sql fail-stop',sql.includes('RAISE_APPLICATION_ERROR(-20962')],
 ['sql DDL commented',sql.indexOf('/*')<sql.indexOf('CREATE TABLE CLT.CLT_FINAL_RELEASE_PERMIT')],
 ['sql no auto execution',sql.includes('not executed by installer')],
 ['docs source checkpoint',fs.existsSync(path.join(root,'docs/collateral/CLT-BUC11-FINAL-RELEASE-POSTING-2026-11-10.md'))]
];for(const [name,valid]of required)test(name,valid);
console.log('CLT_BUC11_SOURCE_CONTRACT_PASS='+passed+'/'+passed);
