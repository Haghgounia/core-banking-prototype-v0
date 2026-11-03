package com.behsazan.corebanking.collateral.insurance;

import java.util.List;

public final class CollateralInsuranceModels {
    private CollateralInsuranceModels() {}
    public record PolicyRow(String insuranceId, String collateralId, String ownerPartyId,
            String insurerCode, String companyId, String companyName, String insuranceTypeId,
            String insuranceTypeName, String depositNumber, String insuranceNumber,
            String insuranceAmount, String receivedAmount, String taxAmount,
            String validFrom, String validTo, String previousInsuranceId,
            String branchCode, String currencyCode, String recordVersion,
            String lifecycleEvidence) {}
    public record PolicyPage(String collateralId, String collateralNumber, boolean insurable,
            String requiredCoverageStatus, String validCoverageStatus, long totalElements,
            int page, int size, List<PolicyRow> items, String source) {}
    public record Option(String id, String code, String name) {}
    public record Options(List<Option> insuranceTypes, List<Option> insuranceCompanies,
            List<Option> currencies, String coverageScopeStatus, String bankInsurerStatus) {}
    public record RenewalOption(String insuranceId, String insuranceNumber, String insuranceTypeId, String validTo) {}
    public record CandidateRequest(String insurerCode, String companyId, String insuranceTypeId,
            String depositNumber, String insuranceNumber, String insuranceAmount,
            String receivedAmount, String taxAmount, String validFrom,
            String validTo, String previousInsuranceId, String currencyCode, String justification) {}
    public record Validation(String collateralId, boolean eligibleForRequest,
            boolean eligibleForFinalRegistration, boolean eligibleForAllocation,
            List<String> blockers, List<String> unresolvedPolicyGaps, String stage) {}
    public record RequestRecord(String requestId, String collateralId, String ownerPartyId,
            String insuranceNumber, String insuranceTypeId, String companyId, String validFrom,
            String validTo, String status, String branchCode, String makerSubject,
            String createdAt, String recordVersion, String source) {}
    public record RequestPage(long totalElements, int page, int size, List<RequestRecord> items) {}
    public record Readiness(boolean readsEnabled, boolean requestWritesEnabled,
            boolean stagingSchemaVerified, boolean finalInsuranceRegistrationEnabled,
            boolean insuranceLifecycleApproved, String stage) {}
}
