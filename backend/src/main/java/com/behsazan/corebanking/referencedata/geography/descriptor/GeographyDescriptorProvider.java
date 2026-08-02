package com.behsazan.corebanking.referencedata.geography.descriptor;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorProvider;
import com.behsazan.corebanking.referencedata.descriptor.domain.FieldType;
import com.behsazan.corebanking.referencedata.descriptor.domain.ParentDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceFieldDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.SelectOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(10)
public class GeographyDescriptorProvider implements ReferenceDescriptorProvider {
    @Value("${core-banking.schemas.reference-data:GEO}")
    private String schemaName = "GEO";

    @Override
    public List<ReferenceTableDescriptor> descriptors() {
        return List.of(provinces(), counties(), districts(), cities(), ruralDistricts(), villages());
    }

    private ReferenceTableDescriptor provinces() {
        return descriptor(
                "provinces", "GEOGRAPHY", "استان‌ها", "map",
                "PROVINCES", "SEQ_PROVINCES", "provinceId", "PROVINCE_ID",
                "provinceCode", "provinceName", null,
                List.of(
                        id("provinceId", "PROVINCE_ID", "شناسه"),
                        text("provinceCode", "PROVINCE_CODE", "کد استان", true, true, true, 3),
                        text("provinceName", "PROVINCE_NAME", "نام استان", true, true, true, 100),
                        text("provinceEnglishName", "PROVINCE_ENGLISH_NAME", "نام انگلیسی", false, false, true, 150),
                        text("diallingCode", "DIALLING_CODE", "کد تلفن", false, false, false, 10),
                        number("countryId", "COUNTRY_ID", "شناسه کشور", true, false, 71L),
                        number("censusHousehold", "CENSUS_HOUSEHOLD", "تعداد خانوار", false, false, null),
                        number("censusPopulation", "CENSUS_POPULATION", "جمعیت", false, true, null),
                        number("censusMale", "CENSUS_MALE", "جمعیت مرد", false, false, null),
                        number("censusFemale", "CENSUS_FEMALE", "جمعیت زن", false, false, null),
                        select("geoType", "GEO_TYPE", "نوع جغرافیایی", true, false, 1L,
                                List.of(new SelectOption(1, "استان"), new SelectOption(0, "سایر"))),
                        bool("isActive", "IS_ACTIVE", "وضعیت", true, true, true),
                        number("sortOrder", "SORT_ORDER", "ترتیب نمایش", true, false, 0L),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", FieldType.NUMBER),
                        audit("createdDate", "CREATED_DATE", "تاریخ ایجاد", FieldType.TIMESTAMP),
                        audit("lastModifiedBy", "LAST_MODIFIED_BY", "ویرایش‌کننده", FieldType.NUMBER),
                        audit("lastModifiedDate", "LAST_MODIFIED_DATE", "تاریخ ویرایش", FieldType.TIMESTAMP)
                )
        );
    }

    private ReferenceTableDescriptor counties() {
        return childDescriptor(
                "counties", "شهرستان‌ها", "apartment", "COUNTIES", "SEQ_COUNTIES",
                "countyId", "COUNTY_ID", "countyCode", "COUNTY_CODE", "countyName", "COUNTY_NAME",
                "countyEnglishName", "COUNTY_ENGLISH_NAME",
                new ParentDescriptor("provinces", "provinceId", "PROVINCE_ID", "استان"),
                2, "شهرستان", 4
        );
    }

    private ReferenceTableDescriptor districts() {
        return childDescriptor(
                "districts", "بخش‌ها", "layers", "DISTRICTS", "SEQ_DISTRICTS",
                "districtId", "DISTRICT_ID", "districtCode", "DISTRICT_CODE", "districtName", "DISTRICT_NAME",
                "districtEnglishName", "DISTRICT_ENGLISH_NAME",
                new ParentDescriptor("counties", "countyId", "COUNTY_ID", "شهرستان"),
                3, "بخش", 6
        );
    }

    private ReferenceTableDescriptor cities() {
        return childDescriptor(
                "cities", "شهرها", "location_city", "CITIES", "SEQ_CITIES",
                "cityId", "CITY_ID", "cityCode", "CITY_CODE", "cityName", "CITY_NAME",
                "cityEnglishName", "CITY_ENGLISH_NAME",
                new ParentDescriptor("districts", "districtId", "DISTRICT_ID", "بخش"),
                5, "شهر", 10
        );
    }

    private ReferenceTableDescriptor ruralDistricts() {
        return childDescriptor(
                "rural-districts", "دهستان‌ها", "holiday_village", "RURAL_DISTRICTS", "SEQ_RURAL_DISTRICTS",
                "ruralDistrictId", "RURAL_DISTRICT_ID", "ruralDistrictCode", "RURAL_DISTRICT_CODE",
                "ruralDistrictName", "RURAL_DISTRICT_NAME", "ruralDistrictEnglishName", "RURAL_DISTRICT_ENGLISH_NAME",
                new ParentDescriptor("districts", "districtId", "DISTRICT_ID", "بخش"),
                4, "دهستان", 10
        );
    }

