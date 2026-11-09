package com.ga.medic.notification.kafka;

import com.ga.medic.common.event.EventEnvelope;
import com.ga.medic.common.event.EventTypes;
import com.ga.medic.notification.enums.NotificationAction;
import com.ga.medic.notification.enums.NotificationType;
import com.ga.medic.notification.model.ProcessedEvent;
import com.ga.medic.notification.repository.ProcessedEventRepository;
import com.ga.medic.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingEventProcessorTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private BookingNotificationFactory notificationFactory;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private BookingEventProcessor processor;

    @Test
    void processCreatesNotificationsAndMarksEvent() {
        EventEnvelope envelope = envelope("evt-dup-1");
        NotificationDraft draft = new NotificationDraft(
                20L,
                "doc@example.com",
                "Dr Doc",
                NotificationType.APPOINTMENT,
                NotificationAction.BOOKED,
                "New Appointment Booking",
                "message",
                99L
        );
        when(processedEventRepository.existsById("evt-dup-1")).thenReturn(false);
        when(notificationFactory.draftsFor(envelope)).thenReturn(List.of(draft));

        processor.process(envelope);

        verify(notificationService).createAndSend(
                eq(20L),
                eq("doc@example.com"),
                eq("Dr Doc"),
                eq(NotificationType.APPOINTMENT),
                eq(NotificationAction.BOOKED),
                eq("New Appointment Booking"),
                eq("message"),
                eq(false),
                eq(99L)
        );
        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo("evt-dup-1");
        assertThat(captor.getValue().getProcessedAt()).isNotNull();
    }

    @Test
    void duplicateEventIdIsSkipped() {
        EventEnvelope envelope = envelope("evt-dup-2");
        when(processedEventRepository.existsById("evt-dup-2")).thenReturn(true);

        processor.process(envelope);

        verifyNoInteractions(notificationFactory, notificationService);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void missingEventIdFails() {
        assertThatThrownBy(() -> processor.process(envelope("  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventId");
        assertThatThrownBy(() -> processor.process(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static EventEnvelope envelope(String eventId) {
        return new EventEnvelope(
                eventId,
                EventTypes.BOOKING_APPOINTMENT_CREATED,
                Instant.parse("2026-11-08T12:00:00Z"),
                "medic-monolith",
                "appointment",
                "99",
                "corr-1",
                Map.of("appointmentId", 99)
        );
    }
}
