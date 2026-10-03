package com.ga.medic.service;

import com.ga.medic.dto.request.AppointmentBookingRequest;
import com.ga.medic.dto.request.CancelAppointmentRequest;
import com.ga.medic.dto.request.AppointmentFilterRequest;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.AvailableSlotResponse;
import com.ga.medic.dto.response.PageResponse;
import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.exception.ForbiddenActionException;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.exception.SlotNotAvailableException;
import com.ga.medic.mapper.AppointmentMapper;
import com.ga.medic.mapper.PageMapper;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.AvailabilityRule;
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.model.PatientProfile;
import com.ga.medic.repository.AppointmentRepository;
import com.ga.medic.repository.AvailabilityRuleRepository;
import com.ga.medic.repository.DoctorProfileRepository;
import com.ga.medic.repository.PatientProfileRepository;
import com.ga.medic.security.AuthenticatedUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final AvailabilityRuleService availabilityRuleService;
    private final AvailabilityExceptionService availabilityExceptionService;
    private final AppointmentMapper appointmentMapper;
    private final AuthenticatedUser authenticatedUser;
    private final PageMapper pageMapper;

    @Transactional
    public PageResponse<AppointmentResponse> getPatientAppointments(Long doctorId, AppointmentFilterRequest filters,
                                                                     Pageable pageable) {
        PatientProfile patient = authenticatedUser.get().user().getPatientProfile();
        Page<Appointment> appointments = appointmentRepository.findByPatientIdWithFilters(
                patient.getId(), doctorId, filters.getDateFrom(), filters.getDateTo(), filters.getStatus(), pageable);
        return pageMapper.toResponse(appointments, appointmentMapper::toResponse);
    }

    @Transactional
    public PageResponse<AppointmentResponse> getDoctorAppointments(Long patientId, AppointmentFilterRequest filters,
                                                                    Pageable pageable) {
        DoctorProfile doctor = authenticatedUser.get().user().getDoctorProfile();
        Page<Appointment> appointments = appointmentRepository.findByDoctorIdWithFilters(
                doctor.getId(), patientId, filters.getDateFrom(), filters.getDateTo(), filters.getStatus(), pageable);
        return pageMapper.toResponse(appointments, appointmentMapper::toResponse);
    }

    /**
     * Books an appointment for the currently authenticated patient with the specified doctor and time slot.
     * Validates whether the requested slot is available and doesn't overlap with any existing appointments.
     */
    @Transactional
    public AppointmentResponse bookAppointment(AppointmentBookingRequest request) {
        PatientProfile patient = authenticatedUser.get().user().getPatientProfile();
        DoctorProfile doctor = doctorProfileRepository.findById(request.doctorId()).orElseThrow(() -> new InformationNotFoundException("Doctor not found"));
        List<AvailableSlotResponse> availableSlots = availabilityRuleService.getAvailability(doctor.getId(), request.date(), request.date());

        // Find the matching slot
        AvailableSlotResponse matchingSlot = availableSlots.stream()
                .filter(slot -> slot.date().equals(request.date()) && slot.startTime().equals(request.startTime()))
                .findFirst()
                .orElseThrow(() -> new SlotNotAvailableException("The requested slot on " + request.date() + " at " + request.startTime() + " is not available. It may have been booked, is in the past, or does not match the doctor's schedule."));

        AvailabilityRule rule = availabilityRuleRepository.findById(matchingSlot.ruleId())
                .orElseThrow(() -> new InformationNotFoundException("Availability rule with id " + matchingSlot.ruleId() + " not found"));

        LocalTime endTime = matchingSlot.endTime();

        // Check if patient already has an appointment at this time
        if (appointmentRepository.existsOverlappingForPatient(patient.getId(), request.date(), request.startTime(), endTime)) {
            throw new SlotNotAvailableException("You already have an overlapping appointment at this time");
        }

        // Double-check slot isn't taken (race condition guard)
        if (appointmentRepository.existsActiveByDoctorAndSlot(doctor.getId(), request.date(), request.startTime())) {
            throw new SlotNotAvailableException("This slot has just been booked by another patient");
        }

        Appointment appointment = appointmentMapper.toEntity(request, doctor, patient, rule, endTime);
        appointment = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(appointment);
    }

    /**
     * Cancels an appointment belonging to the authenticated patient.
     */
    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId, CancelAppointmentRequest request) {
        Long currentUserId = authenticatedUser.getUserId();

        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow(() -> new InformationNotFoundException("Appointment with id " + appointmentId + " not found"));

        if (!appointment.getPatient().getUser().getId().equals(currentUserId)) {
            throw new ForbiddenActionException("You can only cancel your own appointments");
        }

        if (appointment.getStatus() == AppointmentStatusEnum.CANCELLED) {
            throw new IllegalArgumentException("This appointment is already cancelled");
        }

        if (appointment.getStatus() == AppointmentStatusEnum.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel a completed appointment");
        }

        appointment.setStatus(AppointmentStatusEnum.CANCELLED);
        appointment.setCancelledReason(request.reason());
        appointment = appointmentRepository.save(appointment);

        return appointmentMapper.toResponse(appointment);
    }

}
