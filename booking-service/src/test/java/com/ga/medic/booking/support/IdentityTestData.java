package com.ga.medic.booking.support;

import com.ga.medic.common.contract.InternalDoctorSnapshotResponse;
import com.ga.medic.common.contract.InternalPatientSnapshotResponse;

import java.math.BigDecimal;

public final class IdentityTestData {

    private IdentityTestData() {
    }

    public static InternalPatientSnapshotResponse patient(
            long patientId, long patientUserId, String name, String email) {
        return new InternalPatientSnapshotResponse(patientId, patientUserId, name, email);
    }

    public static InternalDoctorSnapshotResponse doctor(
            long doctorId, long doctorUserId, String name, String email,
            long specializationId, BigDecimal consultationFee) {
        return new InternalDoctorSnapshotResponse(
                doctorId, doctorUserId, name, email, specializationId, "Cardiology", consultationFee);
    }

    public static InternalDoctorSnapshotResponse seedDoctorOne() {
        return doctor(1L, 2L, "Sarah Smith", "doctor1@medic.com", 1L, new BigDecimal("150.00"));
    }

    public static InternalDoctorSnapshotResponse seedDoctorTwo() {
        return doctor(2L, 3L, "Robert Brown", "doctor2@medic.com", 4L, new BigDecimal("200.00"));
    }

    public static InternalPatientSnapshotResponse seedPatientOne() {
        return patient(1L, 4L, "John Doe", "patient1@medic.com");
    }

    public static InternalPatientSnapshotResponse seedPatientTwo() {
        return patient(2L, 5L, "Jane Clark", "patient2@medic.com");
    }

    public static InternalPatientSnapshotResponse seedPatientThree() {
        return patient(3L, 6L, "Alex Patient", "patient3@medic.com");
    }
}
