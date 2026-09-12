package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.Product360;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.ProductModuleValidation;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.ProductReadinessItem;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * FIX98 governance/readiness service.
 *
 * Source of truth remains the FIX96 PDL model. No workflow table is introduced:
 * - PRODUCT and PRODUCT_VERSION hold lifecycle state.
 * - PRODUCT_VERSION_MODULE holds configuration/validation state.
 * - Rule tables are re-read for every validation/approval/publish decision.
 */
@Service
public class ProductGovernanceService {
    private static final String COMMON = "COMMON";
    private static final String DEPOSIT = "DEPOSIT";
    private static final String LOAN = "LOAN";

    private final PdlProductBuilderRepository repository;

    public ProductGovernanceService(PdlProductBuilderRepository repository) {
        this.repository = repository;
    }

    public Product360 product360(long productId, Long requestedVersionId) {
        Map<String, Object> product = requiredRow("PRODUCT", productId);
        Map<String, Object> version = resolveVersion(productId, requestedVersionId);
        if (version == null) {
            List<ProductReadinessItem> checks = List.of(
                    new ProductReadinessItem("IDENTITY", "هویت محصول", identityReady(product), identityDetail(product)),
                    new ProductReadinessItem("VERSION", "نسخه محصول", false, "برای محصول هنوز نسخه‌ای ثبت نشده است.")
            );
            return new Product360(product, null, List.of(), Map.of(), List.of(), checks,
                    score(checks), false, false, false, "DRAFT");
        }
        return build360(product, version);
    }

    @Transactional
    public Product360 validateVersion(long productId, long productVersionId, String actor) {
        Map<String, Object> product = requiredRow("PRODUCT", productId);
        Map<String, Object> version = requiredVersion(productId, productVersionId);
        String actorName = actorName(actor);

        Product360 live = build360(product, version);
        Map<String, ProductModuleValidation> validations = new LinkedHashMap<>();
        for (ProductModuleValidation validation : live.moduleValidations()) {
            validations.put(validation.moduleCode(), validation);
        }

        TablePage modulePage = repository.search("PRODUCT_VERSION_MODULE", null, 0, 200,
                "PRODUCT_VERSION_ID", String.valueOf(productVersionId));
        Set<String> applicable = new LinkedHashSet<>();
        for (ModulePolicy policy : applicablePolicies(product)) applicable.add(policy.code());

        for (Map<String, Object> module : modulePage.items()) {
            if (!flag(module.get("IS_ENABLED"))) continue;
            long moduleId = number(module.get("PRODUCT_VERSION_MODULE_ID"));
            if (moduleId <= 0) continue;
            String code = text(module.get("MODULE_CODE"));
            ProductModuleValidation validation = validations.get(code);
            boolean valid = applicable.contains(code)
                    && validation != null
                    && validation.configured()
                    && validation.dataValid();
            repository.update("PRODUCT_VERSION_MODULE", moduleId,
                    Map.of("VALIDATION_STATUS_CODE", valid ? "VALID" : "INVALID"), actorName);
        }
        return build360(product, requiredVersion(productId, productVersionId));
    }

    @Transactional
    public Product360 approveVersion(long productId, long productVersionId, String actor) {
        String actorName = actorName(actor);
        Map<String, Object> version = requiredVersion(productId, productVersionId);
        String state = text(version.get("VERSION_STATUS_CODE"));
        if (!"DRAFT".equals(state)) {
            throw new ProductBuilderValidationException("فقط نسخه DRAFT قابل تصویب است. وضعیت فعلی: " + state);
        }

        Product360 validated = validateVersion(productId, productVersionId, actorName);
        if (!validated.validationPassed() || validated.readinessScore() != 100) {
            throw new ProductBuilderValidationException("نسخه برای تصویب آماده نیست. ابتدا خطاهای Product 360 / Validation را برطرف کنید.");
        }

        repository.update("PRODUCT_VERSION", productVersionId, Map.of(
                "VERSION_STATUS_CODE", "APPROVED",
                "APPROVED_AT", LocalDateTime.now(),
                "APPROVED_BY", actorName
        ), actorName);
        return product360(productId, productVersionId);
    }

