package com.ga.medic.repository;

import com.ga.medic.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

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

    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);

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