package com.ga.medic.enums;

import lombok.Getter;

@Getter
public enum AppointmentStatusEnum {
    BOOKED("Booked"),
    COMPLETED("Completed"),
    NO_SHOW("No Show"),
    CANCELLED("Cancelled");

    private final String label;

    AppointmentStatusEnum(String label) {
        this.label = label;
    }

}