package com.behsazan.corebanking.posting.streaming;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

/** Tests never contact Kafka or Oracle. */
class EventStreamPolicyTest {
    private static EventStreamEnvelope event(String id, String json) {
        return EventStreamEnvelope.create(id, "PST", "STREAM_CONTRACT_TEST", "TEST:001", json, Instant.parse("2026-10-17T00:00:00Z"));
    }

    @Test void unicodeJsonRoundTripAndExactHash() {
        var e = event("EVT:001", "{\"persian\":\"آزمون\"}");
        var copy = EventStreamCodec.parse(EventStreamCodec.serialize(e));
        assertEquals(e, copy);
        assertEquals(e.payloadJson(), copy.payloadJson());
        assertEquals(64, copy.payloadSha256().length());
    }

    @Test void payloadTamperIsRejected() {
        var e = event("EVT:002", "{\"value\":1}");
        var changed = new EventStreamEnvelope(e.schemaVersion(), e.eventId(), e.sourceDomain(), e.eventType(),
                e.aggregateReference(), e.occurredAt(), e.payloadSha256(), Base64.getEncoder().encodeToString("{\"value\":2}".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, changed::validate);
    }

    @Test void semanticKeyCollisionIsNotIdempotent() {
        assertTrue(EventStreamPolicy.sameEvent(event("EVT:003", "{\"a\":1}"), event("EVT:003", "{\"a\":1}")));
        assertFalse(EventStreamPolicy.sameEvent(event("EVT:003", "{\"a\":1}"), event("EVT:003", "{\"a\":2}")));
    }

    @Test void changedEventTypeWithSameDataChangesReceiptFingerprint() {
        var e = event("EVT:003", "{\"a\":1}");
        var differentType = new EventStreamEnvelope(e.schemaVersion(), e.eventId(), e.sourceDomain(),
                "ANOTHER_TEST", e.aggregateReference(), e.occurredAt(), e.payloadSha256(), e.payloadBase64());
        assertNotEquals(EventStreamPolicy.receiptFingerprint(e), EventStreamPolicy.receiptFingerprint(differentType));
    }

    @Test void rejectsMalformedIdentifiersAndOversizedPayload() {
        assertThrows(IllegalArgumentException.class, () -> event("", "{}"));
        assertThrows(IllegalArgumentException.class, () -> event("EVT:004", "{\"x\":\"" + "a".repeat(66000) + "\"}"));
        assertThrows(IllegalArgumentException.class, () -> EventStreamCodec.serialize(event("EVT:004", "not-json")));
    }

    @Test void malformedKafkaWireIsRejectedWithDomainException() {
        // Regression: Jackson 3 StreamReadException must not escape the codec boundary.
        var invalid = assertThrows(IllegalArgumentException.class, () -> EventStreamCodec.parse("not-json"));
        assertEquals("Invalid Kafka envelope JSON", invalid.getMessage());
        assertNull(invalid.getCause()); // Do not leak parser token details.
        assertThrows(IllegalArgumentException.class, () -> EventStreamCodec.parse("{\"eventId\":"));
    }

    @Test void malformedBusinessPayloadInReceivedEnvelopeIsRejected() {
        // Construct the wire independently of codec.serialize(), which must reject it.
        var invalid = event("EVT:006", "not-json");
        var wire = new tools.jackson.databind.json.JsonMapper().writeValueAsString(invalid);
        var failure = assertThrows(IllegalArgumentException.class, () -> EventStreamCodec.parse(wire));
        assertEquals("Invalid business event payload JSON", failure.getMessage());
        assertNull(failure.getCause()); // Never expose potentially sensitive JSON fragments.
    }

    @Test void failClosedRetryPolicy() {
        assertEquals(2L, EventStreamPolicy.retrySeconds(1));
        assertEquals(3600L, EventStreamPolicy.retrySeconds(22));
        assertFalse(EventStreamPolicy.exhausted(3, 4));
        assertTrue(EventStreamPolicy.exhausted(4, 4));
        assertThrows(IllegalArgumentException.class, () -> EventStreamPolicy.retrySeconds(0));
    }

    @Test void rejectsUnsupportedSchemaVersion() {
        var e = event("EVT:005", "{}");
        assertThrows(IllegalArgumentException.class, () -> new EventStreamEnvelope(9, e.eventId(), e.sourceDomain(),
                e.eventType(), e.aggregateReference(), e.occurredAt(), e.payloadSha256(), e.payloadBase64()).validate());
    }
}
