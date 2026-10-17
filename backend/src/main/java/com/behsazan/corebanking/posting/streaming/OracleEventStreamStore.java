package com.behsazan.corebanking.posting.streaming;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.StringReader;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Uses the existing PST outbox and inbox tables.  The additive columns are
 * documented in the review-only SQL; nothing migrates automatically.
 * Financial services are NOT hooked to this component until bank approval.
 */
@Repository
@ConditionalOnProperty(prefix = "core-banking.event-stream", name = "enabled", havingValue = "true")
public class OracleEventStreamStore {
    private final JdbcTemplate jdbc;

    public record Outbound(String eventId, String eventType, String aggregateReference,
                           String payloadSha256, String wirePayload, int attempts) { }

    public OracleEventStreamStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Must be called INSIDE the same existing Oracle transaction as the domain mutation. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean enqueue(String eventId, String sourceDomain, String eventType,
                           String aggregateReference, String jsonPayload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw new IllegalStateException("Outbox insertion requires an existing writable business transaction");
        }
        EventStreamEnvelope event = EventStreamEnvelope.create(eventId, sourceDomain, eventType,
                aggregateReference, jsonPayload, Instant.now());
        String wire = EventStreamCodec.serialize(event);
        try {
            jdbc.update(connection -> {
                var sql = "INSERT INTO PST.POSTING_OUTBOX_EVENTS "
                        + "(EVENT_IDENTIFIER, AGGREGATE_REFERENCE, EVENT_TYPE, PAYLOAD_HASH, DELIVERY_STATE, "
                        + "CREATED_AT, STREAM_PAYLOAD, ATTEMPT_COUNT, NEXT_ATTEMPT_AT) "
                        + "VALUES (?,?,?,?, 'PENDING', SYSTIMESTAMP, ?, 0, SYSTIMESTAMP)";
                var statement = connection.prepareStatement(sql);
                statement.setString(1, event.eventId());
                statement.setString(2, event.aggregateReference());
                statement.setString(3, event.eventType());
                statement.setString(4, event.payloadSha256());
                statement.setCharacterStream(5, new StringReader(wire), wire.length());
                return statement;
            });
            return true;
        } catch (DataIntegrityViolationException collision) {
            List<String> existing = jdbc.query(
                    "SELECT STREAM_PAYLOAD FROM PST.POSTING_OUTBOX_EVENTS WHERE EVENT_IDENTIFIER = ?",
                    (rs, row) -> rs.getString(1), event.eventId());
            if (existing.size() == 1 && existing.getFirst() != null
                    && EventStreamPolicy.sameEvent(EventStreamCodec.parse(existing.getFirst()), event)) {
                return false;  // Safe idempotent replay within a valid transaction.
            }
            throw new IllegalStateException("Event ID reused with conflicting payload or metadata", collision);
        }
    }

    @Transactional(readOnly = true)
    public List<String> eligible(int batchSize) {
        if (batchSize < 1 || batchSize > 100) throw new IllegalArgumentException("Batch limit 1..100");
        return jdbc.query("SELECT O.EVENT_IDENTIFIER FROM PST.POSTING_OUTBOX_EVENTS O "
                        + "WHERE O.STREAM_PAYLOAD IS NOT NULL AND ("
                        + "(O.DELIVERY_STATE IN ('PENDING','RETRY') AND "
                        + "(O.NEXT_ATTEMPT_AT IS NULL OR O.NEXT_ATTEMPT_AT <= SYSTIMESTAMP) "
                        + "AND (O.LEASE_UNTIL IS NULL OR O.LEASE_UNTIL <= SYSTIMESTAMP)) "
                        + "OR (O.DELIVERY_STATE = 'IN_FLIGHT' AND O.LEASE_UNTIL <= SYSTIMESTAMP)) "
                        + "AND NOT EXISTS (SELECT 1 FROM PST.POSTING_OUTBOX_EVENTS PREVIOUS "
                        + "WHERE PREVIOUS.AGGREGATE_REFERENCE=O.AGGREGATE_REFERENCE "
                        + "AND PREVIOUS.IDENTIFIER<O.IDENTIFIER AND PREVIOUS.STREAM_PAYLOAD IS NOT NULL "
                        + "AND (PREVIOUS.DELIVERY_STATE IS NULL OR PREVIOUS.DELIVERY_STATE<>'PUBLISHED')) "
                        + "ORDER BY O.IDENTIFIER FETCH FIRST ? ROWS ONLY",
                (rs, index) -> rs.getString(1), batchSize);
    }

    /** One short Oracle transaction obtains a conditional lease. No DB lock held during Kafka send. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Outbound> lease(String eventId, String leaseToken, int leaseSeconds) {
        if (leaseSeconds < 20 || leaseSeconds > 900) throw new IllegalArgumentException("Invalid lease seconds");
        int count = jdbc.update("UPDATE PST.POSTING_OUTBOX_EVENTS O SET DELIVERY_STATE='IN_FLIGHT', "
                        + "LEASE_TOKEN=?, LEASE_UNTIL=SYSTIMESTAMP + NUMTODSINTERVAL(?,'SECOND'), "
                        + "ATTEMPT_COUNT=NVL(ATTEMPT_COUNT,0)+1 WHERE EVENT_IDENTIFIER=? "
                        + "AND STREAM_PAYLOAD IS NOT NULL AND ("
                        + "(DELIVERY_STATE IN ('PENDING','RETRY') "
                        + "AND (NEXT_ATTEMPT_AT IS NULL OR NEXT_ATTEMPT_AT <= SYSTIMESTAMP) "
                        + "AND (LEASE_UNTIL IS NULL OR LEASE_UNTIL <= SYSTIMESTAMP)) "
                        + "OR (DELIVERY_STATE='IN_FLIGHT' AND LEASE_UNTIL <= SYSTIMESTAMP)) "
                        + "AND NOT EXISTS (SELECT 1 FROM PST.POSTING_OUTBOX_EVENTS PREVIOUS "
                        + "WHERE PREVIOUS.AGGREGATE_REFERENCE=O.AGGREGATE_REFERENCE "
                        + "AND PREVIOUS.IDENTIFIER<O.IDENTIFIER AND PREVIOUS.STREAM_PAYLOAD IS NOT NULL "
                        + "AND (PREVIOUS.DELIVERY_STATE IS NULL OR PREVIOUS.DELIVERY_STATE<>'PUBLISHED'))",
                leaseToken, leaseSeconds, eventId);
        if (count == 0) return Optional.empty();
        if (count != 1) throw new IllegalStateException("Outbox lease has non-unique event identifier");
        return jdbc.query("SELECT EVENT_IDENTIFIER, EVENT_TYPE, AGGREGATE_REFERENCE, PAYLOAD_HASH, "
                        + "STREAM_PAYLOAD, ATTEMPT_COUNT FROM PST.POSTING_OUTBOX_EVENTS "
                        + "WHERE EVENT_IDENTIFIER=? AND LEASE_TOKEN=?",
                (rs, i) -> new Outbound(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getInt(6)), eventId, leaseToken)
                .stream().findFirst();
    }

    /** Called only after Kafka broker acknowledgment; a lost acknowledgment may cause safe redelivery. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void published(String eventId, String leaseToken) {
        int changed = jdbc.update("UPDATE PST.POSTING_OUTBOX_EVENTS "
                        + "SET DELIVERY_STATE='PUBLISHED', PUBLISHED_AT=SYSTIMESTAMP, "
                        + "LEASE_TOKEN=NULL, LEASE_UNTIL=NULL, LAST_ERROR=NULL "
                        + "WHERE EVENT_IDENTIFIER=? AND DELIVERY_STATE='IN_FLIGHT' AND LEASE_TOKEN=? "
                        + "AND LEASE_UNTIL > SYSTIMESTAMP", eventId, leaseToken);
        if (changed != 1) throw new IllegalStateException("Outbox lease lost before publish acknowledgment persisted");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(String eventId, String leaseToken, int attempts, int maximum, String failureClass) {
        boolean stop = EventStreamPolicy.exhausted(attempts, maximum);
        String state = stop ? "QUARANTINED" : "RETRY";
        long delay = stop ? 0 : EventStreamPolicy.retrySeconds(attempts);
        String message = failureClass == null ? "UnknownFailure" : failureClass.replaceAll("[^A-Za-z0-9_.-]", "_");
        if (message.length() > 200) message = message.substring(0, 200);
        int updated = jdbc.update("UPDATE PST.POSTING_OUTBOX_EVENTS SET DELIVERY_STATE=?, "
                        + "NEXT_ATTEMPT_AT=SYSTIMESTAMP+NUMTODSINTERVAL(?,'SECOND'), "
                        + "LAST_ERROR=?, LEASE_TOKEN=NULL, LEASE_UNTIL=NULL "
                        + "WHERE EVENT_IDENTIFIER=? AND DELIVERY_STATE='IN_FLIGHT' AND LEASE_TOKEN=? "
                        + "AND LEASE_UNTIL>SYSTIMESTAMP",
                state, delay, message, eventId, leaseToken);
        if (updated != 1) throw new IllegalStateException("Outbox failure lease expired or transferred");
    }

    /** Audit-only Kafka consumer. It does not create accounting facts, postings or balances. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean receipt(EventStreamEnvelope event, String consumerGroup) {
        event.validate();
        if (consumerGroup == null || consumerGroup.isBlank() || consumerGroup.length() > 140)
            throw new IllegalArgumentException("Invalid consumer group");
        String purpose = "STREAM_AUDIT:" + consumerGroup;
        String fingerprint = EventStreamPolicy.receiptFingerprint(event);
        try {
            jdbc.update("INSERT INTO PST.POSTING_INBOX_RECEIPTS "
                            + "(SOURCE_DOMAIN, SOURCE_EVENT_IDENTIFIER, ACCOUNTING_PURPOSE, PAYLOAD_HASH, RECEIVED_AT) "
                            + "VALUES(?,?,?,?,SYSTIMESTAMP)",
                    event.sourceDomain(), event.eventId(), purpose, fingerprint);
            return true;
        } catch (DataIntegrityViolationException collision) {
            List<String> hashes = jdbc.query("SELECT PAYLOAD_HASH FROM PST.POSTING_INBOX_RECEIPTS "
                            + "WHERE SOURCE_DOMAIN=? AND SOURCE_EVENT_IDENTIFIER=? AND ACCOUNTING_PURPOSE=?",
                    (rs, i) -> rs.getString(1), event.sourceDomain(), event.eventId(), purpose);
            if (hashes.size() == 1 && fingerprint.equals(hashes.getFirst())) return false;
            throw new IllegalStateException("Kafka event ID reused with a conflicting hash", collision);
        }
    }
}
