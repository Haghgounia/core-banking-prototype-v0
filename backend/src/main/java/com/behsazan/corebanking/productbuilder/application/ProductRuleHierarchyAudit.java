package com.behsazan.corebanking.productbuilder.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HF7-A3/HF4-HF9: read-only publication evidence for governed parent/child
 * contracts. Existence is not proof: children must be valid and complete.
 * HF9 additionally audits half-open amount partitions for tiered pricing, per
 * component and per effective date. Undefined term semantics remain fail-closed.
 * This does NOT resolve policy inheritance, channel/customer scope or runtime
 * pricing/posting. No SQL/write operation here.
 */
public final class ProductRuleHierarchyAudit {
    private ProductRuleHierarchyAudit() {}

    @FunctionalInterface
    public interface ChildLoader {
        List<Map<String, Object>> load(String childTable, String foreignKey, long parentId);
    }

    /**
     * Compatibility contract for the earlier HF7-A3 tests: three additional,
     * optional child families that predate the specialized HF4 term/tier audit.
     * The result is purely read-only and DOES NOT by itself certify canApprove
     * or operational canOpen; publication wiring is a distinct change.
     *
     * A missing child is allowed for these three OPTIONAL child families, but
     * an existing, applicable child must be valid and belong to its parent.
     */
    public record AuditResult(int invalidCount, String firstProblem) {
        public boolean valid() { return invalidCount == 0; }
    }

