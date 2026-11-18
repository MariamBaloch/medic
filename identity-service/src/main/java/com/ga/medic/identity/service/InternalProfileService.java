package com.ga.medic.identity.service;

import com.ga.medic.common.contract.InternalDoctorProfileResponse;
import com.ga.medic.common.contract.InternalDoctorSnapshotResponse;
import com.ga.medic.common.contract.InternalPatientProfileResponse;
import com.ga.medic.common.contract.InternalPatientSnapshotResponse;
import com.ga.medic.common.contract.InternalProfileImageResponse;
import com.ga.medic.common.contract.InternalProfileImagesResponse;
import com.ga.medic.identity.exception.InformationNotFoundException;
import com.ga.medic.identity.model.DoctorProfile;
import com.ga.medic.identity.model.PatientProfile;
import com.ga.medic.identity.model.User;
import com.ga.medic.identity.repository.DoctorProfileRepository;
import com.ga.medic.identity.repository.PatientProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InternalProfileService {

    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final FileStorageService fileStorageService;

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
                fileStorageService.publicImageUrl(user.getImageUrl()),
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
                fileStorageService.publicImageUrl(user.getImageUrl()),
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

    /**
     * Returns the current public photo path for the requested doctor and patient profiles.
     *
     * @param doctorIds doctor profile ids
     * @param patientIds patient profile ids
     * @return photo paths, omitting ids that do not exist
     */
    @Transactional(readOnly = true)
    public InternalProfileImagesResponse getProfileImages(List<Long> doctorIds, List<Long> patientIds) {
        return new InternalProfileImagesResponse(doctorImages(doctorIds), patientImages(patientIds));
    }

    private List<InternalProfileImageResponse> doctorImages(List<Long> ids) {
        List<Long> distinct = distinctIds(ids);
        if (distinct.isEmpty()) {
            return List.of();
        }
        return doctorProfileRepository.findAllByIdInWithUser(distinct).stream()
                .map(profile -> new InternalProfileImageResponse(
                        profile.getId(),
                        fileStorageService.publicImageUrl(profile.getUser().getImageUrl())
                ))
                .toList();
    }

    private List<InternalProfileImageResponse> patientImages(List<Long> ids) {
        List<Long> distinct = distinctIds(ids);
        if (distinct.isEmpty()) {
            return List.of();
        }
        return patientProfileRepository.findAllByIdInWithUser(distinct).stream()
                .map(profile -> new InternalProfileImageResponse(
                        profile.getId(),
                        fileStorageService.publicImageUrl(profile.getUser().getImageUrl())
                ))
                .toList();
    }

    private static List<Long> distinctIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
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
