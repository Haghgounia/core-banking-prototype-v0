package com.behsazan.corebanking.collateral.valuation;

import java.util.List;

/** API identifiers and money are decimal strings: Oracle NUMBER(19)/(20,4), never JS Number. */
public final class CollateralValuationModels {
    private CollateralValuationModels() {}

    public record ValuationRow(String valuationId, String collateralId, String sequenceNo,
            String typeCode, String typeName, String methodCode, String methodName,
            String statusCode, String statusName, String valuationDate, String valuationAmount,
            String currencyCode, String forcedSaleValue, String eligibleValue, String haircutPercent,
            String valuerPartyId, String valuerName, String valuationReference,
            String effectiveFrom, String effectiveTo, String supersedesValuationId,
            String finalizedAt, String finalizedBy, String recordVersion) {}

    public record ValuationPage(String collateralId, String collateralNumber,
            String currentValuationId, long totalElements, int page, int size,
            List<ValuationRow> items, String source) {}

    /** Recorded validity is evidence; this is NOT an allocation authorization. */
    public record CurrentEvidence(String collateralId, String collateralNumber, String asOfDate,
            String currentValuationId, ValuationRow currentValuation,
            String recordedPeriodState, boolean withinRecordedPeriod,
            boolean eligibleForNewAllocation, List<String> blockers,
            List<String> unresolvedPolicyGaps, String timeSource) {}

    public record Option(String code, String name) {}
    public record Options(List<Option> types, List<Option> methods, List<Option> currencies,
            String finalStatusCode, String policyStatus) {}

    public record CandidateRequest(String typeCode, String methodCode, String valuationDate,
            String valuationAmount, String currencyCode, String forcedSaleValue,
            String eligibleValue, String haircutPercent, String valuerPartyId,
            String valuerName, String valuationReference, String effectiveFrom,
            String effectiveTo, String supersedesValuationId, String justification) {}

    public record Validation(String collateralId, boolean eligibleForStagingRequest,
            boolean eligibleForFinalValuation, boolean eligibleForNewAllocation,
            List<String> blockers, List<String> unresolvedPolicyGaps, String stage) {}

    public record Readiness(boolean readsEnabled, boolean requestWritesEnabled,
            boolean stagingSchemaVerified, boolean finalValuationEnabled,
            boolean allocationPolicyApproved, String stage) {}

    public record RequestRecord(String requestId, String collateralId, String typeCode,
            String methodCode, String valuationDate, String valuationAmount, String currencyCode,
            String supersedesValuationId, String status, String branchCode,
            String makerSubject, String createdAt, String recordVersion, String source) {}
    public record RequestPage(long totalElements, int page, int size, List<RequestRecord> items) {}
}