    @Transactional
    public Product360 publishVersion(long productId, long productVersionId, String actor) {
        String actorName = actorName(actor);
        Map<String, Object> version = requiredVersion(productId, productVersionId);
        String state = text(version.get("VERSION_STATUS_CODE"));
        if (!"APPROVED".equals(state)) {
            throw new ProductBuilderValidationException("برای Publish، نسخه باید قبلاً APPROVED شده باشد. وضعیت فعلی: " + state);
        }

        Product360 validated = validateVersion(productId, productVersionId, actorName);
        if (!validated.validationPassed() || validated.readinessScore() != 100) {
            throw new ProductBuilderValidationException("Publish متوقف شد؛ Validation زنده Ruleها یا Readiness کامل نیست.");
        }

        TablePage versions = repository.search("PRODUCT_VERSION", null, 0, 200,
                "PRODUCT_ID", String.valueOf(productId));
        for (Map<String, Object> other : versions.items()) {
            long otherId = number(other.get("PRODUCT_VERSION_ID"));
            if (otherId > 0 && otherId != productVersionId && flag(other.get("IS_CURRENT"))) {
                repository.update("PRODUCT_VERSION", otherId, Map.of("IS_CURRENT", 0), actorName);
            }
        }

        repository.update("PRODUCT_VERSION", productVersionId, Map.of(
                "VERSION_STATUS_CODE", "ACTIVE",
                "IS_CURRENT", 1
        ), actorName);
        repository.update("PRODUCT", productId, Map.of("PRODUCT_STATUS_CODE", "ACTIVE"), actorName);
        return product360(productId, productVersionId);
    }

    @Transactional
    public Product360 returnToDraft(long productId, long productVersionId, String actor) {
        String actorName = actorName(actor);
        Map<String, Object> version = requiredVersion(productId, productVersionId);
        String state = text(version.get("VERSION_STATUS_CODE"));
        if ("ACTIVE".equals(state)) {
            throw new ProductBuilderValidationException("نسخه ACTIVE از این مسیر به Draft بازگردانده نمی‌شود؛ ابتدا باید فرآیند Retire/Replacement انجام شود.");
        }
        repository.update("PRODUCT_VERSION", productVersionId, nullableMap(
                "VERSION_STATUS_CODE", "DRAFT",
                "APPROVED_AT", null,
                "APPROVED_BY", null,
                "IS_CURRENT", 0
        ), actorName);
        return product360(productId, productVersionId);
    }

