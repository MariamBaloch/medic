package com.ga.medic.booking.kafka;

import com.ga.medic.booking.repository.AppointmentRepository;
import com.ga.medic.booking.repository.AppointmentWaitlistEntryRepository;
import com.ga.medic.common.event.EventEnvelope;
import com.ga.medic.common.event.EventTypes;
import com.ga.medic.messaging.consumer.ProcessedEvent;
import com.ga.medic.messaging.consumer.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityEventProcessorTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentWaitlistEntryRepository waitlistEntryRepository;

    @InjectMocks
    private IdentityEventProcessor processor;

    @Test
    void refreshesPatientAndDoctorSnapshotsFromUserChanged() {
        when(processedEventRepository.existsById("evt-1")).thenReturn(false);

        processor.process(new EventEnvelope(
                "evt-1",
                EventTypes.IDENTITY_USER_UPDATED,
                Instant.now(),
                "identity-service",
                "user",
                "42",
                null,
                Map.of(
                        "userId", 42,
                        "fullName", "Ada Lovelace",
                        "email", "ada@medic.com"
                )
        ));

        verify(appointmentRepository).updatePatientSnapshotByUserId(42L, "Ada Lovelace", "ada@medic.com");
        verify(appointmentRepository).updateDoctorSnapshotByUserId(42L, "Ada Lovelace", "ada@medic.com");
        verify(waitlistEntryRepository).updatePatientSnapshotByUserId(42L, "Ada Lovelace", "ada@medic.com");
        verify(waitlistEntryRepository).updateDoctorSnapshotByUserId(42L, "Ada Lovelace", "ada@medic.com");

        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(captor.capture());
        assertEquals("evt-1", captor.getValue().getEventId());
    }

    @Test
    void refreshesDoctorSnapshotsFromDoctorChanged() {
        when(processedEventRepository.existsById("evt-2")).thenReturn(false);

        processor.process(new EventEnvelope(
                "evt-2",
                EventTypes.IDENTITY_DOCTOR_UPDATED,
                Instant.now(),
                "identity-service",
                "doctor",
                "7",
                null,
                Map.of(
                        "doctorId", 7,
                        "doctorName", "Dr House",
                        "doctorEmail", "house@medic.com",
                        "doctorSpecializationId", 3
                )
        ));

        verify(appointmentRepository).updateDoctorSnapshotByDoctorId(eq(7L), eq("Dr House"), eq("house@medic.com"), eq(3L));
        verify(waitlistEntryRepository).updateDoctorSnapshotByDoctorId(eq(7L), eq("Dr House"), eq("house@medic.com"), eq(3L));
    }
}
