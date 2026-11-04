package com.behsazan.corebanking.collateral.allocation;

import java.util.List;

/** Diagnostic response deliberately excludes debtor identities, tokens, and any purported approval. */
public final class CollateralAllocationCapacityLockModels {
    private CollateralAllocationCapacityLockModels() {}
    public record Readiness(boolean probeEnabled, boolean intakeWritesEnabled,
            boolean financialAllocationWritesEnabled, boolean globalWriterCooperationProven,
            String state) {}
    public record Probe(String requestId, String collateralId, String consumerDomain,
            String collateralVersion, String oracleCalendarDate, String requestCurrency,
            String requestedAmount, String allocatedSameCurrency,
            String arithmeticResidual, String hypotheticalShortfall,
            boolean parentRowLockedWithinTransaction, boolean debtorRosterWellFormed,
            boolean valuationPeriodProven, boolean capacityReservationPersisted,
            boolean allocationAuthorized, boolean oracleWritePerformed,
            String evidenceLevel, List<String> blockers) {}
}
