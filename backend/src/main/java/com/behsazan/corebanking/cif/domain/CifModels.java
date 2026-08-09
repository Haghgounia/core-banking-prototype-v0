package com.behsazan.corebanking.cif.domain;

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
            String cityCode,
            String districtCode,
            String postalCode,
            String addressLine1,
            String addressLine2,
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
            long recordVersion
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

    public record Party360Response(
            PartyCore party,
            PersonProfile person,
            OrganizationProfile organization,
            List<PartyNameRecord> names,
            List<PartyIdentifierRecord> identifiers,
            List<PartyAddressRecord> addresses,
            List<ContactPointRecord> contacts,
            List<KycCaseRecord> kycCases,
            List<PartyDocumentRecord> documents,
            List<RiskAssessmentRecord> riskAssessments,
            List<ScreeningResultRecord> screenings
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
            @Size(max = 30) String verificationStatusCode,
            @Size(max = 30) String dataQualityStatusCode,
            @Size(max = 50) String creationSourceCode,
            @Size(max = 30) String legalFormCode,
            @Size(max = 300) String registeredName
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
            LocalDate birthDate,
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
            @Size(max = 30) String cityCode,
            @Size(max = 20) String districtCode,
            @Size(max = 20) String postalCode,
            @NotBlank @Size(max = 500) String addressLine1,
            @Size(max = 500) String addressLine2,
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
}
