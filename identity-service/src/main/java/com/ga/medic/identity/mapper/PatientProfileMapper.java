package com.ga.medic.identity.mapper;

import com.ga.medic.identity.config.GlobalMapperConfig;
import com.ga.medic.identity.dto.request.PatientProfileRequest;
import com.ga.medic.identity.dto.response.PatientProfileResponse;
import com.ga.medic.identity.model.PatientProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
public interface PatientProfileMapper {
    PatientProfile toPatientProfile(PatientProfileRequest request);


    @Mapping(target = "audit.createdAt", source = "createdAt")
    @Mapping(target = "audit.updatedAt", source = "updatedAt")
    PatientProfileResponse toPatientProfileResponse(PatientProfile patientProfile);

    void updatePatientProfile(PatientProfileRequest request, @MappingTarget PatientProfile profile);
}
