package com.behsazan.corebanking.collateral.allocation;

import java.util.List;

/** R4C immutable, unauthorizing request journal. Concurrency is request-key scoped, NOT capacity scoped. */
public final class CollateralAllocationIntakePolicy {
    private CollateralAllocationIntakePolicy() {}
    public static final String STATUS = "RECORDED_NOT_ALLOCATED";
    public static final String EVIDENCE = "NON_ATOMIC_SNAPSHOT_NO_CONSUMER_ATTESTATION";
    public static final List<String> OPERATIONAL_BLOCKERS = List.of(
            "G-09_CONSUMER_OBLIGATION_AND_ROSTER_NOT_VERIFIED",
            "G-13_INTRODUCTION_CROSS_BRANCH_PERMISSION_NOT_PROVEN",
            "G-04_G-05_G-07_G-10_G-11_G-18_OPERATIONAL_POLICY_NOT_APPROVED",
            "CLT_ALLOCATION_CAPACITY_NOT_LOCKED",
            "CLT_ALLOCATION_NO_APPROVED_DOMAIN_MODULE_ID",
            "CLT_ALLOCATION_FINANCIAL_WRITE_DISABLED");

    public static String clientId(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:/@-]{0,159}"))
            throw new IllegalArgumentException("CLT_ALLOCATION_CLIENT_ID_INVALID");
        return value;
    }
    public static String actor(String value) {
        if (value == null || value.isBlank() || value.length() > 100 || value.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("CLT_ALLOCATION_SUBJECT_INVALID");
        return value;
    }
    public static int limit(int value) {
        if (value < 1 || value > 50) throw new IllegalArgumentException("CLT_ALLOCATION_JOURNAL_LIMIT_INVALID");
        return value;
    }
    /** Never permit a replay of the same key for a changed payload, collateral or branch. */
    public static void requireReplayMatch(String storedDigest, String suppliedDigest, String storedStatus) {
        if (storedDigest == null || suppliedDigest == null ||
                !java.security.MessageDigest.isEqual(storedDigest.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                suppliedDigest.getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
            throw new IllegalArgumentException("CLT_ALLOCATION_IDEMPOTENCY_PAYLOAD_CONFLICT");
        if (!STATUS.equals(storedStatus))
            throw new IllegalStateException("CLT_ALLOCATION_UNEXPECTED_JOURNAL_STATUS");
    }
    public static boolean everAuthorizesAllocation() { return false; }
}
