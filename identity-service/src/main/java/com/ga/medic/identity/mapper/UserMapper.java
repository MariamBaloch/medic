package com.ga.medic.identity.mapper;

import com.ga.medic.identity.config.GlobalMapperConfig;
import com.ga.medic.identity.dto.request.UserRegistrationRequest;
import com.ga.medic.identity.dto.response.UserAccountResponse;
import com.ga.medic.identity.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

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

    List<UserAccountResponse> toResponses(List<User> users);
}
