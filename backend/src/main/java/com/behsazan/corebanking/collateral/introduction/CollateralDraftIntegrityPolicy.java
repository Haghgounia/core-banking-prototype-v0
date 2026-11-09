package com.behsazan.corebanking.collateral.introduction;

import java.util.ArrayList;
import java.util.List;

/** R2 draft integrity gate: an immutable snapshot is not a registered collateral. */
public final class CollateralDraftIntegrityPolicy {
    private CollateralDraftIntegrityPolicy() {}
    public record Evidence(boolean immutableDraftStatus, boolean initialRecordVersion,
            boolean storedHashMatches, boolean auditEventMatches, boolean currentRulesValid,
            boolean serialRowsValid, List<String> currentRuleBlockers) {}
    public record Assessment(boolean eligibleForReview, boolean snapshotConsistent,
            boolean auditConsistent, boolean currentRulesValid, List<String> blockers) {}

    public static Assessment assess(Evidence e) {
        if (e == null) throw new IllegalArgumentException("CLT_R2_INTEGRITY_EVIDENCE_REQUIRED");
        List<String> blockers = new ArrayList<>();
        if (!e.immutableDraftStatus()) blockers.add("CLT_R2_DRAFT_STATUS_NOT_DRAFT");
        if (!e.initialRecordVersion()) blockers.add("CLT_R2_DRAFT_VERSION_CHANGED");
        if (!e.storedHashMatches()) blockers.add("CLT_R2_DRAFT_FINGERPRINT_MISMATCH");
        if (!e.auditEventMatches()) blockers.add("CLT_R2_DRAFT_AUDIT_MISSING_OR_DIFFERENT");
        if (!e.serialRowsValid()) blockers.add("CLT_R2_DRAFT_SERIAL_RANGES_INVALID");
        if (!e.currentRulesValid()) {
            List<String> reasons = e.currentRuleBlockers() == null ? List.of() : e.currentRuleBlockers();
            if (reasons.isEmpty()) blockers.add("CLT_R2_DRAFT_CURRENT_RULES_NOT_VERIFIED");
            for (String reason : reasons) {
                if (reason != null && reason.matches("CLT_[A-Z0-9_:]{1,150}")
                    && !blockers.contains(reason)) blockers.add(reason);
            }
            if (blockers.stream().noneMatch(x -> x.equals("CLT_R2_DRAFT_CURRENT_RULES_NOT_VERIFIED")
                    || reasons.contains(x))) blockers.add("CLT_R2_DRAFT_CURRENT_RULES_NOT_VERIFIED");
        }
        return new Assessment(blockers.isEmpty(), e.immutableDraftStatus()
                && e.initialRecordVersion() && e.storedHashMatches(),
                e.auditEventMatches(), e.currentRulesValid(), List.copyOf(blockers));
    }
}
