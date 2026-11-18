package com.ga.medic.booking.client;

import com.ga.medic.common.contract.InternalDoctorProfileResponse;
import com.ga.medic.common.contract.InternalDoctorSnapshotResponse;
import com.ga.medic.common.contract.InternalPatientProfileResponse;
import com.ga.medic.common.contract.InternalPatientSnapshotResponse;
import com.ga.medic.common.contract.InternalProfileImagesResponse;
import com.ga.medic.booking.exception.InformationNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalIdentityClient {

    private final RestClient identityRestClient;

    /**
     * Loads the patient snapshot booking stores on an appointment or waitlist row.
     *
     * @param patientProfileId the patient profile ID
     * @return the snapshot
     */
    public InternalPatientSnapshotResponse getPatientSnapshot(Long patientProfileId) {
        return get("/internal/patients/{id}", patientProfileId, InternalPatientSnapshotResponse.class, "Patient profile not found");
    }

    /**
     * Loads the current patient profile for appointment details.
     *
     * @param patientProfileId the patient profile ID
     * @return the live profile
     */
    public InternalPatientProfileResponse getPatientProfile(Long patientProfileId) {
        return get("/internal/patients/{id}/profile", patientProfileId, InternalPatientProfileResponse.class, "Patient profile not found");
    }

    /**
     * Loads the doctor snapshot booking stores on an appointment or waitlist row.
     *
     * @param doctorProfileId the doctor profile ID
     * @return the snapshot
     */
    public InternalDoctorSnapshotResponse getDoctorSnapshot(Long doctorProfileId) {
        return get("/internal/doctors/{id}", doctorProfileId, InternalDoctorSnapshotResponse.class, "Doctor profile not found");
    }

    /**
     * Loads the current doctor profile for appointment details.
     *
     * @param doctorProfileId the doctor profile ID
     * @return the live profile
     */
    public InternalDoctorProfileResponse getDoctorProfile(Long doctorProfileId) {
        return get("/internal/doctors/{id}/profile", doctorProfileId, InternalDoctorProfileResponse.class, "Doctor profile not found");
    }

    /**
     * Loads current profile photo paths for the given doctor and patient profile ids.
     *
     * @param doctorIds doctor profile ids, possibly empty
     * @param patientIds patient profile ids, possibly empty
     * @return the photos identity currently has
     */
    public InternalProfileImagesResponse getProfileImages(Collection<Long> doctorIds, Collection<Long> patientIds) {
        InternalProfileImagesResponse body = identityRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/internal/profile-images");
                    if (doctorIds != null) {
                        doctorIds.forEach(id -> uriBuilder.queryParam("doctorIds", id));
                    }
                    if (patientIds != null) {
                        patientIds.forEach(id -> uriBuilder.queryParam("patientIds", id));
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(InternalProfileImagesResponse.class);
        if (body == null) {
            return new InternalProfileImagesResponse(List.of(), List.of());
        }
        return new InternalProfileImagesResponse(
                body.doctors() == null ? List.of() : body.doctors(),
                body.patients() == null ? List.of() : body.patients()
        );
    }

    private <T> T get(String uri, Long id, Class<T> type, String notFoundMessage) {
        try {
            T body = identityRestClient.get()
                    .uri(uri, id)
                    .retrieve()
                    .body(type);
            if (body == null) {
                throw new InformationNotFoundException(notFoundMessage);
            }
            return body;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new InformationNotFoundException(notFoundMessage);
            }
            throw ex;
        }
    }
}
