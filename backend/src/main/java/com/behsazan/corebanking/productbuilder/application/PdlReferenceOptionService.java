package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.ColumnDescriptor;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TableDescriptor;
import com.behsazan.corebanking.referencedata.management.application.ReferenceService;
import com.behsazan.corebanking.cif.reference.application.PartyReferenceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Bridges Product Definition business-code columns to governed reference data.
 *
 * PDL stores business codes (PERSON, BRANCH, ...), while many reference tables
 * have their own numeric surrogate keys.  Therefore selector values are always
 * the reference CODE and never the surrogate key.  This keeps persistence
 * compatible while presenting the Persian reference title to bank users.
 */
@Service
public class PdlReferenceOptionService {
    private static final Set<String> COMMON_RULE_TABLES = Set.of(
            "PRODUCT_ELIGIBILITY_RULE",
            "PRODUCT_ELIGIBILITY_CRITERION",
            "PRODUCT_CHANNEL_RULE",
            "PRODUCT_CHANNEL_OPERATION",
            "PRODUCT_ORG_SCOPE",
            "PRODUCT_REQUIRED_DOCUMENT",
            "PRODUCT_REQUIRED_INQUIRY",
            "PRODUCT_PRICING_RULE",
            "PRODUCT_PRICING_COMPONENT",
            "PRODUCT_RATE_TIER",
            "PRODUCT_RELATIONSHIP",
            "LOAN_ELIGIBILITY_EXTENSION"
    );

    private static final Map<String, String> RESOURCE_BY_COLUMN = Map.ofEntries(
            Map.entry("CUSTOMER_SEGMENT_CODE", "dps-customer-segments"),
            Map.entry("KYC_LEVEL_CODE", "dps-kyc-levels"),
            Map.entry("MIN_KYC_LEVEL_CODE", "dps-kyc-levels"),
            Map.entry("REQUIRED_KYC_LEVEL_CODE", "dps-kyc-levels"),
            Map.entry("NATIONALITY_SCOPE_CODE", "dps-nationality-scopes"),
            Map.entry("GENDER_CODE", "dps-genders"),
            Map.entry("CHANNEL_CODE", "dps-channels"),
            Map.entry("OPENING_CHANNEL_CODE", "dps-channels"),
            Map.entry("OPERATION_CODE", "dps2-operation"),
            Map.entry("TRANSACTION_TYPE_CODE", "dps-transaction-types"),
            Map.entry("ORG_UNIT_TYPE_CODE", "dps-org-unit-types"),
            Map.entry("ORG_UNIT_CODE", "dps-org-units"),
            Map.entry("REQUIREMENT_STAGE_CODE", "dps-requirement-stages"),
            Map.entry("RELATIONSHIP_TYPE_CODE", "dps-relationship-types"),
            Map.entry("CURRENCY_CODE", "dps-default-currencies"),
            Map.entry("DEFAULT_CURRENCY_CODE", "dps-default-currencies"),
            Map.entry("SETTLEMENT_CURRENCY_CODE", "dps-default-currencies"),
            Map.entry("DAY_COUNT_BASIS_CODE", "dps-day-count-bases"),
            Map.entry("PAYMENT_FREQUENCY_CODE", "dps-payment-frequencies"),
            Map.entry("ACCRUAL_FREQUENCY_CODE", "dps-accrual-frequencies"),
            Map.entry("RULE_STATUS_CODE", "dps-rule-statuses"),
            Map.entry("APPROVAL_LEVEL_CODE", "dps-approval-levels"),
            Map.entry("TERM_UNIT_CODE", "dps-term-units"),
            Map.entry("DOCUMENT_TYPE_CODE", "dps-document-types"),
            Map.entry("INQUIRY_TYPE_CODE", "dps-inquiry-types"),
            // PB-R13: governed deposit module references. Persist the reference CODE, never surrogate ID.
            Map.entry("DEPOSIT_GROUP_CODE", "dps-deposit-groups"),
            Map.entry("DEPOSIT_TYPE_CODE", "dps-deposit-types"),
            Map.entry("WITHDRAWAL_MEDIA_CODE", "dps-withdrawal-media"),
            Map.entry("OWNERSHIP_TYPE_CODE", "dps-ownership-types"),
            Map.entry("SIGNING_RULE_CODE", "dps-signing-rules"),
            Map.entry("PROFIT_DISTRIBUTION_CODE", "dps-profit-distributions"),
            Map.entry("INACTIVITY_PERIOD_UNIT_CODE", "dps-inactivity-period-units"),
            Map.entry("WARNING_PERIOD_UNIT_CODE", "dps-warning-period-units"),
            Map.entry("REACTIVATION_METHOD_CODE", "dps-reactivation-methods"),
            Map.entry("HOLD_TYPE_CODE", "dps-hold-types"),
            Map.entry("CLOSURE_TYPE_CODE", "dps-closure-types"),
            Map.entry("BALANCE_DESTINATION_CODE", "dps-balance-destinations"),
            Map.entry("SETTLEMENT_METHOD_CODE", "dps-settlement-methods"),
            Map.entry("DESTINATION_CODE", "dps-destinations")
    );


