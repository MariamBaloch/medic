package com.ga.medic.mapper;

import com.ga.medic.dto.request.DoctorProfileRequest;
import com.ga.medic.dto.response.DoctorProfileResponse;
import com.ga.medic.model.DoctorProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DoctorProfileMapper {

    @Mapping(target = "specializationId", source = "specialization.id")
    @Mapping(target = "audit.createdAt", source = "createdAt")
    @Mapping(target = "audit.updatedAt", source = "updatedAt")
    DoctorProfileResponse toDoctorProfileResponse(DoctorProfile doctorProfile);

    @Mapping(target = "specialization.id", source = "specializationId")
    void updateDoctorProfile(DoctorProfileRequest request, @MappingTarget DoctorProfile profile);
}
