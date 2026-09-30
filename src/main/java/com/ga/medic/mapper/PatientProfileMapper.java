package com.ga.medic.mapper;

import com.ga.medic.config.GlobalMapperConfig;
import com.ga.medic.dto.request.PatientProfileRequest;
import com.ga.medic.dto.response.PatientProfileResponse;
import com.ga.medic.model.PatientProfile;
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
