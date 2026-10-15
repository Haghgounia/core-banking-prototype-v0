package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDate;

/**
 * PB-R19: control-level governance inventory and publication readiness.
 * Governance category is not a priority order. Applicability dimensions are
 * orthogonal to governance and DO NOT imply a seven-step override hierarchy.
 * This report describes current PDL configuration evidence; it is NOT a
 * runtime transaction-rule evaluator or an inheritance engine.
 */
@Service
public class ProductRuleGovernanceService {
    public record GovernanceLevel(String code, String label, String description) {}
    public record RuleControl(String code, String title, String domain, String moduleCode,
                              String governanceLevel, List<String> applicabilityScopes,
                              String presencePolicy, String whenAbsent,
                              String filterColumn, boolean singleRow) {}
    public record EffectiveRule(String code, String title, String governanceLevel,
                                List<String> applicabilityScopes, String presencePolicy,
                                String whenAbsent, String source, String status,
                                String reason, long configuredCount, boolean blocking,
                                String runtimeIntegration) {}
    public record GovernanceReport(long productVersionId, long productId, String productFamily,
                                   String productDomain, boolean canApprove, boolean canOpen,
                                   List<String> blockers, List<GovernanceLevel> governanceLevels,
                                   List<EffectiveRule> effectiveRules) {}

    private static final List<GovernanceLevel> LEVELS = List.of(
            new GovernanceLevel("REGULATORY", "الزامات نظارتی", "مقررات و محدودیت‌های الزامی نهاد ناظر"),
            new GovernanceLevel("BANK_WIDE", "سیاست‌های کلان بانک", "خط‌مشی‌ها و مصوبات فراگیر بانک"),
            new GovernanceLevel("RISK_COMPLIANCE", "ریسک و انطباق", "کنترل‌های ریسک، شناخت مشتری و انطباق"),
            new GovernanceLevel("PRODUCT_POLICY", "سیاست محصول", "تصمیم‌های مالک محصول؛ بدون تقدم ذاتی بر سایر دسته‌ها"),
            new GovernanceLevel("CONTRACT_VERSION", "قرارداد و نسخه", "تعهدات و شرایط نسخه مصوب محصول"),
            new GovernanceLevel("OPERATIONS", "رویه عملیاتی", "شرایط اجرای خدمت؛ مستقل از دامنه کانال/شعبه"),
            new GovernanceLevel("APPROVED_EXCEPTION", "استثنای مصوب", "استثناهای نیازمند مجوز و ردپای تصمیم")
    );

