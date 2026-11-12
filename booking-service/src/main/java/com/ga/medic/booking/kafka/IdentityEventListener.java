package com.ga.medic.booking.kafka;

import com.ga.medic.common.event.EventEnvelope;
import com.ga.medic.common.event.TopicNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumes identity change topics and refreshes booking snapshots.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.processed-event.enabled", havingValue = "true")
public class IdentityEventListener {

    private final IdentityEventProcessor identityEventProcessor;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TopicNames.IDENTITY_USER_CHANGED, groupId = "booking-service")
    public void onUserChanged(String payload) {
        process(payload);
    }

    @KafkaListener(topics = TopicNames.IDENTITY_DOCTOR_CHANGED, groupId = "booking-service")
    public void onDoctorChanged(String payload) {
        process(payload);
    }

    private void process(String payload) {
        EventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(payload, EventEnvelope.class);
        } catch (Exception exception) {
            log.error("Failed to deserialize identity event", exception);
            throw new IllegalArgumentException("Invalid EventEnvelope payload", exception);
        }
        identityEventProcessor.process(envelope);
    }
}
