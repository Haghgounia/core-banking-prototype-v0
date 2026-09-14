package com.behsazan.corebanking.deposit.opening.reference;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorProvider;
import com.behsazan.corebanking.referencedata.descriptor.domain.FieldType;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceFieldDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorSupport.*;
import static com.behsazan.corebanking.referencedata.descriptor.domain.FieldType.*;

@Component
@Order(45)
public class DepositOpeningReferenceDescriptorProvider implements ReferenceDescriptorProvider {
    public static final String CATEGORY = "DEPOSIT_OPENING_REFERENCE";

    @Value("${core-banking.schemas.deposit-opening:DPS2}")
    private String schemaName = "DPS2";

    @Override
    public List<ReferenceTableDescriptor> descriptors() {
        return List.of(
            descriptor(
                    "dps2-acceptance-source", CATEGORY, "منابع پذیرش شروط", "dataset", schemaName, "REF_DEP_OPEN_ACCEPTANCE_SOURCE", "SEQ_REF_DEP_OPEN_ACCEPTANCE_SOURCE",
                    "refAcceptanceSourceId", "REF_ACCEPTANCE_SOURCE_ID", "acceptanceSourceCode", "titleFa", null,
                    List.of(
                        id("refAcceptanceSourceId", "REF_ACCEPTANCE_SOURCE_ID", "شناسه"),
                        text("acceptanceSourceCode", "ACCEPTANCE_SOURCE_CODE", "منبع پذیرش", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-acceptance-status", CATEGORY, "وضعیت‌های پذیرش شروط", "dataset", schemaName, "REF_DEP_OPEN_ACCEPTANCE_STATUS", "SEQ_REF_DEP_OPEN_ACCEPTANCE_STATUS",
                    "refAcceptanceStatusId", "REF_ACCEPTANCE_STATUS_ID", "acceptanceStatusCode", "titleFa", null,
                    List.of(
                        id("refAcceptanceStatusId", "REF_ACCEPTANCE_STATUS_ID", "شناسه"),
                        text("acceptanceStatusCode", "ACCEPTANCE_STATUS_CODE", "وضعیت پذیرش", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-access-role", CATEGORY, "نقش‌های دسترسی", "dataset", schemaName, "REF_DEP_OPEN_ACCESS_ROLE", "SEQ_REF_DEP_OPEN_ACCESS_ROLE",
                    "refAccessRoleId", "REF_ACCESS_ROLE_ID", "accessRoleCode", "titleFa", null,
                    List.of(
                        id("refAccessRoleId", "REF_ACCESS_ROLE_ID", "شناسه"),
                        text("accessRoleCode", "ACCESS_ROLE_CODE", "نقش دسترسی", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-action-status", CATEGORY, "وضعیت‌های اقدام", "dataset", schemaName, "REF_DEP_OPEN_ACTION_STATUS", "SEQ_REF_DEP_OPEN_ACTION_STATUS",
                    "refActionStatusId", "REF_ACTION_STATUS_ID", "requestStatusCode", "titleFa", null,
                    List.of(
                        id("refActionStatusId", "REF_ACTION_STATUS_ID", "شناسه"),
                        text("requestStatusCode", "REQUEST_STATUS_CODE", "وضعیت درخواست", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-audit-actor-type", CATEGORY, "انواع عامل ممیزی", "dataset", schemaName, "REF_DEP_OPEN_AUDIT_ACTOR_TYPE", "SEQ_REF_DEP_OPEN_AUDIT_ACTOR_TYPE",
                    "refAuditActorTypeId", "REF_AUDIT_ACTOR_TYPE_ID", "actorTypeCode", "titleFa", null,
                    List.of(
                        id("refAuditActorTypeId", "REF_AUDIT_ACTOR_TYPE_ID", "شناسه"),
                        text("actorTypeCode", "ACTOR_TYPE_CODE", "Actor Type Code", true, true, true, 30),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 1000),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 0L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-audit-event-type", CATEGORY, "انواع رویداد ممیزی", "dataset", schemaName, "REF_DEP_OPEN_AUDIT_EVENT_TYPE", "SEQ_REF_DEP_OPEN_AUDIT_EVENT_TYPE",
                    "refAuditEventTypeId", "REF_AUDIT_EVENT_TYPE_ID", "eventTypeCode", "titleFa", null,
                    List.of(
                        id("refAuditEventTypeId", "REF_AUDIT_EVENT_TYPE_ID", "شناسه"),
                        text("eventTypeCode", "EVENT_TYPE_CODE", "Event Type Code", true, true, true, 40),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 1000),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 0L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-authority-level", CATEGORY, "سطوح اختیار", "dataset", schemaName, "REF_DEP_OPEN_AUTHORITY_LEVEL", "SEQ_REF_DEP_OPEN_AUTHORITY_LEVEL",
                    "refAuthorityLevelId", "REF_AUTHORITY_LEVEL_ID", "authorityLevelCode", "titleFa", null,
                    List.of(
                        id("refAuthorityLevelId", "REF_AUTHORITY_LEVEL_ID", "شناسه"),
                        text("authorityLevelCode", "AUTHORITY_LEVEL_CODE", "سطح اختیار", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-authority-scope", CATEGORY, "دامنه‌های اختیار", "dataset", schemaName, "REF_DEP_OPEN_AUTHORITY_SCOPE", "SEQ_REF_DEP_OPEN_AUTHORITY_SCOPE",
                    "refAuthorityScopeId", "REF_AUTHORITY_SCOPE_ID", "authorityScopeCode", "titleFa", null,
                    List.of(
                        id("refAuthorityScopeId", "REF_AUTHORITY_SCOPE_ID", "شناسه"),
                        text("authorityScopeCode", "AUTHORITY_SCOPE_CODE", "دامنه اختیار", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-batch-error-code", CATEGORY, "کدهای خطای افتتاح گروهی", "dataset", schemaName, "REF_DEP_OPEN_BATCH_ERROR_CODE", "SEQ_REF_DEP_OPEN_BATCH_ERROR_CODE",
                    "refBatchErrorCodeId", "REF_BATCH_ERROR_CODE_ID", "errorCode", "titleFa", null,
                    List.of(
                        id("refBatchErrorCodeId", "REF_BATCH_ERROR_CODE_ID", "شناسه"),
                        text("errorCode", "ERROR_CODE", "کد خطا", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        text("errorStageCode", "ERROR_STAGE_CODE", "مرحله خطا", true, true, false, 30),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-batch-error-stage", CATEGORY, "مراحل خطای افتتاح گروهی", "dataset", schemaName, "REF_DEP_OPEN_BATCH_ERROR_STAGE", "SEQ_REF_DEP_OPEN_BATCH_ERROR_STAGE",
                    "refBatchErrorStageId", "REF_BATCH_ERROR_STAGE_ID", "errorStageCode", "titleFa", null,
                    List.of(
                        id("refBatchErrorStageId", "REF_BATCH_ERROR_STAGE_ID", "شناسه"),
                        text("errorStageCode", "ERROR_STAGE_CODE", "مرحله خطا", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-batch-item-status", CATEGORY, "وضعیت ردیف‌های افتتاح گروهی", "dataset", schemaName, "REF_DEP_OPEN_BATCH_ITEM_STATUS", "SEQ_REF_DEP_OPEN_BATCH_ITEM_STATUS",
                    "refBatchItemStatusId", "REF_BATCH_ITEM_STATUS_ID", "itemStatusCode", "titleFa", null,
                    List.of(
                        id("refBatchItemStatusId", "REF_BATCH_ITEM_STATUS_ID", "شناسه"),
                        text("itemStatusCode", "ITEM_STATUS_CODE", "وضعیت ردیف", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-batch-source-type", CATEGORY, "انواع منبع افتتاح گروهی", "dataset", schemaName, "REF_DEP_OPEN_BATCH_SOURCE_TYPE", "SEQ_REF_DEP_OPEN_BATCH_SOURCE_TYPE",
                    "refBatchSourceTypeId", "REF_BATCH_SOURCE_TYPE_ID", "sourceTypeCode", "titleFa", null,
                    List.of(
                        id("refBatchSourceTypeId", "REF_BATCH_SOURCE_TYPE_ID", "شناسه"),
                        text("sourceTypeCode", "SOURCE_TYPE_CODE", "نوع منبع", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-batch-status", CATEGORY, "وضعیت‌های افتتاح گروهی", "dataset", schemaName, "REF_DEP_OPEN_BATCH_STATUS", "SEQ_REF_DEP_OPEN_BATCH_STATUS",
                    "refBatchStatusId", "REF_BATCH_STATUS_ID", "batchStatusCode", "titleFa", null,
                    List.of(
                        id("refBatchStatusId", "REF_BATCH_STATUS_ID", "شناسه"),
                        text("batchStatusCode", "BATCH_STATUS_CODE", "وضعیت Batch", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-beneficiary-type", CATEGORY, "انواع ذی‌نفع", "dataset", schemaName, "REF_DEP_OPEN_BENEFICIARY_TYPE", "SEQ_REF_DEP_OPEN_BENEFICIARY_TYPE",
                    "refBeneficiaryTypeId", "REF_BENEFICIARY_TYPE_ID", "beneficiaryTypeCode", "titleFa", null,
                    List.of(
                        id("refBeneficiaryTypeId", "REF_BENEFICIARY_TYPE_ID", "شناسه"),
                        text("beneficiaryTypeCode", "BENEFICIARY_TYPE_CODE", "نوع ذی‌نفع", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-change-reason", CATEGORY, "دلایل تغییر", "dataset", schemaName, "REF_DEP_OPEN_CHANGE_REASON", "SEQ_REF_DEP_OPEN_CHANGE_REASON",
                    "refChangeReasonId", "REF_CHANGE_REASON_ID", "changeReasonCode", "titleFa", null,
                    List.of(
                        id("refChangeReasonId", "REF_CHANGE_REASON_ID", "شناسه"),
                        text("changeReasonCode", "CHANGE_REASON_CODE", "Change Reason Code", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 1000),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 0L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-change-status", CATEGORY, "وضعیت‌های تغییر", "dataset", schemaName, "REF_DEP_OPEN_CHANGE_STATUS", "SEQ_REF_DEP_OPEN_CHANGE_STATUS",
                    "refChangeStatusId", "REF_CHANGE_STATUS_ID", "changeStatusCode", "titleFa", null,
                    List.of(
                        id("refChangeStatusId", "REF_CHANGE_STATUS_ID", "شناسه"),
                        text("changeStatusCode", "CHANGE_STATUS_CODE", "Change Status Code", true, true, true, 30),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 1000),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 0L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-change-type", CATEGORY, "انواع تغییر", "dataset", schemaName, "REF_DEP_OPEN_CHANGE_TYPE", "SEQ_REF_DEP_OPEN_CHANGE_TYPE",
                    "refChangeTypeId", "REF_CHANGE_TYPE_ID", "changeTypeCode", "titleFa", null,
                    List.of(
                        id("refChangeTypeId", "REF_CHANGE_TYPE_ID", "شناسه"),
                        text("changeTypeCode", "CHANGE_TYPE_CODE", "Change Type Code", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 1000),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 0L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-channel", CATEGORY, "کانال‌های افتتاح", "dataset", schemaName, "REF_DEP_OPEN_CHANNEL", "SEQ_REF_DEP_OPEN_CHANNEL",
                    "refChannelId", "REF_CHANNEL_ID", "channelCode", "titleFa", null,
                    List.of(
                        id("refChannelId", "REF_CHANNEL_ID", "شناسه"),
                        text("channelCode", "CHANNEL_CODE", "کانال", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-channel-org-map", CATEGORY, "نگاشت کانال به واحد سازمانی", "dataset", schemaName, "REF_DEP_OPEN_CHANNEL_ORG_MAP", "SEQ_REF_DEP_OPEN_CHANNEL_ORG_MAP",
                    "refChannelOrgMapId", "REF_CHANNEL_ORG_MAP_ID", "mapCode", "titleFa", null,
                    List.of(
                        id("refChannelOrgMapId", "REF_CHANNEL_ORG_MAP_ID", "شناسه"),
                        text("mapCode", "MAP_CODE", "کد نگاشت", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        text("channelCode", "CHANNEL_CODE", "کانال", true, true, false, 30),
                        text("defaultOrgUnitCode", "DEFAULT_ORG_UNIT_CODE", "واحد سازمانی پیش‌فرض", false, true, false, 50),
                        bool("overrideAllowed", "OVERRIDE_ALLOWED", "امکان تغییر", true, true, false),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-channel-scope", CATEGORY, "دامنه‌های کانال", "dataset", schemaName, "REF_DEP_OPEN_CHANNEL_SCOPE", "SEQ_REF_DEP_OPEN_CHANNEL_SCOPE",
                    "refChannelScopeId", "REF_CHANNEL_SCOPE_ID", "channelScopeCode", "titleFa", null,
                    List.of(
                        id("refChannelScopeId", "REF_CHANNEL_SCOPE_ID", "شناسه"),
                        text("channelScopeCode", "CHANNEL_SCOPE_CODE", "دامنه کانال", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-check", CATEGORY, "کنترل‌های افتتاح", "dataset", schemaName, "REF_DEP_OPEN_CHECK", "SEQ_REF_DEP_OPEN_CHECK",
                    "refCheckId", "REF_CHECK_ID", "checkCode", "titleFa", null,
                    List.of(
                        id("refCheckId", "REF_CHECK_ID", "شناسه"),
                        text("checkCode", "CHECK_CODE", "کد کنترل", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        text("checkTypeCode", "CHECK_TYPE_CODE", "نوع کنترل", true, true, false, 30),
                        bool("defaultRequiredFlag", "DEFAULT_REQUIRED_FLAG", "الزام پیش‌فرض", true, true, false),
                        number("executionOrder", "EXECUTION_ORDER", "ترتیب اجرا", true, true, null),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-check-result", CATEGORY, "نتایج کنترل افتتاح", "dataset", schemaName, "REF_DEP_OPEN_CHECK_RESULT", "SEQ_REF_DEP_OPEN_CHECK_RESULT",
                    "refCheckResultId", "REF_CHECK_RESULT_ID", "resultStatusCode", "titleFa", null,
                    List.of(
                        id("refCheckResultId", "REF_CHECK_RESULT_ID", "شناسه"),
                        text("resultStatusCode", "RESULT_STATUS_CODE", "نتیجه کنترل", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-check-type", CATEGORY, "انواع کنترل افتتاح", "dataset", schemaName, "REF_DEP_OPEN_CHECK_TYPE", "SEQ_REF_DEP_OPEN_CHECK_TYPE",
                    "refCheckTypeId", "REF_CHECK_TYPE_ID", "checkTypeCode", "titleFa", null,
                    List.of(
                        id("refCheckTypeId", "REF_CHECK_TYPE_ID", "شناسه"),
                        text("checkTypeCode", "CHECK_TYPE_CODE", "نوع کنترل", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-day-count-basis", CATEGORY, "مبانی شمارش روز", "dataset", schemaName, "REF_DEP_OPEN_DAY_COUNT_BASIS", "SEQ_REF_DEP_OPEN_DAY_COUNT_BASIS",
                    "refDayCountBasisId", "REF_DAY_COUNT_BASIS_ID", "dayCountBasisCode", "titleFa", null,
                    List.of(
                        id("refDayCountBasisId", "REF_DAY_COUNT_BASIS_ID", "شناسه"),
                        text("dayCountBasisCode", "DAY_COUNT_BASIS_CODE", "مبنای شمارش روز", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-decision", CATEGORY, "تصمیم‌های افتتاح", "dataset", schemaName, "REF_DEP_OPEN_DECISION", "SEQ_REF_DEP_OPEN_DECISION",
                    "refDecisionId", "REF_DECISION_ID", "decisionCode", "titleFa", null,
                    List.of(
                        id("refDecisionId", "REF_DECISION_ID", "شناسه"),
                        text("decisionCode", "DECISION_CODE", "تصمیم", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-decision-reason", CATEGORY, "دلایل تصمیم افتتاح", "dataset", schemaName, "REF_DEP_OPEN_DECISION_REASON", "SEQ_REF_DEP_OPEN_DECISION_REASON",
                    "refDecisionReasonId", "REF_DECISION_REASON_ID", "decisionReasonCode", "titleFa", null,
                    List.of(
                        id("refDecisionReasonId", "REF_DECISION_REASON_ID", "شناسه"),
                        text("decisionReasonCode", "DECISION_REASON_CODE", "علت تصمیم", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        text("decisionCode", "DECISION_CODE", "تصمیم", false, true, false, 30),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-delegation-type", CATEGORY, "انواع وکالت و نمایندگی", "dataset", schemaName, "REF_DEP_OPEN_DELEGATION_TYPE", "SEQ_REF_DEP_OPEN_DELEGATION_TYPE",
                    "refDelegationTypeId", "REF_DELEGATION_TYPE_ID", "delegationTypeCode", "titleFa", null,
                    List.of(
                        id("refDelegationTypeId", "REF_DELEGATION_TYPE_ID", "شناسه"),
                        text("delegationTypeCode", "DELEGATION_TYPE_CODE", "نوع وکالت/نمایندگی", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-document-status", CATEGORY, "وضعیت‌های مدرک", "dataset", schemaName, "REF_DEP_OPEN_DOCUMENT_STATUS", "SEQ_REF_DEP_OPEN_DOCUMENT_STATUS",
                    "refDocumentStatusId", "REF_DOCUMENT_STATUS_ID", "documentStatusCode", "titleFa", null,
                    List.of(
                        id("refDocumentStatusId", "REF_DOCUMENT_STATUS_ID", "شناسه"),
                        text("documentStatusCode", "DOCUMENT_STATUS_CODE", "وضعیت مدرک", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-document-type", CATEGORY, "انواع مدرک افتتاح", "dataset", schemaName, "REF_DEP_OPEN_DOCUMENT_TYPE", "SEQ_REF_DEP_OPEN_DOCUMENT_TYPE",
                    "refDocumentTypeId", "REF_DOCUMENT_TYPE_ID", "documentTypeCode", "titleFa", null,
                    List.of(
                        id("refDocumentTypeId", "REF_DOCUMENT_TYPE_ID", "شناسه"),
                        text("documentTypeCode", "DOCUMENT_TYPE_CODE", "نوع مدرک", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        bool("defaultRequiredFlag", "DEFAULT_REQUIRED_FLAG", "الزام پیش‌فرض", true, true, false),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-enrollment-status", CATEGORY, "وضعیت‌های عضویت", "dataset", schemaName, "REF_DEP_OPEN_ENROLLMENT_STATUS", "SEQ_REF_DEP_OPEN_ENROLLMENT_STATUS",
                    "refEnrollmentStatusId", "REF_ENROLLMENT_STATUS_ID", "enrollmentStatusCode", "titleFa", null,
                    List.of(
                        id("refEnrollmentStatusId", "REF_ENROLLMENT_STATUS_ID", "شناسه"),
                        text("enrollmentStatusCode", "ENROLLMENT_STATUS_CODE", "وضعیت عضویت", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-field-change-type", CATEGORY, "انواع تغییر فیلد", "dataset", schemaName, "REF_DEP_OPEN_FIELD_CHANGE_TYPE", "SEQ_REF_DEP_OPEN_FIELD_CHANGE_TYPE",
                    "refFieldChangeTypeId", "REF_FIELD_CHANGE_TYPE_ID", "fieldChangeTypeCode", "titleFa", null,
                    List.of(
                        id("refFieldChangeTypeId", "REF_FIELD_CHANGE_TYPE_ID", "شناسه"),
                        text("fieldChangeTypeCode", "FIELD_CHANGE_TYPE_CODE", "Field Change Type Code", true, true, true, 30),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 1000),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 0L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-first-payment-rule", CATEGORY, "قواعد اولین پرداخت سود", "dataset", schemaName, "REF_DEP_OPEN_FIRST_PAYMENT_RULE", "SEQ_REF_DEP_OPEN_FIRST_PAYMENT_RULE",
                    "refFirstPaymentRuleId", "REF_FIRST_PAYMENT_RULE_ID", "firstPaymentRuleCode", "titleFa", null,
                    List.of(
                        id("refFirstPaymentRuleId", "REF_FIRST_PAYMENT_RULE_ID", "شناسه"),
                        text("firstPaymentRuleCode", "FIRST_PAYMENT_RULE_CODE", "قاعده اولین پرداخت", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-frequency", CATEGORY, "تناوب‌ها", "dataset", schemaName, "REF_DEP_OPEN_FREQUENCY", "SEQ_REF_DEP_OPEN_FREQUENCY",
                    "refFrequencyId", "REF_FREQUENCY_ID", "frequencyCode", "titleFa", null,
                    List.of(
                        id("refFrequencyId", "REF_FREQUENCY_ID", "شناسه"),
                        text("frequencyCode", "FREQUENCY_CODE", "Frequency Code", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-funding-method", CATEGORY, "روش‌های تأمین وجه", "dataset", schemaName, "REF_DEP_OPEN_FUNDING_METHOD", "SEQ_REF_DEP_OPEN_FUNDING_METHOD",
                    "refFundingMethodId", "REF_FUNDING_METHOD_ID", "fundingMethodCode", "titleFa", null,
                    List.of(
                        id("refFundingMethodId", "REF_FUNDING_METHOD_ID", "شناسه"),
                        text("fundingMethodCode", "FUNDING_METHOD_CODE", "روش تأمین وجه", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-funding-status", CATEGORY, "وضعیت‌های تأمین وجه", "dataset", schemaName, "REF_DEP_OPEN_FUNDING_STATUS", "SEQ_REF_DEP_OPEN_FUNDING_STATUS",
                    "refFundingStatusId", "REF_FUNDING_STATUS_ID", "fundingStatusCode", "titleFa", null,
                    List.of(
                        id("refFundingStatusId", "REF_FUNDING_STATUS_ID", "شناسه"),
                        text("fundingStatusCode", "FUNDING_STATUS_CODE", "وضعیت تأمین وجه", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-holiday-adjustment", CATEGORY, "قواعد تعدیل تعطیلات", "dataset", schemaName, "REF_DEP_OPEN_HOLIDAY_ADJUSTMENT", "SEQ_REF_DEP_OPEN_HOLIDAY_ADJUSTMENT",
                    "refHolidayAdjustmentId", "REF_HOLIDAY_ADJUSTMENT_ID", "holidayAdjustmentCode", "titleFa", null,
                    List.of(
                        id("refHolidayAdjustmentId", "REF_HOLIDAY_ADJUSTMENT_ID", "شناسه"),
                        text("holidayAdjustmentCode", "HOLIDAY_ADJUSTMENT_CODE", "تعدیل تعطیلات", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-instruction-source", CATEGORY, "منابع دستور", "dataset", schemaName, "REF_DEP_OPEN_INSTRUCTION_SOURCE", "SEQ_REF_DEP_OPEN_INSTRUCTION_SOURCE",
                    "refInstructionSourceId", "REF_INSTRUCTION_SOURCE_ID", "instructionSourceCode", "titleFa", null,
                    List.of(
                        id("refInstructionSourceId", "REF_INSTRUCTION_SOURCE_ID", "شناسه"),
                        text("instructionSourceCode", "INSTRUCTION_SOURCE_CODE", "منبع دستور", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-maturity-action", CATEGORY, "اقدام‌های سررسید", "dataset", schemaName, "REF_DEP_OPEN_MATURITY_ACTION", "SEQ_REF_DEP_OPEN_MATURITY_ACTION",
                    "refMaturityActionId", "REF_MATURITY_ACTION_ID", "maturityActionCode", "titleFa", null,
                    List.of(
                        id("refMaturityActionId", "REF_MATURITY_ACTION_ID", "شناسه"),
                        text("maturityActionCode", "MATURITY_ACTION_CODE", "اقدام سررسید", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-operation", CATEGORY, "عملیات مجاز", "dataset", schemaName, "REF_DEP_OPEN_OPERATION", "SEQ_REF_DEP_OPEN_OPERATION",
                    "refOperationId", "REF_OPERATION_ID", "operationCode", "titleFa", null,
                    List.of(
                        id("refOperationId", "REF_OPERATION_ID", "شناسه"),
                        text("operationCode", "OPERATION_CODE", "عملیات", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-ownership-type", CATEGORY, "انواع مالکیت", "dataset", schemaName, "REF_DEP_OPEN_OWNERSHIP_TYPE", "SEQ_REF_DEP_OPEN_OWNERSHIP_TYPE",
                    "refOwnershipTypeId", "REF_OWNERSHIP_TYPE_ID", "ownershipTypeCode", "titleFa", null,
                    List.of(
                        id("refOwnershipTypeId", "REF_OWNERSHIP_TYPE_ID", "شناسه"),
                        text("ownershipTypeCode", "OWNERSHIP_TYPE_CODE", "نوع مالکیت", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-party-role", CATEGORY, "نقش‌های Party در افتتاح", "dataset", schemaName, "REF_DEP_OPEN_PARTY_ROLE", "SEQ_REF_DEP_OPEN_PARTY_ROLE",
                    "refPartyRoleId", "REF_PARTY_ROLE_ID", "roleCode", "titleFa", null,
                    List.of(
                        id("refPartyRoleId", "REF_PARTY_ROLE_ID", "شناسه"),
                        text("roleCode", "ROLE_CODE", "نقش", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-payment-day-rule", CATEGORY, "قواعد روز پرداخت", "dataset", schemaName, "REF_DEP_OPEN_PAYMENT_DAY_RULE", "SEQ_REF_DEP_OPEN_PAYMENT_DAY_RULE",
                    "refPaymentDayRuleId", "REF_PAYMENT_DAY_RULE_ID", "paymentDayRuleCode", "titleFa", null,
                    List.of(
                        id("refPaymentDayRuleId", "REF_PAYMENT_DAY_RULE_ID", "شناسه"),
                        text("paymentDayRuleCode", "PAYMENT_DAY_RULE_CODE", "قاعده روز پرداخت", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-payment-destination", CATEGORY, "مقاصد پرداخت", "dataset", schemaName, "REF_DEP_OPEN_PAYMENT_DESTINATION", "SEQ_REF_DEP_OPEN_PAYMENT_DESTINATION",
                    "refPaymentDestinationId", "REF_PAYMENT_DESTINATION_ID", "paymentDestinationCode", "titleFa", null,
                    List.of(
                        id("refPaymentDestinationId", "REF_PAYMENT_DESTINATION_ID", "شناسه"),
                        text("paymentDestinationCode", "PAYMENT_DESTINATION_CODE", "مقصد پرداخت", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-payment-instrument", CATEGORY, "انواع ابزار پرداخت", "dataset", schemaName, "REF_DEP_OPEN_PAYMENT_INSTRUMENT", "SEQ_REF_DEP_OPEN_PAYMENT_INSTRUMENT",
                    "refPaymentInstrumentId", "REF_PAYMENT_INSTRUMENT_ID", "instrumentTypeCode", "titleFa", null,
                    List.of(
                        id("refPaymentInstrumentId", "REF_PAYMENT_INSTRUMENT_ID", "شناسه"),
                        text("instrumentTypeCode", "INSTRUMENT_TYPE_CODE", "نوع ابزار", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-pricing-override-type", CATEGORY, "انواع استثنای قیمت‌گذاری", "dataset", schemaName, "REF_DEP_OPEN_PRICING_OVERRIDE_TYPE", "SEQ_REF_DEP_OPEN_PRICING_OVERRIDE_TYPE",
                    "refPricingOverrideTypeId", "REF_PRICING_OVERRIDE_TYPE_ID", "overrideTypeCode", "titleFa", null,
                    List.of(
                        id("refPricingOverrideTypeId", "REF_PRICING_OVERRIDE_TYPE_ID", "شناسه"),
                        text("overrideTypeCode", "OVERRIDE_TYPE_CODE", "نوع استثنای قیمت", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-profit-calc-method", CATEGORY, "روش‌های محاسبه سود", "dataset", schemaName, "REF_DEP_OPEN_PROFIT_CALC_METHOD", "SEQ_REF_DEP_OPEN_PROFIT_CALC_METHOD",
                    "refProfitCalcMethodId", "REF_PROFIT_CALC_METHOD_ID", "calculationMethodCode", "titleFa", null,
                    List.of(
                        id("refProfitCalcMethodId", "REF_PROFIT_CALC_METHOD_ID", "شناسه"),
                        text("calculationMethodCode", "CALCULATION_METHOD_CODE", "روش محاسبه", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-purpose", CATEGORY, "اهداف افتتاح", "dataset", schemaName, "REF_DEP_OPEN_PURPOSE", "SEQ_REF_DEP_OPEN_PURPOSE",
                    "refPurposeId", "REF_PURPOSE_ID", "purposeCode", "titleFa", null,
                    List.of(
                        id("refPurposeId", "REF_PURPOSE_ID", "شناسه"),
                        text("purposeCode", "PURPOSE_CODE", "هدف افتتاح", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-request-status", CATEGORY, "وضعیت‌های درخواست افتتاح", "dataset", schemaName, "REF_DEP_OPEN_REQUEST_STATUS", "SEQ_REF_DEP_OPEN_REQUEST_STATUS",
                    "refRequestStatusId", "REF_REQUEST_STATUS_ID", "requestStatusCode", "titleFa", null,
                    List.of(
                        id("refRequestStatusId", "REF_REQUEST_STATUS_ID", "شناسه"),
                        text("requestStatusCode", "REQUEST_STATUS_CODE", "وضعیت درخواست", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-request-type", CATEGORY, "انواع درخواست افتتاح", "dataset", schemaName, "REF_DEP_OPEN_REQUEST_TYPE", "SEQ_REF_DEP_OPEN_REQUEST_TYPE",
                    "refRequestTypeId", "REF_REQUEST_TYPE_ID", "requestTypeCode", "titleFa", null,
                    List.of(
                        id("refRequestTypeId", "REF_REQUEST_TYPE_ID", "شناسه"),
                        text("requestTypeCode", "REQUEST_TYPE_CODE", "نوع درخواست", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-service", CATEGORY, "خدمات قابل انتخاب", "dataset", schemaName, "REF_DEP_OPEN_SERVICE", "SEQ_REF_DEP_OPEN_SERVICE",
                    "refServiceId", "REF_SERVICE_ID", "serviceCode", "titleFa", null,
                    List.of(
                        id("refServiceId", "REF_SERVICE_ID", "شناسه"),
                        text("serviceCode", "SERVICE_CODE", "خدمت", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-signatory-role", CATEGORY, "نقش‌های صاحب امضا", "dataset", schemaName, "REF_DEP_OPEN_SIGNATORY_ROLE", "SEQ_REF_DEP_OPEN_SIGNATORY_ROLE",
                    "refSignatoryRoleId", "REF_SIGNATORY_ROLE_ID", "signatoryRoleCode", "titleFa", null,
                    List.of(
                        id("refSignatoryRoleId", "REF_SIGNATORY_ROLE_ID", "شناسه"),
                        text("signatoryRoleCode", "SIGNATORY_ROLE_CODE", "نقش صاحب امضا", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-signature-rule", CATEGORY, "قواعد امضا", "dataset", schemaName, "REF_DEP_OPEN_SIGNATURE_RULE", "SEQ_REF_DEP_OPEN_SIGNATURE_RULE",
                    "refSignatureRuleId", "REF_SIGNATURE_RULE_ID", "signatureRuleCode", "titleFa", null,
                    List.of(
                        id("refSignatureRuleId", "REF_SIGNATURE_RULE_ID", "شناسه"),
                        text("signatureRuleCode", "SIGNATURE_RULE_CODE", "قاعده امضا", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-snapshot-type", CATEGORY, "انواع Snapshot", "dataset", schemaName, "REF_DEP_OPEN_SNAPSHOT_TYPE", "SEQ_REF_DEP_OPEN_SNAPSHOT_TYPE",
                    "refSnapshotTypeId", "REF_SNAPSHOT_TYPE_ID", "snapshotTypeCode", "titleFa", null,
                    List.of(
                        id("refSnapshotTypeId", "REF_SNAPSHOT_TYPE_ID", "شناسه"),
                        text("snapshotTypeCode", "SNAPSHOT_TYPE_CODE", "Snapshot Type Code", true, true, true, 40),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 1000),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 0L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-source-of-funds", CATEGORY, "منابع وجوه", "dataset", schemaName, "REF_DEP_OPEN_SOURCE_OF_FUNDS", "SEQ_REF_DEP_OPEN_SOURCE_OF_FUNDS",
                    "refSourceOfFundsId", "REF_SOURCE_OF_FUNDS_ID", "sourceOfFundsCode", "titleFa", null,
                    List.of(
                        id("refSourceOfFundsId", "REF_SOURCE_OF_FUNDS_ID", "شناسه"),
                        text("sourceOfFundsCode", "SOURCE_OF_FUNDS_CODE", "منبع وجوه", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-tax-residency", CATEGORY, "اقامت‌های مالیاتی", "dataset", schemaName, "REF_DEP_OPEN_TAX_RESIDENCY", "SEQ_REF_DEP_OPEN_TAX_RESIDENCY",
                    "refTaxResidencyId", "REF_TAX_RESIDENCY_ID", "taxResidencyCode", "titleFa", null,
                    List.of(
                        id("refTaxResidencyId", "REF_TAX_RESIDENCY_ID", "شناسه"),
                        text("taxResidencyCode", "TAX_RESIDENCY_CODE", "اقامت مالیاتی", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-tax-status-source", CATEGORY, "منابع وضعیت مالیاتی", "dataset", schemaName, "REF_DEP_OPEN_TAX_STATUS_SOURCE", "SEQ_REF_DEP_OPEN_TAX_STATUS_SOURCE",
                    "refTaxStatusSourceId", "REF_TAX_STATUS_SOURCE_ID", "taxStatusSourceCode", "titleFa", null,
                    List.of(
                        id("refTaxStatusSourceId", "REF_TAX_STATUS_SOURCE_ID", "شناسه"),
                        text("taxStatusSourceCode", "TAX_STATUS_SOURCE_CODE", "منبع وضعیت مالیاتی", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-term-unit", CATEGORY, "واحدهای مدت", "dataset", schemaName, "REF_DEP_OPEN_TERM_UNIT", "SEQ_REF_DEP_OPEN_TERM_UNIT",
                    "refTermUnitId", "REF_TERM_UNIT_ID", "termUnitCode", "titleFa", null,
                    List.of(
                        id("refTermUnitId", "REF_TERM_UNIT_ID", "شناسه"),
                        text("termUnitCode", "TERM_UNIT_CODE", "واحد مدت", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-verification-status", CATEGORY, "وضعیت‌های احراز", "dataset", schemaName, "REF_DEP_OPEN_VERIFICATION_STATUS", "SEQ_REF_DEP_OPEN_VERIFICATION_STATUS",
                    "refVerificationStatusId", "REF_VERIFICATION_STATUS_ID", "verificationStatusCode", "titleFa", null,
                    List.of(
                        id("refVerificationStatusId", "REF_VERIFICATION_STATUS_ID", "شناسه"),
                        text("verificationStatusCode", "VERIFICATION_STATUS_CODE", "وضعیت احراز", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-withdrawal-media", CATEGORY, "روش‌های برداشت", "dataset", schemaName, "REF_DEP_OPEN_WITHDRAWAL_MEDIA", "SEQ_REF_DEP_OPEN_WITHDRAWAL_MEDIA",
                    "refWithdrawalMediaId", "REF_WITHDRAWAL_MEDIA_ID", "withdrawalMediaCode", "titleFa", null,
                    List.of(
                        id("refWithdrawalMediaId", "REF_WITHDRAWAL_MEDIA_ID", "شناسه"),
                        text("withdrawalMediaCode", "WITHDRAWAL_MEDIA_CODE", "روش برداشت", true, true, true, 50),
                        text("titleFa", "TITLE_FA", "عنوان فارسی", true, true, true, 200),
                        text("titleEn", "TITLE_EN", "عنوان انگلیسی", false, false, true, 200),
                        text("description", "DESCRIPTION", "توضیحات", false, false, true, 500),
                        number("displayOrder", "DISPLAY_ORDER", "ترتیب نمایش", true, true, 1L),
                        bool("isActive", "IS_ACTIVE", "فعال", true, true, true),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            )
        );
    }

    private static ReferenceFieldDescriptor timestamp(String api, String column, String label, boolean required, boolean grid) {
        return new ReferenceFieldDescriptor(api, column, label, FieldType.TIMESTAMP, required, false, grid, false, null, null, null, List.of());
    }
}
