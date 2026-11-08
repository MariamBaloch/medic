package com.ga.medic.mapper;

import com.ga.medic.config.GlobalMapperConfig;
import com.ga.medic.dto.response.SpecializationResponse;
import com.ga.medic.model.Specialization;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface SpecializationMapper {

    SpecializationResponse toResponse(Specialization specialization);

    List<SpecializationResponse> toResponses(List<Specialization> specializations);
}
