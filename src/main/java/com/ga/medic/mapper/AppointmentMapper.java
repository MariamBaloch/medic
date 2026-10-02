package com.ga.medic.mapper;

import com.ga.medic.config.GlobalMapperConfig;
import com.ga.medic.dto.request.AppointmentBookingRequest;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.AvailabilityRule;
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.model.PatientProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalTime;
import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface AppointmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "doctor", source = "doctor")
    @Mapping(target = "patient", source = "patient")
    @Mapping(target = "appointmentDate", source = "request.date")
    @Mapping(target = "startTime", source = "request.startTime")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "status", constant = "BOOKED")
    @Mapping(target = "reason", source = "request.reason")
    @Mapping(target = "availabilityRule", source = "rule")
    Appointment toEntity(AppointmentBookingRequest request, DoctorProfile doctor, PatientProfile patient, AvailabilityRule rule, LocalTime endTime);

    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.user.fullName")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", source = "patient.user.fullName")
    @Mapping(target = "audit.createdAt", source = "createdAt")
    @Mapping(target = "audit.updatedAt", source = "updatedAt")
    AppointmentResponse toResponse(Appointment appointment);

    List<AppointmentResponse> toResponseList(List<Appointment> appointments);
}