package com.ga.medic.mapper;

import com.ga.medic.client.dto.InternalDoctorSnapshotResponse;
import com.ga.medic.client.dto.InternalPatientSnapshotResponse;
import com.ga.medic.config.GlobalMapperConfig;
import com.ga.medic.dto.event.BookingPartiesPayload;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.AppointmentWaitlistEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;

@Mapper(config = GlobalMapperConfig.class)
public interface OutboxPayloadMapper {

    @Mapping(target = "patientId", source = "patient.patientId")
    @Mapping(target = "doctorId", source = "doctor.doctorId")
    @Mapping(target = "patientUserId", source = "patient.patientUserId")
    @Mapping(target = "doctorUserId", source = "doctor.doctorUserId")
    @Mapping(target = "patientName", source = "patient.patientName")
    @Mapping(target = "patientEmail", source = "patient.patientEmail")
    @Mapping(target = "doctorName", source = "doctor.doctorName")
    @Mapping(target = "doctorEmail", source = "doctor.doctorEmail")
    BookingPartiesPayload toBookingParties(InternalPatientSnapshotResponse patient, InternalDoctorSnapshotResponse doctor);

    @Mapping(target = "patientId", source = "patientId")
    @Mapping(target = "doctorId", source = "doctorId")
    @Mapping(target = "patientUserId", source = "patientUserId")
    @Mapping(target = "doctorUserId", source = "doctorUserId")
    @Mapping(target = "patientName", source = "patientName")
    @Mapping(target = "patientEmail", source = "patientEmail")
    @Mapping(target = "doctorName", source = "doctorName")
    @Mapping(target = "doctorEmail", source = "doctorEmail")
    BookingPartiesPayload toBookingParties(Appointment appointment);

    @Mapping(target = "patientId", source = "patientId")
    @Mapping(target = "doctorId", source = "doctorId")
    @Mapping(target = "patientUserId", source = "patientUserId")
    @Mapping(target = "doctorUserId", source = "doctorUserId")
    @Mapping(target = "patientName", source = "patientName")
    @Mapping(target = "patientEmail", source = "patientEmail")
    @Mapping(target = "doctorName", source = "doctorName")
    @Mapping(target = "doctorEmail", source = "doctorEmail")
    BookingPartiesPayload toBookingParties(AppointmentWaitlistEntry entry);

    default Map<String, Object> bookingParties(InternalPatientSnapshotResponse patient, InternalDoctorSnapshotResponse doctor) {
        return toBookingParties(patient, doctor).asMap();
    }

    default Map<String, Object> bookingParties(Appointment appointment) {
        return toBookingParties(appointment).asMap();
    }

    default Map<String, Object> bookingParties(AppointmentWaitlistEntry entry) {
        return toBookingParties(entry).asMap();
    }
}
