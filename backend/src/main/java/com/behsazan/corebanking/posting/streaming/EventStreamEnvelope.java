package com.behsazan.corebanking.posting.streaming;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/** Transport envelope only. Never a claim that a financial posting was committed. */
public record EventStreamEnvelope(
        int schemaVersion,
        String eventId,
        String sourceDomain,
        String eventType,
        String aggregateReference,
        String occurredAt,
        String payloadSha256,
        String payloadBase64) {
    private static final int MAX_PAYLOAD_BYTES = 65536;
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.:-]*");

    public static EventStreamEnvelope create(String eventId, String sourceDomain, String eventType,
                                              String aggregateReference, String payloadJson, Instant occurredAt) {
        Objects.requireNonNull(payloadJson, "payloadJson");
        byte[] content = payloadJson.getBytes(StandardCharsets.UTF_8);
        EventStreamEnvelope event = new EventStreamEnvelope(1, eventId, sourceDomain, eventType,
                aggregateReference, Objects.requireNonNull(occurredAt, "occurredAt").toString(),
                sha256(content), Base64.getEncoder().encodeToString(content));
        event.validate();
        return event;
    }

    public void validate() {
        if (schemaVersion != 1) throw new IllegalArgumentException("Unknown streaming schema version");
        token(eventId, 120, "eventId");
        token(sourceDomain, 160, "sourceDomain");
        token(eventType, 60, "eventType");
        if (aggregateReference == null || aggregateReference.isBlank() || aggregateReference.length() > 160
                || aggregateReference.chars().anyMatch(ch -> ch < 32))
            throw new IllegalArgumentException("Invalid aggregateReference");
        Instant.parse(Objects.requireNonNull(occurredAt, "occurredAt"));
        if (payloadSha256 == null || !payloadSha256.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Invalid payload SHA-256");
        byte[] content = contentBytes();
        if (!MessageDigest.isEqual(sha256(content).getBytes(StandardCharsets.US_ASCII),
                payloadSha256.getBytes(StandardCharsets.US_ASCII)))
            throw new IllegalArgumentException("Payload SHA-256 mismatch");
    }

    public String payloadJson() { return new String(contentBytes(), StandardCharsets.UTF_8); }

    private byte[] contentBytes() {
        if (payloadBase64 == null || payloadBase64.length() > 4 * ((MAX_PAYLOAD_BYTES + 2) / 3))
            throw new IllegalArgumentException("Event payload missing or too large");
        byte[] bytes = Base64.getDecoder().decode(payloadBase64);
        if (bytes.length == 0 || bytes.length > MAX_PAYLOAD_BYTES)
            throw new IllegalArgumentException("Event payload missing or too large");
        return bytes;
    }

    private static void token(String value, int length, String name) {
        if (value == null || value.length() > length || !TOKEN.matcher(value).matches())
            throw new IllegalArgumentException("Invalid " + name);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
