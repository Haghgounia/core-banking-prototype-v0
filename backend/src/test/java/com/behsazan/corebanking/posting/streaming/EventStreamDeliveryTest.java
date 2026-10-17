package com.behsazan.corebanking.posting.streaming;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Pure mocks: proves ack sequencing and fail-closed errors without contacting a broker. */
class EventStreamDeliveryTest {
    private static EventStreamEnvelope event() {
        return EventStreamEnvelope.create("ES1:MOCK:1", "PST", "STREAM_CONTRACT_TEST", "TEST:001", "{\"unit\":true}",
                Instant.parse("2026-10-17T00:00:00Z"));
    }

    @Test void brokerAckMustPrecedePublishedFlag() throws Exception {
        OracleEventStreamStore store = mock(OracleEventStreamStore.class);
        @SuppressWarnings("unchecked") KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        var envelope = event();
        String wire = EventStreamCodec.serialize(envelope);
        when(store.lease(eq(envelope.eventId()), anyString(), eq(60)))
                .thenReturn(Optional.of(new OracleEventStreamStore.Outbound(envelope.eventId(), envelope.eventType(),
                        envelope.aggregateReference(), envelope.payloadSha256(), wire, 1)));
        var waiting = new CompletableFuture<org.springframework.kafka.support.SendResult<String,String>>();
        var sendStarted = new CountDownLatch(1);
        when(kafka.send(eq("corebanking.pst.events.v1"), eq("TEST:001"), eq(wire)))
                .thenAnswer(invocation -> { sendStarted.countDown(); return waiting; });
        var publisher = new EventStreamPublisher(store, kafka, "corebanking.pst.events.v1", 5, 3, 4, 60);
        var sendingThread = Thread.ofVirtual().start(() -> publisher.sendOne(envelope.eventId()));
        assertTrue(sendStarted.await(2, TimeUnit.SECONDS));
        verify(store, never()).published(anyString(), anyString());
        waiting.complete(null); // Simulated broker ACK occurs strictly after the negative assertion.
        sendingThread.join(2000);
        assertFalse(sendingThread.isAlive());
        verify(store, times(1)).published(eq(envelope.eventId()), anyString());
        verify(store, never()).failed(anyString(), anyString(), anyInt(), anyInt(), anyString());
    }

    @Test void brokerFailureNeverMarksPublished() {
        OracleEventStreamStore store = mock(OracleEventStreamStore.class);
        @SuppressWarnings("unchecked") KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        var envelope = event();
        String wire = EventStreamCodec.serialize(envelope);
        when(store.lease(eq(envelope.eventId()), anyString(), eq(60)))
                .thenReturn(Optional.of(new OracleEventStreamStore.Outbound(envelope.eventId(), envelope.eventType(),
                        envelope.aggregateReference(), envelope.payloadSha256(), wire, 1)));
        var failed = new CompletableFuture<org.springframework.kafka.support.SendResult<String,String>>();
        failed.completeExceptionally(new IllegalStateException("synthetic broker failure"));
        when(kafka.send(eq("corebanking.pst.events.v1"), eq("TEST:001"), eq(wire))).thenReturn(failed);
        new EventStreamPublisher(store, kafka, "corebanking.pst.events.v1", 5, 3, 1, 60).sendOne(envelope.eventId());
        verify(store, never()).published(anyString(), anyString());
        verify(store, times(1)).failed(eq(envelope.eventId()), anyString(), eq(1), eq(3), anyString());
    }

    @Test void auditConsumerPersistsBeforeManualAck() {
        OracleEventStreamStore store = mock(OracleEventStreamStore.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        EventStreamEnvelope event = event();
        ConsumerRecord<String,String> record = new ConsumerRecord<>("corebanking.pst.events.v1", 0, 3L,
                event.aggregateReference(), EventStreamCodec.serialize(event));
        new EventStreamAuditConsumer(store, "corebanking.stream-audit.v1").audit(record, acknowledgment);
        var order = inOrder(store, acknowledgment);
        order.verify(store).receipt(eq(event), eq("corebanking.stream-audit.v1"));
        order.verify(acknowledgment).acknowledge();
    }

    @Test void conflictingKafkaKeyIsNotAcknowledged() {
        OracleEventStreamStore store = mock(OracleEventStreamStore.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        EventStreamEnvelope event = event();
        ConsumerRecord<String,String> record = new ConsumerRecord<>("corebanking.pst.events.v1", 0, 3L,
                "WRONG:ACCOUNT", EventStreamCodec.serialize(event));
        assertThrows(IllegalArgumentException.class,
                () -> new EventStreamAuditConsumer(store, "corebanking.stream-audit.v1").audit(record, acknowledgment));
        verifyNoInteractions(store, acknowledgment);
    }
}
