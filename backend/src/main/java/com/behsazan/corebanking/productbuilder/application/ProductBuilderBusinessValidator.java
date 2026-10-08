package com.behsazan.corebanking.productbuilder.application;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Cross-field validation that cannot be expressed by generic Oracle metadata alone.
 * Rules mirror the Unified Product Builder business form contract.
 */
@Component
public class ProductBuilderBusinessValidator {
    private static final Pattern HH_MM = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    public void validate(String table, Map<String, Object> values) {
        if (table == null || values == null) return;
        switch (table.trim().toUpperCase()) {
            case "DEPOSIT_PROFIT_PAYMENT_RULE" -> validateProfitPayment(values);
            case "DEPOSIT_PRODUCT_JOINT_RULE" -> validateJointOwnerCounts(values);
            case "PRODUCT_PRICING_RULE" -> validatePricingRule(values);
            case "PRODUCT_RELATIONSHIP" -> validateRelationship(values);
            case "CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE" -> validateCorrespondentProfile(values);
            case "CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE" -> validateCorrespondentSettlement(values);
            default -> validateGenericDateRange(values);
        }
    }

    private void validateJointOwnerCounts(Map<String, Object> values) {
        Integer minimumOwners = integer(values, "MIN_JOINT_OWNER_COUNT");
        Integer maximumOwners = integer(values, "MAX_JOINT_OWNER_COUNT");
        Integer requiredSigners = integer(values, "MIN_REQUIRED_SIGNER_COUNT");
        if (minimumOwners == null || minimumOwners < 1) {
            fail("حداقل تعداد صاحبان حساب باید عدد صحیح مثبت باشد.");
        }
        if (maximumOwners == null || maximumOwners < minimumOwners) {
            fail("حداکثر تعداد صاحبان حساب نباید از حداقل تعداد کمتر باشد.");
        }
        if (requiredSigners == null || requiredSigners < 1 || requiredSigners > maximumOwners) {
            fail("حداقل تعداد امضاکنندگان باید بین ۱ و حداکثر تعداد صاحبان حساب باشد.");
        }
        validateGenericDateRange(values);
    }

    private void validatePricingRule(Map<String, Object> values) {
        String purpose = text(values, "PRICING_PURPOSE_CODE");
        if (purpose.isBlank()) fail("هدف قیمت‌گذاری باید از فهرست انتخاب شود.");
        String method = text(values, "PRICING_METHOD_CODE");
        if (method.isBlank()) fail("روش محاسبه قیمت‌گذاری باید انتخاب شود.");
        BigDecimal base = decimal(values, "BASE_RATE");
        BigDecimal early = decimal(values, "EARLY_TERMINATION_RATE");
        if (base != null && (base.signum() < 0 || base.compareTo(BigDecimal.ONE) > 0))
            fail("نرخ پایه به صورت اعشاری بین صفر و یک ثبت می‌شود؛ مثلاً ۰٫۱۸ معادل ۱۸ درصد است.");
        if (!bool(values, "EARLY_TERMINATION_ALLOWED") && early != null)
            fail("هنگامی که خاتمه پیش از موعد مجاز نیست، نرخ خاتمه زودهنگام باید خالی باشد.");
        if (early != null && (early.signum() < 0 || early.compareTo(BigDecimal.ONE) > 0))
            fail("نرخ خاتمه زودهنگام باید بین صفر و یک باشد؛ مثلاً ۰٫۱۰ معادل ۱۰ درصد.");
        Integer maxDest = integer(values, "MAX_DESTINATION_COUNT");
        if (maxDest != null && maxDest < 1) fail("حداکثر تعداد مقصد پرداخت باید عدد صحیح مثبت باشد.");
        if ("SINGLE_ACCOUNT".equals(text(values, "DESTINATION_RULE_CODE")) && maxDest != null && maxDest > 1)
            fail("در قاعده «یک حساب مقصد»، حداکثر تعداد مقصد نمی‌تواند بیش از یک باشد.");
        validateGenericDateRange(values);
    }

    private void validateRelationship(Map<String, Object> values) {
        Integer priority = integer(values, "PRIORITY_NO");
        if (priority == null || priority < 1) fail("اولویت رابطه باید یک عدد صحیح مثبت باشد (۱ یعنی اولویت بالاتر).");
        BigDecimal min = decimal(values, "MIN_RELATION_AMOUNT");
        BigDecimal max = decimal(values, "MAX_RELATION_AMOUNT");
        if ((min != null && min.signum() < 0) || (max != null && max.signum() < 0))
            fail("مبالغ رابطه بین محصولات نمی‌توانند منفی باشند.");
        if (min != null && max != null && max.compareTo(min) < 0)
            fail("حداکثر مبلغ رابطه نباید کمتر از حداقل مبلغ باشد.");
        Integer targetProduct = integer(values, "TARGET_PRODUCT_ID");
        if (targetProduct == null || targetProduct <= 0) fail("انتخاب محصول مقصد الزامی است.");
        validateGenericDateRange(values);
    }

