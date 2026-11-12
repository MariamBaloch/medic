package com.ga.medic.identity.mapper;

import com.ga.medic.identity.model.DoctorProfile;
import com.ga.medic.identity.model.User;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class IdentityEventPayloadMapper {

    public Map<String, Object> userPayload(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", user.getId());
        map.put("email", user.getEmail());
        map.put("fullName", user.getFullName());
        map.put("status", user.getStatus() == null ? null : user.getStatus().name());
        map.put("role", user.getRole() == null || user.getRole().getName() == null
                ? null
                : user.getRole().getName().name());
        if (user.getPatientProfile() != null) {
            map.put("patientId", user.getPatientProfile().getId());
        }
        if (user.getDoctorProfile() != null) {
            map.put("doctorId", user.getDoctorProfile().getId());
        }
        return map;
    }

    public Map<String, Object> doctorPayload(DoctorProfile doctor) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("doctorId", doctor.getId());
        map.put("doctorUserId", doctor.getUser().getId());
        map.put("doctorName", doctor.getUser().getFullName());
        map.put("doctorEmail", doctor.getUser().getEmail());
        var specialization = doctor.getSpecialization();
        map.put("doctorSpecializationId", specialization == null ? null : specialization.getId());
        map.put("doctorSpecializationName", specialization == null ? null : specialization.getName());
        map.put("isVerified", doctor.getIsVerified());
        return map;
    }
}
