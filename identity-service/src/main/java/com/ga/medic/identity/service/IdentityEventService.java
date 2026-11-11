package com.ga.medic.identity.service;

import com.ga.medic.common.event.EventTypes;
import com.ga.medic.identity.mapper.IdentityEventPayloadMapper;
import com.ga.medic.identity.model.DoctorProfile;
import com.ga.medic.identity.model.User;
import com.ga.medic.messaging.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdentityEventService {

    private final OutboxService outboxService;
    private final IdentityEventPayloadMapper payloadMapper;

    public void userCreated(User user) {
        emit(EventTypes.IDENTITY_USER_CREATED, "user", String.valueOf(user.getId()), payloadMapper.userPayload(user));
    }

    public void userUpdated(User user) {
        emit(EventTypes.IDENTITY_USER_UPDATED, "user", String.valueOf(user.getId()), payloadMapper.userPayload(user));
    }

    public void userDeactivated(User user) {
        emit(EventTypes.IDENTITY_USER_DEACTIVATED, "user", String.valueOf(user.getId()), payloadMapper.userPayload(user));
    }

    public void doctorCreated(DoctorProfile doctor) {
        emit(EventTypes.IDENTITY_DOCTOR_CREATED, "doctor", String.valueOf(doctor.getId()), payloadMapper.doctorPayload(doctor));
    }

    public void doctorUpdated(DoctorProfile doctor) {
        emit(EventTypes.IDENTITY_DOCTOR_UPDATED, "doctor", String.valueOf(doctor.getId()), payloadMapper.doctorPayload(doctor));
    }

    public void doctorVerified(DoctorProfile doctor) {
        emit(EventTypes.IDENTITY_DOCTOR_VERIFIED, "doctor", String.valueOf(doctor.getId()), payloadMapper.doctorPayload(doctor));
    }

    public void doctorDeactivated(DoctorProfile doctor) {
        emit(EventTypes.IDENTITY_DOCTOR_DEACTIVATED, "doctor", String.valueOf(doctor.getId()), payloadMapper.doctorPayload(doctor));
    }

    private void emit(String eventType, String aggregateType, String aggregateId, java.util.Map<String, Object> payload) {
        outboxService.append(eventType, aggregateType, aggregateId, payload);
    }
}