    // One presence/absence policy PER CONTROL, not per individual row. No
    // inheritance is claimed without an explicitly implemented resolution rule.
    private static final List<RuleControl> CATALOG = List.of(
            c("PRODUCT_ELIGIBILITY_RULE", "اهلیت مشتری", "COMMON", "ELIGIBILITY", "RISK_COMPLIANCE", "CUSTOMER_SEGMENT", true, false),
            c("PRODUCT_CHANNEL_RULE", "کانال و عملیات", "COMMON", "CHANNEL", "OPERATIONS", "CHANNEL,OPERATION", true, false),
            c("PRODUCT_ORG_SCOPE", "دامنه سازمانی", "COMMON", "ORG_SCOPE", "BANK_WIDE", "ORG_UNIT", false, false),
            c("PRODUCT_REQUIRED_DOCUMENT", "مدارک الزامی", "COMMON", "DOCUMENT", "RISK_COMPLIANCE", "PROCESS_STEP", false, false),
            c("PRODUCT_REQUIRED_INQUIRY", "استعلام الزامی", "COMMON", "INQUIRY", "RISK_COMPLIANCE", "CUSTOMER_SEGMENT", false, false),
            c("PRODUCT_PRICING_RULE", "قواعد قیمت‌گذاری", "COMMON", "PRICING", "PRODUCT_POLICY", "PRODUCT_VERSION", false, false),
            c("PRODUCT_RELATIONSHIP", "رابطه بین محصولات", "COMMON", "RELATIONSHIP", "PRODUCT_POLICY", "TARGET_PRODUCT", false, false, "SOURCE_PRODUCT_VERSION_ID"),
            c("DEPOSIT_PRODUCT_PROFILE", "پروفایل سپرده", "DEPOSIT", "PROFILE", "CONTRACT_VERSION", "PRODUCT_VERSION", true, true),
            c("DEPOSIT_PRODUCT_OPENING_RULE", "قواعد افتتاح", "DEPOSIT", "OPENING", "CONTRACT_VERSION", "PRODUCT_VERSION", true, true),
            c("DEPOSIT_PRODUCT_TRANSACTION_RULE", "قواعد تراکنش", "DEPOSIT", "TRANSACTION", "OPERATIONS", "CHANNEL,OPERATION", true, false),
            c("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA", "ابزارهای برداشت", "DEPOSIT", "TRANSACTION", "OPERATIONS", "WITHDRAWAL_MEDIA", false, false),
            c("DEPOSIT_PRODUCT_TERM_RULE", "مدت و سررسید", "DEPOSIT_TERM", "TERM", "CONTRACT_VERSION", "PRODUCT_VERSION", true, false),
            c("DEPOSIT_PROFIT_PAYMENT_RULE", "واریز سود", "DEPOSIT_TERM", "PROFIT_PAYMENT", "CONTRACT_VERSION", "PAYMENT_PERIOD", true, false),
            c("CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE", "پروفایل حساب کارگزاری", "CORRESPONDENT", "CORRESPONDENT", "CONTRACT_VERSION", "PRODUCT_VERSION", true, true),
            c("DEPOSIT_PRODUCT_JOINT_RULE", "حساب مشترک", "DEPOSIT", "JOINT", "CONTRACT_VERSION", "OWNERSHIP", false, false),
            c("DEPOSIT_PRODUCT_DORMANCY_RULE", "راکدی", "DEPOSIT", "DORMANCY", "OPERATIONS", "PRODUCT_VERSION", false, false),
            c("DEPOSIT_PRODUCT_HOLD_RULE", "مسدودی", "DEPOSIT", "HOLD", "RISK_COMPLIANCE", "HOLD_TYPE", false, false),
            c("DEPOSIT_PRODUCT_CLOSURE_RULE", "خاتمه و تسویه", "DEPOSIT", "CLOSURE", "CONTRACT_VERSION", "CLOSURE_TYPE", true, false),
            c("LOAN_PRODUCT_PROFILE", "پروفایل تسهیلات", "LOAN", "LOAN_PROFILE", "CONTRACT_VERSION", "PRODUCT_VERSION", true, true),
            c("LOAN_FINANCIAL_EXTENSION", "شرایط مالی تسهیلات", "LOAN", "LOAN_FINANCIAL", "PRODUCT_POLICY", "PRODUCT_VERSION", true, false),
            c("LOAN_PRODUCT_REPAYMENT_RULE", "بازپرداخت", "LOAN", "REPAYMENT", "CONTRACT_VERSION", "REPAYMENT_TYPE", true, false),
            c("LOAN_PRODUCT_PROCESS_RULE", "فرایند تسهیلات", "LOAN", "PROCESS", "OPERATIONS", "PROCESS_STEP", true, false),
            c("LOAN_PRODUCT_COLLATERAL_RULE", "وثایق", "LOAN", "COLLATERAL", "RISK_COMPLIANCE", "COLLATERAL_TYPE", false, false)
    );

    private static RuleControl c(String code, String title, String domain, String module,
                                 String level, String scopes, boolean required, boolean singleRow) {
        return c(code, title, domain, module, level, scopes, required, singleRow, "PRODUCT_VERSION_ID");
    }
    private static RuleControl c(String code, String title, String domain, String module,
                                 String level, String scopes, boolean required, boolean singleRow, String filter) {
        return new RuleControl(code, title, domain, module, level, List.of(scopes.split(",")),
                required ? "REQUIRED" : "OPTIONAL", required ? "DENY" : "NO_RESTRICTION_DEFINED", filter, singleRow);
    }

    private final PdlProductBuilderRepository repository;
    private final RuleGovernanceLiteService lite;
    private final ProductBuilderBusinessValidator businessValidator;

    // Optional adoption cutover: preserves legacy versions even when their binding is NULL.
    // Set to YYYY-MM-DD only after UAT/operational approval.
    @Value("${core-banking.product-builder.rule-governance-lite.enforce-from:}")
    private String enforcePolicyFrom = "";

