package com.behsazan.corebanking.fee3.f1;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r3.Fee3PreviewEngine;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FEE3-F1: USAGE / per-unit SERVICE_FEE, explicitly unapproved UAT-only preview.
 * No Oracle writes, tariff activation, payment, booking, or GL posting.
 * Input usage is a dimensionless integer: legacy RULE.AMOUNT_BASE_CODE remains REQUEST_AMOUNT
 * for physical schema compatibility but is NEVER used as a money amount for this method.
 */
public final class Fee3F1UsageContract {
    private Fee3F1UsageContract() {}
    public static final String CONTRACT = "FEE3_USAGE_F1_PREVIEW_V1";
    private static final BigDecimal MAX = new BigDecimal("1000000000000000000");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final Set<String> ROUNDINGS = Set.of("HALF_UP", "HALF_EVEN", "FLOOR", "DOWN", "UP", "CEIL");
    private static final Set<String> CONFIG_KEYS = Set.of("basisCode", "basisExplicit", "rateSource",
        "unitPrice", "freeUnits", "priceStatus", "fixedAmount", "fixedPart", "ratePercent",
        "tiers", "brackets", "matrix", "formula", "formulaParams", "dayBasis", "timeEnabled",
        "minOutput", "maxOutput");

    public record Request(DraftRequest draft, Map<String,Object> inputs, LocalDate effectiveOn) {}
    public record Evidence(String contractVersion, String status, boolean persisted,
        boolean financialPosting, boolean published, boolean approved, String simulationMode,
        Fee3R4CalculationContract.VersionReference versionReference, LocalDate oracleBusinessDate,
        LocalDate effectiveOn, String currency, String roundingCode, int roundingScale,
        String snapshotSha256, Fee3PreviewEngine.PreviewResult calculation, List<String> diagnostics) {}

