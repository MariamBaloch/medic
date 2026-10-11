package com.ga.medic.specification;

import com.ga.medic.enums.AppointmentStatusEnum;
import com.ga.medic.model.Appointment;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class AppointmentSpecifications {

    public static Specification<Appointment> forDoctor(Long doctorId) {
        return (root, query, cb) -> cb.equal(root.get("doctor").get("id"), doctorId);
    }

    public static Specification<Appointment> forPatient(Long patientId) {
        return (root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId);
    }

    public static Specification<Appointment> withOptionalDoctor(Long doctorId) {
        return (root, query, cb) -> doctorId == null ? null
                : cb.equal(root.get("doctor").get("id"), doctorId);
    }

    public static Specification<Appointment> withOptionalPatient(Long patientId) {
        return (root, query, cb) -> patientId == null ? null
                : cb.equal(root.get("patient").get("id"), patientId);
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
}