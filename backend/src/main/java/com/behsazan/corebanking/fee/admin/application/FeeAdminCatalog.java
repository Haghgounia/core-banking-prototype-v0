package com.behsazan.corebanking.fee.admin.application;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FeeAdminCatalog {
    private FeeAdminCatalog() {}

    public record Entry(String tableName, String title, String groupCode, String groupTitle, int baselineRows, boolean editable) {}

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        register("FEE_REF_DOMAIN", "دامنه اطلاعات پایه کارمزد", "01", "اطلاعات پایه کارمزد", 52, true);
        register("FEE_REF_VALUE", "مقادیر اطلاعات پایه کارمزد", "01", "اطلاعات پایه کارمزد", 307, true);
        register("FEE_DEMO_PARTY", "طرف آزمایشی کارمزد", "02", "داده‌های آزمایشی مستقل", 7, true);
        register("FEE_DEMO_PRODUCT", "محصول آزمایشی کارمزد", "02", "داده‌های آزمایشی مستقل", 4, true);
        register("FEE_DEMO_PRODUCT_FEATURE", "ویژگی آزمایشی محصول", "02", "داده‌های آزمایشی مستقل", 7, true);
        register("FEE_DEMO_ACCOUNT", "حساب آزمایشی کارمزد", "02", "داده‌های آزمایشی مستقل", 6, true);
        register("FEE_DEMO_FX_RATE", "نرخ ارز آزمایشی کارمزد", "02", "داده‌های آزمایشی مستقل", 3, true);
        register("FEE_FEATURE", "ویژگی کارمزد", "03", "سیاست و مقررات", 8, true);
        register("FEE_REGULATORY_SOURCE", "منبع مقرراتی کارمزد", "03", "سیاست و مقررات", 1, true);
        register("FEE_POLICY_SET", "مجموعه سیاست کارمزد", "03", "سیاست و مقررات", 2, true);
        register("FEE_POLICY_VERSION", "نسخه سیاست کارمزد", "03", "سیاست و مقررات", 2, true);
        register("FEE_REGULATORY_DISCOUNT_LIMIT", "سقف مقرراتی تخفیف کارمزد", "03", "سیاست و مقررات", 3, true);
        register("FEE_DEFINITION", "تعریف کارمزد", "04", "تعریف و پیکربندی کارمزد", 8, true);
        register("FEE_DEFINITION_VERSION", "نسخه تعریف کارمزد", "04", "تعریف و پیکربندی کارمزد", 8, true);
        register("FEE_PRODUCT_FEATURE", "ویژگی کارمزدی محصول", "04", "تعریف و پیکربندی کارمزد", 7, true);
        register("FEE_APPLICABILITY_RULE", "قاعده شرایط اعمال کارمزد", "04", "تعریف و پیکربندی کارمزد", 9, true);
        register("FEE_APPLICABILITY_CONDITION", "شرط قاعده اعمال کارمزد", "04", "تعریف و پیکربندی کارمزد", 11, true);
        register("FEE_CALCULATION_RULE", "نوع و قاعده محاسبه کارمزد", "04", "تعریف و پیکربندی کارمزد", 12, true);
        register("FEE_INPUT_DEFINITION", "تعریف ورودی محاسبه کارمزد", "04", "تعریف و پیکربندی کارمزد", 17, true);
        register("FEE_CALCULATION_TIER", "پله محاسبه کارمزد", "04", "تعریف و پیکربندی کارمزد", 5, true);
        register("FEE_RULE_COMPONENT", "جزء ترکیبی قاعده محاسبه", "04", "تعریف و پیکربندی کارمزد", 5, true);
        register("FEE_CHARGE_COMPONENT", "مؤلفه مبلغ کارمزد", "04", "تعریف و پیکربندی کارمزد", 11, true);
        register("FEE_CHARGE_STAGE", "مرحله اخذ کارمزد", "04", "تعریف و پیکربندی کارمزد", 2, true);
        register("FEE_TIMING_RULE", "قاعده زمان‌بندی کارمزد", "04", "تعریف و پیکربندی کارمزد", 8, true);
        register("FEE_CURRENCY_RULE", "قاعده ارزی کارمزد", "04", "تعریف و پیکربندی کارمزد", 8, true);
        register("FEE_COLLECTION_RULE", "قاعده وصول کارمزد", "04", "تعریف و پیکربندی کارمزد", 8, true);
        register("FEE_POSTING_RULE", "قاعده ثبت حسابداری کارمزد", "04", "تعریف و پیکربندی کارمزد", 8, true);
        register("FEE_ADJUSTMENT_POLICY", "سیاست تخفیف و تعدیل کارمزد", "04", "تعریف و پیکربندی کارمزد", 2, true);
        register("FEE_ALLOCATION_POLICY", "سیاست تسهیم کارمزد", "04", "تعریف و پیکربندی کارمزد", 1, true);
        register("FEE_ALLOCATION_RULE", "قاعده تسهیم کارمزد", "04", "تعریف و پیکربندی کارمزد", 3, true);
        register("FEE_ARRANGEMENT", "ترتیب یا توافق کارمزد", "05", "توافق و Arrangement کارمزد", 2, true);
        register("FEE_ARRANGEMENT_INVOLVEMENT", "نقش طرف در ترتیب کارمزد", "05", "توافق و Arrangement کارمزد", 5, true);
        register("FEE_ARRANGEMENT_ACCOUNT", "حساب مرتبط با ترتیب کارمزد", "05", "توافق و Arrangement کارمزد", 4, true);
        register("FEE_ARRANGEMENT_MODALITY", "مدالیتی ترتیب کارمزد", "05", "توافق و Arrangement کارمزد", 5, true);
        register("FEE_ARRANGEMENT_CALC_TERM", "شرط محاسباتی اختصاصی ترتیب", "05", "توافق و Arrangement کارمزد", 1, true);
        register("FEE_INSTRUCTION", "دستور کارمزد", "06", "اجرا، تراکنش و ممیزی", 1, false);
        register("FEE_TRANSACTION", "تراکنش کارمزد", "06", "اجرا، تراکنش و ممیزی", 1, false);
        register("FEE_TRANSACTION_COMPONENT", "جزء تراکنش کارمزد", "06", "اجرا، تراکنش و ممیزی", 1, false);
        register("FEE_CALCULATION_SNAPSHOT", "تصویر محاسبه کارمزد", "06", "اجرا، تراکنش و ممیزی", 1, false);
        register("FEE_APPLIED_ADJUSTMENT", "تعدیل اعمال‌شده کارمزد", "06", "اجرا، تراکنش و ممیزی", 1, false);
        register("FEE_ALLOCATION_RESULT", "نتیجه تسهیم کارمزد", "06", "اجرا، تراکنش و ممیزی", 3, false);
        register("FEE_OVERRIDE_REQUEST", "درخواست Override کارمزد", "06", "اجرا، تراکنش و ممیزی", 1, false);
        register("FEE_REVERSAL", "برگشت کارمزد", "06", "اجرا، تراکنش و ممیزی", 1, false);
        register("FEE_POSTING_REFERENCE", "مرجع ثبت حسابداری کارمزد", "06", "اجرا، تراکنش و ممیزی", 2, false);
        register("FEE_SETTLEMENT_REFERENCE", "مرجع تسویه کارمزد", "06", "اجرا، تراکنش و ممیزی", 3, false);
        register("FEE_DECISION_TRACE", "ردپای تصمیم کارمزد", "06", "اجرا، تراکنش و ممیزی", 4, false);
        register("FEE_AUDIT_EVIDENCE", "شاهد و مدرک ممیزی کارمزد", "06", "اجرا، تراکنش و ممیزی", 3, false);
    }

    private static void register(String tableName, String title, String groupCode, String groupTitle, int baselineRows, boolean editable) {
        ENTRIES.put(tableName, new Entry(tableName, title, groupCode, groupTitle, baselineRows, editable));
    }

    public static Entry require(String tableName) {
        Entry entry = ENTRIES.get(normalize(tableName));
        if (entry == null) throw new FeeAdminValidationException("Unsupported FEE table: " + tableName);
        return entry;
    }

    public static List<Entry> entries() { return List.copyOf(ENTRIES.values()); }

    public static boolean contains(String tableName) { return ENTRIES.containsKey(normalize(tableName)); }

    private static String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
}