    private void validateProfitPayment(Map<String, Object> values) {
        String frequency = text(values, "PAYMENT_FREQUENCY_CODE");
        String dayRule = text(values, "PAYMENT_DAY_RULE_CODE");
        Integer dayNo = integer(values, "PAYMENT_DAY_NO");
        boolean capitalizationAllowed = bool(values, "CAPITALIZATION_ALLOWED");
        boolean capitalizationDefault = bool(values, "CAPITALIZATION_DEFAULT");

        if ("MATURITY".equals(frequency) && !"MATURITY_DATE".equals(dayRule)) {
            fail("برای واریز سود در سررسید، قاعده تعیین روز پرداخت باید MATURITY_DATE باشد.");
        }
        if ("FIXED_DAY_OF_MONTH".equals(dayRule)) {
            if (dayNo == null || dayNo < 1 || dayNo > 31) {
                fail("برای «روز ثابت ماه»، مقدار روز باید بین 1 تا 31 تعیین شود.");
            }
        } else if (dayNo != null) {
            fail("PAYMENT_DAY_NO فقط وقتی قابل ثبت است که PAYMENT_DAY_RULE_CODE برابر FIXED_DAY_OF_MONTH باشد.");
        }
        if (capitalizationDefault && !capitalizationAllowed) {
            fail("سرمایه‌گذاری مجدد پیش‌فرض فقط وقتی مجاز است که افزودن سود به اصل مجاز باشد.");
        }
        validateGenericDateRange(values);
    }

    private void validateCorrespondentProfile(Map<String, Object> values) {
        BigDecimal intraday = decimal(values, "INTRADAY_LIMIT");
        BigDecimal credit = decimal(values, "CREDIT_LIMIT");
        if (intraday != null && intraday.signum() < 0) fail("حد درون‌روزی نمی‌تواند منفی باشد.");
        if (credit != null && credit.signum() < 0) fail("حد اعتباری نمی‌تواند منفی باشد.");
    }

    private void validateCorrespondentSettlement(Map<String, Object> values) {
        String time = text(values, "CUTOFF_TIME");
        if (!time.isBlank() && !HH_MM.matcher(time).matches()) {
            fail("CUTOFF_TIME باید با قالب 24 ساعته HH:mm مانند 16:00 ثبت شود.");
        }
        validateGenericDateRange(values);
    }

    private void validateGenericDateRange(Map<String, Object> values) {
        LocalDate from = date(values, "VALID_FROM");
        LocalDate to = date(values, "VALID_TO");
        if (from != null && to != null && to.isBefore(from)) {
            fail("تاریخ پایان اعتبار نمی‌تواند قبل از تاریخ شروع اعتبار باشد.");
        }
        LocalDate effectiveFrom = date(values, "EFFECTIVE_FROM_DATE");
        LocalDate effectiveTo = date(values, "EFFECTIVE_TO_DATE");
        if (effectiveFrom != null && effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            fail("تاریخ پایان اعتبار نمی‌تواند قبل از تاریخ شروع اعتبار باشد.");
        }
    }

    private static String text(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : value.toString().trim().toUpperCase();
    }

    private static boolean bool(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        String text = value.toString().trim();
        return "1".equals(text) || "TRUE".equalsIgnoreCase(text) || "Y".equalsIgnoreCase(text);
    }

    private static Integer integer(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null || value.toString().isBlank()) return null;
        try { return new BigDecimal(value.toString()).intValueExact(); }
        catch (ArithmeticException | NumberFormatException ex) {
            fail(key + " باید یک عدد صحیح باشد.");
            return null;
        }
    }

    private static BigDecimal decimal(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null || value.toString().isBlank()) return null;
        try { return new BigDecimal(value.toString()); }
        catch (NumberFormatException ex) {
            fail(key + " باید مقدار عددی معتبر باشد.");
            return null;
        }
    }

    private static LocalDate date(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null || value.toString().isBlank()) return null;
        try { return LocalDate.parse(value.toString().substring(0, 10)); }
        catch (DateTimeParseException | IndexOutOfBoundsException ex) {
            fail(key + " باید تاریخ معتبر ISO/Gregorian در قرارداد Backend باشد.");
            return null;
        }
    }

    private static void fail(String message) {
        throw new ProductBuilderValidationException(message);
    }
}
