package com.ga.medic.service;

import com.ga.medic.dto.request.AppointmentBookingRequest;
import com.ga.medic.dto.request.CancelAppointmentRequest;
import com.ga.medic.dto.response.AvailableSlotResponse;
import com.ga.medic.enums.AppointmentStatusEnum;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
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
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        appointmentService.bookAppointment(request);

        verify(appointmentRepository).save(appointment);
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