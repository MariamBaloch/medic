package com.ga.medic.booking.mapper;

import com.ga.medic.booking.config.GlobalMapperConfig;
import com.ga.medic.booking.dto.request.AvailabilityRuleRequest;
import com.ga.medic.booking.dto.response.AvailabilityRuleResponse;
import com.ga.medic.booking.model.AvailabilityRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface AvailabilityRuleMapper {

    void updateRuleFromRequest(AvailabilityRuleRequest request, @MappingTarget AvailabilityRule rule);

    AvailabilityRule toEntity(AvailabilityRuleRequest request);

    @Mapping(target = "audit.createdAt", source = "createdAt")
    @Mapping(target = "audit.updatedAt", source = "updatedAt")
    AvailabilityRuleResponse toResponse(AvailabilityRule rule);

    List<AvailabilityRuleResponse> toResponseList(List<AvailabilityRule> rules);
}