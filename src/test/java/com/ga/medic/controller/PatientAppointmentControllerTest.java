package com.ga.medic.controller;

import com.ga.medic.dto.request.RescheduleAppointmentRequest;
import com.ga.medic.dto.response.AppointmentDoctorResponse;
import com.ga.medic.dto.response.AppointmentPatientResponse;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.SpecializationResponse;
import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.exception.GlobalExceptionHandler;
import com.ga.medic.service.AppointmentService;
import com.ga.medic.service.AvailabilityRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PatientAppointmentControllerTest {

    private final AppointmentService appointmentService = mock(AppointmentService.class);
    private final AvailabilityRuleService availabilityRuleService = mock(AvailabilityRuleService.class);
    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(new PatientAppointmentController(appointmentService, availabilityRuleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void rescheduleEndpointReturnsUpdatedAppointmentAndVersion() throws Exception {
        Long appointmentId = 7L;
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime startTime = LocalTime.of(10, 0);
        AppointmentResponse response = new AppointmentResponse(
                appointmentId,
                1L,
                new AppointmentDoctorResponse(2L, "Dr. Sarah Smith", "doctor.png",
                        new SpecializationResponse(1L, "Cardiology", "Heart care"), "MBBS, MD"),
                new AppointmentPatientResponse(3L, "John Doe", "patient.png"),
                date,
                startTime,
                startTime.plusMinutes(30),
                null,
                AppointmentStatusEnum.BOOKED,
                null,
                null,
                null
        );
        when(appointmentService.rescheduleAppointment(
                eq(appointmentId), any(RescheduleAppointmentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/patient/appointments/{appointmentId}/reschedule", appointmentId)
                        .contentType("application/json")
                        .content("""
                                {"date":"%s","startTime":"10:00","expectedVersion":0}
                                """.formatted(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.doctor.doctorId").value(2))
                .andExpect(jsonPath("$.doctor.doctorName").value("Dr. Sarah Smith"))
                .andExpect(jsonPath("$.doctor.doctorImageUrl").value("doctor.png"))
                .andExpect(jsonPath("$.doctor.specialization.name").value("Cardiology"))
                .andExpect(jsonPath("$.doctor.doctorQualification").value("MBBS, MD"))
                .andExpect(jsonPath("$.patient.patientId").value(3))
                .andExpect(jsonPath("$.patient.patientName").value("John Doe"))
                .andExpect(jsonPath("$.patient.patientImageUrl").value("patient.png"))
                .andExpect(jsonPath("$.appointmentDate").value(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
                .andExpect(jsonPath("$.startTime").value("10:00"));

        verify(appointmentService).rescheduleAppointment(eq(appointmentId), any(RescheduleAppointmentRequest.class));
    }

    @Test
    void rescheduleEndpointRejectsMissingExpectedVersion() throws Exception {
        mockMvc.perform(put("/patient/appointments/{appointmentId}/reschedule", 7L)
                        .contentType("application/json")
                        .content("""
                                {"date":"%s","startTime":"10:00"}
                                """.formatted(LocalDate.now().plusDays(2).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appointmentService);
    }

}
