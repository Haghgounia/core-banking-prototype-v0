package com.behsazan.corebanking.deposit.opening.readiness.application;

import com.behsazan.corebanking.deposit.opening.domain.DepositOpeningModels.*;
import com.behsazan.corebanking.deposit.opening.error.DepositOpeningValidationException;
import com.behsazan.corebanking.deposit.opening.readiness.domain.DepositOpeningRuntimeModels.ProductVersionContract;
import com.behsazan.corebanking.deposit.opening.readiness.domain.DepositOpeningRuntimeModels.RuntimeValidationResponse;
import com.behsazan.corebanking.deposit.opening.readiness.oracle.DepositOpeningRuntimeRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class DepositOpeningRuntimeValidator {
    private static final Set<String> SUPPORTED_FAMILIES = Set.of(
            "QARD_SAVINGS", "CURRENT_ACCOUNT", "SHORT_TERM_DEPOSIT", "LONG_TERM_DEPOSIT"
    );
    private final DepositOpeningRuntimeRepository repository;

    public DepositOpeningRuntimeValidator(DepositOpeningRuntimeRepository repository) {
        this.repository = repository;
    }

    public RuntimeValidationResponse validateAggregate(AggregateRequest aggregate) {
        Map<String, String> errors = new LinkedHashMap<>();
        OpeningRequest root = aggregate == null ? null : aggregate.request();
        if (root == null) {
            throw new DepositOpeningValidationException("ریشه درخواست افتتاح ارسال نشده است.", Map.of("DEPOSIT_OPENING_REQUEST", "الزامی"));
        }

        checkRef(errors, "DEPOSIT_OPENING_REQUEST.REQUEST_TYPE_CODE", "REF_DEP_OPEN_REQUEST_TYPE", "REQUEST_TYPE_CODE", root.requestTypeCode());
        checkRef(errors, "DEPOSIT_OPENING_REQUEST.OWNERSHIP_TYPE_CODE", "REF_DEP_OPEN_OWNERSHIP_TYPE", "OWNERSHIP_TYPE_CODE", root.ownershipTypeCode());
        checkRef(errors, "DEPOSIT_OPENING_REQUEST.OPENING_CHANNEL_CODE", "REF_DEP_OPEN_CHANNEL", "CHANNEL_CODE", root.openingChannelCode());
        checkRef(errors, "DEPOSIT_OPENING_REQUEST.SOURCE_OF_FUNDS_CODE", "REF_DEP_OPEN_SOURCE_OF_FUNDS", "SOURCE_OF_FUNDS_CODE", root.sourceOfFundsCode());
        checkRef(errors, "DEPOSIT_OPENING_REQUEST.PURPOSE_CODE", "REF_DEP_OPEN_PURPOSE", "PURPOSE_CODE", root.purposeCode());
        checkRef(errors, "DEPOSIT_OPENING_REQUEST.REQUEST_STATUS_CODE", "REF_DEP_OPEN_REQUEST_STATUS", "REQUEST_STATUS_CODE", root.requestStatusCode());
        checkRef(errors, "DEPOSIT_OPENING_REQUEST.JOINT_ACCOUNT_BASIS_CODE", "REF_DEP_OPEN_JOINT_BASIS", "JOINT_ACCOUNT_BASIS_CODE", root.jointAccountBasisCode());
        checkRef(errors, "DEPOSIT_OPENING_REQUEST.ACTIVATION_STATUS_CODE", "REF_DEP_OPEN_ACTIVATION_STATUS", "ACTIVATION_STATUS_CODE", root.activationStatusCode());

        for (OpeningParty value : safe(aggregate.parties())) {
            checkParty(errors, "DEPOSIT_OPENING_PARTY.PARTY_ID", value.partyId());
            checkRef(errors, "DEPOSIT_OPENING_PARTY.ROLE_CODE", "REF_DEP_OPEN_PARTY_ROLE", "ROLE_CODE", value.roleCode());
        }
        if (aggregate.signatureRule() != null) {
            checkRef(errors, "DEPOSIT_OPENING_SIGNATURE_RULE.SIGNATURE_RULE_CODE", "REF_DEP_OPEN_SIGNATURE_RULE", "SIGNATURE_RULE_CODE", aggregate.signatureRule().signatureRuleCode());
        }
        for (Signatory value : safe(aggregate.signatories())) {
            checkParty(errors, "DEPOSIT_OPENING_SIGNATORY.PARTY_ID", value.partyId());
            checkRef(errors, "DEPOSIT_OPENING_SIGNATORY.SIGNATORY_ROLE_CODE", "REF_DEP_OPEN_SIGNATORY_ROLE", "SIGNATORY_ROLE_CODE", value.signatoryRoleCode());
            checkRef(errors, "DEPOSIT_OPENING_SIGNATORY.VERIFICATION_STATUS_CODE", "REF_DEP_OPEN_VERIFICATION_STATUS", "VERIFICATION_STATUS_CODE", value.verificationStatusCode());
        }
        for (SignatoryAuthority value : safe(aggregate.signatoryAuthorities())) {
            checkRef(errors, "DEPOSIT_OPENING_SIGNATORY_AUTHORITY.OPERATION_CODE", "REF_DEP_OPEN_OPERATION", "OPERATION_CODE", value.operationCode());
            checkRef(errors, "DEPOSIT_OPENING_SIGNATORY_AUTHORITY.CHANNEL_CODE", "REF_DEP_OPEN_CHANNEL", "CHANNEL_CODE", value.channelCode());
        }
        for (AuthorizedUser value : safe(aggregate.authorizedUsers())) {
            checkParty(errors, "DEPOSIT_OPENING_AUTHORIZED_USER.PARTY_ID", value.partyId());
            checkRef(errors, "DEPOSIT_OPENING_AUTHORIZED_USER.ACCESS_ROLE_CODE", "REF_DEP_OPEN_ACCESS_ROLE", "ACCESS_ROLE_CODE", value.accessRoleCode());
            checkRef(errors, "DEPOSIT_OPENING_AUTHORIZED_USER.CHANNEL_SCOPE_CODE", "REF_DEP_OPEN_CHANNEL_SCOPE", "CHANNEL_SCOPE_CODE", value.channelScopeCode());
            checkRef(errors, "DEPOSIT_OPENING_AUTHORIZED_USER.STATUS_CODE", "REF_DEP_OPEN_ACTION_STATUS", "REQUEST_STATUS_CODE", value.statusCode());
        }
        for (Delegation value : safe(aggregate.delegations())) {
            checkParty(errors, "DEPOSIT_OPENING_DELEGATION.GRANTOR_PARTY_ID", value.grantorPartyId());
            checkParty(errors, "DEPOSIT_OPENING_DELEGATION.DELEGATE_PARTY_ID", value.delegatePartyId());
            checkRef(errors, "DEPOSIT_OPENING_DELEGATION.DELEGATION_TYPE_CODE", "REF_DEP_OPEN_DELEGATION_TYPE", "DELEGATION_TYPE_CODE", value.delegationTypeCode());
            checkRef(errors, "DEPOSIT_OPENING_DELEGATION.AUTHORITY_SCOPE_CODE", "REF_DEP_OPEN_AUTHORITY_SCOPE", "AUTHORITY_SCOPE_CODE", value.authorityScopeCode());
            checkRef(errors, "DEPOSIT_OPENING_DELEGATION.VERIFICATION_STATUS_CODE", "REF_DEP_OPEN_VERIFICATION_STATUS", "VERIFICATION_STATUS_CODE", value.verificationStatusCode());
        }
        for (Beneficiary value : safe(aggregate.beneficiaries())) {
            checkParty(errors, "DEPOSIT_OPENING_BENEFICIARY.BENEFICIARY_PARTY_ID", value.beneficiaryPartyId());
            checkRef(errors, "DEPOSIT_OPENING_BENEFICIARY.BENEFICIARY_TYPE_CODE", "REF_DEP_OPEN_BENEFICIARY_TYPE", "BENEFICIARY_TYPE_CODE", value.beneficiaryTypeCode());
        }
        if (aggregate.term() != null) {
            checkRef(errors, "DEPOSIT_OPENING_TERM.TERM_UNIT_CODE", "REF_DEP_OPEN_TERM_UNIT", "TERM_UNIT_CODE", aggregate.term().termUnitCode());
        }
        if (aggregate.maturityInstruction() != null) {
            checkRef(errors, "DEPOSIT_OPENING_MATURITY_INSTRUCTION.MATURITY_ACTION_CODE", "REF_DEP_OPEN_MATURITY_ACTION", "MATURITY_ACTION_CODE", aggregate.maturityInstruction().maturityActionCode());
            checkRef(errors, "DEPOSIT_OPENING_MATURITY_INSTRUCTION.INSTRUCTION_SOURCE_CODE", "REF_DEP_OPEN_INSTRUCTION_SOURCE", "INSTRUCTION_SOURCE_CODE", aggregate.maturityInstruction().instructionSourceCode());
        }
        if (aggregate.profitInstruction() != null) {
            ProfitInstruction value = aggregate.profitInstruction();
            checkRef(errors, "DEPOSIT_OPENING_PROFIT_INSTRUCTION.CALCULATION_METHOD_CODE", "REF_DEP_OPEN_PROFIT_CALC_METHOD", "CALCULATION_METHOD_CODE", value.calculationMethodCode());
            checkRef(errors, "DEPOSIT_OPENING_PROFIT_INSTRUCTION.DAY_COUNT_BASIS_CODE", "REF_DEP_OPEN_DAY_COUNT_BASIS", "DAY_COUNT_BASIS_CODE", value.dayCountBasisCode());
            checkRef(errors, "DEPOSIT_OPENING_PROFIT_INSTRUCTION.ACCRUAL_FREQUENCY_CODE", "REF_DEP_OPEN_FREQUENCY", "FREQUENCY_CODE", value.accrualFrequencyCode());
            checkRef(errors, "DEPOSIT_OPENING_PROFIT_INSTRUCTION.PAYMENT_FREQUENCY_CODE", "REF_DEP_OPEN_FREQUENCY", "FREQUENCY_CODE", value.paymentFrequencyCode());
            checkRef(errors, "DEPOSIT_OPENING_PROFIT_INSTRUCTION.PAYMENT_DAY_RULE_CODE", "REF_DEP_OPEN_PAYMENT_DAY_RULE", "PAYMENT_DAY_RULE_CODE", value.paymentDayRuleCode());
            checkRef(errors, "DEPOSIT_OPENING_PROFIT_INSTRUCTION.FIRST_PAYMENT_RULE_CODE", "REF_DEP_OPEN_FIRST_PAYMENT_RULE", "FIRST_PAYMENT_RULE_CODE", value.firstPaymentRuleCode());
            checkRef(errors, "DEPOSIT_OPENING_PROFIT_INSTRUCTION.HOLIDAY_ADJUSTMENT_CODE", "REF_DEP_OPEN_HOLIDAY_ADJUSTMENT", "HOLIDAY_ADJUSTMENT_CODE", value.holidayAdjustmentCode());
            checkRef(errors, "DEPOSIT_OPENING_PROFIT_INSTRUCTION.PAYMENT_DESTINATION_CODE", "REF_DEP_OPEN_PAYMENT_DESTINATION", "PAYMENT_DESTINATION_CODE", value.paymentDestinationCode());
        }
        for (WithdrawalMedia value : safe(aggregate.withdrawalMedia())) {
            checkRef(errors, "DEPOSIT_OPENING_WITHDRAWAL_MEDIA.WITHDRAWAL_MEDIA_CODE", "REF_DEP_OPEN_WITHDRAWAL_MEDIA", "WITHDRAWAL_MEDIA_CODE", value.withdrawalMediaCode());
            checkRef(errors, "DEPOSIT_OPENING_WITHDRAWAL_MEDIA.REQUEST_STATUS_CODE", "REF_DEP_OPEN_ACTION_STATUS", "REQUEST_STATUS_CODE", value.requestStatusCode());
        }
        for (ServiceSelection value : safe(aggregate.serviceSelections())) {
            checkRef(errors, "DEPOSIT_OPENING_SERVICE_SELECTION.SERVICE_CODE", "REF_DEP_OPEN_SERVICE", "SERVICE_CODE", value.serviceCode());
            checkRef(errors, "DEPOSIT_OPENING_SERVICE_SELECTION.CHANNEL_CODE", "REF_DEP_OPEN_CHANNEL", "CHANNEL_CODE", value.channelCode());
            checkRef(errors, "DEPOSIT_OPENING_SERVICE_SELECTION.REQUEST_STATUS_CODE", "REF_DEP_OPEN_ACTION_STATUS", "REQUEST_STATUS_CODE", value.requestStatusCode());
        }
        for (PaymentInstrument value : safe(aggregate.paymentInstruments())) {
            checkParty(errors, "DEPOSIT_OPENING_PAYMENT_INSTRUMENT.LINKED_PARTY_ID", value.linkedPartyId());
            checkRef(errors, "DEPOSIT_OPENING_PAYMENT_INSTRUMENT.INSTRUMENT_TYPE_CODE", "REF_DEP_OPEN_PAYMENT_INSTRUMENT", "INSTRUMENT_TYPE_CODE", value.instrumentTypeCode());
            checkRef(errors, "DEPOSIT_OPENING_PAYMENT_INSTRUMENT.REQUEST_STATUS_CODE", "REF_DEP_OPEN_ACTION_STATUS", "REQUEST_STATUS_CODE", value.requestStatusCode());
        }
        for (PricingOverrideRequest value : safe(aggregate.pricingOverrideRequests())) {
            checkRef(errors, "DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST.OVERRIDE_TYPE_CODE", "REF_DEP_OPEN_PRICING_OVERRIDE_TYPE", "OVERRIDE_TYPE_CODE", value.overrideTypeCode());
            checkRef(errors, "DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST.AUTHORITY_LEVEL_CODE", "REF_DEP_OPEN_AUTHORITY_LEVEL", "AUTHORITY_LEVEL_CODE", value.authorityLevelCode());
            checkRef(errors, "DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST.REQUEST_STATUS_CODE", "REF_DEP_OPEN_ACTION_STATUS", "REQUEST_STATUS_CODE", value.requestStatusCode());
        }
        if (aggregate.taxStatus() != null) {
            checkRef(errors, "DEPOSIT_OPENING_TAX_STATUS.TAX_RESIDENCY_CODE", "REF_DEP_OPEN_TAX_RESIDENCY", "TAX_RESIDENCY_CODE", aggregate.taxStatus().taxResidencyCode());
            checkRef(errors, "DEPOSIT_OPENING_TAX_STATUS.TAX_STATUS_SOURCE_CODE", "REF_DEP_OPEN_TAX_STATUS_SOURCE", "TAX_STATUS_SOURCE_CODE", aggregate.taxStatus().taxStatusSourceCode());
        }
        for (RewardEnrollment value : safe(aggregate.rewardEnrollments())) {
            checkRef(errors, "DEPOSIT_OPENING_REWARD_ENROLLMENT.ENROLLMENT_STATUS_CODE", "REF_DEP_OPEN_ENROLLMENT_STATUS", "ENROLLMENT_STATUS_CODE", value.enrollmentStatusCode());
        }
        for (Funding value : safe(aggregate.fundings())) {
            checkRef(errors, "DEPOSIT_OPENING_FUNDING.FUNDING_METHOD_CODE", "REF_DEP_OPEN_FUNDING_METHOD", "FUNDING_METHOD_CODE", value.fundingMethodCode());
            checkRef(errors, "DEPOSIT_OPENING_FUNDING.FUNDING_PURPOSE_CODE", "REF_DEP_OPEN_FUND_PURPOSE", "FUNDING_PURPOSE_CODE", value.fundingPurposeCode());
            checkRef(errors, "DEPOSIT_OPENING_FUNDING.FUNDING_STATUS_CODE", "REF_DEP_OPEN_FUNDING_STATUS", "FUNDING_STATUS_CODE", value.fundingStatusCode());
        }
        for (OpeningObligation value : safe(aggregate.obligations())) {
            checkRef(errors, "DEPOSIT_OPENING_OBLIGATION.OBLIGATION_TYPE_CODE", "REF_DEP_OPEN_OBLIGATION_TYPE", "OBLIGATION_TYPE_CODE", value.obligationTypeCode());
            checkRef(errors, "DEPOSIT_OPENING_OBLIGATION.SETTLEMENT_STATUS_CODE", "REF_DEP_OPEN_SETTLEMENT_STATUS", "SETTLEMENT_STATUS_CODE", value.settlementStatusCode());
        }
        for (OpeningCheck value : safe(aggregate.checks())) {
            checkRef(errors, "DEPOSIT_OPENING_CHECK.CHECK_CODE", "REF_DEP_OPEN_CHECK", "CHECK_CODE", value.checkCode());
            checkRef(errors, "DEPOSIT_OPENING_CHECK.CHECK_TYPE_CODE", "REF_DEP_OPEN_CHECK_TYPE", "CHECK_TYPE_CODE", value.checkTypeCode());
            checkRef(errors, "DEPOSIT_OPENING_CHECK.CHECK_PHASE_CODE", "REF_DEP_OPEN_CHECK_PHASE", "CHECK_PHASE_CODE", value.checkPhaseCode());
            checkRef(errors, "DEPOSIT_OPENING_CHECK.BLOCKING_SCOPE_CODE", "REF_DEP_OPEN_BLOCKING_SCOPE", "BLOCKING_SCOPE_CODE", value.blockingScopeCode());
            checkRef(errors, "DEPOSIT_OPENING_CHECK.RESULT_STATUS_CODE", "REF_DEP_OPEN_CHECK_RESULT", "RESULT_STATUS_CODE", value.resultStatusCode());
        }
        for (OpeningDocument value : safe(aggregate.documents())) {
            checkRef(errors, "DEPOSIT_OPENING_DOCUMENT.DOCUMENT_TYPE_CODE", "REF_DEP_OPEN_DOCUMENT_TYPE", "DOCUMENT_TYPE_CODE", value.documentTypeCode());
            checkRef(errors, "DEPOSIT_OPENING_DOCUMENT.DOCUMENT_STATUS_CODE", "REF_DEP_OPEN_DOCUMENT_STATUS", "DOCUMENT_STATUS_CODE", value.documentStatusCode());
        }
        if (aggregate.termsAcceptance() != null) {
            checkParty(errors, "DEPOSIT_OPENING_TERMS_ACCEPTANCE.ACCEPTED_BY_PARTY_ID", aggregate.termsAcceptance().acceptedByPartyId());
            checkRef(errors, "DEPOSIT_OPENING_TERMS_ACCEPTANCE.ACCEPTANCE_SOURCE_CODE", "REF_DEP_OPEN_ACCEPTANCE_SOURCE", "ACCEPTANCE_SOURCE_CODE", aggregate.termsAcceptance().acceptanceSourceCode());
            checkRef(errors, "DEPOSIT_OPENING_TERMS_ACCEPTANCE.CHANNEL_CODE", "REF_DEP_OPEN_CHANNEL", "CHANNEL_CODE", aggregate.termsAcceptance().channelCode());
            checkRef(errors, "DEPOSIT_OPENING_TERMS_ACCEPTANCE.ACCEPTANCE_STATUS_CODE", "REF_DEP_OPEN_ACCEPTANCE_STATUS", "ACCEPTANCE_STATUS_CODE", aggregate.termsAcceptance().acceptanceStatusCode());
        }
        if (aggregate.decision() != null) {
            checkRef(errors, "DEPOSIT_OPENING_DECISION.DECISION_CODE", "REF_DEP_OPEN_DECISION", "DECISION_CODE", aggregate.decision().decisionCode());
            checkRef(errors, "DEPOSIT_OPENING_DECISION.DECISION_REASON_CODE", "REF_DEP_OPEN_DECISION_REASON", "DECISION_REASON_CODE", aggregate.decision().decisionReasonCode());
        }

        ProductVersionContract product = validateProduct(errors, root.productVersionId(), root.currencyCode(), root.requestedOpeningDate());
        if (!errors.isEmpty()) {
            throw new DepositOpeningValidationException("اعتبارسنجی Runtime افتتاح حساب در Oracle/CIF/PDL ناموفق بود.", errors);
        }
        return response(product);
    }

    public RuntimeValidationResponse validateBatchItem(Long partyId, Long productVersionId, String currencyCode, LocalDate date) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkParty(errors, "PARTY_ID", partyId);
        ProductVersionContract product = validateProduct(errors, productVersionId, currencyCode, date);
        if (!errors.isEmpty()) throw new DepositOpeningValidationException("اعتبارسنجی Runtime ردیف Batch ناموفق بود.", errors);
        return response(product);
    }

    public void validateBatchProcessContract(String channelCode) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkRef(errors, "REQUEST_TYPE_CODE", "REF_DEP_OPEN_REQUEST_TYPE", "REQUEST_TYPE_CODE", "BULK");
        checkRef(errors, "OWNERSHIP_TYPE_CODE", "REF_DEP_OPEN_OWNERSHIP_TYPE", "OWNERSHIP_TYPE_CODE", "INDIVIDUAL");
        checkRef(errors, "OPENING_CHANNEL_CODE", "REF_DEP_OPEN_CHANNEL", "CHANNEL_CODE", channelCode);
        checkRef(errors, "SOURCE_OF_FUNDS_CODE", "REF_DEP_OPEN_SOURCE_OF_FUNDS", "SOURCE_OF_FUNDS_CODE", "OTHER");
        checkRef(errors, "PURPOSE_CODE", "REF_DEP_OPEN_PURPOSE", "PURPOSE_CODE", "SAVING");
        checkRef(errors, "REQUEST_STATUS_CODE", "REF_DEP_OPEN_REQUEST_STATUS", "REQUEST_STATUS_CODE", "APPROVED");
        if (!errors.isEmpty()) throw new DepositOpeningValidationException("Referenceهای ثابت پردازش Batch معتبر نیستند.", errors);
    }

    private ProductVersionContract validateProduct(Map<String, String> errors, Long productVersionId, String currencyCode, LocalDate openingDate) {
        ProductVersionContract product = repository.productVersion(productVersionId).orElse(null);
        if (product == null) {
            errors.put("DEPOSIT_OPENING_REQUEST.PRODUCT_VERSION_ID", "نسخه محصول در PDL یافت نشد.");
            return null;
        }
        if (!"DEPOSIT".equals(upper(product.productClassCode()))) {
            errors.put("DEPOSIT_OPENING_REQUEST.PRODUCT_VERSION_ID", "نسخه انتخابی متعلق به Product Class سپرده نیست.");
        }
        if (!SUPPORTED_FAMILIES.contains(upper(product.productFamilyCode()))) {
            errors.put("DEPOSIT_OPENING_REQUEST.PRODUCT_VERSION_ID", "نسخه محصول خارج از چهار خانواده مجاز افتتاح سپرده است.");
        }
        if (!blank(product.defaultCurrencyCode()) && !blank(currencyCode)
                && !upper(product.defaultCurrencyCode()).equals(upper(currencyCode))) {
            errors.put("DEPOSIT_OPENING_REQUEST.CURRENCY_CODE", "ارز درخواست با ارز پیش‌فرض محصول در PDL سازگار نیست.");
        }
        if (!blank(product.versionStatusCode()) && !Set.of("ACTIVE", "APPROVED").contains(upper(product.versionStatusCode()))) {
            errors.put("DEPOSIT_OPENING_REQUEST.PRODUCT_VERSION_ID", "نسخه محصول در وضعیت قابل افتتاح نیست: " + product.versionStatusCode());
        }
        if (!blank(product.originationStatusCode()) && !"OPEN".equals(upper(product.originationStatusCode()))) {
            errors.put("DEPOSIT_OPENING_REQUEST.PRODUCT_VERSION_ID", "Origination نسخه محصول باز نیست: " + product.originationStatusCode());
        }
        if (!blank(product.recordStatusCode()) && !"ACTIVE".equals(upper(product.recordStatusCode()))) {
            errors.put("DEPOSIT_OPENING_REQUEST.PRODUCT_VERSION_ID", "رکورد نسخه محصول فعال نیست: " + product.recordStatusCode());
        }
        LocalDate effective = openingDate == null ? LocalDate.now() : openingDate;
        if (product.validFrom() != null && product.validFrom().isAfter(effective)) {
            errors.put("DEPOSIT_OPENING_REQUEST.REQUESTED_OPENING_DATE", "نسخه محصول در تاریخ افتتاح هنوز معتبر نشده است.");
        }
        if (product.validTo() != null && product.validTo().isBefore(effective)) {
            errors.put("DEPOSIT_OPENING_REQUEST.REQUESTED_OPENING_DATE", "اعتبار نسخه محصول پیش از تاریخ افتتاح پایان یافته است.");
        }
        return product;
    }

    private void checkParty(Map<String, String> errors, String field, Long partyId) {
        if (partyId != null && !repository.partyExists(partyId)) errors.put(field, "Party در CIF.PARTY یافت نشد: " + partyId);
    }

    private void checkRef(Map<String, String> errors, String field, String table, String column, String code) {
        if (blank(code)) return;
        if (!repository.referenceCodeExists(table, column, upper(code))) {
            errors.put(field, "کد مرجع فعال/معتبر نیست: " + code);
        }
    }

    private static RuntimeValidationResponse response(ProductVersionContract product) {
        return new RuntimeValidationResponse(true,
                product == null ? null : product.productFamilyCode(),
                product == null ? null : product.versionStatusCode(),
                product == null ? null : product.originationStatusCode(),
                product == null ? null : product.recordStatusCode());
    }

    private static <T> List<T> safe(List<T> values) { return values == null ? List.of() : values; }
    private static String upper(String value) { return value == null ? null : value.trim().toUpperCase(Locale.ROOT); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
