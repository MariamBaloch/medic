package com.ga.medic.mapper;

import com.ga.medic.config.GlobalMapperConfig;
import com.ga.medic.dto.request.AvailabilityExceptionRequest;
import com.ga.medic.dto.response.AvailabilityExceptionDeleteResponse;
import com.ga.medic.dto.response.AvailabilityExceptionResponse;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.AvailabilityException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class, uses = AppointmentMapper.class)
public interface AvailabilityExceptionMapper {

    AvailabilityException toEntity(AvailabilityExceptionRequest request);

    @Mapping(target = "audit.createdAt", source = "exception.createdAt")
    @Mapping(target = "audit.updatedAt", source = "exception.updatedAt")
    AvailabilityExceptionResponse toResponse(AvailabilityException exception, List<Appointment> affectedAppointments, String message);

    void updateExceptionFromRequest(AvailabilityExceptionRequest request, @MappingTarget AvailabilityException existing);

    AvailabilityExceptionDeleteResponse toDeleteResponse(List<Appointment> affectedAppointments, String message);

    List<AvailabilityExceptionResponse> toResponseList(List<AvailabilityException> exceptions);
}