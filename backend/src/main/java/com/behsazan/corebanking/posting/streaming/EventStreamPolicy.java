package com.behsazan.corebanking.posting.streaming;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Pure retry and idempotency rules; no repository or financial dependencies. */
public final class EventStreamPolicy {
    private EventStreamPolicy() { }

    public static long retrySeconds(int attempts) {
        if (attempts < 1) throw new IllegalArgumentException("attempt count must be positive");
        return Math.min(3600L, 1L << Math.min(attempts, 12));
    }

    public static boolean exhausted(int attempts, int maximum) {
        if (maximum < 1 || attempts < 1) throw new IllegalArgumentException("Invalid retry configuration");
        return attempts >= maximum;
    }

    /** Hash also binds business metadata so the inbox rejects a changed type/key with the same data. */
    public static String receiptFingerprint(EventStreamEnvelope event) {
        event.validate();
        String source = String.join("\u0000", String.valueOf(event.schemaVersion()),
                event.sourceDomain(), event.eventId(), event.eventType(),
                event.aggregateReference(), event.occurredAt(), event.payloadSha256());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public static boolean sameEvent(EventStreamEnvelope first, EventStreamEnvelope second) {
        first.validate(); second.validate();
        return first.eventId().equals(second.eventId())
                && first.sourceDomain().equals(second.sourceDomain())
                && first.eventType().equals(second.eventType())
                && first.aggregateReference().equals(second.aggregateReference())
                && first.payloadSha256().equals(second.payloadSha256());
    }
}
