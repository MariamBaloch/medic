package com.ga.medic.repository;

import com.ga.medic.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    /**
     * Non-canceled appointments for a doctor within a date range.
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.status <> AppointmentStatusEnum.CANCELLED AND a.appointmentDate >= :from AND a.appointmentDate <= :to")
    List<Appointment> findActiveByDoctorAndDateRange(@Param("doctorId") Long doctorId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<Appointment> findByAvailabilityRuleId(Long ruleId);

    /**
     * Retrieves a list of active, non-canceled appointments for a specific doctor on a given date.
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate = :date AND a.status <> AppointmentStatusEnum.CANCELLED")
    List<Appointment> findActiveByDoctorAndDate(@Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    /**
     * Retrieves a list of active, non-canceled appointments for a specific doctor on a given date and time range.
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate = :date AND a.status <> AppointmentStatusEnum.CANCELLED AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Appointment> findActiveByDoctorAndDateAndTimeRange(@Param("doctorId") Long doctorId, @Param("date") LocalDate date, @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

//    /**
//     * Retrieves a paginated list of appointments for a specific patient, filtered by optional parameters.
//     * The method supports filtering by doctor ID, appointment date range, and appointment status.
//     * If a filter parameter is provided as null, it is ignored in the query.
//     */
//    @Query("""
//            SELECT a FROM Appointment a
//            WHERE a.patient.id = :patientId
//              AND (:doctorId IS NULL OR a.doctor.id = :doctorId)
//              AND (:dateFrom IS NULL OR a.appointmentDate >= :dateFrom)
//              AND (:dateTo IS NULL OR a.appointmentDate <= :dateTo)
//              AND (:status IS NULL OR a.status = :status)
//            """)
//    Page<Appointment> findByPatientIdWithFilters(
//            @Param("patientId") Long patientId,
//            @Param("doctorId") Long doctorId,
//            @Param("dateFrom") LocalDate dateFrom,
//            @Param("dateTo") LocalDate dateTo,
//            @Param("status") AppointmentStatusEnum status,
//            Pageable pageable);
//
//    /**
//     * Retrieves a paginated list of appointments for a specific doctor, filtered by optional parameters.
//     * The method supports filtering by patient ID, appointment date range, and appointment status.
//     * If a filter parameter is provided as null, it is ignored in the query.
//     */
//    @Query("""
//            SELECT a FROM Appointment a
//            WHERE a.doctor.id = :doctorId
//              AND (:patientId IS NULL OR a.patient.id = :patientId)
//              AND (:dateFrom IS NULL OR a.appointmentDate >= :dateFrom)
//              AND (:dateTo IS NULL OR a.appointmentDate <= :dateTo)
//              AND (:status IS NULL OR a.status = :status)
//            """)
//    Page<Appointment> findByDoctorIdWithFilters(
//            @Param("doctorId") Long doctorId,
//            @Param("patientId") Long patientId,
//            @Param("dateFrom") LocalDate dateFrom,
//            @Param("dateTo") LocalDate dateTo,
//            @Param("status") AppointmentStatusEnum status,
//            Pageable pageable);

    /**
     * Check if a patient already has a not canceled appointment at the given time
     */
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.patient.id = :patientId AND a.appointmentDate = :date AND a.status <> AppointmentStatusEnum.CANCELLED AND a.startTime < :endTime AND a.endTime > :startTime")
    boolean existsOverlappingForPatient(@Param("patientId") Long patientId, @Param("date") LocalDate date, @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

    /**
     * Check if a specific slot is already taken (non-canceled).
     */
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate = :date AND a.startTime = :startTime AND a.status <> AppointmentStatusEnum.CANCELLED")
    boolean existsActiveByDoctorAndSlot(@Param("doctorId") Long doctorId, @Param("date") LocalDate date, @Param("startTime") LocalTime startTime);
}