package com.ga.medic.identity.service;

import com.ga.medic.identity.dto.request.DoctorProfileRequest;
import com.ga.medic.identity.dto.request.PatientProfileRequest;
import com.ga.medic.identity.dto.response.DoctorProfileResponse;
import com.ga.medic.identity.dto.response.PatientProfileResponse;
import com.ga.medic.identity.exception.InformationNotFoundException;
import com.ga.medic.identity.mapper.DoctorProfileMapper;
import com.ga.medic.identity.mapper.PatientProfileMapper;
import com.ga.medic.identity.model.DoctorProfile;
import com.ga.medic.identity.model.PatientProfile;
import com.ga.medic.identity.repository.DoctorProfileRepository;
import com.ga.medic.identity.repository.PatientProfileRepository;
import com.ga.medic.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final AuthenticatedUser authenticatedUser;
    private final PatientProfileMapper patientProfileMapper;
    private final DoctorProfileMapper doctorProfileMapper;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final IdentityEventService identityEventService;

    /**
     * Updates the authenticated patient's profile fields.
     *
     * @param request the patient profile values to apply
     * @return the updated patient profile
     */
    @Transactional
    public PatientProfileResponse updatePatientProfile(PatientProfileRequest request) {
        Long userId = authenticatedUser.getUserId();

        PatientProfile profile = patientProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new InformationNotFoundException("Patient profile not found"));

        patientProfileMapper.updatePatientProfile(request, profile);
        identityEventService.userUpdated(profile.getUser());
        return patientProfileMapper.toPatientProfileResponse(profile);
    }

    /**
     * Updates the authenticated doctor's profile fields.
     *
     * @param request the doctor profile values to apply
     * @return the updated doctor profile
     */
    @Transactional
    public DoctorProfileResponse updateDoctorProfile(DoctorProfileRequest request) {
        Long userId = authenticatedUser.getUserId();

        DoctorProfile profile = doctorProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new InformationNotFoundException("Doctor profile not found"));

        doctorProfileMapper.updateDoctorProfile(request, profile);
        identityEventService.doctorUpdated(profile);
        return doctorProfileMapper.toDoctorProfileResponse(profile);
    }

    /**
     * Sets the verified flag on the specified doctor's profile.
     *
     * @param doctorProfileId the ID of the doctor profile to verify
     */
    @Transactional
    public void verifyDoctor(Long doctorProfileId) {
        DoctorProfile profile = doctorProfileRepository.findById(doctorProfileId)
                .orElseThrow(() -> new InformationNotFoundException("Doctor profile not found"));

        profile.setIsVerified(true);
        identityEventService.doctorVerified(profile);
        identityEventService.doctorUpdated(profile);
    }
}
