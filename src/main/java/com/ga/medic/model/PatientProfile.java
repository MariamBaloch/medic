package com.ga.medic.model;

import com.ga.medic.enums.BloodTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "patient_profile")
public class PatientProfile extends Auditable {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(unique = true, length = 50)
    private String nationalId;

    @Column
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BloodTypeEnum bloodType;

    @Column
    private Integer heightCm;

    @Column
    private Integer weightKg;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(columnDefinition = "TEXT")
    private String chronicConditions;

}