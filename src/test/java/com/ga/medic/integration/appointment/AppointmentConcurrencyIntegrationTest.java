package com.ga.medic.integration.appointment;

import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.User;
import com.ga.medic.repository.AppointmentRepository;
import com.ga.medic.repository.DoctorProfileRepository;
import com.ga.medic.repository.PatientProfileRepository;
import com.ga.medic.repository.UserRepository;
import com.ga.medic.security.JWTUtils;
import com.ga.medic.security.MyUserDetails;
import com.ga.medic.service.EmailService;
import com.ga.medic.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
class AppointmentConcurrencyIntegrationTest {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Environment environment;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DoctorProfileRepository doctorProfileRepository;
    @Autowired
    private PatientProfileRepository patientProfileRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JWTUtils jwtUtils;
    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;
    @MockitoBean
    private EmailService emailService;
    private String patientOneToken;
    private String patientTwoToken;
    private Long patientOneId;
    private Long doctorOneId;
    private Long doctorTwoId;

    @BeforeEach
    void setUp() {
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);

        User patientOne = findSeedUser("patient1@medic.com");
        User patientTwo = findSeedUser("patient2@medic.com");
        User doctorOne = findSeedUser("doctor1@medic.com");
        User doctorTwo = findSeedUser("doctor2@medic.com");

