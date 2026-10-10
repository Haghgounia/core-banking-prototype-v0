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

    public static Evidence evaluate(String control, List<Map<String, Object>> rows,
                                    LocalDate versionStart, LocalDate versionEnd) {
        List<Map<String, Object>> valid = new ArrayList<>();
        List<Map<String, Object>> withinVersion = new ArrayList<>();
        int unknown = 0;
        for (Map<String, Object> row : rows) {
            String status = status(row);
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

    private static String conflictReason(String control, List<Map<String, Object>> rows) {
        List<String> keys = switch (control) {
            case "PRODUCT_CHANNEL_RULE" -> List.of("CHANNEL_CODE");
            case "DEPOSIT_PRODUCT_TRANSACTION_RULE" -> List.of("TRANSACTION_TYPE_CODE");
            case "PRODUCT_PRICING_RULE" -> List.of("PRICING_PURPOSE_CODE", "CURRENCY_CODE");
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
                    if ("DEPOSIT_PRODUCT_TRANSACTION_RULE".equals(control)
                            || "PRODUCT_CHANNEL_RULE".equals(control)
                            || overlap(group.get(a), group.get(b))) {
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

    private static String status(Map<String, Object> row) {
        // IS_ALLOWED is a business permission, not a record lifecycle flag.
        if (row.containsKey("IS_ACTIVE") && row.get("IS_ACTIVE") != null) {
            Object flag = row.get("IS_ACTIVE");
            return ("1".equals(String.valueOf(flag)) || "true".equalsIgnoreCase(String.valueOf(flag)))
                    ? "ACTIVE" : "INACTIVE";
        }
        for (String column : List.of("RULE_STATUS_CODE", "STATUS_CODE", "RECORD_STATUS_CODE")) {
            if (row.containsKey(column) && row.get(column) != null) return text(row.get(column));
        }
        return "";
    }

    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim().toUpperCase(); }
}
