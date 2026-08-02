package com.behsazan.corebanking.referencedata.general.descriptor;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorProvider;
import com.behsazan.corebanking.referencedata.descriptor.domain.ParentDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceFieldDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.SelectOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorSupport.*;

@Component
@Order(20)
public class GeneralReferenceDescriptorProvider implements ReferenceDescriptorProvider {
    @Value("${core-banking.schemas.reference-data:GEO}")
    private String schemaName = "GEO";

    @Override
    public List<ReferenceTableDescriptor> descriptors() {
        return List.of(
                continents(), languages(), currencies(), countries(),
                bloodTypes(), banks(), foreignCities()
        );
    }

    private ReferenceTableDescriptor continents() {
        return descriptor(
                "continents", "GENERAL", "قاره‌ها", "public",
                schemaName, "CONTINENTS", "SEQ_CONTINENTS",
                "continentId", "CONTINENT_ID", "continentCode", "continentName", null,
                fields(
                        id("continentId", "CONTINENT_ID", "شناسه"),
                        text("continentCode", "CONTINENT_CODE", "کد قاره", true, true, true, 10),
                        text("continentName", "CONTINENT_NAME", "نام قاره", true, true, true, 100),
                        text("continentEnglishName", "CONTINENT_ENGLISH_NAME", "نام انگلیسی", true, true, true, 100)
                )
        );
    }

    private ReferenceTableDescriptor languages() {
        return descriptor(
                "languages", "GENERAL", "زبان‌ها", "translate",
                schemaName, "LANGUAGES", "SEQ_LANGUAGES",
                "languageId", "LANGUAGE_ID", "languageIsoCode", "languageName", null,
                fields(
                        id("languageId", "LANGUAGE_ID", "شناسه"),
                        text("languageIsoCode", "LANGUAGE_ISO_CODE", "کد ISO زبان", true, true, true, 10),
                        text("languageName", "LANGUAGE_NAME", "نام زبان", true, true, true, 100),
                        text("languageEnglishName", "LANGUAGE_ENGLISH_NAME", "نام انگلیسی", true, true, true, 100),
                        bool("isActive", "IS_ACTIVE", "وضعیت", true, true, true),
                        number("sortOrder", "SORT_ORDER", "ترتیب نمایش", true, false, 0L)
                )
        );
    }

    private ReferenceTableDescriptor currencies() {
        return descriptor(
                "currencies", "GENERAL", "ارزها", "currency_exchange",
                schemaName, "CURRENCIES", "SEQ_CURRENCIES",
                "currencyId", "CURRENCY_ID", "currencyAlphabeticIso", "currencyName", null,
                fields(
                        id("currencyId", "CURRENCY_ID", "شناسه"),
                        text("currencyAlphabeticIso", "CURRENCY_ALPHABETIC_ISO", "کد الفبایی ISO", true, true, true, 10),
                        text("currencyNumericIso", "CURRENCY_NUMERIC_ISO", "کد عددی ISO", true, true, true, 10),
                        text("currencyName", "CURRENCY_NAME", "نام ارز", true, true, true, 150),
                        text("currencyEnglishName", "CURRENCY_ENGLISH_NAME", "نام انگلیسی", true, true, true, 150),
                        text("currencySymbol", "CURRENCY_SYMBOL", "نماد ارز", false, false, false, 20),
                        number("floatingPoint", "FLOATING_POINT", "تعداد ارقام اعشار", true, false, 0L),
                        bool("isBaseCurrency", "IS_BASE_CURRENCY", "ارز پایه", true, true, false),
                        bool("isDefaultCurrency", "IS_DEFAULT_CURRENCY", "ارز پیش‌فرض", true, true, false),
                        select("conversionMethod", "CONVERSION_METHOD", "روش تبدیل", true, false, 1L,
                                List.of(new SelectOption(1, "مستقیم"), new SelectOption(2, "معکوس"))),
                        bool("isActive", "IS_ACTIVE", "وضعیت", true, true, true),
                        number("sortOrder", "SORT_ORDER", "ترتیب نمایش", true, false, 0L)
                )
        );
    }

