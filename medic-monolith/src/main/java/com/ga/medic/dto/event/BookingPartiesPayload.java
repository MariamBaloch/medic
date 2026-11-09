package com.ga.medic.dto.event;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Patient/doctor fields included in booking and waitlist outbox payloads.
 */
public record BookingPartiesPayload(
        Long patientId,
        Long doctorId,
        Long patientUserId,
        Long doctorUserId,
        String patientName,
        String patientEmail,
        String doctorName,
        String doctorEmail
) {
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("patientId", patientId);
        map.put("doctorId", doctorId);
        map.put("patientUserId", patientUserId);
        map.put("doctorUserId", doctorUserId);
        map.put("patientName", patientName);
        map.put("patientEmail", patientEmail);
        map.put("doctorName", doctorName);
        map.put("doctorEmail", doctorEmail);
        return map;
    }
}
