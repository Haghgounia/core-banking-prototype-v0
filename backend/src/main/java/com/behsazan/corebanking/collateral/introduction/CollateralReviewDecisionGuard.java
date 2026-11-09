package com.behsazan.corebanking.collateral.introduction;

import java.util.ArrayList;
import java.util.List;

/**
 * R2: the checker must independently validate the actual persisted Oracle
 * draft again at decision time. A successful maker-side submit is not proof
 * that the immutable payload, child rows or live CIF/CLT rules remain valid.
 * This is a draft REVIEW guard, not a final registration or asset-identity issuer.
 */
public final class CollateralReviewDecisionGuard {
    private CollateralReviewDecisionGuard() {}
    public record Evidence(boolean immutableDraft, boolean hashMatchesSnapshot,
            boolean creationAuditMatches, boolean childRangesMatch,
            boolean currentCifAndReferenceValid, boolean candidateStageAvailable,
            boolean candidateOtherOwnerConflict, boolean hasCandidate) {}
    public record Assessment(boolean reviewAllowed, List<String> blockers) {}

    public static Assessment assess(Evidence e) {
        if (e == null) throw new IllegalArgumentException("CLT_REVIEW_EVIDENCE_REQUIRED");
        List<String> blockers = new ArrayList<>();
        if (!e.immutableDraft()) blockers.add("CLT_REVIEW_DRAFT_NOT_IMMUTABLE");
        if (!e.hashMatchesSnapshot()) blockers.add("CLT_REVIEW_STORED_SNAPSHOT_TAMPERED");
        if (!e.creationAuditMatches()) blockers.add("CLT_REVIEW_CREATION_AUDIT_INVALID");
        if (!e.childRangesMatch()) blockers.add("CLT_REVIEW_SERIAL_ROWS_MISMATCH");
        if (!e.currentCifAndReferenceValid()) blockers.add("CLT_REVIEW_CURRENT_RULES_INVALID");
        if (e.hasCandidate() && !e.candidateStageAvailable())
            blockers.add("CLT_REVIEW_ASSET_CANDIDATE_CHECK_UNAVAILABLE");
        if (e.candidateOtherOwnerConflict())
            blockers.add("CLT_REVIEW_ASSET_OTHER_OWNER_STAGE_CONFLICT");
        return new Assessment(blockers.isEmpty(), List.copyOf(blockers));
    }
}
