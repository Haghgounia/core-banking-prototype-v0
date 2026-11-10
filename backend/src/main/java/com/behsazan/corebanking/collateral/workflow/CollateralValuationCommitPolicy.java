package com.behsazan.corebanking.collateral.workflow;

import com.behsazan.corebanking.collateral.valuation.CollateralValuationPolicy;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * BUC-08 / SUC-10 / SVC-08 guarded finalization of an initial or superseding valuation.
 * The old valuation is kept as a historical row; its status becomes SUPERSEDED.
 * An operational FINAL receipt is NOT an allocation/capacity decision (G-07/G-18).
 * No bank choices, exchange rates, or revaluation interval defaults are inferred.
 */
public final class CollateralValuationCommitPolicy {
    private CollateralValuationCommitPolicy() {}

    public record Command(String expectedVersion, String idempotencyKey, String reason) {}
    public record Release(String revision, String finalStatusDecision,
            String appraisabilityDecision, String intervalDecision, String businessDateDecision,
            String valuerAuthorityDecision, String valueDecision, String supersessionDecision,
            String approvedBy, boolean approved) {}
    public record StageEvidence(String status, String version, boolean integrityVerified,
            boolean conflictFree, boolean unknownChecks, String makerSubject, String postingSubject) {}
    /** A bank-approved type-specific rule, not an inferred value from missing physical columns. */
    public record TypeRule(String collateralTypeId, boolean appraisable,
            int intervalValue, String intervalUnit, String boundaryMode, String anchor,
            String policyRevision, String approvedBy, boolean approved) {}
    public record ValuerRule(String partyId, String collateralTypeId, String methodCode,
            String policyRevision, String approvedBy, boolean approved) {}
    public record Receipt(String requestId, String collateralId, String valuationId,
            String status, String policyRevision, String idempotencyKey,
            String previousValuationId, String recordedEffectiveTo, boolean replayed) {}

