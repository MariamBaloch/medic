package com.ga.medic.identity.mapper;

import com.ga.medic.identity.config.GlobalMapperConfig;
import com.ga.medic.identity.dto.response.SpecializationResponse;
import com.ga.medic.identity.model.Specialization;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface SpecializationMapper {

    SpecializationResponse toResponse(Specialization specialization);

    List<SpecializationResponse> toResponses(List<Specialization> specializations);
}
