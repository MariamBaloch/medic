package com.ga.medic.specification;

import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.model.Appointment;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Locale;

public class AppointmentSpecifications {

    public static Specification<Appointment> forDoctor(Long doctorId) {
        return (root, query, cb) -> cb.equal(root.get("doctor").get("id"), doctorId);
    }

    public static Specification<Appointment> forPatient(Long patientId) {
        return (root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId);
    }

    public static Specification<Appointment> withOptionalDoctorName(String doctorName) {
        return (root, query, cb) -> {
            if (doctorName == null || doctorName.isBlank()) {
                return null;
            }

            String search = "%" + doctorName.trim().toLowerCase(Locale.ROOT) + "%";
            var fullName = cb.concat(
                    cb.concat(root.get("doctor").get("user").<String>get("firstName"), " "),
                    root.get("doctor").get("user").<String>get("lastName"));
            return cb.like(cb.lower(fullName), search);
        };
    }

    public static Specification<Appointment> withOptionalPatientName(String patientName) {
        return (root, query, cb) -> {
            if (patientName == null || patientName.isBlank()) {
                return null;
            }

            String search = "%" + patientName.trim().toLowerCase(Locale.ROOT) + "%";
            var fullName = cb.concat(
                    cb.concat(root.get("patient").get("user").<String>get("firstName"), " "),
                    root.get("patient").get("user").<String>get("lastName"));
            return cb.like(cb.lower(fullName), search);
        };
    }

    public static Specification<Appointment> fromDate(LocalDate dateFrom) {
        return (root, query, cb) -> dateFrom == null ? null
                : cb.greaterThanOrEqualTo(root.get("appointmentDate"), dateFrom);
    }

    public static Specification<Appointment> toDate(LocalDate dateTo) {
        return (root, query, cb) -> dateTo == null ? null
                : cb.lessThanOrEqualTo(root.get("appointmentDate"), dateTo);
    }

    public static Specification<Appointment> withStatus(AppointmentStatusEnum status) {
        return (root, query, cb) -> status == null ? null
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Appointment> withSpecialization(Long specializationId) {
        return (root, query, cb) -> specializationId == null ? null
                : cb.equal(root.get("doctor").get("specialization").get("id"), specializationId);
    }
}