    public ProductRuleGovernanceService(PdlProductBuilderRepository repository, RuleGovernanceLiteService lite,
                                        ProductBuilderBusinessValidator businessValidator) {
        this.repository = repository;
        this.lite = lite;
        this.businessValidator = businessValidator;
    }

    public static List<RuleControl> defaultControls() { return CATALOG; }
    public List<GovernanceLevel> governanceLevels() { return LEVELS; }
    public List<RuleControl> controls() { return CATALOG; }

    public GovernanceReport report(long versionId) {
        Map<String, Object> version = repository.findById("PRODUCT_VERSION", versionId)
                .orElseThrow(() -> new ProductBuilderValidationException("نسخه محصول برای ارزیابی حاکمیت یافت نشد."));
        long productId = number(version.get("PRODUCT_ID"));
        Map<String, Object> product = repository.findById("PRODUCT", productId)
                .orElseThrow(() -> new ProductBuilderValidationException("محصول مالک نسخه یافت نشد."));
        String domain = text(product.get("PRODUCT_CLASS_CODE"));
        String family = text(product.get("PRODUCT_FAMILY_CODE"));
        List<Map<String, Object>> modules = repository.search("PRODUCT_VERSION_MODULE", null, 0, 200,
                "PRODUCT_VERSION_ID", String.valueOf(versionId)).items();
        Map<String, Boolean> activeModules = new LinkedHashMap<>();
        for (Map<String, Object> module : modules) {
            activeModules.put(text(module.get("MODULE_CODE")), number(module.get("IS_ENABLED")) == 1);
        }
        List<EffectiveRule> rows = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        List<String> operationBlockers = new ArrayList<>();
        long policyId = number(version.get("RULE_POLICY_VERSION_ID"));
        Map<String, RuleGovernanceLiteService.Control> effectivePolicies = new LinkedHashMap<>();
        if (policyId > 0) {
            var policy = lite.policy(policyId);
            if (!"APPROVED".equals(policy.state())) blockers.add("سیاست متصل به نسخه تصویب نشده است.");
            for (var item : lite.controls(policyId)) effectivePolicies.put(item.code(), item);
            if (effectivePolicies.size() != CATALOG.size()) blockers.add("سیاست محصول تمام کنترل‌های تعریف‌شده را پوشش نمی‌دهد.");
        }
        LocalDate versionStart = parseVersionStart(version.get("VALID_FROM"));
        LocalDate versionEnd = version.get("VALID_TO") == null ? LocalDate.MAX
                : parseVersionStart(version.get("VALID_TO"));
        if (versionEnd.isBefore(versionStart)) blockers.add("بازه اعتبار نسخه معکوس است.");
        if (policyId == 0 && !enforcePolicyFrom.isBlank()) {
            LocalDate cutoff;
            try { cutoff = LocalDate.parse(enforcePolicyFrom); }
            catch (RuntimeException e) { throw new ProductBuilderValidationException("تاریخ آغاز الزام سیاست در تنظیمات سامانه معتبر نیست."); }
            Object createdAt = version.get("CREATED_AT");
            if (createdAt == null) {
                blockers.add("زمان ایجاد نسخه مشخص نیست؛ امکان تعیین شمول سیاست جدید وجود ندارد.");
            } else if (!parseVersionStart(createdAt).isBefore(cutoff)) {
                blockers.add("نسخه محصول پس از تاریخ آغاز حاکمیت جدید ساخته شده و باید سیاست مصوب به آن متصل شود.");
            }
        }
        if (policyId > 0) {
            var policy = lite.policy(policyId);
            if (versionStart.isBefore(policy.validFrom()) || policy.validTo() != null && versionEnd.isAfter(policy.validTo())) {
                blockers.add("نسخه محصول خارج از بازه اعتبار سیاست متصل است؛ تاریخ نسخه یا سیاست را اصلاح کنید.");
            }
        }
        for (RuleControl control : CATALOG) {
            if (!applies(control.domain(), domain, family)) continue;
            RuleGovernanceLiteService.Control assigned = policyId > 0 ? effectivePolicies.get(control.code()) : null;
            if (policyId > 0 && assigned == null) {
                blockers.add(control.title() + ": تعریف این کنترل در نسخه سیاست موجود نیست.");
                continue;
            }
            String presence = assigned == null ? control.presencePolicy() : assigned.presence();
            String absence = assigned == null ? control.whenAbsent() : assigned.absence();
            Boolean moduleEnabled = activeModules.get(control.moduleCode());
            // Unknown module configuration is never silently treated as enabled for a bound policy.
            if (assigned != null && "WHEN_ENABLED".equals(presence) && moduleEnabled == null) {
                blockers.add(control.title() + ": وضعیت ماژول تعیین نشده است؛ باید صریحاً فعال یا غیرفعال شود.");
                rows.add(new EffectiveRule(control.code(), control.title(), assigned.category(),
                        control.applicabilityScopes(), presence, absence, "UNDEFINED", "MISSING_MODULE",
                        "وضعیت ماژول در PRODUCT_VERSION_MODULE ثبت نشده است.", 0, true, "NOT_VERIFIED"));
                continue;
            }
            boolean enabled = moduleEnabled == null || moduleEnabled;
            boolean skipDisabled = !enabled && (assigned == null || !"REQUIRED".equals(presence));
            if (skipDisabled) {
                rows.add(new EffectiveRule(control.code(), control.title(), assigned == null ? control.governanceLevel() : assigned.category(),
                        control.applicabilityScopes(), presence, absence,
                        "MODULE_DISABLED", "NOT_APPLICABLE", "ماژول غیرفعال است و سوابق حذف نمی‌شوند؛ این نتیجه فقط برای ارزیابی پیکربندی است، نه تضمین توقف عملیات در Runtime.",
                        0, false, "NOT_APPLICABLE"));
                continue;
            }
            // Page through physical records: a single row or COUNT(*) is not evidence
            // of an ACTIVE, date-applicable rule. Never sample just the first page.
            List<Map<String, Object>> raw = new ArrayList<>();
            int page = 0;
            long total;
            do {
                var result = repository.search(control.code(), null, page, 200,
                        control.filterColumn(), String.valueOf(versionId));
                total = result.totalElements();
                raw.addAll(result.items());
                page++;
                if ((result.items().isEmpty() || page > 10) && raw.size() < total) {
                    blockers.add(control.title() + ": تعداد رکوردها از حد بررسی حاکمیت عبور کرده است؛ انتشار تا ارزیابی کامل مجاز نیست.");
                    break;
                }
            } while (raw.size() < total && total > 0);
            var evidence = ProductRuleEvidenceEvaluator.evaluate(control.code(), raw, versionStart, versionEnd);
            var temporal = ProductRuleEvidenceEvaluator.temporalCoverage(control.code(), raw, versionStart, versionEnd);
            long count = evidence.effectiveCount();
            boolean requiredForVersion = "REQUIRED".equals(presence) || "WHEN_ENABLED".equals(presence) && enabled;
            boolean missingRequired = requiredForVersion && !temporal.complete();
            boolean conflict = (control.singleRow() && ProductRuleEvidenceEvaluator.hasOverlappingActiveRows(
                    control.code(), raw, versionStart, versionEnd)) || evidence.conflict();
            // HF7-A3/HF4: inspect the real child rows, including future-effective
            // rules. Existence alone is no longer evidence of a publishable term
            // or tiered-pricing configuration. This audit is read-only.
            ProductRuleHierarchyAudit.Finding childFinding = "DEPOSIT_PRODUCT_TERM_RULE".equals(control.code())
                    ? hasActiveAllowedTerm(raw, versionStart, versionEnd)
                    : "PRODUCT_PRICING_RULE".equals(control.code())
                    ? hasValidPricingTiers(raw, versionStart, versionEnd)
                    : new ProductRuleHierarchyAudit.Finding(null, null);
            if (childFinding.conflict()) conflict = true;
            // HF7-A3/HF6: optional children are not proof of an authorization, but
            // each persisted, applicable child must be valid and linked to the
            // correct parent before the design version can be approved.  Reuse
            // exactly the same fail-closed, read-only audit that HF5 qualified.
            // The control belongs to this report only when the module is in
            // scope (the disabled-module branch above has already returned).
            ProductRuleHierarchyAudit.AuditResult relationAudit = switch (control.code()) {
                case "PRODUCT_CHANNEL_RULE", "PRODUCT_ELIGIBILITY_RULE", "DEPOSIT_PRODUCT_CLOSURE_RULE",
                        "CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE" ->
                        ProductRuleHierarchyAudit.audit(control.code(), raw, versionStart, versionEnd,
                                domain, this::loadChildEvidence, businessValidator);
                default -> new ProductRuleHierarchyAudit.AuditResult(0, null);
            };
            // HF7-A2: audit scheduled rules throughout the full validity interval,
            // never merely those effective on the first day. Historical rows
            // outside the interval remain unmodified and do not block approval.
            long invalidRows = 0;
            String semanticProblem = null;
            for (var rule : raw) {
                if (ProductRuleEvidenceEvaluator.hasInvalidActivePeriod(control.code(), rule)) {
                    invalidRows++;
                    if (semanticProblem == null) semanticProblem = "بازه اعتبار قاعده فعال معکوس است.";
                } else if ((ProductRuleSemanticContract.auditsOnApproval(control.code())
                        || "CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE".equals(control.code()))
                        && ProductRuleEvidenceEvaluator.isApplicableWithin(control.code(), rule, versionStart, versionEnd)) {
                    try {
                        businessValidator.validate(control.code(), rule);
                        if ("CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE".equals(control.code())) {
                            String expectedAccountType = "NOSTRO_ACCOUNT".equals(family) ? "NOSTRO"
                                    : "VOSTRO_ACCOUNT".equals(family) ? "VOSTRO" : "";
                            if (expectedAccountType.isEmpty() || !expectedAccountType.equals(text(rule.get("ACCOUNT_TYPE_CODE"))))
                                throw new ProductBuilderValidationException("نوع پروفایل نوسترو/وسترو با خانواده محصول سازگار نیست.");
                            if (text(rule.get("SETTLEMENT_CURRENCY_CODE")).isEmpty()
                                    || text(rule.get("RECONCILIATION_METHOD_CODE")).isEmpty())
                                throw new ProductBuilderValidationException("ارز تسویه و روش تطبیق پروفایل کارگزاری الزامی است.");
                        }
                    } catch (ProductBuilderValidationException ex) {
                        invalidRows++;
                        if (semanticProblem == null) semanticProblem = ex.getMessage();
                    }
                }
            }
            if (childFinding.invalidSemantics()) {
                invalidRows++;
                if (semanticProblem == null) semanticProblem = childFinding.reason();
            }
            if (!relationAudit.valid()) {
                invalidRows += relationAudit.invalidCount();
                if (semanticProblem == null) semanticProblem = relationAudit.firstProblem();
            }
            boolean invalidSemantics = invalidRows > 0;
            String status = invalidSemantics ? "INVALID_SEMANTICS" : conflict ? "CONFLICT" : missingRequired ? "MISSING_REQUIRED" : count > 0 ? "CONFIGURED" : "OPTIONAL_ABSENT";
            String reason = invalidSemantics ? "تعداد " + invalidRows + " قاعده مؤثر یا فرزند آن دارای نقص معنایی است؛ نمونه: " + semanticProblem
                    : conflict ? (evidence.conflictReason() != null ? evidence.conflictReason()
                        : childFinding.conflict() ? childFinding.reason()
                        : "بیش از یک رکورد مؤثر برای کنترل تک‌رکوردی نسخه تعریف شده است.")
                    : missingRequired ? "پوشش زمانی Rule اجباری در طول اعتبار نسخه کامل نیست؛ نخستین روز فاقد پوشش: "
                            + temporal.firstGap() + "؛ رفتار نبود: " + absence + "."
                    : count > 0 ? "قاعده فعال در تاریخ شروع نسخه وجود دارد و پوشش زمانی تجمیعی بررسی شد؛ اجرای دامنه‌های کانال/مشتری در Runtime هنوز تأیید نشده است."
                    : "نبود این کنترل اختیاری است؛ نبود Rule به معنی مجوز عمومی عملیات نیست.";
            boolean publishBlocked = invalidSemantics || conflict || missingRequired && (assigned == null || "BLOCK_PUBLISH".equals(absence));
            boolean operationBlocked = missingRequired && assigned != null && "BLOCK_OPERATION".equals(absence);
            if (publishBlocked) blockers.add(control.title() + ": " + reason);
            if (operationBlocked) operationBlockers.add(control.title() + ": " + reason);
            if (assigned != null && assigned.runtimeRequired()) operationBlockers.add(control.title() + ": اجرای عملیاتی این کنترل هنوز در Runtime اثبات نشده است.");
            rows.add(new EffectiveRule(control.code(), control.title(), assigned == null ? control.governanceLevel() : assigned.category(),
                    control.applicabilityScopes(), presence, absence,
                    count > 0 ? "PRODUCT_VERSION" : evidence.historicalCount() > 0 ? "HISTORICAL" : "UNDEFINED",
                    status, reason, count, publishBlocked || operationBlocked,
                    "DEPOSIT_PRODUCT_TRANSACTION_RULE".equals(control.code()) ? "NOT_CONNECTED_TO_OPERATIONAL_ENGINE" : "NOT_VERIFIED"));
        }
        if ("RETIRED".equals(text(product.get("PRODUCT_STATUS_CODE")))) {
            blockers.add("محصول بازنشسته است و انتشار نسخه جدید آن مجاز نیست.");
        }
        if (!Set.of("DEPOSIT", "LOAN").contains(domain)) {
            blockers.add("دامنه کسب‌وکاری محصول معتبر نیست؛ ارزیابی تصویب نسخه مجاز نیست.");
        }
        boolean canApprove = blockers.isEmpty();
        boolean canOpen = canApprove && operationBlockers.isEmpty();
        blockers.addAll(operationBlockers);
        // Fail closed for a material known integration gap; declaring rule rows
        // cannot make cash withdrawal enforcement safe at operational runtime.
        if ("DEPOSIT".equals(domain) && !Set.of("NOSTRO_ACCOUNT", "VOSTRO_ACCOUNT").contains(family)) {
            canOpen = false;
            blockers.add("کنترل انتشار عملیاتی: قواعد تراکنش هنوز در موتور اجرای تراکنش سپرده مصرف نمی‌شوند. افتتاح بر اساس نسخه جدید تا اتصال و آزمون Runtime مجاز نیست.");
        }
        // HF7-A3/HF7: NOSTRO/VOSTRO must NOT be exempt from operational
        // qualification. There is no separately certified correspondent
        // settlement/reconciliation posting engine for this product contract.
        // This is strictly an operational gate, not a design-approval blocker.
        if ("DEPOSIT".equals(domain) && Set.of("NOSTRO_ACCOUNT", "VOSTRO_ACCOUNT").contains(family)) {
            canOpen = false;
            blockers.add("کنترل انتشار عملیاتی نوسترو/وسترو: اجرای قواعد تسویه، تطبیق و ثبت‌های مالی در موتور کارگزاری به‌صورت انتهابه‌انتها احراز نشده است؛ افتتاح عملیاتی مجاز نیست.");
        }
        // Unknown/unsupported domain has no verified operational policy engine.
        if (!Set.of("DEPOSIT", "LOAN").contains(domain)) {
            canOpen = false;
            blockers.add("کنترل انتشار عملیاتی: دامنه محصول فاقد موتور سیاست اجرایی تأییدشده است.");
        }
        // HF7: Product Builder design approval is not operational qualification.
        // No integrated loan-origination/disbursement/repayment/collection policy
        // enforcement has been qualified against this PDL policy contract.  A
        // configured LOAN rule row must never create an implicit OPEN permission.
        // Preserve issuance of an INACTIVE product identity after design approval.
        if ("LOAN".equals(domain)) {
            canOpen = false;
            blockers.add("کنترل انتشار عملیاتی تسهیلات: اتصال و اجرای قواعد مصوب در چرخه درخواست، اعتبارسنجی، تصویب، اعطا، پرداخت، بازپرداخت و وصول تسهیلات به‌صورت انتهابه‌انتها احراز نشده است؛ فعال‌سازی عملیاتی این نسخه مجاز نیست.");
        }
        return new GovernanceReport(versionId, productId, family, domain, canApprove, canOpen,
                List.copyOf(blockers), LEVELS, List.copyOf(rows));
    }

