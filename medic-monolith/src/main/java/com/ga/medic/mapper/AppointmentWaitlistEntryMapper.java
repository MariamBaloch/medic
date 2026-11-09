package com.ga.medic.mapper;

import com.ga.medic.config.GlobalMapperConfig;
import com.ga.medic.dto.request.AppointmentWaitlistRequest;
import com.ga.medic.dto.response.AppointmentWaitlistEntryResponse;
import com.ga.medic.dto.response.SpecializationResponse;
import com.ga.medic.model.AppointmentWaitlistEntry;
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.model.PatientProfile;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class, uses = SpecializationMapper.class)
public interface AppointmentWaitlistEntryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patientId", ignore = true)
    @Mapping(target = "doctorId", ignore = true)
    @Mapping(target = "patientUserId", ignore = true)
    @Mapping(target = "doctorUserId", ignore = true)
    @Mapping(target = "patientName", ignore = true)
    @Mapping(target = "patientEmail", ignore = true)
    @Mapping(target = "doctorName", ignore = true)
    @Mapping(target = "doctorEmail", ignore = true)
    @Mapping(target = "doctorSpecializationId", ignore = true)
    @Mapping(target = "appointmentDate", source = "request.date")
    @Mapping(target = "preferredStartTime", source = "request.startTime")
    @Mapping(target = "status", constant = "WAITING")
    @Mapping(target = "offeredAt", ignore = true)
    @Mapping(target = "offerExpiresAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    AppointmentWaitlistEntry toEntity(AppointmentWaitlistRequest request, DoctorProfile doctor, PatientProfile patient);

    @AfterMapping
    default void populateSnapshots(
            @MappingTarget AppointmentWaitlistEntry entry,
            DoctorProfile doctor,
            PatientProfile patient) {
        entry.applyPartySnapshots(patient, doctor);
    }

    @Mapping(target = "doctorId", source = "doctorId")
    @Mapping(target = "doctorName", source = "doctorName")
    @Mapping(target = "specialization", expression = "java(specializationFromSnapshot(entry))")
    AppointmentWaitlistEntryResponse toResponse(AppointmentWaitlistEntry entry);

    List<AppointmentWaitlistEntryResponse> toResponseList(List<AppointmentWaitlistEntry> entries);

    default SpecializationResponse specializationFromSnapshot(AppointmentWaitlistEntry entry) {
        if (entry.getDoctorSpecializationId() == null) {
            return null;
        }
        return new SpecializationResponse(entry.getDoctorSpecializationId(), null, null);
    }
}
