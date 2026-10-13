package com.ga.medic.repository;

import com.ga.medic.model.DoctorProfile;
import com.ga.medic.enums.UserStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {
    Optional<DoctorProfile> findByUserId(Long userId);

    boolean existsByLicenseNumber(String licenseNumber);

    @Query("""
            select doctorProfile
            from DoctorProfile doctorProfile
            join fetch doctorProfile.user doctor
            join fetch doctorProfile.specialization specialization
            where doctor.status = :status
              and doctor.deletedAt is null
              and (
                  :search is null
                  or lower(doctor.firstName) like lower(concat('%', :search, '%'))
                  or lower(doctor.lastName) like lower(concat('%', :search, '%'))
                  or lower(doctor.email) like lower(concat('%', :search, '%'))
              )
            order by doctor.lastName, doctor.firstName
            """)
    List<DoctorProfile> findActiveDoctors(@Param("status") UserStatusEnum status, @Param("search") String search);
}
