package com.ga.medic.booking.event;

/**
 * Signals that a doctor's availability rules or exceptions changed.
 */
public record DoctorScheduleChangedEvent(Long doctorId) {
}
