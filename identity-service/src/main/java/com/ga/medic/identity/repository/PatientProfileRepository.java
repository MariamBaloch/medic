package com.ga.medic.identity.repository;

import com.ga.medic.identity.model.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    Optional<PatientProfile> findByUserId(Long userId);

    @Query("""
            select p from PatientProfile p join fetch p.user where p.id = :id
            """)
    Optional<PatientProfile> findByIdWithUser(@Param("id") Long id);
}
