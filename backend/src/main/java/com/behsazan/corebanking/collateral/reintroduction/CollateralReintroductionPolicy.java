package com.behsazan.corebanking.collateral.reintroduction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** BR-02/04/14. Pure, fail-closed decision logic; never infers a verified asset from free text. */
public final class CollateralReintroductionPolicy {
    private CollateralReintroductionPolicy() {}
    public record Facts(String branch, String previousBranch,
            String previousStatus, boolean previousStatusIsTerminal,
            boolean actualFinalReleasePosted, boolean finalStatusHistoryMatches,
            long activeAllocationCount, long unknownAllocationCount,
            String oldAssetId, String approvedAssetId, boolean assetIdentityVerified,
            long otherCurrentAssetLinks, boolean currentLinkIsPrevious,
            String originalTypeId, String reviewedTypeId,
            String reviewedBranch, String reviewStatus,
            String reviewHash, String draftHash, boolean draftStillImmutable,
            String reviewedPartyId, String approvedPartyId,
            boolean ownerReverified, boolean introductionRulesVerified,
            boolean serialRangesVerified, boolean bankRegistrationNumberAvailable,
            String oldRegistrationNumber, String bankRegistrationNumber,
            String initialStatusCode, boolean initialStatusApprovedActive,
            String sourceVersion, String approvedSourceVersion,
            String maker, String checker, String poster,
            String approvalReference, String ownerEvidenceReference,
            String identityEvidenceReference, String permitStatus,
            LocalDateTime expiresAt, LocalDateTime oracleNow) {}
    public static List<String> blockers(Facts f) {
        if (f == null) throw new IllegalArgumentException("REINTRODUCTION_FACTS_REQUIRED");
        List<String> b = new ArrayList<>();
        if (!"APPROVED".equals(f.permitStatus())) b.add("REINTRO_PERMIT_NOT_APPROVED");
        if (empty(f.branch()) || !f.branch().equals(f.previousBranch())
                || !f.branch().equals(f.reviewedBranch())) b.add("REINTRO_BRANCH_MISMATCH");
        if (!f.previousStatusIsTerminal() || !f.actualFinalReleasePosted()
                || !f.finalStatusHistoryMatches()) b.add("REINTRO_FINAL_RELEASE_NOT_PROVEN");
        if (f.activeAllocationCount() != 0 || f.unknownAllocationCount() != 0
                || f.activeAllocationCount() < 0 || f.unknownAllocationCount() < 0)
            b.add("REINTRO_PREVIOUS_ALLOCATION_NOT_CLEARED");
        if (empty(f.oldAssetId()) || !f.oldAssetId().equals(f.approvedAssetId())
                || !f.assetIdentityVerified()) b.add("REINTRO_STABLE_ASSET_NOT_VERIFIED");
        if (f.otherCurrentAssetLinks() != 0 || !f.currentLinkIsPrevious())
            b.add("REINTRO_ASSET_ALREADY_CURRENT_OR_LINK_INVALID");
        if (empty(f.originalTypeId()) || !f.originalTypeId().equals(f.reviewedTypeId()))
            b.add("REINTRO_ASSET_TYPE_CHANGED");
        if (!"REVIEWED".equals(f.reviewStatus()) || empty(f.reviewHash())
                || !f.reviewHash().equals(f.draftHash()) || !f.draftStillImmutable())
            b.add("REINTRO_REVIEWED_DRAFT_NOT_CONSISTENT");
        if (empty(f.reviewedPartyId()) || !f.reviewedPartyId().equals(f.approvedPartyId())
                || !f.ownerReverified() || empty(f.ownerEvidenceReference()))
            b.add("REINTRO_OWNER_REVERIFICATION_REQUIRED");
        if (!f.introductionRulesVerified() || !f.serialRangesVerified())
            b.add("REINTRO_INTRODUCTION_OR_SERIAL_RULES_REQUIRED");
        if (!f.bankRegistrationNumberAvailable() || !id(f.bankRegistrationNumber())
                || f.bankRegistrationNumber().equals(f.oldRegistrationNumber()))
            b.add("REINTRO_NEW_BANK_NUMBER_REQUIRED");
        if (!f.initialStatusApprovedActive() || empty(f.initialStatusCode())
                || f.initialStatusCode().equals(f.previousStatus()))
            b.add("REINTRO_NEW_ACTIVE_STATUS_NOT_APPROVED");
        if (!id(f.sourceVersion()) || !f.sourceVersion().equals(f.approvedSourceVersion()))
            b.add("REINTRO_RELEASED_COLLATERAL_VERSION_CHANGED");
        if (empty(f.maker()) || empty(f.checker()) || empty(f.poster())
                || f.maker().equals(f.checker()) || f.maker().equals(f.poster())
                || f.checker().equals(f.poster())) b.add("REINTRO_THREE_PARTY_APPROVAL_REQUIRED");
        if (empty(f.approvalReference()) || empty(f.identityEvidenceReference()))
            b.add("REINTRO_BANK_APPROVAL_OR_IDENTITY_EVIDENCE_REQUIRED");
        if (f.expiresAt() == null || f.oracleNow() == null || !f.expiresAt().isAfter(f.oracleNow()))
            b.add("REINTRO_APPROVAL_EXPIRED");
        return List.copyOf(b);
    }
    static boolean empty(String v) {return v == null || v.isBlank();}
    static boolean id(String v) {return v != null && v.matches("[1-9][0-9]{0,18}");}
    public static String requiredId(String v) {
        if (!id(v)) throw new IllegalArgumentException("REINTRO_DECIMAL_ID_INVALID");
        return v;
    }
    public static String subject(String v) {
        if (v==null || !v.matches("[A-Za-z0-9_.@:-]{1,100}"))
            throw new IllegalArgumentException("REINTRO_ACTOR_INVALID");
        return v;
    }
    public static String etag(String value) {
        if (value==null || !value.matches("\"[1-9][0-9]{0,9}\""))
            throw new IllegalArgumentException("REINTRO_STRONG_ETAG_REQUIRED");
        return value.substring(1,value.length()-1);
    }
    public static String idempotency(String v) {
        if(v==null || !v.matches("[A-Za-z0-9][A-Za-z0-9_./:-]{7,99}"))
            throw new IllegalArgumentException("REINTRO_IDEMPOTENCY_INVALID");
        return v;
    }
}