    private static final Set<String> GOVERNED_DEPOSIT_TABLES = Set.of(
            "DEPOSIT_PRODUCT_PROFILE", "DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA",
            "DEPOSIT_PRODUCT_JOINT_RULE", "DEPOSIT_PRODUCT_DORMANCY_RULE",
            "DEPOSIT_PRODUCT_HOLD_RULE", "DEPOSIT_PRODUCT_CLOSURE_RULE",
            "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE"
    );

    // DPS historical seeds use numeric business codes for these three reference tables,
    // while PDL may use semantic or legacy numeric codes depending on migration.
    // The actual Oracle PDL CHECK is the final authority. This does not mutate DPS.
    // Values with no exact business equivalent are deliberately excluded.
    private static final Map<String, Map<String, String>> JOINT_CODE_TRANSLATIONS = Map.of(
            "OWNERSHIP_TYPE_CODE", Map.of("1", "SINGLE", "2", "JOINT"),
            "SIGNING_RULE_CODE", Map.of("1", "ANY_TO_SIGN", "2", "BOTH_TO_SIGN"),
            "PROFIT_DISTRIBUTION_CODE", Map.of("1", "EQUAL", "2", "OWNERSHIP_SHARE", "3", "CUSTOM_PERCENT")
    );

    // The approved Product Builder form specifies semantic choices even when DPS
    // reference tables are not seeded. Never show numeric DPS surrogates to users.
    // When Oracle publishes CHECK choices, constrain this contract to those codes.
    private static final Map<String, List<SelectOption>> JOINT_FORM_CONTRACT = Map.of(
            "OWNERSHIP_TYPE_CODE", optsLabeled("SINGLE", "انفرادی", "JOINT", "مشترک"),
            "SIGNING_RULE_CODE", optsLabeled("ANY_TO_SIGN", "امضای هر یک کافی است",
                    "BOTH_TO_SIGN", "امضای همه لازم است", "N_OF_M", "تعداد مشخصی از امضاکنندگان"),
            "PROFIT_DISTRIBUTION_CODE", optsLabeled("OWNERSHIP_SHARE", "بر اساس سهم مالکیت",
                    "EQUAL", "به نسبت مساوی", "CUSTOM_PERCENT", "بر اساس درصد توافق‌شده")
    );

    // Labels come from the signed-off interactive Product Builder contract. These
    // options are *only* exposed when the actual PDL database CHECK permits them.
    private static final Map<String, String> PDL_DEPOSIT_CHECK_LABELS = Map.ofEntries(
            Map.entry("LEGAL", "قضایی / قانونی"),
            Map.entry("COLLATERAL", "وثیقه‌ای"),
            Map.entry("INTERNAL", "کنترلی / داخلی بانک"),
            Map.entry("CUSTOMER_REQUEST", "به درخواست مشتری"),
            Map.entry("NORMAL", "عادی"),
            Map.entry("EARLY", "پیش از موعد"),
            Map.entry("FORCED", "اجباری"),
            Map.entry("PARTIAL", "جزئی"),
            Map.entry("CUSTOMER_ACCOUNT", "انتقال به حساب مشتری"),
            Map.entry("CASH", "پرداخت نقدی طبق ضوابط"),
            Map.entry("SUSPENSE", "انتقال به حساب واسط / معلق"),
            Map.entry("CUSTOMER_SELECTED", "مقصد انتخابی مشتری"),
            Map.entry("BANK_ACCOUNT", "حساب داخلی بانک"),
            Map.entry("PAY", "پرداخت به مشتری"),
            Map.entry("DEDUCT", "کسر از مانده"),
            Map.entry("TRANSFER", "انتقال به حساب مقصد"),
            Map.entry("WAIVE", "بخشودگی با مجوز"),
            Map.entry("DRAFT", "پیش‌نویس"),
            Map.entry("ACTIVE", "فعال"),
            Map.entry("INACTIVE", "غیرفعال")
    );

    private static final Set<String> CHECK_BACKED_FIELDS = Set.of(
            "DEPOSIT_PRODUCT_HOLD_RULE.HOLD_TYPE_CODE",
            "DEPOSIT_PRODUCT_CLOSURE_RULE.CLOSURE_TYPE_CODE",
            "DEPOSIT_PRODUCT_CLOSURE_RULE.BALANCE_DESTINATION_CODE",
            "DEPOSIT_PRODUCT_CLOSURE_RULE.STATUS_CODE",
            "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE.SETTLEMENT_METHOD_CODE",
            "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE.DESTINATION_CODE"
    );

    private static final Map<String, String> PARTY_RESOURCE_BY_COLUMN = Map.ofEntries(
            Map.entry("PARTY_TYPE_CODE", "ref-party-type"),
            Map.entry("CUSTOMER_TYPE_CODE", "ref-party-type"),
            Map.entry("CUSTOMER_STATUS_CODE", "ref-party-lifecycle-status"),
            Map.entry("PARTY_STATUS_CODE", "ref-party-lifecycle-status"),
            Map.entry("RISK_LEVEL_CODE", "ref-risk-level"),
            Map.entry("AML_RISK_MAX_CODE", "ref-risk-level"),
            Map.entry("MAX_AML_RISK_CODE", "ref-risk-level"),
            Map.entry("RESIDENCY_STATUS_CODE", "ref-residence-status")
    );

