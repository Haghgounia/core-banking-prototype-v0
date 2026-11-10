package com.behsazan.corebanking.collateral.crossunit;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/** BUC-15/BR-10: both type AND approved introduction-specific grant are mandatory. */
public final class CollateralCrossUnitPolicy {
    private CollateralCrossUnitPolicy() {}
    public static String id(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}"))
            throw new IllegalArgumentException("CLT_CROSS_UNIT_POSITIVE_ID_REQUIRED");
        return value;
    }
    public static String branch(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{1,30}"))
            throw new IllegalArgumentException("CLT_CROSS_UNIT_BRANCH_INVALID");
        return value;
    }
    public static String reference(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,99}"))
            throw new IllegalArgumentException("CLT_CROSS_UNIT_REFERENCE_INVALID");
        return value;
    }
    public static String subject(String value) {
        if (value == null || value.isBlank() || value.length() > 120)
            throw new IllegalArgumentException("CLT_CROSS_UNIT_ACTOR_INVALID");
        return value;
    }
    public static String requestKey(String value) {
        try {
            if (value == null || !UUID.fromString(value).toString().equals(value))
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("CLT_CROSS_UNIT_UUID_REQUIRED");
        }
        return value;
    }
    public static void otherBranch(String owner, String consumer) {
        branch(owner); branch(consumer);
        if (owner.equals(consumer)) throw new IllegalArgumentException("CLT_CROSS_UNIT_SAME_BRANCH_NOT_ALLOWED");
    }
    public static void checker(String maker, String checker) {
        if (subject(maker).equals(subject(checker)))
            throw new IllegalArgumentException("CLT_CROSS_UNIT_MAKER_CHECKER_MUST_DIFFER");
    }
    public static boolean authorized(boolean typeEnabled, boolean statusActive,
            String grantStatus, String owner, String consumer, String grantedConsumer,
            String grantVersion, String currentVersion, Instant expiration, Instant now) {
        return typeEnabled && statusActive && "APPROVED".equals(grantStatus)
            && owner != null && consumer != null && !owner.equals(consumer)
            && consumer.equals(grantedConsumer) && grantVersion != null
            && grantVersion.equals(currentVersion) && expiration != null && now.isBefore(expiration);
    }
    public static Instant expiry(String text, Instant now) {
        Instant t;
        try {t = Instant.parse(text);} catch (Exception ex) {
            throw new IllegalArgumentException("CLT_CROSS_UNIT_EXPIRY_ISO_UTC_REQUIRED");
        }
        // Technical guardrail only; not a substitute for bank-issued policy validity.
        if (!t.isAfter(now) || t.isAfter(now.plusSeconds(366L * 86400)))
            throw new IllegalArgumentException("CLT_CROSS_UNIT_EXPIRY_OUT_OF_BOUNDS");
        return t;
    }
    public static String hash(List<String> fields) {
        try {
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            for (String field : fields) {
                if (field == null) throw new IllegalArgumentException("CLT_CROSS_UNIT_HASH_FIELD_MISSING");
                byte[] bytes=field.getBytes(StandardCharsets.UTF_8);
                md.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
                md.update(bytes);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException ex) {throw new IllegalStateException("CLT_CROSS_UNIT_HASH_UNAVAILABLE",ex);}
    }
    public static void sameDigest(String stored, String submitted) {
        if (stored == null || submitted == null ||
                !MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8),
                        submitted.getBytes(StandardCharsets.UTF_8)))
            throw new IllegalArgumentException("CLT_CROSS_UNIT_IDEMPOTENCY_PAYLOAD_CONFLICT");
    }
}
