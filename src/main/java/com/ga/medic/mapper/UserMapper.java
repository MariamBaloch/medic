package com.ga.medic.mapper;

import com.ga.medic.dto.UserRegistrationRequest;
import com.ga.medic.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    User toEntity(UserRegistrationRequest request);
}
