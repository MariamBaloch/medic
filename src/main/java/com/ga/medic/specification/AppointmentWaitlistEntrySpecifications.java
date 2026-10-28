package com.ga.medic.specification;

import com.ga.medic.enums.WaitlistStatusEnum;
import com.ga.medic.model.AppointmentWaitlistEntry;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Locale;

public class AppointmentWaitlistEntrySpecifications {

    public static Specification<AppointmentWaitlistEntry> forPatient(Long patientId) {
        return (root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId);
    }

    public static Specification<AppointmentWaitlistEntry> withOptionalDoctorName(String doctorName) {
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

    public static Specification<AppointmentWaitlistEntry> fromDate(LocalDate dateFrom) {
        return (root, query, cb) -> dateFrom == null ? null
                : cb.greaterThanOrEqualTo(root.get("appointmentDate"), dateFrom);
    }

    public static Specification<AppointmentWaitlistEntry> toDate(LocalDate dateTo) {
        return (root, query, cb) -> dateTo == null ? null
                : cb.lessThanOrEqualTo(root.get("appointmentDate"), dateTo);
    }

    public static Specification<AppointmentWaitlistEntry> withStatus(WaitlistStatusEnum status) {
        return (root, query, cb) -> status == null ? null
                : cb.equal(root.get("status"), status);
    }
}
