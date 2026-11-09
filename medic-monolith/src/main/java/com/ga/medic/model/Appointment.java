package com.ga.medic.model;

import com.ga.medic.enums.AppointmentStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "appointment")
public class Appointment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "patient_user_id", nullable = false)
    private Long patientUserId;

    @Column(name = "doctor_user_id", nullable = false)
    private Long doctorUserId;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "patient_email", nullable = false)
    private String patientEmail;

    @Column(name = "doctor_name", nullable = false)
    private String doctorName;

    @Column(name = "doctor_email", nullable = false)
    private String doctorEmail;

    @Column(name = "doctor_specialization_id")
    private Long doctorSpecializationId;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(precision = 10, scale = 2)
    private BigDecimal bookingFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatusEnum status = AppointmentStatusEnum.BOOKED;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(length = 255)
    private String cancelledReason;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "availability_rule_id")
    private Long availabilityRuleId;

    public void applyPartySnapshots(PatientProfile patient, DoctorProfile doctor) {
        this.patientId = patient.getId();
        this.doctorId = doctor.getId();
        this.patientUserId = patient.getUser().getId();
        this.doctorUserId = doctor.getUser().getId();
        this.patientName = patient.getUser().getFullName();
        this.patientEmail = patient.getUser().getEmail();
        this.doctorName = doctor.getUser().getFullName();
        this.doctorEmail = doctor.getUser().getEmail();
        this.doctorSpecializationId = doctor.getSpecialization() != null
                ? doctor.getSpecialization().getId()
                : null;
    }
}
