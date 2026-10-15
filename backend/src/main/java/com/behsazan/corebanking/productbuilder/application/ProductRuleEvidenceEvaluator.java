package com.behsazan.corebanking.productbuilder.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PB-R19: evaluate ACTIVE rule evidence for the version's effective-from date
 * and detect contradictory/overlapping business keys. Never treats historical
 * inactive rows as executable rules. Does not resolve rule inheritance.
 */
public final class ProductRuleEvidenceEvaluator {
    private ProductRuleEvidenceEvaluator() {}

    public record Evidence(long effectiveCount, boolean conflict, String conflictReason,
                           long historicalCount, int unrecognizedStatuses) {}

    private static final Set<String> USABLE_STATUSES = Set.of(
            "ACTIVE", "APPROVED", "ENABLED", "VALID", "CURRENT", "PUBLISHED");
    private static final Set<String> INACTIVE_STATUSES = Set.of(
            "INACTIVE", "DISABLED", "SUSPENDED", "CANCELLED", "CANCELED", "DELETED", "DRAFT", "REJECTED", "RETIRED", "EXPIRED");

    // DPS.REF_RULE_STATUS_CODE (user-confirmed current business codes, 2026-10-12):
    // 1=Draft, 2=Active, 3=Inactive, 4=Suspended, 5=Retired.
    // Normalize ONLY fields bound to this reference. Unknown codes must still fail closed.
    private static final Map<String, String> DPS_RULE_STATUSES = Map.of(
            "1", "DRAFT", "2", "ACTIVE", "3", "INACTIVE", "4", "SUSPENDED", "5", "RETIRED");

    public static Evidence evaluate(String control, List<Map<String, Object>> rows,
                                    LocalDate versionStart, LocalDate versionEnd) {
        List<Map<String, Object>> valid = new ArrayList<>();
        List<Map<String, Object>> withinVersion = new ArrayList<>();
        int unknown = 0;
        for (Map<String, Object> row : rows) {
            String status = status(control, row);
            if (!status.isBlank() && !USABLE_STATUSES.contains(status)) {
                if (!INACTIVE_STATUSES.contains(status)) unknown++;
                continue;
            }
            if (containsDate(row, versionStart)) valid.add(row);
            if (!from(row).isAfter(versionEnd) && !to(row).isBefore(versionStart)) withinVersion.add(row);
        }
        String conflictReason = conflictReason(control, withinVersion);
        return new Evidence(valid.size(), conflictReason != null || unknown > 0,
                unknown > 0 ? "وضعیت برخی قواعد در قرارداد معتبر شناخته نشده است؛ تا تعیین نگاشت رسمی منتشر نشود."
                        : conflictReason, rows.size(), unknown);
    }

    /** Same lifecycle/date logic as evaluate; used by approval semantic audit. */
    public static boolean isEffectiveAt(String control, Map<String, Object> row, LocalDate when) {
        return isActive(control, row) && containsDate(row, when);
    }

    /**
     * A scheduled rule that starts after the product-version start is still
     * subject to semantic audit. A start-date-only check can certify an invalid
     * future rule and is therefore unsafe for design approval.
     */
    public static boolean isApplicableWithin(String control, Map<String, Object> row,
                                             LocalDate versionStart, LocalDate versionEnd) {
        return isActive(control, row) && !from(row).isAfter(versionEnd)
                && !to(row).isBefore(versionStart);
    }

    /** Invalid ACTIVE schedules never become silently ignored historical rows. */
    public static boolean hasInvalidActivePeriod(String control, Map<String, Object> row) {
        return isActive(control, row) && to(row).isBefore(from(row));
    }

    /**
     * Coverage here is only a *temporal union* of a table's ACTIVE rules.
     * It deliberately makes no claim about channel, customer, currency,
     * amount, precedence, or bank policy; those require a governed resolver.
     * Both ends are inclusive. A finite last day followed by the next day
     * is contiguous and LocalDate.MAX must not overflow on plusDays(1).
     */
    public record TemporalCoverage(boolean complete, LocalDate firstGap) {}