    private Product360 build360(Map<String, Object> product, Map<String, Object> version) {
        long versionId = number(version.get("PRODUCT_VERSION_ID"));
        TablePage modulePage = repository.search("PRODUCT_VERSION_MODULE", null, 0, 200,
                "PRODUCT_VERSION_ID", String.valueOf(versionId));
        List<Map<String, Object>> modules = modulePage.items();
        Map<String, Map<String, Object>> moduleRows = new LinkedHashMap<>();
        for (Map<String, Object> row : modules) moduleRows.put(text(row.get("MODULE_CODE")), row);

        List<ModulePolicy> policies = applicablePolicies(product);
        Set<String> applicableCodes = new LinkedHashSet<>();
        for (ModulePolicy policy : policies) applicableCodes.add(policy.code());
        Set<String> requiredCodes = requiredModuleCodes(product);

        Map<String, Long> ruleCounts = new LinkedHashMap<>();
        List<ProductModuleValidation> validations = new ArrayList<>();
        for (ModulePolicy policy : policies) {
            Map<String, Object> module = moduleRows.get(policy.code());
            boolean enabled = module != null && flag(module.get("IS_ENABLED"));
            String configurationStatus = module == null ? "NOT_CONFIGURED" : text(module.get("CONFIGURATION_STATUS_CODE"));
            String validationStatus = module == null ? "NOT_VALIDATED" : text(module.get("VALIDATION_STATUS_CODE"));
            boolean configured = enabled && "CONFIGURED".equals(configurationStatus);
            Map<String, Long> counts = new LinkedHashMap<>();
            List<String> missing = new ArrayList<>();
            if (enabled) {
                for (Requirement requirement : policy.requirements()) {
                    long count = countRequirement(requirement, versionId);
                    counts.merge(requirement.resultTable(), count, Long::sum);
                    ruleCounts.merge(requirement.resultTable(), count, Long::sum);
                    if (count <= 0) missing.add(requirement.resultTable());
                }
            } else {
                for (Requirement requirement : policy.requirements()) {
                    counts.putIfAbsent(requirement.resultTable(), 0L);
                    ruleCounts.putIfAbsent(requirement.resultTable(), 0L);
                }
            }
            boolean dataValid = enabled && missing.isEmpty();
            boolean recordedValid = enabled && isValidStatus(validationStatus);
            validations.add(new ProductModuleValidation(
                    policy.code(), policy.label(), policy.domain(), requiredCodes.contains(policy.code()),
                    enabled, configured, dataValid, recordedValid, configurationStatus, validationStatus,
                    List.copyOf(missing), Map.copyOf(counts)
            ));
        }

        List<String> nonApplicableEnabled = new ArrayList<>();
        for (Map<String, Object> row : modules) {
            String code = text(row.get("MODULE_CODE"));
            if (flag(row.get("IS_ENABLED")) && !applicableCodes.contains(code)) nonApplicableEnabled.add(code);
        }

        List<ProductModuleValidation> enabled = validations.stream().filter(ProductModuleValidation::enabled).toList();
        List<String> missingRequired = requiredCodes.stream()
                .filter(code -> enabled.stream().noneMatch(v -> v.moduleCode().equals(code)))
                .toList();
        boolean identityReady = identityReady(product);
        boolean requiredEnabled = missingRequired.isEmpty();
        boolean applicableOnly = nonApplicableEnabled.isEmpty();
        boolean anyEnabled = !enabled.isEmpty();
        boolean allConfigured = anyEnabled && enabled.stream().allMatch(ProductModuleValidation::configured);
        boolean allDataValid = anyEnabled && enabled.stream().allMatch(ProductModuleValidation::dataValid);
        boolean allValidationRecorded = anyEnabled && enabled.stream().allMatch(ProductModuleValidation::validationRecorded);

        List<ProductReadinessItem> checks = List.of(
                new ProductReadinessItem("IDENTITY", "هویت و Family محصول", identityReady, identityDetail(product)),
                new ProductReadinessItem("VERSION", "نسخه محصول", true,
                        "v" + version.get("VERSION_NO") + " — " + text(version.get("VERSION_STATUS_CODE"))),
                new ProductReadinessItem("REQUIRED_MODULES", "ماژول‌های پایه Family", requiredEnabled,
                        requiredEnabled ? "همه ماژول‌های پایه فعال‌اند." : "ماژول‌های الزامی غیرفعال: " + String.join(", ", missingRequired)),
                new ProductReadinessItem("APPLICABILITY", "انطباق ماژول‌ها با Family", applicableOnly,
                        applicableOnly ? "ماژول فعال خارج از Family وجود ندارد." : "ماژول‌های نامرتبط فعال: " + String.join(", ", nonApplicableEnabled)),
                new ProductReadinessItem("CONFIGURATION", "وضعیت Configuration", allConfigured,
                        enabled.stream().filter(v -> !v.configured()).map(ProductModuleValidation::moduleCode).toList().toString()),
                new ProductReadinessItem("RULE_DATA", "پوشش واقعی Ruleها", allDataValid,
                        allDataValid ? "Ruleهای موردنیاز برای همه ماژول‌های فعال موجود است." : missingRuleDetail(enabled)),
                new ProductReadinessItem("VALIDATION", "ثبت نتیجه Validation سیستمی", allValidationRecorded,
                        allValidationRecorded ? "همه ماژول‌های فعال VALID هستند." : "Validate را اجرا کنید یا خطاهای INVALID را برطرف کنید.")
        );

        int readinessScore = score(checks);
        boolean validationPassed = identityReady && requiredEnabled && applicableOnly && allConfigured && allDataValid && allValidationRecorded;
        String versionStatus = text(version.get("VERSION_STATUS_CODE"));
        boolean canApprove = validationPassed && readinessScore == 100 && "DRAFT".equals(versionStatus);
        boolean approved = "APPROVED".equals(versionStatus)
                && version.get("APPROVED_AT") != null && version.get("APPROVED_BY") != null;
        boolean canPublish = validationPassed && readinessScore == 100 && approved;

        return new Product360(product, version, modules, Map.copyOf(ruleCounts), List.copyOf(validations), checks,
                readinessScore, validationPassed, canApprove, canPublish, versionStatus);
    }

