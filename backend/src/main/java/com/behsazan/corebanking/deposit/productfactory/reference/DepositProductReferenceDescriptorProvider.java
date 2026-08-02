package com.behsazan.corebanking.deposit.productfactory.reference;

import com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorProvider;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceFieldDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorSupport.audit;
import static com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorSupport.bool;
import static com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorSupport.date;
import static com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorSupport.descriptor;
import static com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorSupport.id;
import static com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorSupport.number;
import static com.behsazan.corebanking.referencedata.descriptor.application.ReferenceDescriptorSupport.text;
import static com.behsazan.corebanking.referencedata.descriptor.domain.FieldType.NUMBER;
import static com.behsazan.corebanking.referencedata.descriptor.domain.FieldType.TEXT;

@Component
@Order(40)
public class DepositProductReferenceDescriptorProvider implements ReferenceDescriptorProvider {
    public static final String CATEGORY = "DEPOSIT_PRODUCT_REFERENCE";

    @Value("${core-banking.schemas.deposit-product-factory:DPS}")
    private String schemaName = "DPS";

    @Override
    public List<ReferenceTableDescriptor> descriptors() {
        return SPECS.stream().map(this::toDescriptor).toList();
    }

    private ReferenceTableDescriptor toDescriptor(TableSpec spec) {
        List<ReferenceFieldDescriptor> fields = new ArrayList<>();
        fields.add(id(spec.idApiName(), spec.idColumnName(), "شناسه"));
        fields.add(text("code", "CODE", "کد", true, true, true, 50));
        fields.add(text("nameFa", "NAME_FA", "عنوان فارسی", true, true, true, 200));
        fields.add(text("nameEn", "NAME_EN", "عنوان انگلیسی", false, false, true, 200));
        fields.add(text("description", "DESCRIPTION", "توضیحات", false, false, true, 1000));
        fields.add(bool("isActive", "IS_ACTIVE", "فعال", true, true, true));
        if (spec.hasParentCode()) {
            fields.add(text("parentCode", "PARENT_CODE", "کد والد", false, true, true, 50));
        }
        if (spec.hasValidityDates()) {
            fields.add(date("validFrom", "VALID_FROM", "شروع اعتبار", false, false));
            fields.add(date("validTo", "VALID_TO", "پایان اعتبار", false, false));
        }
        fields.add(number("versionNo", "VERSION_NO", "شماره نسخه", true, true, 1L));
        fields.add(bool("isCurrent", "IS_CURRENT", "نسخه جاری", true, true, true));
        if (spec.hasRecordVersion()) {
            fields.add(audit("recordVersion", "RECORD_VERSION", "نسخه رکورد", NUMBER));
        }
        fields.add(audit("createdBy", "CREATED_BY", "ایجادکننده", TEXT));

        return descriptor(
                spec.resource(), CATEGORY, spec.title(), spec.icon(),
                schemaName, spec.tableName(), "SEQ_" + spec.tableName(),
                spec.idApiName(), spec.idColumnName(), "code", "nameFa", null,
                List.copyOf(fields)
        );
    }

    private record TableSpec(
            String resource,
            String tableName,
            String idApiName,
            String idColumnName,
            String title,
            String icon,
            boolean hasParentCode,
            boolean hasValidityDates,
            boolean hasRecordVersion
    ) {
    }

