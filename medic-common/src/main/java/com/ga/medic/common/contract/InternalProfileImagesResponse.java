package com.ga.medic.common.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalProfileImagesResponse(
        List<InternalProfileImageResponse> doctors,
        List<InternalProfileImageResponse> patients
) {
}
