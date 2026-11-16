package com.ga.medic.booking.concurrent;

import com.ga.medic.booking.client.InternalIdentityClient;
import com.ga.medic.booking.model.AvailabilityRule;
import com.ga.medic.booking.repository.AvailabilityRuleRepository;
import com.ga.medic.booking.support.IdentityTestData;
import com.ga.medic.booking.support.TestJwtFactory;
import com.ga.medic.common.contract.InternalDoctorSnapshotResponse;
import com.ga.medic.security.TokenBlacklistService;
import com.ga.medic.security.TokenVersionService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

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
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ConcurrentBookingSteps {

    private static final int PATIENTS = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final LocalTime SLOT_START = LocalTime.of(10, 0);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Autowired
    private Environment environment;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AvailabilityRuleRepository availabilityRuleRepository;

    @Autowired
    private InternalIdentityClient internalIdentityClient;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private TokenVersionService tokenVersionService;

    private LocalDate slotDate;
    private List<HttpResponse<String>> responses;

    @Given("a verified doctor {string} with an open slot next Monday at 10:00")
    public void doctorHasOpenSlot(String doctorName) {
        InternalDoctorSnapshotResponse doctor = IdentityTestData.seedDoctorOne();
        assertEquals(doctorName, doctor.doctorName());
        when(internalIdentityClient.getDoctorSnapshot(anyLong())).thenReturn(doctor);
        when(internalIdentityClient.getPatientSnapshot(anyLong())).thenAnswer(invocation -> {
            long patientId = invocation.getArgument(0);
            return IdentityTestData.patient(
                    patientId, 1_000 + patientId, "Patient " + patientId, "patient" + patientId + "@example.test");
        });
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);
        when(tokenVersionService.matches(anyLong(), anyInt())).thenReturn(true);

        slotDate = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        AvailabilityRule rule = new AvailabilityRule();
        rule.setDoctorId(doctor.doctorId());
        rule.setStartDate(slotDate);
        rule.setEndDate(slotDate);
        rule.setDaysOfWeek(EnumSet.of(DayOfWeek.MONDAY));
        rule.setStartTime(LocalTime.of(9, 0));
        rule.setEndTime(LocalTime.of(12, 0));
        rule.setSlotMinutes(30);
        availabilityRuleRepository.saveAndFlush(rule);
    }

    @When("10 different patients book that slot at the same time")
    public void tenPatientsBookTogether() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(PATIENTS);
        CountDownLatch ready = new CountDownLatch(PATIENTS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < PATIENTS; index++) {
                long patientId = 101L + index;
                String token = TestJwtFactory.patientToken(
                        1_000 + patientId,
                        "patient" + patientId + "@example.test",
                        "Patient " + patientId,
                        patientId,
                        0);
                futures.add(workers.submit(() -> {
                    ready.countDown();
                    if (!start.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start the bookings");
                    }
                    return book(token);
                }));
            }
            if (!ready.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Not every patient reached the start");
            }
            start.countDown();
            responses = new ArrayList<>();
            for (Future<HttpResponse<String>> future : futures) {
                responses.add(future.get(45, TimeUnit.SECONDS));
            }
        } finally {
            start.countDown();
            workers.shutdownNow();
        }
    }

    @Then("exactly 1 booking succeeds")
    public void oneBookingSucceeds() {
        assertEquals(1, countStatus(201), describeResponses());
        Long stored = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM appointment
                WHERE doctor_id = ? AND appointment_date = ? AND start_time = ? AND status = 'BOOKED'
                """, Long.class, IdentityTestData.seedDoctorOne().doctorId(), slotDate, SLOT_START);
        assertEquals(1L, stored);
    }

    @Then("the other 9 are rejected with a conflict")
    public void nineAreRejected() {
        assertEquals(9, countStatus(409), describeResponses());
    }

    private HttpResponse<String> book(String token) throws Exception {
        int port = environment.getRequiredProperty("local.server.port", Integer.class);
        String body = """
                {"doctorId":%d,"date":"%s","startTime":"%s","reason":"Concurrent booking"}
                """.formatted(
                IdentityTestData.seedDoctorOne().doctorId(),
                slotDate.format(DATE_FORMAT),
                SLOT_START.format(TIME_FORMAT));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/patient/appointments"))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private long countStatus(int status) {
        return responses.stream().filter(response -> response.statusCode() == status).count();
    }

    private String describeResponses() {
        StringBuilder description = new StringBuilder();
        for (HttpResponse<String> response : responses) {
            description.append(response.statusCode()).append(' ').append(response.body()).append('\n');
        }
        return description.toString();
    }
}
