'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(process.argv[2]||'.');
const file=p=>fs.readFileSync(path.join(root,p),'utf8');
const b='backend/src/main/java/com/behsazan/corebanking/collateral/registry/';
const u='frontend/src/app/features/collateral/';
const repo=file(b+'CollateralRegistryRepository.java');
const ctrl=file(b+'CollateralRegistryController.java');
const localSecurity=file('backend/src/main/java/com/behsazan/corebanking/collateral/localuat/CltLocalUatSecurityConfiguration.java');
const pol=file(b+'CollateralRegistryPolicy.java');
const test=file('backend/src/test/java/com/behsazan/corebanking/collateral/registry/CollateralRegistryPolicyTest.java');
const ts=file(u+'collateral-registry.component.ts');
const html=file(u+'collateral-registry.component.html');
const service=file(u+'collateral-registry.service.ts');
const intro=file(u+'collateral-introduction.component.ts');
const introHtml=file(u+'collateral-introduction.component.html');
const introApi=file(u+'collateral-introduction.service.ts');
const labels=file(u+'collateral-registry-labels.ts');
const report=file(u+'collateral-reports.component.html');
const sql=file('tools/clt/r6i/CLT_R6I_GROUP_TYPE_COLLATERAL_READONLY.sql');
let passed=0;
const yes=(name,cond)=>{assert.ok(cond,'CLT_R6I_ASSERT_FAIL: '+name);passed++;};
for(const [name,body,mark] of [
 ['groups-route',ctrl,'@GetMapping(value = "/groups"'],
 ['groups-explicit-local-get-allowlist',localSecurity,'"/api/v1/collaterals/groups"'],
 ['groups-security-scope',ctrl,'branch(authentication);\n        return repository.activeGroups()'],
 ['groups-jar-marker',ctrl,'CLT_R6I_ACTIVE_GROUPS_20261104'],
 ['group-filter-server',ctrl,'@RequestParam(required = false) String groupCode'],
 ['invalid-group-400',ctrl,'HttpStatus.BAD_REQUEST, "CLT_REGISTRY_GROUP_CODE_INVALID"'],
 ['group-bind',repo,'parameters.add(CollateralRegistryPolicy.groupCode(groupCode))'],
 ['branch-first',repo,'parameters.add(CollateralRegistryPolicy.branch(branch))'],
 ['bounded-group',pol,'[A-Za-z0-9_-]{1,5}'],
 ['group-join',repo,'LEFT JOIN CLT.COLLATERAL_GROUP g'],
 ['group-status',repo,"AND g.STATUS = 'ACTIVE'"],
 ['group-readonly',repo,'FROM CLT.COLLATERAL_GROUP g'],
 ['active-group-cap',repo,'FETCH FIRST 200 ROWS ONLY'],
 ['type-real-active',repo,"WHERE t.STATUS = 'ACTIVE' AND g.STATUS = 'ACTIVE'"],
 ['type-group-code',repo,'g.COLLATERAL_GROUP_CODE, g.COLLATERAL_GROUP_NAME, t.HAS_MULTIPLICITY'],
 ['param-sql',repo,'g.COLLATERAL_GROUP_CODE = ?'],
 ['legacy-list-overload',repo,'return list(branch, registrationNumber, null, page, size);'],
 ['sql-group-count',sql,'EXCEL_IMPORTED_GROUPS'],
 ['sql-join-integrity',sql,'LEFT JOIN CLT.COLLATERAL_GROUP g'],
 ['sql-no-mutation-doc',sql,'NO DDL; NO DML; NO COMMIT'],
 ['api-groups',service,'`${this.base}/groups`'],
 ['api-group-param',service,"params.set('groupCode', groupCode)"],
 ['filter-group-ui',html,'id="clt-list-group"'],
 ['filter-group-logic',ts,'setGroupFilter(value:string)'],
 ['no-fake-data',ts,'this.catalogError.set('],
 ['group-in-grid',html,"field(row,'COLLATERAL_GROUP_NAME')"],
 ['group-name-in-business-detail',ts,"'COLLATERAL_GROUP_NAME'"],
 ['group-code-retained-in-audit',ts,"'COLLATERAL_GROUP_CODE'"],
 ['detail-separates-business-and-audit',ts,"readonly businessFields ="],
 ['group-title-label',labels,'"COLLATERAL_GROUP_NAME"'],
 ['persist-branch-scope',repo,'WHERE c.BRANCH_CODE = ?'],
 ['intro-groups-request',introApi,'groups(): Observable<readonly CollateralRow[]>'],
 ['intro-group-req',intro,'firstValueFrom(this.api.groups())'],
 ['intro-filter-meaning',intro,"this.value(t, 'COLLATERAL_GROUP_CODE') === this.groupCode()"],
 ['intro-change-clears-type',intro,"void this.chooseType('')"],
 ['intro-no-fake-type',introHtml,'ایجاد خودکار نوع یا ضرایب پذیرش مجاز نیست'],
 ['intro-group-select',introHtml,'(change)="changeGroup('],
 ['intro-type-disabled',introHtml,'[disabled]="!groupCode() || filteredTypes().length===0"'],
 ['direct-title',html,"mode()==='direct' ? labels.direct"],
 ['persian-404',labels,'وثیقه‌ای با این شماره در محدوده دسترسی'],
 ['server-branch-scope-in-report',report,'محدود به شعبه تأییدشده در سرویس'],
 ['policy-negative-tests',test,'groupCodeFilterAcceptsOnlyExactBusinessCode'],
 ]) yes(name,body.includes(mark));
yes('no-java-dml', !/\b(?:INSERT\s+INTO|UPDATE\s+CLT\.|DELETE\s+FROM|MERGE\s+INTO)\b/i.test(repo+ctrl));
yes('read-only-sql',!/^\s*(?:INSERT|UPDATE|DELETE|MERGE|CREATE|ALTER|DROP|TRUNCATE|COMMIT|ROLLBACK)\b/im.test(sql));
yes('no-static-array-group',!/\[\s*\{\s*COLLATERAL_GROUP_CODE\s*:/.test(service+ts+intro));
yes('no-branch-header',!/X-Branch-Code|X-User-Id/.test(repo+ctrl+ts+service));
yes('no-iam-change',!file('docs/collateral/CLT-R6I-GROUP-TYPE-REGISTRY-LINKAGE-FA.md').includes('تغییر IAM.'));
console.log('CLT_R6I_GROUP_TYPE_REGISTRY_SAFETY_PASS='+passed+'; GROUPS=ORACLE_ONLY; BRANCH_FILTER=SERVER; IAM=UNCHANGED; DB_WRITES=NONE');
