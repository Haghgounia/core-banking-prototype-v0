package com.behsazan.corebanking.deposit.opening.operational;

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
@Order(46)
public class DepositOpeningOperationalDescriptorProvider implements ReferenceDescriptorProvider {
    public static final String CATEGORY = "DEPOSIT_OPENING_OPERATIONAL";

    @Value("${core-banking.schemas.deposit-opening:DPS2}")
    private String schemaName = "DPS2";

    @Override
    public List<ReferenceTableDescriptor> descriptors() {
        return List.of(
            descriptor(
                    "dps2-opening-authorized-user", CATEGORY, "کاربران مجاز افتتاح", "table_view", schemaName, "DEPOSIT_OPENING_AUTHORIZED_USER", "SEQ_DEPOSIT_OPENING_AUTHORIZED_USER",
                    "openingAuthorizedUserId", "OPENING_AUTHORIZED_USER_ID", "accessRoleCode", "accessRoleCode", null,
                    List.of(
                        id("openingAuthorizedUserId", "OPENING_AUTHORIZED_USER_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        number("partyId", "PARTY_ID", "شناسه Party", true, true, null),
                        text("accessRoleCode", "ACCESS_ROLE_CODE", "نقش دسترسی", true, true, true, 40),
                        text("channelScopeCode", "CHANNEL_SCOPE_CODE", "دامنه کانال", false, true, true, 100),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        text("statusCode", "STATUS_CODE", "وضعیت", true, true, true, 20),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-batch", CATEGORY, "افتتاح گروهی", "table_view", schemaName, "DEPOSIT_OPENING_BATCH", "SEQ_DEPOSIT_OPENING_BATCH",
                    "openingBatchId", "OPENING_BATCH_ID", "batchNo", "batchNo", null,
                    List.of(
                        id("openingBatchId", "OPENING_BATCH_ID", "شناسه"),
                        text("batchNo", "BATCH_NO", "شماره Batch", true, true, true, 40),
                        text("idempotencyKey", "IDEMPOTENCY_KEY", "کلید یکتایی درخواست", true, true, true, 80),
                        text("sourceTypeCode", "SOURCE_TYPE_CODE", "نوع منبع", true, true, true, 20),
                        text("sourceReference", "SOURCE_REFERENCE", "مرجع منبع", false, true, true, 255),
                        number("totalCount", "TOTAL_COUNT", "تعداد کل", true, false, 0L),
                        number("successCount", "SUCCESS_COUNT", "تعداد موفق", true, false, 0L),
                        number("failedCount", "FAILED_COUNT", "تعداد ناموفق", true, false, 0L),
                        text("batchStatusCode", "BATCH_STATUS_CODE", "وضعیت Batch", true, true, false, 20),
                        timestamp("startedAt", "STARTED_AT", "زمان شروع", false, false),
                        timestamp("completedAt", "COMPLETED_AT", "زمان پایان", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-batch-error", CATEGORY, "خطاهای افتتاح گروهی", "table_view", schemaName, "DEPOSIT_OPENING_BATCH_ERROR", "SEQ_DEPOSIT_OPENING_BATCH_ERROR",
                    "openingBatchErrorId", "OPENING_BATCH_ERROR_ID", "errorStageCode", "errorStageCode", null,
                    List.of(
                        id("openingBatchErrorId", "OPENING_BATCH_ERROR_ID", "شناسه"),
                        number("openingBatchItemId", "OPENING_BATCH_ITEM_ID", "Opening Batch Item Id", true, true, null),
                        text("errorStageCode", "ERROR_STAGE_CODE", "مرحله خطا", true, true, true, 30),
                        text("errorCode", "ERROR_CODE", "کد خطا", true, true, true, 60),
                        text("fieldName", "FIELD_NAME", "نام فیلد", false, true, true, 128),
                        text("errorMessage", "ERROR_MESSAGE", "پیام خطا", true, false, true, 1000),
                        bool("isRetryable", "IS_RETRYABLE", "قابل تلاش مجدد", true, false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-batch-item", CATEGORY, "ردیف‌های افتتاح گروهی", "table_view", schemaName, "DEPOSIT_OPENING_BATCH_ITEM", "SEQ_DEPOSIT_OPENING_BATCH_ITEM",
                    "openingBatchItemId", "OPENING_BATCH_ITEM_ID", "externalRowKey", "externalRowKey", null,
                    List.of(
                        id("openingBatchItemId", "OPENING_BATCH_ITEM_ID", "شناسه"),
                        number("openingBatchId", "OPENING_BATCH_ID", "شناسه Batch", true, true, null),
                        number("rowNo", "ROW_NO", "شماره ردیف", true, true, null),
                        text("externalRowKey", "EXTERNAL_ROW_KEY", "کلید خارجی ردیف", false, true, true, 80),
                        number("partyId", "PARTY_ID", "شناسه Party", true, true, null),
                        number("productVersionId", "PRODUCT_VERSION_ID", "شناسه نسخه محصول", true, true, null),
                        text("currencyCode", "CURRENCY_CODE", "ارز", true, false, true, 3),
                        number("openingAmount", "OPENING_AMOUNT", "مبلغ افتتاح", false, false, null),
                        text("itemStatusCode", "ITEM_STATUS_CODE", "وضعیت ردیف", true, true, false, 20),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", false, true, null),
                        number("accountId", "ACCOUNT_ID", "شناسه حساب", false, false, null),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-beneficiary", CATEGORY, "ذی‌نفعان افتتاح", "table_view", schemaName, "DEPOSIT_OPENING_BENEFICIARY", "SEQ_DEPOSIT_OPENING_BENEFICIARY",
                    "openingBeneficiaryId", "OPENING_BENEFICIARY_ID", "beneficiaryReference", "beneficiaryReference", null,
                    List.of(
                        id("openingBeneficiaryId", "OPENING_BENEFICIARY_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        number("beneficiaryPartyId", "BENEFICIARY_PARTY_ID", "Party ذی‌نفع", false, true, null),
                        text("beneficiaryReference", "BENEFICIARY_REFERENCE", "مرجع ذی‌نفع", false, true, true, 100),
                        text("beneficiaryTypeCode", "BENEFICIARY_TYPE_CODE", "نوع ذی‌نفع", true, true, true, 30),
                        number("sharePercent", "SHARE_PERCENT", "درصد سهم", false, false, null),
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
                    "dps2-opening-check", CATEGORY, "کنترل‌های افتتاح", "table_view", schemaName, "DEPOSIT_OPENING_CHECK", "SEQ_DEPOSIT_OPENING_CHECK",
                    "openingCheckId", "OPENING_CHECK_ID", "checkCode", "checkCode", null,
                    List.of(
                        id("openingCheckId", "OPENING_CHECK_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        text("checkCode", "CHECK_CODE", "کد کنترل", true, true, true, 50),
                        text("checkTypeCode", "CHECK_TYPE_CODE", "نوع کنترل", true, true, true, 30),
                        number("attemptNo", "ATTEMPT_NO", "شماره تلاش", true, true, 1L),
                        text("resultStatusCode", "RESULT_STATUS_CODE", "نتیجه کنترل", true, false, true, 20),
                        text("resultReference", "RESULT_REFERENCE", "مرجع نتیجه", false, false, true, 100),
                        timestamp("checkedAt", "CHECKED_AT", "زمان کنترل", false, false),
                        text("waiverReason", "WAIVER_REASON", "علت صرف‌نظر", false, false, false, 500),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-decision", CATEGORY, "تصمیم‌های افتتاح", "table_view", schemaName, "DEPOSIT_OPENING_DECISION", "SEQ_DEPOSIT_OPENING_DECISION",
                    "openingDecisionId", "OPENING_DECISION_ID", "decisionCode", "decisionCode", null,
                    List.of(
                        id("openingDecisionId", "OPENING_DECISION_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        text("decisionCode", "DECISION_CODE", "تصمیم", true, true, true, 20),
                        text("decisionReasonCode", "DECISION_REASON_CODE", "علت تصمیم", false, true, true, 50),
                        text("decisionNote", "DECISION_NOTE", "یادداشت تصمیم", false, true, true, 1000),
                        text("decidedBy", "DECIDED_BY", "تصمیم‌گیرنده", true, false, true, 100),
                        timestamp("decidedAt", "DECIDED_AT", "زمان تصمیم", true, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-delegation", CATEGORY, "وکالت و نمایندگی افتتاح", "table_view", schemaName, "DEPOSIT_OPENING_DELEGATION", "SEQ_DEPOSIT_OPENING_DELEGATION",
                    "openingDelegationId", "OPENING_DELEGATION_ID", "delegationTypeCode", "delegationTypeCode", null,
                    List.of(
                        id("openingDelegationId", "OPENING_DELEGATION_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        number("grantorPartyId", "GRANTOR_PARTY_ID", "اعطاکننده اختیار", true, true, null),
                        number("delegatePartyId", "DELEGATE_PARTY_ID", "نماینده/وکیل", true, true, null),
                        text("delegationTypeCode", "DELEGATION_TYPE_CODE", "نوع وکالت/نمایندگی", true, true, true, 30),
                        text("authorityScopeCode", "AUTHORITY_SCOPE_CODE", "دامنه اختیار", true, false, true, 100),
                        text("documentReference", "DOCUMENT_REFERENCE", "مرجع سند", true, false, true, 100),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", true, false),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        text("verificationStatusCode", "VERIFICATION_STATUS_CODE", "وضعیت احراز", true, false, false, 20),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-document", CATEGORY, "مدارک افتتاح", "table_view", schemaName, "DEPOSIT_OPENING_DOCUMENT", "SEQ_DEPOSIT_OPENING_DOCUMENT",
                    "openingDocumentId", "OPENING_DOCUMENT_ID", "documentTypeCode", "documentTypeCode", null,
                    List.of(
                        id("openingDocumentId", "OPENING_DOCUMENT_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        text("documentTypeCode", "DOCUMENT_TYPE_CODE", "نوع مدرک", true, true, true, 50),
                        text("documentStatusCode", "DOCUMENT_STATUS_CODE", "وضعیت مدرک", true, true, true, 20),
                        text("documentReference", "DOCUMENT_REFERENCE", "مرجع سند", false, true, true, 120),
                        timestamp("verifiedAt", "VERIFIED_AT", "زمان تأیید", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-funding", CATEGORY, "تأمین وجه اولیه", "table_view", schemaName, "DEPOSIT_OPENING_FUNDING", "SEQ_DEPOSIT_OPENING_FUNDING",
                    "openingFundingId", "OPENING_FUNDING_ID", "fundingMethodCode", "fundingMethodCode", null,
                    List.of(
                        id("openingFundingId", "OPENING_FUNDING_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        text("fundingMethodCode", "FUNDING_METHOD_CODE", "روش تأمین وجه", true, true, true, 30),
                        number("fundingAmount", "FUNDING_AMOUNT", "مبلغ تأمین وجه", true, true, null),
                        text("sourceReference", "SOURCE_REFERENCE", "مرجع منبع", false, true, true, 100),
                        text("fundingStatusCode", "FUNDING_STATUS_CODE", "وضعیت تأمین وجه", true, false, true, 20),
                        text("transactionReference", "TRANSACTION_REFERENCE", "مرجع تراکنش", false, false, true, 80),
                        timestamp("attemptAt", "ATTEMPT_AT", "زمان تلاش", true, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-maturity-instruction", CATEGORY, "دستور سررسید", "table_view", schemaName, "DEPOSIT_OPENING_MATURITY_INSTRUCTION", "SEQ_DEPOSIT_OPENING_MATURITY_INSTRUCTION",
                    "openingMaturityInstructionId", "OPENING_MATURITY_INSTRUCTION_ID", "maturityActionCode", "maturityActionCode", null,
                    List.of(
                        id("openingMaturityInstructionId", "OPENING_MATURITY_INSTRUCTION_ID", "شناسه"),
                        number("openingTermId", "OPENING_TERM_ID", "شناسه مدت افتتاح", true, true, null),
                        text("maturityActionCode", "MATURITY_ACTION_CODE", "اقدام سررسید", true, true, true, 40),
                        text("settlementAccountReference", "SETTLEMENT_ACCOUNT_REFERENCE", "حساب تسویه", false, true, true, 80),
                        text("instructionSourceCode", "INSTRUCTION_SOURCE_CODE", "منبع دستور", true, true, true, 20),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-party", CATEGORY, "اشخاص و مالکان درخواست", "table_view", schemaName, "DEPOSIT_OPENING_PARTY", "SEQ_DEPOSIT_OPENING_PARTY",
                    "openingPartyId", "OPENING_PARTY_ID", "roleCode", "roleCode", null,
                    List.of(
                        id("openingPartyId", "OPENING_PARTY_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        number("partyId", "PARTY_ID", "شناسه Party", true, true, null),
                        text("roleCode", "ROLE_CODE", "نقش", true, true, true, 30),
                        bool("isPrimary", "IS_PRIMARY", "مالک اصلی", true, true, false),
                        number("ownershipPercent", "OWNERSHIP_PERCENT", "درصد مالکیت", false, false, null),
                        number("sequenceNo", "SEQUENCE_NO", "ترتیب", true, false, 1L),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-payment-instrument", CATEGORY, "درخواست ابزار پرداخت", "table_view", schemaName, "DEPOSIT_OPENING_PAYMENT_INSTRUMENT", "SEQ_DEPOSIT_OPENING_PAYMENT_INSTRUMENT",
                    "openingPaymentInstrumentId", "OPENING_PAYMENT_INSTRUMENT_ID", "instrumentTypeCode", "instrumentTypeCode", null,
                    List.of(
                        id("openingPaymentInstrumentId", "OPENING_PAYMENT_INSTRUMENT_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        text("instrumentTypeCode", "INSTRUMENT_TYPE_CODE", "نوع ابزار", true, true, true, 30),
                        number("requestedQuantity", "REQUESTED_QUANTITY", "تعداد درخواستی", true, true, 1L),
                        number("linkedPartyId", "LINKED_PARTY_ID", "Party مرتبط", false, true, null),
                        text("requestStatusCode", "REQUEST_STATUS_CODE", "وضعیت درخواست", true, true, true, 20),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-pricing-override-request", CATEGORY, "درخواست شرایط ترجیحی", "table_view", schemaName, "DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST", "SEQ_DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST",
                    "openingPricingRequestId", "OPENING_PRICING_REQUEST_ID", "overrideTypeCode", "overrideTypeCode", null,
                    List.of(
                        id("openingPricingRequestId", "OPENING_PRICING_REQUEST_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        number("pricingRuleId", "PRICING_RULE_ID", "قاعده قیمت‌گذاری", false, true, null),
                        text("overrideTypeCode", "OVERRIDE_TYPE_CODE", "نوع استثنای قیمت", true, true, true, 30),
                        number("requestedValue", "REQUESTED_VALUE", "مقدار درخواستی", false, true, null),
                        text("authorityLevelCode", "AUTHORITY_LEVEL_CODE", "سطح اختیار", true, false, true, 30),
                        text("approvalReference", "APPROVAL_REFERENCE", "مرجع تأیید", false, false, true, 100),
                        text("requestStatusCode", "REQUEST_STATUS_CODE", "وضعیت درخواست", true, true, true, 20),
                        date("effectiveFrom", "EFFECTIVE_FROM", "شروع اثر", false, false),
                        date("effectiveTo", "EFFECTIVE_TO", "پایان اثر", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-profit-instruction", CATEGORY, "دستور سود افتتاح", "table_view", schemaName, "DEPOSIT_OPENING_PROFIT_INSTRUCTION", "SEQ_DEPOSIT_OPENING_PROFIT_INSTRUCTION",
                    "openingProfitInstructionId", "OPENING_PROFIT_INSTRUCTION_ID", "calculationMethodCode", "calculationMethodCode", null,
                    List.of(
                        id("openingProfitInstructionId", "OPENING_PROFIT_INSTRUCTION_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        number("pricingRuleId", "PRICING_RULE_ID", "قاعده قیمت‌گذاری", true, true, null),
                        number("pricingComponentId", "PRICING_COMPONENT_ID", "مولفه قیمت‌گذاری", false, true, null),
                        number("rateTierId", "RATE_TIER_ID", "سطح نرخ", false, true, null),
                        number("profitPaymentRuleId", "PROFIT_PAYMENT_RULE_ID", "قاعده پرداخت سود", true, false, null),
                        number("rateValue", "RATE_VALUE", "نرخ", true, false, null),
                        text("calculationMethodCode", "CALCULATION_METHOD_CODE", "روش محاسبه", true, true, true, 30),
                        text("dayCountBasisCode", "DAY_COUNT_BASIS_CODE", "مبنای شمارش روز", true, false, false, 20),
                        text("accrualFrequencyCode", "ACCRUAL_FREQUENCY_CODE", "تناوب محاسبه", true, false, false, 20),
                        text("paymentFrequencyCode", "PAYMENT_FREQUENCY_CODE", "تناوب پرداخت", true, false, false, 20),
                        text("paymentDayRuleCode", "PAYMENT_DAY_RULE_CODE", "قاعده روز پرداخت", false, false, false, 30),
                        text("firstPaymentRuleCode", "FIRST_PAYMENT_RULE_CODE", "قاعده اولین پرداخت", false, false, false, 30),
                        text("holidayAdjustmentCode", "HOLIDAY_ADJUSTMENT_CODE", "تعدیل تعطیلات", false, false, false, 30),
                        text("paymentDestinationCode", "PAYMENT_DESTINATION_CODE", "مقصد پرداخت", true, false, false, 30),
                        text("destinationAccountReference", "DESTINATION_ACCOUNT_REFERENCE", "حساب مقصد", false, false, false, 80),
                        bool("destinationSelectedByCustomer", "DESTINATION_SELECTED_BY_CUSTOMER", "انتخاب مقصد توسط مشتری", true, false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-request", CATEGORY, "درخواست افتتاح حساب سپرده", "table_view", schemaName, "DEPOSIT_OPENING_REQUEST", "SEQ_DEPOSIT_OPENING_REQUEST",
                    "openingRequestId", "OPENING_REQUEST_ID", "requestNo", "requestNo", null,
                    List.of(
                        id("openingRequestId", "OPENING_REQUEST_ID", "شناسه"),
                        text("requestNo", "REQUEST_NO", "شماره درخواست", true, true, true, 40),
                        text("idempotencyKey", "IDEMPOTENCY_KEY", "کلید یکتایی درخواست", true, true, true, 80),
                        number("productVersionId", "PRODUCT_VERSION_ID", "شناسه نسخه محصول", true, true, null),
                        text("requestTypeCode", "REQUEST_TYPE_CODE", "نوع درخواست", true, true, true, 30),
                        text("ownershipTypeCode", "OWNERSHIP_TYPE_CODE", "نوع مالکیت", true, false, true, 20),
                        text("currencyCode", "CURRENCY_CODE", "ارز", true, false, true, 3),
                        text("openingChannelCode", "OPENING_CHANNEL_CODE", "کانال افتتاح", true, false, true, 30),
                        text("orgUnitCode", "ORG_UNIT_CODE", "واحد سازمانی", true, false, false, 30),
                        date("requestedOpeningDate", "REQUESTED_OPENING_DATE", "تاریخ درخواستی افتتاح", false, false),
                        number("openingAmount", "OPENING_AMOUNT", "مبلغ افتتاح", false, false, null),
                        text("sourceOfFundsCode", "SOURCE_OF_FUNDS_CODE", "منبع وجوه", false, false, false, 30),
                        text("purposeCode", "PURPOSE_CODE", "هدف افتتاح", false, false, false, 30),
                        text("requestStatusCode", "REQUEST_STATUS_CODE", "وضعیت درخواست", true, true, false, 30),
                        number("batchItemId", "BATCH_ITEM_ID", "شناسه ردیف گروهی", false, false, null),
                        number("createdAccountId", "CREATED_ACCOUNT_ID", "شناسه حساب ایجادشده", false, false, null),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-reward-enrollment", CATEGORY, "عضویت اولیه برنامه جایزه", "table_view", schemaName, "DEPOSIT_OPENING_REWARD_ENROLLMENT", "SEQ_DEPOSIT_OPENING_REWARD_ENROLLMENT",
                    "openingRewardEnrollmentId", "OPENING_REWARD_ENROLLMENT_ID", "enrollmentStatusCode", "enrollmentStatusCode", null,
                    List.of(
                        id("openingRewardEnrollmentId", "OPENING_REWARD_ENROLLMENT_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        number("programId", "PROGRAM_ID", "شناسه برنامه", true, true, null),
                        text("enrollmentStatusCode", "ENROLLMENT_STATUS_CODE", "وضعیت عضویت", true, true, true, 20),
                        text("consentReference", "CONSENT_REFERENCE", "مرجع رضایت", false, true, true, 100),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-service-selection", CATEGORY, "خدمات انتخاب‌شده افتتاح", "table_view", schemaName, "DEPOSIT_OPENING_SERVICE_SELECTION", "SEQ_DEPOSIT_OPENING_SERVICE_SELECTION",
                    "openingServiceSelectionId", "OPENING_SERVICE_SELECTION_ID", "serviceCode", "serviceCode", null,
                    List.of(
                        id("openingServiceSelectionId", "OPENING_SERVICE_SELECTION_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        text("serviceCode", "SERVICE_CODE", "خدمت", true, true, true, 40),
                        text("channelCode", "CHANNEL_CODE", "کانال", false, true, true, 30),
                        text("deliveryTarget", "DELIVERY_TARGET", "مقصد ارائه", false, true, true, 200),
                        text("apiScopeCode", "API_SCOPE_CODE", "دامنه API", false, false, true, 100),
                        text("requestStatusCode", "REQUEST_STATUS_CODE", "وضعیت درخواست", true, true, true, 20),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-signatory", CATEGORY, "صاحبان امضا", "table_view", schemaName, "DEPOSIT_OPENING_SIGNATORY", "SEQ_DEPOSIT_OPENING_SIGNATORY",
                    "openingSignatoryId", "OPENING_SIGNATORY_ID", "signatoryRoleCode", "signatoryRoleCode", null,
                    List.of(
                        id("openingSignatoryId", "OPENING_SIGNATORY_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        number("partyId", "PARTY_ID", "شناسه Party", true, true, null),
                        text("signatoryRoleCode", "SIGNATORY_ROLE_CODE", "نقش صاحب امضا", true, true, true, 30),
                        number("signatureSequenceNo", "SIGNATURE_SEQUENCE_NO", "ترتیب امضا", false, true, null),
                        bool("isPrimarySignatory", "IS_PRIMARY_SIGNATORY", "صاحب امضای اصلی", true, false, false),
                        text("verificationStatusCode", "VERIFICATION_STATUS_CODE", "وضعیت احراز", true, false, true, 20),
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
                    "dps2-opening-signatory-authority", CATEGORY, "اختیارات صاحبان امضا", "table_view", schemaName, "DEPOSIT_OPENING_SIGNATORY_AUTHORITY", "SEQ_DEPOSIT_OPENING_SIGNATORY_AUTHORITY",
                    "openingSignatoryAuthorityId", "OPENING_SIGNATORY_AUTHORITY_ID", "operationCode", "operationCode", null,
                    List.of(
                        id("openingSignatoryAuthorityId", "OPENING_SIGNATORY_AUTHORITY_ID", "شناسه"),
                        number("openingSignatoryId", "OPENING_SIGNATORY_ID", "شناسه صاحب امضا", true, true, null),
                        text("operationCode", "OPERATION_CODE", "عملیات", true, true, true, 50),
                        text("channelCode", "CHANNEL_CODE", "کانال", false, true, true, 30),
                        number("maxAmount", "MAX_AMOUNT", "حداکثر مبلغ", false, true, null),
                        text("currencyCode", "CURRENCY_CODE", "ارز", false, false, true, 3),
                        bool("requiresCosign", "REQUIRES_COSIGN", "نیازمند امضای همراه", true, false, false),
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
                    "dps2-opening-signature-rule", CATEGORY, "قاعده امضای حساب", "table_view", schemaName, "DEPOSIT_OPENING_SIGNATURE_RULE", "SEQ_DEPOSIT_OPENING_SIGNATURE_RULE",
                    "openingSignatureRuleId", "OPENING_SIGNATURE_RULE_ID", "signatureRuleCode", "signatureRuleCode", null,
                    List.of(
                        id("openingSignatureRuleId", "OPENING_SIGNATURE_RULE_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        text("signatureRuleCode", "SIGNATURE_RULE_CODE", "قاعده امضا", true, true, true, 20),
                        number("minSignatureCount", "MIN_SIGNATURE_COUNT", "حداقل تعداد امضا", true, true, 1L),
                        date("validFrom", "VALID_FROM", "شروع اعتبار", false, true),
                        date("validTo", "VALID_TO", "پایان اعتبار", false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-tax-status", CATEGORY, "وضعیت مالیاتی افتتاح", "table_view", schemaName, "DEPOSIT_OPENING_TAX_STATUS", "SEQ_DEPOSIT_OPENING_TAX_STATUS",
                    "openingTaxStatusId", "OPENING_TAX_STATUS_ID", "taxResidencyCode", "taxResidencyCode", null,
                    List.of(
                        id("openingTaxStatusId", "OPENING_TAX_STATUS_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        text("taxResidencyCode", "TAX_RESIDENCY_CODE", "اقامت مالیاتی", true, true, true, 30),
                        bool("withholdingApplicable", "WITHHOLDING_APPLICABLE", "مشمول مالیات تکلیفی", true, true, true),
                        text("exemptionCode", "EXEMPTION_CODE", "کد معافیت", false, true, true, 50),
                        text("exemptionDocumentReference", "EXEMPTION_DOCUMENT_REFERENCE", "مرجع مدرک معافیت", false, false, true, 100),
                        text("taxStatusSourceCode", "TAX_STATUS_SOURCE_CODE", "منبع وضعیت مالیاتی", true, false, true, 30),
                        text("taxVerificationReference", "TAX_VERIFICATION_REFERENCE", "مرجع استعلام مالیاتی", true, false, true, 100),
                        timestamp("taxVerifiedAt", "TAX_VERIFIED_AT", "زمان استعلام مالیاتی", true, false),
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
                    "dps2-opening-term", CATEGORY, "مدت سپرده در افتتاح", "table_view", schemaName, "DEPOSIT_OPENING_TERM", "SEQ_DEPOSIT_OPENING_TERM",
                    "openingTermId", "OPENING_TERM_ID", "termCode", "termCode", null,
                    List.of(
                        id("openingTermId", "OPENING_TERM_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        number("allowedTermId", "ALLOWED_TERM_ID", "شناسه مدت مجاز", true, true, null),
                        text("termCode", "TERM_CODE", "کد مدت", true, true, true, 20),
                        number("termValue", "TERM_VALUE", "مقدار مدت", true, true, null),
                        text("termUnitCode", "TERM_UNIT_CODE", "واحد مدت", true, false, true, 10),
                        date("startDate", "START_DATE", "تاریخ شروع", true, false),
                        date("maturityDate", "MATURITY_DATE", "تاریخ سررسید", true, false),
                        bool("autoRenewFlag", "AUTO_RENEW_FLAG", "تمدید خودکار", true, false, false),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-terms-acceptance", CATEGORY, "پذیرش شروط افتتاح", "table_view", schemaName, "DEPOSIT_OPENING_TERMS_ACCEPTANCE", "SEQ_DEPOSIT_OPENING_TERMS_ACCEPTANCE",
                    "openingTermsAcceptanceId", "OPENING_TERMS_ACCEPTANCE_ID", "termsVersionCode", "termsVersionCode", null,
                    List.of(
                        id("openingTermsAcceptanceId", "OPENING_TERMS_ACCEPTANCE_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        text("termsVersionCode", "TERMS_VERSION_CODE", "نسخه شروط", true, true, true, 50),
                        number("acceptedByPartyId", "ACCEPTED_BY_PARTY_ID", "پذیرنده شروط", true, true, null),
                        text("acceptanceSourceCode", "ACCEPTANCE_SOURCE_CODE", "منبع پذیرش", true, true, true, 20),
                        text("channelCode", "CHANNEL_CODE", "کانال", false, false, true, 30),
                        timestamp("acceptedAt", "ACCEPTED_AT", "زمان پذیرش", true, false),
                        text("acceptanceStatusCode", "ACCEPTANCE_STATUS_CODE", "وضعیت پذیرش", true, false, true, 20),
                        text("evidenceReference", "EVIDENCE_REFERENCE", "مرجع مدرک پذیرش", false, false, false, 120),
                        audit("createdAt", "CREATED_AT", "زمان ایجاد", TIMESTAMP),
                        audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT),
                        audit("updatedAt", "UPDATED_AT", "زمان ویرایش", TIMESTAMP),
                        audit("updatedBy", "UPDATED_BY", "ویرایش‌کننده", TEXT),
                        audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER)
                    )
            ),
            descriptor(
                    "dps2-opening-withdrawal-media", CATEGORY, "روش‌های برداشت افتتاح", "table_view", schemaName, "DEPOSIT_OPENING_WITHDRAWAL_MEDIA", "SEQ_DEPOSIT_OPENING_WITHDRAWAL_MEDIA",
                    "openingWithdrawalMediaId", "OPENING_WITHDRAWAL_MEDIA_ID", "withdrawalMediaCode", "withdrawalMediaCode", null,
                    List.of(
                        id("openingWithdrawalMediaId", "OPENING_WITHDRAWAL_MEDIA_ID", "شناسه"),
                        number("openingRequestId", "OPENING_REQUEST_ID", "Opening Request Id", true, true, null),
                        text("withdrawalMediaCode", "WITHDRAWAL_MEDIA_CODE", "روش برداشت", true, true, true, 40),
                        number("requestedQuantity", "REQUESTED_QUANTITY", "تعداد درخواستی", true, true, 1L),
                        text("requestStatusCode", "REQUEST_STATUS_CODE", "وضعیت درخواست", true, true, true, 20),
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
