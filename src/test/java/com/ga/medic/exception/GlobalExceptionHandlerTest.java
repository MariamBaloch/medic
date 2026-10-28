package com.ga.medic.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void activeAppointmentSlotConstraintIsReturnedAsConflict() {
        var constraintViolation = new ConstraintViolationException(
                "duplicate appointment slot",
                new SQLException("duplicate key"),
                "uq_appointment_active_doctor_slot"
        );
        var exception = new DataIntegrityViolationException("Could not save appointment", constraintViolation);
        var request = new MockHttpServletRequest("POST", "/patient/appointments");

        var response = exceptionHandler.handleDataIntegrityViolation(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("This appointment slot has just been booked by another patient",
                response.getBody().message());
    }

    @Test
    void unrelatedDatabaseConstraintIsNotReportedAsAppointmentConflict() {
        var constraintViolation = new ConstraintViolationException(
                "unrelated constraint",
                new SQLException("duplicate key"),
                "some_other_constraint"
        );
        var exception = new DataIntegrityViolationException("Could not save data", constraintViolation);
        var request = new MockHttpServletRequest("POST", "/patient/appointments");

        var response = exceptionHandler.handleDataIntegrityViolation(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().message());
    }

    @Test
    void optimisticAppointmentUpdateIsReturnedAsConflict() {
        var request = new MockHttpServletRequest("PUT", "/patient/appointments/1/cancel");

        var response = exceptionHandler.handleOptimisticLockingFailure(
                new OptimisticLockingFailureException("stale appointment version"),
                request
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("This appointment was updated by another request. Refresh and try again.",
                response.getBody().message());
    }
}