    public static AuditResult audit(String parentTable,
                                    List<? extends Map<String, ?>> parents,
                                    LocalDate from, LocalDate to,
                                    String domain,
                                    ChildLoader loader,
                                    ProductBuilderBusinessValidator validator) {
        if (parentTable == null || parents == null || from == null || to == null ||
                loader == null || validator == null || to.isBefore(from))
            return new AuditResult(1, "ورودی ممیزی سلسله‌مراتب قاعده معتبر نیست.");
        String parent = parentTable.trim().toUpperCase(java.util.Locale.ROOT);
        String productDomain = domain == null ? "" : domain.trim().toUpperCase(java.util.Locale.ROOT);
        List<String> problems = new ArrayList<>();
        for (Map<String, ?> original : parents) {
            if (original == null) {
                problems.add("قاعده مادر موجود نیست.");
                continue;
            }
            // Evaluation expects Map<String,Object>; never mutate source evidence.
            Map<String, Object> row = new java.util.LinkedHashMap<>(original);
            if (ProductRuleEvidenceEvaluator.evaluate(parent, List.of(row), from, to).unrecognizedStatuses() > 0) {
                problems.add("وضعیت قاعده مادر معتبر نیست: " + parent);
                continue;
            }
            try {
                if (ProductRuleEvidenceEvaluator.hasInvalidActivePeriod(parent, row)) {
                    problems.add("بازه اعتبار قاعده مادر معکوس است: " + parent);
                    continue;
                }
                if (!ProductRuleEvidenceEvaluator.isApplicableWithin(parent, row, from, to)) continue;
            } catch (ProductBuilderValidationException ex) {
                problems.add("تاریخ قاعده مادر معتبر نیست: " + ex.getMessage());
                continue;
            }
            final String parentIdField;
            final String[] childTables;
            switch (parent) {
                case "PRODUCT_CHANNEL_RULE" -> {
                    parentIdField = "CHANNEL_RULE_ID";
                    childTables = new String[]{"PRODUCT_CHANNEL_OPERATION"};
                }
                case "PRODUCT_ELIGIBILITY_RULE" -> {
                    parentIdField = "ELIGIBILITY_RULE_ID";
                    // Criteria belong to COMMON eligibility for both domains;
                    // LOAN-only extension is additive, not a substitute.
                    childTables = "LOAN".equals(productDomain)
                            ? new String[]{"PRODUCT_ELIGIBILITY_CRITERION", "LOAN_ELIGIBILITY_EXTENSION"}
                            : new String[]{"PRODUCT_ELIGIBILITY_CRITERION"};
                }
                case "CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE" -> {
                    if (!"DEPOSIT".equals(productDomain)) continue;
                    parentIdField = "CORRESPONDENT_PRODUCT_PROFILE_ID";
                    childTables = new String[]{"CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE"};
                }
                case "DEPOSIT_PRODUCT_CLOSURE_RULE" -> {
                    if (!"DEPOSIT".equals(productDomain)) continue;
                    parentIdField = "CLOSURE_RULE_ID";
                    childTables = new String[]{"DEPOSIT_PRODUCT_CLOSURE_PRECHECK",
                            "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE", "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE"};
                }
                default -> {
                    problems.add("جدول مادر در این قرارداد ممیزی تعریف نشده است: " + parent);
                    continue;
                }
            }
            Long id = strictId(row.get(parentIdField));
            if (id == null) {
                problems.add("شناسه قاعده مادر برای ممیزی فرزندان معتبر نیست: " + parentIdField);
                continue;
            }
            for (String childTable : childTables) {
                List<Map<String, Object>> children = get(loader, childTable, parentIdField, id);
                if (children == null) {
                    problems.add("قواعد فرزند قابل بازیابی کامل نیستند: " + childTable);
                    continue;
                }
                List<Map<String, Object>> validChildren = new ArrayList<>();
                for (Map<String, Object> child : children) {
                    if (child == null) {
                        problems.add("رکورد فرزند نامعتبر است: " + childTable);
                        continue;
                    }
                    try {
                        if (ProductRuleEvidenceEvaluator.evaluate(childTable, List.of(child), from, to)
                                .unrecognizedStatuses() > 0) {
                            problems.add("وضعیت رکورد فرزند معتبر نیست: " + childTable);
                            continue;
                        }
                        if (ProductRuleEvidenceEvaluator.hasInvalidActivePeriod(childTable, child)) {
                            problems.add("بازه اعتبار قاعده فرزند معکوس است: " + childTable);
                            continue;
                        }
                        if (!ProductRuleEvidenceEvaluator.isApplicableWithin(childTable, child, from, to)) continue;
                        if (!Objects.equals(id, strictId(child.get(parentIdField)))) {
                            problems.add("ارتباط قاعده فرزند با مادر ناسازگار است: " + childTable);
                            continue;
                        }
                        validator.validate(childTable, child);
                        validChildren.add(child);
                        if ("PRODUCT_CHANNEL_OPERATION".equals(childTable)
                                && numericZero(row.get("IS_ALLOWED")) && numericOne(child.get("IS_ALLOWED")))
                            problems.add("در کانال غیرمجاز، عملیات مجاز قابل تعریف نیست.");
                    } catch (ProductBuilderValidationException ex) {
                        problems.add("قاعده فرزند " + childTable + ": " + ex.getMessage());
                    }
                }
                if ("PRODUCT_ELIGIBILITY_CRITERION".equals(childTable)) {
                    // OR is permitted across different values of a dimension,
                    // but identical values and gender outside person-only scope
                    // are not publishable. A missing criterion remains a valid
                    // ALL/wildcard choice, not evidence of runtime eligibility.
                    java.util.Set<String> seen = new java.util.HashSet<>();
                    java.util.List<String> customerTypes = new ArrayList<>();
                    boolean hasGender = false;
                    for (Map<String, Object> criterion : validChildren) {
                        String type = upper(criterion.get("CRITERION_TYPE_CODE"));
                        String code = upper(criterion.get("CRITERION_VALUE_CODE"));
                        String numeric = criterion.get("CRITERION_VALUE_NUMBER") == null ? ""
                                : String.valueOf(decimal(criterion.get("CRITERION_VALUE_NUMBER")));
                        String unit = upper(criterion.get("VALUE_UNIT_CODE"));
                        String unique = type + ":" + (code.isBlank() ? numeric + ":" + unit : code);
                        if (!seen.add(unique)) problems.add("معیار اهلیت تکراری در قاعده مادر تعریف شده است: " + type);
                        if ("CUSTOMER_TYPE".equals(type)) customerTypes.add(code);
                        if ("GENDER".equals(type)) hasGender = true;
                    }
                    if (hasGender && (customerTypes.isEmpty()
                            || customerTypes.stream().anyMatch(type -> !java.util.Set.of("PERSON", "INDIVIDUAL").contains(type))))
                        problems.add("معیار جنسیت فقط برای دامنه صرفاً مشتری حقیقی مجاز است.");
                }
            }
        }
        return new AuditResult(problems.size(), problems.isEmpty() ? null : problems.get(0));
    }

