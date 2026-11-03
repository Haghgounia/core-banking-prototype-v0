package com.behsazan.corebanking.collateral.introduction;

import java.util.Set;

/** R2D draft-review governance only. Never authorizes final collateral registration. */
public final class CollateralIntroductionReviewPolicy {
    private CollateralIntroductionReviewPolicy() {}
    public record Decision(String decision, String reason, String expectedVersion, String draftHash) {}
    public record CheckedDecision(String decision, String reason) {}

    public static CheckedDecision normalize(Decision input) {
        if (input == null || !"1".equals(input.expectedVersion()))
            throw new IllegalArgumentException("CLT_REVIEW_VERSION_MUST_BE_1");
        if (input.draftHash() == null || !input.draftHash().matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("CLT_REVIEW_DRAFT_HASH_REQUIRED");
        if (input.decision() == null || !Set.of("REVIEWED", "RETURNED").contains(input.decision()))
            throw new IllegalArgumentException("CLT_REVIEW_DECISION_INVALID");
        String reason = input.reason() == null ? "" : input.reason().strip();
        if (reason.length() < 10 || reason.length() > 500
                || reason.chars().anyMatch(c -> c < 32 || c == 127))
            throw new IllegalArgumentException("CLT_REVIEW_REASON_LENGTH_OR_CONTROL_INVALID");
        return new CheckedDecision(input.decision(), reason);
    }
    public static void separateActors(String maker, String checker) {
        if (maker == null || checker == null || maker.equals(checker))
            throw new IllegalArgumentException("CLT_REVIEW_MAKER_CHECKER_MUST_DIFFER");
    }
    public static void pending(String status, String version) {
        if (!"PENDING_REVIEW".equals(status) || !"1".equals(version))
            throw new IllegalArgumentException("CLT_REVIEW_NOT_PENDING_OR_STALE_VERSION");
    }
    public static void reviewedNeverRegisters(String status) {
        if (status == null || !Set.of("PENDING_REVIEW", "REVIEWED", "RETURNED").contains(status))
            throw new IllegalArgumentException("CLT_REVIEW_STATUS_INVALID");
    }
}
