package com.ga.medic.model;

import com.ga.medic.enums.WaitlistStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "appointment_waitlist_entry")
@Getter
@Setter
public class AppointmentWaitlistEntry extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

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

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "preferred_start_time", nullable = false)
    private LocalTime preferredStartTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitlistStatusEnum status;

    @Column(name = "offered_at")
    private Instant offeredAt;

    @Column(name = "offer_expires_at")
    private Instant offerExpiresAt;

    @Version
    @Column(nullable = false)
    private Long version;

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