    public static String guard(Request request, LocalDate bankDay) {
        if (bankDay == null) return "ORACLE_BUSINESS_DATE_REQUIRED";
        if (request == null || request.draft() == null || request.draft().item() == null ||
            request.draft().version() == null || request.draft().scope() == null)
            return "DRAFT_INCOMPLETE";
        var d = request.draft(); var v = d.version();
        if (!"SERVICE_FEE".equals(d.item().amountKind())) return "SERVICE_FEE_ONLY";
        if (!"USAGE".equals(v.method())) return "USAGE_METHOD_REQUIRED";
        if (!"IRR".equals(v.feeCurrency()) || !"IRR".equals(v.serviceCurrency()) ||
            !"IRR".equals(d.scope().currency())) return "IRR_ONLY_NO_FX";
        LocalDate effective = request.effectiveOn() == null ? bankDay : request.effectiveOn();
        if (effective.isBefore(bankDay)) return "HISTORICAL_PREVIEW_NOT_ALLOWED";
        if (v.validFrom() == null || effective.isBefore(v.validFrom()) ||
            (v.validTo() != null && (v.validTo().isBefore(v.validFrom()) || effective.isAfter(v.validTo()))))
            return "OUTSIDE_DRAFT_VALIDITY";
        if (v.rounding() == null || !ROUNDINGS.contains(v.rounding())) return "ROUNDING_UNREVIEWED";
        if (v.origin() == null || !Set.of("BANK_INTERNAL", "CBI_MANDATORY").contains(v.origin())) return "TARIFF_ORIGIN_UNREVIEWED";
        if ("CBI_MANDATORY".equals(v.origin()) && v.sourceId() == null)
            return "CBI_SOURCE_REQUIRED";
        if (v.uiContext() != null && !v.uiContext().isEmpty()) return "SPECIALIZED_CONTEXT_NOT_CERTIFIED";
        if (d.rules() == null || d.rules().size() != 1 || d.rules().get(0) == null)
            return "EXACTLY_ONE_RULE_REQUIRED";
        var r = d.rules().get(0);
        if (r.order() != 1 || !"USAGE".equals(r.method())) return "RULE_METHOD_OR_ORDER_MISMATCH";
        if (!"REQUEST_AMOUNT".equals(r.base())) return "LEGACY_AMOUNT_BASE_REQUIRED";
        if (!"RULE".equals(r.rateSource()) || r.rate() != null || r.fixed() != null ||
            r.coverage() != null || (r.collateral() != null && !r.collateral().isBlank()) ||
            (r.formulaCode() != null && !r.formulaCode().isBlank()))
            return "USAGE_RULE_HAS_UNREVIEWED_MODIFIERS";
        if (!money(r.min(), true) || !money(r.max(), true) ||
            (r.min() != null && r.max() != null && r.min().compareTo(r.max()) > 0))
            return "INVALID_MIN_MAX";
        if ((d.calcItemId() == null) != (d.tariffVersionId() == null))
            return "PARTIAL_ORACLE_REFERENCE";
        if (d.calcItemId() == null && (d.expectedItemRecordVersion() != null ||
            d.expectedVersionRecordVersion() != null)) return "UNPERSISTED_RECORD_VERSION_UNEXPECTED";
        if (d.calcItemId() != null && (d.calcItemId() <= 0 || d.tariffVersionId() <= 0 ||
            d.expectedItemRecordVersion() == null || d.expectedItemRecordVersion() <= 0 ||
            d.expectedVersionRecordVersion() == null || d.expectedVersionRecordVersion() <= 0))
            return "INVALID_ORACLE_VERSION_REFERENCE";
        if (request.inputs() == null || !request.inputs().keySet().equals(Set.of("usage")))
            return "EXPLICIT_USAGE_ONLY";
        BigDecimal usage = numeric(request.inputs().get("usage"));
        if (!whole(usage)) return "USAGE_MUST_BE_NONNEGATIVE_INTEGER";
        Map<String,Object> policy = v.financialPolicy() == null ? Map.of() : v.financialPolicy();
        if (policy.keySet().stream().anyMatch(key -> !"taxPercent".equals(key)))
            return "UNREVIEWED_FINANCIAL_POLICY";
        if ("TAXABLE".equals(v.taxMode())) {
            if (!percent(policy.get("taxPercent"))) return "EXPLICIT_TAX_PERCENT_REQUIRED";
        } else if (v.taxMode() != null && Set.of("EXEMPT", "NONE", "NON_TAXABLE").contains(v.taxMode())) {
            if (policy.containsKey("taxPercent") && policy.get("taxPercent") != null)
                return "TAX_PERCENT_NOT_ALLOWED_FOR_NON_TAXABLE";
        } else return "TAX_POLICY_UNREVIEWED";
        if (r.parameters() == null || !r.parameters().keySet().equals(Set.of("originalConfig")))
            return "REVIEWED_ORIGINAL_CONFIG_REQUIRED";
        if (!(r.parameters().get("originalConfig") instanceof Map<?,?> cfg) ||
            !CONFIG_KEYS.containsAll(cfg.keySet())) return "UNKNOWN_USAGE_CONFIG_FIELD";
        if (!"REQUEST_AMOUNT".equals(cfg.get("basisCode")) ||
            (cfg.containsKey("basisExplicit") && !Boolean.TRUE.equals(cfg.get("basisExplicit"))) ||
            (cfg.get("rateSource") != null && !"CONFIG".equals(cfg.get("rateSource"))))
            return "USAGE_BASIS_OR_SOURCE_NOT_REVIEWED";
        if (nonempty(cfg.get("priceStatus"))) return "UNIT_PRICE_SOURCE_PENDING";
        BigDecimal unitPrice = numeric(cfg.get("unitPrice"));
        if (!money(unitPrice, false)) return "UNIT_PRICE_MISSING_OR_INVALID";
        BigDecimal freeUnits = numeric(cfg.get("freeUnits"));
        if (!whole(freeUnits)) return "FREE_UNITS_MUST_BE_NONNEGATIVE_INTEGER";
        if (nonempty(cfg.get("fixedAmount")) || nonempty(cfg.get("fixedPart")) ||
            nonempty(cfg.get("ratePercent")) || nonempty(cfg.get("formula")) ||
            (cfg.get("timeEnabled") != null && !Boolean.FALSE.equals(cfg.get("timeEnabled"))) ||
            notEmptyListOrInvalid(cfg.get("tiers")) || notEmptyListOrInvalid(cfg.get("brackets")) ||
            notEmptyListOrInvalid(cfg.get("matrix")) || notEmptyListOrInvalid(cfg.get("formulaParams")))
            return "ADVANCED_USAGE_MODIFIERS_NOT_CERTIFIED";
        if (cfg.get("minOutput") != null && !sameMoney(r.min(), cfg.get("minOutput")))
            return "MINIMUM_DISAGREES_WITH_RULE";
        if (cfg.get("maxOutput") != null && !sameMoney(r.max(), cfg.get("maxOutput")))
            return "MAXIMUM_DISAGREES_WITH_RULE";
        if (usage.compareTo(freeUnits) <= 0 && r.min() != null && r.min().signum() > 0)
            return "MIN_FEE_FOR_FREE_OR_ZERO_UNITS_REQUIRES_POLICY";
        return null;
    }

