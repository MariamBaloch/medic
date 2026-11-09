package com.ga.medic.notification.kafka;

import com.ga.medic.common.event.EventEnvelope;
import com.ga.medic.common.event.TopicNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final BookingEventProcessor bookingEventProcessor;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TopicNames.BOOKING_APPOINTMENT_CHANGED, groupId = "notification-service")
    public void onAppointmentChanged(String payload) {
        process(payload);
    }

    @KafkaListener(topics = TopicNames.BOOKING_WAITLIST_CHANGED, groupId = "notification-service")
    public void onWaitlistChanged(String payload) {
        process(payload);
    }

    private void process(String payload) {
        EventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(payload, EventEnvelope.class);
        } catch (Exception exception) {
            log.error("Failed to deserialize booking notification event", exception);
            throw new IllegalArgumentException("Invalid EventEnvelope payload", exception);
        }
        bookingEventProcessor.process(envelope);
    }
}
