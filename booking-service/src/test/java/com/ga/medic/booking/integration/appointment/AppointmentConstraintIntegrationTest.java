package com.ga.medic.booking.integration.appointment;

import com.ga.medic.booking.enums.AppointmentStatusEnum;
import com.ga.medic.booking.exception.GlobalExceptionHandler;
import com.ga.medic.booking.model.Appointment;
import com.ga.medic.booking.repository.AppointmentRepository;
import com.ga.medic.booking.support.IdentityTestData;
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

    private static final long DOCTOR_ID = 1L;
    private static final long PATIENT_ONE_ID = 1L;
    private static final long PATIENT_TWO_ID = 2L;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void activeSlotUniqueIndexViolationIsReturnedAsConflict() {
        LocalDate date = LocalDate.now().plusYears(2);
        LocalTime startTime = LocalTime.of(8, 0);

        appointmentRepository.saveAndFlush(createAppointment(PATIENT_ONE_ID, date, startTime));
        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> appointmentRepository.saveAndFlush(createAppointment(PATIENT_ONE_ID, date, startTime)));

        HttpServletRequest request = new MockHttpServletRequest("POST", "/patient/appointments");
        var response = exceptionHandler.handleDataIntegrityViolation(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void waitlistPartialIndexesAllowMultiplePatientsButRejectDuplicateActiveEntriesAndOffers() {
        List<Long> patientIds = List.of(PATIENT_ONE_ID, PATIENT_TWO_ID);

        LocalDate date = LocalDate.now().plusYears(5);
        LocalTime waitingTime = LocalTime.of(9, 0);
        LocalTime offeredTime = LocalTime.of(9, 30);

        try {
            insertWaitlistEntry(patientIds.get(0), DOCTOR_ID, date, waitingTime, "WAITING");
            insertWaitlistEntry(patientIds.get(1), DOCTOR_ID, date, waitingTime, "WAITING");
            assertThrows(DataIntegrityViolationException.class,
                    () -> insertWaitlistEntry(patientIds.get(0), DOCTOR_ID, date, waitingTime, "OFFERED"),
                    "A patient cannot have two active entries for the same slot");

            insertWaitlistEntry(patientIds.get(0), DOCTOR_ID, date, offeredTime, "OFFERED");
            assertThrows(DataIntegrityViolationException.class,
                    () -> insertWaitlistEntry(patientIds.get(1), DOCTOR_ID, date, offeredTime, "OFFERED"),
                    "Only one patient can hold an active offer for a slot");
        } finally {
            jdbcTemplate.update("DELETE FROM appointment_waitlist_entry WHERE doctor_id = ? AND appointment_date = ?",
                    DOCTOR_ID, date);
        }
    }

    private void insertWaitlistEntry(Long patientId, Long doctorId, LocalDate date, LocalTime startTime, String status) {
        var patient = patientId.equals(PATIENT_ONE_ID)
                ? IdentityTestData.seedPatientOne()
                : IdentityTestData.seedPatientTwo();
        var doctor = IdentityTestData.seedDoctorOne();

        jdbcTemplate.update("""
                INSERT INTO appointment_waitlist_entry
                    (patient_id, doctor_id, patient_user_id, doctor_user_id,
                     patient_name, patient_email, doctor_name, doctor_email,
                     appointment_date, preferred_start_time, status, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                patientId, doctorId,
                patient.patientUserId(), doctor.doctorUserId(),
                patient.patientName(), patient.patientEmail(),
                doctor.doctorName(), doctor.doctorEmail(),
                date, startTime, status);
    }

    private Appointment createAppointment(Long patientId, LocalDate date, LocalTime startTime) {
        var patient = patientId.equals(PATIENT_ONE_ID)
                ? IdentityTestData.seedPatientOne()
                : IdentityTestData.seedPatientTwo();
        var doctor = IdentityTestData.seedDoctorOne();

        Appointment appointment = new Appointment();
        appointment.applyPartySnapshots(patient, doctor);
        appointment.setAppointmentDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(startTime.plusMinutes(30));
        appointment.setStatus(AppointmentStatusEnum.BOOKED);
        return appointment;
    }
}
