package com.ga.medic.booking.mapper;

import com.ga.medic.booking.config.GlobalMapperConfig;
import com.ga.medic.booking.dto.request.AvailabilityExceptionRequest;
import com.ga.medic.booking.dto.response.AvailabilityExceptionDeleteResponse;
import com.ga.medic.booking.dto.response.AvailabilityExceptionResponse;
import com.ga.medic.booking.model.Appointment;
import com.ga.medic.booking.model.AvailabilityException;
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