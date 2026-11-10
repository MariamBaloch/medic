package com.ga.medic.client;

import com.ga.medic.client.dto.InternalDoctorSnapshotResponse;
import com.ga.medic.client.dto.InternalPatientSnapshotResponse;
import com.ga.medic.exception.InformationNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class InternalIdentityClient {

    private final RestClient identityRestClient;

    public InternalPatientSnapshotResponse getPatientSnapshot(Long patientProfileId) {
        try {
            InternalPatientSnapshotResponse body = identityRestClient.get()
                    .uri("/internal/patients/{id}", patientProfileId)
                    .retrieve()
                    .body(InternalPatientSnapshotResponse.class);
            if (body == null) {
                throw new InformationNotFoundException("Patient profile not found");
            }
            return body;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new InformationNotFoundException("Patient profile not found");
            }
            throw ex;
        }
    }

    public InternalDoctorSnapshotResponse getDoctorSnapshot(Long doctorProfileId) {
        try {
            InternalDoctorSnapshotResponse body = identityRestClient.get()
                    .uri("/internal/doctors/{id}", doctorProfileId)
                    .retrieve()
                    .body(InternalDoctorSnapshotResponse.class);
            if (body == null) {
                throw new InformationNotFoundException("Doctor profile not found");
            }
            return body;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new InformationNotFoundException("Doctor profile not found");
            }
            throw ex;
        }
    }
}
