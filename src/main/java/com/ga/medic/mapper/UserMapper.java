package com.ga.medic.mapper;

import com.ga.medic.config.GlobalMapperConfig;
import com.ga.medic.dto.request.UserRegistrationRequest;
import com.ga.medic.dto.response.UserAccountResponse;
import com.ga.medic.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class,
        uses = {DoctorProfileMapper.class, PatientProfileMapper.class})
public interface UserMapper {

    User toUser(UserRegistrationRequest request);

    @Mapping(source = "id", target = "userId")
    @Mapping(source = "role.name", target = "role")
    @Mapping(source = "patientProfile", target = "patient")
    @Mapping(source = "doctorProfile", target = "doctor")
    @Mapping(target = "audit.createdAt", source = "createdAt")
    @Mapping(target = "audit.updatedAt", source = "updatedAt")
    UserAccountResponse toResponse(User user);
}