package com.ga.medic.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringJUnitConfig(RoleAuthorizationTest.TestConfig.class)
class RoleAuthorizationTest {

    @Autowired
    private RoleAccess roleAccess;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void patientCanOnlyAccessPatientResource() {
        loginAs("PATIENT");

        assertDoesNotThrow(roleAccess::patient);
        assertThrows(AccessDeniedException.class, roleAccess::doctor);
        assertThrows(AccessDeniedException.class, roleAccess::admin);
    }

    @Test
    void doctorCanOnlyAccessDoctorResource() {
        loginAs("DOCTOR");

        assertDoesNotThrow(roleAccess::doctor);
        assertThrows(AccessDeniedException.class, roleAccess::patient);
        assertThrows(AccessDeniedException.class, roleAccess::admin);
    }

    @Test
    void adminCanOnlyAccessAdminResource() {
        loginAs("ADMIN");

        assertDoesNotThrow(roleAccess::admin);
        assertThrows(AccessDeniedException.class, roleAccess::patient);
        assertThrows(AccessDeniedException.class, roleAccess::doctor);
    }

    private void loginAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(role, "", java.util.List.of(
                        new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        RoleAccess roleAccess() {
            return new RoleAccess();
        }
    }

    static class RoleAccess {
        @PreAuthorize("hasRole('PATIENT')")
        public void patient() {
        }

        @PreAuthorize("hasRole('DOCTOR')")
        public void doctor() {
        }

        @PreAuthorize("hasRole('ADMIN')")
        public void admin() {
        }
    }
}