    public record Finding(String status, String reason) {
        public boolean valid() { return status == null; }
        public boolean invalidSemantics() { return "INVALID_SEMANTICS".equals(status); }
        public boolean conflict() { return "CONFLICT".equals(status); }
    }

    private static Finding ok() { return new Finding(null, null); }
    private static Finding invalid(String reason) { return new Finding("INVALID_SEMANTICS", reason); }
    private static Finding conflict(String reason) { return new Finding("CONFLICT", reason); }

    public static Finding allowedTerms(List<Map<String, Object>> parents,
                                       LocalDate from, LocalDate to,
                                       ChildLoader loader,
                                       ProductBuilderBusinessValidator validator) {
        try {
        for (var parent : parents) {
            if (!ProductRuleEvidenceEvaluator.isApplicableWithin("DEPOSIT_PRODUCT_TERM_RULE", parent, from, to)) continue;
            Long id = strictId(parent.get("TERM_RULE_ID"));
            if (id == null) return invalid("شناسه قاعده مادر مدت سپرده معتبر نیست.");
            List<Map<String, Object>> children = get(loader, "DEPOSIT_PRODUCT_ALLOWED_TERM", "TERM_RULE_ID", id);
            if (children == null) return conflict("مدت‌های مجاز قاعده مادر قابل بازیابی نیستند؛ تصویب تا رفع نقص متوقف است.");
            List<Map<String, Object>> applicable = new ArrayList<>();
            for (var child : children) {
                if (ProductRuleEvidenceEvaluator.hasInvalidActivePeriod("DEPOSIT_PRODUCT_ALLOWED_TERM", child))
                    return invalid("مدت مجاز سپرده دارای بازه اعتبار معکوس است (قاعده مادر " + id + ").");
                if (!ProductRuleEvidenceEvaluator.isApplicableWithin("DEPOSIT_PRODUCT_ALLOWED_TERM", child, from, to)) continue;
                if (!Objects.equals(id, strictId(child.get("TERM_RULE_ID"))))
                    return invalid("ارتباط مدت مجاز سپرده با شناسه قاعده مادر سازگار نیست (" + id + ").");
                try { validator.validate("DEPOSIT_PRODUCT_ALLOWED_TERM", child); }
                catch (ProductBuilderValidationException ex) {
                    return invalid("مدت مجاز قاعده " + id + ": " + ex.getMessage());
                }
                applicable.add(child);
            }
            for (int a = 0; a < applicable.size(); a++) {
                for (int b = a + 1; b < applicable.size(); b++) {
                    var left = applicable.get(a);
                    var right = applicable.get(b);
                    if (!ProductRuleEvidenceEvaluator.overlap(left, right)) continue;
                    if (numericOne(left.get("IS_DEFAULT")) && numericOne(right.get("IS_DEFAULT")))
                        return conflict("دو مدت پیش‌فرض فعال با بازه‌های اعتبار متداخل برای قاعده " + id + " وجود دارد.");
                    if (same(left, right, "TERM_UNIT_CODE") && sameNumber(left, right, "TERM_VALUE"))
                        return conflict("مدت مجاز تکراری با واحد و مقدار یکسان در بازه همپوشان برای قاعده " + id + " وجود دارد.");
                }
            }
            var coverage = ProductRuleEvidenceEvaluator.temporalCoverage("DEPOSIT_PRODUCT_ALLOWED_TERM", applicable, from, to);
            if (!coverage.complete())
                return conflict("مدت‌های مجاز قاعده " + id + " تمام اعتبار نسخه را پوشش نمی‌دهند؛ نخستین روز فاقد پوشش: "
                        + coverage.firstGap() + ".");
        }
        return ok();
        } catch (java.time.format.DateTimeParseException | ProductBuilderValidationException ex) {
            return invalid("تاریخ یا وضعیت قاعده مدت/فرزند معتبر نیست: " + ex.getMessage());
        }
    }

