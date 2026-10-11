package com.behsazan.corebanking.productbuilder.application;

import java.util.regex.Pattern;

/** R28: deterministic product identity, NO client-side or in-memory serials. */
public final class LoanProductCodePolicy {
    private static final Pattern CONTRACT = Pattern.compile("[A-Z0-9][A-Z0-9_-]*");
    private LoanProductCodePolicy() { }

    /** No separators: LOAN_TYPE_CODE + WORK_YEAR + 001..999. */
    public static String format(String rawContractCode, int workingYear, int serial) {
        String code = rawContractCode == null ? "" : rawContractCode.strip().toUpperCase(java.util.Locale.ROOT);
        if (!CONTRACT.matcher(code).matches() || code.length() > 23)
            throw new ProductBuilderValidationException("کد عقد مرجع باید حداکثر ۲۳ حرف/رقم معتبر داشته باشد تا کد محصول حداکثر ۳۰ کاراکتر بماند.");
        if (workingYear < 1300 || workingYear > 1600)
            throw new ProductBuilderValidationException("سال کاری فعال در سامانه معتبر نیست؛ مسئول داده پایه باید سال را تنظیم کند.");
        if (serial < 1 || serial > 999)
            throw new ProductBuilderValidationException("ظرفیت شماره سریال سه‌رقمی این عقد در سال کاری تکمیل شده است.");
        return code + workingYear + String.format(java.util.Locale.ROOT, "%03d", serial);
    }
}
