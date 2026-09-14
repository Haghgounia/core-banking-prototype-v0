package com.behsazan.corebanking.deposit.opening.oracle;

import com.behsazan.corebanking.deposit.opening.domain.DepositOpeningModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class DepositOpeningAggregateRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final String schema;

    public DepositOpeningAggregateRepository(
            NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.schemas.deposit-opening:DPS2}") String schema
    ) {
        this.jdbc = jdbc;
        this.schema = requireIdentifier(schema);
    }

    public Optional<ExistingRequest> findByIdempotencyKey(String idempotencyKey) {
        String sql = """
                SELECT OPENING_REQUEST_ID, REQUEST_NO, IDEMPOTENCY_KEY, REQUEST_STATUS_CODE
                  FROM %s.DEPOSIT_OPENING_REQUEST
                 WHERE IDEMPOTENCY_KEY = :idempotencyKey
                 ORDER BY OPENING_REQUEST_ID
                """.formatted(schema);
        List<ExistingRequest> rows = jdbc.query(sql,
                new MapSqlParameterSource().addValue("idempotencyKey", idempotencyKey, Types.VARCHAR),
                (rs, rowNum) -> new ExistingRequest(
                        rs.getLong("OPENING_REQUEST_ID"),
                        rs.getString("REQUEST_NO"),
                        rs.getString("IDEMPOTENCY_KEY"),
                        rs.getString("REQUEST_STATUS_CODE")
                ));
        return rows.stream().findFirst();
    }

    public long nextRequestId() {
        return nextValue("SEQ_DEPOSIT_OPENING_REQUEST");
    }

    public long nextTermId() {
        return nextValue("SEQ_DEPOSIT_OPENING_TERM");
    }

    public long nextSignatoryId() {
        return nextValue("SEQ_DEPOSIT_OPENING_SIGNATORY");
    }

    public int insertRequest(long requestId, OpeningRequest value, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_REQUEST (
                    OPENING_REQUEST_ID, REQUEST_NO, IDEMPOTENCY_KEY, PRODUCT_VERSION_ID,
                    REQUEST_TYPE_CODE, OWNERSHIP_TYPE_CODE, CURRENCY_CODE, OPENING_CHANNEL_CODE,
                    ORG_UNIT_CODE, REQUESTED_OPENING_DATE, OPENING_AMOUNT, SOURCE_OF_FUNDS_CODE,
                    PURPOSE_CODE, REQUEST_STATUS_CODE, CREATED_BY
                ) VALUES (
                    :requestId, :requestNo, :idempotencyKey, :productVersionId,
                    :requestTypeCode, :ownershipTypeCode, :currencyCode, :openingChannelCode,
                    :orgUnitCode, :requestedOpeningDate, :openingAmount, :sourceOfFundsCode,
                    :purposeCode, :requestStatusCode, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("requestNo", value.requestNo(), Types.VARCHAR)
                .addValue("idempotencyKey", value.idempotencyKey(), Types.VARCHAR)
                .addValue("productVersionId", value.productVersionId(), Types.NUMERIC)
                .addValue("requestTypeCode", value.requestTypeCode(), Types.VARCHAR)
                .addValue("ownershipTypeCode", value.ownershipTypeCode(), Types.VARCHAR)
                .addValue("currencyCode", value.currencyCode(), Types.VARCHAR)
                .addValue("openingChannelCode", value.openingChannelCode(), Types.VARCHAR)
                .addValue("orgUnitCode", value.orgUnitCode(), Types.VARCHAR)
                .addValue("requestedOpeningDate", value.requestedOpeningDate(), Types.DATE)
                .addValue("openingAmount", value.openingAmount(), Types.NUMERIC)
                .addValue("sourceOfFundsCode", value.sourceOfFundsCode(), Types.VARCHAR)
                .addValue("purposeCode", value.purposeCode(), Types.VARCHAR)
                .addValue("requestStatusCode", value.requestStatusCode(), Types.VARCHAR)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertParty(long requestId, OpeningParty value, int fallbackSequence, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_PARTY (
                    OPENING_REQUEST_ID, PARTY_ID, ROLE_CODE, IS_PRIMARY, OWNERSHIP_PERCENT,
                    SEQUENCE_NO, CREATED_BY
                ) VALUES (
                    :requestId, :partyId, :roleCode, :isPrimary, :ownershipPercent,
                    :sequenceNo, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("partyId", value.partyId(), Types.NUMERIC)
                .addValue("roleCode", value.roleCode(), Types.VARCHAR)
                .addValue("isPrimary", value.isPrimary() == null ? 0 : value.isPrimary(), Types.NUMERIC)
                .addValue("ownershipPercent", value.ownershipPercent(), Types.NUMERIC)
                .addValue("sequenceNo", value.sequenceNo() == null ? fallbackSequence : value.sequenceNo(), Types.NUMERIC)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertSignatureRule(long requestId, SignatureRule value, LocalDate defaultValidFrom, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_SIGNATURE_RULE (
                    OPENING_REQUEST_ID, SIGNATURE_RULE_CODE, MIN_SIGNATURE_COUNT, VALID_FROM, VALID_TO, CREATED_BY
                ) VALUES (
                    :requestId, :signatureRuleCode, :minSignatureCount, :validFrom, :validTo, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("signatureRuleCode", value.signatureRuleCode(), Types.VARCHAR)
                .addValue("minSignatureCount", value.minSignatureCount() == null ? 1 : value.minSignatureCount(), Types.NUMERIC)
                .addValue("validFrom", value.validFrom() == null ? defaultValidFrom : value.validFrom(), Types.DATE)
                .addValue("validTo", value.validTo(), Types.DATE)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertSignatory(long signatoryId, long requestId, Signatory value, int fallbackSequence, LocalDate defaultValidFrom, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_SIGNATORY (
                    OPENING_SIGNATORY_ID, OPENING_REQUEST_ID, PARTY_ID, SIGNATORY_ROLE_CODE,
                    SIGNATURE_SEQUENCE_NO, IS_PRIMARY_SIGNATORY, VERIFICATION_STATUS_CODE,
                    VALID_FROM, VALID_TO, CREATED_BY
                ) VALUES (
                    :signatoryId, :requestId, :partyId, :signatoryRoleCode,
                    :signatureSequenceNo, :isPrimarySignatory, :verificationStatusCode,
                    :validFrom, :validTo, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("signatoryId", signatoryId, Types.NUMERIC)
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("partyId", value.partyId(), Types.NUMERIC)
                .addValue("signatoryRoleCode", value.signatoryRoleCode(), Types.VARCHAR)
                .addValue("signatureSequenceNo", value.signatureSequenceNo() == null ? fallbackSequence : value.signatureSequenceNo(), Types.NUMERIC)
                .addValue("isPrimarySignatory", value.isPrimarySignatory() == null ? 0 : value.isPrimarySignatory(), Types.NUMERIC)
                .addValue("verificationStatusCode", value.verificationStatusCode() == null ? "PENDING" : value.verificationStatusCode(), Types.VARCHAR)
                .addValue("validFrom", value.validFrom() == null ? defaultValidFrom : value.validFrom(), Types.DATE)
                .addValue("validTo", value.validTo(), Types.DATE)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertSignatoryAuthority(long signatoryId, SignatoryAuthority value, LocalDate defaultValidFrom, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_SIGNATORY_AUTHORITY (
                    OPENING_SIGNATORY_ID, OPERATION_CODE, CHANNEL_CODE, MAX_AMOUNT,
                    CURRENCY_CODE, REQUIRES_COSIGN, VALID_FROM, VALID_TO, CREATED_BY
                ) VALUES (
                    :signatoryId, :operationCode, :channelCode, :maxAmount,
                    :currencyCode, :requiresCosign, :validFrom, :validTo, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("signatoryId", signatoryId, Types.NUMERIC)
                .addValue("operationCode", value.operationCode(), Types.VARCHAR)
                .addValue("channelCode", value.channelCode(), Types.VARCHAR)
                .addValue("maxAmount", value.maxAmount(), Types.NUMERIC)
                .addValue("currencyCode", value.currencyCode(), Types.VARCHAR)
                .addValue("requiresCosign", value.requiresCosign() == null ? 0 : value.requiresCosign(), Types.NUMERIC)
                .addValue("validFrom", value.validFrom() == null ? defaultValidFrom : value.validFrom(), Types.DATE)
                .addValue("validTo", value.validTo(), Types.DATE)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertAuthorizedUser(long requestId, AuthorizedUser value, LocalDate defaultValidFrom, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_AUTHORIZED_USER (
                    OPENING_REQUEST_ID, PARTY_ID, ACCESS_ROLE_CODE, CHANNEL_SCOPE_CODE,
                    VALID_FROM, VALID_TO, STATUS_CODE, CREATED_BY
                ) VALUES (
                    :requestId, :partyId, :accessRoleCode, :channelScopeCode,
                    :validFrom, :validTo, :statusCode, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("partyId", value.partyId(), Types.NUMERIC)
                .addValue("accessRoleCode", value.accessRoleCode(), Types.VARCHAR)
                .addValue("channelScopeCode", value.channelScopeCode(), Types.VARCHAR)
                .addValue("validFrom", value.validFrom() == null ? defaultValidFrom : value.validFrom(), Types.DATE)
                .addValue("validTo", value.validTo(), Types.DATE)
                .addValue("statusCode", value.statusCode() == null ? "REQUESTED" : value.statusCode(), Types.VARCHAR)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertDelegation(long requestId, Delegation value, LocalDate defaultValidFrom, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_DELEGATION (
                    OPENING_REQUEST_ID, GRANTOR_PARTY_ID, DELEGATE_PARTY_ID, DELEGATION_TYPE_CODE,
                    AUTHORITY_SCOPE_CODE, DOCUMENT_REFERENCE, VALID_FROM, VALID_TO,
                    VERIFICATION_STATUS_CODE, CREATED_BY
                ) VALUES (
                    :requestId, :grantorPartyId, :delegatePartyId, :delegationTypeCode,
                    :authorityScopeCode, :documentReference, :validFrom, :validTo,
                    :verificationStatusCode, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("grantorPartyId", value.grantorPartyId(), Types.NUMERIC)
                .addValue("delegatePartyId", value.delegatePartyId(), Types.NUMERIC)
                .addValue("delegationTypeCode", value.delegationTypeCode(), Types.VARCHAR)
                .addValue("authorityScopeCode", value.authorityScopeCode(), Types.VARCHAR)
                .addValue("documentReference", value.documentReference(), Types.VARCHAR)
                .addValue("validFrom", value.validFrom() == null ? defaultValidFrom : value.validFrom(), Types.DATE)
                .addValue("validTo", value.validTo(), Types.DATE)
                .addValue("verificationStatusCode", value.verificationStatusCode() == null ? "PENDING" : value.verificationStatusCode(), Types.VARCHAR)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertBeneficiary(long requestId, Beneficiary value, LocalDate defaultValidFrom, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_BENEFICIARY (
                    OPENING_REQUEST_ID, BENEFICIARY_PARTY_ID, BENEFICIARY_REFERENCE,
                    BENEFICIARY_TYPE_CODE, SHARE_PERCENT, VALID_FROM, VALID_TO, CREATED_BY
                ) VALUES (
                    :requestId, :beneficiaryPartyId, :beneficiaryReference,
                    :beneficiaryTypeCode, :sharePercent, :validFrom, :validTo, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("beneficiaryPartyId", value.beneficiaryPartyId(), Types.NUMERIC)
                .addValue("beneficiaryReference", value.beneficiaryReference(), Types.VARCHAR)
                .addValue("beneficiaryTypeCode", value.beneficiaryTypeCode(), Types.VARCHAR)
                .addValue("sharePercent", value.sharePercent(), Types.NUMERIC)
                .addValue("validFrom", value.validFrom() == null ? defaultValidFrom : value.validFrom(), Types.DATE)
                .addValue("validTo", value.validTo(), Types.DATE)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertTerm(long termId, long requestId, OpeningTerm value, LocalDate maturityDate, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_TERM (
                    OPENING_TERM_ID, OPENING_REQUEST_ID, ALLOWED_TERM_ID, TERM_CODE, TERM_VALUE,
                    TERM_UNIT_CODE, START_DATE, MATURITY_DATE, AUTO_RENEW_FLAG, CREATED_BY
                ) VALUES (
                    :termId, :requestId, :allowedTermId, :termCode, :termValue,
                    :termUnitCode, :startDate, :maturityDate, :autoRenewFlag, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("termId", termId, Types.NUMERIC)
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("allowedTermId", value.allowedTermId(), Types.NUMERIC)
                .addValue("termCode", value.termCode(), Types.VARCHAR)
                .addValue("termValue", value.termValue(), Types.NUMERIC)
                .addValue("termUnitCode", value.termUnitCode(), Types.VARCHAR)
                .addValue("startDate", value.startDate(), Types.DATE)
                .addValue("maturityDate", maturityDate, Types.DATE)
                .addValue("autoRenewFlag", value.autoRenewFlag() == null ? 0 : value.autoRenewFlag(), Types.NUMERIC)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertMaturityInstruction(long termId, MaturityInstruction value, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_MATURITY_INSTRUCTION (
                    OPENING_TERM_ID, MATURITY_ACTION_CODE, SETTLEMENT_ACCOUNT_REFERENCE,
                    INSTRUCTION_SOURCE_CODE, CREATED_BY
                ) VALUES (
                    :termId, :maturityActionCode, :settlementAccountReference,
                    :instructionSourceCode, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("termId", termId, Types.NUMERIC)
                .addValue("maturityActionCode", value.maturityActionCode(), Types.VARCHAR)
                .addValue("settlementAccountReference", value.settlementAccountReference(), Types.VARCHAR)
                .addValue("instructionSourceCode", value.instructionSourceCode() == null ? "CUSTOMER" : value.instructionSourceCode(), Types.VARCHAR)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertProfitInstruction(long requestId, ProfitInstruction value, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_PROFIT_INSTRUCTION (
                    OPENING_REQUEST_ID, PRICING_RULE_ID, PRICING_COMPONENT_ID, RATE_TIER_ID,
                    PROFIT_PAYMENT_RULE_ID, RATE_VALUE, CALCULATION_METHOD_CODE, DAY_COUNT_BASIS_CODE,
                    ACCRUAL_FREQUENCY_CODE, PAYMENT_FREQUENCY_CODE, PAYMENT_DAY_RULE_CODE,
                    FIRST_PAYMENT_RULE_CODE, HOLIDAY_ADJUSTMENT_CODE, PAYMENT_DESTINATION_CODE,
                    DESTINATION_ACCOUNT_REFERENCE, DESTINATION_SELECTED_BY_CUSTOMER, CREATED_BY
                ) VALUES (
                    :requestId, :pricingRuleId, :pricingComponentId, :rateTierId,
                    :profitPaymentRuleId, :rateValue, :calculationMethodCode, :dayCountBasisCode,
                    :accrualFrequencyCode, :paymentFrequencyCode, :paymentDayRuleCode,
                    :firstPaymentRuleCode, :holidayAdjustmentCode, :paymentDestinationCode,
                    :destinationAccountReference, :destinationSelectedByCustomer, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("pricingRuleId", value.pricingRuleId(), Types.NUMERIC)
                .addValue("pricingComponentId", value.pricingComponentId(), Types.NUMERIC)
                .addValue("rateTierId", value.rateTierId(), Types.NUMERIC)
                .addValue("profitPaymentRuleId", value.profitPaymentRuleId(), Types.NUMERIC)
                .addValue("rateValue", value.rateValue(), Types.NUMERIC)
                .addValue("calculationMethodCode", value.calculationMethodCode(), Types.VARCHAR)
                .addValue("dayCountBasisCode", value.dayCountBasisCode(), Types.VARCHAR)
                .addValue("accrualFrequencyCode", value.accrualFrequencyCode(), Types.VARCHAR)
                .addValue("paymentFrequencyCode", value.paymentFrequencyCode(), Types.VARCHAR)
                .addValue("paymentDayRuleCode", value.paymentDayRuleCode(), Types.VARCHAR)
                .addValue("firstPaymentRuleCode", value.firstPaymentRuleCode(), Types.VARCHAR)
                .addValue("holidayAdjustmentCode", value.holidayAdjustmentCode(), Types.VARCHAR)
                .addValue("paymentDestinationCode", value.paymentDestinationCode(), Types.VARCHAR)
                .addValue("destinationAccountReference", value.destinationAccountReference(), Types.VARCHAR)
                .addValue("destinationSelectedByCustomer", value.destinationSelectedByCustomer() == null ? 0 : value.destinationSelectedByCustomer(), Types.NUMERIC)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertWithdrawalMedia(long requestId, WithdrawalMedia value, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_WITHDRAWAL_MEDIA (
                    OPENING_REQUEST_ID, WITHDRAWAL_MEDIA_CODE, REQUESTED_QUANTITY, REQUEST_STATUS_CODE, CREATED_BY
                ) VALUES (
                    :requestId, :withdrawalMediaCode, :requestedQuantity, :requestStatusCode, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("withdrawalMediaCode", value.withdrawalMediaCode(), Types.VARCHAR)
                .addValue("requestedQuantity", value.requestedQuantity() == null ? 1 : value.requestedQuantity(), Types.NUMERIC)
                .addValue("requestStatusCode", value.requestStatusCode() == null ? "REQUESTED" : value.requestStatusCode(), Types.VARCHAR)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertServiceSelection(long requestId, ServiceSelection value, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_SERVICE_SELECTION (
                    OPENING_REQUEST_ID, SERVICE_CODE, CHANNEL_CODE, DELIVERY_TARGET,
                    API_SCOPE_CODE, REQUEST_STATUS_CODE, CREATED_BY
                ) VALUES (
                    :requestId, :serviceCode, :channelCode, :deliveryTarget,
                    :apiScopeCode, :requestStatusCode, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("serviceCode", value.serviceCode(), Types.VARCHAR)
                .addValue("channelCode", value.channelCode(), Types.VARCHAR)
                .addValue("deliveryTarget", value.deliveryTarget(), Types.VARCHAR)
                .addValue("apiScopeCode", value.apiScopeCode(), Types.VARCHAR)
                .addValue("requestStatusCode", value.requestStatusCode() == null ? "REQUESTED" : value.requestStatusCode(), Types.VARCHAR)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertPaymentInstrument(long requestId, PaymentInstrument value, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_PAYMENT_INSTRUMENT (
                    OPENING_REQUEST_ID, INSTRUMENT_TYPE_CODE, REQUESTED_QUANTITY,
                    LINKED_PARTY_ID, REQUEST_STATUS_CODE, CREATED_BY
                ) VALUES (
                    :requestId, :instrumentTypeCode, :requestedQuantity,
                    :linkedPartyId, :requestStatusCode, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("instrumentTypeCode", value.instrumentTypeCode(), Types.VARCHAR)
                .addValue("requestedQuantity", value.requestedQuantity() == null ? 1 : value.requestedQuantity(), Types.NUMERIC)
                .addValue("linkedPartyId", value.linkedPartyId(), Types.NUMERIC)
                .addValue("requestStatusCode", value.requestStatusCode() == null ? "REQUESTED" : value.requestStatusCode(), Types.VARCHAR)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertPricingOverrideRequest(long requestId, PricingOverrideRequest value, LocalDate defaultEffectiveFrom, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST (
                    OPENING_REQUEST_ID, PRICING_RULE_ID, OVERRIDE_TYPE_CODE, REQUESTED_VALUE,
                    AUTHORITY_LEVEL_CODE, APPROVAL_REFERENCE, REQUEST_STATUS_CODE,
                    EFFECTIVE_FROM, EFFECTIVE_TO, CREATED_BY
                ) VALUES (
                    :requestId, :pricingRuleId, :overrideTypeCode, :requestedValue,
                    :authorityLevelCode, :approvalReference, :requestStatusCode,
                    :effectiveFrom, :effectiveTo, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("pricingRuleId", value.pricingRuleId(), Types.NUMERIC)
                .addValue("overrideTypeCode", value.overrideTypeCode(), Types.VARCHAR)
                .addValue("requestedValue", value.requestedValue(), Types.NUMERIC)
                .addValue("authorityLevelCode", value.authorityLevelCode(), Types.VARCHAR)
                .addValue("approvalReference", value.approvalReference(), Types.VARCHAR)
                .addValue("requestStatusCode", value.requestStatusCode() == null ? "PENDING" : value.requestStatusCode(), Types.VARCHAR)
                .addValue("effectiveFrom", value.effectiveFrom() == null ? defaultEffectiveFrom : value.effectiveFrom(), Types.DATE)
                .addValue("effectiveTo", value.effectiveTo(), Types.DATE)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertTaxStatus(long requestId, TaxStatus value, LocalDate defaultValidFrom, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_TAX_STATUS (
                    OPENING_REQUEST_ID, TAX_RESIDENCY_CODE, WITHHOLDING_APPLICABLE,
                    EXEMPTION_CODE, EXEMPTION_DOCUMENT_REFERENCE, TAX_STATUS_SOURCE_CODE,
                    TAX_VERIFICATION_REFERENCE, TAX_VERIFIED_AT, VALID_FROM, VALID_TO, CREATED_BY
                ) VALUES (
                    :requestId, :taxResidencyCode, :withholdingApplicable,
                    :exemptionCode, :exemptionDocumentReference, :taxStatusSourceCode,
                    :taxVerificationReference, :taxVerifiedAt, :validFrom, :validTo, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("taxResidencyCode", value.taxResidencyCode(), Types.VARCHAR)
                .addValue("withholdingApplicable", value.withholdingApplicable() == null ? 1 : value.withholdingApplicable(), Types.NUMERIC)
                .addValue("exemptionCode", value.exemptionCode(), Types.VARCHAR)
                .addValue("exemptionDocumentReference", value.exemptionDocumentReference(), Types.VARCHAR)
                .addValue("taxStatusSourceCode", value.taxStatusSourceCode(), Types.VARCHAR)
                .addValue("taxVerificationReference", value.taxVerificationReference(), Types.VARCHAR)
                .addValue("taxVerifiedAt", toTimestamp(value.taxVerifiedAt()), Types.TIMESTAMP)
                .addValue("validFrom", value.validFrom() == null ? defaultValidFrom : value.validFrom(), Types.DATE)
                .addValue("validTo", value.validTo(), Types.DATE)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertRewardEnrollment(long requestId, RewardEnrollment value, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_REWARD_ENROLLMENT (
                    OPENING_REQUEST_ID, PROGRAM_ID, ENROLLMENT_STATUS_CODE,
                    CONSENT_REFERENCE, CREATED_BY
                ) VALUES (
                    :requestId, :programId, :enrollmentStatusCode,
                    :consentReference, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("programId", value.programId(), Types.NUMERIC)
                .addValue("enrollmentStatusCode", value.enrollmentStatusCode() == null ? "REQUESTED" : value.enrollmentStatusCode(), Types.VARCHAR)
                .addValue("consentReference", value.consentReference(), Types.VARCHAR)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertFunding(long requestId, Funding value, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_FUNDING (
                    OPENING_REQUEST_ID, FUNDING_METHOD_CODE, FUNDING_AMOUNT, SOURCE_REFERENCE,
                    FUNDING_STATUS_CODE, TRANSACTION_REFERENCE, CREATED_BY
                ) VALUES (
                    :requestId, :fundingMethodCode, :fundingAmount, :sourceReference,
                    :fundingStatusCode, :transactionReference, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("fundingMethodCode", value.fundingMethodCode(), Types.VARCHAR)
                .addValue("fundingAmount", value.fundingAmount(), Types.NUMERIC)
                .addValue("sourceReference", value.sourceReference(), Types.VARCHAR)
                .addValue("fundingStatusCode", value.fundingStatusCode(), Types.VARCHAR)
                .addValue("transactionReference", value.transactionReference(), Types.VARCHAR)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertCheck(long requestId, OpeningCheck value, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_CHECK (
                    OPENING_REQUEST_ID, CHECK_CODE, CHECK_TYPE_CODE, ATTEMPT_NO,
                    RESULT_STATUS_CODE, RESULT_REFERENCE, CHECKED_AT, WAIVER_REASON, CREATED_BY
                ) VALUES (
                    :requestId, :checkCode, :checkTypeCode, :attemptNo,
                    :resultStatusCode, :resultReference,
                    CASE WHEN :resultStatusCode = 'PENDING' THEN NULL ELSE SYSTIMESTAMP END,
                    :waiverReason, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("checkCode", value.checkCode(), Types.VARCHAR)
                .addValue("checkTypeCode", value.checkTypeCode(), Types.VARCHAR)
                .addValue("attemptNo", value.attemptNo() == null ? 1 : value.attemptNo(), Types.NUMERIC)
                .addValue("resultStatusCode", value.resultStatusCode(), Types.VARCHAR)
                .addValue("resultReference", value.resultReference(), Types.VARCHAR)
                .addValue("waiverReason", value.waiverReason(), Types.VARCHAR)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertDocument(long requestId, OpeningDocument value, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_DOCUMENT (
                    OPENING_REQUEST_ID, DOCUMENT_TYPE_CODE, DOCUMENT_STATUS_CODE,
                    DOCUMENT_REFERENCE, VERIFIED_AT, CREATED_BY
                ) VALUES (
                    :requestId, :documentTypeCode, :documentStatusCode,
                    :documentReference, :verifiedAt, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("documentTypeCode", value.documentTypeCode(), Types.VARCHAR)
                .addValue("documentStatusCode", value.documentStatusCode(), Types.VARCHAR)
                .addValue("documentReference", value.documentReference(), Types.VARCHAR)
                .addValue("verifiedAt", toTimestamp(value.verifiedAt()), Types.TIMESTAMP)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertTermsAcceptance(long requestId, TermsAcceptance value, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_TERMS_ACCEPTANCE (
                    OPENING_REQUEST_ID, TERMS_VERSION_CODE, ACCEPTED_BY_PARTY_ID,
                    ACCEPTANCE_SOURCE_CODE, CHANNEL_CODE, ACCEPTANCE_STATUS_CODE,
                    EVIDENCE_REFERENCE, CREATED_BY
                ) VALUES (
                    :requestId, :termsVersionCode, :acceptedByPartyId,
                    :acceptanceSourceCode, :channelCode, :acceptanceStatusCode,
                    :evidenceReference, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("termsVersionCode", value.termsVersionCode(), Types.VARCHAR)
                .addValue("acceptedByPartyId", value.acceptedByPartyId(), Types.NUMERIC)
                .addValue("acceptanceSourceCode", value.acceptanceSourceCode(), Types.VARCHAR)
                .addValue("channelCode", value.channelCode(), Types.VARCHAR)
                .addValue("acceptanceStatusCode", value.acceptanceStatusCode() == null ? "ACCEPTED" : value.acceptanceStatusCode(), Types.VARCHAR)
                .addValue("evidenceReference", value.evidenceReference(), Types.VARCHAR)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertDecision(long requestId, Decision value, String actor) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_DECISION (
                    OPENING_REQUEST_ID, DECISION_CODE, DECISION_REASON_CODE,
                    DECISION_NOTE, DECIDED_BY, CREATED_BY
                ) VALUES (
                    :requestId, :decisionCode, :decisionReasonCode,
                    :decisionNote, :decidedBy, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("decisionCode", value.decisionCode(), Types.VARCHAR)
                .addValue("decisionReasonCode", value.decisionReasonCode(), Types.VARCHAR)
                .addValue("decisionNote", value.decisionNote(), Types.VARCHAR)
                .addValue("decidedBy", value.decidedBy() == null ? actor : value.decidedBy(), Types.VARCHAR)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    public int insertInitialStatusHistory(long requestId, String statusCode, String actor, String correlationId) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_STATUS_HISTORY (
                    OPENING_REQUEST_ID, FROM_STATUS_CODE, TO_STATUS_CODE,
                    CHANGE_NOTE, CHANGED_BY, CORRELATION_ID, CREATED_BY
                ) VALUES (
                    :requestId, NULL, :statusCode,
                    :changeNote, :changedBy, :correlationId, :createdBy
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("requestId", requestId, Types.NUMERIC)
                .addValue("statusCode", statusCode, Types.VARCHAR)
                .addValue("changeNote", "Initial aggregate persistence", Types.VARCHAR)
                .addValue("changedBy", actor, Types.VARCHAR)
                .addValue("correlationId", correlationId, Types.VARCHAR)
                .addValue("createdBy", actor, Types.VARCHAR));
    }

    private long nextValue(String sequenceName) {
        String safeSequence = requireIdentifier(sequenceName);
        return jdbc.getJdbcOperations().queryForObject(
                "SELECT " + schema + "." + safeSequence + ".NEXTVAL FROM DUAL",
                Long.class
        );
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static String requireIdentifier(String raw) {
        if (raw == null) throw new IllegalArgumentException("Oracle identifier is required.");
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z][A-Z0-9_$#]{0,127}")) {
            throw new IllegalArgumentException("Invalid Oracle identifier: " + raw);
        }
        return normalized;
    }

    public record ExistingRequest(long openingRequestId, String requestNo, String idempotencyKey, String requestStatusCode) {
    }
}
