package com.behsazan.corebanking.deposit.account.error;

public class CorrespondentProductProfileNotFoundException extends RuntimeException {
    private final String accountTypeCode;
    private final String settlementCurrencyCode;

    public CorrespondentProductProfileNotFoundException(String accountTypeCode, String settlementCurrencyCode) {
        super("Correspondent Product Profile برای نوع/ارز انتخابی یافت نشد؛ ابتدا Product Builder را مطابق فایل مرجع پیکربندی کنید.");
        this.accountTypeCode = accountTypeCode;
        this.settlementCurrencyCode = settlementCurrencyCode;
    }

    public String accountTypeCode() {
        return accountTypeCode;
    }

    public String settlementCurrencyCode() {
        return settlementCurrencyCode;
    }
}
