package com.ga.medic.audit.kafka;

import com.ga.medic.audit.enums.AuditAction;
import com.ga.medic.audit.enums.AuditEntityType;
import com.ga.medic.messaging.consumer.ProcessedEvent;
import com.ga.medic.messaging.consumer.ProcessedEventRepository;
import com.ga.medic.audit.service.AuditLogService;
import com.ga.medic.common.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventProcessor {

    private final ProcessedEventRepository processedEventRepository;
    private final AuditLogService auditLogService;

    private static String string(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown " + type.getSimpleName() + "=" + raw, exception);
        }
    }

    @Transactional
    public void process(EventEnvelope envelope) {
        if (envelope == null || envelope.eventId() == null || envelope.eventId().isBlank()) {
            throw new IllegalArgumentException("Event envelope missing eventId");
        }
        if (processedEventRepository.existsById(envelope.eventId())) {
            log.debug("Skipping already processed audit eventId={}", envelope.eventId());
            return;
        }

        Map<String, Object> payload = envelope.payload() == null ? Map.of() : envelope.payload();
        String username = string(payload, "username");
        AuditAction action = enumValue(AuditAction.class, string(payload, "action"));
        AuditEntityType entityType = enumValue(AuditEntityType.class, string(payload, "entityType"));
        String description = string(payload, "description");

        if (username.isBlank() || action == null || entityType == null || description.isBlank()) {
            throw new IllegalArgumentException("Audit payload missing required fields for eventId=" + envelope.eventId());
        }

        auditLogService.record(username, action, entityType, description);

        ProcessedEvent processed = new ProcessedEvent();
        processed.setEventId(envelope.eventId());
        processed.setProcessedAt(Instant.now());
        processedEventRepository.save(processed);
    }
}