    private static final Map<String, String> PERSIAN_CODE_LABELS = Map.ofEntries(
            Map.entry("PERSON", "شخص حقیقی"), Map.entry("INDIVIDUAL", "شخص حقیقی"),
            Map.entry("ORGANIZATION", "شخص حقوقی / سازمان"), Map.entry("LEGAL_ENTITY", "شخص حقوقی"),
            Map.entry("GOVERNMENT", "نهاد دولتی"), Map.entry("BANK", "بانک / مؤسسه اعتباری"),
            Map.entry("STANDARD", "استاندارد"), Map.entry("VIP", "ارزنده / ویژه"),
            Map.entry("WELFARE_SUPPORT", "گروه حمایتی"), Map.entry("KNOWLEDGE_BASED", "دانش‌بنیان"),
            Map.entry("ACTIVE", "فعال"), Map.entry("INACTIVE", "غیرفعال"), Map.entry("MERGED", "ادغام‌شده"),
            Map.entry("SUSPENDED", "تعلیق‌شده"), Map.entry("BLOCKED", "مسدود"),
            Map.entry("OPEN", "مجاز"), Map.entry("CLOSED", "بسته"),
            Map.entry("DRAFT", "پیش‌نویس"), Map.entry("APPROVED", "تصویب‌شده"),
            Map.entry("CONFIGURED", "پیکربندی‌شده"), Map.entry("VALID", "معتبر"),
            Map.entry("READY", "آماده"), Map.entry("REJECTED", "ردشده"),
            Map.entry("BRANCH", "شعبه"), Map.entry("MOBILE", "همراه‌بانک"),
            Map.entry("INTERNET", "اینترنت‌بانک"), Map.entry("API", "رابط سرویس"),
            Map.entry("BACKOFFICE", "پشت‌صحنه عملیاتی"), Map.entry("SMS", "پیامک"),
            Map.entry("EMAIL", "پست الکترونیکی"), Map.entry("PHONE", "تلفن"),
            Map.entry("ALL_OPERATIONS", "همه عملیات مجاز"), Map.entry("TRANSFER_OUT", "انتقال وجه خروجی"),
            Map.entry("CASH_DEPOSIT", "واریز نقدی"), Map.entry("CASH_WITHDRAWAL", "برداشت نقدی"),
            Map.entry("TRANSFER", "انتقال وجه"), Map.entry("CHEQUE", "عملیات چک"),
            Map.entry("REGION", "منطقه"), Map.entry("DIGITAL_UNIT", "واحد دیجیتال"),
            Map.entry("IDENTITY", "هویت / مدرک هویتی"), Map.entry("NATIONAL_ID", "شناسه ملی"), Map.entry("PASSPORT", "گذرنامه"), Map.entry("LICENSE", "مجوز"), Map.entry("ARTICLES_OF_ASSOCIATION", "اساسنامه"), Map.entry("ADDRESS", "مدرک نشانی"),
            Map.entry("SIGNATURE", "نمونه / مدرک امضا"), Map.entry("BUSINESS_DOC", "مدرک کسب‌وکار"),
            Map.entry("APPLICATION", "ثبت درخواست"), Map.entry("BEFORE_OPENING", "پیش از افتتاح"),
            Map.entry("AFTER_OPENING", "پس از افتتاح"), Map.entry("AFTER_OPENING_CONDITIONAL", "پس از افتتاح مشروط"),
            Map.entry("PERIODIC_REVIEW", "بازبینی دوره‌ای"), Map.entry("BEFORE_TRANSACTION", "پیش از تراکنش"),
            Map.entry("SHAHAB", "شهاب"), Map.entry("MOBILE_OWNERSHIP", "مالکیت تلفن همراه"),
            Map.entry("PEP", "اشخاص سیاسی"), Map.entry("POLITICAL", "اشخاص سیاسی"),
            Map.entry("SANCTION", "تحریم"), Map.entry("SANCTIONS", "تحریم‌ها"), Map.entry("CREDIT_STATUS", "وضعیت اعتباری"), Map.entry("CREDIT_BUREAU", "اعتبارسنجی اعتباری"), Map.entry("TAX_STATUS", "وضعیت مالیاتی"),
            Map.entry("BASIC", "پایه"), Map.entry("ENHANCED", "تقویت‌شده"), Map.entry("FULL", "کامل"),
            Map.entry("LOW", "کم"), Map.entry("MEDIUM", "متوسط"), Map.entry("HIGH", "زیاد"), Map.entry("VERY_HIGH", "بسیار زیاد"),
            Map.entry("PROHIBITED", "غیرمجاز"), Map.entry("RESIDENT", "مقیم"),
            Map.entry("NON_RESIDENT", "غیرمقیم"), Map.entry("TEMPORARY", "اقامت موقت"),
            Map.entry("DOMESTIC", "داخلی"), Map.entry("FOREIGN", "خارجی"), Map.entry("ALL", "همه"),
            Map.entry("IRR", "ریال ایران"), Map.entry("USD", "دلار آمریکا"),
            Map.entry("EUR", "یورو"), Map.entry("AED", "درهم امارات"),
            Map.entry("PREREQUISITE", "پیش‌نیاز"), Map.entry("ALTERNATIVE", "جایگزین"),
            Map.entry("COMPLEMENTARY", "مکمل"), Map.entry("DEPENDENCY", "وابستگی"),
            Map.entry("REQUIRES", "پیش‌نیاز"), Map.entry("REPLACES", "جایگزین"),
            Map.entry("COMPLEMENTS", "مکمل"), Map.entry("DEPENDS_ON", "وابستگی"),
            Map.entry("PERCENTAGE", "درصدی"), Map.entry("FIXED_AMOUNT", "مبلغ ثابت"),
            Map.entry("TIERED", "پله‌ای"), Map.entry("FIXED", "ثابت"), Map.entry("VARIABLE", "متغیر"),
            Map.entry("PROFIT", "سود"), Map.entry("FEE", "کارمزد"), Map.entry("RATE", "نرخ"),
            Map.entry("AMOUNT", "مبلغ"), Map.entry("BALANCE", "مانده"), Map.entry("TERM", "مدت"),
            Map.entry("DAILY", "روزانه"), Map.entry("MONTHLY", "ماهانه"), Map.entry("QUARTERLY", "سه‌ماهه"),
            Map.entry("SEMI_ANNUAL", "شش‌ماهه"), Map.entry("ANNUAL", "سالانه"), Map.entry("MATURITY", "سررسید"),
            Map.entry("DAY", "روز"), Map.entry("MONTH", "ماه"), Map.entry("YEAR", "سال"),
            Map.entry("NEXT_BUSINESS_DAY", "روز کاری بعد"), Map.entry("PREVIOUS_BUSINESS_DAY", "روز کاری قبل"),
            Map.entry("NO_ADJUSTMENT", "بدون جابه‌جایی"), Map.entry("NONE", "بدون جابه‌جایی")
    );