    private static final List<TableSpec> SPECS = List.of(
            spec("dps-accrual-frequencies", "REF_ACCRUAL_FREQUENCY_CODE", "accrualFrequencyId", "ACCRUAL_FREQUENCY_ID", "تناوب‌های محاسبه سود", "schedule", false, true, true),
            spec("dps-aml-risk-max-levels", "REF_AML_RISK_MAX_CODE", "amlRiskMaxId", "AML_RISK_MAX_ID", "حداکثر سطح ریسک پول‌شویی", "policy", false, true, true),
            spec("dps-approval-levels", "REF_APPROVAL_LEVEL_CODE", "approvalLevelId", "APPROVAL_LEVEL_ID", "سطوح تأیید", "approval", false, true, true),
            spec("dps-balance-destinations", "REF_BALANCE_DESTINATION_CODE", "balanceDestinationId", "BALANCE_DESTINATION_ID", "مقاصد مانده", "account_balance_wallet", false, true, true),
            spec("dps-channels", "REF_CHANNEL_CODE", "channelId", "CHANNEL_ID", "کانال‌ها", "lan", true, true, true),
            spec("dps-check-types", "REF_CHECK_TYPE_CODE", "checkTypeId", "CHECK_TYPE_ID", "انواع کنترل", "fact_check", false, true, true),
            spec("dps-closure-types", "REF_CLOSURE_TYPE_CODE", "closureTypeId", "CLOSURE_TYPE_ID", "انواع بستن سپرده", "lock", false, true, true),
            spec("dps-customer-segments", "REF_CUSTOMER_SEGMENT_CODE", "customerSegmentId", "CUSTOMER_SEGMENT_ID", "بخش‌بندی مشتریان", "groups", true, true, true),
            spec("dps-day-count-bases", "REF_DAY_COUNT_BASIS_CODE", "dayCountBasisId", "DAY_COUNT_BASIS_ID", "مبانی شمارش روز", "date_range", false, true, true),
            spec("dps-default-currencies", "REF_DEFAULT_CURRENCY_CODE", "defaultCurrencyId", "DEFAULT_CURRENCY_ID", "ارزهای پیش‌فرض", "currency_exchange", false, true, true),
            spec("dps-deposit-groups", "REF_DEPOSIT_GROUP_CODE", "depositGroupId", "DEPOSIT_GROUP_ID", "گروه‌های سپرده", "folder", false, true, true),
            spec("dps-deposit-product-codes", "REF_DEPOSIT_PRODUCT_CODE", "depositProductId", "DEPOSIT_PRODUCT_ID", "کدهای محصول سپرده", "savings", false, true, true),
            spec("dps-deposit-types", "REF_DEPOSIT_TYPE_CODE", "depositTypeId", "DEPOSIT_TYPE_ID", "انواع سپرده", "account_balance", false, true, true),
            spec("dps-destinations", "REF_DESTINATION_CODE", "destinationId", "DESTINATION_ID", "مقاصد", "near_me", true, true, true),
            spec("dps-document-types", "REF_DOCUMENT_TYPE_CODE", "documentTypeId", "DOCUMENT_TYPE_ID", "انواع مدارک", "description", true, true, true),
            spec("dps-failure-actions", "REF_FAILURE_ACTION_CODE", "failureActionId", "FAILURE_ACTION_ID", "اقدامات در صورت شکست", "error", false, true, true),
            spec("dps-genders", "REF_GENDER_CODE", "genderId", "GENDER_ID", "جنسیت", "person", false, false, true),
            spec("dps-hold-types", "REF_HOLD_TYPE_CODE", "holdTypeId", "HOLD_TYPE_ID", "انواع مسدودی", "block", true, true, false),
            spec("dps-holiday-adjustments", "REF_HOLIDAY_ADJUSTMENT_CODE", "holidayAdjustmentId", "HOLIDAY_ADJUSTMENT_ID", "روش‌های تعدیل تعطیلات", "event", false, true, true),
            spec("dps-inactivity-period-units", "REF_INACTIVITY_PERIOD_UNIT_CODE", "inactivityPeriodUnitId", "INACTIVITY_PERIOD_UNIT_ID", "واحدهای دوره عدم فعالیت", "timer_off", false, false, true),
            spec("dps-inquiry-types", "REF_INQUIRY_TYPE_CODE", "inquiryTypeId", "INQUIRY_TYPE_ID", "انواع استعلام", "manage_search", true, true, true),
            spec("dps-kyc-levels", "REF_KYC_LEVEL_CODE", "kycLevelId", "KYC_LEVEL_ID", "سطوح شناخت مشتری", "verified_user", false, true, true),
            spec("dps-nationality-scopes", "REF_NATIONALITY_SCOPE_CODE", "nationalityScopeId", "NATIONALITY_SCOPE_ID", "دامنه‌های تابعیت", "public", false, true, true),
            spec("dps-opening-statuses", "REF_OPENING_STATUS_CODE", "openingStatusId", "OPENING_STATUS_ID", "وضعیت‌های افتتاح", "how_to_reg", false, true, true),
            spec("dps-org-units", "REF_ORG_UNIT_CODE", "orgUnitId", "ORG_UNIT_ID", "واحدهای سازمانی", "account_tree", true, true, true),
            spec("dps-org-unit-types", "REF_ORG_UNIT_TYPE_CODE", "orgUnitTypeId", "ORG_UNIT_TYPE_ID", "انواع واحد سازمانی", "corporate_fare", true, false, true),
            spec("dps-ownership-types", "REF_OWNERSHIP_TYPE_CODE", "ownershipTypeId", "OWNERSHIP_TYPE_ID", "انواع مالکیت", "badge", false, true, true),
            spec("dps-party-types", "REF_PARTY_TYPE_CODE", "partyTypeId", "PARTY_TYPE_ID", "انواع طرف حساب", "group", true, true, true),
            spec("dps-payment-frequencies", "REF_PAYMENT_FREQUENCY_CODE", "paymentFrequencyId", "PAYMENT_FREQUENCY_ID", "تناوب‌های پرداخت", "payments", false, true, true),
            spec("dps-product-families", "REF_PRODUCT_FAMILY_CODE", "productFamilyId", "PRODUCT_FAMILY_ID", "خانواده‌های محصول", "category", false, true, true),
            spec("dps-product-statuses", "REF_PRODUCT_STATUS_CODE", "productStatusId", "PRODUCT_STATUS_ID", "وضعیت‌های محصول", "published_with_changes", false, true, true),
            spec("dps-profit-destination-rules", "REF_PROFIT_DESTINATION_RULE_CODE", "profitDestinationRuleId", "PROFIT_DESTINATION_RULE_ID", "قواعد مقصد سود", "call_split", false, true, true),
            spec("dps-profit-distributions", "REF_PROFIT_DISTRIBUTION_CODE", "profitDistributionId", "PROFIT_DISTRIBUTION_ID", "روش‌های توزیع سود", "pie_chart", false, true, true),
            spec("dps-profit-methods", "REF_PROFIT_METHOD_CODE", "profitMethodId", "PROFIT_METHOD_ID", "روش‌های محاسبه سود", "calculate", false, true, true),
            spec("dps-reactivation-methods", "REF_REACTIVATION_METHOD_CODE", "reactivationMethodId", "REACTIVATION_METHOD_ID", "روش‌های فعال‌سازی مجدد", "restart_alt", false, true, true),
            spec("dps-relationship-types", "REF_RELATIONSHIP_TYPE_CODE", "relationshipTypeId", "RELATIONSHIP_TYPE_ID", "انواع ارتباط", "hub", true, true, true),
            spec("dps-renewal-instructions", "REF_RENEWAL_INSTRUCTION_CODE", "renewalInstructionId", "RENEWAL_INSTRUCTION_ID", "دستورهای تمدید", "autorenew", false, true, true),
            spec("dps-requirement-stages", "REF_REQUIREMENT_STAGE_CODE", "requirementStageId", "REQUIREMENT_STAGE_ID", "مراحل الزام", "rule", false, true, true),
            spec("dps-residency-statuses", "REF_RESIDENCY_STATUS_CODE", "residencyStatusId", "RESIDENCY_STATUS_ID", "وضعیت‌های اقامت", "home", false, true, true),
            spec("dps-rule-statuses", "REF_RULE_STATUS_CODE", "ruleStatusId", "RULE_STATUS_ID", "وضعیت‌های قاعده", "rule_settings", false, true, true),
            spec("dps-servicing-statuses", "REF_SERVICING_STATUS_CODE", "servicingStatusId", "SERVICING_STATUS_ID", "وضعیت‌های سرویس‌دهی", "support_agent", false, true, true),
            spec("dps-settlement-components", "REF_SETTLEMENT_COMPONENT_CODE", "settlementComponentId", "SETTLEMENT_COMPONENT_ID", "اجزای تسویه", "view_module", false, true, true),
            spec("dps-settlement-methods", "REF_SETTLEMENT_METHOD_CODE", "settlementMethodId", "SETTLEMENT_METHOD_ID", "روش‌های تسویه", "price_check", false, true, true),
            spec("dps-signing-rules", "REF_SIGNING_RULE_CODE", "signingRuleId", "SIGNING_RULE_ID", "قواعد امضا", "draw", false, true, true),
            spec("dps-statuses", "REF_STATUS_CODE", "statusId", "STATUS_ID", "وضعیت‌ها", "toggle_on", false, true, true),
            spec("dps-term-units", "REF_TERM_UNIT_CODE", "termUnitId", "TERM_UNIT_ID", "واحدهای مدت", "hourglass_bottom", false, false, true),
            spec("dps-transaction-types", "REF_TRANSACTION_TYPE_CODE", "transactionTypeId", "TRANSACTION_TYPE_ID", "انواع تراکنش", "swap_horiz", true, true, true),
            spec("dps-version-statuses", "REF_VERSION_STATUS_CODE", "versionStatusId", "VERSION_STATUS_ID", "وضعیت‌های نسخه", "history", false, true, true),
            spec("dps-warning-period-units", "REF_WARNING_PERIOD_UNIT_CODE", "warningPeriodUnitId", "WARNING_PERIOD_UNIT_ID", "واحدهای دوره هشدار", "warning", false, false, true),
            spec("dps-withdrawal-media", "REF_WITHDRAWAL_MEDIA_CODE", "withdrawalMediaId", "WITHDRAWAL_MEDIA_ID", "ابزارهای برداشت", "credit_card", false, true, true)
    );

    private static TableSpec spec(
            String resource,
            String tableName,
            String idApiName,
            String idColumnName,
            String title,
            String icon,
            boolean hasParentCode,
            boolean hasValidityDates,
            boolean hasRecordVersion
    ) {
        return new TableSpec(resource, tableName, idApiName, idColumnName, title, icon,
                hasParentCode, hasValidityDates, hasRecordVersion);
    }
}
