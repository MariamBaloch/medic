package com.ga.medic.booking.mapper;

import com.ga.medic.booking.config.GlobalMapperConfig;
import com.ga.medic.booking.dto.request.AppointmentBookingRequest;
import com.ga.medic.booking.dto.response.*;
import com.ga.medic.booking.enums.AppointmentStatusEnum;
import com.ga.medic.booking.enums.BloodTypeEnum;
import com.ga.medic.booking.model.Appointment;
import com.ga.medic.booking.model.AvailabilityRule;
import com.ga.medic.common.contract.InternalDoctorProfileResponse;
import com.ga.medic.common.contract.InternalDoctorSnapshotResponse;
import com.ga.medic.common.contract.InternalPatientProfileResponse;
import com.ga.medic.common.contract.InternalPatientSnapshotResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalTime;
import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface AppointmentMapper {

    default Appointment toEntity(
            AppointmentBookingRequest request,
            InternalDoctorSnapshotResponse doctor,
            InternalPatientSnapshotResponse patient,
            AvailabilityRule rule,
            LocalTime endTime
    ) {
        Appointment appointment = new Appointment();
        appointment.setAppointmentDate(request.date());
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(endTime);
        appointment.setBookingFee(doctor.consultationFee());
        appointment.setStatus(AppointmentStatusEnum.BOOKED);
        appointment.setReason(request.reason());
        appointment.setAvailabilityRuleId(rule.getId());
        appointment.applyPartySnapshots(patient, doctor);
        return appointment;
    }

    @Mapping(target = "doctor", expression = "java(doctorFromSnapshot(appointment))")
    @Mapping(target = "patient", expression = "java(patientFromSnapshot(appointment))")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "audit.createdAt", source = "createdAt")
    @Mapping(target = "audit.updatedAt", source = "updatedAt")
    AppointmentResponse toResponse(Appointment appointment);

    List<AppointmentResponse> toResponseList(List<Appointment> appointments);

    default AppointmentDetailResponse toDetailResponse(
            Appointment appointment,
            InternalDoctorProfileResponse doctor,
            InternalPatientProfileResponse patient
    ) {
        return new AppointmentDetailResponse(
                appointment.getVersion(),
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getBookingFee(),
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getCancelledReason(),
                toDoctorDetailResponse(doctor),
                toPatientDetailResponse(patient)
        );
    }

    default AppointmentDoctorDetailResponse toDoctorDetailResponse(InternalDoctorProfileResponse doctor) {
        SpecializationResponse specialization = specialization(
                doctor.specializationId(),
                doctor.specializationName(),
                doctor.specializationDescription()
        );
        return new AppointmentDoctorDetailResponse(
                doctor.doctorId(),
                doctor.doctorName(),
                doctor.imageUrl(),
                doctor.qualification(),
                specialization,
                doctor.yearsOfExperience(),
                doctor.hospitalAffiliation(),
                doctor.clinicAddress(),
                doctor.licenseNumber(),
                doctor.bio()
        );
    }

    default AppointmentPatientDetailResponse toPatientDetailResponse(InternalPatientProfileResponse patient) {
        return new AppointmentPatientDetailResponse(
                patient.patientId(),
                patient.patientName(),
                patient.imageUrl(),
                patient.address(),
                patient.city(),
                patient.country(),
                BloodTypeEnum.fromValue(patient.bloodType()),
                patient.heightCm(),
                patient.weightKg(),
                patient.allergies(),
                patient.chronicConditions()
        );
    }

    @Mapping(target = "value", source = "status")
    @Mapping(target = "label", source = "label")
    AppointmentStatusResponse toStatusResponse(AppointmentStatusEnum status);

    List<AppointmentStatusResponse> toStatusResponses(List<AppointmentStatusEnum> statuses);

    default AppointmentDoctorResponse doctorFromSnapshot(Appointment appointment) {
        SpecializationResponse specialization = specialization(
                appointment.getDoctorSpecializationId(),
                appointment.getDoctorSpecializationName(),
                null
        );
        return new AppointmentDoctorResponse(
                appointment.getDoctorId(),
                appointment.getDoctorName(),
                null,
                specialization,
                null
        );
    }

    default SpecializationResponse specialization(Long id, String name, String description) {
        if (id == null && (name == null || name.isBlank())) {
            return null;
        }
        return new SpecializationResponse(id, name, description);
    }

    default AppointmentPatientResponse patientFromSnapshot(Appointment appointment) {
        return new AppointmentPatientResponse(
                appointment.getPatientId(),
                appointment.getPatientName(),
                null
        );
    }
}
