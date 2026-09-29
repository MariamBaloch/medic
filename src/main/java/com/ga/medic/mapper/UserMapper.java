package com.ga.medic.mapper;

import com.ga.medic.dto.request.UserRegistrationRequest;
import com.ga.medic.dto.response.UserRegistrationResponse;
import com.ga.medic.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    User toUser(UserRegistrationRequest request);

    @Mapping(source = "id", target = "userId")
    @Mapping(source = "role.name", target = "role")
    UserRegistrationResponse toResponse(User user);
}