package com.ga.medic.messaging.outbox;

import com.ga.medic.common.api.CorrelationId;
import com.ga.medic.common.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    @Value("${spring.application.name}")
    private String producer;

    public static Map<String, Object> payload(Object... keyValues) {
        return payload(null, keyValues);
    }

    public static Map<String, Object> payload(Map<String, Object> base, Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Outbox payload requires an even number of key/value arguments");
        }
        Map<String, Object> payload = base == null ? new LinkedHashMap<>() : new LinkedHashMap<>(base);
        for (int i = 0; i < keyValues.length; i += 2) {
            payload.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return payload;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String eventType, String aggregateType, String aggregateId, Map<String, Object> payload) {
        save(eventType, aggregateType, aggregateId, payload);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void appendIndependent(String eventType, String aggregateType, String aggregateId, Map<String, Object> payload) {
        save(eventType, aggregateType, aggregateId, payload);
    }

    private void save(String eventType, String aggregateType, String aggregateId, Map<String, Object> payload) {
        Instant now = Instant.now();
        String eventId = UUID.randomUUID().toString();
        String correlationId = MDC.get(CorrelationId.MDC_KEY);

        EventEnvelope envelope = new EventEnvelope(
                eventId,
                eventType,
                now,
                producer,
                aggregateType,
                aggregateId,
                correlationId,
                payload == null ? Map.of() : payload
        );

        OutboxEvent event = new OutboxEvent();
        event.setEventId(eventId);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(writeJson(envelope));
        event.setCorrelationId(correlationId);
        event.setCreatedAt(now);
        event.setAttempts(0);
        outboxEventRepository.save(event);
    }

    private String writeJson(EventEnvelope envelope) {
        try {
            return jsonMapper.writeValueAsString(envelope);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize outbox event " + envelope.eventType(), exception);
        }
    }
}
