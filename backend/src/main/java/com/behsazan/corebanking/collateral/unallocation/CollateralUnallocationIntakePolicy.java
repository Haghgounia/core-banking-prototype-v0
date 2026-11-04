package com.behsazan.corebanking.collateral.unallocation;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Pure staged-request lifecycle; neither status authorizes a financial write. */
public final class CollateralUnallocationIntakePolicy {
    private CollateralUnallocationIntakePolicy() {}
    public static final String RECORDED = "RECORDED_NOT_UNALLOCATED";
    public static final String CANCELLED = "CANCELLED_NOT_UNALLOCATED";
    public static final String EVIDENCE = "STAGING_REQUEST_ONLY_NO_FINANCIAL_UNALLOCATION";
    public static final List<String> MANDATORY_BLOCKERS = List.of(
            "G-09_CONSUMER_OBLIGATION_NOT_ATTESTED",
            "G-13_CROSS_BRANCH_AUTHORIZATION_NOT_APPROVED",
            "G-19_MODULE_MAPPING_REQUIRES_BANK_APPROVAL",
            "G-20_FULL_PARTIAL_FINANCIAL_LEDGER_POLICY_NOT_APPROVED",
            "CLT_R5B_UNALLOCATION_FINANCIAL_WRITES_DISABLED");
    public static int limit(int value) {
        if (value<1 || value>50) throw new IllegalArgumentException("CLT_R5B_JOURNAL_LIMIT_INVALID");
        return value;
    }
    public static String actor(String raw) {
        if(raw==null || raw.isBlank() || raw.length()>100)
            throw new IllegalArgumentException("CLT_R5B_ACTOR_REQUIRED");
        return raw;
    }
    public static String reason(String raw) {
        if(raw==null || !raw.matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,99}"))
            throw new IllegalArgumentException("CLT_R5B_CANCEL_REASON_INVALID");
        return raw;
    }
    public static List<String> snapshotBlockers(List<String> raw) {
        if(raw==null || raw.isEmpty() || raw.size()>40)
            throw new IllegalArgumentException("CLT_R5B_BLOCKERS_INVALID");
        for (String b : raw) if(!b.matches("[A-Z0-9_:-]{1,120}"))
            throw new IllegalArgumentException("CLT_R5B_BLOCKER_CODE_INVALID");
        if(!raw.contains("CLT_R5A_FINANCIAL_UNALLOCATION_AND_AUDIT_WRITES_DISABLED"))
            throw new IllegalArgumentException("CLT_R5B_MANDATORY_BLOCKER_NOT_PRESENT");
        return List.copyOf(raw);
    }
    public static String serializeBlockers(List<String> blockers) {
        String s = String.join(",",snapshotBlockers(blockers));
        if(s.length()>2000) throw new IllegalArgumentException("CLT_R5B_BLOCKERS_TOO_LARGE");
        return s;
    }
    public static List<String> parseBlockers(String raw) {
        if(raw==null || raw.isBlank()) throw new IllegalStateException("CLT_R5B_BLOCKERS_MISSING");
        return snapshotBlockers(List.of(raw.split(",",-1)));
    }
    public static String digest(String... tokens) {
        try {
            MessageDigest sha=MessageDigest.getInstance("SHA-256");
            for(String token:tokens) {
                if(token==null)throw new IllegalArgumentException("CLT_R5B_DIGEST_TOKEN_NULL");
                byte[] b=token.getBytes(StandardCharsets.UTF_8);
                sha.update(ByteBuffer.allocate(4).putInt(b.length).array());
                sha.update(b);
            }
            return HexFormat.of().formatHex(sha.digest());
        }catch(NoSuchAlgorithmException ex){throw new IllegalStateException("CLT_R5B_SHA256_UNAVAILABLE",ex);}
    }
    public static String cancelDigest(String requestId,String branch,String client,
            String domain,String expectedVersion,String reason) {
        return digest("R5B_CANCEL",requestId,branch,client,domain,expectedVersion,reason);
    }
    public static final class Conflict extends RuntimeException {public Conflict(String reason){super(reason);}}
    public static final class Stale extends RuntimeException {public Stale(String reason){super(reason);}}
    public static void replay(String existingDigest,String candidateDigest) {
        if(existingDigest==null || !MessageDigest.isEqual(
                existingDigest.getBytes(StandardCharsets.US_ASCII),
                candidateDigest.getBytes(StandardCharsets.US_ASCII)))
            throw new Conflict("CLT_R5B_IDEMPOTENCY_PAYLOAD_CONFLICT");
    }
    public static void canCancel(String status,String recordVersion,String expectedVersion) {
        if(!RECORDED.equals(status))
            throw new Conflict("CLT_R5B_REQUEST_NOT_CANCELLABLE");
        if(!recordVersion.equals(expectedVersion))
            throw new Stale("CLT_R5B_IF_MATCH_VERSION_CONFLICT");
    }
    public static boolean financialUnallocationAuthorized(){return false;}
    public static boolean finalReleaseAuthorized(){return false;}
}
