package com.ga.medic.booking.kafka;

import com.ga.medic.booking.repository.AppointmentRepository;
import com.ga.medic.booking.repository.AppointmentWaitlistEntryRepository;
import com.ga.medic.common.event.EventEnvelope;
import com.ga.medic.common.event.EventTypes;
import com.ga.medic.messaging.consumer.ProcessedEvent;
import com.ga.medic.messaging.consumer.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Applies identity change events to denormalized appointment and waitlist snapshots.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.processed-event.enabled", havingValue = "true")
public class IdentityEventProcessor {

    private final ProcessedEventRepository processedEventRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentWaitlistEntryRepository waitlistEntryRepository;

    @Transactional
    public void process(EventEnvelope event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("Event envelope missing eventId");
        }
        if (processedEventRepository.existsById(event.eventId())) {
            log.debug("Skipping already processed identity eventId={}", event.eventId());
            return;
        }

        Map<String, Object> payload = event.payload() == null ? Map.of() : event.payload();
        String eventType = event.eventType();
        if (eventType != null && eventType.startsWith("identity.user.")) {
            refreshFromUserChanged(payload);
        } else if (eventType != null && eventType.startsWith("identity.doctor.")) {
            refreshFromDoctorChanged(payload);
        } else {
            throw new IllegalArgumentException("Unsupported identity eventType=" + eventType);
        }

        ProcessedEvent processed = new ProcessedEvent();
        processed.setEventId(event.eventId());
        processed.setProcessedAt(Instant.now());
        processedEventRepository.save(processed);
    }

    private void refreshFromUserChanged(Map<String, Object> payload) {
        Long userId = longValue(payload.get("userId"));
        String name = stringValue(payload.get("fullName"));
        String email = stringValue(payload.get("email"));
        if (userId == null || name == null || name.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("identity.user payload missing userId/fullName/email");
        }

        int appointmentPatient = appointmentRepository.updatePatientSnapshotByUserId(userId, name, email);
        int appointmentDoctor = appointmentRepository.updateDoctorSnapshotByUserId(userId, name, email);
        int waitlistPatient = waitlistEntryRepository.updatePatientSnapshotByUserId(userId, name, email);
        int waitlistDoctor = waitlistEntryRepository.updateDoctorSnapshotByUserId(userId, name, email);
        log.info(
                "Refreshed booking snapshots from {}: userId={}, appointmentPatient={}, appointmentDoctor={}, waitlistPatient={}, waitlistDoctor={}",
                EventTypes.IDENTITY_USER_UPDATED,
                userId,
                appointmentPatient,
                appointmentDoctor,
                waitlistPatient,
                waitlistDoctor
        );
    }

    private void refreshFromDoctorChanged(Map<String, Object> payload) {
        Long doctorId = longValue(payload.get("doctorId"));
        String name = stringValue(payload.get("doctorName"));
        String email = stringValue(payload.get("doctorEmail"));
        Long specializationId = longValue(payload.get("doctorSpecializationId"));
        String specializationName = stringValue(payload.get("doctorSpecializationName"));
        if (doctorId == null || name == null || name.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("identity.doctor payload missing doctorId/doctorName/doctorEmail");
        }

        int appointments = appointmentRepository.updateDoctorSnapshotByDoctorId(
                doctorId, name, email, specializationId, specializationName);
        int waitlist = waitlistEntryRepository.updateDoctorSnapshotByDoctorId(
                doctorId, name, email, specializationId, specializationName);
        log.info(
                "Refreshed booking doctor snapshots from {}: doctorId={}, appointments={}, waitlist={}",
                EventTypes.IDENTITY_DOCTOR_UPDATED,
                doctorId,
                appointments,
                waitlist
        );
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return Long.parseLong(text);
    }
}
