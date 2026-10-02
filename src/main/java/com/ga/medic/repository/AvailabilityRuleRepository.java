package com.ga.medic.repository;

import com.ga.medic.model.AvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {
    /**
     * Finds rules that overlap the given date range, excluding a specific rule.
     */
    @Query("SELECT r FROM AvailabilityRule r WHERE r.doctor.id = :doctorId AND r.id <> :excludeId AND r.startDate <= :to AND (r.endDate IS NULL OR r.endDate >= :from)")
    List<AvailabilityRule> findOverlappingExcluding(@Param("doctorId") Long doctorId, @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("excludeId") Long excludeId);

    @Query("SELECT r FROM AvailabilityRule r WHERE r.doctor.id = :doctorId AND (r.endDate IS NULL OR r.endDate >= :today) ORDER BY r.startDate")
    List<AvailabilityRule> findCurrentAndFuture(@Param("doctorId") Long doctorId, @Param("today") LocalDate today);

    Optional<AvailabilityRule> findByIdAndDoctorId(Long ruleId, Long id);


    /**
     * Finds availability rules for a specific doctor that overlap with a given date range.
     */
    @Query("SELECT r FROM AvailabilityRule r WHERE r.doctor.id = :doctorId AND r.startDate <= :to AND (r.endDate IS NULL OR r.endDate >= :from)")
    List<AvailabilityRule> findByDoctorIdAndDateRange(@Param("doctorId") Long doctorId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}