package com.ga.medic.messaging.kafka;

import com.ga.medic.common.event.TopicNames;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

/** Retries failed Kafka messages, then sends them to a dead-letter topic. */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "spring.kafka.consumer.group-id")
public class KafkaDltAutoConfiguration {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.dlt.retry-interval-ms:1000}") long retryIntervalMs,
            @Value("${app.kafka.dlt.max-retries:3}") long maxRetries
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> {
                    log.error("Kafka message processing failed after retries. Sending to DLT: topic={}, key={}",
                            record.topic(), record.key(), exception);
                    return new TopicPartition(TopicNames.deadLetter(record.topic()), record.partition());
                }
        );
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(retryIntervalMs, maxRetries)
        );
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                JacksonException.class
        );
        return errorHandler;
    }
}
