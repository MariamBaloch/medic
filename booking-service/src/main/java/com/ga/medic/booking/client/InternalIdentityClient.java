package com.ga.medic.booking.client;

import com.ga.medic.common.contract.InternalDoctorProfileResponse;
import com.ga.medic.common.contract.InternalDoctorSnapshotResponse;
import com.ga.medic.common.contract.InternalPatientProfileResponse;
import com.ga.medic.common.contract.InternalPatientSnapshotResponse;
import com.ga.medic.booking.exception.InformationNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

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
