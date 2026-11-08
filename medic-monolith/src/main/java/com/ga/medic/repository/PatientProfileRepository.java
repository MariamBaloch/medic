package com.ga.medic.repository;

import com.ga.medic.model.PatientProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    Optional<PatientProfile> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select patient from PatientProfile patient where patient.id = :patientId")
    Optional<PatientProfile> findByIdForUpdate(@Param("patientId") Long patientId);
}
