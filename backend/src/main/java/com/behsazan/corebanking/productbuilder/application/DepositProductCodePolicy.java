package com.behsazan.corebanking.productbuilder.application;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** R30: product family is the canonical code prefix for deposit products. */
public final class DepositProductCodePolicy {
    private static final Pattern FAMILY = Pattern.compile("[A-Z0-9][A-Z0-9_]*");
    private static final Set<String> SUPPORTED = Set.of(
            "CURRENT_ACCOUNT", "QARD_SAVINGS", "SHORT_TERM_DEPOSIT", "LONG_TERM_DEPOSIT",
            "CERTIFICATE_OF_DEPOSIT", "NOSTRO_ACCOUNT", "VOSTRO_ACCOUNT");

    private DepositProductCodePolicy() { }

    /** PRODUCT_FAMILY_CODE + bank working year + 001..999, with no manual override. */
    public static String format(String rawFamily, int workingYear, int serial) {
        String family = rawFamily == null ? "" : rawFamily.strip().toUpperCase(Locale.ROOT);
        requireSupportedFamily(family);
        if (workingYear < 1300 || workingYear > 1600)
            throw new ProductBuilderValidationException("سال کاری صدور کد محصول سپرده معتبر نیست.");
        if (serial < 1 || serial > 999)
            throw new ProductBuilderValidationException("ظرفیت ۹۹۹ کد این خانواده سپرده در سال کاری تکمیل شده است.");
        return family + workingYear + String.format(Locale.ROOT, "%03d", serial);
    }

    public static void requireSupportedFamily(String rawFamily) {
        String family = rawFamily == null ? "" : rawFamily.strip().toUpperCase(Locale.ROOT);
        if (!SUPPORTED.contains(family) || !FAMILY.matcher(family).matches() || family.length() > 23)
            throw new ProductBuilderValidationException("خانواده سپرده برای صدور کد سیستمی شناخته‌شده یا از نظر طول مجاز نیست.");
    }

    public static boolean isCorrespondent(String family) {
        return "NOSTRO_ACCOUNT".equals(family) || "VOSTRO_ACCOUNT".equals(family);
    }
}