    // PB-R15: bank-user form contract. These codes are defined by the reviewed Product Builder
    // HTML and are intersected with live Oracle CHECK choices, when a CHECK exists.
    // They are not reference-table surrogate IDs and must never be displayed as input text.
    private static final Map<String, List<SelectOption>> BUSINESS_FORM_OPTIONS = Map.ofEntries(
            Map.entry("PRODUCT_PRICING_RULE.PRICING_PURPOSE_CODE", optsLabeled(
                    "DEPOSIT_PROFIT", "سود سپرده", "LOAN_INTEREST", "سود / نرخ تسهیلات",
                    "COMMISSION", "کارمزد", "PENALTY", "وجه التزام / جریمه")),
            Map.entry("PRODUCT_PRICING_RULE.PRICING_METHOD_CODE", optsLabeled(
                    "FIXED", "ثابت", "FLOATING", "شناور", "TIERED", "پلکانی", "FORMULA", "فرمولی")),
            Map.entry("PRODUCT_PRICING_RULE.DAY_COUNT_BASIS_CODE", optsLabeled(
                    "ACT_365", "روز واقعی / سال ۳۶۵ روزه", "ACT_360", "روز واقعی / سال ۳۶۰ روزه", "30_360", "ماه ۳۰ روزه / سال ۳۶۰ روزه")),
            Map.entry("PRODUCT_PRICING_RULE.ACCRUAL_FREQUENCY_CODE", optsLabeled(
                    "DAILY", "روزانه", "MONTHLY", "ماهانه", "MATURITY", "در سررسید")),
            Map.entry("PRODUCT_PRICING_RULE.SETTLEMENT_FREQUENCY_CODE", optsLabeled(
                    "MONTHLY", "ماهانه", "QUARTERLY", "سه‌ماهه", "ANNUAL", "سالانه", "MATURITY", "در سررسید")),
            Map.entry("PRODUCT_PRICING_RULE.DESTINATION_RULE_CODE", optsLabeled(
                    "SINGLE_ACCOUNT", "یک حساب مقصد", "MULTIPLE_ACCOUNTS", "چند حساب مقصد",
                    "CUSTOMER_SELECTED", "انتخاب مقصد توسط مشتری", "SYSTEM_DEFINED", "تعیین مقصد توسط سامانه")),
            Map.entry("PRODUCT_PRICING_RULE.RULE_STATUS_CODE", optsLabeled(
                    "DRAFT", "پیش‌نویس", "ACTIVE", "فعال", "INACTIVE", "غیرفعال")),
            Map.entry("PRODUCT_RELATIONSHIP.RELATIONSHIP_TYPE_CODE", optsLabeled(
                    "REQUIRES", "محصول پیش‌نیاز", "BUNDLE", "بسته محصولات",
                    "RATE_DEPENDENCY", "وابستگی نرخ", "COLLATERAL_ACCOUNT", "حساب وثیقه")),
            Map.entry("PRODUCT_RELATIONSHIP.RECORD_STATUS_CODE", optsLabeled(
                    "ACTIVE", "فعال", "INACTIVE", "غیرفعال"))
    );

    static List<SelectOption> businessFormOptions(String table, String field, List<SelectOption> databaseChoices) {
        List<SelectOption> declared = BUSINESS_FORM_OPTIONS.getOrDefault(normalize(table) + "." + normalize(field), List.of());
        if (declared.isEmpty()) return List.of();
        if (databaseChoices == null || databaseChoices.isEmpty()) return declared;
        Set<String> allowed = new LinkedHashSet<>();
        for (SelectOption option : databaseChoices) allowed.add(normalize(option.code() == null ? option.value() : option.code()));
        return declared.stream().filter(option -> allowed.contains(normalize(option.code()))).toList();
    }

