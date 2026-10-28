package com.ga.medic.service;

import com.ga.medic.dto.request.AppointmentWaitlistRequest;
import com.ga.medic.dto.request.CancelAppointmentRequest;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.AppointmentWaitlistEntryResponse;
import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.enums.RoleEnum;
import com.ga.medic.enums.UserStatusEnum;
import com.ga.medic.enums.WaitlistStatusEnum;
import com.ga.medic.model.*;
import com.ga.medic.repository.*;
import com.ga.medic.security.AuthenticatedUser;
import com.ga.medic.security.MyUserDetails;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:appointment_waitlist;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class AppointmentWaitlistH2Test {

    @Autowired
    private AppointmentWaitlistService waitlistService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private SpecializationRepository specializationRepository;

    @Autowired
    private AvailabilityRuleRepository availabilityRuleRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentWaitlistEntryRepository waitlistEntryRepository;

    @MockitoBean
    private AuthenticatedUser authenticatedUser;

    @MockitoBean
    private EmailService emailService;

    private TestData testData;

    @BeforeEach
    void setUp() {
        testData = new TransactionTemplate(transactionManager).execute(status -> createTestData());
    }

    @Test
    void waitlistOffersNextPatientAndAcceptsBooking() {
        AppointmentWaitlistEntryResponse firstWaiter = joinAs(
                testData.firstWaiterEmail(), testData.doctorId(), testData.date(), testData.startTime());
        AppointmentWaitlistEntryResponse secondWaiter = joinAs(
                testData.secondWaiterEmail(), testData.doctorId(), testData.date(), testData.startTime());

        assertEquals(WaitlistStatusEnum.WAITING, firstWaiter.status());
        assertEquals(WaitlistStatusEnum.WAITING, secondWaiter.status());

        authenticateAs(testData.occupantEmail());
        AppointmentResponse cancelled = appointmentService.cancelAppointment(
                testData.occupiedAppointmentId(), new CancelAppointmentRequest("Release slot for waitlist"));
        assertEquals(AppointmentStatusEnum.CANCELLED, cancelled.status());

        AppointmentWaitlistEntry offeredToFirst = waitlistEntryRepository.findById(firstWaiter.id()).orElseThrow();
        assertEquals(WaitlistStatusEnum.OFFERED, offeredToFirst.getStatus());
        assertEquals(WaitlistStatusEnum.WAITING,
                waitlistEntryRepository.findById(secondWaiter.id()).orElseThrow().getStatus());
        assertEquals(15 * 60, Duration.between(offeredToFirst.getOfferedAt(), offeredToFirst.getOfferExpiresAt()).getSeconds());

        authenticateAs(testData.firstWaiterEmail());
        waitlistService.withdraw(firstWaiter.id());
        assertEquals(WaitlistStatusEnum.CANCELLED,
                waitlistEntryRepository.findById(firstWaiter.id()).orElseThrow().getStatus());
        assertEquals(WaitlistStatusEnum.OFFERED,
                waitlistEntryRepository.findById(secondWaiter.id()).orElseThrow().getStatus());

        authenticateAs(testData.secondWaiterEmail());
        AppointmentResponse booked = waitlistService.accept(secondWaiter.id());
        assertEquals(AppointmentStatusEnum.BOOKED, booked.status());
        assertEquals(testData.date(), booked.appointmentDate());
        assertEquals(testData.startTime(), booked.startTime());
        assertEquals(WaitlistStatusEnum.BOOKED,
                waitlistEntryRepository.findById(secondWaiter.id()).orElseThrow().getStatus());
        assertEquals(1, appointmentRepository.findActiveByDoctorAndDateAndTimeRange(
                testData.doctorId(), testData.date(), testData.startTime(), testData.startTime().plusMinutes(30)).size());
    }

    private AppointmentWaitlistEntryResponse joinAs(String email, Long doctorId, LocalDate date, LocalTime startTime) {
        authenticateAs(email);
        return waitlistService.join(new AppointmentWaitlistRequest(doctorId, date, startTime));
    }

    private TestData createTestData() {
        Role patientRole = findOrCreateRole(RoleEnum.PATIENT);
        Role doctorRole = findOrCreateRole(RoleEnum.DOCTOR);
        User firstWaiter = createUser(patientRole, "first-waiter");
        User secondWaiter = createUser(patientRole, "second-waiter");
        User occupant = createUser(patientRole, "occupant");
        User doctorUser = createUser(doctorRole, "doctor");

        createPatientProfile(firstWaiter);
        createPatientProfile(secondWaiter);
        PatientProfile occupantPatient = createPatientProfile(occupant);

        Specialization specialization = new Specialization();
        specialization.setName("Waitlist " + UUID.randomUUID());
        specialization.setDescription("Waitlist integration test");
        specialization = specializationRepository.saveAndFlush(specialization);

        DoctorProfile doctor = new DoctorProfile();
        doctor.setUser(doctorUser);
        doctorUser.setDoctorProfile(doctor);
        doctor.setSpecialization(specialization);
        doctor.setLicenseNumber("WAITLIST-" + UUID.randomUUID());
        doctor.setConsultationFee(BigDecimal.valueOf(100));
        doctor.setIsVerified(true);
        doctor = doctorProfileRepository.saveAndFlush(doctor);

        AvailabilityRule rule = new AvailabilityRule();
        rule.setDoctor(doctor);
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
        occupiedAppointment.setPatient(occupantPatient);
        occupiedAppointment.setDoctor(doctor);
        occupiedAppointment.setAvailabilityRule(rule);
        occupiedAppointment.setAppointmentDate(date);
        occupiedAppointment.setStartTime(startTime);
        occupiedAppointment.setEndTime(startTime.plusMinutes(30));
        occupiedAppointment.setBookingFee(doctor.getConsultationFee());
        occupiedAppointment.setStatus(AppointmentStatusEnum.BOOKED);
        occupiedAppointment = appointmentRepository.saveAndFlush(occupiedAppointment);

        return new TestData(firstWaiter.getEmail(), secondWaiter.getEmail(), occupant.getEmail(),
                doctor.getId(), occupiedAppointment.getId(), date, startTime);
    }

    private PatientProfile createPatientProfile(User user) {
        PatientProfile patient = new PatientProfile();
        patient.setUser(user);
        user.setPatientProfile(patient);
        return patientProfileRepository.saveAndFlush(patient);
    }

    private Role findOrCreateRole(RoleEnum roleEnum) {
        return roleRepository.findByName(roleEnum)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(roleEnum);
                    return roleRepository.saveAndFlush(role);
                });
    }

    private User createUser(Role role, String name) {
        String suffix = UUID.randomUUID().toString();
        User user = new User();
        user.setRole(role);
        user.setEmail(name + "-" + suffix + "@example.test");
        user.setPassword("test-only-password");
        user.setFirstName(name);
        user.setLastName("integration");
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPhone("555" + suffix.replace("-", "").substring(0, 10));
        user.setStatus(UserStatusEnum.ACTIVE);
        return userRepository.saveAndFlush(user);
    }

    private void authenticateAs(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        when(authenticatedUser.get()).thenReturn(new MyUserDetails(user));
        when(authenticatedUser.getUserId()).thenReturn(user.getId());
    }

    private record TestData(
            String firstWaiterEmail,
            String secondWaiterEmail,
            String occupantEmail,
            Long doctorId,
            Long occupiedAppointmentId,
            LocalDate date,
            LocalTime startTime
    ) {
    }
}