    private static boolean nonempty(Object x) {
        return x != null && !(x instanceof String s && s.isBlank());
    }
    private static boolean notEmptyListOrInvalid(Object x) {
        return x != null && (!(x instanceof List<?> list) || !list.isEmpty());
    }
    private static BigDecimal numeric(Object input) {
        if (!(input instanceof Number || input instanceof String)) return null;
        String raw = String.valueOf(input);
        if (raw.length() > 42) return null;
        try { return new BigDecimal(raw); }
        catch (NumberFormatException ex) { return null; }
    }
    private static boolean whole(BigDecimal b) {
        return b != null && b.signum() >= 0 && b.compareTo(MAX) <= 0 &&
            b.stripTrailingZeros().scale() <= 0;
    }
    private static boolean money(BigDecimal b, boolean allowNull) {
        return b == null ? allowNull : b.signum() >= 0 && b.compareTo(MAX) <= 0 &&
            Math.max(0, b.stripTrailingZeros().scale()) <= 2;
    }
    private static boolean percent(Object x) {
        BigDecimal b = numeric(x);
        return b != null && b.signum() >= 0 && b.compareTo(HUNDRED) <= 0 &&
            Math.max(0, b.stripTrailingZeros().scale()) <= 8;
    }
    private static boolean sameMoney(BigDecimal a, Object value) {
        BigDecimal b = numeric(value);
        return a != null && b != null && money(b, false) && a.compareTo(b) == 0;
    }
    private static RoundingMode rounding(String code) {
        return switch (code) {
            case "HALF_UP" -> RoundingMode.HALF_UP;
            case "HALF_EVEN" -> RoundingMode.HALF_EVEN;
            case "FLOOR", "DOWN" -> RoundingMode.FLOOR;
            case "CEIL", "UP" -> RoundingMode.CEILING;
            default -> throw new IllegalArgumentException("ROUNDING_NOT_CERTIFIED");
        };
    }

