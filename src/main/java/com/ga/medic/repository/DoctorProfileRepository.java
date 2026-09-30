package com.ga.medic.repository;

import com.ga.medic.model.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {
    Optional<DoctorProfile> findByUserId(Long userId);

    boolean existsByLicenseNumber(String licenseNumber);
}