package com.behsazan.corebanking.referencedata.general.descriptor;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorProvider;
import com.behsazan.corebanking.referencedata.descriptor.domain.FieldType;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceFieldDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.SelectOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorSupport.*;

@Component
@Order(25)
public class NameRomanizationDescriptorProvider implements ReferenceDescriptorProvider {
    @Value("${core-banking.schemas.reference-data:GEO}")
    private String schemaName = "GEO";

    @Override
    public List<ReferenceTableDescriptor> descriptors() {
        return List.of(nameDictionary(), affixDictionary());
    }

    private ReferenceTableDescriptor nameDictionary() {
        return descriptor(
                "name-romanization-dictionary", "VOCABULARY", "واژه‌نامه رومن‌نویسی نام‌ها", "spellcheck",
                schemaName, "NAME_ROMANIZATION_DICTIONARY", "SEQ_NAME_ROMANIZATION_DICT",
                "nameRomanizationId", "NAME_ROMANIZATION_ID", "normalizedPersianName", "persianName", null,
                List.of(
                        id("nameRomanizationId", "NAME_ROMANIZATION_ID", "شناسه"),
                        text("persianName", "PERSIAN_NAME", "نام فارسی", true, true, true, 400),
                        text("normalizedPersianName", "NORMALIZED_PERSIAN_NAME", "نام فارسی نرمال‌شده", false, false, true, 400),
                        text("canonicalEnglishName", "CANONICAL_ENGLISH_NAME", "نام انگلیسی Canonical", false, true, true, 500),
                        text("suggestedEnglishName", "SUGGESTED_ENGLISH_NAME", "نام انگلیسی پیشنهادی", true, true, true, 500),
                        stringSelect("entryTypeCode", "ENTRY_TYPE_CODE", "نوع مدخل", true, false, "NAME", List.of(
                                new SelectOption("NAME", "نام"),
                                new SelectOption("COMPOUND_NAME", "نام مرکب"),
                                new SelectOption("POSSIBLE_FULL_NAME", "احتمالاً نام کامل")
                        )),
                        stringSelect("romanizationMethodCode", "ROMANIZATION_METHOD_CODE", "روش رومن‌نویسی", true, true, "SOURCE_DICTIONARY", List.of(
                                new SelectOption("BANK_POLICY", "سیاست مصوب بانک"),
                                new SelectOption("SOURCE_DICTIONARY", "واژه‌نامه منبع"),
                                new SelectOption("MANUAL_CORRECTION", "اصلاح کارشناسی"),
                                new SelectOption("COMPOSITION", "ترکیب اجزای نام"),
                                new SelectOption("RULE_FALLBACK", "قاعده Fallback"),
                                new SelectOption("SOURCE_CONFLICT", "تعارض منابع")
                        )),
                        stringSelect("governanceStatusCode", "GOVERNANCE_STATUS_CODE", "وضعیت حاکمیتی", true, true, "GENERATED_REVIEW", List.of(
                                new SelectOption("VERIFIED", "تأییدشده"),
                                new SelectOption("PROVISIONAL_SAFE", "موقت - قابل اتکای اولیه"),
                                new SelectOption("PROVISIONAL", "موقت"),
                                new SelectOption("COMPOSITION_HIGH", "ترکیب با اطمینان بالا"),
                                new SelectOption("COMPOSITION_REVIEW", "ترکیب - نیازمند بازبینی"),
                                new SelectOption("STRUCTURE_REVIEW", "بازبینی ساختار"),
                                new SelectOption("GENERATED_REVIEW", "تولیدشده - نیازمند بازبینی")
                        )),
                        number("confidenceScore", "CONFIDENCE_SCORE", "امتیاز اطمینان", true, true, 0),
                        bool("autoFillAllowed", "AUTO_FILL_ALLOWED", "مجاز برای تکمیل خودکار", true, true, false),
                        bool("isActive", "IS_ACTIVE", "وضعیت", true, true, true),
                        text("sourceCode", "SOURCE_CODE", "منبع / شواهد", false, false, true, 500),
                        audit("sourceEvidenceCount", "SOURCE_EVIDENCE_COUNT", "تعداد شواهد منبع", FieldType.NUMBER),
                        audit("d3Count", "D3_COUNT", "فراوانی در Dictionary 3", FieldType.NUMBER),
                        audit("d4Count", "D4_COUNT", "فراوانی در Dictionary 4", FieldType.NUMBER),
                        audit("d5Count", "D5_COUNT", "فراوانی در Dictionary 5", FieldType.NUMBER),
                        audit("qualityFlags", "QUALITY_FLAGS", "پرچم‌های کیفیت", FieldType.TEXT),
                        audit("detectedComponents", "DETECTED_COMPONENTS", "اجزای تشخیص‌داده‌شده", FieldType.TEXT),
                        audit("unresolvedComponents", "UNRESOLVED_COMPONENTS", "اجزای حل‌نشده", FieldType.TEXT),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", FieldType.TEXT),
                        audit("createdDate", "CREATED_AT", "تاریخ ایجاد", FieldType.TIMESTAMP),
                        audit("lastModifiedBy", "UPDATED_BY", "ویرایش‌کننده", FieldType.TEXT),
                        audit("lastModifiedDate", "UPDATED_AT", "تاریخ ویرایش", FieldType.TIMESTAMP),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", FieldType.NUMBER)
                )
        );
    }

