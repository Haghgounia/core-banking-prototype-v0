package com.behsazan.corebanking.collateral.allocation;

import com.behsazan.corebanking.collateral.capacity.CollateralCapacityModels.AllocationBucket;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityPolicy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure policy for a single-connection, parent-row-locked evidence sample.
 * Arithmetic NEVER asserts domain authorization, compliance, or a committed reservation.
 */
public final class CollateralAllocationCapacityLockPolicy {
    private CollateralAllocationCapacityLockPolicy() {}
    public static final String REQUEST_STATUS = "RECORDED_NOT_ALLOCATED";
    public static final String EVIDENCE_LEVEL = "SINGLE_ORACLE_TRANSACTION_PARENT_ROW_LOCK_NO_FINANCIAL_WRITE";
    public static final List<String> NON_NEGOTIABLE_BLOCKERS = List.of(
            "CLT_R4D_PROBE_NOT_AN_ALLOCATION_OR_RESERVATION",
            "CLT_R4D_EXTERNAL_ALLOCATION_WRITERS_NOT_CERTIFIED_TO_LOCK_PARENT",
            "G-09_CONSUMER_OBLIGATION_AND_DEBTOR_ROSTER_NOT_ATTESTED",
            "G-10_OFFICIAL_ALLOCATION_FACTORS_AND_FX_NOT_APPROVED",
            "G-13_PER_INTRODUCTION_CROSS_BRANCH_PERMISSION_UNPROVEN",
            "CLT_R4D_MODULE_ID_AND_FINAL_ATOMIC_WRITE_NOT_APPROVED");

    public record Snapshot(CollateralCapacityPolicy.Input capacity, String collateralVersion,
            int actualDebtorCount, int uniqueDebtorCount, int primaryCount,
            int sponsorMatches) {}
    public record Decision(String allocatedSameCurrency, String arithmeticResidual,
            String hypotheticalShortfall, boolean valuationPeriodProven,
            boolean debtorRosterWellFormed, boolean capacityReservationPersisted,
            boolean allocationAuthorized, List<String> blockers, String evidenceLevel) {}

    public static Decision inspect(Snapshot snapshot, String requestStatus,
            String requestAmount, String requestCurrency, int expectedDebtorCount) {
        if (snapshot == null || snapshot.capacity() == null)
            throw new IllegalArgumentException("CLT_R4D_SNAPSHOT_REQUIRED");
        BigDecimal amount = CollateralCapacityPolicy.positiveMoney(requestAmount);
        String currency = CollateralCapacityPolicy.currency(requestCurrency);
        CollateralCapacityPolicy.Input input = snapshot.capacity();
        var evidence = CollateralCapacityPolicy.calculate(input);
        List<String> blockers = new ArrayList<>(evidence.blockers());
        if (!REQUEST_STATUS.equals(requestStatus))
            blockers.add("CLT_R4D_REQUEST_NOT_IMMUTABLE_INTAKE");
        if (snapshot.collateralVersion() == null || !snapshot.collateralVersion().matches("[1-9][0-9]{0,9}"))
            blockers.add("CLT_R4D_COLLATERAL_VERSION_INVALID");
        boolean rosterValid = expectedDebtorCount >= 1 && expectedDebtorCount <= 20
                && snapshot.actualDebtorCount() == expectedDebtorCount
                && snapshot.uniqueDebtorCount() == expectedDebtorCount
                && snapshot.primaryCount() == 1;
        if (!rosterValid) blockers.add("CLT_R4D_STAGED_DEBTOR_ROSTER_CORRUPT");
        if (snapshot.sponsorMatches() > 0)
            blockers.add("CLT_R4D_CURRENT_SPONSOR_DEBTOR_CONFLICT");
        if (!currency.equals(input.collateralCurrency()))
            blockers.add("CLT_R4D_REQUEST_CURRENCY_MISMATCH_NO_APPROVED_FX");
        String hypotheticalShortfall = currency.equals(input.collateralCurrency())
                && evidence.residual() != null
                ? CollateralCapacityPolicy.hypotheticalShortfall(evidence.residual(), amount.toPlainString())
                : null;
        if (hypotheticalShortfall != null && new BigDecimal(hypotheticalShortfall).signum() > 0)
            blockers.add("CLT_R4D_HYPOTHETICAL_ARITHMETIC_SHORTFALL");
        blockers.addAll(NON_NEGOTIABLE_BLOCKERS);
        return new Decision(evidence.sameCurrencyAllocated(), evidence.residual(),
                hypotheticalShortfall, evidence.periodProven(), rosterValid, false,
                false, List.copyOf(blockers), EVIDENCE_LEVEL);
    }

    public static boolean everAllocates() {return false;}
}
