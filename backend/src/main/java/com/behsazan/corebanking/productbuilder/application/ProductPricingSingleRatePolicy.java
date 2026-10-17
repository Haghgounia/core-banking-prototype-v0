package com.behsazan.corebanking.productbuilder.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * HF7-A3/Final-C2 (HF9): read-only proof for SINGLE_APPLICABLE_RATE.
 *
 * Amount intervals are [MIN_AMOUNT, MAX_AMOUNT). A null amount endpoint means
 * unbounded in that direction, not zero. The selected tier rate applies to the
 * entire base amount; this is NOT progressive/marginal calculation.
 *
 * No duration boundary convention or conversion has been approved. Amount-only
 * tiers can be certified; term-dependent tiers are checked for known conflicts
 * but are NOT publishable until an explicit, comparable term universe and its
 * endpoint/null semantics are supplied. This class never posts money or writes.
 */
public final class ProductPricingSingleRatePolicy {
    private ProductPricingSingleRatePolicy() {}

    public record RateSelection(long tierNo, BigDecimal rate) {}

    private record AmountRange(BigDecimal min, BigDecimal max) {
        boolean includes(BigDecimal value) {
            return (min == null || value.compareTo(min) >= 0)
                    && (max == null || value.compareTo(max) < 0);
        }
        boolean overlaps(AmountRange other) {
            return (max == null || other.min == null || max.compareTo(other.min) > 0)
                    && (other.max == null || min == null || other.max.compareTo(min) > 0);
        }
        boolean within(AmountRange container) {
            return (container.min == null || min != null && min.compareTo(container.min) >= 0)
                    && (container.max == null || max != null && max.compareTo(container.max) <= 0);
        }
    }

    private static ProductRuleHierarchyAudit.Finding invalid(String reason) {
        return new ProductRuleHierarchyAudit.Finding("INVALID_SEMANTICS", reason);
    }
    private static ProductRuleHierarchyAudit.Finding conflict(String reason) {
        return new ProductRuleHierarchyAudit.Finding("CONFLICT", reason);
    }
    private static ProductRuleHierarchyAudit.Finding ok() {
        return new ProductRuleHierarchyAudit.Finding(null, null);
    }

