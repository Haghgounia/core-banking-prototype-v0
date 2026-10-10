import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');
const app = 'backend/src/main/java/com/behsazan/corebanking/productbuilder/';
const ui = 'frontend/src/app/features/product-builder/';
const governance = read(app + 'application/ProductRuleGovernanceService.java');
const evidence = read(app + 'application/ProductRuleEvidenceEvaluator.java');
const evidenceTest = read('backend/src/test/java/com/behsazan/corebanking/productbuilder/application/ProductRuleEvidenceR19Test.java');
const service = read(app + 'application/ProductBuilderService.java');
const guard = read(app + 'application/ProductGovernanceWriteGuard.java');
const stopTest = read('backend/src/test/java/com/behsazan/corebanking/productbuilder/application/ProductGovernanceStopOnlyR19Test.java');
const controller = read(app + 'web/ProductBuilderController.java');
const junit = read('backend/src/test/java/com/behsazan/corebanking/productbuilder/application/ProductRuleGovernanceR19Test.java');
const workspace = read(ui + 'product-workspace.component.ts');
const page = read(ui + 'product-workspace.component.html');
const api = read(ui + 'product-builder.service.ts');
const models = read(ui + 'product-builder.models.ts');
const ref = read('docs/reference/Unified_Product_Builder_Interactive_Forms_FA.html');
const catalog = read(app + 'application/PdlCatalog.java');
const windows = read('build-production.cmd');
const unix = read('build-production.sh');
const levels = [...governance.matchAll(/new GovernanceLevel\("([A-Z_]+)"/g)].map(m => m[1]);
const controls = [...governance.matchAll(/\bc\("([A-Z_]+)",\s*"([^\"]+)"/g)].map(m => m[1]);
const checks = [
  ['seven named governance classifications', levels.length === 7 && new Set(levels).size === 7],
  ['no hierarchy confused with scope dimension', governance.includes('Applicability dimensions are') && governance.includes('orthogonal to governance')],
  ['regulatory bank risk product contract operations exception categories present', ['REGULATORY','BANK_WIDE','RISK_COMPLIANCE','PRODUCT_POLICY','CONTRACT_VERSION','OPERATIONS','APPROVED_EXCEPTION'].every(x => levels.includes(x))],
  ['governance does not use L4/L5/L6 as override steps', !governance.includes('LEVEL_FOUR') && !governance.includes('GOVERNANCE_PRIORITY')],
  ['controls have unique business keys', controls.length >= 23 && new Set(controls).size === controls.length],
  ['all control source tables are registered in canonical PDL catalog', controls.every(x => catalog.includes('"'+x+'"'))],
  ['approved HTML retained as reference without rewriting it', ref.includes('DEPOSIT_PRODUCT_OPENING_RULE') && ref.includes('PRODUCT_PRICING_RULE')],
  ['control-level presence and absence separate from records', governance.includes('String presencePolicy, String whenAbsent')],
  ['requirement is explicit per control', governance.includes('required ? "REQUIRED" : "OPTIONAL"')],
  ['absence rejection is explicit', governance.includes('required ? "DENY"')],
  ['optional absent cannot accidentally mean general permission', governance.includes('نبود Rule به معنی مجوز عمومی عملیات نیست')],
  ['scope dimensions include channel and operation', governance.includes('"CHANNEL,OPERATION"')],
  ['scope dimensions include organization and customer', governance.includes('"ORG_UNIT"') && governance.includes('"CUSTOMER_SEGMENT"')],
  ['disabled modules do not erase historical configuration', governance.includes('MODULE_DISABLED') && governance.includes('سوابق حذف نمی‌شوند')],
  ['version lookup uses governed PDL rows', governance.includes('findById("PRODUCT_VERSION", versionId)')],
  ['actual number of rows is queried by correct version field', governance.includes('control.filterColumn()') && governance.includes('repository.search(control.code()')],
  ['relationship source version uses its own foreign key', governance.includes('"SOURCE_PRODUCT_VERSION_ID"')],
  ['inactive rows never count as effective', evidence.includes('USABLE_STATUSES') && evidence.includes('INACTIVE_STATUSES')],
  ['scope includes version start and end', evidence.includes('LocalDate versionStart, LocalDate versionEnd')],
  ['duplicate transaction and channel rule keys are checked', evidence.includes('DEPOSIT_PRODUCT_TRANSACTION_RULE') && evidence.includes('PRODUCT_CHANNEL_RULE')],
  ['pricing overlapping effective dates are checked', evidence.includes('PRODUCT_PRICING_RULE') && evidence.includes('overlap(group.get(a), group.get(b))')],
  ['unrecognized rule statuses are fail closed', evidence.includes('unknown > 0')],
  ['per-control pagination prevents first-page-only completeness', governance.includes('do {') && governance.includes('raw.addAll(result.items())')],
  ['evidence regression JUnit includes overlap and unknown-status cases', evidenceTest.includes('overlappingPricingIntervalsForSamePurposeAndCurrencyConflict') && evidenceTest.includes('unknownRuleStatusFailsClosed')],
  ['single-row controls detect duplicate rows', governance.includes('count > 1 && control.singleRow()')],
  ['matrix publishes source, applicability, status, reason', ['String source, String status','String reason, long configuredCount','List<String> applicabilityScopes'].every(x => governance.includes(x))],
  ['transaction Rule never masquerades as operational authorization', governance.includes('NOT_CONNECTED_TO_OPERATIONAL_ENGINE')],
  ['cash withdrawal gap fails closed at origination gate', governance.includes('canOpen = false;') && governance.includes('تا اتصال و آزمون Runtime مجاز نیست')],
  ['product retirement blocks approval readiness', governance.includes('"RETIRED".equals(text(product.get("PRODUCT_STATUS_CODE")))')],
  ['approval gate checks version status transition', governance.includes('"APPROVED".equals(status)')],
  ['origination gate checks OPEN transition', governance.includes('"OPEN".equals(origination)')],
  ['activation of current OPEN version also checked', governance.includes('(goCurrent && "OPEN".equals(origination))')],
  ['new version cannot be created directly APPROVED', service.includes('نسخه جدید ابتدا باید به صورت پیش‌نویس')],
  ['new version cannot be created directly open for origination', service.includes('افتتاح محصول برای نسخه ثبت‌نشده مجاز نیست')],
  ['generic CRUD update invokes backend promotion gate', service.includes('governance.assertPromotionAllowed(id, existing, merged)')],
  ['generic CRUD and eligibility guard published rules', service.includes('writeGuard.assertUpdateAllowed(table, existing, prepared)') && service.includes('writeGuard.assertUpdateAllowed("PRODUCT_ELIGIBILITY_RULE", existing, prepared)')],
  ['approved versions permit only emergency reduction in permissions', guard.includes('assertNonDraftVersionStopOnly') && guard.includes('Set.of("CLOSED", "SUSPENDED")')],
  ['approved version emergency stop covered by JUnit', stopTest.includes('bankCanImmediatelyStopOriginationOnApprovedProduct') && stopTest.includes('approvedVersionCanBeExpiredButNotRevertedToDraft')],
  ['published version deletes are protected', service.includes('writeGuard.assertDeleteAllowed(table, findById(table, id))')],
  ['REST exposes governed readiness matrix', controller.includes('@GetMapping("/versions/{versionId}/rule-governance")')],
  ['final review badge follows governance approval readiness, not any row count', workspace.includes('this.governanceReport()?.canApprove === true')],
  ['frontend consumes live governance report', api.includes('ruleGovernance(versionId: number)') && workspace.includes('this.service.ruleGovernance(id)')],
  ['frontend shows seven category filters', page.includes('audit.governanceLevels') && page.includes('governanceLevelFilter.set(level.code)')],
  ['frontend shows applicability scope independently', page.includes('governanceScopeFa(scope)') && page.includes('governanceLevelFa(rule.governanceLevel)')],
  ['frontend shows origin, missing behavior, status, reason and runtime integration', ['governanceSourceFa(rule.source)','governanceAbsenceFa(rule.whenAbsent)','governanceStatusFa(rule.status, rule.blocking)','governanceRuntimeFa(rule.runtimeIntegration)','rule.reason'].every(x => page.includes(x))],
  ['frontend does not call matrix proof of runtime rule resolution', page.includes('نه اثبات اجرای Rule در تراکنش')],
  ['version editor preflights promotion before saving', workspace.includes('readiness.canApprove') && workspace.includes('readiness.canOpen')],
  ['JUnit regression protects major governance cases', ['missingVersionProfileBlocksApproval','disablingTransactionCapabilityPreservesHistory','configuredRulesDoNotClaimOperationalCashWithdrawalEnforcement','doubleSingleRowProfileIsAConflict','draftEditsAreNotPublication'].every(x => junit.includes(x))],
  ['typed report contract exposed to Angular', models.includes('interface PdlRuleGovernanceReport') && models.includes('interface PdlEffectiveRule')],
  ['R19 verifier part of production Windows build', windows.includes('verify-pb-r19.mjs')],
  ['R19 verifier part of production Unix build', unix.includes('verify-pb-r19.mjs')]
];
let fails = 0;
for (const [name, pass] of checks) { console.log((pass ? 'PASS' : 'FAIL') + ' | ' + name); if (!pass) fails++; }
console.log(`PB_R19_STATIC_PASS=${checks.length - fails} PB_R19_STATIC_FAIL=${fails}`);
if (fails) process.exitCode = 1;
