package com.ga.medic.identity.service;

import com.ga.medic.common.contract.InternalDoctorProfileResponse;
import com.ga.medic.common.contract.InternalDoctorSnapshotResponse;
import com.ga.medic.common.contract.InternalPatientProfileResponse;
import com.ga.medic.common.contract.InternalPatientSnapshotResponse;
import com.ga.medic.identity.exception.InformationNotFoundException;
import com.ga.medic.identity.model.DoctorProfile;
import com.ga.medic.identity.model.PatientProfile;
import com.ga.medic.identity.model.User;
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

    /**
     * Returns the patient fields booking stores on an appointment or waitlist row.
     *
     * @param patientProfileId the patient profile ID
     * @return the snapshot used at booking time
     */
    @Transactional(readOnly = true)
    public InternalPatientSnapshotResponse getPatientSnapshot(Long patientProfileId) {
        PatientProfile profile = loadPatient(patientProfileId);
        User user = profile.getUser();
        return new InternalPatientSnapshotResponse(
                profile.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail()
        );
    }

    /**
     * Returns the current patient profile for appointment details.
     *
     * @param patientProfileId the patient profile ID
     * @return the live profile
     */
    @Transactional(readOnly = true)
    public InternalPatientProfileResponse getPatientProfile(Long patientProfileId) {
        PatientProfile profile = loadPatient(patientProfileId);
        User user = profile.getUser();
        return new InternalPatientProfileResponse(
                profile.getId(),
                user.getFullName(),
                user.getImageUrl(),
                profile.getAddress(),
                profile.getCity(),
                profile.getCountry(),
                profile.getBloodType() == null ? null : profile.getBloodType().getValue(),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getAllergies(),
                profile.getChronicConditions()
        );
    }

    /**
     * Returns the doctor fields booking stores on an appointment or waitlist row.
     *
     * @param doctorProfileId the doctor profile ID
     * @return the snapshot used at booking time
     */
    @Transactional(readOnly = true)
    public InternalDoctorSnapshotResponse getDoctorSnapshot(Long doctorProfileId) {
        DoctorProfile profile = loadDoctor(doctorProfileId);
        User user = profile.getUser();
        var specialization = profile.getSpecialization();
        return new InternalDoctorSnapshotResponse(
                profile.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                specialization == null ? null : specialization.getId(),
                specialization == null ? null : specialization.getName(),
                profile.getConsultationFee()
        );
    }

    /**
     * Returns the current doctor profile for appointment details.
     *
     * @param doctorProfileId the doctor profile ID
     * @return the live profile
     */
    @Transactional(readOnly = true)
    public InternalDoctorProfileResponse getDoctorProfile(Long doctorProfileId) {
        DoctorProfile profile = loadDoctor(doctorProfileId);
        User user = profile.getUser();
        var specialization = profile.getSpecialization();
        return new InternalDoctorProfileResponse(
                profile.getId(),
                user.getFullName(),
                user.getImageUrl(),
                profile.getQualification(),
                specialization == null ? null : specialization.getId(),
                specialization == null ? null : specialization.getName(),
                specialization == null ? null : specialization.getDescription(),
                profile.getYearsOfExperience(),
                profile.getHospitalAffiliation(),
                profile.getClinicAddress(),
                profile.getLicenseNumber(),
                profile.getBio()
        );
    }

    private PatientProfile loadPatient(Long patientProfileId) {
        return patientProfileRepository.findByIdWithUser(patientProfileId)
                .orElseThrow(() -> new InformationNotFoundException("Patient profile not found"));
    }

    private DoctorProfile loadDoctor(Long doctorProfileId) {
        return doctorProfileRepository.findByIdWithUserAndSpecialization(doctorProfileId)
                .orElseThrow(() -> new InformationNotFoundException("Doctor profile not found"));
    }
}