    public static TemporalCoverage temporalCoverage(String control, List<Map<String, Object>> rows,
                                                     LocalDate versionStart, LocalDate versionEnd) {
        if (versionEnd.isBefore(versionStart)) return new TemporalCoverage(false, versionStart);
        record Range(LocalDate start, LocalDate end) {}
        List<Range> intervals = new ArrayList<>();
        for (var row : rows) {
            if (!isActive(control, row)) continue;
            LocalDate start = from(row), end = to(row);
            if (end.isBefore(start) || start.isAfter(versionEnd) || end.isBefore(versionStart)) continue;
            intervals.add(new Range(start.isBefore(versionStart) ? versionStart : start,
                    end.isAfter(versionEnd) ? versionEnd : end));
        }
        intervals.sort(java.util.Comparator.comparing(Range::start));
        LocalDate cursor = versionStart;
        for (Range range : intervals) {
            if (range.end().isBefore(cursor)) continue;
            if (range.start().isAfter(cursor)) return new TemporalCoverage(false, cursor);
            if (!range.end().isBefore(versionEnd)) return new TemporalCoverage(true, null);
            cursor = range.end().plusDays(1);
        }
        return new TemporalCoverage(false, cursor);
    }

    /** Prevent simultaneous active single-row policies anywhere in the version interval. */
    public static boolean hasOverlappingActiveRows(String control, List<Map<String, Object>> rows,
                                                   LocalDate versionStart, LocalDate versionEnd) {
        for (int a = 0; a < rows.size(); a++) {
            if (!isApplicableWithin(control, rows.get(a), versionStart, versionEnd)) continue;
            for (int b = a + 1; b < rows.size(); b++) {
                if (isApplicableWithin(control, rows.get(b), versionStart, versionEnd)
                        && overlap(rows.get(a), rows.get(b))) return true;
            }
        }
        return false;
    }

    private static boolean isActive(String control, Map<String, Object> row) {
        String value = status(control, row);
        return value.isBlank() || USABLE_STATUSES.contains(value);
    }

