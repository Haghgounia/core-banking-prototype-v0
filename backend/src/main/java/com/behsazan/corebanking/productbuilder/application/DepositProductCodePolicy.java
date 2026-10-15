package com.behsazan.corebanking.productbuilder.application;

import java.util.Locale;
import java.util.Map;

/** R30-HF5: bank-approved numeric deposit TYPE prefix, never a surrogate ID or product family. */
public final class DepositProductCodePolicy {
    private static final Map<String, String> TYPE_PREFIXES = Map.of(
            "CURRENT_NO_INTEREST", "2510",
            "SAVINGS", "3510",
            "SHORT_TERM_DEPOSIT", "4510",
            "LONG_TERM_DEPOSIT", "5510",
            "CERTIFICATE_OF_DEPOSIT", "6510");

    private DepositProductCodePolicy() { }

    /** Only the five explicitly approved deposit type business codes can receive a code. */
    public static String numericTypeCode(String rawDepositTypeCode) {
        String type = rawDepositTypeCode == null ? "" : rawDepositTypeCode.strip().toUpperCase(Locale.ROOT);
        String prefix = TYPE_PREFIXES.get(type);
        if (prefix == null)
            throw new ProductBuilderValidationException("برای خانواده این محصول، نگاشت گروه و نوع سپرده تعریف نشده است. اطلاعات پایه را اصلاح کنید." + " [DEPOSIT_TYPE_CODE=" + type + "]");
        return prefix;
    }

    /** Four-digit numeric type + four-digit Oracle working year + three-digit serial (001..999). */
    public static String format(String depositTypeCode, int workingYear, int serial) {
        String prefix = numericTypeCode(depositTypeCode);
        if (workingYear < 1300 || workingYear > 1600)
            throw new ProductBuilderValidationException("سال کاری صدور کد محصول سپرده معتبر نیست.");
        if (serial < 1 || serial > 999)
            throw new ProductBuilderValidationException("ظرفیت ۹۹۹ کد این خانواده سپرده در سال کاری تکمیل شده است.");
        return prefix + String.format(Locale.ROOT, "%04d%03d", workingYear, serial);
    }

    public static boolean isCorrespondent(String family) {
        return "NOSTRO_ACCOUNT".equals(family) || "VOSTRO_ACCOUNT".equals(family);
    }
}
