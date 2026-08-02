package com.behsazan.corebanking.referencedata.education.descriptor;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorProvider;
import com.behsazan.corebanking.referencedata.descriptor.domain.ParentDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceFieldDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorSupport.*;

@Component
@Order(40)
public class EducationDescriptorProvider implements ReferenceDescriptorProvider {
    @Value("${core-banking.schemas.reference-data:GEO}")
    private String schemaName = "GEO";

    @Override
    public List<ReferenceTableDescriptor> descriptors() {
        return List.of(groups(), subgroups(), degrees(), fields(), universities());
    }

    private ReferenceTableDescriptor groups() {
        return descriptor(
                "education-groups", "EDUCATION", "گروه‌های تحصیلی", "category",
                schemaName, "EDUCATION_GROUPS", "SEQ_EDUCATION_GROUPS",
                "educationGroupId", "EDUCATION_GROUP_ID", "educationGroupCode", "educationGroupName", null,
                withAudits(
                        id("educationGroupId", "EDUCATION_GROUP_ID", "شناسه"),
                        text("educationGroupCode", "EDUCATION_GROUP_CODE", "کد گروه", true, true, true, 20),
                        text("educationGroupName", "EDUCATION_GROUP_NAME", "نام گروه", true, true, true, 150),
                        text("educationGroupEnglishName", "EDUCATION_GROUP_ENGLISH_NAME", "نام انگلیسی", false, true, true, 150)
                )
        );
    }

    private ReferenceTableDescriptor subgroups() {
        ParentDescriptor parent = new ParentDescriptor(
                "education-groups", "educationGroupId", "EDUCATION_GROUP_ID", "گروه تحصیلی"
        );
        return descriptor(
                "education-subgroups", "EDUCATION", "زیرگروه‌های تحصیلی", "account_tree",
                schemaName, "EDUCATION_SUBGROUPS", "SEQ_EDUCATION_SUBGROUPS",
                "educationSubgroupId", "EDUCATION_SUBGROUP_ID", "educationSubgroupCode", "educationSubgroupName", parent,
                withAudits(
                        id("educationSubgroupId", "EDUCATION_SUBGROUP_ID", "شناسه"),
                        text("educationSubgroupCode", "EDUCATION_SUBGROUP_CODE", "کد زیرگروه", true, true, true, 20),
                        text("educationSubgroupName", "EDUCATION_SUBGROUP_NAME", "نام زیرگروه", true, true, true, 200),
                        text("educationSubgroupEnglishName", "EDUCATION_SUBGROUP_ENGLISH_NAME", "نام انگلیسی", false, true, true, 200),
                        lookup("educationGroupId", "EDUCATION_GROUP_ID", "گروه تحصیلی", "education-groups", true, false)
                )
        );
    }

    private ReferenceTableDescriptor degrees() {
        ParentDescriptor parent = new ParentDescriptor(
                "education-groups", "educationGroupId", "EDUCATION_GROUP_ID", "گروه تحصیلی"
        );
        return descriptor(
                "education-degrees", "EDUCATION", "مقاطع تحصیلی", "school",
                schemaName, "EDUCATION_DEGREES", "SEQ_EDUCATION_DEGREES",
                "educationDegreeId", "EDUCATION_DEGREE_ID", "educationDegreeCode", "educationDegreeName", parent,
                withAudits(
                        id("educationDegreeId", "EDUCATION_DEGREE_ID", "شناسه"),
                        text("educationDegreeCode", "EDUCATION_DEGREE_CODE", "کد مقطع", true, true, true, 20),
                        text("educationDegreeName", "EDUCATION_DEGREE_NAME", "نام مقطع", true, true, true, 150),
                        text("educationDegreeEnglishName", "EDUCATION_DEGREE_ENGLISH_NAME", "نام انگلیسی", false, true, true, 150),
                        lookup("educationGroupId", "EDUCATION_GROUP_ID", "گروه تحصیلی", "education-groups", true, false)
                )
        );
    }

    private ReferenceTableDescriptor fields() {
        ParentDescriptor parent = new ParentDescriptor(
                "education-subgroups", "educationSubgroupId", "EDUCATION_SUBGROUP_ID", "زیرگروه تحصیلی"
        );
        return descriptor(
                "education-fields", "EDUCATION", "رشته‌های تحصیلی", "menu_book",
                schemaName, "EDUCATION_FIELDS", "SEQ_EDUCATION_FIELDS",
                "educationFieldId", "EDUCATION_FIELD_ID", "educationFieldCode", "educationFieldName", parent,
                withAudits(
                        id("educationFieldId", "EDUCATION_FIELD_ID", "شناسه"),
                        text("educationFieldCode", "EDUCATION_FIELD_CODE", "کد رشته", true, true, true, 20),
                        text("educationFieldName", "EDUCATION_FIELD_NAME", "نام رشته", true, true, true, 300),
                        text("educationFieldEnglishName", "EDUCATION_FIELD_ENGLISH_NAME", "نام انگلیسی", false, true, true, 300),
                        lookup("educationSubgroupId", "EDUCATION_SUBGROUP_ID", "زیرگروه تحصیلی", "education-subgroups", true, false),
                        bool("isActive", "IS_ACTIVE", "وضعیت", true, true, true)
                )
        );
    }

    private ReferenceTableDescriptor universities() {
        ParentDescriptor parent = new ParentDescriptor("countries", "countryId", "COUNTRY_ID", "کشور");
        return descriptor(
                "education-universities", "EDUCATION", "دانشگاه‌ها", "domain",
                schemaName, "EDUCATION_UNIVERSITIES", "SEQ_EDUCATION_UNIVERSITIES",
                "educationUniversityId", "EDUCATION_UNIVERSITY_ID", "educationUniversityCode", "educationUniversityName", parent,
                withAudits(
                        id("educationUniversityId", "EDUCATION_UNIVERSITY_ID", "شناسه"),
                        text("educationUniversityCode", "EDUCATION_UNIVERSITY_CODE", "کد دانشگاه", true, true, true, 20),
                        text("educationUniversityName", "EDUCATION_UNIVERSITY_NAME", "نام دانشگاه", true, true, true, 300),
                        text("educationUniversityEnglishName", "EDUCATION_UNIVERSITY_ENGLISH_NAME", "نام انگلیسی", false, true, true, 300),
                        number("universityCategory", "UNIVERSITY_CATEGORY", "نوع دانشگاه", true, true, 1L),
                        number("universityRanking", "UNIVERSITY_RANKING", "رتبه دانشگاه", true, true, 1L),
                        lookup("countryId", "COUNTRY_ID", "کشور", "countries", true, false),
                        lookup("provinceId", "PROVINCE_ID", "استان", "provinces", false, false),
                        lookup("cityId", "CITY_ID", "شهر", "cities", false, false),
                        bool("isActive", "IS_ACTIVE", "وضعیت", true, true, true)
                )
        );
    }

    private static List<ReferenceFieldDescriptor> withAudits(ReferenceFieldDescriptor... mainFields) {
        List<ReferenceFieldDescriptor> result = new ArrayList<>(List.of(mainFields));
        result.addAll(standardAudits());
        return List.copyOf(result);
    }
}
