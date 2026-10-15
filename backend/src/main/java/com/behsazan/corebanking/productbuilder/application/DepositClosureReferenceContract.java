package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Reviewed deposit-closure form business-code vocabulary. No Oracle access or writes. */
final class DepositClosureReferenceContract {
    private DepositClosureReferenceContract() {}

    /** R30-HF4: canonical, reviewed PDL form vocabulary from
     * docs/reference/Unified_Product_Builder_Interactive_Forms_FA.html.
     * These are PDL business codes, not numeric DPS reference surrogate values.
     * An Oracle CHECK, if present, remains authoritative and limits this list.
     */
    private static final Map<String, List<SelectOption>> CLOSURE_FORM_CONTRACT = Map.ofEntries(
            Map.entry("DEPOSIT_PRODUCT_CLOSURE_RULE.CLOSURE_TYPE_CODE", optsLabeled(
                    "NORMAL", "عادی", "EARLY", "پیش از موعد", "FORCED", "اجباری", "PARTIAL", "جزئی")),
            Map.entry("DEPOSIT_PRODUCT_CLOSURE_RULE.BALANCE_DESTINATION_CODE", optsLabeled(
                    "CUSTOMER_ACCOUNT", "انتقال به حساب مشتری", "CASH", "پرداخت نقدی طبق ضوابط",
                    "SUSPENSE", "انتقال به حساب واسط / معلق", "CUSTOMER_SELECTED", "انتخاب مقصد توسط مشتری")),
            Map.entry("DEPOSIT_PRODUCT_CLOSURE_RULE.STATUS_CODE", optsLabeled(
                    "DRAFT", "پیش‌نویس", "ACTIVE", "فعال", "INACTIVE", "غیرفعال")),
            Map.entry("DEPOSIT_PRODUCT_CLOSURE_PRECHECK.CHECK_TYPE_CODE", optsLabeled(
                    "ACTIVE_HOLD", "وجود مسدودی فعال", "PENDING_CHEQUE", "وجود چک در جریان",
                    "OUTSTANDING_DEBT", "وجود بدهی تسویه‌نشده", "PENDING_TRANSACTION", "وجود تراکنش در انتظار")),
            Map.entry("DEPOSIT_PRODUCT_CLOSURE_PRECHECK.FAILURE_ACTION_CODE", optsLabeled(
                    "BLOCK", "جلوگیری از بستن حساب", "REQUIRE_APPROVAL", "نیاز به تأیید مقام مجاز",
                    "WARN", "نمایش هشدار و ادامه با مجوز")),
            Map.entry("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE.SETTLEMENT_COMPONENT_CODE", optsLabeled(
                    "PRINCIPAL", "اصل مانده", "PROFIT", "سود", "COMMISSION", "کارمزد",
                    "TAX", "مالیات", "PENALTY", "وجه التزام / جریمه")),
            Map.entry("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE.SETTLEMENT_METHOD_CODE", optsLabeled(
                    "PAY", "پرداخت به مشتری", "DEDUCT", "کسر از مانده",
                    "TRANSFER", "انتقال به حساب مقصد", "WAIVE", "بخشودگی طبق مجوز")),
            Map.entry("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE.DESTINATION_CODE", optsLabeled(
                    "CUSTOMER_ACCOUNT", "حساب مشتری", "CASH", "پرداخت نقدی",
                    "BANK_ACCOUNT", "حساب داخلی بانک", "CUSTOMER_SELECTED", "مقصد انتخابی مشتری")),
            Map.entry("DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE.APPROVAL_LEVEL_CODE", optsLabeled(
                    "TELLER", "کاربر شعبه", "BRANCH_MANAGER", "رئیس شعبه",
                    "REGION_MANAGER", "مدیریت منطقه", "HEAD_OFFICE", "ستاد"))
    );

    /** A PDL CHECK is more authoritative than the HTML form vocabulary.
     * Unknown CHECK codes must not be guessed or auto-translated to unrelated DPS codes.
     * Exact matching DPS codes with meaningful Persian labels are allowed if CHECK-backed.
     */
    static boolean supports(String table, String field) {
        return CLOSURE_FORM_CONTRACT.containsKey(normalize(table) + "." + normalize(field));
    }

    static List<SelectOption> closureFormOptions(String table, String field,
                                                  List<SelectOption> oracleChecks, List<SelectOption> dpsChoices) {
        List<SelectOption> declared = CLOSURE_FORM_CONTRACT.getOrDefault(normalize(table) + "." + normalize(field), List.of());
        if (declared.isEmpty()) return List.of();
        if (oracleChecks == null || oracleChecks.isEmpty()) return declared;
        Map<String, SelectOption> candidates = new LinkedHashMap<>();
        for (SelectOption option : declared) candidates.put(normalize(option.code()), option);
        if (dpsChoices != null) for (SelectOption option : dpsChoices) {
            if (containsPersian(option.label()) && !option.label().contains("عنوان فارسی مرجع تعریف نشده"))
                candidates.putIfAbsent(normalize(option.code()), option);
        }
        List<SelectOption> limited = new ArrayList<>();
        for (SelectOption check : oracleChecks) {
            String code = normalize(check.code() == null ? check.value() : check.code());
            SelectOption candidate = candidates.get(code);
            if (candidate != null) limited.add(new SelectOption(check.value(), code, candidate.label()));
        }
        return List.copyOf(limited);
    }

    private static String normalize(Object value) {
        return value == null ? "" : value.toString().trim().toUpperCase(Locale.ROOT);
    }
    private static boolean containsPersian(String value) {
        if (value == null) return false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch >= '\u0600' && ch <= '\u06ff') || (ch >= '\u0750' && ch <= '\u077f')) return true;
        }
        return false;
    }
    private static List<SelectOption> optsLabeled(String... codeAndLabel) {
        List<SelectOption> options = new ArrayList<>();
        for (int i = 0; i + 1 < codeAndLabel.length; i += 2)
            options.add(new SelectOption(codeAndLabel[i], codeAndLabel[i], codeAndLabel[i + 1]));
        return List.copyOf(options);
    }
}
