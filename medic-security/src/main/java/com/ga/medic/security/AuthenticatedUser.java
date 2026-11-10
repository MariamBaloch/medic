package com.ga.medic.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUser {

    public UserPrincipal principal() {
        return (UserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    public Long getUserId() {
        return principal().userId();
    }

    public Long getPatientId() {
        return principal().patientId();
    }

    public Long getDoctorId() {
        return principal().doctorId();
    }

    public String getName() {
        return principal().name();
    }

    public String getEmail() {
        return principal().email();
    }

    public Long requirePatientId() {
        Long patientId = getPatientId();
        if (patientId == null) {
            throw new IllegalStateException("Authenticated user has no patientId claim");
        }
        return patientId;
    }

    public Long requireDoctorId() {
        Long doctorId = getDoctorId();
        if (doctorId == null) {
            throw new IllegalStateException("Authenticated user has no doctorId claim");
        }
        return doctorId;
    }
}