    private Map<String, Object> resolveVersion(long productId, Long requestedVersionId) {
        if (requestedVersionId != null) return requiredVersion(productId, requestedVersionId);
        TablePage page = repository.search("PRODUCT_VERSION", null, 0, 200, "PRODUCT_ID", String.valueOf(productId));
        if (page.items().isEmpty()) return null;
        return page.items().stream().filter(row -> flag(row.get("IS_CURRENT"))).findFirst().orElse(page.items().get(0));
    }

    private Map<String, Object> requiredVersion(long productId, long productVersionId) {
        Map<String, Object> version = requiredRow("PRODUCT_VERSION", productVersionId);
        if (number(version.get("PRODUCT_ID")) != productId) {
            throw new ProductBuilderValidationException("نسخه انتخاب‌شده متعلق به این محصول نیست.");
        }
        return version;
    }

    private Map<String, Object> requiredRow(String table, long id) {
        return repository.findById(table, id)
                .orElseThrow(() -> new ProductBuilderValidationException("PDL row not found: " + table + "/" + id));
    }

    private long countRequirement(Requirement requirement, long versionId) {
        if (requirement.parentTable() == null) {
            return repository.search(requirement.resultTable(), null, 0, 1,
                    requirement.filterColumn(), String.valueOf(versionId)).totalElements();
        }
        TablePage parents = repository.search(requirement.parentTable(), null, 0, 200,
                requirement.filterColumn(), String.valueOf(versionId));
        long total = 0;
        for (Map<String, Object> parent : parents.items()) {
            Object parentId = parent.get(requirement.parentPkColumn());
            if (parentId == null) continue;
            total += repository.search(requirement.resultTable(), null, 0, 1,
                    requirement.childFkColumn(), String.valueOf(parentId)).totalElements();
        }
        return total;
    }

    private static List<ModulePolicy> applicablePolicies(Map<String, Object> product) {
        String domain = "LOAN".equals(text(product.get("PRODUCT_CLASS_CODE"))) ? LOAN : DEPOSIT;
        String family = text(product.get("PRODUCT_FAMILY_CODE"));
        Set<String> termFamilies = Set.of("SHORT_TERM_DEPOSIT", "LONG_TERM_DEPOSIT", "CERTIFICATE_OF_DEPOSIT");
        Set<String> correspondentFamilies = Set.of("NOSTRO_ACCOUNT", "VOSTRO_ACCOUNT");
        return POLICIES.stream().filter(policy -> {
            if (!COMMON.equals(policy.domain()) && !domain.equals(policy.domain())) return false;
            if (("TERM".equals(policy.code()) || "PROFIT_PAYMENT".equals(policy.code())) && !termFamilies.contains(family)) return false;
            if ("CORRESPONDENT".equals(policy.code()) && !correspondentFamilies.contains(family)) return false;
            return true;
        }).toList();
    }

    private static Set<String> requiredModuleCodes(Map<String, Object> product) {
        String domain = "LOAN".equals(text(product.get("PRODUCT_CLASS_CODE"))) ? LOAN : DEPOSIT;
        String family = text(product.get("PRODUCT_FAMILY_CODE"));
        Set<String> required = new LinkedHashSet<>(List.of("ELIGIBILITY", "CHANNEL"));
        if (DEPOSIT.equals(domain)) {
            required.addAll(List.of("DEPOSIT_PROFILE", "OPENING", "TRANSACTION", "CLOSURE"));
            if (Set.of("SHORT_TERM_DEPOSIT", "LONG_TERM_DEPOSIT", "CERTIFICATE_OF_DEPOSIT").contains(family)) {
                required.add("TERM");
                required.add("PROFIT_PAYMENT");
            }
            if (Set.of("NOSTRO_ACCOUNT", "VOSTRO_ACCOUNT").contains(family)) required.add("CORRESPONDENT");
        } else {
            required.addAll(List.of("LOAN_PROFILE", "FINANCIAL", "REPAYMENT", "PROCESS"));
        }
        return Set.copyOf(required);
    }

