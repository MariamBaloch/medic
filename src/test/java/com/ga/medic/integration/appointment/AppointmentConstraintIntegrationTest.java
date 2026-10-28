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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    void waitlistPartialIndexesAllowMultiplePatientsButRejectDuplicateActiveEntriesAndOffers() {
        DoctorProfile doctor = doctorProfileRepository.findAll().stream().findFirst().orElseThrow();
        List<PatientProfile> patients = patientProfileRepository.findAll();
        assertTrue(patients.size() >= 2, "The integration seed must provide two patients");

        LocalDate date = LocalDate.now().plusYears(5);
        LocalTime waitingTime = LocalTime.of(9, 0);
        LocalTime offeredTime = LocalTime.of(9, 30);

        try {
            insertWaitlistEntry(patients.get(0).getId(), doctor.getId(), date, waitingTime, "WAITING");
            insertWaitlistEntry(patients.get(1).getId(), doctor.getId(), date, waitingTime, "WAITING");
            assertThrows(DataIntegrityViolationException.class,
                    () -> insertWaitlistEntry(patients.get(0).getId(), doctor.getId(), date, waitingTime, "OFFERED"),
                    "A patient cannot have two active entries for the same slot");

            insertWaitlistEntry(patients.get(0).getId(), doctor.getId(), date, offeredTime, "OFFERED");
            assertThrows(DataIntegrityViolationException.class,
                    () -> insertWaitlistEntry(patients.get(1).getId(), doctor.getId(), date, offeredTime, "OFFERED"),
                    "Only one patient can hold an active offer for a slot");
        } finally {
            jdbcTemplate.update("DELETE FROM appointment_waitlist_entry WHERE doctor_id = ? AND appointment_date = ?",
                    doctor.getId(), date);
        }
    }

    private void insertWaitlistEntry(Long patientId, Long doctorId, LocalDate date, LocalTime startTime, String status) {
        jdbcTemplate.update("""
                INSERT INTO appointment_waitlist_entry
                    (patient_id, doctor_id, appointment_date, preferred_start_time, status)
                VALUES (?, ?, ?, ?, ?)
                """, patientId, doctorId, date, startTime, status);
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
