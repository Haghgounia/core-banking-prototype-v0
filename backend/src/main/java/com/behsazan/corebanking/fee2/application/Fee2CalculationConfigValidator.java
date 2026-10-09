package com.behsazan.corebanking.fee2.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Business validation for the FEE2 calculation editor.
 *
 * <p>This implementation belongs to Core Banking Prototype and validates the
 * persisted FEE2 CALC_CONFIG_JSON contract. It intentionally does not execute
 * formulas or import runtime code from any external/reference project.</p>
 */
public final class Fee2CalculationConfigValidator {
    private static final Set<String> TYPES = Set.of(
            "FIXED", "PERCENTAGE", "FIXED_PLUS_PERCENTAGE", "PROGRESSIVE", "BRACKET",
            "MATRIX", "USAGE", "DAILY", "FORMULA"
    );
    private static final Set<String> ROUNDING_MODES = Set.of("FLOOR", "HALF_EVEN", "HALF_UP", "CEILING");
    private static final Set<String> DAY_BASES = Set.of("ACT_365", "ACT_360");
    private static final Set<String> FORMULA_FUNCTIONS = Set.of("min", "max", "abs", "if");
    private static final Pattern FUNCTION_CALL = Pattern.compile("\\b([A-Za-z_][A-Za-z0-9_]*)\\s*\\(");
    private static final Pattern FORMULA_CHARS = Pattern.compile("[A-Za-z0-9_+\\-*/().,<>=!&| \\t\\r\\n]*");
    private static final Pattern FORBIDDEN_FORMULA_WORDS = Pattern.compile(
            "(?i)\\b(eval|new|class|function|while|for|fetch|window|document|import|require|java|system|runtime)\\b"
    );

    public void validate(
            String calculationType,
            BigDecimal fixedAmount,
            BigDecimal rateValue,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String roundingMode,
            BigDecimal roundingQuantum,
            String dayBasis,
            Map<String, Object> operatorConfig
    ) {
        List<String> errors = new ArrayList<>();
        String type = upper(calculationType);
        Map<String, Object> config = operatorConfig == null ? Map.of() : operatorConfig;

        if (!TYPES.contains(type)) errors.add("روش محاسبه معتبر نیست.");
        if (negative(fixedAmount)) errors.add("مبلغ ثابت نمی‌تواند منفی باشد.");
        if (negative(rateValue)) errors.add("نرخ نمی‌تواند منفی باشد.");
        if (negative(minAmount)) errors.add("حداقل مبلغ نمی‌تواند منفی باشد.");
        if (negative(maxAmount)) errors.add("حداکثر مبلغ نمی‌تواند منفی باشد.");
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            errors.add("حداقل مبلغ نمی‌تواند از حداکثر مبلغ بیشتر باشد.");
        }
        if (!blank(roundingMode) && !ROUNDING_MODES.contains(upper(roundingMode))) {
            errors.add("روش گرد کردن معتبر نیست.");
        }
        if (roundingQuantum != null && roundingQuantum.signum() <= 0) {
            errors.add("گام گرد کردن باید بزرگ‌تر از صفر باشد.");
        }

        switch (type) {
            case "FIXED" -> require(fixedAmount, "برای روش مبلغ ثابت، مبلغ ثابت الزامی است.", errors);
            case "PERCENTAGE" -> require(rateValue, "برای روش درصدی، نرخ الزامی است.", errors);
            case "FIXED_PLUS_PERCENTAGE" -> {
                require(fixedAmount, "برای روش ثابت + درصد، مبلغ ثابت الزامی است.", errors);
                require(rateValue, "برای روش ثابت + درصد، نرخ الزامی است.", errors);
            }
            case "PROGRESSIVE" -> validateRanges(config.get("tiers"), "پله", true, errors);
            case "BRACKET" -> validateRanges(config.get("brackets"), "بازه", false, errors);
            case "MATRIX" -> validateMatrix(config.get("cells"), errors);
            case "USAGE" -> validateUsage(config, rateValue, errors);
            case "DAILY" -> {
                require(rateValue, "برای روش روزشمار، نرخ سالانه الزامی است.", errors);
                if (!DAY_BASES.contains(upper(dayBasis))) errors.add("مبنای روزشماری باید ACT_365 یا ACT_360 باشد.");
            }
            case "FORMULA" -> validateFormula(config.get("expression"), errors);
            default -> { /* invalid type is already reported above */ }
        }

