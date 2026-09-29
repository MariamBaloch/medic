package com.ga.medic.config;

import java.util.List;

public final class Constants {
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()\\-+_=<>?]).{5,}$";
    public static final String PHONE_REGEX = "^\\+?[0-9]{7,20}$";
    public static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );
    private Constants() {
    }
}