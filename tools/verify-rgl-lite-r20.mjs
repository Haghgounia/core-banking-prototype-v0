import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const get=(rel)=>fs.readFileSync(path.join(root,rel),'utf8');
const base='backend/src/main/java/com/behsazan/corebanking/productbuilder/';
const policy=get(base+'application/RuleGovernanceLiteService.java');
const controller=get(base+'web/RuleGovernanceLiteController.java');
const governance=get(base+'application/ProductRuleGovernanceService.java');
const evidence=get(base+'application/ProductRuleEvidenceEvaluator.java');
const service=get(base+'application/ProductBuilderService.java');
const catalog=get(base+'application/PdlCatalog.java');
const page=get('frontend/src/app/features/product-builder/rule-policy-lite.component.html');
const component=get('frontend/src/app/features/product-builder/rule-policy-lite.component.ts');
const scss=get('frontend/src/app/features/product-builder/rule-policy-lite.component.scss');
const workspace=get('frontend/src/app/features/product-builder/product-workspace.component.html');
const model=get('frontend/src/app/features/product-builder/product-builder.models.ts');
const angularApi=get('frontend/src/app/features/product-builder/product-builder.service.ts');
const oracle=fs.readFileSync(path.join(root,'docs/oracle/rgl-lite-installed-contract.sql'),'utf8');
const checks=[
 ['two Oracle policy tables only', (oracle.match(/CREATE TABLE PDL\./g)||[]).length===0 && oracle.includes("'RULE_POLICY_VERSION','RULE_CONTROL_POLICY'")],
 ['no new general rule DSL engine',!get(base+'application/RuleGovernanceLiteService.java').includes('EVAL(') && !policy.includes('Class.forName')],
 ['23 explicit baseline controls', governance.match(/\bc\("[A-Z_]+",/g)?.length===23],
 ['policy CRUD uses explicit SQL not generic catalog',!catalog.includes('register("RULE_CONTROL_POLICY"') && !catalog.includes('register("RULE_POLICY_VERSION"')],
 ['policy value SQL is parametrized', policy.includes('.param("code",code)') && policy.includes('.param("policyId",request.policyId())')],
 ['policy only references configured PDL schema', policy.includes('this.pdl = schema') && policy.includes('schema.matches')],
 ['server validates rule-control whitelist',policy.includes('knownCodes.contains(c.code())')],
 ['server validates duplicate control codes',policy.includes('unique.add(c.code())')],
 ['policy draft seeds all controls',policy.includes('for (ProductRuleGovernanceService.RuleControl control : ProductRuleGovernanceService.defaultControls())')],
 ['clone creates new version',policy.includes('clonePolicy(long sourceId') && policy.includes('createVersion(source.code()')],
 ['draft-only policy changes',policy.includes('ensureDraft(id)')],
 ['approved version cannot be edited',policy.includes('نسخه مصوب قابل ویرایش نیست')],
 ['Maker/Checker separation',policy.includes('checker.equalsIgnoreCase(current.maker())')],
 ['approval reference required',policy.includes('required(approval==null?null:approval.reference()')],
 ['approved policy required for binding',policy.includes('فقط سیاست مصوب قابل اتصال است')],
 ['only draft product versions can bind',policy.includes('اتصال سیاست فقط برای نسخه پیش‌نویس مجاز است')],
 ['policy date covers product version',policy.includes('from.isBefore(policy.validFrom())') && policy.includes('to.isAfter(policy.validTo())')],
 ['binding updates only product version foreign key',policy.includes('SET RULE_POLICY_VERSION_ID=:policyId,RECORD_VERSION=RECORD_VERSION+1')],
 ['generic update cannot modify binding',service.includes('تغییر سیاست نسخه تنها از مسیر اختصاصی مجاز است')],
 ['generic create cannot spoof policy binding',service.includes('اتصال سیاست فقط از مسیر اختصاصی Rule Governance Lite مجاز است')],
 ['explicit API for policy create',controller.includes('@PostMapping("/rule-policies")')],
 ['explicit API for controls save',controller.includes('@PutMapping("/rule-policies/{id}/controls")')],
 ['explicit API for policy approval',controller.includes('@PostMapping("/rule-policies/{id}/approve")')],
 ['explicit API for binding',controller.includes('@PutMapping("/versions/{versionId}/rule-policy")')],
 ['policy category not priority ranking',governance.includes('orthogonal to governance')],
 ['existing legacy policy null retains R19 rules',governance.includes('policyId > 0 ? effectivePolicies.get(control.code()) : null')],
 ['optional cutover date protects historical versions',governance.includes('enforcePolicyFrom') && governance.includes('CREATED_AT')],
 ['effective policy must be approved',governance.includes('سیاست متصل به نسخه تصویب نشده است')],
 ['policy scoped by date at report time',governance.includes('نسخه محصول خارج از بازه اعتبار سیاست')],
 ['bound module unknown fails closed when required',governance.includes('MISSING_MODULE')],
 ['rule absence blocks publication only when policy asks',governance.includes('"BLOCK_PUBLISH".equals(absence)')],
 ['rule absence blocks operation only when policy asks',governance.includes('"BLOCK_OPERATION".equals(absence)')],
 ['runtime integration not falsely approved',governance.includes('NOT_CONNECTED_TO_OPERATIONAL_ENGINE') && governance.includes('اجرای عملیاتی این کنترل هنوز در Runtime اثبات نشده است')],
 ['inactive term rule does not count',evidence.includes('row.containsKey("IS_ACTIVE")')],
 ['IS_ALLOWED permission not active flag',evidence.includes('IS_ALLOWED is a business permission')],
 ['deposit allowed term child checked',governance.includes('hasActiveAllowedTerm')],
 ['tiered pricing child checked',governance.includes('hasValidPricingTiers')],
 ['no irreversible schema migration in code patch',!policy.includes('CREATE TABLE') && !policy.includes('ALTER TABLE')],
 ['component uses existing Angular Material',component.includes('MatFormFieldModule') && component.includes('MatButtonModule')],
 ['UI embedded inside existing final review',workspace.includes('<app-rule-policy-lite') && workspace.includes('currentStep() === 6')],
 ['UI offers policy creation',page.includes('ساخت سیاست جدید')],
 ['UI offers cloning',page.includes('clone()')],
 ['UI offers approval & lock',page.includes('approve()')],
 ['UI offers policy binding',page.includes('bind()')],
 ['UI shows controls editing',page.includes('c.presence') && page.includes('c.absence')],
 ['UI explicitly states test-only maker checker',page.includes('هویت احرازشده سرور')],
 ['UI explicitly does not claim transaction enforcement',page.includes('اجرای قواعد در تراکنش‌های بانکی را فعال نمی‌کند')],
 ['typed policy contract Angular',model.includes('export interface RglPolicy') && model.includes('export interface RglControl')],
 ['frontend API separate endpoints',angularApi.includes('createRulePolicy(') && angularApi.includes('bindRulePolicy(')],
 ['UI follows app CSS design tokens',scss.includes('var(--app-border)') && scss.includes('var(--app-surface)')],
 ['initial and later policy state separated',component.includes("this.editMode.set(false)")],
 ['builder R20 verifier in Windows build',get('build-production.cmd').includes('verify-rgl-lite-r20.mjs')],
 ['builder R20 verifier in Unix build',get('build-production.sh').includes('verify-rgl-lite-r20.mjs')]
];
let failed=0;
for(const [name,success] of checks){ if(!success){console.error('FAIL | '+name);failed++;} }
console.log('RGL_R20_STATIC_PASS='+ (checks.length-failed)+' RGL_R20_STATIC_FAIL='+failed);
if(failed)process.exitCode=1;