    private static final Map<String, List<SelectOption>> FALLBACKS = Map.ofEntries(
            Map.entry("CUSTOMER_TYPE_CODE", opts("PERSON", "ORGANIZATION", "GOVERNMENT", "BANK")),
            Map.entry("PARTY_TYPE_CODE", opts("PERSON", "ORGANIZATION", "GOVERNMENT", "BANK")),
            Map.entry("CUSTOMER_CATEGORY_CODE", opts("PERSON", "ORGANIZATION")),
            Map.entry("CUSTOMER_SEGMENT_CODE", optsLabeled("STANDARD", "مشتری عادی", "VIP", "ارزنده / ویژه", "WELFARE_SUPPORT", "گروه حمایتی", "KNOWLEDGE_BASED", "دانش‌بنیان")),
            Map.entry("CUSTOMER_STATUS_CODE", opts("ACTIVE", "INACTIVE", "SUSPENDED", "BLOCKED")),
            Map.entry("PARTY_STATUS_CODE", opts("ACTIVE", "INACTIVE", "SUSPENDED", "BLOCKED")),
            Map.entry("KYC_LEVEL_CODE", opts("BASIC", "STANDARD", "ENHANCED")),
            Map.entry("MIN_KYC_LEVEL_CODE", opts("BASIC", "STANDARD", "ENHANCED")),
            Map.entry("REQUIRED_KYC_LEVEL_CODE", opts("BASIC", "STANDARD", "ENHANCED")),
            Map.entry("AML_RISK_MAX_CODE", opts("LOW", "MEDIUM", "HIGH", "PROHIBITED")),
            Map.entry("MAX_AML_RISK_CODE", opts("LOW", "MEDIUM", "HIGH", "PROHIBITED")),
            Map.entry("RISK_LEVEL_CODE", opts("LOW", "MEDIUM", "HIGH", "PROHIBITED")),
            Map.entry("CHANNEL_CODE", opts("BRANCH", "MOBILE", "INTERNET", "API")),
            Map.entry("OPENING_CHANNEL_CODE", opts("BRANCH", "MOBILE", "INTERNET", "API")),
            Map.entry("OPERATION_CODE", opts("ALL_OPERATIONS", "TRANSFER_OUT", "CASH_WITHDRAWAL", "CHEQUE")),
            Map.entry("TRANSACTION_TYPE_CODE", opts("CASH_DEPOSIT", "CASH_WITHDRAWAL", "TRANSFER")),
            Map.entry("ORG_UNIT_TYPE_CODE", opts("BANK", "REGION", "BRANCH", "DIGITAL_UNIT")),
            Map.entry("DOCUMENT_TYPE_CODE", opts("IDENTITY", "ADDRESS", "SIGNATURE", "BUSINESS_DOC")),
            Map.entry("REQUIREMENT_STAGE_CODE", opts("APPLICATION", "BEFORE_OPENING", "AFTER_OPENING_CONDITIONAL", "PERIODIC_REVIEW", "BEFORE_TRANSACTION")),
            Map.entry("INQUIRY_TYPE_CODE", opts("IDENTITY", "SHAHAB", "MOBILE_OWNERSHIP", "PEP", "SANCTION", "CREDIT_STATUS")),
            Map.entry("RELATIONSHIP_TYPE_CODE", opts("PREREQUISITE", "ALTERNATIVE", "COMPLEMENTARY", "DEPENDENCY")),
            Map.entry("CURRENCY_CODE", opts("IRR", "USD", "EUR", "AED")),
            Map.entry("DEFAULT_CURRENCY_CODE", opts("IRR", "USD", "EUR", "AED")),
            Map.entry("SETTLEMENT_CURRENCY_CODE", opts("IRR", "USD", "EUR", "AED")),
            Map.entry("GENDER_CODE", optsLabeled("1", "مرد", "2", "زن", "4", "نامشخص")),
            Map.entry("CRITERION_TYPE_CODE", optsLabeled(
                    "CUSTOMER_TYPE", "نوع مشتری", "CUSTOMER_SEGMENT", "بخش مشتری",
                    "CUSTOMER_STATUS", "وضعیت مشتری", "GENDER", "جنسیت",
                    "RESIDENCY_STATUS", "وضعیت اقامت", "NATIONALITY_SCOPE", "دامنه تابعیت",
                    "CUSTOMER_TENURE", "حداقل سابقه مشتری نزد بانک", "ACCOUNT_TENURE", "حداقل سابقه حساب")),
            Map.entry("OPERATOR_CODE", optsLabeled("IN", "یکی از مقادیر", "EQ", "برابر", "MIN", "حداقل", "MAX", "حداکثر")),
            Map.entry("VALUE_UNIT_CODE", optsLabeled("DAY", "روز", "MONTH", "ماه", "YEAR", "سال")),
            Map.entry("RULE_STATUS_CODE", opts("DRAFT", "ACTIVE", "INACTIVE"))
    );

    private static final Map<String, String> ELIGIBILITY_COLUMN_BY_TYPE = Map.ofEntries(
            Map.entry("CUSTOMER_TYPE", "CUSTOMER_TYPE_CODE"),
            Map.entry("CUSTOMER_SEGMENT", "CUSTOMER_SEGMENT_CODE"),
            Map.entry("CUSTOMER_STATUS", "CUSTOMER_STATUS_CODE"),
            Map.entry("GENDER", "GENDER_CODE"),
            Map.entry("RESIDENCY_STATUS", "RESIDENCY_STATUS_CODE"),
            Map.entry("NATIONALITY_SCOPE", "NATIONALITY_SCOPE_CODE")
    );