    private ReferenceTableDescriptor affixDictionary() {
        return descriptor(
                "name-affix-dictionary", "VOCABULARY", "واژه‌نامه پیشوند و پسوند نام", "account_tree",
                schemaName, "NAME_AFFIX_DICTIONARY", "SEQ_NAME_AFFIX_DICTIONARY",
                "nameAffixId", "NAME_AFFIX_ID", "normalizedPersianAffix", "persianAffix", null,
                List.of(
                        id("nameAffixId", "NAME_AFFIX_ID", "شناسه"),
                        text("persianAffix", "PERSIAN_AFFIX", "پیشوند / پسوند فارسی", true, true, true, 100),
                        text("normalizedPersianAffix", "NORMALIZED_PERSIAN_AFFIX", "مقدار نرمال‌شده", false, false, true, 100),
                        text("englishAffix", "ENGLISH_AFFIX", "معادل انگلیسی", true, true, true, 150),
                        stringSelect("affixTypeCode", "AFFIX_TYPE_CODE", "نوع جزء نام", true, true, "NAME_COMPONENT", List.of(
                                new SelectOption("PREFIX_OR_NAME_COMPONENT", "پیشوند یا جزء نام"),
                                new SelectOption("TITLE_OR_NAME_COMPONENT", "عنوان یا جزء نام"),
                                new SelectOption("NAME_COMPONENT", "جزء نام"),
                                new SelectOption("PRONUNCIATION_SUFFIX", "پسوند وابسته به تلفظ")
                        )),
                        stringSelect("positionCode", "POSITION_CODE", "موقعیت", true, true, "ANY", List.of(
                                new SelectOption("PREFIX", "ابتدای نام"),
                                new SelectOption("SUFFIX", "انتهای نام"),
                                new SelectOption("ANY", "هر موقعیت")
                        )),
                        bool("contextSensitive", "CONTEXT_SENSITIVE", "وابسته به Context", true, true, true),
                        bool("autoApplyAllowed", "AUTO_APPLY_ALLOWED", "مجاز برای اعمال خودکار", true, true, false),
                        number("priorityNo", "PRIORITY_NO", "اولویت پردازش", true, true, 100),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 1000),
                        stringSelect("recordStatusCode", "RECORD_STATUS_CODE", "وضعیت رکورد", true, true, "ACTIVE", List.of(
                                new SelectOption("ACTIVE", "فعال"),
                                new SelectOption("INACTIVE", "غیرفعال"),
                                new SelectOption("ARCHIVED", "آرشیوشده")
                        )),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", FieldType.TEXT),
                        audit("createdDate", "CREATED_AT", "تاریخ ایجاد", FieldType.TIMESTAMP),
                        audit("lastModifiedBy", "UPDATED_BY", "ویرایش‌کننده", FieldType.TEXT),
                        audit("lastModifiedDate", "UPDATED_AT", "تاریخ ویرایش", FieldType.TIMESTAMP),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", FieldType.NUMBER)
                )
        );
    }
}
