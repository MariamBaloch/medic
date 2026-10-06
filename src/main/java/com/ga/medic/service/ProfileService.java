package com.ga.medic.service;

import com.ga.medic.dto.request.DoctorProfileRequest;
import com.ga.medic.dto.request.PatientProfileRequest;
import com.ga.medic.dto.response.DoctorProfileResponse;
import com.ga.medic.dto.response.PatientProfileResponse;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.mapper.DoctorProfileMapper;
import com.ga.medic.mapper.PatientProfileMapper;
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.model.PatientProfile;
import com.ga.medic.repository.DoctorProfileRepository;
import com.ga.medic.repository.PatientProfileRepository;
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

    @Transactional
    public PatientProfileResponse updatePatientProfile(PatientProfileRequest request) {
        Long userId = authenticatedUser.getUserId();

        PatientProfile profile = patientProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new InformationNotFoundException("Patient profile not found"));

        patientProfileMapper.updatePatientProfile(request, profile);
        return patientProfileMapper.toPatientProfileResponse(profile);
    }

    @Transactional
    public DoctorProfileResponse updateDoctorProfile(DoctorProfileRequest request) {
        Long userId = authenticatedUser.getUserId();

        DoctorProfile profile = doctorProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new InformationNotFoundException("Doctor profile not found"));

        doctorProfileMapper.updateDoctorProfile(request, profile);
        return doctorProfileMapper.toDoctorProfileResponse(profile);
    }

    @Transactional
    public void verifyDoctor(Long doctorProfileId) {
        DoctorProfile profile = doctorProfileRepository.findById(doctorProfileId)
                .orElseThrow(() -> new InformationNotFoundException("Doctor profile not found"));

        profile.setIsVerified(true);
    }
}
