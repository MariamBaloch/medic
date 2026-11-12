package com.ga.medic.booking.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BloodTypeEnum {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String value;

    BloodTypeEnum(String value) {
        this.value = value;
    }

    public static BloodTypeEnum fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (BloodTypeEnum type : values()) {
            if (type.value.equals(raw) || type.name().equals(raw)) {
                return type;
            }
        }
        return null;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}