    private static boolean identityReady(Map<String, Object> product) {
        return !text(product.get("PRODUCT_CODE")).isBlank()
                && !text(product.get("PRODUCT_NAME")).isBlank()
                && !text(product.get("PRODUCT_CLASS_CODE")).isBlank()
                && !text(product.get("PRODUCT_FAMILY_CODE")).isBlank()
                && !text(product.get("DEFAULT_CURRENCY_CODE")).isBlank();
    }

    private static String identityDetail(Map<String, Object> product) {
        return text(product.get("PRODUCT_CODE")) + " / " + text(product.get("PRODUCT_CLASS_CODE"))
                + " / " + text(product.get("PRODUCT_FAMILY_CODE"));
    }

    private static String missingRuleDetail(List<ProductModuleValidation> enabled) {
        List<String> details = enabled.stream()
                .filter(v -> !v.dataValid())
                .map(v -> v.moduleCode() + ":" + String.join("|", v.missingTables()))
                .toList();
        return details.isEmpty() ? "—" : String.join("، ", details);
    }

    private static int score(List<ProductReadinessItem> checks) {
        if (checks.isEmpty()) return 0;
        long ok = checks.stream().filter(ProductReadinessItem::ok).count();
        return (int) Math.round(ok * 100.0 / checks.size());
    }

