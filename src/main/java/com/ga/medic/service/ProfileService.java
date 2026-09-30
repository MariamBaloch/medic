package com.ga.medic.service;

import com.ga.medic.dto.request.PatientProfileRequest;
import com.ga.medic.dto.response.PatientProfileResponse;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.mapper.PatientProfileMapper;
import com.ga.medic.model.PatientProfile;
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
    private final PatientProfileRepository patientProfileRepository;

    @Transactional
    public PatientProfileResponse updateUserProfile(PatientProfileRequest request) {
        Long userId = authenticatedUser.getUserId();

        PatientProfile profile = patientProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new InformationNotFoundException("Patient profile not found"));

        patientProfileMapper.updatePatientProfile(request, profile);
        return patientProfileMapper.toPatientProfileResponse(profile);
    }
}
