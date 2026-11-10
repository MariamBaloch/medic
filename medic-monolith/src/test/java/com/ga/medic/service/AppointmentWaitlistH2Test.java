package com.ga.medic.service;

import com.ga.medic.client.InternalIdentityClient;
import com.ga.medic.dto.request.AppointmentWaitlistRequest;
import com.ga.medic.dto.request.CancelAppointmentRequest;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.AppointmentWaitlistEntryResponse;
import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.enums.WaitlistStatusEnum;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.AppointmentWaitlistEntry;
import com.ga.medic.model.AvailabilityRule;
import com.ga.medic.repository.AppointmentRepository;
import com.ga.medic.repository.AppointmentWaitlistEntryRepository;
import com.ga.medic.repository.AvailabilityRuleRepository;
import com.ga.medic.security.AuthenticatedUser;
import com.ga.medic.support.IdentityTestData;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:appointment_waitlist;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class AppointmentWaitlistH2Test {

    private static final long DOCTOR_ID = 1L;
    private static final long FIRST_WAITER_PATIENT_ID = 101L;
    private static final long SECOND_WAITER_PATIENT_ID = 102L;
    private static final long OCCUPANT_PATIENT_ID = 103L;

    @Autowired
    private AppointmentWaitlistService waitlistService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private AvailabilityRuleRepository availabilityRuleRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentWaitlistEntryRepository waitlistEntryRepository;

    @MockitoBean
    private AuthenticatedUser authenticatedUser;

    @MockitoBean
    private InternalIdentityClient internalIdentityClient;

    private TestData testData;

    @BeforeEach
    void setUp() {
        stubIdentitySnapshots();
        testData = new TransactionTemplate(transactionManager).execute(status -> createTestData());
    }

    @Test
    void waitlistOffersNextPatientAndAcceptsBooking() {
        AppointmentWaitlistEntryResponse firstWaiter = joinAs(
                FIRST_WAITER_PATIENT_ID, testData.doctorId(), testData.date(), testData.startTime());
        AppointmentWaitlistEntryResponse secondWaiter = joinAs(
                SECOND_WAITER_PATIENT_ID, testData.doctorId(), testData.date(), testData.startTime());

        assertEquals(WaitlistStatusEnum.WAITING, firstWaiter.status());
        assertEquals(WaitlistStatusEnum.WAITING, secondWaiter.status());

        authenticateAs(OCCUPANT_PATIENT_ID, 1003L, "Occupant Patient");
        AppointmentResponse cancelled = appointmentService.cancelAppointment(
                testData.occupiedAppointmentId(), new CancelAppointmentRequest("Release slot for waitlist"));
        assertEquals(AppointmentStatusEnum.CANCELLED, cancelled.status());

        AppointmentWaitlistEntry offeredToFirst = waitlistEntryRepository.findById(firstWaiter.id()).orElseThrow();
        assertEquals(WaitlistStatusEnum.OFFERED, offeredToFirst.getStatus());
        assertEquals(WaitlistStatusEnum.WAITING,
                waitlistEntryRepository.findById(secondWaiter.id()).orElseThrow().getStatus());
        assertEquals(15 * 60, Duration.between(offeredToFirst.getOfferedAt(), offeredToFirst.getOfferExpiresAt()).getSeconds());

        authenticateAs(FIRST_WAITER_PATIENT_ID, 1001L, "First Waiter");
        waitlistService.withdraw(firstWaiter.id());
        assertEquals(WaitlistStatusEnum.CANCELLED,
                waitlistEntryRepository.findById(firstWaiter.id()).orElseThrow().getStatus());
        assertEquals(WaitlistStatusEnum.OFFERED,
                waitlistEntryRepository.findById(secondWaiter.id()).orElseThrow().getStatus());

        authenticateAs(SECOND_WAITER_PATIENT_ID, 1002L, "Second Waiter");
        AppointmentResponse booked = waitlistService.accept(secondWaiter.id());
        assertEquals(AppointmentStatusEnum.BOOKED, booked.status());
        assertEquals(testData.date(), booked.appointmentDate());
        assertEquals(testData.startTime(), booked.startTime());
        assertEquals(WaitlistStatusEnum.BOOKED,
                waitlistEntryRepository.findById(secondWaiter.id()).orElseThrow().getStatus());
        assertEquals(1, appointmentRepository.findActiveByDoctorAndDateAndTimeRange(
                testData.doctorId(), testData.date(), testData.startTime(), testData.startTime().plusMinutes(30)).size());
    }

    private AppointmentWaitlistEntryResponse joinAs(Long patientId, Long doctorId, LocalDate date, LocalTime startTime) {
        authenticateAs(patientId, patientUserId(patientId), patientName(patientId));
        return waitlistService.join(new AppointmentWaitlistRequest(doctorId, date, startTime));
    }

    private TestData createTestData() {
        AvailabilityRule rule = new AvailabilityRule();
        rule.setDoctorId(DOCTOR_ID);
        rule.setStartDate(LocalDate.now().minusDays(1));
        rule.setEndDate(LocalDate.now().plusDays(90));
        rule.setDaysOfWeek(EnumSet.allOf(DayOfWeek.class));
        rule.setStartTime(LocalTime.of(9, 0));
        rule.setEndTime(LocalTime.of(12, 0));
        rule.setSlotMinutes(30);
        rule = availabilityRuleRepository.saveAndFlush(rule);

        LocalDate date = LocalDate.now().plusDays(7);
        LocalTime startTime = LocalTime.of(10, 0);
        Appointment occupiedAppointment = new Appointment();
        occupiedAppointment.applyPartySnapshots(
                IdentityTestData.patient(
                        OCCUPANT_PATIENT_ID, 1003L, "Occupant Patient", "occupant@example.test"),
                IdentityTestData.doctor(
                        DOCTOR_ID, 2001L, "Waitlist Doctor", "doctor@example.test", 1L, BigDecimal.valueOf(100)));
        occupiedAppointment.setAvailabilityRuleId(rule.getId());
        occupiedAppointment.setAppointmentDate(date);
        occupiedAppointment.setStartTime(startTime);
        occupiedAppointment.setEndTime(startTime.plusMinutes(30));
        occupiedAppointment.setBookingFee(BigDecimal.valueOf(100));
        occupiedAppointment.setStatus(AppointmentStatusEnum.BOOKED);
        occupiedAppointment = appointmentRepository.saveAndFlush(occupiedAppointment);

        return new TestData(DOCTOR_ID, occupiedAppointment.getId(), date, startTime);
    }

    private void stubIdentitySnapshots() {
        when(internalIdentityClient.getDoctorSnapshot(DOCTOR_ID)).thenReturn(IdentityTestData.doctor(
                DOCTOR_ID, 2001L, "Waitlist Doctor", "doctor@example.test", 1L, BigDecimal.valueOf(100)));
        when(internalIdentityClient.getPatientSnapshot(FIRST_WAITER_PATIENT_ID)).thenReturn(IdentityTestData.patient(
                FIRST_WAITER_PATIENT_ID, 1001L, "First Waiter", "first-waiter@example.test"));
        when(internalIdentityClient.getPatientSnapshot(SECOND_WAITER_PATIENT_ID)).thenReturn(IdentityTestData.patient(
                SECOND_WAITER_PATIENT_ID, 1002L, "Second Waiter", "second-waiter@example.test"));
        when(internalIdentityClient.getPatientSnapshot(OCCUPANT_PATIENT_ID)).thenReturn(IdentityTestData.patient(
                OCCUPANT_PATIENT_ID, 1003L, "Occupant Patient", "occupant@example.test"));
        when(internalIdentityClient.getPatientSnapshot(anyLong())).thenAnswer(invocation -> {
            long patientId = invocation.getArgument(0);
            return IdentityTestData.patient(
                    patientId, patientUserId(patientId), patientName(patientId), "patient" + patientId + "@example.test");
        });
    }

    private void authenticateAs(long patientId, long userId, String name) {
        when(authenticatedUser.getUserId()).thenReturn(userId);
        when(authenticatedUser.requirePatientId()).thenReturn(patientId);
        when(authenticatedUser.getPatientId()).thenReturn(patientId);
        when(authenticatedUser.getName()).thenReturn(name);
    }

    private static long patientUserId(long patientId) {
        return 1000L + patientId;
    }

    private static String patientName(long patientId) {
        return switch ((int) patientId) {
            case 101 -> "First Waiter";
            case 102 -> "Second Waiter";
            case 103 -> "Occupant Patient";
            default -> "Patient " + patientId;
        };
    }

    private record TestData(
            Long doctorId,
            Long occupiedAppointmentId,
            LocalDate date,
            LocalTime startTime
    ) {
    }
}
