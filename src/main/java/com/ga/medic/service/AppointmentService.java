package com.ga.medic.service;

import com.ga.medic.dto.request.AppointmentBookingRequest;
import com.ga.medic.dto.request.AppointmentFilterRequest;
import com.ga.medic.dto.request.CancelAppointmentRequest;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.AvailableSlotResponse;
import com.ga.medic.dto.response.PageResponse;
import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.enums.NotificationAction;
import com.ga.medic.enums.NotificationType;
import com.ga.medic.exception.ForbiddenActionException;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.exception.SlotNotAvailableException;
import com.ga.medic.mapper.AppointmentMapper;
import com.ga.medic.mapper.NotificationMapper;
import com.ga.medic.mapper.PageMapper;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.AvailabilityRule;
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.model.PatientProfile;
import com.ga.medic.repository.*;
import com.ga.medic.security.AuthenticatedUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

import static com.ga.medic.specification.AppointmentSpecifications.*;

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
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    /**
     * Returns the authenticated patient's appointments with optional filters and pagination.
     *
     * @param doctorId optional doctor ID to filter by
     * @param filters optional date and status filters
     * @param pageable pagination and sorting information
     * @return the requested page of appointment responses
     */
    @Transactional
    public PageResponse<AppointmentResponse> getPatientAppointments(Long doctorId, AppointmentFilterRequest filters,
                                                                    Pageable pageable) {
        PatientProfile patient = authenticatedUser.get().user().getPatientProfile();

        Specification<Appointment> spec = Specification
                .where(forPatient(patient.getId()))
                .and(withOptionalDoctor(doctorId))
                .and(fromDate(filters.getDateFrom()))
                .and(toDate(filters.getDateTo()))
                .and(withStatus(filters.getStatus()));

        Page<Appointment> appointments = appointmentRepository.findAll(spec, pageable);
        return pageMapper.toResponse(appointments, appointmentMapper::toResponse);
    }

    /**
     * Returns the authenticated doctor's appointments with optional filters and pagination.
     *
     * @param patientId optional patient ID to filter by
     * @param filters optional date and status filters
     * @param pageable pagination and sorting information
     * @return the requested page of appointment responses
     */
    @Transactional
    public PageResponse<AppointmentResponse> getDoctorAppointments(Long patientId, AppointmentFilterRequest filters,
                                                                   Pageable pageable) {
        DoctorProfile doctor = authenticatedUser.get().user().getDoctorProfile();
        Specification<Appointment> spec = Specification
                .where(forDoctor(doctor.getId()))
                .and(withOptionalPatient(patientId))
                .and(fromDate(filters.getDateFrom()))
                .and(toDate(filters.getDateTo()))
                .and(withStatus(filters.getStatus()));

        Page<Appointment> appointments = appointmentRepository.findAll(spec, pageable);
        return pageMapper.toResponse(appointments, appointmentMapper::toResponse);
    }

    /**
     * Books an appointment for the authenticated patient after validating the doctor's slot and patient conflicts.
     *
     * @param request the doctor, date, and start time for the requested appointment
     * @return the saved appointment details
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

        notificationService.createAndSend(doctor.getUser(), NotificationType.APPOINTMENT, NotificationAction.BOOKED,
                "New Appointment Booking",
                "You have a new appointment booking from " + patient.getUser().getFullName()
                        + " on " + request.date()
                        + " from " + request.startTime()
                        + " to " + endTime + ".",
                false, appointment.getId()
        );

        return appointmentMapper.toResponse(appointment);
    }

    /**
     * Cancels an eligible appointment belonging to the authenticated patient and notifies the doctor.
     *
     * @param appointmentId the ID of the appointment to cancel
     * @param request the cancellation reason
     * @return the updated appointment details
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

        if (appointment.getStatus() == AppointmentStatusEnum.COMPLETED
                || appointment.getStatus() == AppointmentStatusEnum.NO_SHOW) {
            throw new IllegalArgumentException("Cannot cancel a completed appointment");
        }

        appointment.setStatus(AppointmentStatusEnum.CANCELLED);
        appointment.setCancelledReason(request.reason());
        appointment = appointmentRepository.save(appointment);

        notificationService.createAndSend(
                appointment.getDoctor().getUser(),
                NotificationType.APPOINTMENT,
                NotificationAction.CANCELLED,
                "Appointment Cancelled",
                "Appointment cancelled by " + authenticatedUser.get().user().getFullName()
                        + " for " + appointment.getAppointmentDate()
                        + " at " + appointment.getStartTime() + ".",
                false,
                appointment.getId()
        );

        return appointmentMapper.toResponse(appointment);
    }

    /**
     * Updates a booked appointment owned by the authenticated doctor to a completed outcome.
     *
     * @param appointmentId the appointment to update
     * @param status the outcome, either COMPLETED or NO_SHOW
     * @return the updated appointment details
     */
    @Transactional
    public AppointmentResponse updateDoctorAppointmentStatus(Long appointmentId, AppointmentStatusEnum status) {
        if (status != AppointmentStatusEnum.COMPLETED && status != AppointmentStatusEnum.NO_SHOW) {
            throw new IllegalArgumentException("Appointment status must be COMPLETED or NO_SHOW");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new InformationNotFoundException("Appointment with id " + appointmentId + " not found"));

        Long doctorId = authenticatedUser.get().user().getDoctorProfile().getId();
        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new ForbiddenActionException("You can only update your own appointments");
        }

        if (appointment.getStatus() != AppointmentStatusEnum.BOOKED) {
            throw new IllegalArgumentException("Only booked appointments can be marked completed or no-show");
        }

        appointment.setStatus(status);
        appointment = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(appointment);
    }

}
