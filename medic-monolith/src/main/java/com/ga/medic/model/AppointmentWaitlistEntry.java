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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientProfile patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private DoctorProfile doctor;

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
}
