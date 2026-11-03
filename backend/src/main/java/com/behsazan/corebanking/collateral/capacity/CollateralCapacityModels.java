package com.behsazan.corebanking.collateral.capacity;

import java.util.List;

/** Currency and monetary values are decimal strings in the API, never JS floating numbers. */
public final class CollateralCapacityModels {
    private CollateralCapacityModels() {}

    public record AllocationBucket(String moduleId, String currencyCode,
            long activeAllocationCount, String allocatedAmount) {}

    /** Reference parameters are source values, not a certified acceptance formula. */
    public record TypeParameters(String typeCode, String status,
            String mortgagePercentLoan, String usagePercentLoan,
            String mortgagePercentGuarantee, String usagePercentGuarantee,
            String mortgagePercentLetterCredit, String usagePercentLetterCredit,
            String usagePercentGreenLicense, String reserveRatio,
            boolean sponsorRequired, boolean insurable, boolean exclusiveUsage,
            boolean typeCrossBranchAllowed) {}

    /** A read-only evidence snapshot. All computed arithmetic is expressly NON-AUTHORIZING. */
    public record CapacityEvidence(String collateralId, String collateralNumber,
            String collateralStatus, String currencyCode, String asOfDate,
            String asOfClock, String currentValuationId, String valuationStatus,
            String valuationCurrency, String valuationEffectiveFrom, String valuationEffectiveTo,
            String recordedEligibleValue, String allocatedActiveSameCurrency,
            String arithmeticResidual, String arithmeticShortfall,
            long activeAllocationCount, List<AllocationBucket> allocationBreakdown,
            TypeParameters typeParameters, boolean valuationPeriodProven,
            boolean eligibleForNewAllocation, String verifiedFreeCapacity,
            List<String> blockers, List<String> unresolvedPolicyGaps, String evidenceLevel) {}

    public record PreviewRequest(String consumerDomain, String requestedAmount,
            String currencyCode) {}

    /** This is a what-if comparison, never approval of the consuming-domain obligation. */
    public record PreviewResult(String collateralId, String consumerDomain,
            String requestedAmount, String currencyCode, String hypotheticalShortfall,
            boolean approvedForNewAllocation, String verifiedAvailableAmount,
            List<String> blockers, String resultStatus) {}
}
