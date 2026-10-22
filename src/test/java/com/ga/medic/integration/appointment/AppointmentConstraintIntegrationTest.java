package com.ga.medic.integration.appointment;

import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.exception.GlobalExceptionHandler;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.model.PatientProfile;
import com.ga.medic.repository.AppointmentRepository;
import com.ga.medic.repository.DoctorProfileRepository;
import com.ga.medic.repository.PatientProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("integration")
class AppointmentConstraintIntegrationTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @Test
    @Transactional
    void activeSlotUniqueIndexViolationIsReturnedAsConflict() {
        DoctorProfile doctor = doctorProfileRepository.findAll().stream().findFirst().orElseThrow();
        PatientProfile patient = patientProfileRepository.findAll().stream().findFirst().orElseThrow();
        LocalDate date = LocalDate.now().plusYears(2);
        LocalTime startTime = LocalTime.of(8, 0);

        appointmentRepository.saveAndFlush(createAppointment(doctor, patient, date, startTime));
        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> appointmentRepository.saveAndFlush(createAppointment(doctor, patient, date, startTime)));

        HttpServletRequest request = new MockHttpServletRequest("POST", "/patient/appointments");
        var response = exceptionHandler.handleDataIntegrityViolation(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    private Appointment createAppointment(DoctorProfile doctor, PatientProfile patient,
                                          LocalDate date, LocalTime startTime) {
        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(startTime.plusMinutes(30));
        appointment.setStatus(AppointmentStatusEnum.BOOKED);
        return appointment;
    }
}
