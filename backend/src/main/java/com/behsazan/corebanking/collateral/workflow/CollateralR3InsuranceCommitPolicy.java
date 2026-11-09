package com.behsazan.corebanking.collateral.workflow;

import com.behsazan.corebanking.collateral.insurance.CollateralInsurancePolicy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * BUC-07 / SUC-09 / SVC-07: guarded creation or one-to-one renewal of a policy.
 * An operational policy row does not, by itself, establish approved coverage for allocation.
 * Cancellation is explicitly NOT performed by this contract (G-06).
 */
public final class CollateralR3InsuranceCommitPolicy {
    private CollateralR3InsuranceCommitPolicy() {}

    public record Command(String expectedVersion, String idempotencyKey, String reason) {}
    public record Release(String revision, String businessDecision, String dataDecision,
            String integrationDecision, String coverageDecision, String renewalDecision,
            String cancellationImpactDecision, String approvedBy, boolean approved) {}
    public record StageEvidence(String status, String version, boolean integrityVerified,
            boolean conflictFree, boolean unknownChecks, String makerSubject, String postingSubject) {}
    public record CoverageRule(String collateralTypeId, String insuranceTypeId,
            String currencyCode, String minimumAmount, String policyRevision,
            String approvedBy, boolean approved) {}
    public record Receipt(String requestId, String collateralId, String insuranceId,
            String status, String policyRevision, String idempotencyKey,
            String previousInsuranceId, boolean replayed) {}

    public static String key(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{7,99}"))
            throw new IllegalArgumentException("CLT_R3_INSURANCE_COMMIT_IDEMPOTENCY_INVALID");
        return value;
    }
    public static String version(String value) {
        if (!"3".equals(value))
            throw new IllegalArgumentException("CLT_R3_INSURANCE_COMMIT_EXPECTED_VERSION_3_REQUIRED");
        return value;
    }
    public static String reason(String value) {
        return CollateralR3WorkflowPolicy.reason(value);
    }
    public static BigDecimal positiveId(Object raw, String code) {
        if (raw instanceof BigDecimal n && n.signum() > 0 && n.scale() <= 0 && n.precision() <= 19)
            return n;
        throw new IllegalStateException(code);
    }
    private static boolean ref(String value) {
        return value != null && value.trim().length() >= 8 && value.trim().length() <= 120;
    }
    public static void guard(Command command, StageEvidence stage, Release release,
            boolean featureEnabled, boolean schemaVerified, boolean permitted) {
        if (command == null || stage == null)
            throw new IllegalArgumentException("CLT_R3_INSURANCE_COMMIT_INPUT_REQUIRED");
        version(command.expectedVersion()); key(command.idempotencyKey()); reason(command.reason());
        if (!featureEnabled || !schemaVerified)
            throw new IllegalStateException("CLT_R3_INSURANCE_COMMIT_DEPLOYMENT_GATE_CLOSED");
        if (!permitted || stage.postingSubject() == null || stage.postingSubject().isBlank()
                || stage.makerSubject() == null || stage.makerSubject().isBlank()
                || Objects.equals(stage.postingSubject(), stage.makerSubject()))
            throw new SecurityException("CLT_R3_INSURANCE_COMMIT_INDEPENDENT_OPERATOR_REQUIRED");
        if (release != null && Objects.equals(stage.postingSubject(), release.approvedBy()))
            throw new SecurityException("CLT_R3_INSURANCE_COMMIT_INDEPENDENT_BANK_APPROVER_REQUIRED");
        if (release == null || !release.approved() || !ref(release.revision())
                || !ref(release.businessDecision()) || !ref(release.dataDecision())
                || !ref(release.integrationDecision()) || !ref(release.coverageDecision())
                || !ref(release.renewalDecision()) || !ref(release.cancellationImpactDecision())
                || !ref(release.approvedBy()))
            throw new IllegalStateException("CLT_R3_INSURANCE_COMMIT_APPROVED_RELEASE_REQUIRED");
        if (!"REVIEWED".equals(stage.status()) || !"3".equals(stage.version())
                || !stage.integrityVerified() || !stage.conflictFree() || stage.unknownChecks())
            throw new IllegalStateException("CLT_R3_INSURANCE_COMMIT_STAGE_EVIDENCE_INVALID");
    }
    /** Per-collateral-type, per-insurance-type, per-currency approved minimum: no inferred default. */
    public static void requireCoverage(CoverageRule rule, String collateralTypeId,
            CollateralInsurancePolicy.Candidate candidate, Release release,
            String poster) {
        if (rule == null || !rule.approved() || release == null
                || !Objects.equals(rule.policyRevision(), release.revision())
                || !Objects.equals(rule.collateralTypeId(), collateralTypeId)
                || !Objects.equals(rule.insuranceTypeId(), candidate.insuranceTypeId())
                || !Objects.equals(rule.currencyCode(), candidate.currencyCode())
                || !ref(rule.approvedBy()) || Objects.equals(rule.approvedBy(), poster))
            throw new IllegalStateException("CLT_R3_INSURANCE_COMMIT_COVERAGE_RULE_NOT_APPROVED");
        final BigDecimal minimum;
        try { minimum = new BigDecimal(rule.minimumAmount()); }
        catch (RuntimeException ex) {
            throw new IllegalStateException("CLT_R3_INSURANCE_COMMIT_MINIMUM_COVERAGE_INVALID",ex);
        }
        if (minimum.signum() <= 0 || minimum.scale() > 4 || minimum.precision() > 20)
            throw new IllegalStateException("CLT_R3_INSURANCE_COMMIT_MINIMUM_COVERAGE_INVALID");
        if (new BigDecimal(candidate.insuranceAmount()).compareTo(minimum) < 0)
            throw new IllegalStateException("CLT_R3_INSURANCE_COMMIT_COVERAGE_BELOW_APPROVED_MINIMUM");
    }
    /** Renewals must link exactly one predecessor of the same collateral and insurance type. */
    public static void requireRenewal(CollateralInsurancePolicy.Candidate candidate,
            String collateralId, String predecessorCollateralId, String predecessorTypeId,
            String predecessorValidTo, long successorCount) {
        if (candidate.previousInsuranceId() == null) return;
        if (predecessorCollateralId == null || predecessorTypeId == null || predecessorValidTo == null
                || successorCount != 0 || !CollateralInsurancePolicy.validRenewal(
                    collateralId,candidate.insuranceTypeId(),predecessorCollateralId,
                    predecessorTypeId,predecessorValidTo,candidate.validFrom(),false))
            throw new IllegalStateException("CLT_R3_INSURANCE_COMMIT_RENEWAL_CHAIN_INVALID");
    }
    public static void checkReplay(Command cmd, String key, String actualHash, String priorHash,
            String reason, String version, String poster, String previousPoster) {
        if (!Objects.equals(cmd.idempotencyKey(),key) || !Objects.equals(actualHash,priorHash)
                || !Objects.equals(cmd.reason().trim(),reason)
                || !Objects.equals(cmd.expectedVersion(),version)
                || !Objects.equals(poster,previousPoster))
            throw new IllegalStateException("CLT_R3_INSURANCE_COMMIT_REPLAY_CONFLICT");
    }
    public static List<String> unresolvedAfterRegistration() {
        return List.of("G-04_REQUIRED_INSURANCE_SET_AND_ALLOCATION_COVERAGE",
                "G-06_CANCELLATION_IMPACT_AND_HISTORY",
                "G-09_EXTERNAL_INSURER_VERIFICATION_OR_INTEGRATION");
    }
}