    // Retain these method names for compatibility with the R20 governance audit,
    // but return detailed evidence rather than an unsafe "any child exists" flag.
    private ProductRuleHierarchyAudit.Finding hasActiveAllowedTerm(List<Map<String, Object>> parents,
                                                                    LocalDate from, LocalDate to) {
        return ProductRuleHierarchyAudit.allowedTerms(parents, from, to,
                this::loadChildEvidence, businessValidator);
    }

    private ProductRuleHierarchyAudit.Finding hasValidPricingTiers(List<Map<String, Object>> parents,
                                                                   LocalDate from, LocalDate to) {
        return ProductRuleHierarchyAudit.pricingTiers(parents, from, to,
                this::loadChildEvidence, businessValidator);
    }

    /** Read-only, fully paginated FK lookup. No first-page-only publication. */
    private List<Map<String, Object>> loadChildEvidence(String table, String foreignKey, long parentId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        final int pageSize = 200;
        for (int page = 0; page < 10; page++) {
            var result = repository.search(table, null, page, pageSize,
                    foreignKey, String.valueOf(parentId));
            if (result == null) throw new ProductBuilderValidationException("پاسخ بازیابی قاعده فرزند موجود نیست: " + table);
            rows.addAll(result.items());
            if (rows.size() >= result.totalElements()) return rows;
            if (result.items().isEmpty()) break;
        }
        throw new ProductBuilderValidationException("تعداد قواعد فرزند قابل ارزیابی کامل نیست: " + table);
    }

