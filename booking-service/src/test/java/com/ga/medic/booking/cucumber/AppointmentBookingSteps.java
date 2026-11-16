package com.ga.medic.booking.cucumber;

import com.ga.medic.booking.client.InternalIdentityClient;
import com.ga.medic.booking.model.AvailabilityRule;
import com.ga.medic.booking.repository.AppointmentRepository;
import com.ga.medic.booking.repository.AvailabilityRuleRepository;
import com.ga.medic.booking.support.IdentityTestData;
import com.ga.medic.booking.support.TestJwtFactory;
import com.ga.medic.common.contract.InternalDoctorSnapshotResponse;
import com.ga.medic.common.contract.InternalPatientSnapshotResponse;
import com.ga.medic.security.TokenBlacklistService;
import com.ga.medic.security.TokenVersionService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class AppointmentBookingSteps {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final LocalTime SLOT_START = LocalTime.of(10, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AvailabilityRuleRepository availabilityRuleRepository;

    @Autowired
    private InternalIdentityClient internalIdentityClient;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private TokenVersionService tokenVersionService;

    private ResultActions response;
    private LocalDate slotDate;
    private Long appointmentId;

    @Before
    public void resetBookings() {
        appointmentRepository.deleteAll();
        availabilityRuleRepository.deleteAll();
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);
        when(tokenVersionService.matches(anyLong(), anyInt())).thenReturn(true);
        when(internalIdentityClient.getDoctorSnapshot(anyLong())).thenReturn(IdentityTestData.seedDoctorOne());
        when(internalIdentityClient.getPatientSnapshot(anyLong())).thenAnswer(invocation -> {
            long patientId = invocation.getArgument(0);
            if (patientId == IdentityTestData.seedPatientTwo().patientId()) {
                return IdentityTestData.seedPatientTwo();
            }
            return IdentityTestData.seedPatientOne();
        });
    }

    @Given("a verified doctor {string} with an open slot next Monday at 10:00")
    public void doctorHasOpenSlot(String doctorName) {
        InternalDoctorSnapshotResponse doctor = IdentityTestData.seedDoctorOne();
        assertEquals(doctorName, doctor.doctorName());

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

    @Given("a registered patient {string}")
    public void registeredPatient(String patientName) {
        assertEquals(patientName, patientNamed(patientName).patientName());
    }

    @Given("{string} has booked that slot")
    public void patientHasBooked(String patientName) throws Exception {
        book(patientName);
        assertEquals(201, status(), body());
        appointmentId = json().path("id").asLong();
    }

    @When("{string} books that slot")
    public void patientBooks(String patientName) throws Exception {
        book(patientName);
    }

    @When("{string} cancels that booking")
    public void patientCancels(String patientName) throws Exception {
        response = mockMvc.perform(put("/patient/appointments/{id}/cancel", appointmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(patientName))
                .content("{\"reason\":\"No longer needed\"}"));
    }

    @When("a client lists patient appointments without a token")
    public void listAppointmentsWithoutToken() throws Exception {
        response = mockMvc.perform(get("/patient/appointments"));
    }

    @When("a client calls the health endpoint without a token")
    public void callHealth() throws Exception {
        response = mockMvc.perform(get("/actuator/health"));
    }

    @Then("the response status is {int}")
    public void responseStatus(int expected) throws Exception {
        assertEquals(expected, status(), body());
    }

    @Then("the health status is {string}")
    public void healthStatus(String expected) throws Exception {
        response.andExpect(jsonPath("$.status").value(expected));
    }

    @Then("the booking is confirmed for {string}")
    public void bookingConfirmed(String doctorName) throws Exception {
        assertEquals(201, status(), body());
        assertEquals("BOOKED", json().path("status").asText(), body());
        assertEquals(doctorName, json().path("doctor").path("doctorName").asText(), body());
    }

    @Then("the booking is rejected with a conflict")
    public void bookingRejected() throws Exception {
        assertEquals(409, status(), body());
        assertEquals(409, json().path("status").asInt(), body());
        assertEquals("Conflict", json().path("error").asText(), body());
    }

    @Then("the appointment is cancelled")
    public void appointmentCancelled() throws Exception {
        assertEquals(200, status(), body());
        assertEquals("CANCELLED", json().path("status").asText(), body());
    }

    @Then("the slot is no longer available")
    public void slotIsBooked() throws Exception {
        assertSlotStatus("BOOKED");
    }

    @Then("the slot is available again")
    public void slotIsAvailable() throws Exception {
        assertSlotStatus("AVAILABLE");
    }

    private void book(String patientName) throws Exception {
        response = mockMvc.perform(post("/patient/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(patientName))
                .content(slotJson()));
    }

    private void assertSlotStatus(String expected) throws Exception {
        response = mockMvc.perform(get("/patient/appointments/doctors/{doctorId}/availability",
                        IdentityTestData.seedDoctorOne().doctorId())
                .param("from", slotDate.format(DATE_FORMAT))
                .param("to", slotDate.format(DATE_FORMAT))
                .header(HttpHeaders.AUTHORIZATION, bearer(IdentityTestData.seedPatientOne().patientName())));
        assertEquals(200, status(), body());
        String start = SLOT_START.format(TIME_FORMAT);
        JsonNode match = null;
        for (JsonNode slot : json().path("slots")) {
            if (start.equals(slot.path("startTime").asText())) {
                match = slot;
            }
        }
        assertTrue(match != null, body());
        assertEquals(expected, match.path("status").asText(), body());
    }

    private String slotJson() {
        return """
                {"doctorId":%d,"date":"%s","startTime":"%s","reason":"Routine check-up"}
                """.formatted(
                IdentityTestData.seedDoctorOne().doctorId(),
                slotDate.format(DATE_FORMAT),
                SLOT_START.format(TIME_FORMAT));
    }

    private static String bearer(String patientName) {
        InternalPatientSnapshotResponse patient = patientNamed(patientName);
        String token = TestJwtFactory.patientToken(
                patient.patientUserId(),
                patient.patientEmail(),
                patient.patientName(),
                patient.patientId(),
                0);
        return "Bearer " + token;
    }

    private static InternalPatientSnapshotResponse patientNamed(String patientName) {
        if (patientName.equals(IdentityTestData.seedPatientTwo().patientName())) {
            return IdentityTestData.seedPatientTwo();
        }
        InternalPatientSnapshotResponse john = IdentityTestData.seedPatientOne();
        assertEquals(patientName, john.patientName());
        return john;
    }

    private int status() {
        return response.andReturn().getResponse().getStatus();
    }

    private String body() throws Exception {
        return response.andReturn().getResponse().getContentAsString();
    }

    private JsonNode json() throws Exception {
        return objectMapper.readTree(body());
    }
}
