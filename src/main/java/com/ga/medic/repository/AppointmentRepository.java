package com.ga.medic.repository;

import com.ga.medic.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Non-canceled appointments for a doctor within a date range.
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.status <> AppointmentStatusEnum.CANCELLED AND a.appointmentDate >= :from AND a.appointmentDate <= :to")
    List<Appointment> findActiveByDoctorAndDateRange(@Param("doctorId") Long doctorId, @Param("from") LocalDate from, @Param("to") LocalDate to);

}