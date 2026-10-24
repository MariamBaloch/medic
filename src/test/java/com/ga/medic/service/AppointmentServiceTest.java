package com.ga.medic.service;

import com.ga.medic.dto.request.AppointmentBookingRequest;
import com.ga.medic.dto.request.CancelAppointmentRequest;
import com.ga.medic.dto.request.RescheduleAppointmentRequest;
import com.ga.medic.dto.response.AppointmentDetailResponse;
import com.ga.medic.dto.response.AvailableSlotResponse;
import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.exception.ForbiddenActionException;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.exception.SlotNotAvailableException;
import com.ga.medic.mapper.AppointmentMapper;
import com.ga.medic.mapper.NotificationMapper;
import com.ga.medic.mapper.PageMapper;
import com.ga.medic.model.*;
import com.ga.medic.repository.*;
import com.ga.medic.security.AuthenticatedUser;
import com.ga.medic.security.MyUserDetails;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AppointmentServiceTest {

    private final LocalDate DATE = LocalDate.now().plusDays(1);
    private final LocalTime START = LocalTime.of(9, 0);
    private final LocalTime END = LocalTime.of(9, 30);
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private PatientProfileRepository patientProfileRepository;
    @Mock
    private AvailabilityRuleRepository availabilityRuleRepository;
    @Mock
    private AvailabilityRuleService availabilityRuleService;
    @Mock
    private AvailabilityExceptionService availabilityExceptionService;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private AuthenticatedUser authenticatedUser;
    @Mock
    private PageMapper pageMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private MyUserDetails userDetails;
    @InjectMocks
    private AppointmentService appointmentService;
    private PatientProfile patient;
    private DoctorProfile doctor;

    @Before
    public void setUp() {
        User patientUser = new User();
        patientUser.setId(1L);

        patient = new PatientProfile();
        patient.setId(1L);
        patient.setUser(patientUser);
        patientUser.setPatientProfile(patient);

        User doctorUser = new User();
        doctorUser.setId(2L);

        doctor = new DoctorProfile();
        doctor.setId(2L);
        doctor.setUser(doctorUser);

        when(authenticatedUser.get()).thenReturn(userDetails);
        when(userDetails.user()).thenReturn(patientUser);
        when(patientProfileRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(patient));
    }

    @Test(expected = InformationNotFoundException.class)
    public void bookAppointment_shouldThrow_whenDoctorNotFound() {
        AppointmentBookingRequest request = mock(AppointmentBookingRequest.class);
        when(request.doctorId()).thenReturn(99L);
        when(doctorProfileRepository.findById(99L)).thenReturn(Optional.empty());

        appointmentService.bookAppointment(request);
    }

    @Test(expected = SlotNotAvailableException.class)
    public void bookAppointment_shouldThrow_whenNoMatchingSlot() {
        AppointmentBookingRequest request = mock(AppointmentBookingRequest.class);
        when(request.doctorId()).thenReturn(2L);
        when(request.date()).thenReturn(DATE);
        when(request.startTime()).thenReturn(START);
        when(doctorProfileRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(availabilityRuleService.getAvailability(any(), any(), any())).thenReturn(List.of());

        appointmentService.bookAppointment(request);
    }

    @Test
    public void bookAppointment_shouldSave_whenValid() {
        AppointmentBookingRequest request = mock(AppointmentBookingRequest.class);
        when(request.doctorId()).thenReturn(2L);
        when(request.date()).thenReturn(DATE);
        when(request.startTime()).thenReturn(START);
        AvailableSlotResponse slot = new AvailableSlotResponse(1L, DATE, START, END, 30);
        Appointment appointment = new Appointment();

        when(doctorProfileRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(availabilityRuleService.getAvailability(any(), any(), any())).thenReturn(List.of(slot));
        when(availabilityRuleRepository.findById(1L)).thenReturn(Optional.of(new AvailabilityRule()));
        when(appointmentRepository.existsOverlappingForPatient(any(), any(), any(), any())).thenReturn(false);
        when(appointmentRepository.existsActiveByDoctorAndSlot(any(), any(), any())).thenReturn(false);
        when(appointmentMapper.toEntity(any(), any(), any(), any(), any())).thenReturn(appointment);
        when(appointmentRepository.saveAndFlush(appointment)).thenReturn(appointment);

        appointmentService.bookAppointment(request);

        verify(patientProfileRepository).findByIdForUpdate(1L);
        verify(appointmentRepository).saveAndFlush(appointment);
    }

    @Test
    public void rescheduleAppointment_shouldUpdateExistingAppointment_whenTargetSlotIsAvailable() {
        Long appointmentId = 10L;
        LocalDate newDate = DATE.plusDays(1);
        LocalTime newStartTime = LocalTime.of(10, 0);
        LocalTime newEndTime = LocalTime.of(10, 30);
        AvailabilityRule targetRule = new AvailabilityRule();
        targetRule.setId(3L);

        Appointment appointment = new Appointment();
        appointment.setId(appointmentId);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAvailabilityRule(new AvailabilityRule());
        appointment.setAppointmentDate(DATE);
        appointment.setStartTime(START);
        appointment.setEndTime(END);
        appointment.setStatus(AppointmentStatusEnum.BOOKED);
        appointment.setVersion(0L);

        AvailableSlotResponse targetSlot = new AvailableSlotResponse(
                targetRule.getId(), newDate, newStartTime, newEndTime, 30);
        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(newDate, newStartTime, 0L);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(availabilityRuleService.getAvailability(doctor.getId(), newDate, newDate, appointmentId))
                .thenReturn(List.of(targetSlot));
        when(appointmentRepository.existsOverlappingForPatientExcludingAppointment(
                patient.getId(), newDate, newStartTime, newEndTime, appointmentId)).thenReturn(false);
        when(availabilityRuleRepository.findById(targetRule.getId())).thenReturn(Optional.of(targetRule));
        when(appointmentRepository.saveAndFlush(appointment)).thenReturn(appointment);

        appointmentService.rescheduleAppointment(appointmentId, request);

        assertEquals(newDate, appointment.getAppointmentDate());
        assertEquals(newStartTime, appointment.getStartTime());
        assertEquals(newEndTime, appointment.getEndTime());
        assertEquals(targetRule, appointment.getAvailabilityRule());
        verify(appointmentRepository).saveAndFlush(appointment);
        verify(notificationService, times(2)).createAndSend(
                any(), eq(com.ga.medic.enums.NotificationType.APPOINTMENT),
                eq(com.ga.medic.enums.NotificationAction.RESCHEDULED),
                anyString(), anyString(), eq(false), eq(appointmentId));
    }

    @Test(expected = SlotNotAvailableException.class)
    public void rescheduleAppointment_shouldKeepOriginalSlot_whenTargetIsUnavailable() {
        Long appointmentId = 10L;
        LocalDate targetDate = DATE.plusDays(1);
        Appointment appointment = createBookedAppointment(appointmentId, patient, 0L);
        LocalDate originalDate = appointment.getAppointmentDate();
        LocalTime originalStartTime = appointment.getStartTime();

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(availabilityRuleService.getAvailability(doctor.getId(), targetDate, targetDate, appointmentId))
                .thenReturn(List.of());

        try {
            appointmentService.rescheduleAppointment(
                    appointmentId,
                    new RescheduleAppointmentRequest(targetDate, LocalTime.of(10, 0), 0L));
        } finally {
            assertEquals(originalDate, appointment.getAppointmentDate());
            assertEquals(originalStartTime, appointment.getStartTime());
            verify(appointmentRepository, never()).saveAndFlush(any());
        }
    }

    @Test(expected = ForbiddenActionException.class)
    public void rescheduleAppointment_shouldRejectAppointmentOwnedByAnotherPatient() {
        Long appointmentId = 10L;
        User otherPatientUser = new User();
        otherPatientUser.setId(2L);
        PatientProfile otherPatient = new PatientProfile();
        otherPatient.setId(2L);
        otherPatient.setUser(otherPatientUser);

        Appointment appointment = createBookedAppointment(appointmentId, otherPatient, 0L);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        appointmentService.rescheduleAppointment(
                appointmentId,
                new RescheduleAppointmentRequest(DATE.plusDays(1), LocalTime.of(10, 0), 0L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rescheduleAppointment_shouldRejectCancelledAppointment() {
        Long appointmentId = 10L;
        Appointment appointment = createBookedAppointment(appointmentId, patient, 0L);
        appointment.setStatus(AppointmentStatusEnum.CANCELLED);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        appointmentService.rescheduleAppointment(
                appointmentId,
                new RescheduleAppointmentRequest(DATE.plusDays(1), LocalTime.of(10, 0), 0L));
    }

    @Test(expected = ObjectOptimisticLockingFailureException.class)
    public void rescheduleAppointment_shouldRejectStaleVersion() {
        Long appointmentId = 10L;
        Appointment appointment = createBookedAppointment(appointmentId, patient, 1L);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        appointmentService.rescheduleAppointment(
                appointmentId,
                new RescheduleAppointmentRequest(DATE.plusDays(1), LocalTime.of(10, 0), 0L));
    }

    private Appointment createBookedAppointment(Long appointmentId, PatientProfile owner, Long version) {
        Appointment appointment = new Appointment();
        appointment.setId(appointmentId);
        appointment.setPatient(owner);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(DATE);
        appointment.setStartTime(START);
        appointment.setEndTime(END);
        appointment.setStatus(AppointmentStatusEnum.BOOKED);
        appointment.setVersion(version);
        return appointment;
    }

    @Test
    public void getAppointmentDetails_shouldAllowOwningPatient() {
        Long appointmentId = 10L;
        Appointment appointment = createBookedAppointment(appointmentId, patient, 0L);
        AppointmentDetailResponse response = mock(AppointmentDetailResponse.class);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentMapper.toDetailResponse(appointment)).thenReturn(response);

        assertSame(response, appointmentService.getAppointmentDetails(appointmentId));
    }

    @Test
    public void getAppointmentDetails_shouldAllowOwningDoctor() {
        Long appointmentId = 10L;
        Appointment appointment = createBookedAppointment(appointmentId, patient, 0L);
        AppointmentDetailResponse response = mock(AppointmentDetailResponse.class);
        when(userDetails.user()).thenReturn(doctor.getUser());
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentMapper.toDetailResponse(appointment)).thenReturn(response);

        assertSame(response, appointmentService.getAppointmentDetails(appointmentId));
    }

    @Test(expected = ForbiddenActionException.class)
    public void getAppointmentDetails_shouldRejectUnrelatedUser() {
        Long appointmentId = 10L;
        Appointment appointment = createBookedAppointment(appointmentId, patient, 0L);
        User unrelatedUser = new User();
        unrelatedUser.setId(99L);
        when(userDetails.user()).thenReturn(unrelatedUser);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        appointmentService.getAppointmentDetails(appointmentId);
    }


    @Test(expected = InformationNotFoundException.class)
    public void cancelAppointment_shouldThrow_whenNotFound() {
        when(authenticatedUser.getUserId()).thenReturn(1L);
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        appointmentService.cancelAppointment(99L, new CancelAppointmentRequest("reason"));
    }

    @Test
    public void cancelAppointment_shouldSetStatusCancelled_whenValid() {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStatus(AppointmentStatusEnum.BOOKED);
        appointment.setAppointmentDate(DATE);
        appointment.setStartTime(START);

        when(authenticatedUser.getUserId()).thenReturn(1L);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenReturn(appointment);

        appointmentService.cancelAppointment(1L, new CancelAppointmentRequest("reason"));

        assertEquals(AppointmentStatusEnum.CANCELLED, appointment.getStatus());
        verify(appointmentRepository).save(appointment);
    }
}
