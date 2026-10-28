package com.ga.medic.service;

import com.ga.medic.dto.request.AppointmentBookingRequest;
import com.ga.medic.dto.request.AppointmentWaitlistFilterRequest;
import com.ga.medic.dto.request.AppointmentWaitlistRequest;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.AppointmentWaitlistEntryResponse;
import com.ga.medic.dto.response.AvailableSlotResponse;
import com.ga.medic.dto.response.PageResponse;
import com.ga.medic.enums.AvailableSlotStatusEnum;
import com.ga.medic.enums.NotificationAction;
import com.ga.medic.enums.NotificationType;
import com.ga.medic.enums.WaitlistStatusEnum;
import com.ga.medic.exception.ForbiddenActionException;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.exception.SlotNotAvailableException;
import com.ga.medic.exception.WaitlistOfferConflictException;
import com.ga.medic.mapper.AppointmentMapper;
import com.ga.medic.mapper.AppointmentWaitlistEntryMapper;
import com.ga.medic.mapper.PageMapper;
import com.ga.medic.model.*;
import com.ga.medic.repository.*;
import com.ga.medic.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.ga.medic.specification.AppointmentWaitlistEntrySpecifications.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentWaitlistService {

    private static final List<WaitlistStatusEnum> ACTIVE_ENTRY_STATUSES =
            List.of(WaitlistStatusEnum.WAITING, WaitlistStatusEnum.OFFERED);
    private static final long OFFER_WINDOW_MINUTES = 15;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort NEWEST_FIRST_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private static final DateTimeFormatter NOTIFICATION_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter NOTIFICATION_TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter OFFER_EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm:ss a", Locale.ENGLISH)
                    .withZone(ZoneOffset.UTC);

    private final AppointmentWaitlistEntryRepository waitlistRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AppointmentRepository appointmentRepository;
    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final AvailabilityRuleService availabilityRuleService;
    private final AppointmentSlotLockService slotLockService;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentWaitlistEntryMapper waitlistEntryMapper;
    private final PageMapper pageMapper;
    private final AuthenticatedUser authenticatedUser;
    private final NotificationService notificationService;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Adds the authenticated patient to the waitlist for a valid future slot that is not available for normal booking.
     *
     * @param request the requested doctor, date, and start time
     * @return the newly created waitlist entry
     */
    @Transactional
    public AppointmentWaitlistEntryResponse join(AppointmentWaitlistRequest request) {
        PatientProfile patientProfile = authenticatedUser.get().user().getPatientProfile();
        if (patientProfile == null) {
            throw new InformationNotFoundException("Patient profile not found");
        }
        PatientProfile patient = lockPatient(patientProfile.getId());

        DoctorProfile doctor = doctorProfileRepository.findById(request.doctorId())
                .orElseThrow(() -> new InformationNotFoundException("Doctor with id " + request.doctorId() + " not found"));
        if (!LocalDateTime.of(request.date(), request.startTime()).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Requested waitlist slot must be in the future");
        }

        slotLockService.acquire(doctor.getId(), request.date(), request.startTime());
        AvailableSlotResponse slot = findSlot(doctor.getId(), request.date(), request.startTime(), null);
        if (slot == null) {
            log.warn("Waitlist join rejected for invalid slot: patientId={}, doctorId={}, date={}, startTime={}",
                    patient.getId(), doctor.getId(), request.date(), request.startTime());
            throw new SlotNotAvailableException("The requested time is outside the doctor's current schedule or is no longer a valid future slot");
        }
        if (slot.status() == AvailableSlotStatusEnum.AVAILABLE) {
            log.info("Waitlist join rejected because normal booking is available: patientId={}, doctorId={}, date={}, startTime={}",
                    patient.getId(), doctor.getId(), request.date(), request.startTime());
            throw new WaitlistOfferConflictException("This slot is available to book normally; no waitlist entry was created");
        }
        if (appointmentRepository.existsOverlappingForPatient(patient.getId(), request.date(), slot.startTime(), slot.endTime())) {
            log.info("Waitlist join rejected due to patient overlap: patientId={}, doctorId={}, date={}, startTime={}",
                    patient.getId(), doctor.getId(), request.date(), request.startTime());
            throw new SlotNotAvailableException("You already have an overlapping appointment at this time");
        }
        if (waitlistRepository.existsByPatient_IdAndDoctor_IdAndAppointmentDateAndPreferredStartTimeAndStatusIn(
                patient.getId(), doctor.getId(), request.date(), request.startTime(), ACTIVE_ENTRY_STATUSES)) {
            log.info("Duplicate waitlist join rejected: patientId={}, doctorId={}, date={}, startTime={}",
                    patient.getId(), doctor.getId(), request.date(), request.startTime());
            throw new WaitlistOfferConflictException("You already have an active waitlist entry for this slot");
        }

        AppointmentWaitlistEntry entry = waitlistEntryMapper.toEntity(request, doctor, patient);
        entry = waitlistRepository.saveAndFlush(entry);
        log.info("Patient joined appointment waitlist: waitlistEntryId={}, patientId={}, doctorId={}, date={}, startTime={}",
                entry.getId(), patient.getId(), doctor.getId(), request.date(), request.startTime());

        return waitlistEntryMapper.toResponse(entry);
    }

    /**
     * Retrieves the authenticated patient's waitlist entries with optional doctor, date, and status filters.
     *
     * @param doctorName case-insensitive partial match against the doctor's full name
     * @param filters    optional requested-date range and status filters
     * @param pageable   page and sort options
     * @return a page of the patient's matching waitlist entries, newest first by default
     */
    @Transactional(readOnly = true)
    public PageResponse<AppointmentWaitlistEntryResponse> getMyEntries(
            String doctorName, AppointmentWaitlistFilterRequest filters, Pageable pageable) {
        if (pageable == null || pageable.isUnpaged() || pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        if (pageable.getPageSize() < 1 || pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }

        PatientProfile patient = authenticatedUser.get().user().getPatientProfile();
        if (patient == null) {
            throw new InformationNotFoundException("Patient profile not found");
        }

        Specification<AppointmentWaitlistEntry> specification = forPatient(patient.getId())
                .and(withOptionalDoctorName(doctorName))
                .and(fromDate(filters.getDateFrom()))
                .and(toDate(filters.getDateTo()))
                .and(withStatus(filters.getStatus()));
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : NEWEST_FIRST_SORT;
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<AppointmentWaitlistEntry> entries = waitlistRepository.findAll(specification, pageRequest);
        return pageMapper.toResponse(entries, waitlistEntryMapper::toResponse);
    }

    /**
     * Withdraws the authenticated patient from an active waitlist entry and advances the queue if needed.
     *
     * @param entryId the waitlist entry to withdraw
     */
    @Transactional
    public void withdraw(Long entryId) {
        AppointmentWaitlistEntry entry = getEntry(entryId);
        PatientProfile patientProfile = authenticatedUser.get().user().getPatientProfile();
        if (patientProfile == null) {
            throw new InformationNotFoundException("Patient profile not found");
        }
        PatientProfile patient = lockPatient(patientProfile.getId());
        assertOwner(entry, patient);

        slotLockService.acquire(entry.getDoctor().getId(), entry.getAppointmentDate(), entry.getPreferredStartTime());
        entityManager.refresh(entry);
        assertOwner(entry, patient);
        if (entry.getStatus() == WaitlistStatusEnum.WAITING) {
            entry.setStatus(WaitlistStatusEnum.CANCELLED);
            waitlistRepository.save(entry);
            log.info("Patient withdrew from appointment waitlist: waitlistEntryId={}, patientId={}", entryId, patient.getId());
            return;
        }
        if (entry.getStatus() == WaitlistStatusEnum.OFFERED) {
            boolean expired = entry.getOfferExpiresAt() == null || !entry.getOfferExpiresAt().isAfter(Instant.now());
            entry.setStatus(expired ? WaitlistStatusEnum.EXPIRED : WaitlistStatusEnum.CANCELLED);
            waitlistRepository.saveAndFlush(entry);
            log.info("Patient released appointment waitlist offer: waitlistEntryId={}, patientId={}, doctorId={}, date={}, startTime={}, expired={}",
                    entry.getId(), patient.getId(), entry.getDoctor().getId(), entry.getAppointmentDate(), entry.getPreferredStartTime(), expired);
            offerNextWaiting(entry.getDoctor(), entry.getAppointmentDate(), entry.getPreferredStartTime());
            return;
        }
        throw new WaitlistOfferConflictException("This waitlist entry is no longer active and cannot be withdrawn");
    }

    /**
     * Books the offered slot for its owner, provided the offer is still valid and the slot remains available.
     *
     * @param entryId the offered waitlist entry
     * @return the appointment created from the accepted offer
     */
    @Transactional(noRollbackFor = WaitlistOfferConflictException.class)
    public AppointmentResponse accept(Long entryId) {
        AppointmentWaitlistEntry entry = getEntry(entryId);
        PatientProfile patientProfile = authenticatedUser.get().user().getPatientProfile();
        if (patientProfile == null) {
            throw new InformationNotFoundException("Patient profile not found");
        }
        PatientProfile patient = lockPatient(patientProfile.getId());
        assertOwner(entry, patient);
        slotLockService.acquire(entry.getDoctor().getId(), entry.getAppointmentDate(), entry.getPreferredStartTime());
        entityManager.refresh(entry);
        assertOwner(entry, patient);

        if (entry.getStatus() != WaitlistStatusEnum.OFFERED) {
            log.warn("Waitlist acceptance rejected because entry is not offered: waitlistEntryId={}, patientId={}, status={}",
                    entryId, patient.getId(), entry.getStatus());
            throw new WaitlistOfferConflictException("This waitlist offer is no longer active");
        }

        Instant now = Instant.now();
        AvailableSlotResponse slot = findSlot(entry.getDoctor().getId(), entry.getAppointmentDate(), entry.getPreferredStartTime(), null);
        if (entry.getOfferExpiresAt() == null || !entry.getOfferExpiresAt().isAfter(now)) {
            entry.setStatus(WaitlistStatusEnum.EXPIRED);
            waitlistRepository.saveAndFlush(entry);
            log.info("Appointment waitlist offer expired during acceptance: waitlistEntryId={}, patientId={}", entryId, patient.getId());
            offerNextWaiting(entry.getDoctor(), entry.getAppointmentDate(), entry.getPreferredStartTime());
            throw new WaitlistOfferConflictException("This waitlist offer has expired");
        }

        if (slot == null || appointmentRepository.existsActiveByDoctorAndSlot(
                entry.getDoctor().getId(), entry.getAppointmentDate(), entry.getPreferredStartTime())) {
            entry.setStatus(WaitlistStatusEnum.EXPIRED);
            waitlistRepository.saveAndFlush(entry);
            log.warn("Waitlist offer could not be accepted because its slot is unavailable: waitlistEntryId={}, doctorId={}, date={}, startTime={}",
                    entryId, entry.getDoctor().getId(), entry.getAppointmentDate(), entry.getPreferredStartTime());
            throw new WaitlistOfferConflictException("The offered appointment slot is no longer available");
        }

        if (appointmentRepository.existsOverlappingForPatient(patient.getId(), entry.getAppointmentDate(), slot.startTime(), slot.endTime())) {
            entry.setStatus(WaitlistStatusEnum.CANCELLED);
            waitlistRepository.saveAndFlush(entry);
            log.info("Waitlist offer released because the patient now has an overlapping appointment: waitlistEntryId={}, patientId={}",
                    entryId, patient.getId());
            offerNextWaiting(entry.getDoctor(), entry.getAppointmentDate(), entry.getPreferredStartTime());
            throw new WaitlistOfferConflictException("You now have an overlapping appointment and can no longer accept this offer");
        }

        AvailabilityRule rule = availabilityRuleRepository.findById(slot.ruleId())
                .orElseThrow(() -> new InformationNotFoundException("Availability rule with id " + slot.ruleId() + " not found"));
        AppointmentBookingRequest booking = new AppointmentBookingRequest(
                entry.getDoctor().getId(), entry.getAppointmentDate(), entry.getPreferredStartTime(), null);
        Appointment appointment = appointmentMapper.toEntity(booking, entry.getDoctor(), patient, rule, slot.endTime());
        appointment = appointmentRepository.saveAndFlush(appointment);
        entry.setStatus(WaitlistStatusEnum.BOOKED);
        waitlistRepository.saveAndFlush(entry);

        notificationService.createAndSend(entry.getDoctor().getUser(), NotificationType.APPOINTMENT,
                NotificationAction.BOOKED, "Waitlist Appointment Booked",
                patient.getUser().getFullName() + " accepted a waitlist offer for " + entry.getAppointmentDate()
                        + " at " + entry.getPreferredStartTime() + ".",
                false, appointment.getId());
        notificationService.createAndSend(patient.getUser(), NotificationType.APPOINTMENT,
                NotificationAction.BOOKED, "Waitlist Offer Accepted",
                "Your appointment with " + entry.getDoctor().getUser().getFullName() + " is booked for "
                        + entry.getAppointmentDate() + " at " + entry.getPreferredStartTime() + ".",
                false, appointment.getId());
        log.info("Appointment waitlist offer accepted: waitlistEntryId={}, appointmentId={}, patientId={}, doctorId={}",
                entry.getId(), appointment.getId(), patient.getId(), entry.getDoctor().getId());
        return appointmentMapper.toResponse(appointment);
    }

    /**
     * Expires an elapsed offer for the specified slot and offers it to the next eligible patient.
     * Acquires the slot lock internally. When called during a patient booking/reschedule flow,
     * the patient lock must be acquired first.
     *
     * @param doctorId  the doctor who owns the slot
     * @param date      the slot date
     * @param startTime the slot start time
     */
    @Transactional
    public void advanceExpiredOfferIfNeeded(Long doctorId, LocalDate date, LocalTime startTime) {
        slotLockService.acquire(doctorId, date, startTime);
        AppointmentWaitlistEntry offered = waitlistRepository
                .findFirstByDoctor_IdAndAppointmentDateAndPreferredStartTimeAndStatus(
                        doctorId, date, startTime, WaitlistStatusEnum.OFFERED).orElse(null);

        if (offered == null || offered.getOfferExpiresAt() == null || offered.getOfferExpiresAt().isAfter(Instant.now())) {
            return;
        }

        offered.setStatus(WaitlistStatusEnum.EXPIRED);
        waitlistRepository.saveAndFlush(offered);
        log.info("Appointment waitlist offer expired: waitlistEntryId={}, doctorId={}, date={}, startTime={}",
                offered.getId(), doctorId, date, startTime);
        offerNextWaiting(offered.getDoctor(), date, startTime);
    }

    /**
     * Expires an elapsed offer by entry ID and advances the queue without taking a patient-profile lock.
     *
     * @param entryId the waitlist entry whose offer should be checked
     */
    @Transactional(noRollbackFor = WaitlistOfferConflictException.class)
    public void expireOffer(Long entryId) {
        AppointmentWaitlistEntry entry = getEntry(entryId);
        slotLockService.acquire(entry.getDoctor().getId(), entry.getAppointmentDate(), entry.getPreferredStartTime());
        entityManager.refresh(entry);
        if (entry.getStatus() != WaitlistStatusEnum.OFFERED || entry.getOfferExpiresAt() == null
                || entry.getOfferExpiresAt().isAfter(Instant.now())) {
            return;
        }
        entry.setStatus(WaitlistStatusEnum.EXPIRED);
        waitlistRepository.saveAndFlush(entry);
        log.info("Appointment waitlist offer expired by processor: waitlistEntryId={}, doctorId={}, date={}, startTime={}",
                entry.getId(), entry.getDoctor().getId(), entry.getAppointmentDate(), entry.getPreferredStartTime());
        offerNextWaiting(entry.getDoctor(), entry.getAppointmentDate(), entry.getPreferredStartTime());
    }

    /**
     * Offers the slot to the earliest eligible waiting patient.
     * Acquires the slot lock before checking the slot and waitlist queue.
     * Must be called within an active transaction.
     *
     * @param doctor    the doctor who owns the slot
     * @param date      the slot date
     * @param startTime the slot start time
     */
    public void offerNextWaiting(DoctorProfile doctor, LocalDate date, LocalTime startTime) {
        slotLockService.acquire(doctor.getId(), date, startTime);
        if (waitlistRepository.findFirstByDoctor_IdAndAppointmentDateAndPreferredStartTimeAndStatus(
                doctor.getId(), date, startTime, WaitlistStatusEnum.OFFERED).isPresent()
                || appointmentRepository.existsActiveByDoctorAndSlot(doctor.getId(), date, startTime)) {
            return;
        }

        AvailableSlotResponse slot = findSlot(doctor.getId(), date, startTime, null);
        if (slot == null) {
            return;
        }
        List<AppointmentWaitlistEntry> queue = waitlistRepository
                .findAllByDoctor_IdAndAppointmentDateAndPreferredStartTimeAndStatusOrderByCreatedAtAscIdAsc(
                        doctor.getId(), date, startTime, WaitlistStatusEnum.WAITING);

        // Skip candidates with an overlapping appointment, leaving them in the queue.
        for (AppointmentWaitlistEntry candidate : queue) {
            if (appointmentRepository.existsOverlappingForPatient(candidate.getPatient().getId(), date,
                    startTime, slot.endTime())) {
                continue;
            }
            Instant offeredAt = Instant.now();
            candidate.setStatus(WaitlistStatusEnum.OFFERED);
            candidate.setOfferedAt(offeredAt);
            candidate.setOfferExpiresAt(offeredAt.plusSeconds(OFFER_WINDOW_MINUTES * 60));
            candidate = waitlistRepository.saveAndFlush(candidate);

            String doctorName = doctor.getUser().getFullName();
            String appointmentDate = date.format(NOTIFICATION_DATE_FORMAT);
            String appointmentTime = startTime.format(NOTIFICATION_TIME_FORMAT);
            String offerExpiresAt = OFFER_EXPIRY_FORMAT.format(candidate.getOfferExpiresAt());
            String message = "A waitlist appointment with " + doctorName + " is available on " + appointmentDate
                    + " at " + appointmentTime + ". Please accept by " + offerExpiresAt + " to book this slot.";
            notificationService.createAndSend(candidate.getPatient().getUser(), NotificationType.WAITLIST,
                    NotificationAction.OFFERED, "Appointment Slot Available", message, false, candidate.getId());
            log.info("Appointment waitlist offer created: waitlistEntryId={}, patientId={}, doctorId={}, date={}, startTime={}, expiresAt={}",
                    candidate.getId(), candidate.getPatient().getId(), doctor.getId(), date, startTime, candidate.getOfferExpiresAt());
            return;
        }
    }

    /**
     * Checks whether a slot currently has an active waitlist offer.
     *
     * @param doctorId  the doctor who owns the slot
     * @param date      the slot date
     * @param startTime the slot start time
     * @return {@code true} if an offer exists; otherwise {@code false}
     */
    public boolean hasActiveOffer(Long doctorId, LocalDate date, LocalTime startTime) {
        return waitlistRepository.findFirstByDoctor_IdAndAppointmentDateAndPreferredStartTimeAndStatus(
                doctorId, date, startTime, WaitlistStatusEnum.OFFERED).isPresent();
    }

    /**
     * Loads and locks the patient profile before modifying patient-owned waitlist state.
     *
     * @param patientId the patient profile ID
     * @return the locked patient profile
     */
    private PatientProfile lockPatient(Long patientId) {
        return patientProfileRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new InformationNotFoundException("Patient profile not found"));
    }

    /**
     * Loads a waitlist entry or fails when the ID does not exist.
     *
     * @param entryId the waitlist entry ID
     * @return the matching waitlist entry
     */
    private AppointmentWaitlistEntry getEntry(Long entryId) {
        return waitlistRepository.findById(entryId)
                .orElseThrow(() -> new InformationNotFoundException("Waitlist entry with id " + entryId + " not found"));
    }

    /**
     * Ensures the supplied patient owns the waitlist entry.
     *
     * @param entry   the waitlist entry being managed
     * @param patient the authenticated patient's profile
     * @throws ForbiddenActionException if the patient does not own the entry
     */
    private void assertOwner(AppointmentWaitlistEntry entry, PatientProfile patient) {
        if (!Objects.equals(entry.getPatient().getId(), patient.getId())) {
            throw new ForbiddenActionException("You can only manage your own waitlist entries");
        }
    }

    /**
     * Finds the exact slot for a doctor and date, optionally excluding an appointment.
     *
     * @param doctorId              the doctor profile ID
     * @param date                  the slot date
     * @param startTime             the slot start time
     * @param excludedAppointmentId appointment ID to exclude from availability checks, or {@code null}
     * @return the matching scheduled slot, or {@code null} if no matching slot is found
     */
    private AvailableSlotResponse findSlot(Long doctorId, LocalDate date, LocalTime startTime, Long excludedAppointmentId) {
        return availabilityRuleService.getAvailability(doctorId, date, date, excludedAppointmentId).stream()
                .filter(slot -> slot.date().equals(date) && slot.startTime().equals(startTime))
                .findFirst().orElse(null);
    }

}
