package com.ga.medic.identity.mapper;

import com.ga.medic.identity.config.GlobalMapperConfig;
import com.ga.medic.identity.dto.request.DoctorProfileRequest;
import com.ga.medic.identity.dto.response.DoctorProfileResponse;
import com.ga.medic.identity.model.DoctorProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class, uses = SpecializationMapper.class)
public interface DoctorProfileMapper {

    @Mapping(target = "doctorProfileId", source = "id")
    @Mapping(target = "specialization", source = "specialization")
    @Mapping(target = "audit.createdAt", source = "createdAt")
    @Mapping(target = "audit.updatedAt", source = "updatedAt")
    DoctorProfileResponse toDoctorProfileResponse(DoctorProfile doctorProfile);

    @Mapping(target = "specialization.id", source = "specializationId")
    void updateDoctorProfile(DoctorProfileRequest request, @MappingTarget DoctorProfile profile);
}
