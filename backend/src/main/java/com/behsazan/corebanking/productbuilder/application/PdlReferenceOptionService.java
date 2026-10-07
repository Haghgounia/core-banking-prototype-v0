package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.ColumnDescriptor;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TableDescriptor;
import com.behsazan.corebanking.referencedata.management.application.ReferenceService;
import com.behsazan.corebanking.cif.reference.application.PartyReferenceService;
import org.springframework.stereotype.Service;

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
            Map.entry("INQUIRY_TYPE_CODE", "dps-inquiry-types")
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
            Map.entry("RULE_STATUS_CODE", opts("DRAFT", "ACTIVE", "INACTIVE"))
    );

    private final ReferenceService referenceService;
    private final PartyReferenceService partyReferenceService;

    public PdlReferenceOptionService(ReferenceService referenceService, PartyReferenceService partyReferenceService) {
        this.referenceService = referenceService;
        this.partyReferenceService = partyReferenceService;
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
        String resource = RESOURCE_BY_COLUMN.get(name);
        String partyResource = PARTY_RESOURCE_BY_COLUMN.get(name);
        boolean checkedCode = isCommonRuleTable(table) && name.endsWith("_CODE") && !column.options().isEmpty();
        boolean controlled = resource != null || partyResource != null || checkedCode || FALLBACKS.containsKey(name);
        if (!controlled) return column;

        List<SelectOption> governed = partyResource != null ? partyReferenceOptions(partyResource) : referenceOptions(resource);
        List<SelectOption> options = mergeOptions(column.options(), governed, FALLBACKS.getOrDefault(name, List.of()));
        if (options.isEmpty() && checkedCode) options = localize(column.options());
        String source = partyResource != null ? "CIF:" + partyResource
                : resource != null ? "REF:" + resource
                : checkedCode ? "PDL_CHECK_CONSTRAINT" : "PDL_COMMON_RULE";
        return copy(column, true, source, options);
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
