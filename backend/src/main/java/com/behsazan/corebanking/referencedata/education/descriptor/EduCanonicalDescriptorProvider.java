package com.behsazan.corebanking.referencedata.education.descriptor;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorProvider;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceFieldDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.SelectOption;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorSupport.*;

/**
 * Canonical EDU reference model.
 *
 * Kept separate from the legacy GEO education descriptors so both generations
 * of forms remain available during prototype migration.
 */
@Component
@Order(41)
public class EduCanonicalDescriptorProvider implements ReferenceDescriptorProvider {
    private static final String SCHEMA = "EDU";
    private static final String CATEGORY = "EDU_REFERENCE";

    private static final List<SelectOption> YES_NO = List.of(
            new SelectOption("Y", "بله"),
            new SelectOption("N", "خیر")
    );

    private static final List<SelectOption> EDUCATION_SYSTEMS = List.of(
            new SelectOption("GENERAL", "آموزش عمومی"),
            new SelectOption("ACADEMIC", "دانشگاهی"),
            new SelectOption("VOCATIONAL", "فنی و حرفه‌ای / مهارتی"),
            new SelectOption("TECHNICAL_VOCATIONAL", "فنی و حرفه‌ای"),
            new SelectOption("SEMINARY", "حوزوی"),
            new SelectOption("OTHER", "سایر")
    );

    @Override
    public List<ReferenceTableDescriptor> descriptors() {
        return List.of(levels(), fields(), fieldLevels(), institutions(), sources(), sourceMappings());
    }

    private ReferenceTableDescriptor levels() {
        return descriptor(
                "edu-education-levels", CATEGORY, "مقاطع و مدارک تحصیلی", "school",
                SCHEMA, "EDUCATION_LEVELS", "SEQ_EDUCATION_LEVELS",
                "educationLevelsId", "EDUCATION_LEVELS_ID", "levelCode", "nameFa", null,
                List.of(
                        id("educationLevelsId", "EDUCATION_LEVELS_ID", "شناسه"),
                        stringSelect("educationSystemCode", "EDUCATION_SYSTEM_CODE", "نظام آموزشی", true, true, "ACADEMIC", EDUCATION_SYSTEMS),
                        text("levelCode", "LEVEL_CODE", "کد مقطع", true, true, true, 60),
                        text("nameFa", "NAME_FA", "عنوان فارسی", true, true, true, 200),
                        text("nameEn", "NAME_EN", "عنوان انگلیسی", false, false, true, 200),
                        number("levelOrder", "LEVEL_ORDER", "ترتیب نمایش", true, true, 0),
                        stringSelect("isSelectable", "IS_SELECTABLE", "قابل انتخاب", true, true, "Y", YES_NO),
                        stringSelect("activeFlag", "ACTIVE_FLAG", "وضعیت فعال", true, true, "Y", YES_NO),
                        date("validFrom", "VALID_FROM", "معتبر از", false, false),
                        date("validTo", "VALID_TO", "معتبر تا", false, false)
                )
        );
    }

