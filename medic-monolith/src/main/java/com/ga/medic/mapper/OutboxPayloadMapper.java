package com.ga.medic.mapper;

import com.ga.medic.config.GlobalMapperConfig;
import com.ga.medic.dto.event.BookingPartiesPayload;
import com.ga.medic.model.Appointment;
import com.ga.medic.model.AppointmentWaitlistEntry;
import com.ga.medic.model.DoctorProfile;
import com.ga.medic.model.PatientProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;

/**
 * Builds outbox payload  from booking snapshots or live profiles
 */
@Mapper(config = GlobalMapperConfig.class)
public interface OutboxPayloadMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "patientUserId", source = "patient.user.id")
    @Mapping(target = "doctorUserId", source = "doctor.user.id")
    @Mapping(target = "patientName", source = "patient.user.fullName")
    @Mapping(target = "patientEmail", source = "patient.user.email")
    @Mapping(target = "doctorName", source = "doctor.user.fullName")
    @Mapping(target = "doctorEmail", source = "doctor.user.email")
    BookingPartiesPayload toBookingParties(PatientProfile patient, DoctorProfile doctor);

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

    default Map<String, Object> bookingParties(PatientProfile patient, DoctorProfile doctor) {
        return toBookingParties(patient, doctor).asMap();
    }

    default Map<String, Object> bookingParties(Appointment appointment) {
        return toBookingParties(appointment).asMap();
    }

    default Map<String, Object> bookingParties(AppointmentWaitlistEntry entry) {
        return toBookingParties(entry).asMap();
    }
}