    private static boolean isValidStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("VALID") || normalized.equals("VALIDATED") || normalized.equals("PASSED");
    }

    private static boolean flag(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        return "1".equals(value.toString()) || "TRUE".equalsIgnoreCase(value.toString()) || "Y".equalsIgnoreCase(value.toString());
    }

    private static long number(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString()); }
        catch (NumberFormatException ex) { return 0; }
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString().trim().toUpperCase(Locale.ROOT);
    }

    private static String actorName(String actor) {
        return actor == null || actor.isBlank() ? "prototype-ui" : actor.trim();
    }

    private static Map<String, Object> nullableMap(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        return result;
    }

    private record Requirement(String resultTable, String filterColumn, String parentTable,
                               String parentPkColumn, String childFkColumn) {
        static Requirement direct(String table) { return new Requirement(table, "PRODUCT_VERSION_ID", null, null, null); }
        static Requirement direct(String table, String filterColumn) { return new Requirement(table, filterColumn, null, null, null); }
        static Requirement child(String parentTable, String parentFilterColumn, String parentPkColumn,
                                 String childTable, String childFkColumn) {
            return new Requirement(childTable, parentFilterColumn, parentTable, parentPkColumn, childFkColumn);
        }
    }

    private record ModulePolicy(String code, String label, String domain, List<Requirement> requirements) {}

    private static final List<ModulePolicy> POLICIES = List.of(
            new ModulePolicy("ELIGIBILITY", "اهلیت مشتری", COMMON, List.of(Requirement.direct("PRODUCT_ELIGIBILITY_RULE"))),
            new ModulePolicy("CHANNEL", "کانال و عملیات", COMMON, List.of(
                    Requirement.direct("PRODUCT_CHANNEL_RULE"),
                    Requirement.child("PRODUCT_CHANNEL_RULE", "PRODUCT_VERSION_ID", "CHANNEL_RULE_ID", "PRODUCT_CHANNEL_OPERATION", "CHANNEL_RULE_ID")
            )),
            new ModulePolicy("ORG_SCOPE", "محدوده سازمانی", COMMON, List.of(Requirement.direct("PRODUCT_ORG_SCOPE"))),
            new ModulePolicy("DOCUMENT", "مدارک", COMMON, List.of(Requirement.direct("PRODUCT_REQUIRED_DOCUMENT"))),
            new ModulePolicy("INQUIRY", "استعلام‌ها", COMMON, List.of(Requirement.direct("PRODUCT_REQUIRED_INQUIRY"))),
            new ModulePolicy("PRICING", "قیمت‌گذاری", COMMON, List.of(
                    Requirement.direct("PRODUCT_PRICING_RULE"),
                    Requirement.child("PRODUCT_PRICING_RULE", "PRODUCT_VERSION_ID", "PRICING_RULE_ID", "PRODUCT_PRICING_COMPONENT", "PRICING_RULE_ID")
            )),
            new ModulePolicy("RELATIONSHIP", "ارتباط محصولات", COMMON, List.of(Requirement.direct("PRODUCT_RELATIONSHIP", "SOURCE_PRODUCT_VERSION_ID"))),
            new ModulePolicy("DEPOSIT_PROFILE", "پروفایل سپرده", DEPOSIT, List.of(Requirement.direct("DEPOSIT_PRODUCT_PROFILE"))),
            new ModulePolicy("OPENING", "قواعد افتتاح", DEPOSIT, List.of(Requirement.direct("DEPOSIT_PRODUCT_OPENING_RULE"))),
            new ModulePolicy("TRANSACTION", "تراکنش و ابزار برداشت", DEPOSIT, List.of(Requirement.direct("DEPOSIT_PRODUCT_TRANSACTION_RULE"))),
            new ModulePolicy("TERM", "مدت، سررسید و تمدید", DEPOSIT, List.of(
                    Requirement.direct("DEPOSIT_PRODUCT_TERM_RULE"),
                    Requirement.child("DEPOSIT_PRODUCT_TERM_RULE", "PRODUCT_VERSION_ID", "TERM_RULE_ID", "DEPOSIT_PRODUCT_ALLOWED_TERM", "TERM_RULE_ID")
            )),
            new ModulePolicy("PROFIT_PAYMENT", "برنامه واریز سود", DEPOSIT, List.of(Requirement.direct("DEPOSIT_PROFIT_PAYMENT_RULE"))),
            new ModulePolicy("CORRESPONDENT", "نوسترو / وسترو", DEPOSIT, List.of(
                    Requirement.direct("CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE"),
                    Requirement.child("CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE", "PRODUCT_VERSION_ID", "CORRESPONDENT_PRODUCT_PROFILE_ID", "CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE", "CORRESPONDENT_PRODUCT_PROFILE_ID")
            )),
            new ModulePolicy("JOINT", "مالکیت مشترک", DEPOSIT, List.of(Requirement.direct("DEPOSIT_PRODUCT_JOINT_RULE"))),
            new ModulePolicy("DORMANCY", "راکدی", DEPOSIT, List.of(Requirement.direct("DEPOSIT_PRODUCT_DORMANCY_RULE"))),
            new ModulePolicy("HOLD", "مسدودی", DEPOSIT, List.of(Requirement.direct("DEPOSIT_PRODUCT_HOLD_RULE"))),
            new ModulePolicy("CLOSURE", "بستن و تسویه", DEPOSIT, List.of(Requirement.direct("DEPOSIT_PRODUCT_CLOSURE_RULE"))),
            new ModulePolicy("LOAN_PROFILE", "پروفایل تسهیلات", LOAN, List.of(Requirement.direct("LOAN_PRODUCT_PROFILE"))),
            new ModulePolicy("LOAN_ELIGIBILITY", "اعتبارسنجی تکمیلی", LOAN, List.of(
                    Requirement.child("PRODUCT_ELIGIBILITY_RULE", "PRODUCT_VERSION_ID", "ELIGIBILITY_RULE_ID", "LOAN_ELIGIBILITY_EXTENSION", "ELIGIBILITY_RULE_ID")
            )),
            new ModulePolicy("FINANCIAL", "پارامترهای مالی", LOAN, List.of(Requirement.direct("LOAN_FINANCIAL_EXTENSION"))),
            new ModulePolicy("REPAYMENT", "بازپرداخت", LOAN, List.of(Requirement.direct("LOAN_PRODUCT_REPAYMENT_RULE"))),
            new ModulePolicy("PROCESS", "فرایند و تصویب", LOAN, List.of(Requirement.direct("LOAN_PRODUCT_PROCESS_RULE"))),
            new ModulePolicy("COLLATERAL", "وثایق", LOAN, List.of(Requirement.direct("LOAN_PRODUCT_COLLATERAL_RULE")))
    );
}
