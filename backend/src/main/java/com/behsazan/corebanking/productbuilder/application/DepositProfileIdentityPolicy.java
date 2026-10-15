package com.behsazan.corebanking.productbuilder.application;

import java.util.Locale;

/**
 * One authoritative deposit-family to persisted business-identity contract.
 * The numeric issuance prefix is a DIFFERENT HF5 policy; these values remain
 * PDL.DEPOSIT_PRODUCT_PROFILE semantic codes, not DPS surrogate IDs.
 */
public final class DepositProfileIdentityPolicy {
    private DepositProfileIdentityPolicy() { }

    public static String[] forFamily(String productFamilyCode) {
        String family = productFamilyCode == null ? "" : productFamilyCode.trim().toUpperCase(Locale.ROOT);
        return switch (family) {
            case "SAVINGS", "QARD_SAVINGS" -> new String[]{"QARD_HASAN", "SAVINGS"};
            case "CURRENT", "CURRENT_ACCOUNT" -> new String[]{"CURRENT", "CURRENT_NO_INTEREST"};
            case "SHORT_TERM_DEPOSIT" -> new String[]{"SHORT_TERM", "SHORT_TERM_DEPOSIT"};
            case "LONG_TERM_DEPOSIT" -> new String[]{"LONG_TERM", "LONG_TERM_DEPOSIT"};
            case "CERTIFICATE_OF_DEPOSIT" -> new String[]{"CERTIFICATE", "CERTIFICATE_OF_DEPOSIT"};
            default -> throw new ProductBuilderValidationException(
                    "خانواده محصول سپرده نگاشت معتبر گروه و نوع ندارد؛ از اصلاح داده‌های مرجع شروع کنید.");
        };
    }
}
