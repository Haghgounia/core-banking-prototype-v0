package com.behsazan.corebanking.deposit.account.wavec.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class DepositAccountWaveCModels {
    private DepositAccountWaveCModels(){}

    public record InquiryRequest(String inquiryTypeCode,String channelCode,Long requestedByPartyId){}
    public record ConfirmationRequest(String confirmationTypeCode,String deliveryChannelCode){}
    public record NotificationPreferenceRequest(String eventCode,String channelCode,String deliveryTarget,Boolean enabled,LocalDate validFrom,LocalDate validTo){}
    public record NotificationEventRequest(String eventCode,String eventReference,String channelCode,String deliveryTarget,String providerReference){}
    public record ApiAccessRequest(String clientReference,String apiScopeCode,Long authorizedPartyId,String consentReference,OffsetDateTime validFrom,OffsetDateTime validTo){}
    public record SignatureRuleRequest(String signatureRuleCode,Integer minSignatureCount,LocalDate validFrom,LocalDate validTo){}
    public record DelegationRequest(long grantorPartyId,long delegatePartyId,String delegationTypeCode,String authorityScopeCode,BigDecimal maxAmount,String documentReference,LocalDate validFrom,LocalDate validTo){}
    public record AuthorizedUserRequest(long partyId,String accessRoleCode,String channelScopeCode,LocalDate validFrom,LocalDate validTo){}
    public record BeneficiaryRequest(Long beneficiaryPartyId,String beneficiaryReference,String beneficiaryTypeCode,BigDecimal sharePercent,LocalDate validFrom,LocalDate validTo){}
    public record PaymentInstrumentRequest(String instrumentTypeCode,String instrumentReference,Long holderPartyId,OffsetDateTime issuedAt,OffsetDateTime expiresAt){}
    public record RegulatoryRestrictionRequest(Long regulatoryRuleId,String restrictionTypeCode,String transactionTypeCode,String channelCode,BigDecimal limitAmount,String legalOrderReference,String issuingAuthorityCode,OffsetDateTime validFrom,OffsetDateTime validTo,String releasePolicyCode){}
    public record ComplianceEvaluationRequest(long regulatoryRuleId,String resultStatusCode,String resultReasonCode,String engineReference,String waiverReference,String evaluationPhaseCode,OffsetDateTime validUntil,Boolean recheckRequired){}
    public record PricingOverrideRequest(Long pricingRuleId,String overrideTypeCode,BigDecimal baseValue,BigDecimal overrideValue,String authorityLevelCode,String approverUserId,LocalDate effectiveFrom,LocalDate effectiveTo){}
    public record TaxExemptionRequest(long partyId,String exemptionCode,String taxTypeCode,String documentReference,LocalDate validFrom,LocalDate validTo){}
    public record TaxCalculationRequest(long taxRuleId,Long profitPaymentId,Long transactionId,BigDecimal taxableBaseAmount){}
    public record TaxCertificateRequest(long partyId,String taxTypeCode,LocalDate periodStart,LocalDate periodEnd,BigDecimal grossAmount,BigDecimal taxAmount,String documentReference){}

    public record Inquiry(long accountInquiryId,long accountId,String inquiryTypeCode,String channelCode,String requestReference,Long requestedByPartyId,OffsetDateTime asOfTimestamp,String resultStatusCode,String responseReference){}
    public record Confirmation(long accountConfirmationId,long accountId,String confirmationNo,String confirmationTypeCode,LocalDate asOfDate,BigDecimal balanceAmount,String accountStatusCode,String currencyCode,OffsetDateTime issuedAt,String documentReference,String deliveryChannelCode){}
    public record NotificationPreference(long notificationPreferenceId,long accountId,String eventCode,String channelCode,String deliveryTarget,boolean enabled,LocalDate validFrom,LocalDate validTo,long recordVersion){}
    public record NotificationEvent(long notificationEventId,long accountId,String eventCode,String eventReference,String channelCode,String deliveryTarget,String deliveryStatusCode,long attemptNo,OffsetDateTime sentAt,OffsetDateTime deliveredAt,String providerReference){}
    public record ApiAccess(long accountApiAccessId,long accountId,String clientReference,String apiScopeCode,Long authorizedPartyId,String consentReference,String statusCode,OffsetDateTime validFrom,OffsetDateTime validTo,OffsetDateTime lastUsedAt,long recordVersion){}
    public record AccountServicesView(List<Inquiry> inquiries,List<Confirmation> confirmations,List<NotificationPreference> notificationPreferences,List<NotificationEvent> notificationEvents,List<ApiAccess> apiAccesses){}

    public record SignatureRule(long accountSignatureRuleId,long accountId,String signatureRuleCode,Integer minSignatureCount,LocalDate validFrom,LocalDate validTo,String statusCode,long recordVersion){}
    public record Delegation(long accountDelegationId,long accountId,long grantorPartyId,long delegatePartyId,String delegationTypeCode,String authorityScopeCode,BigDecimal maxAmount,String documentReference,LocalDate validFrom,LocalDate validTo,String statusCode,long recordVersion){}
    public record AuthorizedUser(long accountAuthorizedUserId,long accountId,long partyId,String accessRoleCode,String channelScopeCode,LocalDate validFrom,LocalDate validTo,String statusCode,long recordVersion){}
    public record Beneficiary(long accountBeneficiaryId,long accountId,Long beneficiaryPartyId,String beneficiaryReference,String beneficiaryTypeCode,BigDecimal sharePercent,LocalDate validFrom,LocalDate validTo,String statusCode,long recordVersion){}
    public record PaymentInstrument(long accountPaymentInstrumentId,long accountId,String instrumentTypeCode,String instrumentReference,Long holderPartyId,String statusCode,OffsetDateTime issuedAt,OffsetDateTime expiresAt,long recordVersion){}
    public record PartyAccessView(List<SignatureRule> signatureRules,List<Delegation> delegations,List<AuthorizedUser> authorizedUsers,List<Beneficiary> beneficiaries,List<PaymentInstrument> paymentInstruments){}

    public record RegulatoryRestriction(long accountRegRestrictionId,long accountId,Long regulatoryRuleId,String restrictionTypeCode,String transactionTypeCode,String channelCode,BigDecimal limitAmount,String legalOrderReference,String issuingAuthorityCode,OffsetDateTime validFrom,OffsetDateTime validTo,String statusCode,String releasePolicyCode,long recordVersion){}
    public record ComplianceEvaluation(long complianceEvaluationId,long regulatoryRuleId,long accountId,String resultStatusCode,String resultReasonCode,OffsetDateTime evaluatedAt,String engineReference,String waiverReference,String evaluationPhaseCode,OffsetDateTime validUntil,boolean recheckRequired){}
    public record ComplianceView(List<RegulatoryRestriction> restrictions,List<ComplianceEvaluation> evaluations){}

    public record PricingOverride(long accountPricingOverrideId,long accountId,Long pricingRuleId,String overrideTypeCode,BigDecimal baseValue,BigDecimal overrideValue,String authorityLevelCode,Long approvalRequestId,String approvalStatusCode,String approverUserId,LocalDate effectiveFrom,LocalDate effectiveTo,String statusCode,long recordVersion){}
    public record FeeAssessment(long feeAssessmentId,long accountId,Long transactionId,long feeRuleId,BigDecimal baseAmount,BigDecimal calculatedFeeAmount,BigDecimal discountAmount,BigDecimal finalFeeAmount,String assessmentStatusCode,String postingReference,String collectionMethodCode,Long collectionTransactionId,String waiverReference){}
    public record ProfitabilitySnapshot(long profitabilitySnapshotId,Long accountId,Long productVersionId,Long partyId,LocalDate periodStart,LocalDate periodEnd,BigDecimal averageBalance,BigDecimal interestExpense,BigDecimal feeRevenue,BigDecimal otherRevenue,BigDecimal directCost,BigDecimal netMargin,OffsetDateTime calculatedAt){}
    public record PricingView(List<PricingOverride> overrides,List<FeeAssessment> feeAssessments,List<ProfitabilitySnapshot> profitabilitySnapshots){}

    public record TaxExemption(long taxExemptionId,long partyId,Long accountId,String exemptionCode,String taxTypeCode,String documentReference,LocalDate validFrom,LocalDate validTo,String statusCode,long recordVersion){}
    public record TaxCalculation(long taxCalculationId,long accountId,long taxRuleId,Long profitPaymentId,Long transactionId,BigDecimal taxableBaseAmount,BigDecimal exemptAmount,BigDecimal taxRate,BigDecimal taxAmount,String calculationStatusCode){}
    public record TaxCertificate(long taxCertificateId,long accountId,long partyId,String certificateNo,String taxTypeCode,LocalDate periodStart,LocalDate periodEnd,BigDecimal grossAmount,BigDecimal taxAmount,OffsetDateTime issuedAt,String documentReference){}
    public record TaxView(List<TaxExemption> exemptions,List<TaxCalculation> calculations,List<TaxCertificate> certificates){}

    public record WaveCView(AccountServicesView services,PartyAccessView partyAccess,ComplianceView compliance,PricingView pricing,TaxView tax){}
}