    private final ReferenceService referenceService;
    private final PartyReferenceService partyReferenceService;
    private final PdlDormancyFeePlanReference feePlanReference;

    public PdlReferenceOptionService(ReferenceService referenceService, PartyReferenceService partyReferenceService,
                                     PdlDormancyFeePlanReference feePlanReference) {
        this.referenceService = referenceService;
        this.partyReferenceService = partyReferenceService;
        this.feePlanReference = feePlanReference;
    }

    public List<SelectOption> eligibilityCriterionOptions(String criterionType) {
        String type = normalize(criterionType);
        String column = ELIGIBILITY_COLUMN_BY_TYPE.get(type);
        if (column == null) return List.of();
        String resource = RESOURCE_BY_COLUMN.get(column);
        String partyResource = PARTY_RESOURCE_BY_COLUMN.get(column);
        List<SelectOption> governed = partyResource != null
                ? partyReferenceOptions(partyResource)
                : referenceOptions(resource);
        return mergeOptions(List.of(), governed, FALLBACKS.getOrDefault(column, List.of()));
    }

    public void validateEligibilityCriterion(Map<String, Object> values) {
        String type = normalize(values.get("CRITERION_TYPE_CODE"));
        String operator = normalize(values.get("OPERATOR_CODE"));
        if (type.isBlank()) throw new ProductBuilderValidationException("نوع معیار اهلیت الزامی است.");
        if (operator.isBlank()) operator = "IN";

        if (Set.of("CUSTOMER_TENURE", "ACCOUNT_TENURE").contains(type)) {
            if (!"MIN".equals(operator)) {
                throw new ProductBuilderValidationException("معیار سابقه باید با عملگر حداقل ثبت شود.");
            }
            Object number = values.get("CRITERION_VALUE_NUMBER");
            if (number == null || number.toString().isBlank()) {
                throw new ProductBuilderValidationException("مقدار سابقه الزامی است.");
            }
            try {
                if (new BigDecimal(number.toString()).signum() < 0) {
                    throw new ProductBuilderValidationException("مقدار سابقه نمی‌تواند منفی باشد.");
                }
            } catch (NumberFormatException ex) {
                throw new ProductBuilderValidationException("مقدار سابقه باید عددی باشد.");
            }
            String unit = normalize(values.get("VALUE_UNIT_CODE"));
            if (!Set.of("DAY", "MONTH", "YEAR").contains(unit)) {
                throw new ProductBuilderValidationException("واحد سابقه باید روز، ماه یا سال باشد.");
            }
            return;
        }

        if (!Set.of("IN", "EQ").contains(operator)) {
            throw new ProductBuilderValidationException("عملگر معیار دامنه‌ای باید IN یا EQ باشد.");
        }
        String code = values.get("CRITERION_VALUE_CODE") == null ? "" : values.get("CRITERION_VALUE_CODE").toString().trim();
        if (code.isBlank()) throw new ProductBuilderValidationException("مقدار معیار اهلیت الزامی است.");
        List<SelectOption> options = eligibilityCriterionOptions(type);
        if (options.isEmpty() || options.stream().noneMatch(o -> same(code, o.value()) || same(code, o.code()))) {
            throw new ProductBuilderValidationException("مقدار معیار اهلیت باید از داده مرجع معتبر انتخاب شود.");
        }
    }

    public TableDescriptor enrich(TableDescriptor descriptor) {
        List<ColumnDescriptor> columns = descriptor.columns().stream()
                .map(column -> enrichColumn(descriptor.tableName(), column))
                .toList();
        return new TableDescriptor(
                descriptor.schemaName(), descriptor.tableName(), descriptor.title(),
                descriptor.packageCode(), descriptor.packageTitle(), descriptor.primaryKeyColumn(), columns
        );
    }

    public boolean isCommonRuleTable(String table) {
        return COMMON_RULE_TABLES.contains(normalize(table));
    }

    public void validateChangedValues(TableDescriptor descriptor, Map<String, Object> values, Map<String, Object> existing) {
        for (ColumnDescriptor column : descriptor.columns()) {
            if (!column.referenceControlled() || !values.containsKey(column.name())) continue;
            Object raw = values.get(column.name());
            if (raw == null || raw.toString().isBlank()) continue;
            if (existing != null && same(raw, existing.get(column.name()))) continue;
            String requested = raw.toString().trim();
            boolean valid = column.options().stream().anyMatch(option ->
                    same(requested, option.value()) || same(requested, option.code()));
            if (!valid) {
                throw new ProductBuilderValidationException(
                        "مقدار «" + column.label() + "» باید از فهرست مرجع انتخاب شود."
                );
            }
        }
    }


