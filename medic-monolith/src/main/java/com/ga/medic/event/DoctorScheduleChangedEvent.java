package com.ga.medic.event;

/**
 * Signals that a doctor's availability rules or exceptions changed.
 */
public record DoctorScheduleChangedEvent(Long doctorId) {
}
