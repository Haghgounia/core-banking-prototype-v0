package com.behsazan.corebanking.posting.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** One leased Oracle outbox event is sent to Kafka; marked published only after broker ack. */
@Component
@ConditionalOnProperty(prefix = "core-banking.event-stream", name = {"enabled", "producer-enabled"}, havingValue = "true")
public class EventStreamPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(EventStreamPublisher.class);
    private final OracleEventStreamStore store;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;
    private final int batchSize;
    private final int maxAttempts;
    private final int ackTimeoutSeconds;
    private final int leaseSeconds;

    public EventStreamPublisher(OracleEventStreamStore store, KafkaTemplate<String, String> kafka,
            @Value("${core-banking.event-stream.topic:corebanking.pst.events.v1}") String topic,
            @Value("${core-banking.event-stream.batch-size:20}") int batchSize,
            @Value("${core-banking.event-stream.max-attempts:12}") int maxAttempts,
            @Value("${core-banking.event-stream.ack-timeout-seconds:10}") int ackTimeoutSeconds,
            @Value("${core-banking.event-stream.lease-seconds:60}") int leaseSeconds) {
        if (topic == null || !topic.matches("[A-Za-z0-9._-]{1,160}"))
            throw new IllegalArgumentException("Invalid approved Kafka topic");
        if (batchSize < 1 || batchSize > 100 || maxAttempts < 1 || maxAttempts > 100
                || ackTimeoutSeconds < 1 || ackTimeoutSeconds > 120
                || leaseSeconds < Math.max(20, ackTimeoutSeconds + 10) || leaseSeconds > 900)
            throw new IllegalArgumentException("Unsafe event streaming retry/lease configuration");
        this.store = Objects.requireNonNull(store);
        this.kafka = Objects.requireNonNull(kafka);
        this.topic = topic;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.ackTimeoutSeconds = ackTimeoutSeconds;
        this.leaseSeconds = leaseSeconds;
    }

    @Scheduled(fixedDelayString = "${core-banking.event-stream.poll-ms:5000}")
    public void publishBatch() {
        try {
            for (String eventId : store.eligible(batchSize)) {
                if (Thread.currentThread().isInterrupted()) break;
                sendOne(eventId);
            }
        } catch (RuntimeException failure) {
            LOG.error("Event-stream outbox polling failed (no financial posting performed): {}",
                    failure.getClass().getSimpleName());
        }
    }

    void sendOne(String eventId) {
        String leaseToken = UUID.randomUUID().toString();
        var maybe = store.lease(eventId, leaseToken, leaseSeconds);
        if (maybe.isEmpty()) return; // Another dispatcher owns the lease.
        var row = maybe.get();
        try {
            EventStreamEnvelope envelope = EventStreamCodec.parse(row.wirePayload());
            if (!row.eventId().equals(envelope.eventId())
                    || !row.eventType().equals(envelope.eventType())
                    || !row.aggregateReference().equals(envelope.aggregateReference())
                    || !row.payloadSha256().equals(envelope.payloadSha256()))
                throw new IllegalArgumentException("Persisted outbox header/envelope mismatch");
            // Kafka producer MUST use acks=all + enable.idempotence=true (reviewed external configuration).
            kafka.send(topic, envelope.aggregateReference(), row.wirePayload())
                    .get(ackTimeoutSeconds, TimeUnit.SECONDS);
            store.published(eventId, leaseToken);
        } catch (InterruptedException interrupted) {
            recordFailure(eventId, leaseToken, row.attempts(), interrupted);
            Thread.currentThread().interrupt();
        } catch (Exception failure) {
            recordFailure(eventId, leaseToken, row.attempts(), failure);
        }
    }

    private void recordFailure(String eventId, String token, int attempts, Exception failure) {
        LOG.error("Event-stream send failed, eventId={}, attempt={}, reason={} (payload suppressed)",
                eventId, attempts, failure.getClass().getSimpleName());
        try {
            store.failed(eventId, token, attempts, maxAttempts, failure.getClass().getSimpleName());
        } catch (RuntimeException databaseFailure) {
            // Expired/lost leases will be reclaimed, and duplicate Kafka deliveries remain possible.
            LOG.error("Unable to persist outbox retry status: {}", databaseFailure.getClass().getSimpleName());
        }
    }
}
