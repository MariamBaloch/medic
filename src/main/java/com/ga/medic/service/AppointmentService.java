package com.ga.medic.service;

import com.ga.medic.dto.request.AppointmentBookingRequest;
import com.ga.medic.dto.request.AppointmentFilterRequest;
import com.ga.medic.dto.request.AppointmentStatusCountFilterRequest;
import com.ga.medic.dto.request.CancelAppointmentRequest;
import com.ga.medic.dto.request.RescheduleAppointmentRequest;
import com.ga.medic.dto.response.*;
import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.enums.NotificationAction;
import com.ga.medic.enums.NotificationType;
import com.ga.medic.exception.ForbiddenActionException;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.exception.SlotNotAvailableException;
import com.ga.medic.exception.WaitlistOfferConflictException;
import com.ga.medic.mapper.AppointmentMapper;
import com.ga.medic.mapper.PageMapper;
import com.ga.medic.model.*;
import com.ga.medic.repository.AppointmentRepository;
import com.ga.medic.repository.AvailabilityRuleRepository;
import com.ga.medic.repository.DoctorProfileRepository;
import com.ga.medic.repository.PatientProfileRepository;
import com.ga.medic.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.ga.medic.specification.AppointmentSpecifications.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final AvailabilityRuleService availabilityRuleService;
    private final AppointmentMapper appointmentMapper;
    private final AuthenticatedUser authenticatedUser;
    private final PageMapper pageMapper;
    private final NotificationService notificationService;
    private final AppointmentSlotLockService slotLockService;
    private final AppointmentWaitlistService waitlistService;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Returns the authenticated patient's appointments with optional filters and pagination.
     *
     * @param doctorName optional case-insensitive partial match against the doctor's full name
     * @param filters  optional date and status filters
     * @param pageable pagination and sorting information
     * @return the requested page of appointment responses
     */
    @Transactional
    public PageResponse<AppointmentResponse> getPatientAppointments(String doctorName, AppointmentFilterRequest filters,
                                                                     Pageable pageable) {
        PatientProfile patient = authenticatedUser.get().user().getPatientProfile();

        Specification<Appointment> spec = patientAppointmentFilters(
                patient.getId(), doctorName, filters.getDateFrom(), filters.getDateTo(), filters.getSpecializationId())
                .and(withStatus(filters.getStatus()));

        Page<Appointment> appointments = appointmentRepository.findAll(spec, pageable);
        return pageMapper.toResponse(appointments, appointmentMapper::toResponse);
    }

    /**
     * Returns counts by status for the authenticated patient's matching appointments.
     *
     * @param doctorName optional case-insensitive partial match against the doctor's full name
     * @param filters optional date range and specialization filters
     * @return total and per-status counts, including zero counts
     */
    @Transactional
    public AppointmentStatusCountsResponse getPatientAppointmentStatusCounts(
            String doctorName, AppointmentStatusCountFilterRequest filters) {
        PatientProfile patient = authenticatedUser.get().user().getPatientProfile();
        Specification<Appointment> specification = patientAppointmentFilters(
                patient.getId(), doctorName, filters.getDateFrom(), filters.getDateTo(), filters.getSpecializationId());
        return toStatusCounts(appointmentRepository.countByStatus(specification));
    }

    /**
     * Returns a single appointment belonging to the authenticated patient or doctor, including both profiles.
     *
     * @param appointmentId the ID of the appointment to retrieve
     * @return the appointment details
     */
    @Transactional
    public AppointmentDetailResponse getAppointmentDetails(Long appointmentId) {
        User currentUser = authenticatedUser.get().user();
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new InformationNotFoundException("Appointment with id " + appointmentId + " not found"));

        boolean isPatient = appointment.getPatient().getUser().getId().equals(currentUser.getId());
        boolean isDoctor = appointment.getDoctor().getUser().getId().equals(currentUser.getId());
        if (!isPatient && !isDoctor) {
            throw new ForbiddenActionException("You can only view appointments you are associated with");
        }

        return appointmentMapper.toDetailResponse(appointment);
    }

    /**
     * Returns the authenticated doctor's appointments with optional filters and pagination.
     *
     * @param patientName optional case-insensitive partial match against the patient's full name
     * @param filters   optional date and status filters
     * @param pageable  pagination and sorting information
     * @return the requested page of appointment responses
     */
    @Transactional
    public PageResponse<AppointmentResponse> getDoctorAppointments(String patientName, AppointmentFilterRequest filters,
                                                                   Pageable pageable) {
        DoctorProfile doctor = authenticatedUser.get().user().getDoctorProfile();
        Specification<Appointment> spec = doctorAppointmentFilters(
                doctor.getId(), patientName, filters.getDateFrom(), filters.getDateTo(), filters.getSpecializationId())
                .and(withStatus(filters.getStatus()));

        Page<Appointment> appointments = appointmentRepository.findAll(spec, pageable);
        return pageMapper.toResponse(appointments, appointmentMapper::toResponse);
    }

    /**
     * Returns counts by status for the authenticated doctor's matching appointments.
     *
     * @param patientName optional case-insensitive partial match against the patient's full name
     * @param filters optional date range and specialization filters
     * @return total and per-status counts, including zero counts
     */
    @Transactional
    public AppointmentStatusCountsResponse getDoctorAppointmentStatusCounts(
            String patientName, AppointmentStatusCountFilterRequest filters) {
        DoctorProfile doctor = authenticatedUser.get().user().getDoctorProfile();
        Specification<Appointment> specification = doctorAppointmentFilters(
                doctor.getId(), patientName, filters.getDateFrom(), filters.getDateTo(), filters.getSpecializationId());
        return toStatusCounts(appointmentRepository.countByStatus(specification));
    }

    private Specification<Appointment> patientAppointmentFilters(Long patientId, String doctorName,
                                                                  LocalDate dateFrom, LocalDate dateTo,
                                                                  Long specializationId) {
        return Specification.where(forPatient(patientId))
                .and(withOptionalDoctorName(doctorName))
                .and(fromDate(dateFrom))
                .and(toDate(dateTo))
                .and(withSpecialization(specializationId));
    }

    private Specification<Appointment> doctorAppointmentFilters(Long doctorId, String patientName,
                                                                  LocalDate dateFrom, LocalDate dateTo,
                                                                  Long specializationId) {
        return Specification.where(forDoctor(doctorId))
                .and(withOptionalPatientName(patientName))
                .and(fromDate(dateFrom))
                .and(toDate(dateTo))
                .and(withSpecialization(specializationId));
    }

    private AppointmentStatusCountsResponse toStatusCounts(Map<AppointmentStatusEnum, Long> matchingCounts) {
        Map<AppointmentStatusEnum, Long> counts = new EnumMap<>(AppointmentStatusEnum.class);
        for (AppointmentStatusEnum status : AppointmentStatusEnum.values()) {
            counts.put(status, matchingCounts.getOrDefault(status, 0L));
        }

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new AppointmentStatusCountsResponse(total, counts);
    }

    /**
     * Returns all appointment statuses with their display labels.
     *
     * @return the list of appointment status options
     */
    public List<AppointmentStatusResponse> getAppointmentStatuses() {
        return appointmentMapper.toStatusResponses(List.of(AppointmentStatusEnum.values()));
    }

    /**
     * Books an appointment for the authenticated patient after validating the doctor's slot and patient conflicts.
     *
     * @param request the doctor, date, and start time for the requested appointment
     * @return the saved appointment details
     */
    @Transactional(noRollbackFor = WaitlistOfferConflictException.class)
    public AppointmentResponse bookAppointment(AppointmentBookingRequest request) {
        PatientProfile authenticatedPatient = authenticatedUser.get().user().getPatientProfile();

        // Lock patient to prevent from booking overlapping appointments at the same time
        PatientProfile patient = patientProfileRepository.findByIdForUpdate(authenticatedPatient.getId())
                .orElseThrow(() -> new InformationNotFoundException("Patient profile not found"));

        slotLockService.acquire(request.doctorId(), request.date(), request.startTime());
        waitlistService.advanceExpiredOfferIfNeeded(request.doctorId(), request.date(), request.startTime());

        log.info("Booking appointment: patientId={}, doctorId={}, date={}, startTime={}",
                patient.getId(), request.doctorId(), request.date(), request.startTime());

        DoctorProfile doctor = doctorProfileRepository.findById(request.doctorId()).orElseThrow(() -> new InformationNotFoundException("Doctor not found"));
        List<AvailableSlotResponse> availableSlots = availabilityRuleService.getAvailability(doctor.getId(), request.date(), request.date());

        // Recheck the generated schedule and current booking state after acquiring the slot lock.
        AvailableSlotResponse matchingSlot = availableSlots.stream()
                .filter(slot -> slot.date().equals(request.date()) && slot.startTime().equals(request.startTime()))
                .findFirst()
                .orElseThrow(() -> new SlotNotAvailableException("The requested slot on " + request.date() + " at " + request.startTime() + " is not available. It may have been booked, is in the past, or does not match the doctor's schedule."));

        AvailabilityRule rule = availabilityRuleRepository.findById(matchingSlot.ruleId())
                .orElseThrow(() -> new InformationNotFoundException("Availability rule with id " + matchingSlot.ruleId() + " not found"));

        LocalTime endTime = matchingSlot.endTime();

        if (waitlistService.hasActiveOffer(doctor.getId(), request.date(), request.startTime())) {
            log.warn("Appointment booking blocked by waitlist offer: patientId={}, doctorId={}, date={}, startTime={}",
                    patient.getId(), doctor.getId(), request.date(), request.startTime());
            throw new WaitlistOfferConflictException("This slot is reserved by an active waitlist offer");
        }

        if (appointmentRepository.existsActiveByDoctorAndSlot(doctor.getId(), request.date(), request.startTime())) {
            log.warn("Appointment booking conflict: patientId={}, doctorId={}, date={}, startTime={}",
                    patient.getId(), doctor.getId(), request.date(), request.startTime());
            throw new SlotNotAvailableException("This slot has just been booked by another patient");
        }

        // Check if patient already has an appointment at this time
        if (appointmentRepository.existsOverlappingForPatient(patient.getId(), request.date(), request.startTime(), endTime)) {
            log.warn("Appointment booking overlap conflict: patientId={}, doctorId={}, date={}, startTime={}",
                    patient.getId(), doctor.getId(), request.date(), request.startTime());
            throw new SlotNotAvailableException("You already have an overlapping appointment at this time");
        }

        Appointment appointment = appointmentMapper.toEntity(request, doctor, patient, rule, endTime);

        // Flush here so the database's unique active-slot index rejects concurrent requests
        appointment = appointmentRepository.saveAndFlush(appointment);

        log.info("Appointment booked: appointmentId={}, patientId={}, doctorId={}, date={}, startTime={}",
                appointment.getId(), patient.getId(), doctor.getId(), request.date(), request.startTime());

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
     * Reschedules one future appointment owned by the authenticated patient to another dynamically generated slot.
     * The existing appointment row is updated atomically; a failed move leaves its original slot unchanged.
     *
     * @param appointmentId the appointment being moved
     * @param request       the requested date, start time, and version last observed by the patient
     * @return the updated appointment, including its new version
     */
    @Transactional(noRollbackFor = WaitlistOfferConflictException.class)
    public AppointmentResponse rescheduleAppointment(Long appointmentId, RescheduleAppointmentRequest request) {
        PatientProfile authenticatedPatient = authenticatedUser.get().user().getPatientProfile();
        if (authenticatedPatient == null) {
            throw new InformationNotFoundException("Patient profile not found");
        }

        // Lock patient to prevent from rescheduling overlapping appointments at the same time
        PatientProfile patient = patientProfileRepository.findByIdForUpdate(authenticatedPatient.getId())
                .orElseThrow(() -> new InformationNotFoundException("Patient profile not found"));
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new InformationNotFoundException("Appointment with id " + appointmentId + " not found"));

        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new ForbiddenActionException("You can only reschedule your own appointments");
        }
        if (appointment.getStatus() != AppointmentStatusEnum.BOOKED) {
            throw new IllegalArgumentException("Only booked appointments can be rescheduled");
        }

        LocalDate today = LocalDate.now();
        if (appointment.getAppointmentDate().isBefore(today)
                || (appointment.getAppointmentDate().equals(today)
                && !appointment.getStartTime().isAfter(LocalTime.now()))) {
            throw new IllegalArgumentException("Only future appointments can be rescheduled");
        }

        // Check for optimistic locking conflict to prevent overwriting concurrent updates.
        if (!Objects.equals(appointment.getVersion(), request.expectedVersion())) {
            throw new ObjectOptimisticLockingFailureException(Appointment.class, appointmentId);
        }

        if (appointment.getAppointmentDate().equals(request.date())
                && appointment.getStartTime().equals(request.startTime())) {
            return appointmentMapper.toResponse(appointment);
        }

        DoctorProfile doctor = appointment.getDoctor();
        LocalDate initiallyObservedDate = appointment.getAppointmentDate();
        LocalTime initiallyObservedStart = appointment.getStartTime();
        slotLockService.acquireAll(List.of(
                new AppointmentSlotLockService.SlotKey(doctor.getId(), initiallyObservedDate, initiallyObservedStart),
                new AppointmentSlotLockService.SlotKey(doctor.getId(), request.date(), request.startTime())));
        entityManager.refresh(appointment);
        if (!appointment.getAppointmentDate().equals(initiallyObservedDate)
                || !appointment.getStartTime().equals(initiallyObservedStart)
                || appointment.getStatus() != AppointmentStatusEnum.BOOKED
                || !appointment.getPatient().getId().equals(patient.getId())
                || !Objects.equals(appointment.getVersion(), request.expectedVersion())) {
            throw new ObjectOptimisticLockingFailureException(Appointment.class, appointmentId);
        }
        LocalDate revalidatedToday = LocalDate.now();
        if (appointment.getAppointmentDate().isBefore(revalidatedToday)
                || (appointment.getAppointmentDate().equals(revalidatedToday)
                && !appointment.getStartTime().isAfter(LocalTime.now()))) {
            throw new IllegalArgumentException("Only future appointments can be rescheduled");
        }
        waitlistService.advanceExpiredOfferIfNeeded(doctor.getId(), request.date(), request.startTime());

        log.info("Rescheduling appointment: appointmentId={}, patientId={}, doctorId={}, from={}/{}, to={}/{}",
                appointmentId, patient.getId(), doctor.getId(), appointment.getAppointmentDate(),
                appointment.getStartTime(), request.date(), request.startTime());

        if (waitlistService.hasActiveOffer(doctor.getId(), request.date(), request.startTime())) {
            log.warn("Appointment reschedule blocked by waitlist offer: appointmentId={}, patientId={}, doctorId={}, date={}, startTime={}",
                    appointmentId, patient.getId(), doctor.getId(), request.date(), request.startTime());
            throw new WaitlistOfferConflictException("The requested time is reserved by an active waitlist offer");
        }

        AvailableSlotResponse targetSlot = availabilityRuleService
                .getAvailability(doctor.getId(), request.date(), request.date(), appointmentId)
                .stream()
                .filter(slot -> slot.date().equals(request.date()) && slot.startTime().equals(request.startTime()))
                .findFirst()
                .orElseThrow(() -> new SlotNotAvailableException(
                        "The requested slot on " + request.date() + " at " + request.startTime()
                                + " is not available. It may have been booked, is in the past, or does not match the doctor's schedule."));

        if (appointmentRepository.existsActiveByDoctorAndSlot(doctor.getId(), request.date(), request.startTime())) {
            throw new SlotNotAvailableException("This slot has just been booked by another patient");
        }

        if (appointmentRepository.existsOverlappingForPatientExcludingAppointment(
                patient.getId(), request.date(), targetSlot.startTime(), targetSlot.endTime(), appointmentId)) {
            throw new SlotNotAvailableException("You already have an overlapping appointment at this time");
        }

        AvailabilityRule rule = availabilityRuleRepository.findById(targetSlot.ruleId())
                .orElseThrow(() -> new InformationNotFoundException("Availability rule with id " + targetSlot.ruleId() + " not found"));

        LocalDate oldDate = appointment.getAppointmentDate();
        LocalTime oldStartTime = appointment.getStartTime();
        appointment.setAppointmentDate(targetSlot.date());
        appointment.setStartTime(targetSlot.startTime());
        appointment.setEndTime(targetSlot.endTime());
        appointment.setAvailabilityRule(rule);
        appointment = appointmentRepository.saveAndFlush(appointment);
        waitlistService.offerNextWaiting(doctor, oldDate, oldStartTime);

        log.info("Appointment rescheduled: appointmentId={}, patientId={}, doctorId={}, from={}/{}, to={}/{}",
                appointment.getId(), patient.getId(), doctor.getId(), oldDate, oldStartTime,
                appointment.getAppointmentDate(), appointment.getStartTime());

        notificationService.createAndSend(patient.getUser(), NotificationType.APPOINTMENT,
                NotificationAction.RESCHEDULED, "Appointment Rescheduled",
                "Your appointment with " + doctor.getUser().getFullName() + " has been rescheduled to "
                        + appointment.getAppointmentDate() + " at " + appointment.getStartTime() + ".",
                false, appointment.getId());

        notificationService.createAndSend(doctor.getUser(), NotificationType.APPOINTMENT,
                NotificationAction.RESCHEDULED, "Patient Rescheduled Appointment",
                patient.getUser().getFullName() + " rescheduled their appointment to "
                        + appointment.getAppointmentDate() + " at " + appointment.getStartTime() + ".",
                false, appointment.getId());

        return appointmentMapper.toResponse(appointment);
    }

    /**
     * Cancels an eligible appointment belonging to the authenticated patient and notifies the doctor.
     *
     * @param appointmentId the ID of the appointment to cancel
     * @param request       the cancellation reason
     * @return the updated appointment details
     */
    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId, CancelAppointmentRequest request) {
        Long currentUserId = authenticatedUser.getUserId();

        PatientProfile identity = authenticatedUser.get().user().getPatientProfile();
        if (identity == null) {
            throw new InformationNotFoundException("Patient profile not found");
        }
        PatientProfile patient = patientProfileRepository.findByIdForUpdate(identity.getId())
                .orElseThrow(() -> new InformationNotFoundException("Patient profile not found"));
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new InformationNotFoundException("Appointment with id " + appointmentId + " not found"));

        if (!appointment.getPatient().getUser().getId().equals(currentUserId)) {
            throw new ForbiddenActionException("You can only cancel your own appointments");
        }

        LocalDate observedDate = appointment.getAppointmentDate();
        LocalTime observedStart = appointment.getStartTime();
        DoctorProfile observedDoctor = appointment.getDoctor();
        slotLockService.acquire(observedDoctor.getId(), observedDate, observedStart);
        entityManager.refresh(appointment);
        if (!appointment.getAppointmentDate().equals(observedDate)
                || !appointment.getStartTime().equals(observedStart)
                || !appointment.getDoctor().getId().equals(observedDoctor.getId())) {
            throw new WaitlistOfferConflictException("This appointment changed while cancellation was being processed. Refresh and try again.");
        }

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
        appointment = appointmentRepository.saveAndFlush(appointment);
        waitlistService.offerNextWaiting(appointment.getDoctor(), appointment.getAppointmentDate(), appointment.getStartTime());
        log.info("Appointment cancelled: appointmentId={}, patientId={}, doctorId={}, date={}, startTime={}",
                appointment.getId(), patient.getId(), appointment.getDoctor().getId(),
                appointment.getAppointmentDate(), appointment.getStartTime());

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
     * @param status        the outcome, either COMPLETED or NO_SHOW
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