    public void assertPromotionAllowed(long versionId, Map<String, Object> previous, Map<String, Object> merged) {
        String oldStatus = text(previous.get("VERSION_STATUS_CODE"));
        String status = text(merged.get("VERSION_STATUS_CODE"));
        String previousOrigination = text(previous.get("ORIGINATION_STATUS_CODE"));
        String origination = text(merged.get("ORIGINATION_STATUS_CODE"));
        boolean approval = !"APPROVED".equals(oldStatus) && "APPROVED".equals(status);
        boolean opening = !"OPEN".equals(previousOrigination) && "OPEN".equals(origination);
        boolean goCurrent = number(previous.get("IS_CURRENT")) != 1 && number(merged.get("IS_CURRENT")) == 1;
        if (!approval && !opening && !goCurrent) return;
        GovernanceReport result = report(versionId);
        if (approval && !result.canApprove()) {
            throw new ProductBuilderValidationException("تصویب نسخه به علت نقص یا تعارض کنترل‌ها مجاز نیست: "
                    + String.join("؛ ", result.blockers()));
        }
        if ((opening || (goCurrent && "OPEN".equals(origination)) || (approval && "OPEN".equals(origination))) && !result.canOpen()) {
            throw new ProductBuilderValidationException("فعال‌سازی افتتاح برای نسخه دارای نقص حاکمیتی مجاز نیست: "
                    + String.join("؛ ", result.blockers()));
        }
    }

