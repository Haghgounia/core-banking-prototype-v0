package com.behsazan.corebanking.calendar.reference.application;

import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.CatalogGroup;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.CatalogItem;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.CatalogResponse;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.FieldDescriptor;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.FieldType;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.SelectOption;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.TableDescriptor;
import com.behsazan.corebanking.shared.error.ReferenceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CalendarReferenceRegistry {
    private final String schemaName;
    private final Map<String, TableDescriptor> descriptors;

    public CalendarReferenceRegistry(@Value("${core-banking.schemas.calendar:CAL}") String schemaName) {
        this.schemaName = schemaName.trim().toUpperCase();
        LinkedHashMap<String, TableDescriptor> items = new LinkedHashMap<>();
        for (TableDescriptor descriptor : buildDescriptors()) {
            items.put(descriptor.resource(), descriptor);
        }
        this.descriptors = Collections.unmodifiableMap(items);
    }

    public String schemaName() {
        return schemaName;
    }

    public TableDescriptor require(String resource) {
        TableDescriptor descriptor = descriptors.get(resource);
        if (descriptor == null) throw new ReferenceNotFoundException("فرم تقویم یافت نشد: " + resource);
        return descriptor;
    }

    public CatalogResponse catalog() {
        List<CatalogGroup> groups = List.of(
                group("CORE", "ساختار و داده تقویم", "تعریف سیستم‌های تقویم، الگوریتم، روز هفته، ماه و داده‌مجموعه تقویم", "calendar_month"),
                group("BUSINESS", "تقویم کاری و بانکی", "تقویم کاری بانک، وضعیت روز کاری، استثناها و قواعد تعدیل روز کاری", "event_available"),
                group("OCCASION", "مناسبت‌ها و رویدادها", "دسته‌بندی، تعریف، قواعد تکرار و رخداد واقعی مناسبت‌ها", "celebration"),
                group("HIJRI", "اصلاحات رسمی تقویم قمری", "ثبت اصلاحات رؤیتی/اعلام رسمی بدون تغییر داده‌مجموعه محاسباتی", "verified")
        );
        return new CatalogResponse(schemaName, descriptors.size(), groups);
    }

    private CatalogGroup group(String code, String title, String description, String icon) {
        List<CatalogItem> tables = descriptors.values().stream()
                .filter(item -> item.group().equals(code))
                .map(item -> new CatalogItem(
                        item.resource(), item.title(), item.description(), item.icon(), item.tableName(),
                        !item.allowCreate() && !item.allowUpdate() && !item.allowDelete()
                ))
                .toList();
        return new CatalogGroup(code, title, description, icon, tables);
    }

    private List<TableDescriptor> buildDescriptors() {
        return List.of(
                calendarSystem(), calendarAlgorithm(), weekday(), calendarMonth(), calendarDay(), calendarDate(),
                businessCalendar(), businessCalendarDay(), calendarException(), businessDayConvention(),
                occasionCategory(), occasion(), occasionRule(), occasionOccurrence(), calendarDayOccasion(),
                hijriDateOverride()
        );
    }

    private TableDescriptor calendarSystem() {
        return table("calendar-systems", "CORE", "سیستم‌های تقویم", "تعریف تقویم میلادی، هجری شمسی ایران و قمری محاسباتی", "date_range",
                "CALENDAR_SYSTEM", true, true, true, false, "calendarSystemCode", "calendarSystemNameFa",
                List.of(
                        keyText("calendarSystemCode", "CALENDAR_SYSTEM_CODE", "کد سیستم تقویم", 30, true),
                        text("calendarSystemNameFa", "CALENDAR_SYSTEM_NAME_FA", "نام فارسی", true, 100, true, true),
                        text("calendarSystemNameEn", "CALENDAR_SYSTEM_NAME_EN", "نام انگلیسی", true, 100, true, true),
                        select("calendarKindCode", "CALENDAR_KIND_CODE", "ماهیت تقویم", true, true, null,
                                option("SOLAR", "خورشیدی"), option("LUNAR", "قمری"), option("CIVIL", "مدنی/میلادی")),
                        bool("activeFlag", "ACTIVE_FLAG", "فعال", true, true, true)
                ));
    }

    private TableDescriptor calendarAlgorithm() {
        return table("calendar-algorithms", "CORE", "الگوریتم‌های تبدیل تقویم", "نسخه و بازه اعتبار الگوریتم‌های تبدیل تاریخ", "functions",
                "CALENDAR_ALGORITHM", true, true, true, false, "algorithmCode", "algorithmName",
                List.of(
                        keyText("algorithmCode", "ALGORITHM_CODE", "کد الگوریتم", 50, true),
                        lookupText("calendarSystemCode", "CALENDAR_SYSTEM_CODE", "سیستم تقویم", "calendar-systems", true, true),
                        text("algorithmName", "ALGORITHM_NAME", "نام الگوریتم", true, 200, true, true),
                        text("algorithmVersion", "ALGORITHM_VERSION", "نسخه الگوریتم", false, 50, true, true),
                        bool("deterministicFlag", "DETERMINISTIC_FLAG", "قطعی بودن الگوریتم", true, true, true),
                        number("verifiedFromYear", "VERIFIED_FROM_YEAR", "سال شروع بازه آزموده‌شده", false, true, false),
                        number("verifiedToYear", "VERIFIED_TO_YEAR", "سال پایان بازه آزموده‌شده", false, true, false),
                        text("description", "DESCRIPTION", "توضیحات", false, 1000, false, true),
                        bool("activeFlag", "ACTIVE_FLAG", "فعال", true, true, true)
                ));
    }

    private TableDescriptor weekday() {
        return table("weekdays", "CORE", "روزهای هفته", "نام و شماره ISO/ایرانی روزهای هفته", "view_week",
                "WEEKDAY", false, true, false, false, "weekdayCode", "weekdayNameFa",
                List.of(
                        keyNumber("weekdayId", "WEEKDAY_ID", "شناسه روز هفته", true, true),
                        text("weekdayCode", "WEEKDAY_CODE", "کد روز هفته", true, 20, true, true),
                        number("isoWeekdayNo", "ISO_WEEKDAY_NO", "شماره ISO", true, true, false),
                        number("irWeekdayNo", "IR_WEEKDAY_NO", "شماره ایرانی", true, true, false),
                        text("weekdayNameFa", "WEEKDAY_NAME_FA", "نام فارسی", true, 30, true, true),
                        text("weekdayNameEn", "WEEKDAY_NAME_EN", "نام انگلیسی", true, 30, true, true)
                ));
    }

    private TableDescriptor calendarMonth() {
        return table("calendar-months", "CORE", "ماه‌های تقویم", "نام ماه‌ها برای هر سیستم تقویم", "calendar_view_month",
                "CALENDAR_MONTH", true, true, true, false, "monthCode", "monthNameFa",
                List.of(
                        keyLookupText("calendarSystemCode", "CALENDAR_SYSTEM_CODE", "سیستم تقویم", "calendar-systems", true, true),
                        keyNumber("monthNo", "MONTH_NO", "شماره ماه", true, true),
                        text("monthCode", "MONTH_CODE", "کد ماه", true, 30, true, true),
                        text("monthNameFa", "MONTH_NAME_FA", "نام فارسی ماه", true, 60, true, true),
                        text("monthNameEn", "MONTH_NAME_EN", "نام انگلیسی ماه", true, 60, true, true)
                ));
    }

    private TableDescriptor calendarDay() {
        return table("calendar-days", "CORE", "روزهای مرجع تقویم", "محور مطلق روز؛ داده‌مجموعه تقویم و فقط‌خواندنی", "today",
                "CALENDAR_DAY", false, false, false, false, "dayId", "canonicalDate",
                List.of(
                        readKeyNumber("dayId", "DAY_ID", "شناسه روز", true),
                        readDate("canonicalDate", "CANONICAL_DATE", "تاریخ مرجع / میلادی", true, true),
                        readNumber("epochDay", "EPOCH_DAY", "شماره روز از مبدأ", true),
                        readNumber("julianDayNumber", "JULIAN_DAY_NUMBER", "شماره روز ژولیوسی", true),
                        readNumber("weekdayId", "WEEKDAY_ID", "شناسه روز هفته", true),
                        readNumber("isoWeekdayNo", "ISO_WEEKDAY_NO", "شماره ISO روز هفته", true),
                        readNumber("irWeekdayNo", "IR_WEEKDAY_NO", "شماره ایرانی روز هفته", true)
                ));
    }

    private TableDescriptor calendarDate() {
        return table("calendar-dates", "CORE", "نمایش تاریخ در سه تقویم", "سه نمایش میلادی، شمسی و قمری برای هر شناسه روز؛ داده‌مجموعه فقط‌خواندنی", "event_note",
                "CALENDAR_DATE", false, false, false, false, "calendarDateId", "formattedDate",
                List.of(
                        readKeyNumber("calendarDateId", "CALENDAR_DATE_ID", "شناسه نمایش تاریخ", true),
                        readNumber("dayId", "DAY_ID", "شناسه روز", true),
                        readText("calendarSystemCode", "CALENDAR_SYSTEM_CODE", "سیستم تقویم", true, true),
                        readNumber("yearNo", "YEAR_NO", "سال", true),
                        readNumber("monthNo", "MONTH_NO", "ماه", true),
                        readNumber("dayNo", "DAY_NO", "روز", true),
                        readNumber("dayOfYear", "DAY_OF_YEAR", "روز سال", false),
                        readText("formattedDate", "FORMATTED_DATE", "تاریخ قالب‌بندی‌شده", true, true),
                        readBoolean("isLeapYear", "IS_LEAP_YEAR", "سال کبیسه", true),
                        readText("algorithmCode", "ALGORITHM_CODE", "الگوریتم", false, true)
                ));
    }

    private TableDescriptor businessCalendar() {
        return table("business-calendars", "BUSINESS", "تقویم‌های کاری", "تعریف تقویم بانک، پرداخت، تسویه، پایاپای، بازار یا سفارشی", "business_center",
                "BUSINESS_CALENDAR", true, true, true, true, "calendarCode", "calendarNameFa",
                List.of(
                        autoKey("businessCalendarId", "BUSINESS_CALENDAR_ID", "شناسه تقویم کاری", true),
                        text("calendarCode", "CALENDAR_CODE", "کد تقویم کاری", true, 50, true, true),
                        text("calendarNameFa", "CALENDAR_NAME_FA", "نام فارسی", true, 200, true, true),
                        text("calendarNameEn", "CALENDAR_NAME_EN", "نام انگلیسی", false, 200, true, true),
                        select("calendarTypeCode", "CALENDAR_TYPE_CODE", "نوع تقویم کاری", true, true, null,
                                option("BANK", "بانکی"), option("PAYMENT_SYSTEM", "سیستم پرداخت"), option("SETTLEMENT", "تسویه"),
                                option("CLEARING", "پایاپای"), option("MARKET", "بازار"), option("COUNTRY", "کشور"),
                                option("CURRENCY", "ارز"), option("CUSTOM", "سفارشی")),
                        text("countryCode", "COUNTRY_CODE", "کد کشور", false, 3, true, true),
                        text("currencyCode", "CURRENCY_CODE", "کد ارز", false, 3, true, true),
                        textDefault("timeZoneCode", "TIME_ZONE_CODE", "منطقه زمانی", true, 100, "Asia/Tehran", true, true),
                        date("validFromDate", "VALID_FROM_DATE", "اعتبار از", false, true),
                        date("validToDate", "VALID_TO_DATE", "اعتبار تا", false, true),
                        bool("activeFlag", "ACTIVE_FLAG", "فعال", true, true, true)
                ));
    }

    private TableDescriptor businessCalendarDay() {
        return table("business-calendar-days", "BUSINESS", "روزهای تقویم کاری", "وضعیت کاری، تعطیلی، تسویه، پایاپای و ثبت حسابداری برای هر روز", "event_available",
                "BUSINESS_CALENDAR_DAY", true, true, true, true, "businessCalendarDayId", "businessCalendarDayId",
                List.of(
                        autoKey("businessCalendarDayId", "BUSINESS_CALENDAR_DAY_ID", "شناسه", true),
                        lookupNumber("businessCalendarId", "BUSINESS_CALENDAR_ID", "تقویم کاری", "business-calendars", true, true),
                        lookupNumber("dayId", "DAY_ID", "روز تقویم", "calendar-days", true, true),
                        bool("workingDay", "IS_WORKING_DAY", "روز کاری", true, true, true),
                        boolFalse("bankHoliday", "IS_BANK_HOLIDAY", "تعطیل بانکی", true, true),
                        bool("settlementDay", "IS_SETTLEMENT_DAY", "روز تسویه", true, true, true),
                        bool("clearingDay", "IS_CLEARING_DAY", "روز پایاپای", true, true, true),
                        bool("postingDay", "IS_POSTING_DAY", "روز ثبت حسابداری", true, true, true),
                        time("openTime", "OPEN_TIME", "زمان بازگشایی", false, 5, false),
                        time("closeTime", "CLOSE_TIME", "زمان بسته‌شدن", false, 5, false),
                        text("statusSourceCode", "STATUS_SOURCE_CODE", "منبع وضعیت", false, 50, true, true)
                ));
    }

    private TableDescriptor calendarException() {
        return table("calendar-exceptions", "BUSINESS", "استثناهای تقویم کاری", "تعطیلی پیش‌بینی‌نشده، روز کاری ویژه، نیم‌روز یا توقف سامانه", "event_busy",
                "CALENDAR_EXCEPTION", true, true, true, true, "exceptionId", "exceptionTypeCode",
                List.of(
                        autoKey("exceptionId", "EXCEPTION_ID", "شناسه استثنا", true),
                        lookupNumber("businessCalendarId", "BUSINESS_CALENDAR_ID", "تقویم کاری", "business-calendars", true, true),
                        lookupNumber("dayId", "DAY_ID", "روز تقویم", "calendar-days", true, true),
                        select("exceptionTypeCode", "EXCEPTION_TYPE_CODE", "نوع استثنا", true, true, null,
                                option("UNSCHEDULED_HOLIDAY", "تعطیلی پیش‌بینی‌نشده"), option("SPECIAL_WORKING_DAY", "روز کاری ویژه"),
                                option("PARTIAL_DAY", "نیم‌روز"), option("SYSTEM_CLOSURE", "توقف سامانه"), option("OTHER", "سایر")),
                        text("reasonCode", "REASON_CODE", "کد علت", false, 50, true, true),
                        text("description", "DESCRIPTION", "توضیحات", false, 1000, false, true),
                        text("sourceAuthorityCode", "SOURCE_AUTHORITY_CODE", "مرجع اعلام‌کننده", false, 50, true, true),
                        nullableBool("workingDayOverride", "WORKING_DAY_OVERRIDE", "اصلاح وضعیت روز کاری", false, true),
                        nullableBool("settlementDayOverride", "SETTLEMENT_DAY_OVERRIDE", "اصلاح وضعیت روز تسویه", false, true),
                        nullableBool("clearingDayOverride", "CLEARING_DAY_OVERRIDE", "اصلاح وضعیت روز پایاپای", false, true),
                        nullableBool("postingDayOverride", "POSTING_DAY_OVERRIDE", "اصلاح وضعیت روز ثبت حسابداری", false, true)
                ));
    }

    private TableDescriptor businessDayConvention() {
        return table("business-day-conventions", "BUSINESS", "قواعد تعدیل روز کاری", "قواعد پس‌رو، پیش‌رو و حالت‌های تعدیل‌شده روز کاری", "swap_horiz",
                "BUSINESS_DAY_CONVENTION", true, true, true, false, "conventionCode", "conventionNameFa",
                List.of(
                        keyText("conventionCode", "CONVENTION_CODE", "کد قاعده", 30, true),
                        text("conventionNameFa", "CONVENTION_NAME_FA", "نام فارسی", true, 100, true, true),
                        text("conventionNameEn", "CONVENTION_NAME_EN", "نام انگلیسی", true, 100, true, true),
                        text("description", "DESCRIPTION", "توضیحات", false, 500, false, true),
                        bool("activeFlag", "ACTIVE_FLAG", "فعال", true, true, true)
                ));
    }

    private TableDescriptor occasionCategory() {
        return table("occasion-categories", "OCCASION", "دسته‌بندی مناسبت‌ها", "مذهبی، ملی، فرهنگی، بانکی، اقتصادی و سایر دسته‌ها", "category",
                "OCCASION_CATEGORY", true, true, true, true, "categoryCode", "categoryNameFa",
                List.of(
                        autoKey("occasionCategoryId", "OCCASION_CATEGORY_ID", "شناسه دسته", true),
                        text("categoryCode", "CATEGORY_CODE", "کد دسته", true, 30, true, true),
                        text("categoryNameFa", "CATEGORY_NAME_FA", "نام فارسی", true, 100, true, true),
                        text("categoryNameEn", "CATEGORY_NAME_EN", "نام انگلیسی", false, 100, true, true),
                        lookupNumber("parentCategoryId", "PARENT_CATEGORY_ID", "دسته والد", "occasion-categories", false, true),
                        bool("activeFlag", "ACTIVE_FLAG", "فعال", true, true, true)
                ));
    }

    private TableDescriptor occasion() {
        return table("occasions", "OCCASION", "تعریف مناسبت‌ها", "تعریف پایدار مناسبت؛ مناسبت به‌تنهایی به معنی تعطیلی نیست", "celebration",
                "OCCASION", true, true, true, true, "occasionCode", "occasionNameFa",
                List.of(
                        autoKey("occasionId", "OCCASION_ID", "شناسه مناسبت", true),
                        text("occasionCode", "OCCASION_CODE", "کد مناسبت", true, 60, true, true),
                        text("occasionNameFa", "OCCASION_NAME_FA", "عنوان فارسی", true, 300, true, true),
                        text("occasionNameEn", "OCCASION_NAME_EN", "عنوان انگلیسی", false, 300, true, true),
                        lookupNumber("occasionCategoryId", "OCCASION_CATEGORY_ID", "دسته مناسبت", "occasion-categories", true, true),
                        lookupText("dateSystemCode", "DATE_SYSTEM_CODE", "سیستم تاریخ مبنا", "calendar-systems", false, true),
                        bool("recurring", "IS_RECURRING", "تکرارشونده", true, true, true),
                        boolFalse("multiDay", "IS_MULTI_DAY", "چندروزه", true, true),
                        text("description", "DESCRIPTION", "توضیحات", false, 1000, false, true),
                        bool("activeFlag", "ACTIVE_FLAG", "فعال", true, true, true)
                ));
    }

    private TableDescriptor occasionRule() {
        return table("occasion-rules", "OCCASION", "قواعد مناسبت", "قاعده ثابت، محاسباتی، دستی یا نسبی برای تولید رخدادها", "rule",
                "OCCASION_RULE", true, true, true, true, "occasionRuleId", "ruleTypeCode",
                List.of(
                        autoKey("occasionRuleId", "OCCASION_RULE_ID", "شناسه قاعده", true),
                        lookupNumber("occasionId", "OCCASION_ID", "مناسبت", "occasions", true, true),
                        select("ruleTypeCode", "RULE_TYPE_CODE", "نوع قاعده", true, true, null,
                                option("FIXED_DATE", "تاریخ ثابت"), option("COMPUTED_DATE", "تاریخ محاسباتی"),
                                option("MANUAL", "دستی"), option("RELATIVE", "نسبی")),
                        lookupText("dateSystemCode", "DATE_SYSTEM_CODE", "سیستم تقویم", "calendar-systems", true, true),
                        number("monthNo", "MONTH_NO", "ماه", false, true, false),
                        number("dayNo", "DAY_NO", "روز", false, true, false),
                        numberDefault("durationDays", "DURATION_DAYS", "مدت (روز)", true, true, 1),
                        number("effectiveFromYear", "EFFECTIVE_FROM_YEAR", "از سال", false, true, false),
                        number("effectiveToYear", "EFFECTIVE_TO_YEAR", "تا سال", false, true, false),
                        numberDefault("priorityNo", "PRIORITY_NO", "اولویت", true, true, 100),
                        bool("activeFlag", "ACTIVE_FLAG", "فعال", true, true, true)
                ));
    }

    private TableDescriptor occasionOccurrence() {
        return table("occasion-occurrences", "OCCASION", "رخدادهای مناسبت", "رخداد واقعی مناسبت در بازه روز با وضعیت رسمی و تأیید", "event",
                "OCCASION_OCCURRENCE", true, true, true, true, "occasionOccurrenceId", "occasionOccurrenceId",
                List.of(
                        autoKey("occasionOccurrenceId", "OCCASION_OCCURRENCE_ID", "شناسه رخداد", true),
                        lookupNumber("occasionId", "OCCASION_ID", "مناسبت", "occasions", true, true),
                        lookupNumber("startDayId", "START_DAY_ID", "روز شروع", "calendar-days", true, true),
                        lookupNumber("endDayId", "END_DAY_ID", "روز پایان", "calendar-days", true, true),
                        selectDefault("occurrenceStatusCode", "OCCURRENCE_STATUS_CODE", "وضعیت رخداد", true, true, "GENERATED",
                                option("GENERATED", "تولیدشده"), option("PROVISIONAL", "موقت"), option("CONFIRMED", "تأییدشده"),
                                option("CORRECTED", "اصلاح‌شده"), option("CANCELLED", "لغوشده")),
                        text("sourceAuthorityCode", "SOURCE_AUTHORITY_CODE", "مرجع اعلام‌کننده", false, 50, true, true),
                        text("sourceReference", "SOURCE_REFERENCE", "مرجع/شماره مستند", false, 500, false, true),
                        boolFalse("official", "IS_OFFICIAL", "رسمی", true, true),
                        boolFalse("confirmed", "IS_CONFIRMED", "تأیید نهایی", true, true)
                ));
    }

    private TableDescriptor calendarDayOccasion() {
        return table("calendar-day-occasions", "OCCASION", "مناسبت‌های روز", "ارتباط چندبه‌چند روز با رخداد مناسبت و اولویت نمایش", "event_repeat",
                "CALENDAR_DAY_OCCASION", true, true, true, true, "calendarDayOccasionId", "calendarDayOccasionId",
                List.of(
                        autoKey("calendarDayOccasionId", "CALENDAR_DAY_OCCASION_ID", "شناسه", true),
                        lookupNumber("dayId", "DAY_ID", "روز تقویم", "calendar-days", true, true),
                        lookupNumber("occasionOccurrenceId", "OCCASION_OCCURRENCE_ID", "رخداد مناسبت", "occasion-occurrences", true, true),
                        numberDefault("displayPriority", "DISPLAY_PRIORITY", "اولویت نمایش", true, true, 100),
                        boolFalse("primaryOccasion", "PRIMARY_OCCASION_FLAG", "مناسبت اصلی روز", true, true)
                ));
    }

    private TableDescriptor hijriDateOverride() {
        return table("hijri-date-overrides", "HIJRI", "اصلاحات رسمی تاریخ قمری", "لایه اصلاح رسمی/رؤیتی روی تقویم قمری محاسباتی", "verified",
                "HIJRI_DATE_OVERRIDE", true, true, true, true, "hijriOverrideId", "sourceAuthorityCode",
                List.of(
                        autoKey("hijriOverrideId", "HIJRI_OVERRIDE_ID", "شناسه اصلاح", true),
                        lookupNumber("dayId", "DAY_ID", "روز مرجع تقویم", "calendar-days", true, true),
                        number("officialHijriYear", "OFFICIAL_HIJRI_YEAR", "سال قمری رسمی", true, true, false),
                        number("officialHijriMonth", "OFFICIAL_HIJRI_MONTH", "ماه قمری رسمی", true, true, false),
                        number("officialHijriDay", "OFFICIAL_HIJRI_DAY", "روز قمری رسمی", true, true, false),
                        text("sourceAuthorityCode", "SOURCE_AUTHORITY_CODE", "مرجع اعلام‌کننده", true, 50, true, true),
                        text("sourceReference", "SOURCE_REFERENCE", "مرجع/شماره مستند", false, 500, false, true),
                        date("announcementDate", "ANNOUNCEMENT_DATE", "تاریخ اعلام", false, true),
                        selectDefault("statusCode", "STATUS_CODE", "وضعیت", true, true, "CONFIRMED",
                                option("PROVISIONAL", "موقت"), option("CONFIRMED", "تأییدشده"),
                                option("CORRECTED", "اصلاح‌شده"), option("CANCELLED", "لغوشده")),
                        date("validFrom", "VALID_FROM", "اعتبار از", true, true),
                        date("validTo", "VALID_TO", "اعتبار تا", false, true)
                ));
    }

    private TableDescriptor table(String resource, String group, String title, String description, String icon,
                                  String tableName, boolean create, boolean update, boolean delete, boolean autoPk,
                                  String lookupCode, String lookupName, List<FieldDescriptor> fields) {
        return new TableDescriptor(resource, group, title, description, icon, schemaName, tableName,
                create, update, delete, autoPk, lookupCode, lookupName, fields);
    }

    private static FieldDescriptor keyText(String api, String column, String label, int max, boolean grid) {
        return new FieldDescriptor(api, column, label, FieldType.TEXT, true, true, false, grid, true, max, null, null, List.of());
    }

    private static FieldDescriptor keyLookupText(String api, String column, String label, String lookup, boolean required, boolean grid) {
        return new FieldDescriptor(api, column, label, FieldType.LOOKUP, required, true, false, grid, true, null, null, lookup, List.of());
    }

    private static FieldDescriptor keyNumber(String api, String column, String label, boolean required, boolean grid) {
        return new FieldDescriptor(api, column, label, FieldType.NUMBER, required, true, false, grid, true, null, null, null, List.of());
    }

    private static FieldDescriptor autoKey(String api, String column, String label, boolean grid) {
        return new FieldDescriptor(api, column, label, FieldType.NUMBER, true, true, true, grid, false, null, null, null, List.of());
    }

    private static FieldDescriptor readKeyNumber(String api, String column, String label, boolean grid) {
        return new FieldDescriptor(api, column, label, FieldType.NUMBER, true, true, true, grid, true, null, null, null, List.of());
    }

    private static FieldDescriptor text(String api, String column, String label, boolean required, int max, boolean grid, boolean searchable) {
        return new FieldDescriptor(api, column, label, FieldType.TEXT, required, false, false, grid, searchable, max, null, null, List.of());
    }

    private static FieldDescriptor textDefault(String api, String column, String label, boolean required, int max, Object defaultValue, boolean grid, boolean searchable) {
        return new FieldDescriptor(api, column, label, FieldType.TEXT, required, false, false, grid, searchable, max, defaultValue, null, List.of());
    }

    private static FieldDescriptor number(String api, String column, String label, boolean required, boolean grid, boolean searchable) {
        return new FieldDescriptor(api, column, label, FieldType.NUMBER, required, false, false, grid, searchable, null, null, null, List.of());
    }

    private static FieldDescriptor numberDefault(String api, String column, String label, boolean required, boolean grid, Object defaultValue) {
        return new FieldDescriptor(api, column, label, FieldType.NUMBER, required, false, false, grid, false, null, defaultValue, null, List.of());
    }

    private static FieldDescriptor date(String api, String column, String label, boolean required, boolean grid) {
        return new FieldDescriptor(api, column, label, FieldType.DATE, required, false, false, grid, true, null, null, null, List.of());
    }

    private static FieldDescriptor time(String api, String column, String label, boolean required, int max, boolean grid) {
        return new FieldDescriptor(api, column, label, FieldType.TIME, required, false, false, grid, false, max, null, null, List.of());
    }

    private static FieldDescriptor bool(String api, String column, String label, boolean required, boolean grid, boolean defaultValue) {
        return new FieldDescriptor(api, column, label, FieldType.BOOLEAN, required, false, false, grid, false, null, defaultValue, null, List.of());
    }

    private static FieldDescriptor boolFalse(String api, String column, String label, boolean required, boolean grid) {
        return bool(api, column, label, required, grid, false);
    }

    private static FieldDescriptor nullableBool(String api, String column, String label, boolean required, boolean grid) {
        return new FieldDescriptor(api, column, label, FieldType.BOOLEAN, required, false, false, grid, false, null, null, null, List.of());
    }

    private static FieldDescriptor select(String api, String column, String label, boolean required, boolean grid, Object defaultValue, SelectOption... options) {
        return new FieldDescriptor(api, column, label, FieldType.SELECT, required, false, false, grid, false, null, defaultValue, null, List.of(options));
    }

    private static FieldDescriptor selectDefault(String api, String column, String label, boolean required, boolean grid, Object defaultValue, SelectOption... options) {
        return select(api, column, label, required, grid, defaultValue, options);
    }

    private static FieldDescriptor lookupText(String api, String column, String label, String lookup, boolean required, boolean grid) {
        return new FieldDescriptor(api, column, label, FieldType.LOOKUP, required, false, false, grid, true, null, null, lookup, List.of());
    }

    private static FieldDescriptor lookupNumber(String api, String column, String label, String lookup, boolean required, boolean grid) {
        return new FieldDescriptor(api, column, label, FieldType.LOOKUP, required, false, false, grid, true, null, null, lookup, List.of());
    }

    private static FieldDescriptor readText(String api, String column, String label, boolean grid, boolean searchable) {
        return new FieldDescriptor(api, column, label, FieldType.TEXT, false, false, true, grid, searchable, null, null, null, List.of());
    }

    private static FieldDescriptor readNumber(String api, String column, String label, boolean grid) {
        return new FieldDescriptor(api, column, label, FieldType.NUMBER, false, false, true, grid, true, null, null, null, List.of());
    }

    private static FieldDescriptor readDate(String api, String column, String label, boolean grid, boolean searchable) {
        return new FieldDescriptor(api, column, label, FieldType.DATE, false, false, true, grid, searchable, null, null, null, List.of());
    }

    private static FieldDescriptor readBoolean(String api, String column, String label, boolean grid) {
        return new FieldDescriptor(api, column, label, FieldType.BOOLEAN, false, false, true, grid, false, null, null, null, List.of());
    }

    private static SelectOption option(Object value, String label) {
        return new SelectOption(value, label);
    }
}
