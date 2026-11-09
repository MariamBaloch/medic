package com.ga.medic.mapper;

import com.ga.medic.config.GlobalMapperConfig;
import com.ga.medic.dto.request.AppointmentBookingRequest;
import com.ga.medic.dto.response.AppointmentDoctorResponse;
import com.ga.medic.dto.response.AppointmentDoctorDetailResponse;
import com.ga.medic.dto.response.AppointmentDetailResponse;
import com.ga.medic.dto.response.AppointmentPatientResponse;
import com.ga.medic.dto.response.AppointmentPatientDetailResponse;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.AppointmentStatusResponse;
import com.ga.medic.dto.response.SpecializationResponse;
import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.AvailabilityRule;
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.model.PatientProfile;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalTime;
import java.util.List;

@Mapper(config = GlobalMapperConfig.class, uses = SpecializationMapper.class)
public interface AppointmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "doctorId", ignore = true)
    @Mapping(target = "patientId", ignore = true)
    @Mapping(target = "patientUserId", ignore = true)
    @Mapping(target = "doctorUserId", ignore = true)
    @Mapping(target = "patientName", ignore = true)
    @Mapping(target = "patientEmail", ignore = true)
    @Mapping(target = "doctorName", ignore = true)
    @Mapping(target = "doctorEmail", ignore = true)
    @Mapping(target = "doctorSpecializationId", ignore = true)
    @Mapping(target = "appointmentDate", source = "request.date")
    @Mapping(target = "startTime", source = "request.startTime")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "bookingFee", source = "doctor.consultationFee")
    @Mapping(target = "status", constant = "BOOKED")
    @Mapping(target = "reason", source = "request.reason")
    @Mapping(target = "availabilityRuleId", source = "rule.id")
    @Mapping(target = "cancelledReason", ignore = true)
    @Mapping(target = "version", ignore = true)
    Appointment toEntity(AppointmentBookingRequest request, DoctorProfile doctor, PatientProfile patient, AvailabilityRule rule, LocalTime endTime);

    @AfterMapping
    default void populateSnapshots(
            @MappingTarget Appointment appointment,
            DoctorProfile doctor,
            PatientProfile patient) {
        appointment.applyPartySnapshots(patient, doctor);
    }

    @Mapping(target = "doctor", expression = "java(doctorFromSnapshot(appointment))")
    @Mapping(target = "patient", expression = "java(patientFromSnapshot(appointment))")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "audit.createdAt", source = "createdAt")
    @Mapping(target = "audit.updatedAt", source = "updatedAt")
    AppointmentResponse toResponse(Appointment appointment);

    @Mapping(target = "doctorId", source = "id")
    @Mapping(target = "doctorName", source = "user.fullName")
    @Mapping(target = "doctorImageUrl", source = "user.imageUrl")
    @Mapping(target = "specialization", source = "specialization")
    @Mapping(target = "doctorQualification", source = "qualification")
    AppointmentDoctorResponse toDoctorResponse(DoctorProfile doctor);

    @Mapping(target = "patientId", source = "id")
    @Mapping(target = "patientName", source = "user.fullName")
    @Mapping(target = "patientImageUrl", source = "user.imageUrl")
    AppointmentPatientResponse toPatientResponse(PatientProfile patient);

    List<AppointmentResponse> toResponseList(List<Appointment> appointments);

    @Mapping(target = "doctor", source = "doctor")
    @Mapping(target = "patient", source = "patient")
    @Mapping(target = "version", source = "appointment.version")
    @Mapping(target = "appointmentDate", source = "appointment.appointmentDate")
    @Mapping(target = "startTime", source = "appointment.startTime")
    @Mapping(target = "endTime", source = "appointment.endTime")
    @Mapping(target = "bookingFee", source = "appointment.bookingFee")
    @Mapping(target = "status", source = "appointment.status")
    @Mapping(target = "reason", source = "appointment.reason")
    @Mapping(target = "cancelledReason", source = "appointment.cancelledReason")
    AppointmentDetailResponse toDetailResponse(Appointment appointment, DoctorProfile doctor, PatientProfile patient);

    @Mapping(target = "doctorName", source = "user.fullName")
    @Mapping(target = "doctorImageUrl", source = "user.imageUrl")
    @Mapping(target = "doctorQualification", source = "qualification")
    @Mapping(target = "specialization", source = "specialization")
    @Mapping(target = "doctorYearsOfExperience", source = "yearsOfExperience")
    @Mapping(target = "doctorHospitalAffiliation", source = "hospitalAffiliation")
    @Mapping(target = "doctorClinicAddress", source = "clinicAddress")
    @Mapping(target = "doctorLicenseNumber", source = "licenseNumber")
    @Mapping(target = "doctorBio", source = "bio")
    @Mapping(target = "doctorId", source = "id")
    AppointmentDoctorDetailResponse toDoctorDetailResponse(DoctorProfile doctor);

    @Mapping(target = "patientId", source = "id")
    @Mapping(target = "patientName", source = "user.fullName")
    @Mapping(target = "patientImageUrl", source = "user.imageUrl")
    AppointmentPatientDetailResponse toPatientDetailResponse(PatientProfile patient);

    @Mapping(target = "value", source = "status")
    @Mapping(target = "label", source = "label")
    AppointmentStatusResponse toStatusResponse(AppointmentStatusEnum status);

    List<AppointmentStatusResponse> toStatusResponses(List<AppointmentStatusEnum> statuses);

    default AppointmentDoctorResponse doctorFromSnapshot(Appointment appointment) {
        SpecializationResponse specialization = appointment.getDoctorSpecializationId() == null
                ? null
                : new SpecializationResponse(appointment.getDoctorSpecializationId(), null, null);
        return new AppointmentDoctorResponse(
                appointment.getDoctorId(),
                appointment.getDoctorName(),
                null,
                specialization,
                null
        );
    }

    default AppointmentPatientResponse patientFromSnapshot(Appointment appointment) {
        return new AppointmentPatientResponse(
                appointment.getPatientId(),
                appointment.getPatientName(),
                null
        );
    }
}