    private static boolean applies(String type, String domain, String family) {
        return switch (type) {
            case "COMMON" -> true;
            case "DEPOSIT" -> "DEPOSIT".equals(domain) && !Set.of("NOSTRO_ACCOUNT", "VOSTRO_ACCOUNT").contains(family);
            case "DEPOSIT_TERM" -> "DEPOSIT".equals(domain) && Set.of("TERM_DEPOSIT", "SHORT_TERM_DEPOSIT", "LONG_TERM_DEPOSIT", "CERTIFICATE_OF_DEPOSIT").contains(family);
            case "CORRESPONDENT" -> "DEPOSIT".equals(domain) && Set.of("NOSTRO_ACCOUNT", "VOSTRO_ACCOUNT").contains(family);
            case "LOAN" -> "LOAN".equals(domain);
            default -> false;
        };
    }

    private static LocalDate parseVersionStart(Object value) {
        if (value == null) throw new ProductBuilderValidationException("تاریخ شروع اعتبار نسخه برای ارزیابی حاکمیت الزامی است.");
        String date = String.valueOf(value).trim();
        if (date.length() > 10) date = date.substring(0, 10);
        try { return LocalDate.parse(date); }
        catch (RuntimeException e) { throw new ProductBuilderValidationException("تاریخ شروع نسخه برای ارزیابی معتبر نیست."); }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toUpperCase();
    }
    private static long number(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(value).trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}
