package com.ga.medic.repository;

import com.ga.medic.enums.WaitlistStatusEnum;
import com.ga.medic.model.AppointmentWaitlistEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Provides database operations for appointment waitlist entries.
 */
public interface AppointmentWaitlistEntryRepository extends JpaRepository<AppointmentWaitlistEntry, Long>,
        JpaSpecificationExecutor<AppointmentWaitlistEntry> {

    /**
     * Retrieves all waitlist entries for a patient, ordered from newest to oldest.
     */
    List<AppointmentWaitlistEntry> findAllByPatientIdOrderByCreatedAtDescIdDesc(Long patientId);

    /**
     * Retrieves waitlist entries for a specific doctor, slot, and status,
     * ordered by creation time from oldest to newest, then by ID.
     */
    List<AppointmentWaitlistEntry> findAllByDoctorIdAndAppointmentDateAndPreferredStartTimeAndStatusOrderByCreatedAtAscIdAsc(
            Long doctorId, LocalDate appointmentDate, LocalTime preferredStartTime, WaitlistStatusEnum status);

    /**
     * Retrieves a doctor's waitlist entries within a date range for a specific status.
     */
    List<AppointmentWaitlistEntry> findAllByDoctorIdAndAppointmentDateBetweenAndStatus(
            Long doctorId, LocalDate from, LocalDate to, WaitlistStatusEnum status);

    /**
     * Retrieves a patient's waitlist entries for a specific doctor within a date range
     * and matching any of the supplied statuses.
     */
    List<AppointmentWaitlistEntry> findAllByPatientIdAndDoctorIdAndAppointmentDateBetweenAndStatusIn(
            Long patientId, Long doctorId, LocalDate from, LocalDate to, Collection<WaitlistStatusEnum> statuses);

    /**
     * Retrieves a waitlist entry matching a specific doctor, slot, and status.
     */
    Optional<AppointmentWaitlistEntry> findFirstByDoctorIdAndAppointmentDateAndPreferredStartTimeAndStatus(
            Long doctorId, LocalDate appointmentDate, LocalTime preferredStartTime, WaitlistStatusEnum status);

    /**
     * Checks whether a patient already has a waitlist entry for a specific slot
     * with any of the supplied statuses.
     */
    boolean existsByPatientIdAndDoctorIdAndAppointmentDateAndPreferredStartTimeAndStatusIn(
            Long patientId, Long doctorId, LocalDate appointmentDate, LocalTime preferredStartTime,
            Collection<WaitlistStatusEnum> statuses);

    /**
     * Retrieves IDs of expired waitlist offers, ordered by expiration time from earliest to latest.
     */
    @Query("select entry.id from AppointmentWaitlistEntry entry " +
            "where entry.status = :status and entry.offerExpiresAt <= :now order by entry.offerExpiresAt asc")
    List<Long> findExpiredOfferIds(@Param("status") WaitlistStatusEnum status, @Param("now") Instant now, Pageable pageable);
}
