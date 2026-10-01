package com.ga.medic.repository;

import com.ga.medic.model.AvailabilityException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, Long> {

    /**
     * Retrieves a list of availability exceptions for a specific doctor on a given date,
     * excluding the exception with the specified ID.
     */
    @Query("SELECT e FROM AvailabilityException e WHERE e.doctor.id = :doctorId AND e.exceptionDate = :date AND e.id <> :excludeId")
    List<AvailabilityException> findByDoctorAndDateExcluding(@Param("doctorId") Long doctorId, @Param("date") LocalDate date, @Param("excludeId") Long excludeId);

    Optional<AvailabilityException> findByIdAndDoctorId(Long exceptionId, Long id);

    @Query("SELECT e FROM AvailabilityException e WHERE e.doctor.id = :doctorId AND e.exceptionDate >= :today ORDER BY e.exceptionDate")
    List<AvailabilityException> findCurrentAndFutureByDoctorId(@Param("doctorId") Long doctorId, @Param("today") LocalDate today);
}