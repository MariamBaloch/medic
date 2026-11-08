package com.ga.medic.service;

import com.ga.medic.dto.request.RescheduleAppointmentRequest;
import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.enums.RoleEnum;
import com.ga.medic.enums.UserStatusEnum;
import com.ga.medic.exception.SlotNotAvailableException;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.AvailabilityException;
import com.ga.medic.model.AvailabilityRule;
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.model.PatientProfile;
import com.ga.medic.model.Role;
import com.ga.medic.model.Specialization;
import com.ga.medic.model.User;
import com.ga.medic.repository.AppointmentRepository;
import com.ga.medic.repository.AvailabilityExceptionRepository;
import com.ga.medic.repository.AvailabilityRuleRepository;
import com.ga.medic.repository.DoctorProfileRepository;
import com.ga.medic.repository.PatientProfileRepository;
import com.ga.medic.repository.RoleRepository;
import com.ga.medic.repository.SpecializationRepository;
import com.ga.medic.repository.UserRepository;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:appointment_business_rules;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class AppointmentRescheduleH2Test {

    private static final LocalTime RULE_START = LocalTime.of(9, 0);
    private static final LocalTime RULE_END = LocalTime.of(12, 0);

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
    private AvailabilityExceptionRepository availabilityExceptionRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @MockitoBean
    private AuthenticatedUser authenticatedUser;

    @MockitoBean
    private EmailService emailService;

    private AppointmentTestData testData;

    @BeforeEach
    void setUp() {
        testData = new TransactionTemplate(transactionManager).execute(status -> createTestData());
    }

    @Test
    void validRescheduleMovesTheExistingAppointmentRow() {
        authenticateAs(testData.patientEmail());
        LocalDate targetDate = LocalDate.now().plusDays(15);
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
        authenticateAs(testData.patientEmail());
        LocalDate pastDate = LocalDate.now().minusDays(1);
        Long pastAppointmentId = persistAppointment(
                testData.patientId(), pastDate, LocalTime.of(9, 0), AppointmentStatusEnum.BOOKED);

        assertThrows(IllegalArgumentException.class, () -> appointmentService.rescheduleAppointment(
                pastAppointmentId,
                new RescheduleAppointmentRequest(LocalDate.now().plusDays(15), LocalTime.of(10, 0), 0L)));

        Appointment unchanged = appointmentRepository.findById(pastAppointmentId).orElseThrow();
        assertEquals(pastDate, unchanged.getAppointmentDate());
        assertEquals(LocalTime.of(9, 0), unchanged.getStartTime());
    }

    @Test
    void completedAppointmentCannotBeRescheduled() {
        authenticateAs(testData.patientEmail());
        setStatus(testData.appointmentId(), AppointmentStatusEnum.COMPLETED);

        assertThrows(IllegalArgumentException.class, () -> appointmentService.rescheduleAppointment(
                testData.appointmentId(),
                new RescheduleAppointmentRequest(LocalDate.now().plusDays(15), LocalTime.of(10, 0), testData.version())));
    }

    @Test
    void availabilityExceptionBlocksRescheduleAndPreservesOriginalAppointment() {
        authenticateAs(testData.patientEmail());
        LocalDate targetDate = LocalDate.now().plusDays(15);
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
        Role patientRole = findOrCreateRole(RoleEnum.PATIENT);
        Role doctorRole = findOrCreateRole(RoleEnum.DOCTOR);
        User patientUser = createUser(patientRole, "patient");
        User doctorUser = createUser(doctorRole, "doctor");

        PatientProfile patient = new PatientProfile();
        patient.setUser(patientUser);
        patientUser.setPatientProfile(patient);
        patient = patientProfileRepository.saveAndFlush(patient);

        Specialization specialization = new Specialization();
        specialization.setName("H2 " + UUID.randomUUID());
        specialization.setDescription("Business-rule integration test");
        specialization = specializationRepository.saveAndFlush(specialization);

        DoctorProfile doctor = new DoctorProfile();
        doctor.setUser(doctorUser);
        doctor.setSpecialization(specialization);
        doctor.setLicenseNumber("H2-" + UUID.randomUUID());
        doctor.setConsultationFee(BigDecimal.valueOf(100));
        doctor.setIsVerified(true);
        doctor = doctorProfileRepository.saveAndFlush(doctor);

        AvailabilityRule rule = new AvailabilityRule();
        rule.setDoctor(doctor);
        rule.setStartDate(LocalDate.now().minusDays(1));
        rule.setEndDate(LocalDate.now().plusDays(90));
        rule.setDaysOfWeek(EnumSet.allOf(DayOfWeek.class));
        rule.setStartTime(RULE_START);
        rule.setEndTime(RULE_END);
        rule.setSlotMinutes(30);
        rule = availabilityRuleRepository.saveAndFlush(rule);

        LocalDate originalDate = LocalDate.now().plusDays(2);
        LocalTime originalStartTime = LocalTime.of(9, 0);
        Appointment appointment = createAppointment(patient, doctor, rule, originalDate, originalStartTime,
                AppointmentStatusEnum.BOOKED);
        appointment = appointmentRepository.saveAndFlush(appointment);

        return new AppointmentTestData(patientUser.getEmail(), patient.getId(), doctor.getId(),
                rule.getId(), appointment.getId(), appointment.getVersion(), originalDate, originalStartTime);
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

    private Appointment createAppointment(PatientProfile patient, DoctorProfile doctor, AvailabilityRule rule,
                                         LocalDate date, LocalTime startTime, AppointmentStatusEnum status) {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAvailabilityRule(rule);
        appointment.setAppointmentDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(startTime.plusMinutes(rule.getSlotMinutes()));
        appointment.setBookingFee(doctor.getConsultationFee());
        appointment.setStatus(status);
        return appointment;
    }

    private Long persistAppointment(Long patientId, LocalDate date, LocalTime startTime,
                                    AppointmentStatusEnum status) {
        return new TransactionTemplate(transactionManager).execute(transactionStatus -> {
            PatientProfile patient = patientProfileRepository.findById(patientId).orElseThrow();
            DoctorProfile doctor = doctorProfileRepository.findById(testData.doctorId()).orElseThrow();
            AvailabilityRule rule = availabilityRuleRepository.findById(testData.ruleId()).orElseThrow();
            Appointment appointment = createAppointment(patient, doctor, rule, date, startTime, status);
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
            DoctorProfile doctor = doctorProfileRepository.findById(testData.doctorId()).orElseThrow();
            AvailabilityException exception = new AvailabilityException();
            exception.setDoctor(doctor);
            exception.setExceptionDate(date);
            exception.setStartTime(startTime);
            exception.setEndTime(endTime);
            exception.setReason("H2 integration test block");
            availabilityExceptionRepository.saveAndFlush(exception);
            return null;
        });
    }

    private void authenticateAs(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        when(authenticatedUser.get()).thenReturn(new MyUserDetails(user));
    }

    private record AppointmentTestData(
            String patientEmail,
            Long patientId,
            Long doctorId,
            Long ruleId,
            Long appointmentId,
            Long version,
            LocalDate originalDate,
            LocalTime originalStartTime
    ) {
    }
}