    private ColumnDescriptor enrichColumn(String table, ColumnDescriptor column) {
        String name = normalize(column.name());
        String businessField = normalize(table) + "." + name;
        if (BUSINESS_FORM_OPTIONS.containsKey(businessField)) {
            List<SelectOption> choices = businessFormOptions(table, name, column.options());
            return copy(column, true, "PDL_FORM_CONTRACT", choices);
        }
        String resource = RESOURCE_BY_COLUMN.get(name);
        // STATUS_CODE is ambiguous globally: only the closure rule represents a rule status.
        if ("DEPOSIT_PRODUCT_CLOSURE_RULE".equals(normalize(table)) && "STATUS_CODE".equals(name)) {
            resource = "dps-rule-statuses";
        }
        String partyResource = PARTY_RESOURCE_BY_COLUMN.get(name);
        boolean depositField = GOVERNED_DEPOSIT_TABLES.contains(normalize(table)) && resource != null;
        boolean feePlanField = "DEPOSIT_PRODUCT_DORMANCY_RULE".equals(normalize(table))
                && "MAINTENANCE_FEE_PLAN_ID".equals(name);
        boolean checkedCode = isCommonRuleTable(table) && name.endsWith("_CODE") && !column.options().isEmpty();
        boolean controlled = resource != null || partyResource != null || checkedCode || FALLBACKS.containsKey(name) || feePlanField;
        if (!controlled) return column;

        // Do not invent DPS reference entries. Closure and hold fields can use actual
        // Oracle PDL CHECK options when the corresponding DPS reference is unpopulated.
        if (feePlanField) {
            return copy(column, true, "FEE:ACTIVE_DORMANCY_FEE_VERSIONS", feePlanReference.options());
        }
        List<SelectOption> governed = partyResource != null ? partyReferenceOptions(partyResource) : referenceOptions(resource);
        if (depositField) {
            String fieldKey = normalize(table) + "." + name;
            if ("DEPOSIT_PRODUCT_JOINT_RULE".equals(normalize(table))) {
                // Resolve numeric DPS codes to their corresponding PDL semantic CHECK values.
                // Persist the exact code accepted by the live PDL CHECK, not a guessed code.
                return copy(column, true, "DPS_MAPPED:" + resource,
                        mapJointOptions(name, column.options(), governed));
            }
            List<SelectOption> compatible = intersection(column.options(), governed.stream()
                    .filter(option -> !"عنوان فارسی مرجع تعریف نشده".equals(option.label()))
                    .toList());
            if (CHECK_BACKED_FIELDS.contains(fieldKey) && !column.options().isEmpty()) {
                // Several DPS reference tables have zero seeds in the supplied baseline.
                // Fall back to actual Oracle CHECK values, never to mock HTML values alone.
                return copy(column, true, compatible.isEmpty() ? "PDL_CHECK_CONSTRAINT" : "DPS:" + resource,
                        compatible.isEmpty() ? checkedDepositOptions(column.options()) : compatible);
            }
            return copy(column, true, "DPS:" + resource, compatible);
        }
        List<SelectOption> options = mergeOptions(column.options(), governed, FALLBACKS.getOrDefault(name, List.of()));
        if (options.isEmpty() && checkedCode) options = localize(column.options());
        String source = partyResource != null ? "CIF:" + partyResource
                : resource != null ? "REF:" + resource
                : checkedCode ? "PDL_CHECK_CONSTRAINT" : "PDL_COMMON_RULE";
        return copy(column, true, source, options);
    }