    private static boolean reference(String s) {
        return s != null && !s.isBlank() && s.trim().length() >= 8
                && s.trim().length() <= 120;
    }
    public static String idempotencyKey(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{7,99}"))
            throw new IllegalArgumentException("CLT_R3_VAL_COMMIT_IDEMPOTENCY_INVALID");
        return value;
    }
    public static String expectedVersion(String value) {
        if (!"3".equals(value))
            throw new IllegalArgumentException("CLT_R3_VAL_COMMIT_EXPECTED_VERSION_3_REQUIRED");
        return value;
    }
    public static String reason(String value) { return CollateralWorkflowPolicy.reason(value); }
    public static BigDecimal positiveId(Object raw, String error) {
        if (raw instanceof BigDecimal id && id.signum() > 0 && id.scale() <= 0 && id.precision() <= 19)
            return id;
        throw new IllegalStateException(error);
    }
    public static void guard(Command command, StageEvidence stage, Release release,
            boolean featureEnabled, boolean schemaVerified, boolean allowed) {
        if (command == null || stage == null)
            throw new IllegalArgumentException("CLT_R3_VAL_COMMIT_INPUT_REQUIRED");
        expectedVersion(command.expectedVersion()); idempotencyKey(command.idempotencyKey()); reason(command.reason());
        if (!featureEnabled || !schemaVerified)
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_DEPLOYMENT_GATE_CLOSED");
        if (!allowed || stage.postingSubject() == null || stage.postingSubject().isBlank()
                || stage.makerSubject() == null || stage.makerSubject().isBlank()
                || stage.postingSubject().equals(stage.makerSubject()))
            throw new SecurityException("CLT_R3_VAL_COMMIT_INDEPENDENT_POSTER_REQUIRED");
        if (release != null && Objects.equals(stage.postingSubject(), release.approvedBy()))
            throw new SecurityException("CLT_R3_VAL_COMMIT_INDEPENDENT_BANK_APPROVER_REQUIRED");
        if (release == null || !release.approved() || !reference(release.revision())
                || !reference(release.finalStatusDecision()) || !reference(release.appraisabilityDecision())
                || !reference(release.intervalDecision()) || !reference(release.businessDateDecision())
                || !reference(release.valuerAuthorityDecision()) || !reference(release.valueDecision())
                || !reference(release.supersessionDecision()) || !reference(release.approvedBy()))
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_APPROVED_RELEASE_REQUIRED");
        if (!"REVIEWED".equals(stage.status()) || !"3".equals(stage.version())
                || !stage.integrityVerified() || !stage.conflictFree() || stage.unknownChecks())
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_STAGE_EVIDENCE_INVALID");
    }
    /** Validate a bank-approved rule. HALF-OPEN policy: expiration is anchor+period-1 day. */
    public static LocalDate requirePeriod(TypeRule rule, String collateralTypeId,
            CollateralValuationPolicy.Candidate candidate, Release release,
            String postingSubject, LocalDate bankApprovedAsOfDate) {
        if (rule == null || !rule.approved() || !rule.appraisable() || release == null
                || !Objects.equals(rule.policyRevision(), release.revision())
                || !Objects.equals(rule.collateralTypeId(), collateralTypeId)
                || !reference(rule.approvedBy()) || Objects.equals(rule.approvedBy(), postingSubject))
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_TYPE_RULE_NOT_APPROVED");
        if (rule.intervalValue() < 1 || rule.intervalValue() > 36500
                || !List.of("DAY", "MONTH", "YEAR").contains(rule.intervalUnit())
                || !"EXCLUSIVE_NEXT_START".equals(rule.boundaryMode())
                || !List.of("VALUATION_DATE", "EFFECTIVE_FROM").contains(rule.anchor()))
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_INTERVAL_POLICY_INVALID");
        if (bankApprovedAsOfDate == null)
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_BUSINESS_DATE_NOT_PROVEN");
        LocalDate valuationDate = CollateralValuationPolicy.date(candidate.valuationDate());
        LocalDate effectiveFrom = CollateralValuationPolicy.date(candidate.effectiveFrom());
        if (candidate.effectiveTo() == null)
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_EXPLICIT_EFFECTIVE_TO_REQUIRED");
        LocalDate effectiveTo = CollateralValuationPolicy.date(candidate.effectiveTo());
        if (valuationDate.isAfter(bankApprovedAsOfDate) || effectiveFrom.isAfter(bankApprovedAsOfDate)
                || effectiveTo.isBefore(bankApprovedAsOfDate))
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_NOT_EFFECTIVE_ON_APPROVED_DATE");
        LocalDate anchorDate = "VALUATION_DATE".equals(rule.anchor()) ? valuationDate : effectiveFrom;
        final LocalDate ceiling;
        try {
            ceiling = switch (rule.intervalUnit()) {
                case "DAY" -> anchorDate.plusDays(rule.intervalValue()).minusDays(1);
                case "MONTH" -> anchorDate.plusMonths(rule.intervalValue()).minusDays(1);
                case "YEAR" -> anchorDate.plusYears(rule.intervalValue()).minusDays(1);
                default -> throw new IllegalStateException("CLT_R3_VAL_COMMIT_INTERVAL_POLICY_INVALID");
            };
        } catch (DateTimeException e) {
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_INTERVAL_OVERFLOW", e);
        }
        if (effectiveTo.isAfter(ceiling))
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_EFFECTIVE_TO_EXCEEDS_APPROVED_INTERVAL");
        return effectiveTo;
    }
    public static void requireValuer(ValuerRule rule, String typeId,
            CollateralValuationPolicy.Candidate candidate, Release release, String poster) {
        if (candidate.valuerPartyId() == null || rule == null || !rule.approved()
                || release == null || !Objects.equals(rule.policyRevision(), release.revision())
                || !Objects.equals(rule.partyId(), candidate.valuerPartyId())
                || !Objects.equals(rule.collateralTypeId(), typeId)
                || !Objects.equals(rule.methodCode(), candidate.methodCode())
                || !reference(rule.approvedBy()) || Objects.equals(rule.approvedBy(), poster))
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_VALUER_AUTHORITY_NOT_PROVEN");
    }
    public static void requireAmount(CollateralValuationPolicy.Candidate candidate,
            String collateralCurrency) {
        if (!Objects.equals(collateralCurrency, candidate.currencyCode()))
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_FX_NOT_APPROVED");
        BigDecimal assessed = new BigDecimal(candidate.valuationAmount());
        if (assessed.signum() <= 0)
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_NONPOSITIVE_ASSESSED_VALUE");
        if (candidate.forcedSaleValue() != null
                && new BigDecimal(candidate.forcedSaleValue()).compareTo(assessed) > 0)
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_FORCED_VALUE_EXCEEDS_ASSESSED");
        if (candidate.eligibleValue() != null
                && new BigDecimal(candidate.eligibleValue()).compareTo(assessed) > 0)
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_ELIGIBLE_VALUE_EXCEEDS_ASSESSED");
        // No inferred haircut/eligible-value calculation or FX conversion (G-10/G-09).
    }
    /** The parent is SELECT FOR UPDATE; previous row is also locked and must be FINAL. */
    public static void requireSupersession(String currentId, String requestedPreviousId,
            String predecessorCollateralId, String predecessorStatus, String predecessorCurrency,
            String predecessorEffectiveFrom, String collateralId, String currency,
            String newEffectiveFrom, long alreadySuperseded) {
        if (!Objects.equals(currentId, requestedPreviousId))
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_CURRENT_POINTER_CHANGED");
        if (currentId == null) {
            if (predecessorCollateralId != null || alreadySuperseded != 0)
                throw new IllegalStateException("CLT_R3_VAL_COMMIT_UNEXPECTED_PREDECESSOR");
            return;
        }
        if (!Objects.equals(collateralId, predecessorCollateralId)
                || !"FINAL".equals(predecessorStatus)
                || !Objects.equals(currency, predecessorCurrency)
                || predecessorEffectiveFrom == null || alreadySuperseded != 0L
                || CollateralValuationPolicy.date(newEffectiveFrom).isBefore(
                        CollateralValuationPolicy.date(predecessorEffectiveFrom)))
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_PREDECESSOR_NOT_CURRENT_FINAL");
    }
    public static long nextSequence(long existingMaximum) {
        if (existingMaximum < 0 || existingMaximum >= 9999999999L)
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_SEQUENCE_EXHAUSTED");
        return existingMaximum + 1;
    }
    public static void checkReplay(Command input, String key, String actualHash,
            String ledgerHash, String previousReason, String version,
            String currentPoster, String originalPoster) {
        if (!Objects.equals(input.idempotencyKey(), key)
                || !Objects.equals(actualHash, ledgerHash)
                || !Objects.equals(input.reason().trim(), previousReason)
                || !Objects.equals(input.expectedVersion(), version)
                || !Objects.equals(currentPoster, originalPoster))
            throw new IllegalStateException("CLT_R3_VAL_COMMIT_REPLAY_CONFLICT");
    }
    public static List<String> unresolvedAfterRegistration() {
        return List.of("G-07_VALUATION_FINAL_APPROVAL_MAPPING_FOR_OTHER_CHANNELS",
                "G-18_PERIODIC_APPRAISAL_DUE_AND_HOLIDAY_CALENDAR",
                "G-09_VALUER_EXTERNAL_RECONCILIATION_AND_FX",
                "G-10_ELIGIBLE_VALUE_AND_HAIRCUT_FOR_ALLOCATION");
    }
}
