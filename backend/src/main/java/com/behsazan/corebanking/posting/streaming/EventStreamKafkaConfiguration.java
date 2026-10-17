package com.behsazan.corebanking.posting.streaming;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;

/** All message traffic is opt-in. No local topic creation, no automatic schema mutation. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "core-banking.event-stream", name = "enabled", havingValue = "true")
public class EventStreamKafkaConfiguration {
    @Bean("eventStreamProducerSafetyGate")
    @ConditionalOnProperty(prefix = "core-banking.event-stream", name = "producer-enabled", havingValue = "true")
    public SmartInitializingSingleton producerSafetyGate(KafkaProperties properties) {
        return () -> {
            var p = properties.buildProducerProperties();
            String acknowledgments = String.valueOf(p.get(ProducerConfig.ACKS_CONFIG));
            String idempotence = String.valueOf(p.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));
            String inFlight = String.valueOf(p.get(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION));
            if (!("all".equalsIgnoreCase(acknowledgments) || "-1".equals(acknowledgments))
                    || !"true".equalsIgnoreCase(idempotence))
                throw new IllegalStateException("Event streaming producer requires explicit acks=all and enable.idempotence=true");
            try {
                int allowed = Integer.parseInt(inFlight);
                if (allowed < 1 || allowed > 5) throw new IllegalStateException("Unsafe Kafka max.in.flight.requests.per.connection");
            } catch (NumberFormatException exception) {
                throw new IllegalStateException("Kafka producer max.in.flight.requests.per.connection must be explicit", exception);
            }
        };
    }
    @Bean("eventStreamListenerContainerFactory")
    @ConditionalOnProperty(prefix = "core-banking.event-stream", name = "consumer-enabled", havingValue = "true")
    public ConcurrentKafkaListenerContainerFactory<String, String> auditOnlyListenerFactory(KafkaProperties properties) {
        var config = new HashMap<>(properties.buildConsumerProperties());
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(config));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        // No default recoverer is allowed to skip an invalid accounting-related event.
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new FixedBackOff(5000L, FixedBackOff.UNLIMITED_ATTEMPTS)));
        return factory;
    }
}