    private ReferenceTableDescriptor villages() {
        ReferenceTableDescriptor base = childDescriptor(
                "villages", "روستاها/آبادی‌ها", "cottage", "VILLAGES", "SEQ_VILLAGES",
                "villageId", "VILLAGE_ID", "villageCode", "VILLAGE_CODE", "villageName", "VILLAGE_NAME",
                "villageEnglishName", "VILLAGE_ENGLISH_NAME",
                new ParentDescriptor("rural-districts", "ruralDistrictId", "RURAL_DISTRICT_ID", "دهستان"),
                6, "روستا", 16
        );
        List<ReferenceFieldDescriptor> fields = base.fields().stream()
                .map(field -> field.apiName().equals("geoType")
                        ? select("geoType", "GEO_TYPE", "نوع آبادی", true, true, 6L,
                        List.of(new SelectOption(6, "روستا"), new SelectOption(8, "آبادی")))
                        : field)
                .toList();
        return new ReferenceTableDescriptor(
                base.resource(), base.category(), base.title(), base.icon(), base.schemaName(), base.tableName(),
                base.sequenceName(), base.idApiName(), base.idColumnName(), base.codeApiName(), base.nameApiName(),
                base.parent(), fields
        );
    }

    private ReferenceTableDescriptor childDescriptor(
            String resource, String title, String icon, String table, String sequence,
            String idApi, String idColumn, String codeApi, String codeColumn, String nameApi, String nameColumn,
            String englishApi, String englishColumn, ParentDescriptor parent,
            int geoType, String geoTypeLabel, int codeLength
    ) {
        return descriptor(
                resource, "GEOGRAPHY", title, icon, table, sequence, idApi, idColumn, codeApi, nameApi, parent,
                List.of(
                        id(idApi, idColumn, "شناسه"),
                        text(codeApi, codeColumn, "کد", true, true, true, codeLength),
                        text(nameApi, nameColumn, "نام", true, true, true, resource.equals("villages") ? 250 : 150),
                        text(englishApi, englishColumn, "نام انگلیسی", false, false, true, 150),
                        lookup(parent.apiField(), parent.columnName(), parent.label(), parent.resource()),
                        number("censusHousehold", "CENSUS_HOUSEHOLD", "تعداد خانوار", false, false, null),
                        number("censusPopulation", "CENSUS_POPULATION", "جمعیت", false, true, null),
                        number("censusMale", "CENSUS_MALE", "جمعیت مرد", false, false, null),
                        number("censusFemale", "CENSUS_FEMALE", "جمعیت زن", false, false, null),
                        select("geoType", "GEO_TYPE", "نوع جغرافیایی", true, false, (long) geoType,
                                List.of(new SelectOption(geoType, geoTypeLabel))),
                        bool("isActive", "IS_ACTIVE", "وضعیت", true, true, true),
                        number("sortOrder", "SORT_ORDER", "ترتیب نمایش", true, false, 0L),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", FieldType.NUMBER),
                        audit("createdDate", "CREATED_DATE", "تاریخ ایجاد", FieldType.TIMESTAMP),
                        audit("lastModifiedBy", "LAST_MODIFIED_BY", "ویرایش‌کننده", FieldType.NUMBER),
                        audit("lastModifiedDate", "LAST_MODIFIED_DATE", "تاریخ ویرایش", FieldType.TIMESTAMP)
                )
        );
    }

    private ReferenceTableDescriptor descriptor(
            String resource, String category, String title, String icon,
            String table, String sequence, String idApi, String idColumn,
            String codeApi, String nameApi, ParentDescriptor parent,
            List<ReferenceFieldDescriptor> fields
    ) {
        return new ReferenceTableDescriptor(
                resource, category, title, icon, schemaName, table, sequence,
                idApi, idColumn, codeApi, nameApi, parent, fields
        );
    }

    private static ReferenceFieldDescriptor id(String api, String column, String label) {
        return new ReferenceFieldDescriptor(api, column, label, FieldType.NUMBER, false, true, true, false,
                null, null, null, List.of());
    }

    private static ReferenceFieldDescriptor text(String api, String column, String label,
                                                  boolean required, boolean grid, boolean searchable, int max) {
        return new ReferenceFieldDescriptor(api, column, label, FieldType.TEXT, required, false, grid, searchable,
                max, null, null, List.of());
    }

    private static ReferenceFieldDescriptor number(String api, String column, String label,
                                                    boolean required, boolean grid, Object defaultValue) {
        return new ReferenceFieldDescriptor(api, column, label, FieldType.NUMBER, required, false, grid, false,
                null, defaultValue, null, List.of());
    }

    private static ReferenceFieldDescriptor bool(String api, String column, String label,
                                                  boolean required, boolean grid, boolean defaultValue) {
        return new ReferenceFieldDescriptor(api, column, label, FieldType.BOOLEAN, required, false, grid, false,
                null, defaultValue, null, List.of());
    }

    private static ReferenceFieldDescriptor select(String api, String column, String label,
                                                    boolean required, boolean grid, Object defaultValue,
                                                    List<SelectOption> options) {
        return new ReferenceFieldDescriptor(api, column, label, FieldType.SELECT, required, false, grid, false,
                null, defaultValue, null, options);
    }

    private static ReferenceFieldDescriptor lookup(String api, String column, String label, String resource) {
        return new ReferenceFieldDescriptor(api, column, label, FieldType.LOOKUP, true, false, false, false,
                null, null, resource, List.of());
    }

    private static ReferenceFieldDescriptor audit(String api, String column, String label, FieldType type) {
        return new ReferenceFieldDescriptor(api, column, label, type, false, true, false, false,
                null, null, null, List.of());
    }
}
