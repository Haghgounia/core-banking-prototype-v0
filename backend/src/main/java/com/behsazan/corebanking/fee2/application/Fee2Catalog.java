package com.behsazan.corebanking.fee2.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Fee2Catalog {
    private Fee2Catalog() {}

    public record Entry(String tableName, String title, String groupCode, String groupTitle, boolean editable, String description) {}

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        register("FEE_SCOPE", "دامنه‌های موتور کارمزد", "01", "دامنه و کاتالوگ", true, "دامنه اجرایی و منطقه زمانی موتور کارمزد");
        register("FEE_DEFINITION", "تعریف کارمزد", "01", "دامنه و کاتالوگ", true, "کد، نوع، دسته و مالک کسب‌وکاری کارمزد");
        register("FEE_VERSION", "نسخه‌های کارمزد", "02", "طراحی و نسخه‌بندی", true, "نسخه، روش محاسبه، بازه اعتبار و چرخه عمر");
        register("FEE_BINDING", "نحوه اعمال کارمزد", "02", "طراحی و نسخه‌بندی", true, "اتصال نسخه کارمزد به خدمت، رویداد و عملیات");
        register("FEE_CONDITION", "شرایط اعمال کارمزد", "02", "طراحی و نسخه‌بندی", true, "شرایط گروه‌بندی‌شده بر اساس ابعاد و عملگرها");
        register("FEE_MODIFIER", "تعدیلات و سقف‌ها", "02", "طراحی و نسخه‌بندی", true, "تخفیف، کمک‌هزینه، سقف دوره‌ای و تعدیلات");
        register("FEE_TAX", "مالیات کارمزد", "02", "طراحی و نسخه‌بندی", true, "مالیات مرتبط با نسخه کارمزد و مبنای محاسبه");
        register("FEE_SHARE", "تسهیم کارمزد", "02", "طراحی و نسخه‌بندی", true, "سهم ذی‌نفعان و اولویت باقیمانده");
        register("FEE_REGULATION", "مقررات و بخشنامه‌ها", "03", "مقررات و اطلاعات مرجع", true, "مبنای قانونی، مرجع صادرکننده و بازه اعتبار");
        register("FEE_REFERENCE_DATA", "اطلاعات مرجع کارمزد", "03", "مقررات و اطلاعات مرجع", true, "مقادیر مرجع دامنه‌دار و تاریخ‌دار موتور کارمزد");
        register("FEE_APPROVAL", "گردش تأیید نسخه", "04", "گردش کار و کنترل", false, "تاریخچه تصمیم‌های Maker/Checker نسخه‌ها");
        register("FEE_CONFIG_REVISION", "نسخه پیکربندی فعال", "04", "گردش کار و کنترل", false, "Revision و Checksum پیکربندی فعال هر دامنه");
        register("FEE_SIMULATION_RUN", "اجرای شبیه‌سازی", "05", "شبیه‌سازی", true, "تعریف اجرای شبیه‌سازی برای یک نسخه کارمزد");
        register("FEE_SIMULATION_CASE", "سناریوهای شبیه‌سازی", "05", "شبیه‌سازی", true, "ورودی و انتظار هر سناریوی آزمون");
        register("FEE_SIMULATION_RESULT", "نتایج شبیه‌سازی", "05", "شبیه‌سازی", false, "خروجی واقعی، اختلاف و وضعیت هر سناریو");
        register("FEE_CALC_LOG", "تاریخچه محاسبات", "06", "اجرا و ردپا", false, "ردپای درخواست و نتیجه محاسبه کارمزد");
        register("FEE_CALC_ITEM", "اقلام محاسبه", "06", "اجرا و ردپا", false, "ریز Fee، Tax، Share و Adjustment هر محاسبه");
        register("FEE_AUDIT", "ممیزی تغییرات", "06", "اجرا و ردپا", false, "Before/After و هویت عامل تغییر");
        register("FEE_IDEMPOTENCY", "کنترل تکرار درخواست", "06", "اجرا و ردپا", false, "وضعیت Idempotency و پاسخ ذخیره‌شده");
    }

    private static void register(String tableName, String title, String groupCode, String groupTitle, boolean editable, String description) {
        ENTRIES.put(tableName, new Entry(tableName, title, groupCode, groupTitle, editable, description));
    }

    public static List<Entry> entries() { return List.copyOf(ENTRIES.values()); }

    public static Entry require(String tableName) {
        Entry entry = ENTRIES.get(normalize(tableName));
        if (entry == null) throw new Fee2ValidationException("جدول FEE2 پشتیبانی نمی‌شود: " + tableName);
        return entry;
    }

    public static boolean contains(String tableName) { return ENTRIES.containsKey(normalize(tableName)); }

    private static String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
}
