package com.behsazan.corebanking.productbuilder.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HF7-A1: minimum verifiable cross-field contracts for existing PDL columns.
 * This is NOT a runtime rule engine, an inheritance model, or a claim that a
 * product with a valid configuration may be opened/disbursed/serviced.
 *
 * No synthetic meaning is assigned to a NULL limit. Until a separately
 * approved limit-policy state exists in the database, an ALLOWED transaction
 * or withdrawal medium without any upper cap is NOT publishable/writable.
 * Disallowed operations may be configured without limits. Historical rows
 * remain untouched: approval evaluation examines effective rows read-only.
 */
public final class ProductRuleSemanticContract {
    private ProductRuleSemanticContract() {}

    // This subset has a verifiable, self-contained meaning in persisted PDL data.
    // Other tables require parent/child, approved policy, or runtime evidence.
    private static final Set<String> APPROVAL_AUDIT = Set.of(
            "DEPOSIT_PRODUCT_OPENING_RULE",
            "DEPOSIT_PRODUCT_TRANSACTION_RULE",
            "DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA",
            "DEPOSIT_PRODUCT_JOINT_RULE",
            "DEPOSIT_PRODUCT_DORMANCY_RULE",
            "DEPOSIT_PRODUCT_HOLD_RULE",
            "DEPOSIT_PRODUCT_CLOSURE_RULE",
            "LOAN_FINANCIAL_EXTENSION",
            "LOAN_PRODUCT_REPAYMENT_RULE",
            "LOAN_PRODUCT_PROCESS_RULE",
            "LOAN_PRODUCT_COLLATERAL_RULE",
            // Existing self-contained COMMON write contracts now audited on approval.
            "PRODUCT_CHANNEL_RULE",
            "PRODUCT_ELIGIBILITY_RULE",
            "PRODUCT_REQUIRED_DOCUMENT",
            "PRODUCT_REQUIRED_INQUIRY",
            "PRODUCT_ORG_SCOPE",
            // HF7-A3/HF4: primary contractual fields of the two audited hierarchies.
            "DEPOSIT_PRODUCT_TERM_RULE",
            "PRODUCT_PRICING_RULE",
            // HF7-A3/Final: publication revalidation for existing governance controls.
            "DEPOSIT_PRODUCT_PROFILE",
            "DEPOSIT_PROFIT_PAYMENT_RULE",
            "LOAN_PRODUCT_PROFILE",
            "PRODUCT_RELATIONSHIP"
    );

    public static boolean auditsOnApproval(String table) {
        return table != null && APPROVAL_AUDIT.contains(table.trim().toUpperCase());
    }

