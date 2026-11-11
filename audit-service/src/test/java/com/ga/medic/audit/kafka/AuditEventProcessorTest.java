package com.ga.medic.audit.kafka;

import com.ga.medic.audit.enums.AuditAction;
import com.ga.medic.audit.enums.AuditEntityType;
import com.ga.medic.messaging.consumer.ProcessedEventRepository;
import com.ga.medic.audit.service.AuditLogService;
import com.ga.medic.common.event.EventEnvelope;
import com.ga.medic.common.event.EventTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventProcessorTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private AuditLogService auditLogService;
    @InjectMocks
    private AuditEventProcessor processor;

    @Test
    void processRecordsOnce() {
        EventEnvelope envelope = envelope("evt-1");
        when(processedEventRepository.existsById("evt-1")).thenReturn(false);

        processor.process(envelope);

        verify(auditLogService).record("Admin", AuditAction.UPDATE, AuditEntityType.USER, "desc");
        verify(processedEventRepository).save(any());
    }

    @Test
    void duplicateEventIdSkipped() {
        when(processedEventRepository.existsById("evt-1")).thenReturn(true);

        processor.process(envelope("evt-1"));

        verifyNoInteractions(auditLogService);
        verify(processedEventRepository, never()).save(any());
    }

    private static EventEnvelope envelope(String eventId) {
        return new EventEnvelope(
                eventId,
                EventTypes.AUDIT_EVENT_RECORDED,
                Instant.parse("2026-11-09T12:00:00Z"),
                "medic-monolith",
                "audit",
                "USER",
                "corr-1",
                Map.of(
                        "username", "Admin",
                        "action", "UPDATE",
                        "entityType", "USER",
                        "description", "desc"
                )
        );
    }
}
