package com.ga.medic.mapper;

import com.ga.medic.config.GlobalMapperConfig;
import com.ga.medic.dto.request.AppointmentBookingRequest;
import com.ga.medic.dto.response.AppointmentDetailResponse;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.AppointmentStatusResponse;
import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.AvailabilityRule;
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.model.PatientProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalTime;
import java.util.List;

@Mapper(config = GlobalMapperConfig.class, uses = SpecializationMapper.class)
public interface AppointmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "doctor", source = "doctor")
    @Mapping(target = "patient", source = "patient")
    @Mapping(target = "appointmentDate", source = "request.date")
    @Mapping(target = "startTime", source = "request.startTime")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "bookingFee", source = "doctor.consultationFee")
    @Mapping(target = "status", constant = "BOOKED")
    @Mapping(target = "reason", source = "request.reason")
    @Mapping(target = "availabilityRule", source = "rule")
    Appointment toEntity(AppointmentBookingRequest request, DoctorProfile doctor, PatientProfile patient, AvailabilityRule rule, LocalTime endTime);

    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.user.fullName")
    @Mapping(target = "doctorImageUrl", source = "doctor.user.imageUrl")
    @Mapping(target = "specialization", source = "doctor.specialization")
    @Mapping(target = "doctorQualification", source = "doctor.qualification")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", source = "patient.user.fullName")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "audit.createdAt", source = "createdAt")
    @Mapping(target = "audit.updatedAt", source = "updatedAt")
    AppointmentResponse toResponse(Appointment appointment);

    List<AppointmentResponse> toResponseList(List<Appointment> appointments);

    @Mapping(target = "doctorName", source = "doctor.user.fullName")
    @Mapping(target = "doctorImageUrl", source = "doctor.user.imageUrl")
    @Mapping(target = "doctorQualification", source = "doctor.qualification")
    @Mapping(target = "specialization", source = "doctor.specialization")
    @Mapping(target = "doctorYearsOfExperience", source = "doctor.yearsOfExperience")
    @Mapping(target = "doctorHospitalAffiliation", source = "doctor.hospitalAffiliation")
    @Mapping(target = "doctorClinicAddress", source = "doctor.clinicAddress")
    @Mapping(target = "doctorLicenseNumber", source = "doctor.licenseNumber")
    @Mapping(target = "doctorBio", source = "doctor.bio")
    @Mapping(target = "version", source = "version")
    AppointmentDetailResponse toDetailResponse(Appointment appointment);

    @Mapping(target = "value", source = "status")
    @Mapping(target = "label", source = "label")
    AppointmentStatusResponse toStatusResponse(AppointmentStatusEnum status);

    List<AppointmentStatusResponse> toStatusResponses(List<AppointmentStatusEnum> statuses);
}