    private ReferenceTableDescriptor fields() {
        return descriptor(
                "edu-education-fields", CATEGORY, "رشته‌ها و گرایش‌های تحصیلی", "menu_book",
                SCHEMA, "EDUCATION_FIELDS", "SEQ_EDUCATION_FIELDS",
                "educationFieldsId", "EDUCATION_FIELDS_ID", "fieldCode", "nameFa", null,
                List.of(
                        id("educationFieldsId", "EDUCATION_FIELDS_ID", "شناسه"),
                        lookup("parentEducationFieldsId", "PARENT_EDUCATION_FIELDS_ID", "رشته/گروه والد", "edu-education-fields", false, false),
                        stringSelect("educationSystemCode", "EDUCATION_SYSTEM_CODE", "نظام آموزشی", true, true, "ACADEMIC", EDUCATION_SYSTEMS),
                        text("fieldCode", "FIELD_CODE", "کد رشته", true, true, true, 80),
                        stringSelect("fieldNodeTypeCode", "FIELD_NODE_TYPE_CODE", "نوع گره", true, true, "FIELD", List.of(
                                new SelectOption("GROUP", "گروه"),
                                new SelectOption("FIELD", "رشته"),
                                new SelectOption("SPECIALIZATION", "گرایش")
                        )),
                        text("nameFa", "NAME_FA", "عنوان فارسی", true, true, true, 300),
                        text("nameEn", "NAME_EN", "عنوان انگلیسی", false, false, true, 300),
                        text("normalizedNameFa", "NORMALIZED_NAME_FA", "عنوان نرمال‌شده", true, false, true, 300),
                        stringSelect("isSelectable", "IS_SELECTABLE", "قابل انتخاب", true, true, "Y", YES_NO),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 0),
                        stringSelect("activeFlag", "ACTIVE_FLAG", "وضعیت فعال", true, true, "Y", YES_NO),
                        date("validFrom", "VALID_FROM", "معتبر از", false, false),
                        date("validTo", "VALID_TO", "معتبر تا", false, false)
                )
        );
    }

    private ReferenceTableDescriptor fieldLevels() {
        return descriptor(
                "edu-education-field-levels", CATEGORY, "مقاطع معتبر هر رشته", "rule",
                SCHEMA, "EDUCATION_FIELD_LEVELS", "SEQ_EDUCATION_FIELD_LEVELS",
                "educationFieldLevelsId", "EDUCATION_FIELD_LEVELS_ID", "educationFieldsId", "educationFieldsId", null,
                List.of(
                        id("educationFieldLevelsId", "EDUCATION_FIELD_LEVELS_ID", "شناسه"),
                        lookup("educationFieldsId", "EDUCATION_FIELDS_ID", "رشته تحصیلی", "edu-education-fields", true, true),
                        lookup("educationLevelsId", "EDUCATION_LEVELS_ID", "مقطع تحصیلی", "edu-education-levels", true, true),
                        lookup("sourceId", "SOURCE_ID", "منبع", "edu-education-sources", false, false),
                        stringSelect("activeFlag", "ACTIVE_FLAG", "وضعیت فعال", true, true, "Y", YES_NO),
                        date("validFrom", "VALID_FROM", "معتبر از", false, false),
                        date("validTo", "VALID_TO", "معتبر تا", false, false)
                )
        );
    }

    private ReferenceTableDescriptor institutions() {
        return descriptor(
                "edu-education-institutions", CATEGORY, "دانشگاه‌ها و مؤسسات آموزشی", "domain",
                SCHEMA, "EDUCATION_INSTITUTIONS", "SEQ_EDUCATION_INSTITUTIONS",
                "educationInstitutionsId", "EDUCATION_INSTITUTIONS_ID", "institutionCode", "nameFa", null,
                List.of(
                        id("educationInstitutionsId", "EDUCATION_INSTITUTIONS_ID", "شناسه"),
                        lookup("parentInstitutionsId", "PARENT_INSTITUTIONS_ID", "مؤسسه والد", "edu-education-institutions", false, false),
                        lookup("successorInstitutionsId", "SUCCESSOR_INSTITUTIONS_ID", "مؤسسه جانشین", "edu-education-institutions", false, false),
                        text("institutionCode", "INSTITUTION_CODE", "کد مؤسسه", true, true, true, 80),
                        stringSelect("institutionTypeCode", "INSTITUTION_TYPE_CODE", "نوع مؤسسه", true, true, "UNIVERSITY", List.of(
                                new SelectOption("UNIVERSITY", "دانشگاه"),
                                new SelectOption("MEDICAL_UNIVERSITY", "دانشگاه علوم پزشکی"),
                                new SelectOption("UNIVERSITY_BRANCH", "واحد / شعبه دانشگاه"),
                                new SelectOption("COLLEGE", "دانشکده / کالج"),
                                new SelectOption("MEDICAL_SCHOOL", "دانشکده علوم پزشکی"),
                                new SelectOption("HIGHER_EDUCATION_INSTITUTE", "مؤسسه آموزش عالی"),
                                new SelectOption("APPLIED_SCIENCE_CENTER", "مرکز علمی کاربردی"),
                                new SelectOption("SEMINARY", "حوزه / مرکز حوزوی"),
                                new SelectOption("OTHER", "سایر")
                        )),
                        stringSelect("educationSystemCode", "EDUCATION_SYSTEM_CODE", "نظام آموزشی", true, true, "ACADEMIC", EDUCATION_SYSTEMS),
                        text("nameFa", "NAME_FA", "نام فارسی", true, true, true, 300),
                        text("nameEn", "NAME_EN", "نام انگلیسی", false, false, true, 300),
                        text("normalizedNameFa", "NORMALIZED_NAME_FA", "نام نرمال‌شده", true, false, true, 300),
                        number("countryId", "COUNTRY_ID", "شناسه کشور", false, false, null),
                        number("provinceId", "PROVINCE_ID", "شناسه استان", false, false, null),
                        number("cityId", "CITY_ID", "شناسه شهر", false, false, null),
                        stringSelect("institutionStatusCode", "INSTITUTION_STATUS_CODE", "وضعیت مؤسسه", true, true, "NEEDS_REVIEW", List.of(
                                new SelectOption("ACTIVE", "فعال"),
                                new SelectOption("HISTORICAL", "تاریخی"),
                                new SelectOption("RENAMED", "تغییرنام‌یافته"),
                                new SelectOption("MERGED", "ادغام‌شده"),
                                new SelectOption("CLOSED", "بسته / خاتمه‌یافته"),
                                new SelectOption("NEEDS_REVIEW", "نیازمند بررسی")
                        )),
                        text("officialWebsite", "OFFICIAL_WEBSITE", "وب‌سایت رسمی", false, false, false, 1000),
                        stringSelect("isDegreeGranting", "IS_DEGREE_GRANTING", "اعطاکننده مدرک", true, true, "Y", YES_NO),
                        stringSelect("activeFlag", "ACTIVE_FLAG", "وضعیت فعال", true, true, "Y", YES_NO),
                        date("validFrom", "VALID_FROM", "معتبر از", false, false),
                        date("validTo", "VALID_TO", "معتبر تا", false, false)
                )
        );
    }

    private ReferenceTableDescriptor sources() {
        return descriptor(
                "edu-education-sources", CATEGORY, "منابع داده آموزشی", "source",
                SCHEMA, "EDUCATION_SOURCES", "SEQ_EDUCATION_SOURCES",
                "sourceId", "SOURCE_ID", "sourceCode", "sourceAuthorityNameFa", null,
                List.of(
                        id("sourceId", "SOURCE_ID", "شناسه"),
                        text("sourceCode", "SOURCE_CODE", "کد منبع", true, true, true, 100),
                        text("sourceAuthorityNameFa", "SOURCE_AUTHORITY_NAME_FA", "مرجع / سازمان منبع", true, true, true, 300),
                        text("sourceDocumentTitle", "SOURCE_DOCUMENT_TITLE", "عنوان سند", false, true, true, 500),
                        stringSelect("sourceTypeCode", "SOURCE_TYPE_CODE", "نوع منبع", true, true, "OTHER", List.of(
                                new SelectOption("OFFICIAL_GUIDEBOOK", "دفترچه رسمی"),
                                new SelectOption("OFFICIAL_REGULATION", "مصوبه / مقرره رسمی"),
                                new SelectOption("OFFICIAL_DIRECTORY", "فهرست رسمی"),
                                new SelectOption("INSTITUTION_WEBSITE", "وب‌سایت مؤسسه"),
                                new SelectOption("LEGACY_DATASET", "داده Legacy"),
                                new SelectOption("INTERNATIONAL_STANDARD", "استاندارد بین‌المللی"),
                                new SelectOption("OTHER", "سایر")
                        )),
                        number("sourceYear", "SOURCE_YEAR", "سال منبع", false, true, null),
                        text("versionCode", "VERSION_CODE", "نسخه", false, false, true, 60),
                        date("publicationDate", "PUBLICATION_DATE", "تاریخ انتشار", false, false),
                        text("sourceUri", "SOURCE_URI", "نشانی منبع", false, false, false, 1000),
                        number("sourcePriority", "SOURCE_PRIORITY", "اولویت منبع", true, true, 100),
                        stringSelect("activeFlag", "ACTIVE_FLAG", "وضعیت فعال", true, true, "Y", YES_NO)
                )
        );
    }

    private ReferenceTableDescriptor sourceMappings() {
        return descriptor(
                "edu-education-source-mappings", CATEGORY, "نگاشت منابع به داده Canonical", "compare_arrows",
                SCHEMA, "EDUCATION_SOURCE_MAPPINGS", "SEQ_EDUCATION_SOURCE_MAPPINGS",
                "sourceMappingId", "SOURCE_MAPPING_ID", "sourceEntityCode", "sourceNameFa", null,
                List.of(
                        id("sourceMappingId", "SOURCE_MAPPING_ID", "شناسه"),
                        lookup("sourceId", "SOURCE_ID", "منبع", "edu-education-sources", true, true),
                        stringSelect("entityTypeCode", "ENTITY_TYPE_CODE", "نوع موجودیت", true, true, "FIELD", List.of(
                                new SelectOption("LEVEL", "مقطع"),
                                new SelectOption("FIELD", "رشته"),
                                new SelectOption("INSTITUTION", "مؤسسه آموزشی")
                        )),
                        lookup("educationLevelsId", "EDUCATION_LEVELS_ID", "مقطع Canonical", "edu-education-levels", false, false),
                        lookup("educationFieldsId", "EDUCATION_FIELDS_ID", "رشته Canonical", "edu-education-fields", false, false),
                        lookup("educationInstitutionsId", "EDUCATION_INSTITUTIONS_ID", "مؤسسه Canonical", "edu-education-institutions", false, false),
                        text("sourceEntityCode", "SOURCE_ENTITY_CODE", "کد موجودیت در منبع", false, true, true, 100),
                        text("sourceNameFa", "SOURCE_NAME_FA", "عنوان فارسی در منبع", true, true, true, 500),
                        text("sourceNameEn", "SOURCE_NAME_EN", "عنوان انگلیسی در منبع", false, false, true, 500),
                        stringSelect("mappingTypeCode", "MAPPING_TYPE_CODE", "نوع نگاشت", true, true, "UNMAPPED", List.of(
                                new SelectOption("EXACT_NAME", "نام دقیق"),
                                new SelectOption("NORMALIZED_NAME", "نام نرمال‌شده"),
                                new SelectOption("ALIAS", "نام جایگزین"),
                                new SelectOption("FORMER_NAME", "نام سابق"),
                                new SelectOption("EXTERNAL_CODE", "کد خارجی"),
                                new SelectOption("MANUAL", "تطبیق دستی"),
                                new SelectOption("UNMAPPED", "بدون نگاشت")
                        )),
                        stringSelect("matchStatusCode", "MATCH_STATUS_CODE", "وضعیت تطبیق", true, true, "NEEDS_REVIEW", List.of(
                                new SelectOption("AUTO_MATCHED", "تطبیق خودکار"),
                                new SelectOption("CONFIRMED", "تأییدشده"),
                                new SelectOption("NEEDS_REVIEW", "نیازمند بررسی"),
                                new SelectOption("UNMAPPED", "بدون نگاشت")
                        )),
                        date("validFrom", "VALID_FROM", "معتبر از", false, false),
                        date("validTo", "VALID_TO", "معتبر تا", false, false),
                        stringSelect("activeFlag", "ACTIVE_FLAG", "وضعیت فعال", true, true, "Y", YES_NO)
                )
        );
    }
}