    /**
     * HF9: per-component, per-date SINGLE_APPLICABLE_RATE publication gate.
     * Amount bounds are half-open; duration-dependent rules remain closed until
     * the bank supplies an explicit comparable term domain / boundary policy.
     */
    public static Finding pricingTiers(List<Map<String, Object>> parents,
                                       LocalDate from, LocalDate to,
                                       ChildLoader loader,
                                       ProductBuilderBusinessValidator validator) {
        try {
            for (var parent : parents) {
                if (!ProductRuleEvidenceEvaluator.isApplicableWithin("PRODUCT_PRICING_RULE", parent, from, to))
                    continue;
                // A method/flag disagreement cannot be safely resolved by a
                // generic priority or an inferred pricing execution strategy.
                boolean tiered = numericOne(parent.get("IS_TIERED_RATE"));
                boolean tierMethod = "TIERED".equals(upper(parent.get("PRICING_METHOD_CODE")));
                if (tiered != tierMethod)
                    return invalid("روش قیمت‌گذاری TIERED با پرچم پلکانی بودن نرخ ناسازگار است؛ تصویب متوقف است.");
                if (!tiered) continue;
                Long ruleId = strictId(parent.get("PRICING_RULE_ID"));
                if (ruleId == null) return invalid("شناسه قاعده قیمت‌گذاری پله‌ای معتبر نیست.");
                LocalDate scopedFrom = later(from, parsedDate(parent, "VALID_FROM", LocalDate.MIN));
                LocalDate scopedTo = earlier(to, parsedDate(parent, "VALID_TO", LocalDate.MAX));
                if (scopedTo.isBefore(scopedFrom)) continue;
                var components = get(loader, "PRODUCT_PRICING_COMPONENT", "PRICING_RULE_ID", ruleId);
                if (components == null) return conflict("اجزای قیمت‌گذاری قاعده " + ruleId + " قابل بازیابی نیستند.");
                if (components.isEmpty()) return conflict("قاعده قیمت‌گذاری پله‌ای " + ruleId + " فاقد جزء قیمت‌گذاری است.");
                int audited = 0;
                for (var component : components) {
                    if (component == null) return invalid("رکورد جزء قیمت‌گذاری موجود نیست.");
                    if (ProductRuleEvidenceEvaluator.evaluate("PRODUCT_PRICING_COMPONENT",
                            List.of(component), scopedFrom, scopedTo).unrecognizedStatuses() > 0)
                        return invalid("وضعیت جزء قیمت‌گذاری شناخته‌شده نیست (قاعده " + ruleId + ").");
                    if (ProductRuleEvidenceEvaluator.hasInvalidActivePeriod("PRODUCT_PRICING_COMPONENT", component))
                        return invalid("بازه اعتبار جزء قیمت‌گذاری معکوس است (قاعده " + ruleId + ").");
                    if (!ProductRuleEvidenceEvaluator.isApplicableWithin("PRODUCT_PRICING_COMPONENT", component, scopedFrom, scopedTo))
                        continue;
                    if (!Objects.equals(ruleId, strictId(component.get("PRICING_RULE_ID"))))
                        return invalid("ارتباط جزء قیمت‌گذاری با قاعده " + ruleId + " معتبر نیست.");
                    Long componentId = strictId(component.get("PRICING_COMPONENT_ID"));
                    if (componentId == null) return invalid("شناسه جزء قیمت‌گذاری قاعده " + ruleId + " معتبر نیست.");
                    try { validator.validate("PRODUCT_PRICING_COMPONENT", component); }
                    catch (ProductBuilderValidationException ex) {
                        return invalid("جزء قیمت‌گذاری " + componentId + ": " + ex.getMessage());
                    }
                    LocalDate componentFrom = later(scopedFrom, parsedDate(component, "VALID_FROM", LocalDate.MIN));
                    LocalDate componentTo = earlier(scopedTo, parsedDate(component, "VALID_TO", LocalDate.MAX));
                    if (componentTo.isBefore(componentFrom)) continue;
                    audited++;
                    var children = get(loader, "PRODUCT_RATE_TIER", "PRICING_COMPONENT_ID", componentId);
                    if (children == null) return conflict("پله‌های نرخ جزء " + componentId + " قابل بازیابی نیستند.");
                    // HF9-COVERAGE-GUARD: proof is per component. A second
                    // component with no owned tier MUST fail as CONFLICT even
                    // if another component has complete amount coverage.
                    if (children.isEmpty()) return conflict("جزء قیمت‌گذاری " + componentId
                            + " هیچ پله نرخ متعلق به خود ندارد؛ پوشش جزء دیگری قابل استفاده نیست.");
                    List<Map<String, Object>> relevant = new ArrayList<>();
                    for (var tier : children) {
                        if (tier == null) return invalid("رکورد پله نرخ جزء " + componentId + " خالی است.");
                        if (ProductRuleEvidenceEvaluator.evaluate("PRODUCT_RATE_TIER",
                                List.of(tier), componentFrom, componentTo).unrecognizedStatuses() > 0)
                            return invalid("وضعیت پله نرخ جزء " + componentId + " معتبر نیست.");
                        if (ProductRuleEvidenceEvaluator.hasInvalidActivePeriod("PRODUCT_RATE_TIER", tier))
                            return invalid("پله نرخ جزء " + componentId + " بازه اعتبار معکوس دارد.");
                        if (!ProductRuleEvidenceEvaluator.isApplicableWithin("PRODUCT_RATE_TIER", tier, componentFrom, componentTo))
                            continue;
                        if (!Objects.equals(componentId, strictId(tier.get("PRICING_COMPONENT_ID"))))
                            return invalid("ارتباط پله نرخ با جزء قیمت‌گذاری " + componentId + " معتبر نیست.");
                        try { validator.validate("PRODUCT_RATE_TIER", tier); }
                        catch (ProductBuilderValidationException ex) {
                            return invalid("پله نرخ جزء " + componentId + ": " + ex.getMessage());
                        }
                        relevant.add(tier);
                    }
                    for (int a=0; a<relevant.size(); a++) for (int b=a+1; b<relevant.size(); b++) {
                        if (sameNumber(relevant.get(a), relevant.get(b), "TIER_NO")
                                && ProductRuleEvidenceEvaluator.overlap(relevant.get(a), relevant.get(b)))
                            return conflict("شماره پله نرخ تکراری در یک جزء قیمت‌گذاری و بازه اعتبار متداخل است (" + componentId + ").");
                    }
                    Finding check = ProductPricingSingleRatePolicy.auditComponent(componentId,
                            component, relevant, componentFrom, componentTo);
                    if (!check.valid()) return check;
                }
                if (audited == 0) return conflict("قاعده قیمت‌گذاری پله‌ای " + ruleId + " جزء مؤثر قابل ممیزی ندارد.");
            }
            return ok();
        } catch (java.time.format.DateTimeParseException | ProductBuilderValidationException ex) {
            return invalid("تاریخ، مبلغ یا وضعیت قاعده قیمت‌گذاری/فرزند معتبر نیست: " + ex.getMessage());
        }
    }

