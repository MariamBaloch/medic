package com.ga.medic.booking.repository;

import com.ga.medic.booking.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment>,
        AppointmentStatusCountRepository {

    /**
     * Non-canceled appointments for a doctor within a date range.
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.status <> AppointmentStatusEnum.CANCELLED AND a.appointmentDate >= :from AND a.appointmentDate <= :to")
    List<Appointment> findActiveByDoctorAndDateRange(@Param("doctorId") Long doctorId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Non-canceled appointments for a patient within a date range, used to calculate waitlist eligibility.
     */
    @Query("SELECT a FROM Appointment a WHERE a.patientId = :patientId AND a.status <> AppointmentStatusEnum.CANCELLED AND a.appointmentDate >= :from AND a.appointmentDate <= :to")
    List<Appointment> findActiveByPatientAndDateRange(@Param("patientId") Long patientId,
                                                      @Param("from") LocalDate from,
                                                      @Param("to") LocalDate to);

    List<Appointment> findByAvailabilityRuleId(Long ruleId);

    /**
     * Retrieves a list of active, non-canceled appointments for a specific doctor on a given date.
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.appointmentDate = :date AND a.status <> AppointmentStatusEnum.CANCELLED")
    List<Appointment> findActiveByDoctorAndDate(@Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    /**
     * Retrieves a list of active, non-canceled appointments for a specific doctor on a given date and time range.
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.appointmentDate = :date AND a.status <> AppointmentStatusEnum.CANCELLED AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Appointment> findActiveByDoctorAndDateAndTimeRange(@Param("doctorId") Long doctorId, @Param("date") LocalDate date, @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

    /**
     * Check if a patient already has a not canceled appointment at the given time
     */
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.patientId = :patientId AND a.appointmentDate = :date AND a.status <> AppointmentStatusEnum.CANCELLED AND a.startTime < :endTime AND a.endTime > :startTime")
    boolean existsOverlappingForPatient(@Param("patientId") Long patientId, @Param("date") LocalDate date, @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

    /**
     * Checks whether the patient has another non-canceled appointment that overlaps the requested time,
     * excluding the appointment being rescheduled.
     */
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.patientId = :patientId AND a.id <> :excludedAppointmentId AND a.appointmentDate = :date AND a.status <> AppointmentStatusEnum.CANCELLED AND a.startTime < :endTime AND a.endTime > :startTime")
    boolean existsOverlappingForPatientExcludingAppointment(@Param("patientId") Long patientId,
                                                            @Param("date") LocalDate date,
                                                            @Param("startTime") LocalTime startTime,
                                                            @Param("endTime") LocalTime endTime,
                                                            @Param("excludedAppointmentId") Long excludedAppointmentId);

    /**
     * Check if a specific slot is already taken (non-canceled).
     */
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctorId = :doctorId AND a.appointmentDate = :date AND a.startTime = :startTime AND a.status <> AppointmentStatusEnum.CANCELLED")
    boolean existsActiveByDoctorAndSlot(@Param("doctorId") Long doctorId, @Param("date") LocalDate date, @Param("startTime") LocalTime startTime);

    /**
     * Refreshes patient name/email snapshots for appointments tied to a patient user id.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Appointment a set a.patientName = :name, a.patientEmail = :email where a.patientUserId = :userId")
    int updatePatientSnapshotByUserId(@Param("userId") Long userId, @Param("name") String name, @Param("email") String email);

    /**
     * Refreshes doctor name/email snapshots for appointments tied to a doctor user id.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Appointment a set a.doctorName = :name, a.doctorEmail = :email where a.doctorUserId = :userId")
    int updateDoctorSnapshotByUserId(@Param("userId") Long userId, @Param("name") String name, @Param("email") String email);

    /**
     * Refreshes doctor display snapshots for a doctor profile.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Appointment a
            set a.doctorName = :name,
                a.doctorEmail = :email,
                a.doctorSpecializationId = :specializationId,
                a.doctorSpecializationName = :specializationName
            where a.doctorId = :doctorId
            """)
    int updateDoctorSnapshotByDoctorId(@Param("doctorId") Long doctorId,
                                       @Param("name") String name,
                                       @Param("email") String email,
                                       @Param("specializationId") Long specializationId,
                                       @Param("specializationName") String specializationName);
}
