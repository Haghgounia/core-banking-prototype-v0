package com.behsazan.corebanking.posting.streaming;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Full wire-message round trip plus validation of the opaque business JSON. */
public final class EventStreamCodec {
    private static final JsonMapper MAPPER = new JsonMapper();
    private EventStreamCodec() { }

    public static String serialize(EventStreamEnvelope envelope) {
        validate(envelope);
        return MAPPER.writeValueAsString(envelope);
    }

    public static EventStreamEnvelope parse(String wireMessage) {
        if (wireMessage == null || wireMessage.length() > 150000)
            throw new IllegalArgumentException("Missing/oversized Kafka envelope");
        final EventStreamEnvelope envelope;
        try {
            envelope = MAPPER.readValue(wireMessage, EventStreamEnvelope.class);
        } catch (JacksonException malformedWire) {
            // Never propagate parser internals or untrusted wire content into error logs.
            throw new IllegalArgumentException("Invalid Kafka envelope JSON");
        }
        validate(envelope);
        return envelope;
    }

    private static void validate(EventStreamEnvelope envelope) {
        if (envelope == null) throw new IllegalArgumentException("Event is required");
        envelope.validate();
        final JsonNode payload;
        try {
            payload = MAPPER.readTree(envelope.payloadJson());
        } catch (JacksonException malformedPayload) {
            // A malformed business payload is an invalid event, not a raw Jackson failure.
            // Do not attach the cause: it can include sensitive JSON token fragments.
            throw new IllegalArgumentException("Invalid business event payload JSON");
        }
        if (payload == null || !payload.isObject())
            throw new IllegalArgumentException("Business event payload must be a JSON object");
    }
}