    /** Pure arithmetic; controller/service MUST call guard and Oracle R3 preflight before this method. */
    public static Fee3PreviewEngine.PreviewResult calculate(Request request) {
        var rule = request.draft().rules().get(0);
        Map<?,?> cfg = (Map<?,?>) rule.parameters().get("originalConfig");
        BigDecimal units = numeric(request.inputs().get("usage"));
        BigDecimal allowance = numeric(cfg.get("freeUnits"));
        BigDecimal rate = numeric(cfg.get("unitPrice"));
        BigDecimal charged = units.subtract(allowance).max(BigDecimal.ZERO);
        BigDecimal raw = charged.multiply(rate);
        if (raw.signum() < 0 || raw.compareTo(MAX) > 0)
            throw new IllegalArgumentException("RAW_FEE_EXCEEDS_SAFE_LIMIT");
        BigDecimal adjusted = raw;
        if (rule.min() != null) adjusted = adjusted.max(rule.min());
        if (rule.max() != null) adjusted = adjusted.min(rule.max());
        RoundingMode round = rounding(request.draft().version().rounding());
        BigDecimal fee = adjusted.setScale(0, round);
        BigDecimal tax = BigDecimal.ZERO;
        if ("TAXABLE".equals(request.draft().version().taxMode())) {
            BigDecimal taxRate = numeric(request.draft().version().financialPolicy().get("taxPercent"));
            tax = fee.multiply(taxRate).divide(HUNDRED, 12, RoundingMode.HALF_EVEN).setScale(0, round);
        }
        BigDecimal total = fee.add(tax);
        if (total.compareTo(MAX) > 0) throw new IllegalArgumentException("TOTAL_EXCEEDS_SAFE_LIMIT");
        Map<String,Object> trace = new LinkedHashMap<>();
        trace.put("usageUnits", units); trace.put("freeUnits", allowance);
        trace.put("chargedUnits", charged); trace.put("unitPriceIrr", rate);
        trace.put("rawFeeIrr", raw); trace.put("cappedFeeIrr", adjusted);
        return new Fee3PreviewEngine.PreviewResult("SIMULATED", false, false, "USAGE", raw, fee, tax, total,
            List.of(trace), List.of("UAT_SIMULATION_ONLY", "USAGE_UNIT_COUNT_NOT_MONEY_BASIS",
                "NO_ORACLE_MUTATION", "NO_ACCOUNTING_POSTING"));
    }

    public static Evidence blocked(Request req, LocalDate bankDay, String code) {
        var v = req == null || req.draft() == null ? null : req.draft().version();
        LocalDate when = req == null || req.effectiveOn() == null ? bankDay : req.effectiveOn();
        return new Evidence(CONTRACT, "BLOCKED", false, false, false, false,
            "UAT_DRAFT_SIMULATION", null, bankDay, when, "IRR", v == null ? null : v.rounding(),
            0, null, null, List.of(code));
    }
    public static Evidence finish(Request req, LocalDate bankDay,
            Fee3R4CalculationContract.VersionReference ref, Fee3PreviewEngine.PreviewResult calc) {
        if (ref == null || ref.active() ||
            !("DRAFT".equals(ref.status()) || "UNSAVED".equals(ref.status())) ||
            calc == null || !"SIMULATED".equals(calc.status()) || calc.persisted() ||
            calc.financialPosting() || calc.rawIrr() == null || calc.feeIrr() == null ||
            calc.taxIrr() == null || calc.totalIrr() == null ||
            calc.feeIrr().scale() != 0 || calc.totalIrr().scale() != 0)
            return blocked(req, bankDay, "UNSAFE_RESULT_OR_REFERENCE");
        LocalDate on = req.effectiveOn() == null ? bankDay : req.effectiveOn();
        String hash;
        try {
            String snapshot = Fee3R4CalculationContract.fingerprint(
                new Fee3R4CalculationContract.Request(req.draft(), req.inputs(), req.effectiveOn()), ref, on);
            String payload = CONTRACT + "|" + snapshot + "|" + calc.rawIrr().toPlainString() +
                "|" + calc.totalIrr().toPlainString();
            hash = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA256_UNAVAILABLE", ex); }
        return new Evidence(CONTRACT, "SIMULATED", false, false, false, false,
            on.isAfter(bankDay) ? "FUTURE_DATED_UAT_SIMULATION" : "UAT_DRAFT_SIMULATION",
            ref, bankDay, on, "IRR", req.draft().version().rounding(), 0, hash,
            calc, List.of("UNAPPROVED_DRAFT_ONLY", "NOT_A_CHARGE_AUTHORIZATION",
                "NOT_A_TAX_DETERMINATION", "NO_ORACLE_WRITE", "NO_ACCOUNTING_POSTING"));
    }
}
