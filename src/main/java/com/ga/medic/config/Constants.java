package com.ga.medic.config;

public final class Constants {
    private Constants() {
    }
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()\\-+_=<>?]).{5,}$";
    public static final String PHONE_REGEX = "^\\+?[0-9]{7,20}$";
}