package com.ga.medic.common.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalProfileImageResponse(
        Long profileId,
        String imageUrl
) {
}