    static List<SelectOption> mapJointOptions(String columnName, List<SelectOption> constraints,
                                               List<SelectOption> governed) {
        // The model-approved form contract is the fallback for incomplete DPS seeds;
        // database CHECK codes, when supplied, always restrict the selectable domain.
        List<SelectOption> declared = JOINT_FORM_CONTRACT.getOrDefault(columnName, List.of());
        if (declared.isEmpty()) return List.of();
        boolean hasCheck = constraints != null && !constraints.isEmpty();
        Map<String, String> translations = JOINT_CODE_TRANSLATIONS.getOrDefault(columnName, Map.of());
        Set<String> allowed = new LinkedHashSet<>();
        if (hasCheck) for (SelectOption check : constraints)
            allowed.add(normalize(check.code() == null ? check.value() : check.code()));
        else for (SelectOption choice : declared) allowed.add(normalize(choice.code()));
        LinkedHashMap<String, SelectOption> mapped = new LinkedHashMap<>();
        for (SelectOption reference : governed) {
            String incoming = normalize(reference.code() == null ? reference.value() : reference.code());
            String semantic = translations.getOrDefault(incoming, incoming);
            String persistedCode = allowed.contains(semantic) ? semantic :
                    allowed.contains(incoming) ? incoming : null;
            if (persistedCode == null) continue;
            if ("عنوان فارسی مرجع تعریف نشده".equals(reference.label())) continue;
            mapped.putIfAbsent(persistedCode, new SelectOption(persistedCode, persistedCode, reference.label()));
        }
        // If the reference is empty or entirely incompatible, use the reviewed
        // semantic form contract. Where CHECK exists, it is still the authority.
        if (mapped.isEmpty()) {
            for (SelectOption choice : declared) {
                String semantic = normalize(choice.code());
                if (allowed.contains(semantic)) mapped.putIfAbsent(semantic, choice);
            }
            // Some legacy PDL installations CHECK numeric codes rather than semantics.
            // Only actual CHECK-permitted numbers are eligible; labels stay Persian.
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                String numeric = entry.getKey();
                String semantic = entry.getValue();
                if (!allowed.contains(numeric)) continue;
                for (SelectOption choice : declared) {
                    if (semantic.equals(normalize(choice.code()))) {
                        mapped.putIfAbsent(numeric, new SelectOption(numeric, numeric, choice.label()));
                        break;
                    }
                }
            }
        }
        return List.copyOf(mapped.values());
    }

    private static List<SelectOption> checkedDepositOptions(List<SelectOption> constraints) {
        LinkedHashMap<String, SelectOption> result = new LinkedHashMap<>();
        for (SelectOption check : constraints) {
            String code = normalize(check.code() == null ? check.value() : check.code());
            String label = PDL_DEPOSIT_CHECK_LABELS.get(code);
            if (label != null) result.putIfAbsent(code, new SelectOption(code, code, label));
        }
        return List.copyOf(result.values());
    }

    private static List<SelectOption> intersection(List<SelectOption> constraints, List<SelectOption> governed) {
        if (constraints == null || constraints.isEmpty()) return governed;
        Set<String> allowed = new LinkedHashSet<>();
        for (SelectOption option : constraints) allowed.add(normalize(option.code() == null ? option.value() : option.code()));
        return governed.stream().filter(option -> allowed.contains(normalize(option.code()))).toList();
    }

    private List<SelectOption> partyReferenceOptions(String resource) {
        try {
            return partyReferenceService.lookup(resource, null, 1000).stream()
                    .map(option -> new SelectOption(option.code(), option.code(), localizedLabel(option.code(), option.label())))
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private List<SelectOption> referenceOptions(String resource) {
        if (resource == null) return List.of();
        try {
            return referenceService.lookup(resource, null, null, 1000).stream()
                    .map(option -> new SelectOption(option.code(), option.code(), localizedLabel(option.code(), option.label())))
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static List<SelectOption> mergeOptions(List<SelectOption> checks, List<SelectOption> references, List<SelectOption> fallbacks) {
        LinkedHashMap<String, SelectOption> governed = new LinkedHashMap<>();
        for (SelectOption option : references) put(governed, option);

        if (!checks.isEmpty()) {
            LinkedHashMap<String, SelectOption> candidates = new LinkedHashMap<>(governed);
            for (SelectOption option : fallbacks) put(candidates, option);
            LinkedHashSet<String> allowed = new LinkedHashSet<>();
            checks.forEach(option -> allowed.add(normalize(option.code() == null ? option.value() : option.code())));
            LinkedHashMap<String, SelectOption> constrained = new LinkedHashMap<>();
            for (String code : allowed) {
                SelectOption hit = candidates.get(code);
                constrained.put(code, hit != null ? hit : localized(checks.stream()
                        .filter(option -> code.equals(normalize(option.code() == null ? option.value() : option.code())))
                        .findFirst().orElse(new SelectOption(code, code, code))));
            }
            return List.copyOf(constrained.values());
        }

        if (!governed.isEmpty()) return List.copyOf(governed.values());
        LinkedHashMap<String, SelectOption> safeFallback = new LinkedHashMap<>();
        for (SelectOption option : fallbacks) put(safeFallback, option);
        return List.copyOf(safeFallback.values());
    }

    private static List<SelectOption> localize(List<SelectOption> source) {
        return source.stream().map(PdlReferenceOptionService::localized).toList();
    }

    private static SelectOption localized(SelectOption option) {
        String code = option.code() == null || option.code().isBlank() ? String.valueOf(option.value()) : option.code();
        return new SelectOption(option.value(), code, localizedLabel(code, option.label()));
    }

    private static String localizedLabel(String code, String label) {
        String translated = PERSIAN_CODE_LABELS.get(normalize(code));
        if (translated != null) return translated;
        if (containsPersian(label)) return label.trim();
        return "عنوان فارسی مرجع تعریف نشده";
    }

    private static void put(Map<String, SelectOption> target, SelectOption source) {
        SelectOption option = localized(source);
        String key = normalize(option.code() == null ? option.value() : option.code());
        if (!key.isBlank()) target.putIfAbsent(key, option);
    }

    private static List<SelectOption> optsLabeled(String... codeAndLabel) {
        List<SelectOption> result = new ArrayList<>();
        for (int i = 0; i + 1 < codeAndLabel.length; i += 2) {
            result.add(new SelectOption(codeAndLabel[i], codeAndLabel[i], codeAndLabel[i + 1]));
        }
        return List.copyOf(result);
    }

    private static List<SelectOption> opts(String... codes) {
        List<SelectOption> result = new ArrayList<>();
        for (String code : codes) result.add(new SelectOption(code, code, localizedLabel(code, code)));
        return List.copyOf(result);
    }

    private static ColumnDescriptor copy(ColumnDescriptor c, boolean controlled, String source, List<SelectOption> options) {
        return new ColumnDescriptor(
                c.name(), c.label(), c.dataType(), c.length(), c.precision(), c.scale(), c.nullable(),
                c.primaryKey(), c.foreignKey(), c.parentTable(), c.parentColumn(), c.readOnly(), c.defaultValue(),
                options, controlled, source
        );
    }

    private static boolean containsPersian(String value) {
        if (value == null) return false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch >= '\u0600' && ch <= '\u06ff') || (ch >= '\u0750' && ch <= '\u077f')) return true;
        }
        return false;
    }

    private static boolean same(Object left, Object right) {
        return left != null && right != null && left.toString().trim().equalsIgnoreCase(right.toString().trim());
    }

    private static String normalize(Object value) {
        return value == null ? "" : value.toString().trim().toUpperCase(Locale.ROOT);
    }
}