    private static BigDecimal number(Map<String,Object> row, String key) {
        Object raw = row.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) return null;
        try { return new BigDecimal(String.valueOf(raw).trim()); }
        catch (NumberFormatException ex) { throw new ProductBuilderValidationException("مقدار عددی نامعتبر: " + key); }
    }

    private static AmountRange amountRange(Map<String,Object> row, String entity) {
        BigDecimal lo = number(row, "MIN_AMOUNT"), hi = number(row, "MAX_AMOUNT");
        if (lo != null && lo.signum() < 0 || hi != null && hi.signum() < 0)
            throw new ProductBuilderValidationException(entity + ": مبلغ منفی مجاز نیست.");
        if (lo != null && hi != null && hi.compareTo(lo) <= 0)
            throw new ProductBuilderValidationException(entity + ": بازه مبلغ [حداقل،حداکثر) باید عرض مثبت داشته باشد؛ سقف مساوی حداقل نیز نامعتبر است.");
        return new AmountRange(lo, hi);
    }

    /** Also used at the PDL generic write boundary; null endpoints are allowed. */
    public static void validateAmountRange(Map<String,Object> row, String entity) {
        amountRange(row, entity);
    }

    private static String durationUnit(Map<String,Object> tier) {
        Object raw = tier.get("TERM_UNIT_CODE");
        if (raw == null || String.valueOf(raw).isBlank()) return "";
        return String.valueOf(raw).trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static boolean hasDuration(Map<String,Object> tier) {
        return number(tier, "MIN_TERM_VALUE") != null || number(tier, "MAX_TERM_VALUE") != null;
    }

    private static String explicitUnit(String unit) {
        return switch (unit) {
            case "DAY", "D" -> "DAY";
            case "MONTH", "M" -> "MONTH";
            case "YEAR", "Y" -> "YEAR";
            default -> "";  // Numeric DPS codes are never guessed.
        };
    }

    private static boolean termOverlap(Map<String,Object> a, Map<String,Object> b) {
        boolean ad = hasDuration(a), bd = hasDuration(b);
        if (!ad || !bd) return true; // An amount-only tier applies to every duration.
        String au = explicitUnit(durationUnit(a)), bu = explicitUnit(durationUnit(b));
        if (au.isBlank() || bu.isBlank() || !au.equals(bu))
            throw new ProductBuilderValidationException("واحدهای مدت پله‌های همزمان قابل مقایسه نیستند؛ بدون تبدیل مصوب روز/ماه/سال انتشار مسدود است.");
        // Conservative conflict detection: endpoints of duration intervals have
        // no approved inclusion convention. Equality must NOT resolve a tie.
        return number(a,"MIN_TERM_VALUE").compareTo(number(b,"MAX_TERM_VALUE")) <= 0
                && number(b,"MIN_TERM_VALUE").compareTo(number(a,"MAX_TERM_VALUE")) <= 0;
    }

    /**
     * Prove amount-only tiers partition the component's declared amount scope
     * on EVERY date in the bounded rule scope. Component scope may be unbounded.
     */
    public static ProductRuleHierarchyAudit.Finding auditComponent(long componentId,
                                                                    Map<String,Object> component,
                                                                    List<Map<String,Object>> tiers,
                                                                    LocalDate from, LocalDate to) {
        try {
            if (tiers == null) return conflict("پله‌های جزء " + componentId + " قابل بازیابی کامل نیستند.");
            AmountRange componentRange = amountRange(component, "جزء " + componentId);
            List<AmountRange> ranges = new ArrayList<>();
            for (Map<String,Object> tier : tiers) {
                if (tier == null) return invalid("رکورد پله خالی برای جزء " + componentId + " دریافت شد.");
                AmountRange r = amountRange(tier, "پله جزء " + componentId);
                if (!r.within(componentRange)) return invalid("دامنه مبلغ پله خارج از دامنه مبلغ جزء " + componentId + " است.");
                // Do not replace absent duration endpoints by infinity/zero.
                boolean lower = number(tier,"MIN_TERM_VALUE") != null;
                boolean upper = number(tier,"MAX_TERM_VALUE") != null;
                if (lower != upper) return invalid("در پله جزء " + componentId + " یکی از حدود مدت NULL است؛ معنای آن تصویب نشده و انتشار متوقف است.");
                if (lower && explicitUnit(durationUnit(tier)).isBlank())
                    return invalid("واحد مدت پله جزء " + componentId + " مرجع قابل مقایسه صریح ندارد؛ کد عددی DPS بدون نگاشت مصوب تفسیر نمی‌شود.");
                ranges.add(r);
            }
            // Split time into intervals where active-tier membership is constant;
            // do not sample only the version start or check union of dates.
            TreeSet<LocalDate> slices = new TreeSet<>();
            slices.add(from);
            for (Map<String,Object> t : tiers) {
                LocalDate start = boundary(t,"VALID_FROM", LocalDate.MIN);
                LocalDate end = boundary(t,"VALID_TO", LocalDate.MAX);
                if (start.isAfter(from) && !start.isAfter(to)) slices.add(start);
                if (end.isBefore(to) && !end.equals(LocalDate.MAX) && !end.isBefore(from)) slices.add(end.plusDays(1));
            }
            for (LocalDate date : slices) {
                List<Integer> active = new ArrayList<>();
                for (int i=0;i<tiers.size();i++)
                    if (ProductRuleEvidenceEvaluator.isEffectiveAt("PRODUCT_RATE_TIER",tiers.get(i),date)) active.add(i);
                if (active.isEmpty()) return conflict("جزء " + componentId + " در تاریخ " + date + " هیچ پله مؤثری ندارد.");
                for (int a=0;a<active.size();a++) for (int b=a+1;b<active.size();b++) {
                    int ia=active.get(a), ib=active.get(b);
                    if (ranges.get(ia).overlaps(ranges.get(ib)) && termOverlap(tiers.get(ia),tiers.get(ib)))
                        return conflict("جزء " + componentId + " در تاریخ " + date
                                + " برای یک مبلغ و مدت بیش از یک پله همزمان دارد؛ انتخاب نرخ یکتا نیست.");
                }
                // The approved amount scope is the component's actual MIN/MAX,
                // not an invented interval from the first and last tier.
                if (active.stream().anyMatch(i -> hasDuration(tiers.get(i))))
                    return invalid("جزء " + componentId + " پله وابسته به مدت دارد؛ دامنه مجاز مدت و معنای شمول مرزهای آن در قرارداد موجود تصریح نشده است. بدون فرض جدید تصویب نمی‌شود.");
                List<AmountRange> sorted = new ArrayList<>();
                for (int i : active) sorted.add(ranges.get(i));
                sorted.sort(Comparator.comparing(AmountRange::min, Comparator.nullsFirst(Comparator.naturalOrder())));
                BigDecimal cursor = componentRange.min;
                boolean unlimited = false;
                for (AmountRange r : sorted) {
                    if (unlimited) return conflict("جزء " + componentId + " دارای پله مبلغی اضافی پس از دامنه بدون سقف است.");
                    if (cursor == null ? r.min != null : r.min != null && r.min.compareTo(cursor) > 0)
                        return conflict("جزء " + componentId + " در تاریخ " + date + " دارای فاصله بدون نرخ در دامنه مبلغ است.");
                    // Overlaps were checked above, including null lower/upper.
                    cursor = r.max;
                    unlimited = cursor == null;
                }
                if (!unlimited && (componentRange.max == null || cursor.compareTo(componentRange.max) < 0))
                    return conflict("جزء " + componentId + " در تاریخ " + date + " انتهای دامنه مبلغ را پوشش نمی‌دهد.");
            }
            return ok();
        } catch (ProductBuilderValidationException | java.time.format.DateTimeParseException ex) {
            return invalid(ex.getMessage());
        }
    }

    /** Design-only selection proof; NEVER a runtime posting/pricing adapter. */
    static RateSelection previewAmountOnlyRate(List<Map<String,Object>> tiers,
                                                       BigDecimal amount, LocalDate date) {
        if (tiers == null || amount == null || date == null)
            throw new ProductBuilderValidationException("مبلغ، تاریخ و مجموعه پله‌ها برای بازبینی نرخ الزامی هستند.");
        RateSelection selected = null;
        for (Map<String,Object> tier : tiers) {
            if (!ProductRuleEvidenceEvaluator.isEffectiveAt("PRODUCT_RATE_TIER",tier,date)) continue;
            ProductRuleSemanticContract.validate("PRODUCT_RATE_TIER",tier);
            if (hasDuration(tier) || !durationUnit(tier).isBlank())
                throw new ProductBuilderValidationException("انتخاب آزمایشی بدون سیاست مدت برای پله مدت‌دار مجاز نیست.");
            if (amountRange(tier,"پله نرخ").includes(amount)) {
                if (selected != null) throw new ProductBuilderValidationException("دو پله برای یک مبلغ و تاریخ انتخاب می‌شوند؛ انتخاب نرخ یکتا نیست.");
                BigDecimal n = number(tier,"TIER_NO"), rate = number(tier,"RATE_VALUE");
                if (n == null || rate == null) throw new ProductBuilderValidationException("شناسه یا نرخ پله معتبر نیست.");
                try { selected = new RateSelection(n.longValueExact(),rate); }
                catch (ArithmeticException ex) { throw new ProductBuilderValidationException("شماره پله باید عدد صحیح باشد."); }
            }
        }
        if (selected == null) throw new ProductBuilderValidationException("برای مبلغ و تاریخ مورد نظر هیچ پله مجازی وجود ندارد.");
        return selected;
    }

    private static LocalDate boundary(Map<String,Object> row,String key,LocalDate fallback) {
        Object raw = row.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) return fallback;
        String s=String.valueOf(raw).trim();
        return LocalDate.parse(s.length()>10?s.substring(0,10):s);
    }
}
