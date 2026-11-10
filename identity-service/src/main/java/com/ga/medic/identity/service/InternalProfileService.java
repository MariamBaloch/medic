package com.ga.medic.identity.service;

import com.ga.medic.common.contract.InternalDoctorSnapshotResponse;
import com.ga.medic.common.contract.InternalPatientSnapshotResponse;
import com.ga.medic.identity.exception.InformationNotFoundException;
import com.ga.medic.identity.model.DoctorProfile;
import com.ga.medic.identity.model.PatientProfile;
import com.ga.medic.identity.repository.DoctorProfileRepository;
import com.ga.medic.identity.repository.PatientProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalProfileService {

    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    @Transactional(readOnly = true)
    public InternalPatientSnapshotResponse getPatientSnapshot(Long patientProfileId) {
        PatientProfile profile = patientProfileRepository.findByIdWithUser(patientProfileId)
                .orElseThrow(() -> new InformationNotFoundException("Patient profile not found"));
        var user = profile.getUser();
        return new InternalPatientSnapshotResponse(
                profile.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail()
        );
    }

    @Transactional(readOnly = true)
    public InternalDoctorSnapshotResponse getDoctorSnapshot(Long doctorProfileId) {
        DoctorProfile profile = doctorProfileRepository.findByIdWithUserAndSpecialization(doctorProfileId)
                .orElseThrow(() -> new InformationNotFoundException("Doctor profile not found"));
        var user = profile.getUser();
        return new InternalDoctorSnapshotResponse(
                profile.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                profile.getSpecialization().getId(),
                profile.getConsultationFee()
        );
    }
}