    private static String conflictReason(String control, List<Map<String, Object>> rows) {
        List<String> keys = switch (control) {
            case "PRODUCT_CHANNEL_RULE" -> List.of("CHANNEL_CODE");
            case "PRODUCT_REQUIRED_DOCUMENT" -> List.of("DOCUMENT_TYPE_ID", "REQUIREMENT_STAGE_CODE");
            case "PRODUCT_REQUIRED_INQUIRY" -> List.of("INQUIRY_TYPE_CODE", "REQUIREMENT_STAGE_CODE");
            case "DEPOSIT_PRODUCT_TRANSACTION_RULE" -> List.of("TRANSACTION_TYPE_CODE");
            case "DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA" -> List.of("WITHDRAWAL_MEDIA_CODE", "CHANNEL_CODE");
            case "DEPOSIT_PRODUCT_HOLD_RULE" -> List.of("HOLD_TYPE_CODE");
            case "DEPOSIT_PRODUCT_JOINT_RULE" -> List.of("OWNERSHIP_TYPE_CODE");
            case "LOAN_PRODUCT_COLLATERAL_RULE" -> List.of("LOAN_PRODUCT_COLLATERAL_ID");
            case "LOAN_PRODUCT_REPAYMENT_RULE" -> List.of("REPAYMENT_RULE_CODE");
            case "PRODUCT_PRICING_RULE" -> List.of("PRICING_PURPOSE_CODE", "CURRENCY_CODE");
            case "PRODUCT_RATE_TIER" -> List.of("PRICING_COMPONENT_ID", "TIER_NO");
            case "PRODUCT_ORG_SCOPE" -> List.of("ORG_UNIT_TYPE_CODE", "ORG_UNIT_ID");
            case "DEPOSIT_PRODUCT_CLOSURE_RULE" -> List.of("CLOSURE_TYPE_CODE");
            default -> List.of();
        };
        if (keys.isEmpty()) return null;
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String key = keys.stream().map(name -> text(row.get(name))).reduce((a, b) -> a + "::" + b).orElse("");
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }
        for (var entry : grouped.entrySet()) {
            List<Map<String, Object>> group = entry.getValue();
            for (int a = 0; a < group.size(); a++) {
                for (int b = a + 1; b < group.size(); b++) {
                    if (overlap(group.get(a), group.get(b))) {
                        return "چند Rule مؤثر با کلید کسب‌وکاری یکسان و بازه اعتبار متداخل یافت شد: " + control;
                    }
                }
            }
        }
        return null;
    }

    static boolean overlap(Map<String, Object> left, Map<String, Object> right) {
        return !from(left).isAfter(to(right)) && !from(right).isAfter(to(left));
    }

    private static boolean containsDate(Map<String, Object> row, LocalDate when) {
        return !when.isBefore(from(row)) && !when.isAfter(to(row));
    }

    private static LocalDate from(Map<String, Object> row) {
        return date(row, "VALID_FROM", "EFFECTIVE_FROM_DATE", LocalDate.MIN);
    }

    private static LocalDate to(Map<String, Object> row) {
        return date(row, "VALID_TO", "EFFECTIVE_TO_DATE", LocalDate.MAX);
    }

    private static LocalDate date(Map<String, Object> row, String a, String b, LocalDate fallback) {
        Object value = row.get(a) == null ? row.get(b) : row.get(a);
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        String s = String.valueOf(value).trim();
        if (s.length() >= 10) s = s.substring(0, 10);
        try { return LocalDate.parse(s); }
        catch (RuntimeException e) { throw new ProductBuilderValidationException("تاریخ اعتبار قاعده معتبر نیست: " + s); }
    }

    private static String status(String control, Map<String, Object> row) {
        // IS_ACTIVE is a record lifecycle flag and keeps its existing precedence.
        // IS_ALLOWED is a business permission, not a record lifecycle flag.
        if (row.containsKey("IS_ACTIVE") && row.get("IS_ACTIVE") != null) {
            Object flag = row.get("IS_ACTIVE");
            if (flag instanceof Boolean bool) return bool ? "ACTIVE" : "INACTIVE";
            try {
                java.math.BigDecimal value = new java.math.BigDecimal(String.valueOf(flag).trim());
                if (value.compareTo(java.math.BigDecimal.ONE) == 0) return "ACTIVE";
                if (value.compareTo(java.math.BigDecimal.ZERO) == 0) return "INACTIVE";
            } catch (NumberFormatException ex) {
                if ("true".equalsIgnoreCase(String.valueOf(flag))) return "ACTIVE";
                if ("false".equalsIgnoreCase(String.valueOf(flag))) return "INACTIVE";
            }
            // Unknown IS_ACTIVE must never masquerade as safe inactive history.
            return "INVALID_ACTIVE_FLAG";
        }
        if (row.get("RULE_STATUS_CODE") != null) {
            return dpsRuleStatus(text(row.get("RULE_STATUS_CODE")));
        }
        // Only the deposit closure rule's STATUS_CODE is backed by the same DPS
        // reference. STATUS_CODE in other tables is NOT necessarily a rule status.
        if ("DEPOSIT_PRODUCT_CLOSURE_RULE".equals(control) && row.get("STATUS_CODE") != null) {
            return dpsRuleStatus(text(row.get("STATUS_CODE")));
        }
        for (String column : List.of("STATUS_CODE", "RECORD_STATUS_CODE")) {
            if (row.get(column) != null) return text(row.get(column));
        }
        return "";
    }

    private static String dpsRuleStatus(String value) {
        return DPS_RULE_STATUSES.getOrDefault(value, value);
    }

    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim().toUpperCase(); }
}