    /** Independent of Angular and of Oracle implicit NOT NULL/default coercion. */
    public static void validate(String table, Map<String, Object> row) {
        if (table == null || row == null) return;
        switch (table.trim().toUpperCase()) {
            case "DEPOSIT_PRODUCT_OPENING_RULE" -> opening(row);
            case "DEPOSIT_PRODUCT_TRANSACTION_RULE" -> transaction(row);
            case "DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA" -> withdrawalMedia(row);
            case "DEPOSIT_PRODUCT_JOINT_RULE" -> joint(row);
            case "DEPOSIT_PRODUCT_DORMANCY_RULE" -> dormancy(row);
            case "DEPOSIT_PRODUCT_HOLD_RULE" -> hold(row);
            case "DEPOSIT_PRODUCT_CLOSURE_RULE" -> closure(row);
            case "DEPOSIT_PRODUCT_TERM_RULE" -> termRule(row);
            case "PRODUCT_PRICING_RULE" -> pricingRule(row);
            // HF7-A3/HF3: independent, persisted child-rule field semantics.
            // Child parent/scope, unique defaults and governance precedence remain separate checks.
            case "DEPOSIT_PRODUCT_ALLOWED_TERM" -> allowedTerm(row);
            case "PRODUCT_RATE_TIER" -> rateTier(row);
            case "LOAN_FINANCIAL_EXTENSION" -> loanFinancial(row);
            case "LOAN_PRODUCT_REPAYMENT_RULE" -> loanRepayment(row);
            case "LOAN_PRODUCT_PROCESS_RULE" -> loanProcess(row);
            case "LOAN_PRODUCT_COLLATERAL_RULE" -> loanCollateral(row);
            case "LOAN_ELIGIBILITY_EXTENSION" -> loanEligibility(row);
            case "PRODUCT_ELIGIBILITY_RULE" -> eligibility(row);
            case "PRODUCT_CHANNEL_RULE" -> {
                required(row, "CHANNEL_CODE", "کانال");
                binary(row, "IS_ALLOWED", "مجاز بودن کانال");
            }
            case "PRODUCT_ORG_SCOPE" -> {
                positiveLong(row, "ORG_UNIT_ID", "شناسه واحد سازمانی");
                binary(row, "IS_ALLOWED", "مجاز بودن عرضه در واحد سازمانی");
            }
            case "PRODUCT_CHANNEL_OPERATION" -> {
                required(row, "OPERATION_CODE", "نوع عملیات کانال");
                binary(row, "IS_ALLOWED", "مجاز بودن عملیات کانال");
                binary(row, "REQUIRES_ADDITIONAL_AUTH", "الزام احراز هویت تکمیلی");
            }
            case "PRODUCT_REQUIRED_INQUIRY" -> {
                required(row, "INQUIRY_TYPE_CODE", "نوع استعلام");
                required(row, "REQUIREMENT_STAGE_CODE", "مرحله استعلام");
                binary(row, "IS_MANDATORY", "الزامی بودن استعلام");
                positiveIfPresent(row, "MAX_RESULT_AGE_MINUTES", "اعتبار زمانی نتیجه استعلام");
            }
            case "PRODUCT_REQUIRED_DOCUMENT" -> {
                positiveLong(row, "DOCUMENT_TYPE_ID", "نوع مدرک");
                required(row, "REQUIREMENT_STAGE_CODE", "مرحله دریافت مدرک");
                binary(row, "IS_MANDATORY", "الزامی بودن مدرک");
                positiveIfPresent(row, "COPY_COUNT", "تعداد نسخه مدرک");
                positiveIfPresent(row, "PROCESS_STEP_NO", "مرحله فرایند");
            }
            default -> { /* No invented semantics for unreviewed tables. */ }
        }
    }

    /** Schema-backed flags and numeric values only; maturity action codes remain governed references. */
    private static void termRule(Map<String, Object> row) {
        binary(row, "IS_RENEWABLE", "مجاز بودن تمدید سپرده");
        if (present(row, "IS_ACTIVE")) binary(row, "IS_ACTIVE", "فعال بودن قاعده مدت");
        nonnegativeIntegerIfPresent(row, "MAX_RENEWAL_COUNT", "حداکثر دفعات تمدید");
        nonnegativeIntegerIfPresent(row, "GRACE_PERIOD_DAYS", "مهلت پس از سررسید");
    }

    private static void pricingRule(Map<String, Object> row) {
        required(row, "PRICING_PURPOSE_CODE", "هدف قیمت‌گذاری");
        required(row, "PRICING_METHOD_CODE", "روش قیمت‌گذاری");
        binary(row, "IS_TIERED_RATE", "پلکانی بودن نرخ");
        binary(row, "EARLY_TERMINATION_ALLOWED", "مجاز بودن خاتمه زودهنگام");
    }

    /** The term is a positive whole number in its explicitly selected unit. */
    private static void allowedTerm(Map<String, Object> row) {
        reviewedTermUnit(row, "TERM_UNIT_CODE", "واحد مدت مجاز سپرده");
        positive(row, "TERM_VALUE", "مدت مجاز سپرده");
        if (present(row, "IS_DEFAULT")) binary(row, "IS_DEFAULT", "پیش فرض بودن مدت");
        if (present(row, "IS_ACTIVE")) binary(row, "IS_ACTIVE", "فعال بودن مدت");
        positiveIntegerIfPresent(row, "DISPLAY_ORDER", "ترتیب نمایش مدت");
        // TERM_CODE is system-generated and may be absent in incoming drafts.
    }

