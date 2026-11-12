package com.ga.medic.booking.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a doctor's scheduled time slot for caching.
 * Contains the availability rule, date, start and end times, and slot duration.
 */
public record DoctorScheduledSlot(
        Long ruleId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        int slotMinutes
) implements Serializable {
}
