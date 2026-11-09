package com.behsazan.corebanking.collateral.lifecycle;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * R3 BUC-06/07/08 evidence projection. No inferred approval, debtor eligibility,
 * required insurer/guarantor policy or revaluation unit is introduced.
 */
public final class CollateralR3LifecycleEvidencePolicy {
    private CollateralR3LifecycleEvidencePolicy() {}

    public record Facts(String collateralId, String collateralNumber,
            String collateralStatus, String typeStatus, String currencyCode,
            boolean sponsorRequired, boolean insurable,
            long sponsorRows, long insuranceRows, long valuationRows,
            String currentValuationPointer, String currentValuationRowId,
            String currentValuationStatus, String currentValuationCurrency,
            LocalDate effectiveFrom, LocalDate effectiveTo, LocalDate asOfDate) {}

    public record Section(String applicability, long recordedRows, String validity,
            String reason, List<String> policyGaps) {}

    public record ValuationSection(long recordedRows, String pointer,
            String physicalStatusCode, String recordedPeriodState,
            String recordedEffectiveFrom, String recordedEffectiveTo,
            boolean withinRecordedEffectivePeriod, boolean eligibleForNewAllocation,
            List<String> evidenceBlockers, List<String> policyGaps) {}

    public record Evidence(String collateralId, String collateralNumber,
            String collateralStatus, String collateralTypeStatus, String currencyCode,
            String asOfDate, Section sponsor, Section insurance,
            ValuationSection valuation, boolean finalRegistrationEnabled,
            boolean eligibleForNewAllocation, List<String> unresolvedApprovalGates,
            String evidenceKind, String timeSource) {}

    public static Evidence evaluate(Facts facts) {
        if (facts == null || facts.collateralId() == null || facts.collateralNumber() == null
                || facts.asOfDate() == null || facts.sponsorRows() < 0
                || facts.insuranceRows() < 0 || facts.valuationRows() < 0) {
            throw new IllegalArgumentException("CLT_R3_EVIDENCE_FACTS_INVALID");
        }
        Section sponsors = new Section(facts.sponsorRequired() ? "REQUIRED_BY_TYPE" : "NOT_REQUIRED_BY_TYPE",
                facts.sponsorRows(), "NOT_VERIFIED", "RECORDED_SPONSORS_ARE_NOT_CONFIRMED_VALID_SPONSORS",
                List.of("G-05_SPONSOR_LIFECYCLE_NOT_APPROVED", "G-11_MIN_VALID_COUNT_NOT_DEFINED"));
        Section insurance = new Section(facts.insurable() ? "INSURABLE_TYPE" : "NOT_INSURABLE_TYPE",
                facts.insuranceRows(), "NOT_VERIFIED", "RECORDED_POLICIES_ARE_NOT_APPROVED_COVERAGE",
                List.of("G-04_REQUIRED_COVERAGE_NOT_DEFINED", "G-06_POLICY_LIFECYCLE_NOT_APPROVED"));
        List<String> evidenceBlockers = new ArrayList<>();
        String recordedPeriod = "NOT_PROVEN";
        boolean withinRecorded = false;
        String pointer = facts.currentValuationPointer();
        if (pointer == null || pointer.isBlank()) {
            evidenceBlockers.add("CLT_R3_CURRENT_VALUATION_POINTER_MISSING");
        } else if (!pointer.equals(facts.currentValuationRowId())) {
            evidenceBlockers.add("CLT_R3_CURRENT_VALUATION_POINTER_UNRESOLVED");
        } else {
            if (!"FINAL".equals(facts.currentValuationStatus())) {
                evidenceBlockers.add("CLT_R3_CURRENT_VALUATION_NOT_FINAL");
            }
            if (facts.currencyCode() == null || !facts.currencyCode().equals(facts.currentValuationCurrency())) {
                evidenceBlockers.add("CLT_R3_CURRENT_VALUATION_CURRENCY_MISMATCH");
            }
            if (facts.effectiveFrom() == null || facts.asOfDate().isBefore(facts.effectiveFrom())) {
                evidenceBlockers.add("CLT_R3_VALUATION_NOT_YET_EFFECTIVE");
            }
            if (facts.effectiveTo() == null) {
                evidenceBlockers.add("CLT_R3_VALUATION_EFFECTIVE_TO_UNKNOWN");
            } else if (facts.asOfDate().isAfter(facts.effectiveTo())) {
                evidenceBlockers.add("CLT_R3_VALUATION_RECORDED_PERIOD_EXPIRED");
                recordedPeriod = "EXPIRED_RECORDED_PERIOD";
            }
            if (evidenceBlockers.isEmpty()) {
                recordedPeriod = "WITHIN_RECORDED_EFFECTIVE_PERIOD";
                withinRecorded = true;
            }
        }
        ValuationSection valuation = new ValuationSection(facts.valuationRows(), pointer,
                facts.currentValuationStatus(), recordedPeriod,
                facts.effectiveFrom() == null ? null : facts.effectiveFrom().toString(),
                facts.effectiveTo() == null ? null : facts.effectiveTo().toString(),
                withinRecorded, false, List.copyOf(evidenceBlockers),
                List.of("G-07_FINALIZATION_POLICY_NOT_APPROVED",
                        "G-18_REVALUATION_INTERVAL_UNIT_NOT_MODELED"));
        return new Evidence(facts.collateralId(), facts.collateralNumber(),
                facts.collateralStatus(), facts.typeStatus(), facts.currencyCode(),
                facts.asOfDate().toString(), sponsors, insurance, valuation,
                false, false,
                List.of("G-04", "G-05", "G-06", "G-07", "G-09", "G-11", "G-18"),
                "ORACLE_CLT_SINGLE_STATEMENT_BRANCH_SCOPED_READ_ONLY_NO_FINAL_DECISION",
                "ORACLE_SYSTIMESTAMP_ASIA_TEHRAN_NOT_OFFICIAL_BUSINESS_DATE");
    }
}
