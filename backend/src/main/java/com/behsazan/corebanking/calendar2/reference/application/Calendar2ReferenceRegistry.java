package com.behsazan.corebanking.calendar2.reference.application;

import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.CatalogGroup;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.CatalogItem;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.CatalogResponse;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.FieldDescriptor;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.FieldType;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.SelectOption;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.TableDescriptor;
import com.behsazan.corebanking.shared.error.ReferenceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class Calendar2ReferenceRegistry {
    private final String schemaName;
    private final Map<String, TableDescriptor> descriptors;

    public Calendar2ReferenceRegistry(@Value("${core-banking.schemas.calendar2:CAL2}") String schemaName) {
        this.schemaName = schemaName.trim().toUpperCase();
        LinkedHashMap<String, TableDescriptor> items = new LinkedHashMap<>();
        for (TableDescriptor descriptor : buildDescriptors()) items.put(descriptor.resource(), descriptor);
        this.descriptors = Collections.unmodifiableMap(items);
    }

    public String schemaName() { return schemaName; }

    public TableDescriptor require(String resource) {
        TableDescriptor descriptor = descriptors.get(resource);
        if (descriptor == null) throw new ReferenceNotFoundException("فرم CAL2 یافت نشد: " + resource);
        return descriptor;
    }

    public CatalogResponse catalog() {
        return new CatalogResponse(schemaName, descriptors.size(), List.of(
                group("DEFINITION", "تعاریف تقویم", "سیستم تقویم، Variant، ماه و روز هفته", "calendar_month"),
                group("SOURCE", "منبع و نسخه Dataset", "مراجع داده و شناسنامه نسخه Dataset", "fact_check"),
                group("DATASET", "Dataset تقویم", "روز مرجع تقویم و نگاشت تاریخ در سه Variant؛ فقط‌خواندنی", "date_range"),
                group("EVENT", "مناسبت‌ها و رویدادها", "تعریف مناسبت، قاعده تکرار و رخدادهای واقعی/تولیدشده", "celebration"),
                group("BUSINESS", "تقویم کاری و بانکی", "تعریف تقویم کاری و وضعیت عملیاتی هر روز", "business_center"),
                group("VALIDATION", "کنترل و ممیزی Dataset", "اجرای اعتبارسنجی و شواهد نتیجه؛ فقط‌خواندنی", "verified")
        ));
    }

    private CatalogGroup group(String code, String title, String description, String icon) {
        List<CatalogItem> tables = descriptors.values().stream().filter(item -> item.group().equals(code))
                .map(item -> new CatalogItem(item.resource(), item.title(), item.description(), item.icon(), item.tableName(),
                        !item.allowCreate() && !item.allowUpdate() && !item.allowDelete())).toList();
        return new CatalogGroup(code, title, description, icon, tables);
    }

    private List<TableDescriptor> buildDescriptors() {
        return List.of(calendarSystem(), sourceAuthority(), datasetVersion(), calendarVariant(), calendarMonth(), weekday(),
                canonicalDay(), calendarDate(), eventType(), event(), eventRecurrenceRule(), eventOccurrence(), businessCalendar(), businessCalendarDay(),
                validationRun(), validationResult());
    }

    private TableDescriptor calendarSystem() {
        return table("calendar-systems", "DEFINITION", "سیستم‌های تقویم", "تعریف Gregorian، Persian و Islamic", "calendar_today",
                "CALENDAR_SYSTEM", true, true, false, true, "calendarCode", "nameFa", List.of(
                        autoKey("calendarSystemId", "CALENDAR_SYSTEM_ID", "شناسه سیستم", true),
                        text("calendarCode", "CALENDAR_CODE", "کد تقویم", true, 30, true, true),
                        text("nameFa", "NAME_FA", "نام فارسی", true, 100, true, true),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, 120, true, true),
                        select("calendarType", "CALENDAR_TYPE", "نوع تقویم", true, true, null,
                                option("SOLAR", "خورشیدی"), option("LUNAR", "قمری")),
                        bool("activeFlag", "ACTIVE_FLAG", "فعال", true, true, true),
                        timestamp("createdAt", "CREATED_AT", "زمان ایجاد", false, false, true)
                ));
    }

    private TableDescriptor sourceAuthority() {
        return table("source-authorities", "SOURCE", "مراجع و منابع داده", "مرجع علمی، رسمی یا داخلی تولید/اعلام داده", "source",
                "SOURCE_AUTHORITY", true, true, false, true, "sourceCode", "nameFa", List.of(
                        autoKey("sourceId", "SOURCE_ID", "شناسه منبع", true),
                        text("sourceCode", "SOURCE_CODE", "کد منبع", true, 60, true, true),
                        text("nameFa", "NAME_FA", "نام فارسی", true, 200, true, true),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, 250, true, true),
                        select("sourceType", "SOURCE_TYPE", "نوع منبع", false, true, null,
                                option("SCIENTIFIC", "علمی"), option("GOVERNMENT", "دولتی/رسمی"), option("INTERNAL", "داخلی")),
                        text("sourceUri", "SOURCE_URI", "نشانی منبع", false, 1000, false, true),
                        text("countryCode", "COUNTRY_CODE", "کد کشور", false, 3, true, true),
                        select("authorityLevel", "AUTHORITY_LEVEL", "سطح مرجع", false, true, null,
                                option("REFERENCE", "مرجع"), option("OFFICIAL", "رسمی"), option("INTERNAL", "داخلی")),
                        bool("activeFlag", "ACTIVE_FLAG", "فعال", true, true, true)
                ));
    }

    private TableDescriptor datasetVersion() {
        return table("dataset-versions", "SOURCE", "نسخه‌های Dataset", "شناسنامه، بازه، موتور تولید و checksum هر Dataset", "inventory_2",
                "DATASET_VERSION", false, false, false, false, "versionCode", "versionCode", List.of(
                        readKeyNumber("datasetVersionId", "DATASET_VERSION_ID", "شناسه نسخه", true),
                        readText("versionCode", "VERSION_CODE", "کد نسخه", true, true),
                        readText("generatorVersion", "GENERATOR_VERSION", "نسخه مولد", true, true),
                        readText("algorithmVersion", "ALGORITHM_VERSION", "نسخه الگوریتم", true, true),
                        readText("sourceVersion", "SOURCE_VERSION", "نسخه منبع", true, true),
                        readDate("rangeStartDate", "RANGE_START_DATE", "شروع بازه", true, true),
                        readDate("rangeEndDate", "RANGE_END_DATE", "پایان بازه", true, true),
                        readText("status", "STATUS", "وضعیت", true, true),
                        readText("checksumSha256", "CHECKSUM_SHA256", "SHA-256", false, true),
                        readTimestamp("generatedAt", "GENERATED_AT", "زمان تولید", true),
                        readText("approvedBy", "APPROVED_BY", "تأییدکننده", false, true),
                        readTimestamp("approvedAt", "APPROVED_AT", "زمان تأیید", false)
                ));
    }

    private TableDescriptor calendarVariant() {
        return table("calendar-variants", "DEFINITION", "Variantهای تقویم", "روش/الگوریتم و مرجع هر گونه تقویمی", "account_tree",
                "CALENDAR_VARIANT", true, true, false, true, "variantCode", "variantCode", List.of(
                        autoKey("calendarVariantId", "CALENDAR_VARIANT_ID", "شناسه Variant", true),
                        lookupNumber("calendarSystemId", "CALENDAR_SYSTEM_ID", "سیستم تقویم", "calendar-systems", true, true),
                        text("variantCode", "VARIANT_CODE", "کد Variant", true, 80, true, true),
                        select("methodType", "METHOD_TYPE", "روش تولید", true, true, null,
                                option("CALCULATED", "محاسباتی"), option("OFFICIAL", "رسمی"), option("OVERRIDDEN", "اصلاح‌شده")),
                        text("algorithmCode", "ALGORITHM_CODE", "کد الگوریتم", false, 100, true, true),
                        lookupNumber("authorityId", "AUTHORITY_ID", "مرجع", "source-authorities", false, true),
                        date("validFrom", "VALID_FROM", "اعتبار از", false, true),
                        date("validTo", "VALID_TO", "اعتبار تا", false, true),
                        bool("isDefault", "IS_DEFAULT", "Variant پیش‌فرض", true, true, false),
                        numberDefault("versionNo", "VERSION_NO", "شماره نسخه", true, true, 1),
                        bool("activeFlag", "ACTIVE_FLAG", "فعال", true, true, true)
                ));
    }

    private TableDescriptor calendarMonth() {
        return table("calendar-months", "DEFINITION", "ماه‌های تقویم", "نام و ترتیب ماه‌ها در هر سیستم تقویم", "calendar_view_month",
                "CALENDAR_MONTH", true, true, false, true, "calendarMonthId", "nameFa", List.of(
                        autoKey("calendarMonthId", "CALENDAR_MONTH_ID", "شناسه ماه", true),
                        lookupNumber("calendarSystemId", "CALENDAR_SYSTEM_ID", "سیستم تقویم", "calendar-systems", true, true),
                        number("monthNo", "MONTH_NO", "شماره ماه", true, true, true),
                        text("nameFa", "NAME_FA", "نام فارسی", true, 100, true, true),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, 100, true, true),
                        text("shortNameFa", "SHORT_NAME_FA", "نام کوتاه فارسی", false, 30, true, true),
                        text("shortNameEn", "SHORT_NAME_EN", "نام کوتاه انگلیسی", false, 30, true, true),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", false, true, false)
                ));
    }

    private TableDescriptor weekday() {
        return table("weekdays", "DEFINITION", "روزهای هفته", "شماره ISO و ترتیب نمایش ایرانی روز هفته", "view_week",
                "WEEKDAY", true, true, false, true, "isoWeekdayNo", "nameFa", List.of(
                        autoKey("weekdayId", "WEEKDAY_ID", "شناسه روز هفته", true),
                        number("isoWeekdayNo", "ISO_WEEKDAY_NO", "شماره ISO", true, true, true),
                        text("nameFa", "NAME_FA", "نام فارسی", true, 50, true, true),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, 50, true, true),
                        text("shortNameFa", "SHORT_NAME_FA", "نام کوتاه فارسی", false, 20, true, true),
                        text("shortNameEn", "SHORT_NAME_EN", "نام کوتاه انگلیسی", false, 20, true, true),
                        number("irDisplayOrder", "IR_DISPLAY_ORDER", "ترتیب نمایش ایرانی", false, true, false)
                ));
    }

    private TableDescriptor canonicalDay() {
        return table("canonical-days", "DATASET", "روزهای مرجع تقویم", "محور روز مستقل از نوع تقویم؛ Dataset تقویم و فقط‌خواندنی", "today",
                "CANONICAL_DAY", false, false, false, false, "dayId", "isoDateText", List.of(
                        readKeyNumber("dayId", "DAY_ID", "شناسه روز", true),
                        readNumber("epochDay", "EPOCH_DAY", "Epoch Day", true),
                        readDate("canonicalDate", "CANONICAL_DATE", "تاریخ مرجع", true, true),
                        readText("isoDateText", "ISO_DATE_TEXT", "تاریخ ISO", true, true),
                        readNumber("weekdayId", "WEEKDAY_ID", "شناسه روز هفته", true),
                        readNumber("isoWeekNo", "ISO_WEEK_NO", "هفته ISO", true),
                        readNumber("isoWeekYear", "ISO_WEEK_YEAR", "سال هفته ISO", true),
                        readTimestamp("createdAt", "CREATED_AT", "زمان ایجاد", false)
                ));
    }

    private TableDescriptor calendarDate() {
        return table("calendar-dates", "DATASET", "نگاشت تاریخ‌های تقویمی", "نگاشت هر روز مرجع تقویم به Gregorian/Persian/Islamic Variant", "event_note",
                "CALENDAR_DATE", false, false, false, false, "calendarDateId", "calendarDateId", List.of(
                        readKeyNumber("calendarDateId", "CALENDAR_DATE_ID", "شناسه نگاشت", true),
                        readNumber("dayId", "DAY_ID", "شناسه روز", true),
                        readNumber("calendarVariantId", "CALENDAR_VARIANT_ID", "شناسه Variant", true),
                        readNumber("yearNo", "YEAR_NO", "سال", true),
                        readNumber("monthNo", "MONTH_NO", "ماه", true),
                        readNumber("dayNo", "DAY_NO", "روز", true),
                        readNumber("dayOfYear", "DAY_OF_YEAR", "روز سال", true),
                        readNumber("weekdayId", "WEEKDAY_ID", "روز هفته", true),
                        readBoolean("isLeapYear", "IS_LEAP_YEAR", "سال کبیسه", true),
                        readBoolean("isLeapMonth", "IS_LEAP_MONTH", "ماه کبیسه", false),
                        readNumber("monthLength", "MONTH_LENGTH", "طول ماه", true),
                        readText("dataStatus", "DATA_STATUS", "وضعیت داده", true, true),
                        readNumber("datasetVersionId", "DATASET_VERSION_ID", "نسخه Dataset", true)
                ));
    }

    private TableDescriptor eventType() {
        return table("event-types", "EVENT", "انواع رویداد و مناسبت", "ملی، مذهبی، بانکی، نظارتی و سایر انواع", "category",
                "EVENT_TYPE", true, true, false, true, "eventTypeCode", "nameFa", List.of(
                        autoKey("eventTypeId", "EVENT_TYPE_ID", "شناسه نوع", true),
                        text("eventTypeCode", "EVENT_TYPE_CODE", "کد نوع", true, 60, true, true),
                        text("nameFa", "NAME_FA", "نام فارسی", true, 120, true, true),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, 120, true, true),
                        bool("activeFlag", "ACTIVE_FLAG", "فعال", true, true, true)
                ));
    }

    private TableDescriptor event() {
        return table("events", "EVENT", "رویدادها و مناسبت‌ها", "تعریف پایدار مناسبت مستقل از وقوع تاریخی", "celebration",
                "EVENT", true, true, true, true, "eventCode", "nameFa", List.of(
                        autoKey("eventId", "EVENT_ID", "شناسه رویداد", true),
                        text("eventCode", "EVENT_CODE", "کد رویداد", true, 80, true, true),
                        lookupNumber("eventTypeId", "EVENT_TYPE_ID", "نوع رویداد", "event-types", true, true),
                        text("nameFa", "NAME_FA", "عنوان فارسی", true, 300, true, true),
                        text("nameEn", "NAME_EN", "عنوان انگلیسی", false, 300, true, true),
                        text("description", "DESCRIPTION", "توضیحات", false, 2000, false, true),
                        bool("officialFlag", "OFFICIAL_FLAG", "رسمی", true, true, false),
                        bool("defaultHolidayFlag", "DEFAULT_HOLIDAY_FLAG", "تعطیل پیش‌فرض", true, true, false),
                        bool("activeFlag", "ACTIVE_FLAG", "فعال", true, true, true)
                ));
    }

    private TableDescriptor eventRecurrenceRule() {
        return table("event-recurrence-rules", "EVENT", "مناسبت‌های تقویم",
                "تعریف مناسبت‌های سالانه و یک‌باره و تولید خودکار رخدادها در تقویم",
                "event_repeat", "EVENT_RECURRENCE_RULE", true, true, false, true, "eventRuleId", "eventRuleId", List.of(
                        autoKey("eventRuleId", "EVENT_RULE_ID", "شناسه قاعده", false),
                        lookupNumber("eventId", "EVENT_ID", "مناسبت", "events", true, true),
                        select("ruleType", "RULE_TYPE", "نحوه وقوع", true, true, "ANNUAL_FIXED_DATE",
                                option("ANNUAL_FIXED_DATE", "سالانه در تاریخ ثابت"),
                                option("ONE_TIME_DATE", "یک‌باره در تاریخ مشخص")),
                        lookupNumber("calendarVariantId", "CALENDAR_VARIANT_ID", "تقویم مبنا", "calendar-variants", true, true),
                        number("yearNo", "YEAR_NO", "سال وقوع (فقط یک‌باره)", false, true, true),
                        number("monthNo", "MONTH_NO", "ماه", true, true, true),
                        number("dayNo", "DAY_NO", "روز", true, true, true),
                        number("startYearNo", "START_YEAR_NO", "سال شروع (اختیاری)", false, false, true),
                        number("endYearNo", "END_YEAR_NO", "سال پایان (اختیاری)", false, false, true),
                        lookupNumber("sourceId", "SOURCE_ID", "منبع/مرجع مناسبت", "source-authorities", false, false),
                        text("description", "DESCRIPTION", "توضیحات", false, 1000, false, true),
                        bool("activeFlag", "ACTIVE_FLAG", "فعال", true, true, true)
                ));
    }

    private TableDescriptor eventOccurrence() {
        return table("event-occurrences", "EVENT", "رخدادهای مناسبت‌ها", "مشاهده وقوع مناسبت‌ها در تاریخ‌های واقعی تقویم؛ رخدادهای تولیدشده فقط‌خواندنی هستند", "event",
                "EVENT_OCCURRENCE", true, true, true, true, "eventOccurrenceId", "eventOccurrenceId", List.of(
                        autoKey("eventOccurrenceId", "EVENT_OCCURRENCE_ID", "شناسه رخداد", true),
                        lookupNumber("eventId", "EVENT_ID", "رویداد", "events", true, true),
                        lookupNumber("eventRuleId", "EVENT_RULE_ID", "قاعده مولد", "event-recurrence-rules", false, true),
                        lookupNumber("dayId", "DAY_ID", "روز مرجع تقویم", "canonical-days", true, true),
                        lookupNumber("sourceId", "SOURCE_ID", "منبع", "source-authorities", false, true),
                        select("occurrenceSource", "OCCURRENCE_SOURCE", "منشأ رخداد", true, true, "MANUAL",
                                option("GENERATED", "تولیدشده"), option("MANUAL", "دستی"), option("OFFICIAL", "رسمی")),
                        select("dataStatus", "DATA_STATUS", "وضعیت داده", false, true, null,
                                option("CALCULATED", "محاسباتی"), option("VERIFIED", "تأییدشده"), option("OFFICIAL", "رسمی"),
                                option("OVERRIDDEN", "اصلاح‌شده"), option("CANCELLED", "لغوشده")),
                        bool("holidayFlag", "HOLIDAY_FLAG", "تعطیل", true, true, false),
                        timestamp("startTime", "START_TIME", "شروع", false, true, false),
                        timestamp("endTime", "END_TIME", "پایان", false, true, false),
                        text("description", "DESCRIPTION", "توضیحات", false, 2000, false, true),
                        lookupNumber("datasetVersionId", "DATASET_VERSION_ID", "نسخه Dataset", "dataset-versions", false, true)
                ));
    }

    private TableDescriptor businessCalendar() {
        return table("business-calendars", "BUSINESS", "تقویم‌های کاری", "تقویم بانکی/سازمانی مستقل برای کشور یا سازمان", "business_center",
                "BUSINESS_CALENDAR", true, true, true, true, "calendarCode", "nameFa", List.of(
                        autoKey("businessCalendarId", "BUSINESS_CALENDAR_ID", "شناسه تقویم کاری", true),
                        text("calendarCode", "CALENDAR_CODE", "کد تقویم کاری", true, 80, true, true),
                        text("nameFa", "NAME_FA", "نام فارسی", true, 200, true, true),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, 200, true, true),
                        lookupText("countryCode", "COUNTRY_CODE", "کشور", "geo-countries", false, true, 3),
                        lookupText("timeZone", "TIME_ZONE", "منطقه زمانی", "iana-time-zones", false, true, 80),
                        text("organizationId", "ORGANIZATION_ID", "شناسه سازمان", false, 80, true, true),
                        date("validFrom", "VALID_FROM", "اعتبار از", false, true),
                        date("validTo", "VALID_TO", "اعتبار تا", false, true),
                        bool("activeFlag", "ACTIVE_FLAG", "فعال", true, true, true)
                ));
    }

    private TableDescriptor businessCalendarDay() {
        return table("business-calendar-days", "BUSINESS", "روزهای تقویم کاری", "وضعیت کاری، تسویه، پایاپای و پردازش برای هر روز", "event_available",
                "BUSINESS_CALENDAR_DAY", true, true, true, true, "businessCalendarDayId", "businessCalendarDayId", List.of(
                        autoKey("businessCalendarDayId", "BUSINESS_CALENDAR_DAY_ID", "شناسه", true),
                        lookupNumber("businessCalendarId", "BUSINESS_CALENDAR_ID", "تقویم کاری", "business-calendars", true, true),
                        lookupNumber("dayId", "DAY_ID", "روز مرجع تقویم", "canonical-days", true, true),
                        select("dayStatus", "DAY_STATUS", "وضعیت روز", false, true, null,
                                option("UNCLASSIFIED", "طبقه‌بندی‌نشده"), option("OPEN", "باز"), option("CLOSED", "بسته"),
                                option("PARTIAL", "نیمه‌وقت"), option("BUSINESS", "روز کاری"), option("WEEKEND", "تعطیلی هفتگی"),
                                option("HOLIDAY", "تعطیل رسمی")),
                        timestamp("openTime", "OPEN_TIME", "زمان بازشدن", false, true, false),
                        timestamp("closeTime", "CLOSE_TIME", "زمان بسته‌شدن", false, true, false),
                        bool("isBusinessDay", "IS_BUSINESS_DAY", "روز کاری", true, true, false),
                        bool("isSettlementDay", "IS_SETTLEMENT_DAY", "روز تسویه", true, true, false),
                        bool("isClearingDay", "IS_CLEARING_DAY", "روز پایاپای", true, true, false),
                        bool("isProcessingDay", "IS_PROCESSING_DAY", "روز پردازش", true, true, false),
                        select("reasonCode", "REASON_CODE", "دلیل وضعیت", false, true, null,
                                option("PENDING_RULE_EVALUATION", "در انتظار اعمال قواعد"),
                                option("PUBLIC_HOLIDAY", "تعطیل رسمی"), option("WEEKEND", "تعطیلی هفتگی"),
                                option("BANK_HOLIDAY", "تعطیلی بانکی"), option("SPECIAL_CLOSED", "تعطیلی موردی"),
                                option("EMERGENCY_CLOSED", "تعطیلی اضطراری"), option("HALF_DAY", "نیمه‌وقت"),
                                option("MANUAL_OVERRIDE", "اصلاح دستی")),
                        lookupNumber("sourceId", "SOURCE_ID", "منبع", "source-authorities", false, true)
                ));
    }

    private TableDescriptor validationRun() {
        return table("validation-runs", "VALIDATION", "اجرای اعتبارسنجی", "مشخصات اجرای Validator و تعداد assertionها", "verified_user",
                "VALIDATION_RUN", false, false, false, false, "validationRunId", "validatorName", List.of(
                        readKeyNumber("validationRunId", "VALIDATION_RUN_ID", "شناسه اجرا", true),
                        readNumber("datasetVersionId", "DATASET_VERSION_ID", "نسخه Dataset", true),
                        readTimestamp("runStartedAt", "RUN_STARTED_AT", "شروع اجرا", true),
                        readTimestamp("runFinishedAt", "RUN_FINISHED_AT", "پایان اجرا", true),
                        readText("validatorName", "VALIDATOR_NAME", "Validator", true, true),
                        readText("validatorVersion", "VALIDATOR_VERSION", "نسخه Validator", true, true),
                        readNumber("totalAssertions", "TOTAL_ASSERTIONS", "کل assertion", true),
                        readNumber("passedAssertions", "PASSED_ASSERTIONS", "موفق", true),
                        readNumber("failedAssertions", "FAILED_ASSERTIONS", "ناموفق", true),
                        readText("runStatus", "RUN_STATUS", "وضعیت اجرا", true, true),
                        readText("evidenceChecksum", "EVIDENCE_CHECKSUM", "Checksum شواهد", false, true)
                ));
    }

    private TableDescriptor validationResult() {
        return table("validation-results", "VALIDATION", "نتایج اعتبارسنجی", "نتیجه هر Test و مقادیر مورد انتظار/واقعی", "fact_check",
                "VALIDATION_RESULT", false, false, false, false, "validationResultId", "testCode", List.of(
                        readKeyNumber("validationResultId", "VALIDATION_RESULT_ID", "شناسه نتیجه", true),
                        readNumber("validationRunId", "VALIDATION_RUN_ID", "شناسه اجرا", true),
                        readText("testCode", "TEST_CODE", "کد آزمون", true, true),
                        readNumber("dayId", "DAY_ID", "شناسه روز", false),
                        readNumber("calendarVariantId", "CALENDAR_VARIANT_ID", "شناسه Variant", false),
                        readText("resultStatus", "RESULT_STATUS", "نتیجه", true, true),
                        readText("expectedValue", "EXPECTED_VALUE", "مقدار مورد انتظار", false, true),
                        readText("actualValue", "ACTUAL_VALUE", "مقدار واقعی", false, true),
                        readText("errorCode", "ERROR_CODE", "کد خطا", false, true),
                        readText("details", "DETAILS", "جزئیات", false, true)
                ));
    }

    private TableDescriptor table(String resource, String group, String title, String description, String icon, String tableName,
                                  boolean create, boolean update, boolean delete, boolean autoPk,
                                  String lookupCode, String lookupName, List<FieldDescriptor> fields) {
        return new TableDescriptor(resource, group, title, description, icon, schemaName, tableName,
                create, update, delete, autoPk, lookupCode, lookupName, fields);
    }

    private static FieldDescriptor autoKey(String api, String col, String label, boolean grid) {
        return f(api, col, label, FieldType.NUMBER, true, true, true, grid, false, null, null, null);
    }
    private static FieldDescriptor text(String api, String col, String label, boolean req, int max, boolean grid, boolean search) {
        return f(api, col, label, FieldType.TEXT, req, false, false, grid, search, max, null, null);
    }
    private static FieldDescriptor number(String api, String col, String label, boolean req, boolean grid, boolean search) {
        return f(api, col, label, FieldType.NUMBER, req, false, false, grid, search, null, null, null);
    }
    private static FieldDescriptor numberDefault(String api, String col, String label, boolean req, boolean grid, Object def) {
        return f(api, col, label, FieldType.NUMBER, req, false, false, grid, false, null, def, null);
    }
    private static FieldDescriptor date(String api, String col, String label, boolean req, boolean grid) {
        return f(api, col, label, FieldType.DATE, req, false, false, grid, true, null, null, null);
    }
    private static FieldDescriptor timestamp(String api, String col, String label, boolean req, boolean grid, boolean readOnly) {
        return f(api, col, label, FieldType.TIMESTAMP, req, false, readOnly, grid, true, null, null, null);
    }
    private static FieldDescriptor bool(String api, String col, String label, boolean req, boolean grid, boolean def) {
        return f(api, col, label, FieldType.BOOLEAN, req, false, false, grid, false, null, def, null);
    }
    private static FieldDescriptor select(String api, String col, String label, boolean req, boolean grid, Object def, SelectOption... opts) {
        return new FieldDescriptor(api, col, label, FieldType.SELECT, req, false, false, grid, false, null, def, null, List.of(opts));
    }
    private static FieldDescriptor lookupNumber(String api, String col, String label, String lookup, boolean req, boolean grid) {
        return f(api, col, label, FieldType.LOOKUP, req, false, false, grid, true, null, null, lookup);
    }
    private static FieldDescriptor lookupText(String api, String col, String label, String lookup, boolean req, boolean grid, int max) {
        return f(api, col, label, FieldType.LOOKUP, req, false, false, grid, true, max, null, lookup);
    }
    private static FieldDescriptor readKeyNumber(String api, String col, String label, boolean grid) {
        return f(api, col, label, FieldType.NUMBER, true, true, true, grid, true, null, null, null);
    }
    private static FieldDescriptor readNumber(String api, String col, String label, boolean grid) {
        return f(api, col, label, FieldType.NUMBER, false, false, true, grid, true, null, null, null);
    }
    private static FieldDescriptor readText(String api, String col, String label, boolean grid, boolean search) {
        return f(api, col, label, FieldType.TEXT, false, false, true, grid, search, null, null, null);
    }
    private static FieldDescriptor readDate(String api, String col, String label, boolean grid, boolean search) {
        return f(api, col, label, FieldType.DATE, false, false, true, grid, search, null, null, null);
    }
    private static FieldDescriptor readTimestamp(String api, String col, String label, boolean grid) {
        return f(api, col, label, FieldType.TIMESTAMP, false, false, true, grid, true, null, null, null);
    }
    private static FieldDescriptor readBoolean(String api, String col, String label, boolean grid) {
        return f(api, col, label, FieldType.BOOLEAN, false, false, true, grid, false, null, null, null);
    }
    private static FieldDescriptor f(String api, String col, String label, FieldType type, boolean req, boolean key,
                                     boolean readOnly, boolean grid, boolean search, Integer max, Object def, String lookup) {
        return new FieldDescriptor(api, col, label, type, req, key, readOnly, grid, search, max, def, lookup, List.of());
    }
    private static SelectOption option(Object value, String label) { return new SelectOption(value, label); }
}
