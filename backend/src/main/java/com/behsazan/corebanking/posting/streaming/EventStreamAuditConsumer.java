package com.behsazan.corebanking.posting.streaming;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * The consumer is intentionally AUDIT-ONLY: it stores a durable de-dup receipt,
 * not a GL posting, balance mutation, or posting instruction.
 */
@Component
@ConditionalOnProperty(prefix = "core-banking.event-stream", name = {"enabled", "consumer-enabled"}, havingValue = "true")
public class EventStreamAuditConsumer {
    private final OracleEventStreamStore store;
    private final String groupId;

    public EventStreamAuditConsumer(OracleEventStreamStore store,
            @Value("${core-banking.event-stream.consumer-group:corebanking.stream-audit.v1}") String groupId) {
        this.store = Objects.requireNonNull(store);
        if (groupId == null || !groupId.matches("[A-Za-z0-9._-]{1,140}"))
            throw new IllegalArgumentException("Invalid Kafka consumer group");
        this.groupId = groupId;
    }

    @KafkaListener(topics = "${core-banking.event-stream.topic:corebanking.pst.events.v1}",
                   groupId = "${core-banking.event-stream.consumer-group:corebanking.stream-audit.v1}",
                   containerFactory = "eventStreamListenerContainerFactory")
    public void audit(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        EventStreamEnvelope event = EventStreamCodec.parse(record.value());
        if (!Objects.equals(record.key(), event.aggregateReference()))
            throw new IllegalArgumentException("Kafka partition key does not match aggregate identity");
        // One committed Oracle receipt (or matching duplicate) must precede Kafka offset acknowledgement.
        store.receipt(event, groupId);
        acknowledgment.acknowledge();
    }
}