        if (!errors.isEmpty()) {
            throw new Fee2ValidationException("تنظیمات روش محاسبه معتبر نیست: " + String.join(" ", new LinkedHashSet<>(errors)));
        }
    }

    private static void validateRanges(Object raw, String title, boolean progressive, List<String> errors) {
        if (!(raw instanceof List<?> rows) || rows.isEmpty()) {
            errors.add("حداقل یک " + title + " محاسباتی لازم است.");
            return;
        }
        if (progressive && rows.size() > 100) errors.add("تعداد پله‌های تدریجی نمی‌تواند بیشتر از 100 باشد.");

        BigDecimal expectedFrom = BigDecimal.ZERO;
        boolean openEnded = false;
        for (int i = 0; i < rows.size(); i++) {
            Object rawRow = rows.get(i);
            int displayIndex = i + 1;
            if (!(rawRow instanceof Map<?, ?> row)) {
                errors.add(title + " شماره " + displayIndex + " ساختار معتبر ندارد.");
                continue;
            }
            BigDecimal from = decimal(row.get("from"));
            if (from == null) from = BigDecimal.ZERO;
            BigDecimal to = decimal(row.get("to"));
            BigDecimal rate = decimal(row.get("rate"));
            BigDecimal fixed = decimal(row.get("fixedAmount"));

            if (from.signum() < 0) errors.add("مقدار شروع " + title + " شماره " + displayIndex + " نمی‌تواند منفی باشد.");
            if (expectedFrom != null && from.compareTo(expectedFrom) != 0) {
                errors.add(title + " شماره " + displayIndex + " با " + title + " قبل پیوستگی ندارد.");
            }
            if (to != null && to.compareTo(from) <= 0) {
                errors.add("حد پایان " + title + " شماره " + displayIndex + " باید از حد شروع بزرگ‌تر باشد.");
            }
            if (negative(rate)) errors.add("نرخ " + title + " شماره " + displayIndex + " نمی‌تواند منفی باشد.");
            if (negative(fixed)) errors.add("مبلغ ثابت " + title + " شماره " + displayIndex + " نمی‌تواند منفی باشد.");
            if (rate == null && fixed == null) errors.add("برای " + title + " شماره " + displayIndex + " نرخ یا مبلغ ثابت لازم است.");
            if (openEnded) errors.add("پس از " + title + " بدون سقف، ردیف دیگری مجاز نیست.");

            if (to == null) {
                openEnded = true;
                expectedFrom = null;
            } else {
                expectedFrom = to;
            }
        }
        if (!openEnded) errors.add("آخرین " + title + " باید بدون سقف باشد.");
    }

    private static void validateMatrix(Object raw, List<String> errors) {
        if (!(raw instanceof List<?> rows) || rows.isEmpty()) {
            errors.add("حداقل یک سلول ماتریس لازم است.");
            return;
        }
        if (rows.size() > 500) errors.add("تعداد سلول‌های ماتریس نمی‌تواند بیشتر از 500 باشد.");

        Set<String> combinations = new LinkedHashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            Object rawRow = rows.get(i);
            int displayIndex = i + 1;
            if (!(rawRow instanceof Map<?, ?> row)) {
                errors.add("سلول ماتریس شماره " + displayIndex + " ساختار معتبر ندارد.");
                continue;
            }
            Object dimensionsRaw = row.get("dimensions");
            if (!(dimensionsRaw instanceof Map<?, ?> dimensions) || dimensions.isEmpty()) {
                errors.add("سلول ماتریس شماره " + displayIndex + " باید حداقل یک بُعد داشته باشد.");
                continue;
            }
            TreeMap<String, String> canonical = new TreeMap<>();
            dimensions.forEach((key, value) -> canonical.put(String.valueOf(key).trim().toUpperCase(Locale.ROOT), String.valueOf(value).trim()));
            if (canonical.values().stream().anyMatch(String::isBlank)) {
                errors.add("همه مقادیر ابعاد سلول ماتریس شماره " + displayIndex + " باید تکمیل شوند.");
            }
            if (!combinations.add(canonical.toString())) {
                errors.add("ترکیب ابعاد سلول ماتریس شماره " + displayIndex + " تکراری است.");
            }

            BigDecimal amount = decimal(row.get("amount"));
            BigDecimal rate = decimal(row.get("rate"));
            if ((amount == null) == (rate == null)) {
                errors.add("سلول ماتریس شماره " + displayIndex + " باید دقیقاً یکی از مبلغ یا نرخ را داشته باشد.");
            }
            if (negative(amount)) errors.add("مبلغ سلول ماتریس شماره " + displayIndex + " نمی‌تواند منفی باشد.");
            if (negative(rate)) errors.add("نرخ سلول ماتریس شماره " + displayIndex + " نمی‌تواند منفی باشد.");
        }
    }

    private static void validateUsage(Map<String, Object> config, BigDecimal fallbackRate, List<String> errors) {
        BigDecimal unitPrice = decimal(config.get("unitPrice"));
        if (unitPrice == null) unitPrice = fallbackRate;
        if (unitPrice == null) errors.add("برای روش مبتنی بر مصرف، قیمت واحد الزامی است.");
        if (negative(unitPrice)) errors.add("قیمت واحد نمی‌تواند منفی باشد.");
        BigDecimal allowance = decimal(config.get("allowance"));
        if (negative(allowance)) errors.add("سهمیه معاف نمی‌تواند منفی باشد.");
    }

    private static void validateFormula(Object raw, List<String> errors) {
        String expression = raw == null ? "" : String.valueOf(raw).trim();
        if (expression.isEmpty()) {
            errors.add("فرمول الزامی است.");
            return;
        }
        if (expression.length() > 2048) errors.add("طول فرمول بیش از حد مجاز است.");
        if (!FORMULA_CHARS.matcher(expression).matches()) errors.add("فرمول دارای کاراکتر غیرمجاز است.");
        if (FORBIDDEN_FORMULA_WORDS.matcher(expression).find()) errors.add("فرمول شامل واژه یا قابلیت غیرمجاز است.");

        int depth = 0;
        int maxDepth = 0;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '(') {
                depth++;
                maxDepth = Math.max(maxDepth, depth);
            } else if (ch == ')') {
                depth--;
                if (depth < 0) {
                    errors.add("پرانتزهای فرمول متوازن نیستند.");
                    break;
                }
            }
        }
        if (depth != 0) errors.add("پرانتزهای فرمول متوازن نیستند.");
        if (maxDepth > 16) errors.add("عمق تو در توی فرمول بیش از حد مجاز است.");

        Matcher functions = FUNCTION_CALL.matcher(expression);
        while (functions.find()) {
            String functionName = functions.group(1);
            if (!FORMULA_FUNCTIONS.contains(functionName)) errors.add("تابع غیرمجاز در فرمول: " + functionName);
        }
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim().replace(",", "");
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) return null;
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static void require(Object value, String message, List<String> errors) {
        if (value == null) errors.add(message);
    }

    private static boolean negative(BigDecimal value) { return value != null && value.signum() < 0; }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String upper(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
}
