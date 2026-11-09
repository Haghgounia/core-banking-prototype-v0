package com.behsazan.corebanking.collateral.workflow;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * R3 sponsor Stage-to-domain commit contract. A Stage REVIEWED event does not itself
 * authorize an operational change. All decision evidence must exist in CLT Oracle.
 * No new IAM subsystem, separate connection, or application-side policy override.
 */
public final class CollateralR3SponsorCommitPolicy {
    private CollateralR3SponsorCommitPolicy() {}

    public record Command(String expectedVersion, String idempotencyKey, String reason) {}
    public record Release(String revision, String businessDecision, String dataDecision,
            String integrationDecision, String approvedBy, boolean approved) {}
    public record StageEvidence(String status, String version, boolean integrityVerified,
            boolean conflictFree, boolean unknownChecks, String makerSubject,
            String postingSubject) {}
    public record Receipt(String requestId, String collateralId, String sponsorId,
            String status, String policyRevision, String idempotencyKey, boolean replayed) {}

    public static String key(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{7,99}"))
            throw new IllegalArgumentException("CLT_R3_SPONSOR_COMMIT_IDEMPOTENCY_INVALID");
        return value;
    }
    public static String reason(String value) {
        return CollateralR3WorkflowPolicy.reason(value);
    }
    public static String version(String value) {
        if (value == null || !value.equals("3"))
            throw new IllegalArgumentException("CLT_R3_SPONSOR_COMMIT_EXPECTED_VERSION_3_REQUIRED");
        return value;
    }
    public static void guard(Command command, StageEvidence evidence, Release release,
            boolean codeEnabled, boolean schemaVerified, boolean authorized) {
        if (command == null || evidence == null)
            throw new IllegalArgumentException("CLT_R3_SPONSOR_COMMIT_INPUT_REQUIRED");
        version(command.expectedVersion());
        key(command.idempotencyKey());
        reason(command.reason());
        if (!codeEnabled || !schemaVerified)
            throw new IllegalStateException("CLT_R3_SPONSOR_COMMIT_DEPLOYMENT_GATE_CLOSED");
        if (!authorized || Objects.equals(evidence.makerSubject(), evidence.postingSubject())
                || evidence.postingSubject() == null || evidence.postingSubject().isBlank())
            throw new SecurityException("CLT_R3_SPONSOR_COMMIT_INDEPENDENT_OPERATOR_REQUIRED");
        if (release != null && Objects.equals(release.approvedBy(), evidence.postingSubject()))
            throw new SecurityException("CLT_R3_SPONSOR_COMMIT_APPROVER_MUST_BE_INDEPENDENT");
        if (release == null || !release.approved() || !decision(release.businessDecision())
                || !decision(release.dataDecision()) || !decision(release.integrationDecision())
                || !decision(release.revision()) || !decision(release.approvedBy()))
            throw new IllegalStateException("CLT_R3_SPONSOR_COMMIT_BANK_RELEASE_REQUIRED");
        if (!"REVIEWED".equals(evidence.status()) || !"3".equals(evidence.version())
                || !evidence.integrityVerified() || !evidence.conflictFree()
                || evidence.unknownChecks())
            throw new IllegalStateException("CLT_R3_SPONSOR_COMMIT_STAGE_EVIDENCE_INVALID");
    }
    private static boolean decision(String ref) {
        return ref != null && ref.trim().length() >= 8 && ref.trim().length() <= 120;
    }
    public static void checkReplay(Command command, String previousKey, String priorHash,
            String currentHash, String originalPoster) {
        if (!Objects.equals(command.idempotencyKey(),previousKey)
                || !Objects.equals(priorHash,currentHash)
                || originalPoster == null || originalPoster.isBlank())
            throw new IllegalStateException("CLT_R3_SPONSOR_COMMIT_REPLAY_CONFLICT");
    }
    public static void requireExactlyOne(long count, String code) {
        if (count != 1) throw new IllegalStateException(code);
    }
    public static BigDecimal decimal(Object value, String code) {
        if (value instanceof BigDecimal dec && dec.signum() > 0 && dec.precision() <= 19)
            return dec;
        throw new IllegalStateException(code);
    }
    /** Sponsor addition does not settle G-11 minimum sponsor count for later allocation. */
    public static List<String> unresolvedOutsideAdd() {
        return List.of("G-05_SPONSOR_CANCELLATION_LIFECYCLE", "G-11_MINIMUM_SPONSORS_ON_ALLOCATION");
    }
}
