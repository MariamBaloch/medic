package com.ga.medic.booking.repository;

import com.ga.medic.booking.enums.WaitlistStatusEnum;
import com.ga.medic.booking.model.AppointmentWaitlistEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Refreshes patient name/email snapshots for waitlist rows tied to a patient user id.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AppointmentWaitlistEntry e set e.patientName = :name, e.patientEmail = :email where e.patientUserId = :userId")
    int updatePatientSnapshotByUserId(@Param("userId") Long userId, @Param("name") String name, @Param("email") String email);

    /**
     * Refreshes doctor name/email snapshots for waitlist rows tied to a doctor user id.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AppointmentWaitlistEntry e set e.doctorName = :name, e.doctorEmail = :email where e.doctorUserId = :userId")
    int updateDoctorSnapshotByUserId(@Param("userId") Long userId, @Param("name") String name, @Param("email") String email);

    /**
     * Refreshes doctor display snapshots for a doctor profile.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AppointmentWaitlistEntry e
            set e.doctorName = :name,
                e.doctorEmail = :email,
                e.doctorSpecializationId = :specializationId,
                e.doctorSpecializationName = :specializationName
            where e.doctorId = :doctorId
            """)
    int updateDoctorSnapshotByDoctorId(@Param("doctorId") Long doctorId,
                                       @Param("name") String name,
                                       @Param("email") String email,
                                       @Param("specializationId") Long specializationId,
                                       @Param("specializationName") String specializationName);
}
