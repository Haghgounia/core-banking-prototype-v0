package com.behsazan.corebanking.collateral.reporting;

import java.util.List;

/** Counts are branch-scoped; currency amounts are NEVER combined. */
public final class CollateralReportingModels {
    private CollateralReportingModels() {}
    public record StatusCount(String statusCode, long count) {}
    public record ActiveExposure(String currencyCode, long activeAllocationCount, String activeAllocatedAmount) {}
    public record Overview(String source, String scope, String branchCode, long totalCollaterals,
                           List<StatusCount> byStatus, List<ActiveExposure> activeExposures,
                           boolean allocationAuthorization, String evidenceNote) {}
    public record StatusEvent(String eventId, String collateralNumber, String fromStatus,
                              String toStatus, String changedAt) {}
    public record ReferenceSummary(String source, String scope, long activeTypes,
                                   long activeGroups, boolean hqAttestation) {}
}
