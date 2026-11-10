package com.ga.medic.mapper;

import com.ga.medic.client.dto.InternalDoctorSnapshotResponse;
import com.ga.medic.client.dto.InternalPatientSnapshotResponse;
import com.ga.medic.config.GlobalMapperConfig;
import com.ga.medic.dto.request.AppointmentBookingRequest;
import com.ga.medic.dto.response.AppointmentDoctorDetailResponse;
import com.ga.medic.dto.response.AppointmentDoctorResponse;
import com.ga.medic.dto.response.AppointmentDetailResponse;
import com.ga.medic.dto.response.AppointmentPatientDetailResponse;
import com.ga.medic.dto.response.AppointmentPatientResponse;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.AppointmentStatusResponse;
import com.ga.medic.dto.response.SpecializationResponse;
import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.AvailabilityRule;
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
            InternalDoctorSnapshotResponse doctor,
            InternalPatientSnapshotResponse patient
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

    default AppointmentDoctorDetailResponse toDoctorDetailResponse(InternalDoctorSnapshotResponse doctor) {
        SpecializationResponse specialization = doctor.doctorSpecializationId() == null
                ? null
                : new SpecializationResponse(doctor.doctorSpecializationId(), null, null);
        return new AppointmentDoctorDetailResponse(
                doctor.doctorId(),
                doctor.doctorName(),
                null,
                null,
                specialization,
                null,
                null,
                null,
                null,
                null
        );
    }

    default AppointmentPatientDetailResponse toPatientDetailResponse(InternalPatientSnapshotResponse patient) {
        return new AppointmentPatientDetailResponse(
                patient.patientId(),
                patient.patientName(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

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
