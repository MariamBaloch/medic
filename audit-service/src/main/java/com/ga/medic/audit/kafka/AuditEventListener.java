package com.ga.medic.audit.kafka;

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
public class AuditEventListener {

    private final AuditEventProcessor auditEventProcessor;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TopicNames.AUDIT_EVENT_RECORDED, groupId = "audit-service")
    public void onAuditEvent(String payload) {
        EventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(payload, EventEnvelope.class);
        } catch (Exception exception) {
            log.error("Failed to deserialize audit event", exception);
            throw new IllegalArgumentException("Invalid EventEnvelope payload", exception);
        }
        auditEventProcessor.process(envelope);
    }
}