        patientOneToken = jwtUtils.generateJwtToken(new MyUserDetails(patientOne));
        patientTwoToken = jwtUtils.generateJwtToken(new MyUserDetails(patientTwo));
        patientOneId = patientProfileRepository.findByUserId(patientOne.getId()).orElseThrow().getId();
        doctorOneId = doctorProfileRepository.findByUserId(doctorOne.getId()).orElseThrow().getId();
        doctorTwoId = doctorProfileRepository.findByUserId(doctorTwo.getId()).orElseThrow().getId();
    }

    @Test
    void concurrentPatientsClaimingSameSlotHaveOneWinnerAcrossRepeatedRaces() throws Exception {
        LocalDate firstRaceDate = nextWeekdayFromNow(35);

        for (int race = 0; race < 3; race++) {
            LocalDate date = firstRaceDate.plusWeeks(race);
            LocalTime startTime = LocalTime.of(9, 0).plusMinutes(30L * race);

            List<ApiResponse> responses = concurrently(
                    () -> book(patientOneToken, doctorOneId, date, startTime),
                    () -> book(patientTwoToken, doctorOneId, date, startTime)
            );

            assertExactlyOneCreatedAndOneConflict(responses);
            assertEquals(1L, countActiveDoctorSlot(doctorOneId, date, startTime),
                    "Each contested doctor slot must have exactly one active appointment");
        }
    }

    @Test
    void independentBookingsCanSucceedAtTheSameTimeForDifferentDoctors() throws Exception {
        LocalDate date = nextWednesdayFromNow(70);
        LocalTime startTime = LocalTime.of(10, 0);

        List<ApiResponse> responses = concurrently(
                () -> book(patientOneToken, doctorOneId, date, startTime),
                () -> book(patientTwoToken, doctorTwoId, date, startTime)
        );

        assertEquals(2, responses.stream().filter(response -> response.statusCode() == HttpStatus.CREATED.value()).count());
        assertEquals(1L, countActiveDoctorSlot(doctorOneId, date, startTime));
        assertEquals(1L, countActiveDoctorSlot(doctorTwoId, date, startTime));
    }

    @Test
    void patientRowLockPreventsOverlappingConcurrentBookingsAcrossDoctors() throws Exception {
        LocalDate date = nextWednesdayFromNow(90);
        LocalTime startTime = LocalTime.of(10, 0);

        List<ApiResponse> responses = concurrently(
                () -> book(patientOneToken, doctorOneId, date, startTime),
                () -> book(patientOneToken, doctorTwoId, date, startTime)
        );

        assertExactlyOneCreatedAndOneConflict(responses);
        assertEquals(1L, countOverlappingPatientBookings(
                        patientOneId, date, startTime, startTime.plusMinutes(30)),
                "The patient must not retain overlapping active appointments across doctors");
    }

    @Test
    void concurrentReschedulesUsingTheSameVersionHaveOneWinner() throws Exception {
        LocalDate targetDate = nextWednesdayFromNow(120);
        LocalDate originalDate = targetDate.minusWeeks(1);
        LocalTime originalStartTime = LocalTime.of(9, 0);
        LocalTime targetStartTime = LocalTime.of(10, 0);

        ApiResponse created = book(patientOneToken, doctorOneId, originalDate, originalStartTime);
        assertEquals(HttpStatus.CREATED.value(), created.statusCode());
        JsonNode createdBody = json(created);
        long appointmentId = createdBody.path("id").asLong();
        long expectedVersion = createdBody.path("version").asLong();

        List<ApiResponse> responses = concurrently(
                () -> reschedule(patientOneToken, appointmentId, targetDate, targetStartTime, expectedVersion),
                () -> reschedule(patientOneToken, appointmentId, targetDate, targetStartTime, expectedVersion)
        );

        assertEquals(1, responses.stream().filter(response -> response.statusCode() == HttpStatus.OK.value()).count());
        assertEquals(1, responses.stream().filter(response -> response.statusCode() == HttpStatus.CONFLICT.value()).count());

        Appointment finalAppointment = appointmentRepository.findById(appointmentId).orElseThrow();
        assertEquals(targetDate, finalAppointment.getAppointmentDate());
        assertEquals(targetStartTime, finalAppointment.getStartTime());
        assertEquals(expectedVersion + 1, finalAppointment.getVersion());
        assertEquals(1L, countActiveDoctorSlot(doctorOneId, targetDate, targetStartTime));
    }

    @Test
    void competingBookingAndRescheduleCannotBothClaimTheTargetSlot() throws Exception {
        LocalDate targetDate = nextWeekdayFromNow(180);
        LocalDate originalDate = targetDate.minusWeeks(1);
        LocalTime originalStartTime = LocalTime.of(9, 0);
        LocalTime targetStartTime = LocalTime.of(10, 0);

        ApiResponse created = book(patientOneToken, doctorOneId, originalDate, originalStartTime);
        assertEquals(HttpStatus.CREATED.value(), created.statusCode());
        JsonNode createdBody = json(created);
        long appointmentId = createdBody.path("id").asLong();
        long expectedVersion = createdBody.path("version").asLong();

        List<ApiResponse> responses = concurrently(
                () -> reschedule(patientOneToken, appointmentId, targetDate, targetStartTime, expectedVersion),
                () -> book(patientTwoToken, doctorOneId, targetDate, targetStartTime)
        );

        assertEquals(1, responses.stream().filter(response -> response.statusCode() == HttpStatus.OK.value()
                || response.statusCode() == HttpStatus.CREATED.value()).count());
        assertEquals(1, responses.stream().filter(response -> response.statusCode() == HttpStatus.CONFLICT.value()).count());
        assertEquals(1L, countActiveDoctorSlot(doctorOneId, targetDate, targetStartTime));

        Appointment original = appointmentRepository.findById(appointmentId).orElseThrow();
        boolean rescheduleWon = responses.stream()
                .anyMatch(response -> response.statusCode() == HttpStatus.OK.value());
        if (rescheduleWon) {
            assertEquals(targetDate, original.getAppointmentDate());
            assertEquals(targetStartTime, original.getStartTime());
            assertEquals(expectedVersion + 1, original.getVersion());
        } else {
            assertEquals(originalDate, original.getAppointmentDate(),
                    "A losing reschedule must preserve its original appointment date");
            assertEquals(originalStartTime, original.getStartTime(),
                    "A losing reschedule must preserve its original slot");
            assertEquals(expectedVersion, original.getVersion());
        }
    }

    @Test
    void cancelledSlotCanBeRebookedByAnotherPatient() throws Exception {
        LocalDate date = nextWeekdayFromNow(150);
        LocalTime startTime = LocalTime.of(11, 0);

        ApiResponse booking = book(patientOneToken, doctorOneId, date, startTime);
        assertEquals(HttpStatus.CREATED.value(), booking.statusCode());
        long appointmentId = json(booking).path("id").asLong();

        ApiResponse cancelled = send(patientOneToken, "/patient/appointments/" + appointmentId + "/cancel",
                "PUT", Map.of("reason", "Integration test cancellation"));
        assertEquals(HttpStatus.OK.value(), cancelled.statusCode());
        assertEquals(AppointmentStatusEnum.CANCELLED,
                appointmentRepository.findById(appointmentId).orElseThrow().getStatus());

        ApiResponse replacement = book(patientTwoToken, doctorOneId, date, startTime);
        assertEquals(HttpStatus.CREATED.value(), replacement.statusCode());
        assertEquals(1L, countActiveDoctorSlot(doctorOneId, date, startTime));
    }

    private ApiResponse book(String token, Long doctorId, LocalDate date, LocalTime startTime) throws Exception {
        Map<String, Object> body = Map.of(
                "doctorId", doctorId,
                "date", DATE_FORMAT.format(date),
                "startTime", TIME_FORMAT.format(startTime),
                "reason", "Day 5 concurrency integration test"
        );
        return send(token, "/patient/appointments", "POST", body);
    }

    private ApiResponse reschedule(String token, long appointmentId, LocalDate date,
                                   LocalTime startTime, long expectedVersion) throws Exception {
        Map<String, Object> body = Map.of(
                "date", DATE_FORMAT.format(date),
                "startTime", TIME_FORMAT.format(startTime),
                "expectedVersion", expectedVersion
        );
        return send(token, "/patient/appointments/" + appointmentId + "/reschedule", "PUT", body);
    }

    private ApiResponse send(String token, String path, String method, Map<String, Object> body) throws Exception {
        int port = environment.getRequiredProperty("local.server.port", Integer.class);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }

    @SafeVarargs
    private final List<ApiResponse> concurrently(Callable<ApiResponse>... requests) throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(requests.length);
        CountDownLatch ready = new CountDownLatch(requests.length);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ApiResponse>> futures = new ArrayList<>();

        try {
            for (Callable<ApiResponse> request : requests) {
                futures.add(workers.submit(() -> {
                    ready.countDown();
                    if (!start.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting at the request start barrier");
                    }
                    return request.call();
                }));
            }

            assertTrue(ready.await(15, TimeUnit.SECONDS), "All request workers should reach the start barrier");
            start.countDown();

            List<ApiResponse> responses = new ArrayList<>();
            for (Future<ApiResponse> future : futures) {
                responses.add(future.get(45, TimeUnit.SECONDS));
            }
            return responses;
        } finally {
            start.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(15, TimeUnit.SECONDS), "Request workers should stop cleanly");
        }
    }

    private JsonNode json(ApiResponse response) throws Exception {
        assertNotNull(response.body(), "Successful appointment responses must contain a body");
        return objectMapper.readTree(response.body());
    }

    private void assertExactlyOneCreatedAndOneConflict(List<ApiResponse> responses) {
        assertEquals(1, responses.stream().filter(response -> response.statusCode() == HttpStatus.CREATED.value()).count());
        assertEquals(1, responses.stream().filter(response -> response.statusCode() == HttpStatus.CONFLICT.value()).count());
    }

    private long countActiveDoctorSlot(Long doctorId, LocalDate date, LocalTime startTime) {
        Long count = jdbcTemplate.queryForObject("""
                select count(*) from appointment
                where doctor_id = ? and appointment_date = ? and start_time = ? and status <> 'CANCELLED'
                """, Long.class, doctorId, date, startTime);
        return count == null ? 0 : count;
    }

    private long countOverlappingPatientBookings(Long patientId, LocalDate date,
                                                 LocalTime startTime, LocalTime endTime) {
        Long count = jdbcTemplate.queryForObject("""
                select count(*) from appointment
                where patient_id = ? and appointment_date = ? and status <> 'CANCELLED'
                  and start_time < ? and end_time > ?
                """, Long.class, patientId, date, endTime, startTime);
        return count == null ? 0 : count;
    }

    private User findSeedUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Missing integration seed user: " + email));
    }

    private LocalDate nextWeekdayFromNow(int daysFromNow) {
        LocalDate date = LocalDate.now().plusDays(daysFromNow);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private LocalDate nextWednesdayFromNow(int daysFromNow) {
        return LocalDate.now().plusDays(daysFromNow)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
    }

    private record ApiResponse(int statusCode, String body) {
    }
}