    private ReferenceTableDescriptor countries() {
        ParentDescriptor parent = new ParentDescriptor("continents", "continentId", "CONTINENT_ID", "قاره");
        return descriptor(
                "countries", "GENERAL", "کشورها", "flag",
                schemaName, "COUNTRIES", "SEQ_COUNTRIES",
                "countryId", "COUNTRY_ID", "countryIsoCode", "countryName", parent,
                fields(
                        id("countryId", "COUNTRY_ID", "شناسه"),
                        text("countryIsoCode", "COUNTRY_ISO_CODE", "کد ISO سه‌حرفی", true, true, true, 10),
                        text("countryIsoCode2", "COUNTRY_ISO_CODE2", "کد ISO دوحرفی", true, true, true, 10),
                        text("countryName", "COUNTRY_NAME", "نام کشور", true, true, true, 150),
                        text("countryEnglishName", "COUNTRY_ENGLISH_NAME", "نام انگلیسی", true, true, true, 150),
                        text("countryFullName", "COUNTRY_FULL_NAME", "نام کامل", true, false, true, 200),
                        text("countryEnglishFullName", "COUNTRY_ENGLISH_FULL_NAME", "نام کامل انگلیسی", false, false, true, 200),
                        lookup("continentId", "CONTINENT_ID", "قاره", "continents", true, false),
                        number("diallingCode", "DIALLING_CODE", "کد تلفن", false, false, null),
                        bool("isDefaultCountry", "IS_DEFAULT_COUNTRY", "کشور پیش‌فرض", true, true, false),
                        lookup("currencyId", "CURRENCY_ID", "ارز", "currencies", true, false),
                        lookup("officialLanguageId", "OFFICIAL_LANGUAGE_ID", "زبان رسمی", "languages", true, false),
                        text("nationality", "NATIONALITY", "ملیت", true, false, true, 150),
                        text("englishNationality", "ENGLISH_NATIONALITY", "ملیت انگلیسی", true, false, true, 150),
                        text("countryRegion", "COUNTRY_REGION", "منطقه کشور", false, false, true, 100),
                        bool("isActive", "IS_ACTIVE", "وضعیت", true, true, true),
                        number("sortOrder", "SORT_ORDER", "ترتیب نمایش", true, false, 0L)
                )
        );
    }

    private ReferenceTableDescriptor bloodTypes() {
        return descriptor(
                "blood-types", "GENERAL", "گروه‌های خونی", "bloodtype",
                schemaName, "BLOOD_TYPES", "SEQ_BLOOD_TYPES",
                "bloodTypeId", "BLOOD_TYPE_ID", "bloodTypeCode", "bloodTypeName", null,
                fields(
                        id("bloodTypeId", "BLOOD_TYPE_ID", "شناسه"),
                        text("bloodTypeCode", "BLOOD_TYPE_CODE", "کد گروه خونی", true, true, true, 10),
                        text("bloodTypeName", "BLOOD_TYPE_NAME", "نام گروه خونی", true, true, true, 100),
                        text("bloodTypeEnglishName", "BLOOD_TYPE_ENGLISH_NAME", "نام انگلیسی", true, true, true, 100)
                )
        );
    }

    private ReferenceTableDescriptor banks() {
        ParentDescriptor parent = new ParentDescriptor("countries", "countryId", "COUNTRY_ID", "کشور");
        return descriptor(
                "banks", "GENERAL", "بانک‌ها", "account_balance",
                schemaName, "BANKS", "SEQ_BANKS",
                "bankId", "BANK_ID", "bankCode", "bankName", parent,
                fields(
                        id("bankId", "BANK_ID", "شناسه"),
                        text("bankCode", "BANK_CODE", "کد بانک", true, true, true, 20),
                        text("bankName", "BANK_NAME", "نام بانک", true, true, true, 150),
                        text("bankEnglishName", "BANK_ENGLISH_NAME", "نام انگلیسی", false, true, true, 150),
                        lookup("countryId", "COUNTRY_ID", "کشور", "countries", true, false),
                        text("bankUrl", "BANK_URL", "نشانی وب‌سایت", false, false, false, 500),
                        bool("isActive", "IS_ACTIVE", "وضعیت", true, true, true),
                        number("sortOrder", "SORT_ORDER", "ترتیب نمایش", false, false, null)
                )
        );
    }

    private ReferenceTableDescriptor foreignCities() {
        ParentDescriptor parent = new ParentDescriptor("countries", "countryId", "COUNTRY_ID", "کشور");
        return descriptor(
                "foreign-cities", "GENERAL", "شهرهای خارجی", "travel_explore",
                schemaName, "FOREIGN_CITIES", "SEQ_FOREIGN_CITIES",
                "foreignCityId", "FOREIGN_CITY_ID", "foreignCityCode", "foreignCityName", parent,
                fields(
                        id("foreignCityId", "FOREIGN_CITY_ID", "شناسه"),
                        text("foreignCityCode", "FOREIGN_CITY_CODE", "کد شهر", true, true, true, 20),
                        text("foreignCityName", "FOREIGN_CITY_NAME", "نام شهر", true, true, true, 200),
                        text("foreignCityEnglishName", "FOREIGN_CITY_ENGLISH_NAME", "نام انگلیسی", false, true, true, 200),
                        lookup("countryId", "COUNTRY_ID", "کشور", "countries", true, false),
                        bool("isCapital", "IS_CAPITAL", "پایتخت", true, true, false),
                        bool("isMetropolis", "IS_METROPOLIS", "کلان‌شهر", true, true, false),
                        bool("isActive", "IS_ACTIVE", "وضعیت", true, true, true),
                        number("sortOrder", "SORT_ORDER", "ترتیب نمایش", false, false, null)
                )
        );
    }

    private static List<ReferenceFieldDescriptor> fields(ReferenceFieldDescriptor... mainFields) {
        List<ReferenceFieldDescriptor> result = new ArrayList<>(List.of(mainFields));
        result.addAll(standardAudits());
        return List.copyOf(result);
    }
}
