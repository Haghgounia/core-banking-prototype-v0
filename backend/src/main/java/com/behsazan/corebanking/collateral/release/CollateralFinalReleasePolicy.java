package com.behsazan.corebanking.collateral.release;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/** Pure business safety boundary: evidence is never a final release authorization. */
public final class CollateralFinalReleasePolicy {
    private CollateralFinalReleasePolicy() {}

    public record Facts(long activeAllocations, long allAllocations, long foreignBranchAllocations,
            long invalidUsedAllocations, long pendingTransactions, boolean releaseInquiryRequired,
            boolean terminalStatusReported, String expectedVersion, String currentVersion) {}

    public static String positiveId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}"))
            throw new IllegalArgumentException("CLT_R5C_INVALID_COLLATERAL_ID");
        // NUMBER(19) max is 19 digits; strip no zeros, do not coerce through double.
        return new BigDecimal(value).toPlainString();
    }
    public static int limit(int value) {
        if (value < 1 || value > 50) throw new IllegalArgumentException("CLT_R5C_HISTORY_LIMIT_INVALID");
        return value;
    }
    public static String strongEtag(String value) {
        if (value == null || !value.matches("\"[1-9][0-9]{0,9}\""))
            throw new IllegalArgumentException("CLT_R5C_STRONG_ETAG_REQUIRED");
        return value.substring(1, value.length() - 1);
    }
    public static String reason(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,99}"))
            throw new IllegalArgumentException("CLT_R5C_REASON_REFERENCE_INVALID");
        return value;
    }
    public static String idempotencyKey(String value) {
        if (value == null || !value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"))
            throw new IllegalArgumentException("CLT_R5C_IDEMPOTENCY_KEY_INVALID");
        UUID.fromString(value);
        return value;
    }
    /** Digest is diagnostic only: it is NOT persisted and does not make preview idempotent. */
    public static String digest(String id, String branch, String expectedVersion,
            String reference, String key) {
        try {
            MessageDigest sha=MessageDigest.getInstance("SHA-256");
            for(String part:List.of(positiveId(id),branch,expectedVersion,reason(reference),idempotencyKey(key))){
                byte[] bytes=part.getBytes(StandardCharsets.UTF_8);
                sha.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
                sha.update(bytes);
            }
            return HexFormat.of().formatHex(sha.digest());
        }catch(NoSuchAlgorithmException e){throw new IllegalStateException("CLT_R5C_SHA256_UNAVAILABLE",e);}
    }
    public static List<String> blockers(Facts facts) {
        if (facts == null || facts.activeAllocations < 0 || facts.allAllocations < 0 ||
                facts.foreignBranchAllocations < 0 || facts.invalidUsedAllocations < 0 ||
                facts.pendingTransactions < 0 || facts.activeAllocations > facts.allAllocations)
            throw new IllegalArgumentException("CLT_R5C_INCONSISTENT_EVIDENCE");
        List<String> blockers = new ArrayList<>();
        if (facts.activeAllocations() > 0) blockers.add("CLT_R5C_ACTIVE_ALLOCATIONS_REMAIN");
        if (facts.foreignBranchAllocations() > 0) blockers.add("CLT_R5C_CROSS_BRANCH_ALLOCATION_REVIEW_REQUIRED");
        if (facts.invalidUsedAllocations() > 0) blockers.add("CLT_R5C_UNKNOWN_ALLOCATION_USAGE_STATE");
        if (facts.pendingTransactions() > 0) blockers.add("CLT_R5C_PENDING_TRANSACTIONS_REMAIN");
        if (facts.releaseInquiryRequired()) blockers.add("CLT_R5C_REQUIRED_RELEASE_INQUIRY_NOT_ATTESTED");
        if (facts.terminalStatusReported()) blockers.add("CLT_R5C_TERMINAL_STATUS_IS_NOT_RELEASE_PROOF");
        if (facts.expectedVersion() != null && !facts.expectedVersion().equals(facts.currentVersion()))
            blockers.add("CLT_R5C_COLLATERAL_VERSION_MISMATCH");
        blockers.add("G-09_CONSUMER_OBLIGATION_CLEARANCE_NOT_ATTESTED");
        blockers.add("G-13_CROSS_BRANCH_LEGAL_AUTHORIZATION_UNCONFIRMED");
        blockers.add("G-20_FINANCIAL_UNALLOCATION_AND_LEDGER_NOT_FINALIZED");
        blockers.add("G-21_LEGAL_HOLDS_DPS2_PLEDGES_AND_INQUIRIES_NOT_VERIFIED");
        blockers.add("CLT_R5C_MULTI_QUERY_EVIDENCE_NOT_TRANSACTIONALLY_LOCKED");
        blockers.add("CLT_R5C_FINAL_RELEASE_AND_STATUS_WRITES_DISABLED");
        return List.copyOf(blockers);
    }
    public static boolean finalReleaseAllowed(Facts ignored) {return false;}
}