    /** A rate tier cannot persist an implicit rate or a fractional term. */
    private static void rateTier(Map<String, Object> row) {
        positive(row, "TIER_NO", "شماره پله نرخ");
        amounts(row, "MIN_AMOUNT", "MAX_AMOUNT", "دامنه مبلغ پله نرخ");
        nonnegativeIntegerIfPresent(row, "MIN_TERM_VALUE", "حداقل مدت پله نرخ");
        nonnegativeIntegerIfPresent(row, "MAX_TERM_VALUE", "حداکثر مدت پله نرخ");
        BigDecimal minTerm = number(row, "MIN_TERM_VALUE");
        BigDecimal maxTerm = number(row, "MAX_TERM_VALUE");
        if (minTerm != null && maxTerm != null && minTerm.compareTo(maxTerm) > 0)
            fail("حداکثر مدت پله نرخ نباید از حداقل مدت کمتر باشد.");
        if (minTerm != null || maxTerm != null)
            reviewedTermUnit(row, "TERM_UNIT_CODE", "واحد مدت پله نرخ");
        else if (present(row, "TERM_UNIT_CODE"))
            fail("واحد مدت پله نرخ بدون تعیین حداقل یا حداکثر مدت کاربرد ندارد.");
        BigDecimal rate = number(row, "RATE_VALUE");
        if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0)
            fail("نرخ پله الزامی است و باید به صورت نسبت اعشاری بین صفر و یک ثبت شود (مانند ۰٫۱۸ برای ۱۸ درصد).");
    }

    /** Reviewed business terms use day/month/year. The PDL reference service
     * still verifies the actual active DPS code at the persistence boundary.
     * A numeric business code is NOT mapped to a guessed unit here.
     */
    private static void reviewedTermUnit(Map<String, Object> row, String key, String label) {
        required(row, key, label);
        String code = String.valueOf(row.get(key)).trim().toUpperCase(java.util.Locale.ROOT);
        if (!code.matches("[0-9]+") && !Set.of("DAY", "MONTH", "YEAR", "D", "M", "Y").contains(code))
            fail(label + " باید از واحدهای روز، ماه یا سال در فهرست مرجع بانک انتخاب شود.");
    }

    private static void transaction(Map<String, Object> row) {
        required(row, "TRANSACTION_TYPE_CODE", "نوع تراکنش سپرده");
        boolean allowed = binary(row, "IS_ALLOWED", "مجاز بودن تراکنش");
        amounts(row, "MIN_TRANSACTION_AMOUNT", "MAX_TRANSACTION_AMOUNT", "حد مبلغ تراکنش");
        caps(row, "DAILY_AMOUNT_LIMIT", "MONTHLY_AMOUNT_LIMIT");
        counts(row, "DAILY_COUNT_LIMIT", "MONTHLY_COUNT_LIMIT");
        if (allowed) {
            requireAnyCap(row, "تراکنش مجاز",
                    "MAX_TRANSACTION_AMOUNT", "DAILY_AMOUNT_LIMIT", "DAILY_COUNT_LIMIT",
                    "MONTHLY_AMOUNT_LIMIT", "MONTHLY_COUNT_LIMIT");
            BigDecimal minimum = number(row, "MIN_TRANSACTION_AMOUNT");
            if (minimum != null) for (String cap : List.of("MAX_TRANSACTION_AMOUNT", "DAILY_AMOUNT_LIMIT", "MONTHLY_AMOUNT_LIMIT")) {
                BigDecimal limit = number(row, cap);
                if (limit != null && limit.compareTo(minimum) < 0)
                    fail("سقف «" + cap + "» از حداقل مبلغ تراکنش کوچک‌تر است و قاعده قابل اجرا نیست.");
            }
        } else {
            requireAbsent(row, "تراکنش غیرمجاز", "MIN_TRANSACTION_AMOUNT", "MAX_TRANSACTION_AMOUNT",
                    "DAILY_AMOUNT_LIMIT", "DAILY_COUNT_LIMIT", "MONTHLY_AMOUNT_LIMIT", "MONTHLY_COUNT_LIMIT");
        }
    }

    private static void withdrawalMedia(Map<String, Object> row) {
        required(row, "WITHDRAWAL_MEDIA_CODE", "ابزار برداشت");
        boolean allowed = binary(row, "IS_ALLOWED", "مجاز بودن ابزار برداشت");
        caps(row, "MAX_TRANSACTION_AMOUNT", "DAILY_AMOUNT_LIMIT");
        counts(row, "DAILY_COUNT_LIMIT");
        if (allowed) {
            requireAnyCap(row, "ابزار برداشت مجاز", "MAX_TRANSACTION_AMOUNT", "DAILY_AMOUNT_LIMIT", "DAILY_COUNT_LIMIT");
        } else {
            requireAbsent(row, "ابزار برداشت غیرمجاز", "MAX_TRANSACTION_AMOUNT", "DAILY_AMOUNT_LIMIT", "DAILY_COUNT_LIMIT");
        }
    }

    private static void opening(Map<String, Object> row) {
        amounts(row, "MIN_OPENING_AMOUNT", "MAX_OPENING_AMOUNT", "حد مبلغ افتتاح");
        amounts(row, "MIN_REQUIRED_BALANCE", "MAX_ALLOWED_BALANCE", "حد مانده");
        positiveIntegerIfPresent(row, "MAX_CUSTOMER_ACCOUNT_COUNT", "سقف تعداد حساب مشتری");
        positiveIntegerIfPresent(row, "MAX_BANK_ACCOUNT_COUNT", "سقف تعداد حساب بانک");
        BigDecimal minOpening = number(row, "MIN_OPENING_AMOUNT");
        BigDecimal maxOpening = number(row, "MAX_OPENING_AMOUNT");
        BigDecimal requiredBalance = number(row, "MIN_REQUIRED_BALANCE");
        BigDecimal maxBalance = number(row, "MAX_ALLOWED_BALANCE");
        if (minOpening != null && maxBalance != null && minOpening.compareTo(maxBalance) > 0)
            fail("حداقل مبلغ افتتاح از حداکثر مانده مجاز بیشتر است؛ افتتاح قابل انجام نیست.");
        if (requiredBalance != null && maxOpening != null && requiredBalance.compareTo(maxOpening) > 0)
            fail("حداقل مانده لازم از حداکثر مبلغ افتتاح بیشتر است؛ قاعده قابل اجرا نیست.");
        binary(row, "IS_INTRODUCER_REQUIRED", "الزام معرفی‌کننده");
        binary(row, "IS_OVERDRAFT_ALLOWED", "مجاز بودن اضافه‌برداشت");
        boolean stamp = binary(row, "IS_STAMP_DUTY_APPLICABLE", "مشمولیت تمبر");
        positiveIntegerIfPresent(row, "STAMP_COUNT", "تعداد تمبر");
        if (stamp && !present(row, "STAMP_COUNT")) fail("هنگام مشمول بودن تمبر، تعداد تمبر باید تعیین شود.");
        if (!stamp && present(row, "STAMP_COUNT")) fail("وقتی تمبر لازم نیست، تعداد تمبر باید خالی باشد.");
    }

    private static void joint(Map<String, Object> row) {
        required(row, "OWNERSHIP_TYPE_CODE", "نوع مالکیت");
        required(row, "SIGNING_RULE_CODE", "الگوی امضا");
        int ownersMin = positive(row, "MIN_JOINT_OWNER_COUNT", "حداقل تعداد مالکان");
        int ownersMax = positive(row, "MAX_JOINT_OWNER_COUNT", "حداکثر تعداد مالکان");
        int signers = positive(row, "MIN_REQUIRED_SIGNER_COUNT", "تعداد امضاکنندگان");
        if (ownersMin > ownersMax || signers > ownersMin)
            fail("تعداد امضاکنندگان باید برای حداقل تعداد مالکان نیز قابل اجرا باشد؛ حدود مالکیت/امضا را اصلاح کنید.");
        // A single-owner deposit has exactly one owner and one required signer;
        // a profit-sharing policy between owners has no business meaning here.
        String ownership = String.valueOf(row.get("OWNERSHIP_TYPE_CODE")).trim().toUpperCase();
        if (Set.of("SINGLE", "INDIVIDUAL", "1").contains(ownership)) {
            if (ownersMin != 1 || ownersMax != 1 || signers != 1)
                fail("\u062d\u0633\u0627\u0628 \u0627\u0646\u0641\u0631\u0627\u062f\u06cc \u0628\u0627\u06cc\u062f \u062f\u0642\u06cc\u0642\u0627\u064b \u06cc\u06a9 \u0645\u0627\u0644\u06a9 \u0648 \u06cc\u06a9 \u0627\u0645\u0636\u0627\u06a9\u0646\u0646\u062f\u0647 \u062f\u0627\u0634\u062a\u0647 \u0628\u0627\u0634\u062f.");
            if (present(row, "PROFIT_DISTRIBUTION_CODE"))
                fail("\u062a\u0642\u0633\u06cc\u0645 \u0633\u0648\u062f \u0628\u06cc\u0646 \u0645\u0627\u0644\u06a9\u0627\u0646 \u0628\u0631\u0627\u06cc \u062d\u0633\u0627\u0628 \u0627\u0646\u0641\u0631\u0627\u062f\u06cc \u0641\u0627\u0642\u062f \u06a9\u0627\u0631\u0628\u0631\u062f \u0627\u0633\u062a.");
        } else if (Set.of("JOINT", "2").contains(ownership) && ownersMin < 2) {
            fail("\u062d\u0633\u0627\u0628 \u0645\u0634\u062a\u0631\u06a9 \u0628\u0627\u06cc\u062f \u062f\u0633\u062a\u200c\u06a9\u0645 \u062f\u0648 \u0645\u0627\u0644\u06a9 \u062f\u0627\u0634\u062a\u0647 \u0628\u0627\u0634\u062f.");
        }
        // Numeric DPS/PDL ownership/signature codes must not be guessed or remapped here.
    }

    private static void dormancy(Map<String, Object> row) {
        required(row, "REACTIVATION_METHOD_CODE", "روش رفع راکدی");
        binary(row, "AUTO_REACTIVATION_ALLOWED", "رفع خودکار راکدی");
    }

    private static void hold(Map<String, Object> row) {
        required(row, "HOLD_TYPE_CODE", "نوع مسدودی");
        boolean supported = binary(row, "IS_SUPPORTED", "پشتیبانی از مسدودی");
        amounts(row, "MIN_HOLD_AMOUNT", "MAX_HOLD_AMOUNT", "حد مبلغ مسدودی");
        if (!supported) requireAbsent(row, "مسدودی غیرقابل پشتیبانی", "MIN_HOLD_AMOUNT", "MAX_HOLD_AMOUNT");
        // When supported, missing limits are still an approval-policy gap;
        // do not silently re-interpret them as zero or unlimited.
    }

    private static void closure(Map<String, Object> row) {
        required(row, "CLOSURE_TYPE_CODE", "نوع خاتمه");
        boolean allowed = binary(row, "IS_CLOSURE_ALLOWED", "مجاز بودن خاتمه");
        boolean partial = binary(row, "IS_PARTIAL_CLOSURE_ALLOWED", "مجاز بودن خاتمه جزئی");
        if (partial && !allowed) fail("خاتمه جزئی بدون مجوز خاتمه اصلی مجاز نیست.");
        nonnegativeIntegerIfPresent(row, "MIN_HOLDING_PERIOD_DAYS", "حداقل مدت نگهداری");
        nonnegativeIntegerIfPresent(row, "NOTICE_PERIOD_DAYS", "زمان اعلام قبلی");
        // BALANCE_DESTINATION_CODE is optional: settlement children may specify the destination.
    }

    private static void loanFinancial(Map<String, Object> row) {
        amounts(row, "MIN_FACILITY_AMOUNT", "MAX_FACILITY_AMOUNT", "مبلغ تسهیلات");
        amounts(row, "MIN_CUSTOMER_CONTRIBUTION", "MAX_CUSTOMER_CONTRIBUTION", "آورده مشتری");
        if (present(row, "MIN_CUSTOMER_CONTRIBUTION") != present(row, "MAX_CUSTOMER_CONTRIBUTION"))
            fail("برای آورده مشتری، حداقل و حداکثر باید با هم تعیین شوند یا هر دو خالی باشند.");
        for (String key : List.of("MIN_CUSTOMER_CONTRIBUTION", "MAX_CUSTOMER_CONTRIBUTION")) {
            BigDecimal ratio = number(row, key);
            if (ratio != null && ratio.compareTo(BigDecimal.ONE) > 0)
                fail("درصد سهم / آورده مشتری باید در فرم بین ۰ تا ۱۰۰ باشد؛ مقدار ذخیره‌شده، نسبت اعشاری آن در بازه صفر تا یک است.");
        }
        boolean discount = binary(row, "INTEREST_DISCOUNT_ALLOWED", "مجاز بودن تخفیف سود");
        binary(row, "PENALTY_HOLIDAY_CALCULATION", "محاسبه وجه التزام در تعطیلات");
        nonnegativeIntegerIfPresent(row, "INTEREST_DISCOUNT_DAY", "روز اعمال تخفیف");
        if (!discount && present(row, "INTEREST_DISCOUNT_DAY")) fail("روز تخفیف برای حالت غیرفعال باید خالی باشد.");
        if (discount && !present(row, "INTEREST_DISCOUNT_DAY")) fail("برای فعال بودن تخفیف سود، روز مبنای آن باید مشخص شود.");
    }

    private static void loanRepayment(Map<String, Object> row) {
        required(row, "REPAYMENT_RULE_CODE", "الگوی بازپرداخت");
        required(row, "REPAYMENT_METHOD_CODE", "روش بازپرداخت");
        boolean grace = binary(row, "GRACE_ALLOWED", "مجاز بودن تنفس");
        boolean prepay = binary(row, "PREPAYMENT_ALLOWED", "مجاز بودن پیش‌پرداخت");
        boolean partial = binary(row, "PARTIAL_PREPAYMENT_ALLOWED", "مجاز بودن پیش‌پرداخت جزئی");
        binary(row, "EARLY_SETTLEMENT_ALLOWED", "مجاز بودن تسویه زودهنگام");
        if (partial && !prepay) fail("پیش‌پرداخت جزئی بدون مجوز پیش‌پرداخت مجاز نیست.");
        amounts(row, "MIN_TERM_DURATION", "MAX_TERM_DURATION", "مدت تسهیلات");
        amounts(row, "MIN_INSTALLMENT_COUNT", "MAX_INSTALLMENT_COUNT", "تعداد اقساط");
        amounts(row, "MIN_GRACE_DURATION", "MAX_GRACE_DURATION", "مدت تنفس");
        for (String field : List.of("MIN_TERM_DURATION", "MAX_TERM_DURATION", "MIN_INSTALLMENT_COUNT",
                "MAX_INSTALLMENT_COUNT", "MIN_GRACE_DURATION", "MAX_GRACE_DURATION"))
            nonnegativeIntegerIfPresent(row, field, "مقدار " + field);
        BigDecimal maxTerm = number(row, "MAX_TERM_DURATION");
        BigDecimal minGrace = number(row, "MIN_GRACE_DURATION");
        if (grace && maxTerm != null && minGrace != null
                && present(row, "TERM_UNIT_CODE") && present(row, "GRACE_UNIT_CODE")
                && String.valueOf(row.get("TERM_UNIT_CODE")).trim().equalsIgnoreCase(
                        String.valueOf(row.get("GRACE_UNIT_CODE")).trim())
                && minGrace.compareTo(maxTerm) > 0)
            fail("حداقل مدت تنفس از حداکثر مدت تسهیلات در واحد یکسان بیشتر است.");
        if (present(row, "INSTALLMENT_INTERVAL") != present(row, "INSTALLMENT_INTERVAL_UNIT_CODE"))
            fail("فاصله اقساط و واحد آن باید با هم تعیین شوند.");
        positiveIntegerIfPresent(row, "INSTALLMENT_INTERVAL", "فاصله اقساط");
        if (grace && !(present(row, "MIN_GRACE_DURATION") || present(row, "MAX_GRACE_DURATION")))
            fail("دوره تنفس فعال، حد مدت مصوب لازم دارد.");
    }

    private static void loanProcess(Map<String, Object> row) {
        for (String field : List.of("MANUAL_APPROVAL_REQUIRED", "COMMITTEE_APPROVAL_REQUIRED",
                "MULTI_LEVEL_APPROVAL_REQUIRED", "CBI_TRACKING_REQUIRED", "EXTERNAL_INQUIRY_REQUIRED", "ALLOW_OVERRIDE"))
            binary(row, field, "پرچم فرایند «" + field + "»");
        for (String field : List.of("APPLICATION_VALIDITY_DAYS", "APPROVAL_VALIDITY_DAYS", "CONTRACT_VALIDITY_DAYS"))
            nonnegativeIntegerIfPresent(row, field, "مدت اعتبار فرایندی");
        // 0/0/0 could mean AUTOMATIC but the schema has no explicit method column.
        // That ambiguity remains a contract gap and never grants operational OPEN.
    }

    private static void loanCollateral(Map<String, Object> row) {
        positiveLong(row, "LOAN_PRODUCT_COLLATERAL_ID", "نوع وثیقه");
        for (String field : List.of("IS_REQUIRED", "SUBSTITUTION_ALLOWED", "THIRD_PARTY_ALLOWED", "CURRENCY_MATCH_REQUIRED"))
            binary(row, field, "پرچم وثیقه «" + field + "»");
        amounts(row, "MIN_COVERAGE_PERCENT", "MAX_COVERAGE_PERCENT", "پوشش وثیقه");
        nonnegativeIntegerRequired(row, "PRIORITY_NO", "اولویت وثیقه");
    }

    private static void loanEligibility(Map<String, Object> row) {
        nonnegative(row, "MIN_INCOME_AMOUNT", "درآمد");
        nonnegative(row, "MIN_CREDIT_SCORE", "امتیاز اعتباری");
        nonnegativeIntegerIfPresent(row, "REQUIRED_GUARANTOR_COUNT", "تعداد ضامنان");
        for (String field : List.of("CBI_TRACKING_REQUIRED", "CREDIT_INQUIRY_REQUIRED", "AML_CHECK_REQUIRED"))
            binary(row, field, "پرچم اهلیت «" + field + "»");
        // A score is not verifiable without a bank-approved model/scale.
    }

    private static void eligibility(Map<String, Object> row) {
        amounts(row, "MIN_AGE", "MAX_AGE", "سن/سابقه فعالیت");
        nonnegativeIntegerIfPresent(row, "MIN_CUSTOMER_TENURE_MONTHS", "سابقه مشتری");
        nonnegativeIntegerIfPresent(row, "MIN_ACCOUNT_TENURE_MONTHS", "سابقه حساب");
        nonnegativeIntegerIfPresent(row, "MIN_AGE", "حداقل سن/سابقه فعالیت");
        nonnegativeIntegerIfPresent(row, "MAX_AGE", "حداکثر سن/سابقه فعالیت");
        nonnegative(row, "MIN_AVERAGE_BALANCE", "میانگین مانده");
        if (present(row, "PEP_ALLOWED_FLAG")) binary(row, "PEP_ALLOWED_FLAG", "مجاز بودن PEP");
        // Child criteria can supply the entire eligibility scope; do not require
        // a non-null legacy scalar field or silently treat empty as ANY.
    }

    private static void amounts(Map<String, Object> row, String minField, String maxField, String label) {
        nonnegative(row, minField, label);
        nonnegative(row, maxField, label);
        BigDecimal min = number(row, minField), max = number(row, maxField);
        if (min != null && max != null && min.compareTo(max) > 0)
            fail("حداکثر «" + label + "» نباید از حداقل آن کمتر باشد.");
    }
    private static void caps(Map<String, Object> row, String... fields) {
        for (String field : fields) positiveIfPresent(row, field, "سقف «" + field + "»");
    }
    private static void counts(Map<String, Object> row, String... fields) {
        for (String field : fields) positiveIfPresent(row, field, "سقف تعداد «" + field + "»");
        for (String field : fields) {
            BigDecimal n = number(row, field);
            if (n != null && n.stripTrailingZeros().scale() > 0)
                fail("سقف تعداد تراکنش باید عدد صحیح باشد: " + field);
        }
    }
    private static void positiveIfPresent(Map<String, Object> row, String field, String label) {
        BigDecimal n = number(row, field);
        if (n != null && n.signum() <= 0) fail(label + " باید بزرگ‌تر از صفر باشد.");
    }
    private static void positiveIntegerIfPresent(Map<String, Object> row, String field, String label) {
        BigDecimal n = number(row, field);
        if (n == null) return;
        if (n.signum() <= 0) fail(label + " باید عدد صحیح مثبت باشد.");
        try { n.toBigIntegerExact(); }
        catch (ArithmeticException ex) { fail(label + " باید عدد صحیح باشد."); }
    }
    private static void nonnegativeIntegerIfPresent(Map<String, Object> row, String field, String label) {
        BigDecimal n = number(row, field);
        if (n == null) return;
        if (n.signum() < 0) fail(label + " نباید منفی باشد.");
        try { n.toBigIntegerExact(); }
        catch (ArithmeticException ex) { fail(label + " باید عدد صحیح باشد."); }
    }
    private static void nonnegative(Map<String, Object> row, String field, String label) {
        BigDecimal n = number(row, field);
        if (n != null && n.signum() < 0) fail(label + " نباید منفی باشد.");
    }
    private static void positiveLong(Map<String, Object> row, String field, String label) {
        BigDecimal n = number(row, field);
        if (n == null || n.signum() <= 0) fail(label + " باید شناسه صحیح مثبت باشد.");
        try { n.toBigIntegerExact(); }
        catch (ArithmeticException ex) { fail(label + " باید شناسه صحیح مثبت باشد."); }
    }
    private static int positive(Map<String, Object> row, String field, String label) {
        BigDecimal n = number(row, field);
        if (n == null || n.signum() <= 0) fail(label + " باید عدد صحیح مثبت باشد.");
        try { return n.intValueExact(); }
        catch (ArithmeticException e) { throw new ProductBuilderValidationException(label + " باید عدد صحیح در دامنه مجاز باشد."); }
    }
    private static int nonnegativeIntegerRequired(Map<String, Object> row, String field, String label) {
        BigDecimal n = number(row, field);
        if (n == null || n.signum() < 0) fail(label + " باید عدد صحیح غیرمنفی باشد.");
        try { return n.intValueExact(); }
        catch (ArithmeticException ex) { throw new ProductBuilderValidationException(label + " باید عدد صحیح در دامنه مجاز باشد."); }
    }
    private static boolean binary(Map<String, Object> row, String field, String label) {
        Object raw = row.get(field);
        if (raw instanceof Boolean b) return b;
        BigDecimal n = number(row, field);
        if (n == null || !(n.compareTo(BigDecimal.ZERO) == 0 || n.compareTo(BigDecimal.ONE) == 0))
            fail(label + " باید صریحاً بله (۱) یا خیر (۰) باشد؛ NULL مجاز نیست.");
        return n.compareTo(BigDecimal.ONE) == 0;
    }
    private static boolean present(Map<String, Object> row, String field) {
        Object value = row.get(field);
        return value != null && !String.valueOf(value).isBlank();
    }
    private static void required(Map<String, Object> row, String field, String label) {
        if (!present(row, field)) fail("«" + label + "» الزامی است؛ مقدار خالی به معنی مجوز عمومی نیست.");
    }
    private static void requireAbsent(Map<String, Object> row, String context, String... fields) {
        for (String field : fields) if (present(row, field))
            fail(context + ": مقدار «" + field + "» باید حذف شود تا سیاست متناقض ذخیره نشود.");
    }
    private static void requireAnyCap(Map<String, Object> row, String context, String... fields) {
        for (String field : fields) if (present(row, field)) return;
        fail(context + ": حداقل یک سقف مبلغ/تعداد باید تعیین شود. مدل تصویب صریح «بدون سقف محصول» هنوز در PDL موجود نیست؛ NULL مجوز نامحدود نیست.");
    }
    private static BigDecimal number(Map<String, Object> row, String field) {
        if (!present(row, field)) return null;
        try { return new BigDecimal(String.valueOf(row.get(field)).trim()); }
        catch (NumberFormatException ex) { throw new ProductBuilderValidationException("عدد نامعتبر در «" + field + "»."); }
    }
    private static void fail(String reason) { throw new ProductBuilderValidationException(reason); }
}
