package com.ga.medic.identity.repository;

import com.ga.medic.identity.enums.UserStatusEnum;
import com.ga.medic.identity.model.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {
    Optional<DoctorProfile> findByUserId(Long userId);

    @Query("""
            select d from DoctorProfile d
            join fetch d.user
            join fetch d.specialization
            where d.id = :id
            """)
    Optional<DoctorProfile> findByIdWithUserAndSpecialization(@Param("id") Long id);

    boolean existsByLicenseNumber(String licenseNumber);

    @Query("""
            select doctorProfile
            from DoctorProfile doctorProfile
            join fetch doctorProfile.user doctor
            join fetch doctorProfile.specialization specialization
            where doctor.status = :status
              and doctor.deletedAt is null
              and (:specializationId is null or specialization.id = :specializationId)
              and (
                  :search = ''
                  or lower(doctor.firstName) like concat('%', lower(:search), '%')
                  or lower(doctor.lastName) like concat('%', lower(:search), '%')
                  or lower(doctor.email) like concat('%', lower(:search), '%')
              )
            order by doctor.lastName, doctor.firstName
            """)
    List<DoctorProfile> findActiveDoctors(
            @Param("status") UserStatusEnum status,
            @Param("search") String search,
            @Param("specializationId") Long specializationId
    );
}
