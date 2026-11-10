'use strict';
// Deterministic source and business-boundary verification. NO Oracle connection or mutation.
const fs=require('node:fs'), path=require('node:path'), assert=require('node:assert/strict');
const root=path.resolve(__dirname,'../../..');
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const main='backend/src/main/java/com/behsazan/corebanking/collateral/';
const ui='frontend/src/app/features/collateral/';
const src={repo:read(main+'registry/CollateralRegistryRepository.java'),
 ctrl:read(main+'registry/CollateralRegistryController.java'),
 policy:read(main+'registry/CollateralRegistryPolicy.java'),
 config:read(main+'introduction/CollateralIntroductionConfiguration.java'),
 writer:read(main+'introduction/CollateralIntroductionWriterPool.java'),
 introRepo:read(main+'introduction/CollateralIntroductionRepository.java'),
 introSvc:read(main+'introduction/CollateralIntroductionService.java'),
 sec:read(main+'prototype/CltUnifiedPrototypeSecurityConfiguration.java'),
 overlay:read('config/clt-unified-local.yml'),
 registryTS:read(ui+'collateral-registry.component.ts'),
 registryHTML:read(ui+'collateral-registry.component.html'),
 api:read(ui+'collateral-registry.service.ts'),
 introTS:read(ui+'collateral-introduction.component.ts'),
 utility:read(ui+'collateral-presentation.ts')};
let passed=0;const good=(name,yes)=>{assert.ok(yes,`CLT_R2_R3_CONTRACT_FAIL: ${name}`);passed++;console.log('PASS | '+name);};
for(const k of ['partyId','typeId','statusCode']){
 good(k+' parameter accepted by backend',src.ctrl.includes('@RequestParam(required = false) String '+k));
 good(k+' service HTTP binding',src.api.includes("params.set('"+k+"', "+k+")"));
 good(k+' has strict SQL bind',src.repo.includes('parameters.add(CollateralRegistryPolicy.'+(k==='statusCode'?'statusCode':'id')+'('+k+'))'));
}
good('branch remains first mandatory query parameter',src.repo.includes('parameters.add(CollateralRegistryPolicy.branch(branch))'));
good('no dynamic table or filter SQL input',!/(?:COLLATERAL_NUMBER|PARTY_ID|COLLATERAL_TYPE_ID|COLLATERAL_STATUS_CODE)\s*=\s*['"`]\s*\+/.test(src.repo));
good('status labels resolved from real reference table',src.repo.includes('CLT.REF_COLLATERAL_STATUS')&&src.repo.includes('s.COLLATERAL_STATUS_NAME'));
good('status select server-scoped',src.ctrl.includes('branch(authentication);\n        return repository.activeStatuses()'));
good('type and status controls use reference choices',src.registryHTML.includes('id="clt-list-type"')&&src.registryHTML.includes('id="clt-list-status"'));
good('Party filter uses decimal validation',src.registryTS.includes('cltValidNumber(party)')&&src.registryTS.includes('cltNormalizedNumber(party)'));
good('server-side filters survive pagination',src.registryTS.includes('this.appliedPartyFilter(), this.typeFilter(), this.statusFilter()'));
good('business-first profile with hidden system fields',src.registryHTML.includes('<details class="clt-profile-audit">')&&src.registryTS.includes('readonly businessFields')&&src.registryTS.includes('readonly systemFields'));
good('date presentation separate from API values',src.utility.includes('Date.UTC')&&src.utility.includes("'fa-IR-u-ca-persian'"));
good('staging drafts share existing datasource',src.config.includes('new CollateralIntroductionWriterPool(dataSource)')&&src.writer.includes('DataSource applicationDataSource'));
good('shared datasource never closed by staging writer',src.writer.includes('if (pool != null) pool.close()'));
good('shared writer restricted to local loopback',src.config.includes('CLT_INTRODUCTION_SHARED_WRITER_LOOPBACK_ONLY'));
good('draft-only flag declared in local overlay',src.overlay.includes('CLT_DRAFT_WRITES_ENABLED')&&src.overlay.includes('review-workflow-enabled: false'));
good('staging schema and sequence checked before write',src.introRepo.includes('ALL_TABLES')&&src.introRepo.includes('ALL_SEQUENCES')&&src.introRepo.includes('ALL_TAB_COLUMNS'));
good('readiness reflects staging metadata',src.introSvc.includes('repository.draftSchemaReady(staging)'));
good('read-back proof required before claiming draft persisted',src.introTS.includes('cltPersistedDraftMatches(persisted, draft)'));
good('draft exact POST allowlist',src.sec.includes('HttpMethod.POST, "/api/v1/collaterals/introduction-drafts").permitAll()'));
good('no final collateral POST enabled',src.sec.includes('.anyRequest().denyAll()')&&!src.sec.includes('HttpMethod.POST, "/api/v1/collaterals"'));
good('review workflow not enabled in local mode',src.overlay.includes('review-workflow-enabled: false'));
good('source has no new Oracle DDL/DML installer',!src.config.includes('CREATE TABLE')&&!src.overlay.includes('writer-jdbc-password'));
const catalog=read(ui+'clt-reference-catalog.ts');
const names=[...catalog.matchAll(/"table": "([A-Z_]+)"/g)].map(x=>x[1]);
good('reference catalog is exactly 17 physical unique tables',names.length===17&&new Set(names).size===17);
console.log(`CLT_R2_R3_SOURCE_PASS=${passed}/${passed}; ORACLE_CRUD=NOT_RUN; ANGULAR_BUILD=NOT_RUN`);
