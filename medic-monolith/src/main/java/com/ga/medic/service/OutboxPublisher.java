package com.ga.medic.service;

import com.ga.medic.common.api.CorrelationId;
import com.ga.medic.common.event.TopicNames;
import com.ga.medic.model.OutboxEvent;
import com.ga.medic.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.outbox.publisher-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:5000}")
    @Transactional
    public void publishBatch() {
        List<OutboxEvent> batch = outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        if (batch.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        for (OutboxEvent event : batch) {
            event.setAttempts(event.getAttempts() + 1);
            try {
                String topic = TopicNames.forEventType(event.getEventType());
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getAggregateId(), event.getPayload());

                if (event.getCorrelationId() != null) {
                    record.headers().add(new RecordHeader(
                            CorrelationId.HEADER,
                            event.getCorrelationId().getBytes(StandardCharsets.UTF_8)
                    ));
                }
                kafkaTemplate.send(record).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                event.setPublishedAt(now);

                log.info("Published outbox event: eventId={}, topic={}, type={}, aggregate={}/{}, correlationId={}",
                        event.getEventId(),
                        topic,
                        event.getEventType(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        event.getCorrelationId());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while publishing outbox event: eventId={}, type={}, attempts={}",
                        event.getEventId(),
                        event.getEventType(),
                        event.getAttempts(),
                        exception);
            } catch (Exception exception) {
                log.error("Failed to publish outbox event: eventId={}, type={}, attempts={}",
                        event.getEventId(),
                        event.getEventType(),
                        event.getAttempts(),
                        exception);
            }
        }
    }
}
