package com.behsazan.corebanking.cif.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class CifModels {
    private CifModels() {
    }

    public record PartySummary(
            long partyId,
            String partyUid,
            String partyTypeCode,
            String lifecycleStatusCode,
            String verificationStatusCode,
            String dataQualityStatusCode,
            String displayName,
            String primaryIdentifier,
            LocalDate validFrom,
            LocalDate validTo,
            long recordVersion
    ) {
    }

    public record PartyCore(
            long partyId,
            String partyUid,
            String partyTypeCode,
            String lifecycleStatusCode,
            String statusReasonCode,
            LocalDateTime statusChangedAt,
            String verificationStatusCode,
            String dataQualityStatusCode,
            String creationSourceCode,
            Long mergedIntoPartyId,
            LocalDateTime mergedAt,
            String mergedBy,
            LocalDate validFrom,
            LocalDate validTo,
            String isCurrent,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime updatedAt,
            String updatedBy,
            long recordVersion
    ) {
    }

    public record PersonProfile(
            long partyId,
            LocalDate birthDate,
            String genderCode,
            String birthCountryCode,
            Long birthPlaceId,
            String birthPlaceText,
            String fatherGivenName,
            String motherGivenName,
            String maritalStatusCode,
            LocalDate deathDate,
            String legalCapacityCode,
            String primaryLanguageCode,
            String dataQualityStatusCode,
            String verificationStatusCode,
            String residenceStatusCode,
            String physicalAbility,
            String lifeStatusCode,
            String nationalityCountryCode,
            long recordVersion
    ) {
    }

    public record OrganizationProfile(
            long organizationId,
            long partyId,
            String registeredName,
            String tradeName,
            String legalFormCode,
            String registrationNo,
            String registrationPlaceCode,
            LocalDate incorporationDate,
            LocalDate dissolutionDate,
            String economicSectorCode,
            String isicCode,
            String listedCompanyFlag,
            String registrationCountryCode,
            String activityStatusCode,
            String mainActivityDescription,
            Long employeeCount,
            String enterpriseSizeCode,
            String ownershipTypeCode,
            long recordVersion
    ) {
    }

    public record PartyNameRecord(
            long partyNameId,
            long partyId,
            String nameTypeCode,
            String languageCode,
            String scriptCode,
            String prefixText,
            String givenName,
            String middleName,
            String familyName,
            String suffixText,
            String fullName,
            String displayName,
            String sortName,
            String normalizedName,
            String phoneticKey,
            String isPrimary,
            LocalDate validFrom,
            LocalDate validTo,
            String verificationStatusCode,
            String sourceCode,
            String sourceReference,
            long recordVersion
    ) {
    }

    public record PartyIdentifierRecord(
            long partyIdentifierId,
            long partyId,
            String identifierTypeCode,
            String identifierValue,
            String normalizedIdentifierValue,
            String issuingCountryCode,
            String issuingAuthorityCode,
            String issuerCode,
            LocalDate issueDate,
            LocalDate expiryDate,
            String isPrimary,
            String isActive,
            String verificationStatusCode,
            String verificationSourceCode,
            String verificationMethodCode,
            LocalDateTime verifiedAt,
            LocalDate validFrom,
            LocalDate validTo,
            long recordVersion
    ) {
    }

    public record PartyAddressRecord(
            long partyAddressId,
            long addressId,
            long partyId,
            String addressTypeCode,
            String isPrimary,
            LocalDate validFrom,
            LocalDate validTo,
            String countryCode,
            String provinceCode,
            String countyCode,
            String cityCode,
            String districtCode,
            String postalCode,
            String addressLine1,
            String addressLine2,
            String neighborhoodText,
            String mainStreetText,
            String sideStreetText,
            String plaqueNo,
            String floorNo,
            String unitNo,
            String addressDetail,
            String tenureTypeCode,
            String verificationStatusCode,
            String sourceCode,
            long partyAddressRecordVersion,
            long addressRecordVersion
    ) {
    }

    public record ContactPointRecord(
            long contactPointId,
            long partyId,
            String contactTypeCode,
            String contactValue,
            String normalizedValue,
            String purposeCode,
            String isPrimary,
            String isVerified,
            LocalDateTime verifiedAt,
            LocalDate validFrom,
            LocalDate validTo,
            String countryDialCode,
            String areaCode,
            String extensionNo,
            String ownerTypeCode,
            String verificationStatusCode,
            String verificationMethodCode,
            long recordVersion
    ) {
    }

    public record ContactPointAddressRecord(
            long contactPointAddressId,
            long contactPointId,
            long partyAddressId,
            String associationTypeCode,
            String isPrimaryForAddress,
            LocalDate validFrom,
            LocalDate validTo,
            long recordVersion
    ) {
    }

    public record FinancialProfileRecord(
            long financialProfileId,
            long partyId,
            LocalDate asOfDate,
            BigDecimal annualIncome,
            BigDecimal totalAssets,
            BigDecimal totalLiabilities,
            String currencyCode,
            String sourceOfFundsCode,
            String sourceOfWealthCode,
            BigDecimal expectedMonthlyTurnover,
            String taxStatusCode,
            String verificationStatusCode,
            BigDecimal netMonthlyIncome,
            BigDecimal otherMonthlyIncome,
            Long expectedMonthlyTxnCount,
            String fundsCountriesText,
            String financialRelationPurposeCode,
            BigDecimal realEstateValue,
            BigDecimal investmentValue,
            BigDecimal totalMonthlyInstallment,
            BigDecimal estimatedNetWorth,
            String financialCapacityCode,
            long recordVersion
    ) {
    }

    public record PartyEmploymentRecord(
            long employmentId,
            long partyId,
            Long employerPartyId,
            String employerName,
            String occupationCode,
            String jobTitle,
            String economicSectorCode,
            String isicCode,
            BigDecimal monthlyIncome,
            String incomeCurrencyCode,
            String familyRange,
            String jobStatus,
            String employeeRange,
            LocalDate validFrom,
            LocalDate validTo,
            String employmentStatusCode,
            String occupationGroupCode,
            String employerIdentifier,
            String contractTypeCode,
            String insuranceNo,
            String taxCode,
            long recordVersion
    ) {
    }

    public record PartyIncomeSourceRecord(
            long incomeSourceId,
            long partyId,
            String sourceTypeCode,
            BigDecimal monthlyAmount,
            String currencyCode,
            String documentedFlag,
            String statusCode,
            long recordVersion
    ) {
    }

    public record PartyAssetLiabilityRecord(
            long assetLiabilityId,
            long partyId,
            String itemTypeCode,
            String descriptionText,
            BigDecimal amount,
            String currencyCode,
            LocalDate assessmentDate,
            String statusCode,
            long recordVersion
    ) {
    }

    public record PartyLicenseRecord(
            long licenseId,
            long partyId,
            String licenseTypeCode,
            String licenseNumber,
            Long issuerPartyId,
            String issuerName,
            LocalDate issueDate,
            LocalDate expiryDate,
            String statusCode,
            String documentRef,
            long recordVersion
    ) {
    }

    public record PartyClassificationRecord(
            long partyClassificationId,
            long partyId,
            String classificationTypeCode,
            String classificationValueCode,
            String assignmentReasonCode,
            LocalDate validFrom,
            LocalDate validTo,
            String descriptionText,
            long recordVersion
    ) {
    }


    public record PartyRelationshipRecord(
            long partyRelationshipId,
            long partyId,
            long relatedPartyId,
            String relationshipTypeCode,
            BigDecimal ownershipPercent,
            String positionTitle,
            String signingRightCode,
            BigDecimal authorityLimitAmount,
            LocalDate startDate,
            LocalDate endDate,
            Long evidenceDocumentId,
            String verificationStatusCode,
            long recordVersion
    ) {
    }

    public record BeneficialOwnershipRecord(
            long ownershipId,
            long legalPartyId,
            long beneficialOwnerPartyId,
            BigDecimal directOwnershipPercent,
            BigDecimal indirectOwnershipPercent,
            BigDecimal controlPercent,
            String controlBasisCode,
            String isUltimateOwner,
            String ownershipPath,
            LocalDate validFrom,
            LocalDate validTo,
            String evidenceRef,
            long recordVersion
    ) {
    }

    public record PartyAuthorityRecord(
            long authorityId,
            long principalPartyId,
            long authorizedPartyId,
            String authorityTypeCode,
            String scopeCode,
            BigDecimal maxAmount,
            String currencyCode,
            LocalDate validFrom,
            LocalDate validTo,
            String documentRef,
            long partyId,
            long recordVersion
    ) {
    }

    public record PartyRoleRecord(
            long partyRoleId,
            long partyId,
            String roleTypeCode,
            String contextTypeCode,
            String contextId,
            LocalDate validFrom,
            LocalDate validTo,
            String statusCode,
            long recordVersion,
            Long principalPartyId,
            String relationshipTypeCode,
            String authorityBasisCode,
            String authorityDocumentNo,
            String authorityIssuer,
            String authorityScopeText,
            String assignmentReasonText,
            String descriptionText
    ) {
    }

    public record PartyCustomerRecord(
            long partyCustomerId,
            long partyId,
            long partyRoleId,
            String customerNo,
            String customerStatusCode,
            LocalDate validFrom,
            LocalDate validTo,
            String isCurrent,
            long recordVersion
    ) {
    }

    public record KycCaseRecord(
            long kycCaseId,
            long partyId,
            String kycTypeCode,
            String dueDiligenceLevelCode,
            String statusCode,
            LocalDateTime openedAt,
            LocalDateTime completedAt,
            LocalDateTime reviewedAt,
            LocalDate nextReviewDate,
            String finalRiskLevelCode,
            String decisionCode,
            String decisionReason,
            String approvedBy,
            long recordVersion,
            String relationPurposeCode,
            String expectedActivityLevelCode,
            String geographicScopeCode,
            String activityCountriesText,
            String requestedProductsText,
            String preferredServiceChannelCode,
            String pepStatusCode,
            String highRiskCountryFlag,
            String eddRequiredFlag
    ) {
    }

    public record PartyDocumentRecord(
            long documentId,
            long partyId,
            Long kycCaseId,
            String documentTypeCode,
            String documentNumber,
            String issuerCode,
            LocalDate issueDate,
            LocalDate expiryDate,
            String verificationStatusCode,
            LocalDateTime verifiedAt,
            String contentHash,
            String storageRef,
            String mimeType,
            String issuingAuthorityText,
            String controlStatusCode,
            String descriptionText,
            long recordVersion
    ) {
    }

    public record RiskAssessmentRecord(
            long riskAssessmentId,
            long partyId,
            Long kycCaseId,
            String riskTypeCode,
            BigDecimal scoreValue,
            String ratingCode,
            String decisionCode,
            String modelCode,
            String modelVersion,
            LocalDateTime assessmentDate,
            LocalDateTime validTo,
            String explanation,
            long recordVersion
    ) {
    }

    public record ScreeningResultRecord(
            long screeningResultId,
            long partyId,
            Long kycCaseId,
            String screeningTypeCode,
            String sourceListCode,
            String providerCode,
            String providerReferenceNo,
            String matchedName,
            BigDecimal matchScore,
            String initialDecisionCode,
            String finalDecisionCode,
            String falsePositiveFlag,
            LocalDateTime screenedAt,
            LocalDateTime reviewedAt,
            String reviewedBy,
            String payloadRef,
            long recordVersion
    ) {
    }

    public record ExternalInquiryRecord(
            long inquiryResultId,
            long partyId,
            String inquiryTypeCode,
            String providerCode,
            String referenceNo,
            String inquiryResultCode,
            LocalDateTime requestedAt,
            LocalDateTime respondedAt,
            LocalDateTime expiryAt,
            String payloadRef,
            String payloadHash,
            long recordVersion
    ) {
    }

    public record PartyConsentRecord(
            long consentId,
            long partyId,
            String consentTypeCode,
            String purposeCode,
            String consentStatusCode,
            LocalDateTime grantedAt,
            LocalDateTime revokedAt,
            String evidenceRef,
            String sourceCode,
            String customerDecisionCode,
            String captureChannelCode,
            LocalDateTime declaredAt,
            LocalDate validTo,
            String consentTextVersionCode,
            String scopeText,
            String scopeLimitationText,
            long recordVersion
    ) {
    }

    public record CommunicationPreferenceRecord(
            long preferenceId,
            long partyId,
            String channelCode,
            String purposeCode,
            String allowedFlag,
            String preferredTimeFrom,
            String preferredTimeTo,
            String languageCode,
            String allowedDaysCode,
            String timeZoneCode,
            String marketingOptOutFlag,
            long recordVersion
    ) {
    }

    public record PartyGeneralPreferenceRecord(
            long preferenceId,
            long partyId,
            String preferenceTypeCode,
            String preferenceValue,
            LocalDateTime validFrom,
            LocalDateTime validTo,
            String sourceCode,
            long recordVersion
    ) {
    }

    public record PartyStatusHistoryRecord(
            long partyStatusHistoryId,
            long partyId,
            String lifecycleStatusCode,
            String statusReasonCode,
            LocalDate validFrom,
            LocalDate validTo,
            String descriptionText,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime updatedAt,
            String updatedBy,
            long recordVersion
    ) {
    }

    public record PartyMergeHistoryRecord(
            long partyMergeId,
            long sourcePartyId,
            long targetPartyId,
            String mergeReasonCode,
            String conflictResolutionCode,
            LocalDateTime mergedAt,
            String mergedBy,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime updatedAt,
            String updatedBy,
            long recordVersion,
            LocalDateTime createdDate
    ) {
    }

    public record Party360SummaryRecord(
            long partyId,
            String partyUid,
            String lifecycleStatusCode,
            long activeProductCount,
            long openComplaintCount,
            LocalDateTime lastInteractionAt,
            BigDecimal currentValueScore,
            String currentSegmentCode,
            long activeRecommendationCount,
            LocalDateTime asOfTimestamp
    ) {
    }

    public record OrganizationOfficer360Record(long organizationOfficerId, long organizationPartyId, long officerPartyId,
                                                String officerRoleCode, LocalDate validFrom, LocalDate validTo) {
    }

    public record PartyAlertCase360Record(long alertCaseId, long partyId, String alertTypeCode, String severityCode,
                                          String statusCode, String sourceSystemCode, String sourceReferenceNo,
                                          LocalDateTime openedAt, LocalDateTime closedAt, String assignedTo,
                                          String resolutionCode, String resolutionNote) {
    }

    public record PartyComplaint360Record(long complaintId, long partyId, String complaintTypeCode, String productTypeCode,
                                          String severityCode, String statusCode, LocalDateTime openedAt, LocalDateTime dueAt,
                                          LocalDateTime resolvedAt, String resolutionCode, String assignedUnitCode,
                                          String complaintDescription) {
    }

    public record PartyComplaintStatus360Record(long complaintStatusHistoryId, long complaintId, String fromStatusCode,
                                                String toStatusCode, LocalDateTime changedAt, String changedBy,
                                                String reasonCode, String commentText) {
    }

    public record PartyGroupMembership360Record(long groupMemberId, long groupId, String groupTypeCode, String groupName,
                                                String groupStatusCode, String memberRoleCode, BigDecimal ownershipPercent,
                                                LocalDate validFrom, LocalDate validTo) {
    }

    public record PartyInteraction360Record(long interactionId, long partyId, String channelCode, String interactionTypeCode,
                                            String subject, LocalDateTime occurredAt, String outcomeCode, String employeeId,
                                            String branchCode, String referenceTypeCode, String referenceId,
                                            String interactionDetails) {
    }

    public record PartyJourneyEvent360Record(long journeyEventId, long partyId, String journeyCode, String stageCode,
                                             String eventCode, LocalDateTime occurredAt, String channelCode,
                                             String referenceTypeCode, String referenceId) {
    }

    public record PartyMetricSnapshot360Record(long metricSnapshotId, long partyId, String metricCode, BigDecimal metricValue,
                                               String metricUnitCode, LocalDate asOfDate, LocalDate periodFrom,
                                               LocalDate periodTo, String sourceSnapshotId) {
    }

    public record PartyOperationLimit360Record(long limitId, long partyId, String limitTypeCode, String contextTypeCode,
                                               String contextId, String currencyCode, BigDecimal limitAmount, String periodCode,
                                               LocalDate validFrom, LocalDate validTo, String approvalRef) {
    }

    public record PartyProductHolding360Record(long partyProductHoldingId, long partyId, String productTypeCode,
                                               String productInstanceId, String relationshipRoleCode, String statusCode,
                                               LocalDate startDate, LocalDate endDate, String isPrimary) {
    }

    public record PartyProductRestriction360Record(long restrictionId, long partyId, String productTypeCode,
                                                   String restrictionTypeCode, String reasonCode, String severityCode,
                                                   LocalDateTime validFrom, LocalDateTime validTo, String statusCode,
                                                   String approvalRef) {
    }

    public record PartyRecommendation360Record(long recommendationId, long partyId, String recommendationTypeCode,
                                               String offerCode, Long priorityValue, BigDecimal scoreValue,
                                               LocalDateTime generatedAt, LocalDateTime expiryAt, String statusCode,
                                               String modelCode) {
    }

    public record PartyRegistrationRequest360Record(long registrationRequestId, String temporaryKey, String partyTypeCode,
                                                    String creationSourceCode, LocalDate validFrom, String requestStatusCode,
                                                    String identityKindCode, LocalDateTime requestedAt, LocalDateTime expiresAt,
                                                    Long completedPartyId) {
    }

    public record PartySegmentMembership360Record(long segmentMembershipId, long partyId, String segmentCode,
                                                  String modelCode, LocalDateTime assignedAt, LocalDateTime validTo,
                                                  BigDecimal confidenceLevel) {
    }

    public record PartyValueScore360Record(long valueScoreId, long partyId, String scoreTypeCode, BigDecimal scoreValue,
                                           String scoreBandCode, String modelCode, String modelVersion, LocalDate asOfDate,
                                           LocalDate dataPeriodFrom, LocalDate dataPeriodTo, BigDecimal confidenceLevel,
                                           String explanation) {
    }

    public record SignatureSpecimen360Record(long signatureId, long partyId, String signatoryId, String specimenTypeCode,
                                             boolean hasSignatureImage, LocalDate effectiveFrom, LocalDate effectiveTo,
                                             String statusCode, String signingRuleCode, String verificationStatusCode,
                                             String captureChannelCode, Long documentId, Long branchId, String capturedBy,
                                             LocalDate revokedAt, String revocationReason) {
    }

    public record AuditEvent360Record(long auditEventId, String entityTypeCode, String entityId, String actionCode,
                                      String actorId, String actorRoleCode, LocalDateTime occurredAt, String channelCode,
                                      String requestId, String reasonCode, String approvalRef, String clientIp,
                                      String hashAlgorithmCode) {
    }

    public record Party360SourceData(
            List<OrganizationOfficer360Record> organizationOfficers,
            List<PartyAlertCase360Record> alerts,
            List<PartyComplaint360Record> complaints,
            List<PartyComplaintStatus360Record> complaintStatusHistory,
            List<PartyGroupMembership360Record> groupMemberships,
            List<PartyInteraction360Record> interactions,
            List<PartyJourneyEvent360Record> journeyEvents,
            List<PartyMetricSnapshot360Record> metricSnapshots,
            List<PartyOperationLimit360Record> operationLimits,
            List<PartyProductHolding360Record> productHoldings,
            List<PartyProductRestriction360Record> productRestrictions,
            List<PartyRecommendation360Record> recommendations,
            List<PartyRegistrationRequest360Record> registrationRequests,
            List<PartySegmentMembership360Record> segmentMemberships,
            List<PartyValueScore360Record> valueScores,
            List<SignatureSpecimen360Record> signatureSpecimens,
            List<AuditEvent360Record> auditEvents
    ) {
    }

    public record PartyReadinessItem(String code, String label, boolean required, boolean complete, int recordCount,
                                     String actionPath, String detail) {
    }

    public record PartyReadinessSummary(boolean customerRole, String customerNo, boolean readyForFinalization,
                                        int requiredCompleted, int requiredTotal,
                                        List<PartyReadinessItem> items, List<String> blockers) {
    }

    public record Party360Response(
            PartyCore party,
            Party360SummaryRecord summary,
            Party360SourceData source360,
            PersonProfile person,
            OrganizationProfile organization,
            List<PartyNameRecord> names,
            List<PartyIdentifierRecord> identifiers,
            List<PartyAddressRecord> addresses,
            List<ContactPointRecord> contacts,
            List<ContactPointAddressRecord> contactAddressAssociations,
            List<FinancialProfileRecord> financialProfiles,
            List<PartyEmploymentRecord> employments,
            List<PartyIncomeSourceRecord> incomeSources,
            List<PartyAssetLiabilityRecord> assetLiabilities,
            List<PartyLicenseRecord> licenses,
            List<PartyClassificationRecord> classifications,
            List<PartyRelationshipRecord> relationships,
            List<BeneficialOwnershipRecord> beneficialOwnerships,
            List<PartyAuthorityRecord> authorities,
            List<PartyRoleRecord> roles,
            List<PartyCustomerRecord> customers,
            List<KycCaseRecord> kycCases,
            List<PartyDocumentRecord> documents,
            List<RiskAssessmentRecord> riskAssessments,
            List<ScreeningResultRecord> screenings,
            List<ExternalInquiryRecord> externalInquiries,
            List<PartyConsentRecord> consents,
            List<CommunicationPreferenceRecord> communicationPreferences,
            List<PartyGeneralPreferenceRecord> generalPreferences,
            List<PartyStatusHistoryRecord> statusHistory,
            List<PartyMergeHistoryRecord> mergeHistory
    ) {
    }

    public record CifDashboardSummary(
            long parties,
            long persons,
            long organizations,
            long openKycCases
    ) {
    }

    public record CreatePartyRequest(
            @NotBlank String partyTypeCode,
            @NotBlank @Size(max = 500) String primaryName,
            @Size(max = 30) String lifecycleStatusCode,
            @Size(max = 50) String statusReasonCode,
            @Size(max = 30) String verificationStatusCode,
            @Size(max = 30) String dataQualityStatusCode,
            @Size(max = 50) String creationSourceCode,
            LocalDate validFrom,
            LocalDate validTo,
            @Size(max = 30) String legalFormCode,
            @Size(max = 300) String registeredName
    ) {
    }

    public record PartyOnboardingRequest(
            @NotNull @Valid CreatePartyRequest party,
            @Valid PersonRequest person,
            @Valid OrganizationRequest organization,
            @Valid PartyNameRequest primaryNameDetails,
            @NotNull @Valid PartyIdentifierRequest primaryIdentifier
    ) {
    }

    public record UpdatePartyRequest(
            @NotBlank @Size(max = 30) String lifecycleStatusCode,
            @Size(max = 50) String statusReasonCode,
            @NotBlank @Size(max = 30) String verificationStatusCode,
            @NotBlank @Size(max = 30) String dataQualityStatusCode,
            @Size(max = 50) String creationSourceCode,
            Long mergedIntoPartyId,
            LocalDate validFrom,
            LocalDate validTo,
            @NotBlank @Size(max = 1) String isCurrent,
            @NotNull Long recordVersion
    ) {
    }

    public record PersonRequest(
            @NotNull LocalDate birthDate,
            @Size(max = 20) String genderCode,
            @Size(max = 3) String birthCountryCode,
            Long birthPlaceId,
            @Size(max = 200) String birthPlaceText,
            @Size(max = 100) String fatherGivenName,
            @Size(max = 100) String motherGivenName,
            @Size(max = 30) String maritalStatusCode,
            LocalDate deathDate,
            @NotBlank @Size(max = 20) String legalCapacityCode,
            @Size(max = 10) String primaryLanguageCode,
            @NotBlank @Size(max = 30) String dataQualityStatusCode,
            @NotBlank @Size(max = 50) String verificationStatusCode,
            @Size(max = 30) String residenceStatusCode,
            @Size(max = 15) String physicalAbility,
            @NotBlank @Size(max = 50) String lifeStatusCode,
            @Size(max = 3) String nationalityCountryCode,
            Long recordVersion
    ) {
    }

    public record OrganizationRequest(
            @NotBlank @Size(max = 300) String registeredName,
            @Size(max = 300) String tradeName,
            @NotBlank @Size(max = 30) String legalFormCode,
            @Size(max = 80) String registrationNo,
            @Size(max = 30) String registrationPlaceCode,
            LocalDate incorporationDate,
            LocalDate dissolutionDate,
            @Size(max = 30) String economicSectorCode,
            @Size(max = 20) String isicCode,
            @NotBlank @Size(max = 1) String listedCompanyFlag,
            @Size(max = 3) String registrationCountryCode,
            @Size(max = 30) String activityStatusCode,
            @Size(max = 1000) String mainActivityDescription,
            Long employeeCount,
            @Size(max = 30) String enterpriseSizeCode,
            @Size(max = 30) String ownershipTypeCode,
            Long recordVersion
    ) {
    }

    public record PartyNameRequest(
            @NotBlank @Size(max = 30) String nameTypeCode,
            @Size(max = 10) String languageCode,
            @Size(max = 10) String scriptCode,
            @Size(max = 50) String prefixText,
            @Size(max = 150) String givenName,
            @Size(max = 150) String middleName,
            @Size(max = 150) String familyName,
            @Size(max = 50) String suffixText,
            @NotBlank @Size(max = 500) String fullName,
            @Size(max = 500) String displayName,
            @Size(max = 500) String sortName,
            @Size(max = 500) String normalizedName,
            @Size(max = 200) String phoneticKey,
            @NotBlank @Size(max = 1) String isPrimary,
            LocalDate validFrom,
            LocalDate validTo,
            @NotBlank @Size(max = 30) String verificationStatusCode,
            @Size(max = 50) String sourceCode,
            @Size(max = 200) String sourceReference,
            Long recordVersion
    ) {
    }

    public record PartyIdentifierRequest(
            @NotBlank @Size(max = 40) String identifierTypeCode,
            @NotBlank @Size(max = 200) String identifierValue,
            @Size(max = 200) String normalizedIdentifierValue,
            @Size(max = 3) String issuingCountryCode,
            @Size(max = 30) String issuingAuthorityCode,
            @Size(max = 50) String issuerCode,
            LocalDate issueDate,
            LocalDate expiryDate,
            @NotBlank @Size(max = 1) String isPrimary,
            @NotBlank @Size(max = 1) String isActive,
            @NotBlank @Size(max = 30) String verificationStatusCode,
            @Size(max = 50) String verificationSourceCode,
            @Size(max = 30) String verificationMethodCode,
            LocalDateTime verifiedAt,
            LocalDate validFrom,
            LocalDate validTo,
            Long recordVersion
    ) {
    }

    public record PartyAddressRequest(
            @NotBlank @Size(max = 30) String addressTypeCode,
            @NotBlank @Size(max = 1) String isPrimary,
            LocalDate validFrom,
            LocalDate validTo,
            @NotBlank @Size(max = 3) String countryCode,
            @Size(max = 30) String provinceCode,
            @Size(max = 20) String countyCode,
            @Size(max = 30) String cityCode,
            @Size(max = 20) String districtCode,
            @Size(max = 20) String postalCode,
            @NotBlank @Size(max = 500) String addressLine1,
            @Size(max = 500) String addressLine2,
            @Size(max = 100) String neighborhoodText,
            @Size(max = 200) String mainStreetText,
            @Size(max = 200) String sideStreetText,
            @Size(max = 30) String plaqueNo,
            @Size(max = 20) String floorNo,
            @Size(max = 20) String unitNo,
            @Size(max = 500) String addressDetail,
            @Size(max = 30) String tenureTypeCode,
            @Size(max = 30) String verificationStatusCode,
            @Size(max = 30) String sourceCode,
            Long partyAddressRecordVersion,
            Long addressRecordVersion
    ) {
    }

    public record ContactPointRequest(
            @NotBlank @Size(max = 30) String contactTypeCode,
            @NotBlank @Size(max = 500) String contactValue,
            @Size(max = 500) String normalizedValue,
            @NotBlank @Size(max = 30) String purposeCode,
            @NotBlank @Size(max = 1) String isPrimary,
            @NotBlank @Size(max = 1) String isVerified,
            LocalDateTime verifiedAt,
            LocalDate validFrom,
            LocalDate validTo,
            @Size(max = 8) String countryDialCode,
            @Size(max = 10) String areaCode,
            @Size(max = 10) String extensionNo,
            @Size(max = 30) String ownerTypeCode,
            @Size(max = 30) String verificationStatusCode,
            @Size(max = 30) String verificationMethodCode,
            Long recordVersion
    ) {
    }

    public record ContactPointAddressRequest(
            @NotNull Long contactPointId,
            @NotNull Long partyAddressId,
            @NotBlank @Size(max = 30) String associationTypeCode,
            @NotBlank @Size(max = 1) String isPrimaryForAddress,
            LocalDate validFrom,
            LocalDate validTo,
            Long recordVersion
    ) {
    }

    public record FinancialProfileRequest(
            @NotNull LocalDate asOfDate,
            @DecimalMin("0") BigDecimal annualIncome,
            @DecimalMin("0") BigDecimal totalAssets,
            @DecimalMin("0") BigDecimal totalLiabilities,
            @NotBlank @Size(max = 3) String currencyCode,
            @Size(max = 50) String sourceOfFundsCode,
            @Size(max = 50) String sourceOfWealthCode,
            @DecimalMin("0") BigDecimal expectedMonthlyTurnover,
            @Size(max = 30) String taxStatusCode,
            @NotBlank @Size(max = 30) String verificationStatusCode,
            @DecimalMin("0") BigDecimal netMonthlyIncome,
            @DecimalMin("0") BigDecimal otherMonthlyIncome,
            @DecimalMin("0") Long expectedMonthlyTxnCount,
            @Size(max = 500) String fundsCountriesText,
            @Size(max = 30) String financialRelationPurposeCode,
            @DecimalMin("0") BigDecimal realEstateValue,
            @DecimalMin("0") BigDecimal investmentValue,
            @DecimalMin("0") BigDecimal totalMonthlyInstallment,
            BigDecimal estimatedNetWorth,
            @Size(max = 30) String financialCapacityCode,
            Long recordVersion
    ) {
    }

    public record PartyEmploymentRequest(
            Long employerPartyId,
            @Size(max = 300) String employerName,
            @NotBlank @Size(max = 40) String occupationCode,
            @Size(max = 200) String jobTitle,
            @Size(max = 30) String economicSectorCode,
            @Size(max = 20) String isicCode,
            @DecimalMin("0") BigDecimal monthlyIncome,
            @Size(max = 3) String incomeCurrencyCode,
            @Size(max = 20) String familyRange,
            @NotBlank @Size(max = 20) String jobStatus,
            @Size(max = 20) String employeeRange,
            @NotNull LocalDate validFrom,
            LocalDate validTo,
            @Size(max = 30) String employmentStatusCode,
            @Size(max = 30) String occupationGroupCode,
            @Size(max = 50) String employerIdentifier,
            @Size(max = 30) String contractTypeCode,
            @Size(max = 50) String insuranceNo,
            @Size(max = 50) String taxCode,
            Long recordVersion
    ) {
    }

    public record PartyIncomeSourceRequest(
            @NotBlank @Size(max = 30) String sourceTypeCode,
            @NotNull @DecimalMin("0") BigDecimal monthlyAmount,
            @NotBlank @Size(max = 3) String currencyCode,
            @NotBlank @Size(max = 1) String documentedFlag,
            @NotBlank @Size(max = 30) String statusCode,
            Long recordVersion
    ) {
    }

    public record PartyAssetLiabilityRequest(
            @NotBlank @Size(max = 30) String itemTypeCode,
            @Size(max = 500) String descriptionText,
            @NotNull @DecimalMin("0") BigDecimal amount,
            @NotBlank @Size(max = 3) String currencyCode,
            @NotNull LocalDate assessmentDate,
            @NotBlank @Size(max = 30) String statusCode,
            Long recordVersion
    ) {
    }

    public record PartyLicenseRequest(
            @NotBlank @Size(max = 50) String licenseTypeCode,
            @NotBlank @Size(max = 100) String licenseNumber,
            Long issuerPartyId,
            @Size(max = 300) String issuerName,
            LocalDate issueDate,
            LocalDate expiryDate,
            @NotBlank @Size(max = 30) String statusCode,
            @Size(max = 500) String documentRef,
            Long recordVersion
    ) {
    }

    public record PartyClassificationRequest(
            @NotBlank @Size(max = 40) String classificationTypeCode,
            @NotBlank @Size(max = 60) String classificationValueCode,
            @NotBlank @Size(max = 40) String assignmentReasonCode,
            @NotNull LocalDate validFrom,
            LocalDate validTo,
            @Size(max = 1000) String descriptionText,
            Long recordVersion
    ) {
    }


    public record PartyRelationshipRequest(
            @NotNull Long relatedPartyId,
            @NotBlank @Size(max = 30) String relationshipTypeCode,
            @DecimalMin("0") @DecimalMax("100") BigDecimal ownershipPercent,
            @Size(max = 100) String positionTitle,
            @Size(max = 30) String signingRightCode,
            @DecimalMin("0") BigDecimal authorityLimitAmount,
            @NotNull LocalDate startDate,
            LocalDate endDate,
            Long evidenceDocumentId,
            @Size(max = 30) String verificationStatusCode,
            Long recordVersion
    ) {
    }

    public record BeneficialOwnershipRequest(
            @NotNull Long beneficialOwnerPartyId,
            @DecimalMin("0") @DecimalMax("100") BigDecimal directOwnershipPercent,
            @DecimalMin("0") @DecimalMax("100") BigDecimal indirectOwnershipPercent,
            @DecimalMin("0") @DecimalMax("100") BigDecimal controlPercent,
            @Size(max = 50) String controlBasisCode,
            @NotBlank @Size(max = 1) String isUltimateOwner,
            @Size(max = 2000) String ownershipPath,
            @NotNull LocalDate validFrom,
            LocalDate validTo,
            @Size(max = 200) String evidenceRef,
            Long recordVersion
    ) {
    }

    public record PartyAuthorityRequest(
            @NotNull Long authorizedPartyId,
            @NotBlank @Size(max = 40) String authorityTypeCode,
            @NotBlank @Size(max = 60) String scopeCode,
            @DecimalMin("0") BigDecimal maxAmount,
            @Size(max = 3) String currencyCode,
            @NotNull LocalDate validFrom,
            LocalDate validTo,
            @NotBlank @Size(max = 500) String documentRef,
            Long recordVersion
    ) {
    }

    public record PartyRoleRequest(
            @NotBlank @Size(max = 40) String roleTypeCode,
            @Size(max = 40) String contextTypeCode,
            @Size(max = 100) String contextId,
            @NotNull LocalDate validFrom,
            LocalDate validTo,
            @NotBlank @Size(max = 30) String statusCode,
            Long principalPartyId,
            @Size(max = 30) String relationshipTypeCode,
            @Size(max = 30) String authorityBasisCode,
            @Size(max = 100) String authorityDocumentNo,
            @Size(max = 200) String authorityIssuer,
            @Size(max = 500) String authorityScopeText,
            @Size(max = 500) String assignmentReasonText,
            @Size(max = 500) String descriptionText,
            Long recordVersion
    ) {
    }

    public record KycCaseRequest(
            @NotBlank @Size(max = 30) String kycTypeCode,
            @NotBlank @Size(max = 30) String dueDiligenceLevelCode,
            @NotBlank @Size(max = 30) String statusCode,
            LocalDateTime openedAt,
            LocalDateTime completedAt,
            LocalDateTime reviewedAt,
            LocalDate nextReviewDate,
            @Size(max = 30) String finalRiskLevelCode,
            @Size(max = 30) String decisionCode,
            @Size(max = 1000) String decisionReason,
            @Size(max = 100) String approvedBy,
            @Size(max = 30) String relationPurposeCode,
            @Size(max = 30) String expectedActivityLevelCode,
            @Size(max = 30) String geographicScopeCode,
            @Size(max = 500) String activityCountriesText,
            @Size(max = 500) String requestedProductsText,
            @Size(max = 30) String preferredServiceChannelCode,
            @Size(max = 30) String pepStatusCode,
            @Size(max = 1) String highRiskCountryFlag,
            @Size(max = 1) String eddRequiredFlag,
            Long recordVersion
    ) {
    }

    public record PartyDocumentRequest(
            Long kycCaseId,
            @NotBlank @Size(max = 50) String documentTypeCode,
            @NotBlank @Size(max = 150) String documentNumber,
            @Size(max = 50) String issuerCode,
            LocalDate issueDate,
            LocalDate expiryDate,
            @NotBlank @Size(max = 30) String verificationStatusCode,
            LocalDateTime verifiedAt,
            @NotBlank @Size(max = 128) String contentHash,
            @NotBlank @Size(max = 1000) String storageRef,
            @NotBlank @Size(max = 100) String mimeType,
            @Size(max = 200) String issuingAuthorityText,
            @Size(max = 30) String controlStatusCode,
            @Size(max = 1000) String descriptionText,
            Long recordVersion
    ) {
    }

    public record RiskAssessmentRequest(
            Long kycCaseId,
            @NotBlank @Size(max = 30) String riskTypeCode,
            @NotNull BigDecimal scoreValue,
            @NotBlank @Size(max = 30) String ratingCode,
            @NotBlank @Size(max = 30) String decisionCode,
            @NotBlank @Size(max = 60) String modelCode,
            @NotBlank @Size(max = 30) String modelVersion,
            LocalDateTime assessmentDate,
            LocalDateTime validTo,
            String explanation,
            Long recordVersion
    ) {
    }

    public record ScreeningResultRequest(
            Long kycCaseId,
            @NotBlank @Size(max = 40) String screeningTypeCode,
            @NotBlank @Size(max = 60) String sourceListCode,
            @NotBlank @Size(max = 50) String providerCode,
            @Size(max = 100) String providerReferenceNo,
            @Size(max = 500) String matchedName,
            @DecimalMin("0") @DecimalMax("100") BigDecimal matchScore,
            @NotBlank @Size(max = 30) String initialDecisionCode,
            @Size(max = 30) String finalDecisionCode,
            @NotBlank @Size(max = 1) String falsePositiveFlag,
            LocalDateTime screenedAt,
            LocalDateTime reviewedAt,
            @Size(max = 100) String reviewedBy,
            @Size(max = 1000) String payloadRef,
            Long recordVersion
    ) {
    }

    public record ExternalInquiryRequest(
            @NotBlank @Size(max = 50) String inquiryTypeCode,
            @NotBlank @Size(max = 50) String providerCode,
            @NotBlank @Size(max = 100) String referenceNo,
            @Size(max = 50) String inquiryResultCode,
            LocalDateTime requestedAt,
            LocalDateTime respondedAt,
            LocalDateTime expiryAt,
            @Size(max = 1000) String payloadRef,
            @Size(max = 100) String payloadHash,
            Long recordVersion
    ) {
    }

    public record PartyConsentRequest(
            @NotBlank @Size(max = 50) String consentTypeCode,
            @NotBlank @Size(max = 40) String purposeCode,
            @NotBlank @Size(max = 30) String customerDecisionCode,
            @NotBlank @Size(max = 30) String captureChannelCode,
            @NotNull LocalDateTime declaredAt,
            LocalDate validTo,
            @NotBlank @Size(max = 50) String consentTextVersionCode,
            @Size(max = 1000) String scopeText,
            @Size(max = 1000) String scopeLimitationText,
            @Size(max = 200) String evidenceRef,
            @NotBlank @Size(max = 30) String sourceCode,
            Long recordVersion
    ) {
    }

    public record CommunicationPreferenceRequest(
            @NotBlank @Size(max = 30) String channelCode,
            @NotBlank @Size(max = 40) String purposeCode,
            @NotBlank @Size(max = 1) String allowedFlag,
            @Size(max = 5) String preferredTimeFrom,
            @Size(max = 5) String preferredTimeTo,
            @Size(max = 10) String languageCode,
            @Size(max = 30) String allowedDaysCode,
            @Size(max = 50) String timeZoneCode,
            @Size(max = 1) String marketingOptOutFlag,
            Long recordVersion
    ) {
    }

    public record PartyGeneralPreferenceRequest(
            @NotBlank @Size(max = 50) String preferenceTypeCode,
            @NotBlank @Size(max = 500) String preferenceValue,
            @NotNull LocalDateTime validFrom,
            LocalDateTime validTo,
            @NotBlank @Size(max = 30) String sourceCode,
            Long recordVersion
    ) {
    }

    public record PartyStatusChangeRequest(
            @NotBlank @Size(max = 30) String lifecycleStatusCode,
            @NotBlank @Size(max = 30) String statusReasonCode,
            @NotNull LocalDate effectiveDate,
            @Size(max = 1000) String descriptionText,
            @NotNull Long partyRecordVersion
    ) {
    }

    public record PartyMergeRequest(
            @NotNull Long targetPartyId,
            @NotBlank @Size(max = 30) String mergeReasonCode,
            @Size(max = 30) String conflictResolutionCode,
            @NotNull Long partyRecordVersion
    ) {
    }
}
