package com.ga.medic.booking.mapper;

import com.ga.medic.common.contract.InternalDoctorSnapshotResponse;
import com.ga.medic.common.contract.InternalPatientSnapshotResponse;
import com.ga.medic.booking.config.GlobalMapperConfig;
import com.ga.medic.booking.dto.request.AppointmentWaitlistRequest;
import com.ga.medic.booking.dto.response.AppointmentWaitlistEntryResponse;
import com.ga.medic.booking.dto.response.SpecializationResponse;
import com.ga.medic.booking.enums.WaitlistStatusEnum;
import com.ga.medic.booking.model.AppointmentWaitlistEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface AppointmentWaitlistEntryMapper {

    default AppointmentWaitlistEntry toEntity(
            AppointmentWaitlistRequest request,
            InternalDoctorSnapshotResponse doctor,
            InternalPatientSnapshotResponse patient
    ) {
        AppointmentWaitlistEntry entry = new AppointmentWaitlistEntry();
        entry.setAppointmentDate(request.date());
        entry.setPreferredStartTime(request.startTime());
        entry.setStatus(WaitlistStatusEnum.WAITING);
        entry.applyPartySnapshots(patient, doctor);
        return entry;
    }

    @Mapping(target = "doctorId", source = "doctorId")
    @Mapping(target = "doctorName", source = "doctorName")
    @Mapping(target = "specialization", expression = "java(specializationFromSnapshot(entry))")
    AppointmentWaitlistEntryResponse toResponse(AppointmentWaitlistEntry entry);

    List<AppointmentWaitlistEntryResponse> toResponseList(List<AppointmentWaitlistEntry> entries);

    default SpecializationResponse specializationFromSnapshot(AppointmentWaitlistEntry entry) {
        if (entry.getDoctorSpecializationId() == null
                && (entry.getDoctorSpecializationName() == null || entry.getDoctorSpecializationName().isBlank())) {
            return null;
        }
        return new SpecializationResponse(entry.getDoctorSpecializationId(), entry.getDoctorSpecializationName(), null);
    }
}
