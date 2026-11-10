package com.behsazan.corebanking.collateral.release;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** BUC-11: pure decision boundary for a trusted, independently approved final-release permit. */
public final class CollateralReleasePostingPolicy {
    private CollateralReleasePostingPolicy() {}
    public record Facts(String expectedVersion, String actualVersion, String permitVersion,
            String currentStatus, String approvedSourceStatus, String targetStatus,
            String configuredTargetStatus, boolean targetIsActiveTerminal,
            String collateralBranch, String permitBranch,
            long activeAllocations, long foreignBranchAllocations, long invalidAllocations,
            long unresolvedTransactions, String permitStatus, String maker, String checker,
            String poster, String approvalRef, String legalClearanceRef,
            String consumerClearanceRef, String unallocationRef,
            LocalDateTime expiresAt, LocalDateTime databaseNow,
            String lastHistoryStatus, long lastHistorySequence) {}
    public static List<String> blockers(Facts f) {
        if (f == null) throw new IllegalArgumentException("CLT_RELEASE_FACTS_REQUIRED");
        List<String> issues=new ArrayList<>();
        if (!"APPROVED".equals(f.permitStatus())) issues.add("RELEASE_PERMIT_NOT_APPROVED");
        if (blank(f.poster()) || blank(f.maker()) || blank(f.checker())
                || f.maker().equals(f.checker()) || f.poster().equals(f.maker())
                || f.poster().equals(f.checker())) issues.add("RELEASE_INDEPENDENT_ACTORS_REQUIRED");
        if (blank(f.collateralBranch()) || !f.collateralBranch().equals(f.permitBranch()))
            issues.add("RELEASE_BRANCH_MISMATCH");
        if (blank(f.expectedVersion()) || !f.expectedVersion().equals(f.actualVersion())
                || !f.actualVersion().equals(f.permitVersion())) issues.add("RELEASE_COLLATERAL_VERSION_CHANGED");
        if (blank(f.approvedSourceStatus()) || !f.approvedSourceStatus().equals(f.currentStatus()))
            issues.add("RELEASE_SOURCE_STATUS_NOT_APPROVED");
        if (blank(f.configuredTargetStatus()) || !f.configuredTargetStatus().equals(f.targetStatus())
                || !f.targetIsActiveTerminal() || f.targetStatus().equals(f.currentStatus()))
            issues.add("RELEASE_TARGET_STATUS_NOT_APPROVED");
        if (f.activeAllocations() != 0) issues.add("RELEASE_ACTIVE_ALLOCATIONS");
        if (f.foreignBranchAllocations() != 0) issues.add("RELEASE_FOREIGN_BRANCH_ALLOCATIONS");
        if (f.invalidAllocations() != 0) issues.add("RELEASE_UNRECOGNIZED_ALLOCATION_STATE");
        if (f.unresolvedTransactions() != 0) issues.add("RELEASE_UNRESOLVED_TRANSACTIONS");
        if (f.activeAllocations()<0 || f.foreignBranchAllocations()<0 || f.invalidAllocations()<0
                || f.unresolvedTransactions()<0) issues.add("RELEASE_INVALID_EVIDENCE_COUNTS");
        if (blank(f.approvalRef()) || blank(f.legalClearanceRef())
                || blank(f.consumerClearanceRef()) || blank(f.unallocationRef()))
            issues.add("RELEASE_SIGNED_APPROVAL_EVIDENCE_INCOMPLETE");
        if (f.databaseNow()==null || f.expiresAt()==null
                || !f.expiresAt().isAfter(f.databaseNow())) issues.add("RELEASE_PERMIT_EXPIRED");
        if (f.lastHistorySequence()<1 || !f.currentStatus().equals(f.lastHistoryStatus()))
            issues.add("RELEASE_STATUS_HISTORY_CHAIN_INCONSISTENT");
        return List.copyOf(issues);
    }
    private static boolean blank(String s) {return s==null || s.isBlank();}
    public static String actor(String value) {
        if (value==null || !value.matches("[A-Za-z0-9_.@:-]{1,100}"))
            throw new IllegalArgumentException("RELEASE_ACTOR_INVALID");
        return value;
    }
    public static String code(String value) {
        if (value==null || !value.matches("[A-Z][A-Z0-9_]{1,29}"))
            throw new IllegalArgumentException("RELEASE_STATUS_CODE_NOT_APPROVED");
        return value;
    }
    public static String decimalId(String raw) {
        if (raw == null || !raw.matches("[1-9][0-9]{0,18}"))
            throw new IllegalArgumentException("RELEASE_DECIMAL_ID_INVALID");
        return raw;
    }
    public static String version(String raw) {
        if(raw == null || !raw.matches("[1-9][0-9]{0,9}"))
            throw new IllegalArgumentException("RELEASE_VERSION_INVALID");
        return raw;
    }
    public static String key(String raw) {return CollateralFinalReleasePolicy.idempotencyKey(raw);}
    public static String etag(String raw) {return CollateralFinalReleasePolicy.strongEtag(raw);}
}
