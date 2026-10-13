package com.ga.medic.enums;

public enum AppointmentStatusEnum {
    BOOKED("Booked"),
    COMPLETED("Completed"),
    NO_SHOW("No Show"),
    CANCELLED("Cancelled");

    private final String label;

    AppointmentStatusEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
