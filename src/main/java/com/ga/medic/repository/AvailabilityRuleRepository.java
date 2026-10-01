package com.ga.medic.repository;

import com.ga.medic.model.AvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {
    /**
     * Used for overlap detection when creating rules.
     */
    @Query("SELECT r FROM AvailabilityRule r WHERE r.doctor.id = :doctorId AND r.id <> :excludeId AND r.startDate <= :to AND (r.endDate IS NULL OR r.endDate >= :from)")
    List<AvailabilityRule> findOverlappingExcluding(@Param("doctorId") Long doctorId, @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("excludeId") Long excludeId);
}