package com.ga.medic.repository;

import com.ga.medic.model.DoctorProfile;
import com.ga.medic.enums.UserStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

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
              and (:specializationId is null or specialization.id = :specializationId)
              and (
                  :search = ''
                  or lower(doctor.firstName) like concat('%', lower(:search), '%')
                  or lower(doctor.lastName) like concat('%', lower(:search), '%')
                  or lower(doctor.email) like concat('%', lower(:search), '%')
              )
              and (
                  :hasFutureAvailability is null
                  or (
                      :hasFutureAvailability = true
                      and exists (
                          select rule.id
                          from AvailabilityRule rule
                          where rule.doctor = doctorProfile
                            and (rule.endDate is null or rule.endDate >= :today)
                      )
                  )
                  or (
                      :hasFutureAvailability = false
                      and not exists (
                          select rule.id
                          from AvailabilityRule rule
                          where rule.doctor = doctorProfile
                            and (rule.endDate is null or rule.endDate >= :today)
                      )
                  )
              )
            order by doctor.lastName, doctor.firstName
            """)
    List<DoctorProfile> findActiveDoctors(
            @Param("status") UserStatusEnum status,
            @Param("search") String search,
            @Param("specializationId") Long specializationId,
            @Param("hasFutureAvailability") Boolean hasFutureAvailability,
            @Param("today") LocalDate today
    );
}
