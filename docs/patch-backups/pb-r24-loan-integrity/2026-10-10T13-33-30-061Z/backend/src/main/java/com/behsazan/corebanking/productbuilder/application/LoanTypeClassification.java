package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * PB-R23: fixed business taxonomy, not an Oracle reference-data surrogate or a CBI code.
 * Numeric codes fit the existing NUMBER(1) columns of PDL.LOAN_TYPE.
 * Changing these codes later requires a separately approved data migration.
 */
public final class LoanTypeClassification {
    private LoanTypeClassification() {}

    public enum LoanGroup {
        QARD_HASAN(1, "قرض‌الحسنه"),
        EXCHANGE(2, "مبادله‌ای"),
        PARTICIPATORY(3, "مشارکتی"),
        COMMITMENT(4, "تعهدات");

        private final int code;
        private final String title;
        LoanGroup(int code, String title) { this.code = code; this.title = title; }
        public int code() { return code; }
        public String title() { return title; }
    }

    public enum ObligationType {
        NOT_APPLICABLE(0, "فاقد کاربرد (غیرتعهدی)"),
        LETTER_OF_CREDIT(1, "اعتبارات اسنادی"),
        BANK_GUARANTEE(2, "ضمانت‌نامه");

        private final int code;
        private final String title;
        ObligationType(int code, String title) { this.code = code; this.title = title; }
        public int code() { return code; }
        public String title() { return title; }
    }

    /** Reserved only as an interim unconfigured value; no interest calculation is enabled. */
    public static final int INTEREST_NOT_CONFIGURED = 0;

    public static List<SelectOption> loanGroupOptions() {
        return Arrays.stream(LoanGroup.values())
                .map(value -> new SelectOption(value.code(), Integer.toString(value.code()), value.title()))
                .toList();
    }

    public static List<SelectOption> obligationOptions() {
        return Arrays.stream(ObligationType.values())
                .map(value -> new SelectOption(value.code(), Integer.toString(value.code()), value.title()))
                .toList();
    }

    public static List<SelectOption> disabledInterestOptions() {
        return List.of(new SelectOption(INTEREST_NOT_CONFIGURED, "0", "غیرفعال ـ هنوز پیکربندی نشده"));
    }

    /**
     * Validates both create and update after merging update fields with the persisted row.
     * On create only, interest method is system-filled as 0 to satisfy the existing NOT NULL.
     * Existing nonzero values remain untouched and cannot be modified via this paused feature.
     */
    public static void validateForSave(Map<String, Object> values, Map<String, Object> previous) {
        int group = number(values.get("LOAN_GROUP"), "گروه تسهیلات");
        int obligation = number(values.get("OBLIGATION_TYPE"), "نوع تعهد");
        boolean groupAllowed = Arrays.stream(LoanGroup.values()).anyMatch(g -> g.code() == group);
        if (!groupAllowed) throw new ProductBuilderValidationException("گروه تسهیلات باید از چهار گروه تعریف‌شده انتخاب شود.");
        if (group == LoanGroup.COMMITMENT.code()) {
            if (obligation != ObligationType.LETTER_OF_CREDIT.code() && obligation != ObligationType.BANK_GUARANTEE.code()) {
                throw new ProductBuilderValidationException("برای گروه تعهدات، نوع تعهد باید اعتبارات اسنادی یا ضمانت‌نامه باشد.");
            }
        } else if (obligation != ObligationType.NOT_APPLICABLE.code()) {
            throw new ProductBuilderValidationException("نوع تعهد برای گروه غیرتعهدی باید «فاقد کاربرد» باشد.");
        }

        if (previous == null) {
            Object entered = values.get("INTEREST_METHOD");
            if (entered != null && !entered.toString().isBlank() && number(entered, "روش محاسبه سود") != INTEREST_NOT_CONFIGURED) {
                throw new ProductBuilderValidationException("روش محاسبه سود فعلاً غیرفعال است و قابل انتخاب نیست.");
            }
            values.put("INTEREST_METHOD", INTEREST_NOT_CONFIGURED);
        } else if (values.containsKey("INTEREST_METHOD") && !sameInteger(values.get("INTEREST_METHOD"), previous.get("INTEREST_METHOD"))) {
            throw new ProductBuilderValidationException("روش محاسبه سود فعلاً غیرفعال است؛ مقدار قبلی نباید تغییر کند.");
        }
    }

    private static boolean sameInteger(Object left, Object right) {
        try { return number(left, "روش محاسبه سود") == number(right, "روش محاسبه سود"); }
        catch (ProductBuilderValidationException ex) { return false; }
    }

    private static int number(Object raw, String label) {
        if (raw == null || raw.toString().isBlank())
            throw new ProductBuilderValidationException("«" + label + "» الزامی است.");
        try { return new BigDecimal(raw.toString().trim()).intValueExact(); }
        catch (NumberFormatException | ArithmeticException ex) {
            throw new ProductBuilderValidationException("کد «" + label + "» باید عدد صحیح معتبر باشد.");
        }
    }
}
