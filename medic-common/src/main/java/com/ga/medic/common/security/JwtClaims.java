package com.ga.medic.common.security;

public final class JwtClaims {

    public static final String SUBJECT = "sub";
    public static final String JWT_ID = "jti";
    public static final String USER_ID = "uid";
    public static final String ROLES = "roles";
    public static final String EMAIL = "email";
    public static final String TOKEN_VERSION = "ver";
    public static final String PATIENT_ID = "patientId";
    public static final String DOCTOR_ID = "doctorId";
    public static final String NAME = "name";

    private JwtClaims() {
    }
}
