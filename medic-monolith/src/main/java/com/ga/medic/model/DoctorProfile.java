package com.ga.medic.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "doctor_profiles")
public class DoctorProfile extends Auditable {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialization_id", nullable = false)
    private Specialization specialization;

    @Column(nullable = false, unique = true, length = 100)
    private String licenseNumber;

    @Column(length = 255)
    private String qualification;

    @Column
    private Integer yearsOfExperience;

    @Column(precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(length = 255)
    private String hospitalAffiliation;

    @Column
    private String clinicAddress;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(nullable = false)
    private Boolean isVerified = false;
}