package com.ga.medic.common.event;

import java.time.Instant;
import java.util.Map;

/**
 * Standard wrapper for events published between services.
 */
public record EventEnvelope(
        String eventId,
        String eventType,
        Instant occurredAt,
        String producer,
        String aggregateType,
        String aggregateId,
        String correlationId,
        Map<String, Object> payload
) {
}
