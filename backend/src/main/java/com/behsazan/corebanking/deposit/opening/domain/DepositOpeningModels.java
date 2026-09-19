package com.behsazan.corebanking.deposit.opening.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class DepositOpeningModels {
    private DepositOpeningModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AggregateRequest(
            @JsonProperty("DEPOSIT_OPENING_REQUEST") OpeningRequest request,
            @JsonProperty("DEPOSIT_OPENING_PARTY") List<OpeningParty> parties,
            @JsonProperty("DEPOSIT_OPENING_SIGNATURE_RULE") SignatureRule signatureRule,
            @JsonProperty("DEPOSIT_OPENING_SIGNATORY") List<Signatory> signatories,
            @JsonProperty("DEPOSIT_OPENING_SIGNATORY_AUTHORITY") List<SignatoryAuthority> signatoryAuthorities,
            @JsonProperty("DEPOSIT_OPENING_AUTHORIZED_USER") List<AuthorizedUser> authorizedUsers,
            @JsonProperty("DEPOSIT_OPENING_DELEGATION") List<Delegation> delegations,
            @JsonProperty("DEPOSIT_OPENING_BENEFICIARY") List<Beneficiary> beneficiaries,
            @JsonProperty("DEPOSIT_OPENING_TERM") OpeningTerm term,
            @JsonProperty("DEPOSIT_OPENING_MATURITY_INSTRUCTION") MaturityInstruction maturityInstruction,
            @JsonProperty("DEPOSIT_OPENING_PROFIT_INSTRUCTION") ProfitInstruction profitInstruction,
            @JsonProperty("DEPOSIT_OPENING_WITHDRAWAL_MEDIA") List<WithdrawalMedia> withdrawalMedia,
            @JsonProperty("DEPOSIT_OPENING_SERVICE_SELECTION") List<ServiceSelection> serviceSelections,
            @JsonProperty("DEPOSIT_OPENING_PAYMENT_INSTRUMENT") List<PaymentInstrument> paymentInstruments,
            @JsonProperty("DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST") List<PricingOverrideRequest> pricingOverrideRequests,
            @JsonProperty("DEPOSIT_OPENING_TAX_STATUS") TaxStatus taxStatus,
            @JsonProperty("DEPOSIT_OPENING_REWARD_ENROLLMENT") List<RewardEnrollment> rewardEnrollments,
            @JsonProperty("DEPOSIT_OPENING_OBLIGATION") List<OpeningObligation> obligations,
            @JsonProperty("DEPOSIT_OPENING_FUNDING") List<Funding> fundings,
            @JsonProperty("DEPOSIT_OPENING_FUND_ALLOC") List<FundAllocation> fundAllocations,
            @JsonProperty("DEPOSIT_OPENING_CHECK") List<OpeningCheck> checks,
            @JsonProperty("DEPOSIT_OPENING_DOCUMENT") List<OpeningDocument> documents,
            @JsonProperty("DEPOSIT_OPENING_TERMS_ACCEPTANCE") TermsAcceptance termsAcceptance,
            @JsonProperty("DEPOSIT_OPENING_DECISION") Decision decision
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpeningRequest(
            @JsonProperty("REQUEST_NO") String requestNo,
            @JsonProperty("IDEMPOTENCY_KEY") String idempotencyKey,
            @JsonProperty("PRODUCT_VERSION_ID") Long productVersionId,
            @JsonProperty("REQUEST_TYPE_CODE") String requestTypeCode,
            @JsonProperty("OWNERSHIP_TYPE_CODE") String ownershipTypeCode,
            @JsonProperty("CURRENCY_CODE") String currencyCode,
            @JsonProperty("OPENING_CHANNEL_CODE") String openingChannelCode,
            @JsonProperty("ORG_UNIT_CODE") String orgUnitCode,
            @JsonProperty("REQUESTED_OPENING_DATE") LocalDate requestedOpeningDate,
            @JsonProperty("OPENING_AMOUNT") BigDecimal openingAmount,
            @JsonProperty("SOURCE_OF_FUNDS_CODE") String sourceOfFundsCode,
            @JsonProperty("PURPOSE_CODE") String purposeCode,
            @JsonProperty("CUSTOMER_RISK_LEVEL_CODE") String customerRiskLevelCode,
            @JsonProperty("RISK_ASSESSMENT_REFERENCE") String riskAssessmentReference,
            @JsonProperty("EXPECTED_ACTIVITY_REFERENCE") String expectedActivityReference,
            @JsonProperty("JOINT_ACCOUNT_BASIS_CODE") String jointAccountBasisCode,
            @JsonProperty("JOINT_BASIS_REFERENCE") String jointBasisReference,
            @JsonProperty("ACTIVATION_STATUS_CODE") String activationStatusCode,
            @JsonProperty("ACTIVATION_DEADLINE_AT") OffsetDateTime activationDeadlineAt,
            @JsonProperty("REQUEST_STATUS_CODE") String requestStatusCode,
            @JsonProperty("BATCH_ITEM_ID") Long batchItemId
    ) {
        public OpeningRequest(
                String requestNo, String idempotencyKey, Long productVersionId, String requestTypeCode,
                String ownershipTypeCode, String currencyCode, String openingChannelCode, String orgUnitCode,
                LocalDate requestedOpeningDate, BigDecimal openingAmount, String sourceOfFundsCode,
                String purposeCode, String requestStatusCode
        ) {
            this(requestNo, idempotencyKey, productVersionId, requestTypeCode, ownershipTypeCode, currencyCode,
                    openingChannelCode, orgUnitCode, requestedOpeningDate, openingAmount, sourceOfFundsCode, purposeCode,
                    null, null, null, null, null, "NOT_CREATED", null, requestStatusCode, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpeningParty(
            @JsonProperty("PARTY_ID") Long partyId,
            @JsonProperty("ROLE_CODE") String roleCode,
            @JsonProperty("IS_PRIMARY") Integer isPrimary,
            @JsonProperty("OWNERSHIP_PERCENT") BigDecimal ownershipPercent,
            @JsonProperty("SEQUENCE_NO") Integer sequenceNo
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SignatureRule(
            @JsonProperty("SIGNATURE_RULE_CODE") String signatureRuleCode,
            @JsonProperty("MIN_SIGNATURE_COUNT") Integer minSignatureCount,
            @JsonProperty("VALID_FROM") LocalDate validFrom,
            @JsonProperty("VALID_TO") LocalDate validTo
    ) {
    }

    /**
     * OPENING_SIGNATORY_ID is accepted as a client-side correlation key in the create aggregate payload.
     * The repository always allocates the persisted Oracle identifier from DPS2.SEQ_DEPOSIT_OPENING_SIGNATORY.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Signatory(
            @JsonProperty("OPENING_SIGNATORY_ID") Long openingSignatoryId,
            @JsonProperty("PARTY_ID") Long partyId,
            @JsonProperty("SIGNATORY_ROLE_CODE") String signatoryRoleCode,
            @JsonProperty("SIGNATURE_SEQUENCE_NO") Integer signatureSequenceNo,
            @JsonProperty("IS_PRIMARY_SIGNATORY") Integer isPrimarySignatory,
            @JsonProperty("VERIFICATION_STATUS_CODE") String verificationStatusCode,
            @JsonProperty("VALID_FROM") LocalDate validFrom,
            @JsonProperty("VALID_TO") LocalDate validTo
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SignatoryAuthority(
            @JsonProperty("OPENING_SIGNATORY_ID") Long openingSignatoryId,
            @JsonProperty("OPERATION_CODE") String operationCode,
            @JsonProperty("CHANNEL_CODE") String channelCode,
            @JsonProperty("MAX_AMOUNT") BigDecimal maxAmount,
            @JsonProperty("CURRENCY_CODE") String currencyCode,
            @JsonProperty("REQUIRES_COSIGN") Integer requiresCosign,
            @JsonProperty("VALID_FROM") LocalDate validFrom,
            @JsonProperty("VALID_TO") LocalDate validTo
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthorizedUser(
            @JsonProperty("PARTY_ID") Long partyId,
            @JsonProperty("ACCESS_ROLE_CODE") String accessRoleCode,
            @JsonProperty("CHANNEL_SCOPE_CODE") String channelScopeCode,
            @JsonProperty("VALID_FROM") LocalDate validFrom,
            @JsonProperty("VALID_TO") LocalDate validTo,
            @JsonProperty("STATUS_CODE") String statusCode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Delegation(
            @JsonProperty("GRANTOR_PARTY_ID") Long grantorPartyId,
            @JsonProperty("DELEGATE_PARTY_ID") Long delegatePartyId,
            @JsonProperty("DELEGATION_TYPE_CODE") String delegationTypeCode,
            @JsonProperty("AUTHORITY_SCOPE_CODE") String authorityScopeCode,
            @JsonProperty("DOCUMENT_REFERENCE") String documentReference,
            @JsonProperty("VALID_FROM") LocalDate validFrom,
            @JsonProperty("VALID_TO") LocalDate validTo,
            @JsonProperty("VERIFICATION_STATUS_CODE") String verificationStatusCode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Beneficiary(
            @JsonProperty("BENEFICIARY_PARTY_ID") Long beneficiaryPartyId,
            @JsonProperty("BENEFICIARY_REFERENCE") String beneficiaryReference,
            @JsonProperty("BENEFICIARY_TYPE_CODE") String beneficiaryTypeCode,
            @JsonProperty("SHARE_PERCENT") BigDecimal sharePercent,
            @JsonProperty("VALID_FROM") LocalDate validFrom,
            @JsonProperty("VALID_TO") LocalDate validTo
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpeningTerm(
            @JsonProperty("ALLOWED_TERM_ID") Long allowedTermId,
            @JsonProperty("TERM_CODE") String termCode,
            @JsonProperty("TERM_VALUE") Integer termValue,
            @JsonProperty("TERM_UNIT_CODE") String termUnitCode,
            @JsonProperty("START_DATE") LocalDate startDate,
            @JsonProperty("MATURITY_DATE") LocalDate maturityDate,
            @JsonProperty("AUTO_RENEW_FLAG") Integer autoRenewFlag
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MaturityInstruction(
            @JsonProperty("MATURITY_ACTION_CODE") String maturityActionCode,
            @JsonProperty("SETTLEMENT_ACCOUNT_REFERENCE") String settlementAccountReference,
            @JsonProperty("INSTRUCTION_SOURCE_CODE") String instructionSourceCode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProfitInstruction(
            @JsonProperty("PRICING_RULE_ID") Long pricingRuleId,
            @JsonProperty("PRICING_COMPONENT_ID") Long pricingComponentId,
            @JsonProperty("RATE_TIER_ID") Long rateTierId,
            @JsonProperty("PROFIT_PAYMENT_RULE_ID") Long profitPaymentRuleId,
            @JsonProperty("RATE_VALUE") BigDecimal rateValue,
            @JsonProperty("CALCULATION_METHOD_CODE") String calculationMethodCode,
            @JsonProperty("DAY_COUNT_BASIS_CODE") String dayCountBasisCode,
            @JsonProperty("ACCRUAL_FREQUENCY_CODE") String accrualFrequencyCode,
            @JsonProperty("PAYMENT_FREQUENCY_CODE") String paymentFrequencyCode,
            @JsonProperty("PAYMENT_DAY_RULE_CODE") String paymentDayRuleCode,
            @JsonProperty("FIRST_PAYMENT_RULE_CODE") String firstPaymentRuleCode,
            @JsonProperty("HOLIDAY_ADJUSTMENT_CODE") String holidayAdjustmentCode,
            @JsonProperty("PAYMENT_DESTINATION_CODE") String paymentDestinationCode,
            @JsonProperty("DESTINATION_ACCOUNT_REFERENCE") String destinationAccountReference,
            @JsonProperty("DESTINATION_SELECTED_BY_CUSTOMER") Integer destinationSelectedByCustomer
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WithdrawalMedia(
            @JsonProperty("WITHDRAWAL_MEDIA_CODE") String withdrawalMediaCode,
            @JsonProperty("REQUESTED_QUANTITY") Integer requestedQuantity,
            @JsonProperty("REQUEST_STATUS_CODE") String requestStatusCode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ServiceSelection(
            @JsonProperty("SERVICE_CODE") String serviceCode,
            @JsonProperty("CHANNEL_CODE") String channelCode,
            @JsonProperty("DELIVERY_TARGET") String deliveryTarget,
            @JsonProperty("API_SCOPE_CODE") String apiScopeCode,
            @JsonProperty("REQUEST_STATUS_CODE") String requestStatusCode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentInstrument(
            @JsonProperty("INSTRUMENT_TYPE_CODE") String instrumentTypeCode,
            @JsonProperty("REQUESTED_QUANTITY") Integer requestedQuantity,
            @JsonProperty("LINKED_PARTY_ID") Long linkedPartyId,
            @JsonProperty("REQUEST_STATUS_CODE") String requestStatusCode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PricingOverrideRequest(
            @JsonProperty("PRICING_RULE_ID") Long pricingRuleId,
            @JsonProperty("OVERRIDE_TYPE_CODE") String overrideTypeCode,
            @JsonProperty("REQUESTED_VALUE") BigDecimal requestedValue,
            @JsonProperty("AUTHORITY_LEVEL_CODE") String authorityLevelCode,
            @JsonProperty("APPROVAL_REFERENCE") String approvalReference,
            @JsonProperty("REQUEST_STATUS_CODE") String requestStatusCode,
            @JsonProperty("EFFECTIVE_FROM") LocalDate effectiveFrom,
            @JsonProperty("EFFECTIVE_TO") LocalDate effectiveTo
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TaxStatus(
            @JsonProperty("TAX_RESIDENCY_CODE") String taxResidencyCode,
            @JsonProperty("WITHHOLDING_APPLICABLE") Integer withholdingApplicable,
            @JsonProperty("EXEMPTION_CODE") String exemptionCode,
            @JsonProperty("EXEMPTION_DOCUMENT_REFERENCE") String exemptionDocumentReference,
            @JsonProperty("TAX_STATUS_SOURCE_CODE") String taxStatusSourceCode,
            @JsonProperty("TAX_VERIFICATION_REFERENCE") String taxVerificationReference,
            @JsonProperty("TAX_VERIFIED_AT") OffsetDateTime taxVerifiedAt,
            @JsonProperty("VALID_FROM") LocalDate validFrom,
            @JsonProperty("VALID_TO") LocalDate validTo
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RewardEnrollment(
            @JsonProperty("PROGRAM_ID") Long programId,
            @JsonProperty("ENROLLMENT_STATUS_CODE") String enrollmentStatusCode,
            @JsonProperty("CONSENT_REFERENCE") String consentReference
    ) {
    }

    /**
     * OPENING_FUNDING_ID is a client-side correlation key on create; Oracle persistence allocates its own PK.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Funding(
            @JsonProperty("OPENING_FUNDING_ID") Long openingFundingId,
            @JsonProperty("FUNDING_METHOD_CODE") String fundingMethodCode,
            @JsonProperty("FUNDING_AMOUNT") BigDecimal fundingAmount,
            @JsonProperty("SOURCE_PARTY_ID") Long sourcePartyId,
            @JsonProperty("SOURCE_ACCOUNT_ID") Long sourceAccountId,
            @JsonProperty("SOURCE_REFERENCE") String sourceReference,
            @JsonProperty("FUNDING_PURPOSE_CODE") String fundingPurposeCode,
            @JsonProperty("SOURCE_OWNERSHIP_VERIFIED_FLAG") Integer sourceOwnershipVerifiedFlag,
            @JsonProperty("SOURCE_VERIFICATION_REFERENCE") String sourceVerificationReference,
            @JsonProperty("CASH_MANAGEMENT_TXN_REF") String cashManagementTxnRef,
            @JsonProperty("FUNDING_STATUS_CODE") String fundingStatusCode,
            @JsonProperty("TRANSACTION_REFERENCE") String transactionReference,
            @JsonProperty("ATTEMPT_AT") OffsetDateTime attemptAt
    ) {
    }

    /**
     * OPENING_OBLIGATION_ID is a client-side correlation key on create; Oracle persistence allocates its own PK.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpeningObligation(
            @JsonProperty("OPENING_OBLIGATION_ID") Long openingObligationId,
            @JsonProperty("OBLIGATION_TYPE_CODE") String obligationTypeCode,
            @JsonProperty("SOURCE_SYSTEM_CODE") String sourceSystemCode,
            @JsonProperty("SOURCE_REFERENCE") String sourceReference,
            @JsonProperty("DESCRIPTION") String description,
            @JsonProperty("GROSS_AMOUNT") BigDecimal grossAmount,
            @JsonProperty("WAIVED_AMOUNT") BigDecimal waivedAmount,
            @JsonProperty("FINAL_AMOUNT") BigDecimal finalAmount,
            @JsonProperty("CURRENCY_CODE") String currencyCode,
            @JsonProperty("MANDATORY_FOR_ACTIVATION_FLAG") Integer mandatoryForActivationFlag,
            @JsonProperty("SETTLEMENT_STATUS_CODE") String settlementStatusCode,
            @JsonProperty("SETTLEMENT_REFERENCE") String settlementReference,
            @JsonProperty("WAIVER_REFERENCE") String waiverReference,
            @JsonProperty("SETTLED_AT") OffsetDateTime settledAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FundAllocation(
            @JsonProperty("OPENING_FUNDING_ID") Long openingFundingId,
            @JsonProperty("OPENING_OBLIGATION_ID") Long openingObligationId,
            @JsonProperty("ALLOCATED_AMOUNT") BigDecimal allocatedAmount,
            @JsonProperty("ALLOCATION_STATUS_CODE") String allocationStatusCode,
            @JsonProperty("SETTLEMENT_REFERENCE") String settlementReference
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpeningCheck(
            @JsonProperty("CHECK_CODE") String checkCode,
            @JsonProperty("CHECK_TYPE_CODE") String checkTypeCode,
            @JsonProperty("ATTEMPT_NO") Integer attemptNo,
            @JsonProperty("CHECK_PHASE_CODE") String checkPhaseCode,
            @JsonProperty("BLOCKING_SCOPE_CODE") String blockingScopeCode,
            @JsonProperty("REQUIRED_FLAG") Integer requiredFlag,
            @JsonProperty("RECHECK_REQUIRED_FLAG") Integer recheckRequiredFlag,
            @JsonProperty("RESULT_STATUS_CODE") String resultStatusCode,
            @JsonProperty("RESULT_REFERENCE") String resultReference,
            @JsonProperty("CHECKED_AT") OffsetDateTime checkedAt,
            @JsonProperty("VALID_UNTIL") OffsetDateTime validUntil,
            @JsonProperty("SOURCE_EVALUATION_REFERENCE") String sourceEvaluationReference,
            @JsonProperty("WAIVER_REASON") String waiverReason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpeningDocument(
            @JsonProperty("DOCUMENT_TYPE_CODE") String documentTypeCode,
            @JsonProperty("DOCUMENT_STATUS_CODE") String documentStatusCode,
            @JsonProperty("DOCUMENT_REFERENCE") String documentReference,
            @JsonProperty("VERIFIED_AT") OffsetDateTime verifiedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TermsAcceptance(
            @JsonProperty("TERMS_VERSION_CODE") String termsVersionCode,
            @JsonProperty("ACCEPTED_BY_PARTY_ID") Long acceptedByPartyId,
            @JsonProperty("ACCEPTANCE_SOURCE_CODE") String acceptanceSourceCode,
            @JsonProperty("CHANNEL_CODE") String channelCode,
            @JsonProperty("ACCEPTANCE_STATUS_CODE") String acceptanceStatusCode,
            @JsonProperty("EVIDENCE_REFERENCE") String evidenceReference
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Decision(
            @JsonProperty("DECISION_CODE") String decisionCode,
            @JsonProperty("DECISION_REASON_CODE") String decisionReasonCode,
            @JsonProperty("DECISION_NOTE") String decisionNote,
            @JsonProperty("DECIDED_BY") String decidedBy
    ) {
    }

    public record PersistedAggregateResponse(
            long openingRequestId,
            String requestNo,
            String idempotencyKey,
            String requestStatusCode,
            boolean idempotentReplay,
            Map<String, Integer> persistedRows
    ) {
    }
}
