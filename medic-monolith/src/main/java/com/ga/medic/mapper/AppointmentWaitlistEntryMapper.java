package com.ga.medic.mapper;

import com.ga.medic.config.GlobalMapperConfig;
import com.ga.medic.dto.request.AppointmentWaitlistRequest;
import com.ga.medic.dto.response.AppointmentWaitlistEntryResponse;
import com.ga.medic.model.AppointmentWaitlistEntry;
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.model.PatientProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class, uses = SpecializationMapper.class)
public interface AppointmentWaitlistEntryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patient", source = "patient")
    @Mapping(target = "doctor", source = "doctor")
    @Mapping(target = "appointmentDate", source = "request.date")
    @Mapping(target = "preferredStartTime", source = "request.startTime")
    @Mapping(target = "status", constant = "WAITING")
    AppointmentWaitlistEntry toEntity(AppointmentWaitlistRequest request, DoctorProfile doctor, PatientProfile patient);

    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.user.fullName")
    @Mapping(target = "specialization", source = "doctor.specialization")
    AppointmentWaitlistEntryResponse toResponse(AppointmentWaitlistEntry entry);

    List<AppointmentWaitlistEntryResponse> toResponseList(List<AppointmentWaitlistEntry> entries);
}
