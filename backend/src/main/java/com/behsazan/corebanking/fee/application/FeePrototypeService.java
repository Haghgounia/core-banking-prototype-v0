package com.behsazan.corebanking.fee.application;

import com.behsazan.corebanking.fee.domain.FeeModels.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class FeePrototypeService {
    public FeePrototypeMetadata metadata() {
        return new FeePrototypeMetadata(
                options("TRANSFER|انتقال وجوه", "ACCOUNT|خدمات حساب", "PAYMENT|پرداخت", "OTHER|سایر"),
                options("MONEY_TRANSFER|انتقال وجه", "ACCOUNT_MAINTENANCE|نگهداری حساب", "CASH_WITHDRAWAL|برداشت نقدی"),
                options("SATNA|ساتنا", "PAYA|پایا", "INTERNAL_TRANSFER|انتقال داخلی"),
                options("BRANCH|شعبه", "MOBILE|همراه بانک", "INTERNET|اینترنت بانک", "OPEN_API|API باز"),
                options("IRR|ریال ایران", "USD|دلار آمریکا", "EUR|یورو"),
                options("MASS|عمومی", "VIP|ویژه", "CORPORATE|شرکتی", "STAFF|کارکنان"),
                options("BANK|بانک ملت", "BRANCH|شعبه عامل", "EXTERNAL|سازمان بیرونی"),
                options("FIXED|مبلغ ثابت", "PERCENTAGE|درصدی", "PERCENTAGE_FLOOR_CAP|درصدی با حداقل و حداکثر", "FIXED_PLUS_PERCENTAGE|ثابت + درصد", "TIERED|پلکانی"),
                options("PERCENT_REDUCTION|تخفیف درصدی", "FIXED_REDUCTION|تخفیف مبلغی", "WAIVER|معافیت کامل", "REPLACE_AMOUNT|جایگزینی مبلغ", "SURCHARGE|افزایش"),
                options("BANK|بانک", "AGENT_BRANCH|شعبه عامل", "OWNER_BRANCH|شعبه مالک", "CENTRAL_BANK|بانک مرکزی", "EXTERNAL_ORG|سازمان بیرونی"),
                options("DEBIT_ACCOUNT|برداشت از حساب", "FROM_TRANSACTION_AMOUNT|کسر از مبلغ تراکنش", "CASH|نقدی", "SEPARATE_PAYMENT|پرداخت جداگانه"),
                options("BEFORE_TRANSACTION|قبل از تراکنش", "DURING_TRANSACTION|همزمان با تراکنش", "AFTER_TRANSACTION|بعد از تراکنش", "PERIODIC|دوره‌ای", "ON_EVENT|رویدادمحور")
        );
    }

    public CalculateFeeResponse calculate(CalculateFeeRequest r) {
        BigDecimal basis = nz(r.basisAmount());
        BigDecimal fixed = nz(r.fixedAmount());
        BigDecimal rate = nz(r.rate());
        BigDecimal raw = switch (r.strategy()) {
            case "FIXED" -> fixed;
            case "PERCENTAGE", "PERCENTAGE_FLOOR_CAP" -> basis.multiply(rate);
            case "FIXED_PLUS_PERCENTAGE" -> fixed.add(basis.multiply(rate));
            case "TIERED" -> basis.compareTo(new BigDecimal("100000000")) <= 0
                    ? new BigDecimal("50000")
                    : basis.multiply(new BigDecimal("0.0015"));
            default -> BigDecimal.ZERO;
        };
        BigDecimal gross = raw;
        if (r.minFeeAmount() != null) gross = gross.max(r.minFeeAmount());
        if (r.maxFeeAmount() != null && r.maxFeeAmount().signum() > 0) gross = gross.min(r.maxFeeAmount());
        gross = gross.setScale(0, RoundingMode.HALF_UP);
        BigDecimal discountRate = nz(r.discountRate());
        BigDecimal adjustment = gross.multiply(discountRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(adjustment).max(BigDecimal.ZERO);
        return new CalculateFeeResponse(r.feeCode(), r.strategy(), basis, r.currencyCode(), gross, adjustment, net,
                List.of(
                        new CalculationStep(1, "مبلغ مبنا", "مبلغ/مبنای ورودی برای محاسبه", basis),
                        new CalculationStep(2, "محاسبه ناخالص", "اعمال Strategy، حداقل و حداکثر", gross),
                        new CalculationStep(3, "تعدیل", "اعمال تخفیف/معافیت انتخاب‌شده", adjustment),
                        new CalculationStep(4, "کارمزد نهایی", "مبلغ نهایی قابل وصول", net)
                ));
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static List<LookupOption> options(String... values) {
        return java.util.Arrays.stream(values).map(v -> v.split("\\|", 2)).map(a -> new LookupOption(a[0], a[1])).toList();
    }
}
