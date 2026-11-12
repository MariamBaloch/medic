package com.ga.medic.booking.service;

import com.ga.medic.booking.client.InternalIdentityClient;
import com.ga.medic.booking.dto.request.RescheduleAppointmentRequest;
import com.ga.medic.booking.enums.AppointmentStatusEnum;
import com.ga.medic.booking.exception.SlotNotAvailableException;
import com.ga.medic.booking.model.Appointment;
import com.ga.medic.booking.model.AvailabilityException;
import com.ga.medic.booking.model.AvailabilityRule;
import com.ga.medic.booking.repository.AppointmentRepository;
import com.ga.medic.booking.repository.AvailabilityExceptionRepository;
import com.ga.medic.booking.repository.AvailabilityRuleRepository;
import com.ga.medic.security.AuthenticatedUser;
import com.ga.medic.booking.support.IdentityTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:appointment_business_rules;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class AppointmentRescheduleH2Test {

    private static final long DOCTOR_ID = 1L;
    private static final long PATIENT_ID = 11L;
    private static final long PATIENT_USER_ID = 401L;
    private static final LocalTime RULE_START = LocalTime.of(9, 0);
    private static final LocalTime RULE_END = LocalTime.of(12, 0);

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private AvailabilityRuleRepository availabilityRuleRepository;

    @Autowired
    private AvailabilityExceptionRepository availabilityExceptionRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @MockitoBean
    private AuthenticatedUser authenticatedUser;

    @MockitoBean
    private InternalIdentityClient internalIdentityClient;

    private AppointmentTestData testData;

    @BeforeEach
    void setUp() {
        stubIdentitySnapshots();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            appointmentRepository.deleteAll();
            availabilityExceptionRepository.deleteAll();
            availabilityRuleRepository.deleteAll();
        });
        testData = new TransactionTemplate(transactionManager).execute(status -> createTestData());
    }

    @Test
    void validRescheduleMovesTheExistingAppointmentRow() {
        authenticateAsPatient();
        LocalDate targetDate = testData.originalDate();
        LocalTime targetStartTime = LocalTime.of(10, 0);

        long appointmentCountBefore = appointmentRepository.count();
        var response = appointmentService.rescheduleAppointment(
                testData.appointmentId(),
                new RescheduleAppointmentRequest(targetDate, targetStartTime, testData.version()));

        assertEquals(testData.appointmentId(), response.id());
        assertEquals(targetDate, response.appointmentDate());
        assertEquals(targetStartTime, response.startTime());
        assertEquals(testData.version() + 1, response.version());
        assertEquals(appointmentCountBefore, appointmentRepository.count(),
                "Rescheduling should update the existing appointment rather than insert a new row");
    }

    @Test
    void pastAppointmentCannotBeRescheduled() {
        authenticateAsPatient();
        LocalDate pastDate = LocalDate.now().minusDays(1);
        Long pastAppointmentId = persistAppointment(
                pastDate, LocalTime.of(9, 0), AppointmentStatusEnum.BOOKED);

        assertThrows(IllegalArgumentException.class, () -> appointmentService.rescheduleAppointment(
                pastAppointmentId,
                new RescheduleAppointmentRequest(LocalDate.now().plusDays(15), LocalTime.of(10, 0), 0L)));

        Appointment unchanged = appointmentRepository.findById(pastAppointmentId).orElseThrow();
        assertEquals(pastDate, unchanged.getAppointmentDate());
        assertEquals(LocalTime.of(9, 0), unchanged.getStartTime());
    }

    @Test
    void completedAppointmentCannotBeRescheduled() {
        authenticateAsPatient();
        setStatus(testData.appointmentId(), AppointmentStatusEnum.COMPLETED);

        assertThrows(IllegalArgumentException.class, () -> appointmentService.rescheduleAppointment(
                testData.appointmentId(),
                new RescheduleAppointmentRequest(LocalDate.now().plusDays(15), LocalTime.of(10, 0), testData.version())));
    }

    @Test
    void availabilityExceptionBlocksRescheduleAndPreservesOriginalAppointment() {
        authenticateAsPatient();
        LocalDate targetDate = testData.originalDate();
        addAvailabilityException(targetDate, LocalTime.of(10, 0), LocalTime.of(10, 30));

        assertThrows(SlotNotAvailableException.class, () -> appointmentService.rescheduleAppointment(
                testData.appointmentId(),
                new RescheduleAppointmentRequest(targetDate, LocalTime.of(10, 0), testData.version())));

        Appointment unchanged = appointmentRepository.findById(testData.appointmentId()).orElseThrow();
        assertEquals(testData.originalDate(), unchanged.getAppointmentDate());
        assertEquals(testData.originalStartTime(), unchanged.getStartTime());
        assertEquals(testData.version(), unchanged.getVersion());
    }

    private AppointmentTestData createTestData() {
        AvailabilityRule rule = new AvailabilityRule();
        rule.setDoctorId(DOCTOR_ID);
        rule.setStartDate(LocalDate.now().minusDays(1));
        rule.setEndDate(LocalDate.now().plusDays(90));
        rule.setDaysOfWeek(EnumSet.allOf(DayOfWeek.class));
        rule.setStartTime(RULE_START);
        rule.setEndTime(RULE_END);
        rule.setSlotMinutes(30);
        rule = availabilityRuleRepository.saveAndFlush(rule);
        rule = availabilityRuleRepository.findById(rule.getId()).orElseThrow();

        LocalDate originalDate = LocalDate.now().plusDays(2);
        LocalTime originalStartTime = LocalTime.of(9, 0);
        Appointment appointment = createAppointment(rule, originalDate, originalStartTime, AppointmentStatusEnum.BOOKED);
        appointment = appointmentRepository.saveAndFlush(appointment);

        return new AppointmentTestData(
                rule.getId(), appointment.getId(), appointment.getVersion(), originalDate, originalStartTime);
    }

    private Appointment createAppointment(
            AvailabilityRule rule, LocalDate date, LocalTime startTime, AppointmentStatusEnum status) {
        Appointment appointment = new Appointment();
        appointment.applyPartySnapshots(
                IdentityTestData.patient(PATIENT_ID, PATIENT_USER_ID, "Reschedule Patient", "patient@example.test"),
                IdentityTestData.doctor(
                        DOCTOR_ID, 201L, "Reschedule Doctor", "doctor@example.test", 1L, BigDecimal.valueOf(100)));
        appointment.setAvailabilityRuleId(rule.getId());
        appointment.setAppointmentDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(startTime.plusMinutes(rule.getSlotMinutes()));
        appointment.setBookingFee(BigDecimal.valueOf(100));
        appointment.setStatus(status);
        return appointment;
    }

    private Long persistAppointment(LocalDate date, LocalTime startTime, AppointmentStatusEnum status) {
        return new TransactionTemplate(transactionManager).execute(transactionStatus -> {
            AvailabilityRule rule = availabilityRuleRepository.findById(testData.ruleId()).orElseThrow();
            Appointment appointment = createAppointment(rule, date, startTime, status);
            return appointmentRepository.saveAndFlush(appointment).getId();
        });
    }

    private void setStatus(Long appointmentId, AppointmentStatusEnum status) {
        new TransactionTemplate(transactionManager).execute(transactionStatus -> {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
            appointment.setStatus(status);
            appointmentRepository.saveAndFlush(appointment);
            return null;
        });
    }

    private void addAvailabilityException(LocalDate date, LocalTime startTime, LocalTime endTime) {
        new TransactionTemplate(transactionManager).execute(transactionStatus -> {
            AvailabilityException exception = new AvailabilityException();
            exception.setDoctorId(DOCTOR_ID);
            exception.setExceptionDate(date);
            exception.setStartTime(startTime);
            exception.setEndTime(endTime);
            exception.setReason("H2 integration test block");
            availabilityExceptionRepository.saveAndFlush(exception);
            return null;
        });
    }

    private void stubIdentitySnapshots() {
        when(internalIdentityClient.getDoctorSnapshot(DOCTOR_ID)).thenReturn(IdentityTestData.doctor(
                DOCTOR_ID, 201L, "Reschedule Doctor", "doctor@example.test", 1L, BigDecimal.valueOf(100)));
        when(internalIdentityClient.getPatientSnapshot(PATIENT_ID)).thenReturn(IdentityTestData.patient(
                PATIENT_ID, PATIENT_USER_ID, "Reschedule Patient", "patient@example.test"));
        when(internalIdentityClient.getPatientSnapshot(anyLong())).thenReturn(IdentityTestData.patient(
                PATIENT_ID, PATIENT_USER_ID, "Reschedule Patient", "patient@example.test"));
    }

    private void authenticateAsPatient() {
        when(authenticatedUser.getUserId()).thenReturn(PATIENT_USER_ID);
        when(authenticatedUser.requirePatientId()).thenReturn(PATIENT_ID);
        when(authenticatedUser.getPatientId()).thenReturn(PATIENT_ID);
        when(authenticatedUser.getName()).thenReturn("Reschedule Patient");
    }

    private record AppointmentTestData(
            Long ruleId,
            Long appointmentId,
            Long version,
            LocalDate originalDate,
            LocalTime originalStartTime
    ) {
    }
}
