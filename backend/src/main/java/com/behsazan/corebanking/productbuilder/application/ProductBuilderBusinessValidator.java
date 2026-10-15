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
        // HF7-A1: enforce the same minimum semantic contract for Angular,
        // direct API requests, and atomic eligibility/loan-extension saves.
        ProductRuleSemanticContract.validate(table, values);
        switch (table.trim().toUpperCase()) {
            case "DEPOSIT_PROFIT_PAYMENT_RULE" -> validateProfitPayment(values);
            case "DEPOSIT_PRODUCT_OPENING_RULE" -> validateOpeningAmounts(values);
            case "DEPOSIT_PRODUCT_JOINT_RULE" -> validateJointOwnerCounts(values);
            case "DEPOSIT_PRODUCT_DORMANCY_RULE" -> validateDepositDormancy(values);
            case "DEPOSIT_PRODUCT_CLOSURE_RULE" -> validateDepositClosure(values);
            case "DEPOSIT_PRODUCT_CLOSURE_PRECHECK" -> validateDepositClosurePrecheck(values);
            case "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE" -> validateDepositClosureSettlement(values);
            case "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE" -> validateDepositClosureApproval(values);
            case "PRODUCT_PRICING_RULE" -> validatePricingRule(values);
            case "PRODUCT_RELATIONSHIP" -> validateRelationship(values);
            case "LOAN_PRODUCT_PROFILE" -> validateLoanProfile(values);
            case "FACILITY_OPERATION_PARAMETER" -> validateFacilityOperationParameter(values);
            case "LOAN_PRODUCT_REPAYMENT_RULE" -> validateLoanRepayment(values);
            case "LOAN_PRODUCT_PROCESS_RULE" -> validateLoanProcess(values);
            case "LOAN_PRODUCT_COLLATERAL_RULE" -> validateLoanCollateral(values);
            case "LOAN_FINANCIAL_EXTENSION" -> validateLoanFinancial(values);
            case "LOAN_ELIGIBILITY_EXTENSION" -> validateLoanEligibility(values);
            case "CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE" -> validateCorrespondentProfile(values);
            case "CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE" -> validateCorrespondentSettlement(values);
            default -> validateGenericDateRange(values);
        }
    }

    private void nonnegative(Map<String, Object> values, String key, String caption) {
        BigDecimal number = decimal(values, key);
        if (number != null && number.signum() < 0) fail(caption + " نمی‌تواند منفی باشد.");
    }
    private void durationRange(Map<String, Object> values, String minKey, String maxKey, String caption) {
        nonnegative(values, minKey, caption);
        nonnegative(values, maxKey, caption);
        BigDecimal min = decimal(values, minKey), max = decimal(values, maxKey);
        if (min != null && max != null && max.compareTo(min) < 0) fail("حداکثر " + caption + " نمی‌تواند کمتر از حداقل باشد.");
    }
    private void validateLoanProfile(Map<String, Object> values) {
        String nature = text(values, "PARTY_NATURE_CODE");
        if (!java.util.Set.of("ALL", "NATURAL", "LEGAL").contains(nature))
            fail("ماهیت متقاضی باید «همه موارد»، «حقیقی» یا «حقوقی» باشد.");
    }

    /** This table is already present in PDL; no invented meanings for METHOD numeric codes. */
    private void validateFacilityOperationParameter(Map<String, Object> values) {
        for (String column : new String[]{"OVERDUE_DURATION", "PASTDUE_DURATION", "DOUBTFUL_DURATION"}) {
            Integer days = integer(values, column);
            if (days == null || days < 1 || days > 99)
                fail("مدت‌های طبقه‌بندی مطالبات باید عدد صحیح مثبت بین ۱ تا ۹۹ باشند.");
        }
        Integer method = integer(values, "TRANSFER_CATEGORY_METHOD");
        if (method == null || method < 0 || method > 9)
            fail("کد روش انتقال طبقه باید یک رقم صحیح طبق دستورالعمل بانک باشد.");
        for (String column : new String[]{"TRANSFER_PRINCIPLE", "TRANSFER_INTEREST", "TRANSFER_PENALTY", "TRANSFER_COMMISSION"}) {
            Integer flag = integer(values, column);
            if (flag == null || (flag != 0 && flag != 1))
                fail("اقلام انتقال مطالبات فقط مقدار بله/خیر (۱ یا ۰) می‌پذیرند.");
        }
        if (text(values, "PARAMETER_STATUS_CODE").isBlank())
            fail("کد وضعیت پارامتر عملیاتی الزامی است؛ مطابق دستورالعمل بانک وارد کنید.");
    }

    private void validateLoanRepayment(Map<String, Object> values) {
        if (text(values, "REPAYMENT_RULE_CODE").isBlank() || text(values, "REPAYMENT_METHOD_CODE").isBlank())
            fail("انتخاب الگوی بازپرداخت و روش بازپرداخت الزامی است.");
        if (text(values, "REPAYMENT_RULE_NAME").isBlank()) fail("عنوان الگوی بازپرداخت باید از الگوی انتخابی تعیین شود.");
        durationRange(values, "MIN_TERM_DURATION", "MAX_TERM_DURATION", "مدت تسهیلات");
        String unit = text(values, "TERM_UNIT_CODE");
        if ((decimal(values, "MIN_TERM_DURATION") != null || decimal(values, "MAX_TERM_DURATION") != null) && unit.isBlank())
            fail("برای حداقل و حداکثر مدت تسهیلات، واحد روز یا ماه را تعیین کنید.");
        durationRange(values, "MIN_INSTALLMENT_COUNT", "MAX_INSTALLMENT_COUNT", "تعداد اقساط");
        durationRange(values, "MIN_GRACE_DURATION", "MAX_GRACE_DURATION", "دوره تنفس");
        Integer interval = integer(values, "INSTALLMENT_INTERVAL");
        if (interval != null && interval <= 0) fail("فاصله اقساط باید عدد صحیح مثبت باشد.");
        if (interval != null && text(values, "INSTALLMENT_INTERVAL_UNIT_CODE").isBlank())
            fail("واحد فاصله اقساط را انتخاب کنید.");
        if (!bool(values, "GRACE_ALLOWED")) {
            if (values.get("MIN_GRACE_DURATION") != null || values.get("MAX_GRACE_DURATION") != null ||
                    !text(values, "GRACE_METHOD_CODE").isBlank() || !text(values, "GRACE_UNIT_CODE").isBlank())
                fail("در صورت غیرفعال بودن دوره تنفس، جزئیات دوره تنفس باید خالی باشند.");
        } else if (text(values, "GRACE_METHOD_CODE").isBlank() || text(values, "GRACE_UNIT_CODE").isBlank())
            fail("وقتی تنفس مجاز است، روش و واحد مدت تنفس باید انتخاب شوند.");
        if (bool(values, "PARTIAL_PREPAYMENT_ALLOWED") && !bool(values, "PREPAYMENT_ALLOWED"))
            fail("پیش‌پرداخت جزئی فقط زمانی مجاز است که اصل پیش‌پرداخت مجاز باشد.");
        validateGenericDateRange(values);
    }
    private void validateLoanProcess(Map<String, Object> values) {
        int selectedApprovalModes = 0;
        for (String flag : new String[]{"MANUAL_APPROVAL_REQUIRED", "COMMITTEE_APPROVAL_REQUIRED", "MULTI_LEVEL_APPROVAL_REQUIRED"}) {
            if (bool(values, flag)) selectedApprovalModes++;
        }
        if (selectedApprovalModes > 1)
            fail("روش اصلی تصویب باید یکتا باشد: دستی، کمیته یا چندسطحی. مراحل ترکیبی باید در گردش کار مصوب تعریف شوند، نه با فعال‌کردن هم‌زمان سه گزینه.");
        for (String key : new String[]{"APPLICATION_VALIDITY_DAYS", "APPROVAL_VALIDITY_DAYS", "CONTRACT_VALIDITY_DAYS"})
            nonnegative(values, key, "مدت اعتبار");
        if (!bool(values, "ALLOW_OVERRIDE") && !text(values, "OVERRIDE_APPROVAL_LEVEL_CODE").isBlank())
            fail("سطح تأیید استثناء فقط وقتی قابل ثبت است که استثناء مجاز باشد.");
        if (bool(values, "ALLOW_OVERRIDE") && text(values, "OVERRIDE_APPROVAL_LEVEL_CODE").isBlank())
            fail("در صورت مجاز بودن استثناء، سطح تأیید آن باید تعیین شود.");
        validateGenericDateRange(values);
    }
    private void validateLoanCollateral(Map<String, Object> values) {
        durationRange(values, "MIN_COVERAGE_PERCENT", "MAX_COVERAGE_PERCENT", "پوشش وثیقه");
        Integer priority = integer(values, "PRIORITY_NO");
        if (priority == null || priority < 0) fail("اولویت وثیقه باید صفر یا عدد صحیح مثبت باشد.");
        // In the supplied PDL contract 1.20 means 120 percent; never silently multiply by 100.
    }
    private void validateLoanFinancial(Map<String, Object> values) {
        durationRange(values, "MIN_FACILITY_AMOUNT", "MAX_FACILITY_AMOUNT", "مبلغ تسهیلات");
        durationRange(values, "MIN_CUSTOMER_CONTRIBUTION", "MAX_CUSTOMER_CONTRIBUTION", "آورده مشتری");
        BigDecimal max = decimal(values, "MAX_CUSTOMER_CONTRIBUTION");
        BigDecimal min = decimal(values, "MIN_CUSTOMER_CONTRIBUTION");
        if ((min != null && min.compareTo(BigDecimal.ONE) > 0) ||
                (max != null && max.compareTo(BigDecimal.ONE) > 0)) fail("درصد آورده مشتری باید بین صفر و صد درصد باشد (در ذخیره‌سازی به نسبت صفر تا یک تبدیل می‌شود).");
        nonnegative(values, "INTEREST_DISCOUNT_DAY", "روز اعمال تخفیف سود");
        if (!bool(values, "INTEREST_DISCOUNT_ALLOWED") && values.get("INTEREST_DISCOUNT_DAY") != null)
            fail("تعداد روز مبنای تخفیف سود فقط زمانی قابل ثبت است که سوئیچ مجاز بودن تخفیف سود فعال باشد.");
        validateGenericDateRange(values);
    }
    private void validateLoanEligibility(Map<String, Object> values) {
        nonnegative(values, "MIN_INCOME_AMOUNT", "حداقل درآمد");
        nonnegative(values, "MIN_CREDIT_SCORE", "حداقل امتیاز اعتباری");
        nonnegative(values, "REQUIRED_GUARANTOR_COUNT", "تعداد ضامنان");
    }

    /** The OFF position of dormancy auto-reactivation is an explicit numeric zero. */
    private void validateDepositDormancy(Map<String, Object> values) {
        Integer inactivity = integer(values, "INACTIVITY_PERIOD_VALUE");
        if (inactivity == null || inactivity <= 0) fail("مقدار دوره عدم فعالیت باید عدد صحیح مثبت باشد.");
        if (text(values, "INACTIVITY_PERIOD_UNIT_CODE").isBlank())
            fail("واحد دوره عدم فعالیت را انتخاب کنید.");
        Integer warning = integer(values, "WARNING_PERIOD_VALUE");
        String warningUnit = text(values, "WARNING_PERIOD_UNIT_CODE");
        if ((warning == null) != warningUnit.isBlank())
            fail("مقدار و واحد دوره هشدار پیش از راکدی باید با هم تعیین شوند.");
        if (warning != null && warning < 0)
            fail("مقدار هشدار پیش از راکدی نباید منفی باشد.");
        if (warning != null && warningUnit.equals(text(values, "INACTIVITY_PERIOD_UNIT_CODE")) && warning > inactivity)
            fail("در واحد یکسان، مدت هشدار نباید از دوره عدم فعالیت بیشتر باشد.");
        binaryFlag(values, "AUTO_REACTIVATION_ALLOWED", "رفع خودکار راکدی");
        validateGenericDateRange(values);
    }

    private void validateDepositClosure(Map<String, Object> values) {
        binaryFlag(values, "IS_CLOSURE_ALLOWED", "مجاز بودن خاتمه");
        binaryFlag(values, "IS_PARTIAL_CLOSURE_ALLOWED", "مجاز بودن خاتمه جزئی");
        if (bool(values, "IS_PARTIAL_CLOSURE_ALLOWED") && !bool(values, "IS_CLOSURE_ALLOWED"))
            fail("خاتمه جزئی تنها زمانی مجاز است که اصل خاتمه سپرده مجاز باشد.");
        nonnegative(values, "MIN_HOLDING_PERIOD_DAYS", "حداقل مدت نگهداری");
        nonnegative(values, "NOTICE_PERIOD_DAYS", "مدت اعلام قبلی");
        validateGenericDateRange(values);
    }

    private void validateDepositClosurePrecheck(Map<String, Object> values) {
        if (text(values, "CHECK_TYPE_CODE").isBlank() || text(values, "FAILURE_ACTION_CODE").isBlank())
            fail("نوع کنترل پیش از خاتمه و اقدام در صورت عدم موفقیت باید از فهرست مصوب انتخاب شوند.");
        requirePositiveSequence(values, "SEQUENCE_NO", "ترتیب اجرای کنترل پیش از خاتمه");
        binaryFlag(values, "IS_MANDATORY", "الزامی بودن کنترل پیش از خاتمه");
    }

    private void validateDepositClosureSettlement(Map<String, Object> values) {
        if (text(values, "SETTLEMENT_COMPONENT_CODE").isBlank() || text(values, "SETTLEMENT_METHOD_CODE").isBlank())
            fail("جزء مالی و روش تسویه هنگام خاتمه باید تعیین شوند.");
        String method = text(values, "SETTLEMENT_METHOD_CODE");
        if (!java.util.Set.of("PAY", "TRANSFER", "DEDUCT", "WAIVE").contains(method))
            fail("\u0631\u0648\u0634 \u062a\u0633\u0648\u06cc\u0647 \u0628\u0627\u06cc\u062f \u0627\u0632 \u06af\u0632\u06cc\u0646\u0647\u200c\u0647\u0627\u06cc \u0645\u0635\u0648\u0628 \u0627\u0646\u062a\u062e\u0627\u0628 \u0634\u0648\u062f.");
        String destination = text(values, "DESTINATION_CODE");
        if (("TRANSFER".equals(method) || "PAY".equals(method)) && destination.isBlank())
            fail("\u0628\u0631\u0627\u06cc \u0631\u0648\u0634 \u067e\u0631\u062f\u0627\u062e\u062a \u06cc\u0627 \u0627\u0646\u062a\u0642\u0627\u0644\u060c \u0645\u0642\u0635\u062f \u062a\u0633\u0648\u06cc\u0647 \u0627\u0644\u0632\u0627\u0645\u06cc \u0627\u0633\u062a.");
        if (("DEDUCT".equals(method) || "WAIVE".equals(method)) && !destination.isBlank())
            fail("\u062f\u0631 \u0631\u0648\u0634 \u06a9\u0633\u0631 \u0627\u0632 \u0645\u0627\u0646\u062f\u0647 \u06cc\u0627 \u0628\u062e\u0634\u0648\u062f\u06af\u06cc\u060c \u0645\u0642\u0635\u062f \u062a\u0633\u0648\u06cc\u0647 \u0628\u0627\u06cc\u062f \u062e\u0627\u0644\u06cc \u0628\u0627\u0634\u062f.");
        if ("TRANSFER".equals(method) && "CASH".equals(destination))
            fail("\u067e\u0631\u062f\u0627\u062e\u062a \u0646\u0642\u062f\u06cc \u0645\u0642\u0635\u062f \u0627\u0646\u062a\u0642\u0627\u0644 \u0628\u0627\u0646\u06a9\u06cc \u0646\u06cc\u0633\u062a.");
        if ("PAY".equals(method) && "BANK_ACCOUNT".equals(destination))
            fail("\u067e\u0631\u062f\u0627\u062e\u062a \u0628\u0647 \u0645\u0634\u062a\u0631\u06cc \u0646\u0628\u0627\u06cc\u062f \u0628\u0647 \u062d\u0633\u0627\u0628 \u062f\u0627\u062e\u0644\u06cc \u0628\u0627\u0646\u06a9 \u062a\u0633\u0648\u06cc\u0647 \u0634\u0648\u062f.");
        requirePositiveSequence(values, "SEQUENCE_NO", "ترتیب اجرای تسویه");
        binaryFlag(values, "IS_MANDATORY", "الزامی بودن مرحله تسویه");
    }

    private void validateDepositClosureApproval(Map<String, Object> values) {
        if (text(values, "APPROVAL_LEVEL_CODE").isBlank()) fail("نقش یا سطح تأییدکننده خاتمه الزامی است.");
        requirePositiveSequence(values, "APPROVAL_SEQUENCE_NO", "ترتیب تأیید خاتمه");
        durationRange(values, "MIN_AMOUNT", "MAX_AMOUNT", "مبلغ مشمول تأیید");
        binaryFlag(values, "IS_MANDATORY", "الزامی بودن تأیید خاتمه");
    }

    private void requirePositiveSequence(Map<String, Object> values, String key, String caption) {
        Integer sequence = integer(values, key);
        if (sequence == null || sequence <= 0) fail(caption + " باید عدد صحیح مثبت باشد.");
    }

    private void binaryFlag(Map<String, Object> values, String key, String caption) {
        Integer flag = integer(values, key);
        if (flag == null || (flag != 0 && flag != 1))
            fail(caption + " فقط مقدار ۰ یا ۱ می‌پذیرد.");
    }

    private void validateOpeningAmounts(Map<String, Object> values) {
        // Nullable limits mean "not configured", never 0. The bank's approval
        // policy may later require explicit values before product activation.
        BigDecimal minOpening = decimal(values, "MIN_OPENING_AMOUNT");
        BigDecimal maxOpening = decimal(values, "MAX_OPENING_AMOUNT");
        BigDecimal minBalance = decimal(values, "MIN_REQUIRED_BALANCE");
        BigDecimal maxBalance = decimal(values, "MAX_ALLOWED_BALANCE");
        if ((minOpening != null && minOpening.signum() < 0)
                || (maxOpening != null && maxOpening.signum() < 0)
                || (minBalance != null && minBalance.signum() < 0)
                || (maxBalance != null && maxBalance.signum() < 0)) {
            fail("حدود مالی افتتاح و نگهداری مانده نمی‌توانند منفی باشند.");
        }
        if (minOpening != null && maxOpening != null && maxOpening.compareTo(minOpening) < 0)
            fail("حداکثر مبلغ افتتاح نباید کمتر از حداقل مبلغ افتتاح باشد.");
        if (minBalance != null && maxBalance != null && maxBalance.compareTo(minBalance) < 0)
            fail("حداکثر مانده مجاز نباید کمتر از حداقل مانده لازم باشد.");
        validateGenericDateRange(values);
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
        if ("TRUE".equalsIgnoreCase(text) || "Y".equalsIgnoreCase(text)) return true;
        // Oracle NUMBER(1) can arrive as BigDecimal(1.0), or as numeric text from an API.
        // The semantic contract has already rejected invalid mandatory binary flags.
        try { return new BigDecimal(text).compareTo(BigDecimal.ONE) == 0; }
        catch (NumberFormatException ignored) { return false; }
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