    private static List<Map<String, Object>> get(ChildLoader loader, String table, String key, long parentId) {
        try { return loader.load(table, key, parentId); }
        catch (RuntimeException ex) { return null; } // fail closed with a visible governance finding
    }
    private static String upper(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toUpperCase(java.util.Locale.ROOT);
    }
    private static boolean same(Map<String, Object> a, Map<String, Object> b, String field) {
        return Objects.equals(String.valueOf(a.get(field)).trim().toUpperCase(),
                String.valueOf(b.get(field)).trim().toUpperCase());
    }
    private static boolean sameNumber(Map<String, Object> a, Map<String, Object> b, String field) {
        BigDecimal left = decimal(a.get(field)), right = decimal(b.get(field));
        return left != null && right != null && left.compareTo(right) == 0;
    }
    private static boolean numericZero(Object value) {
        BigDecimal n = decimal(value);
        return n != null && n.compareTo(BigDecimal.ZERO) == 0;
    }
    private static boolean numericOne(Object value) {
        BigDecimal n = decimal(value);
        return n != null && n.compareTo(BigDecimal.ONE) == 0;
    }
    private static BigDecimal decimal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try { return new BigDecimal(String.valueOf(value).trim()); }
        catch (NumberFormatException ex) { return null; }
    }
    private static Long strictId(Object value) {
        BigDecimal n = decimal(value);
        if (n == null || n.signum() <= 0) return null;
        try { return n.longValueExact(); }
        catch (ArithmeticException ex) { return null; }
    }
    private static LocalDate parsedDate(Map<String, Object> row, String field, LocalDate defaultValue) {
        Object value = row.get(field);
        if (value == null || String.valueOf(value).isBlank()) return defaultValue;
        String s = String.valueOf(value);
        return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
    }
    private static LocalDate later(LocalDate a, LocalDate b) { return a.isAfter(b) ? a : b; }
    private static LocalDate earlier(LocalDate a, LocalDate b) { return a.isBefore(b) ? a : b; }
}
