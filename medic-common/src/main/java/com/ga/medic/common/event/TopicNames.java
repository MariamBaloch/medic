package com.ga.medic.common.event;

import java.util.List;

/**
 * Kafka topic names shared by producers and consumers.
 */
public final class TopicNames {

    public static final String IDENTITY_USER_CHANGED = "identity.user.changed";
    public static final String IDENTITY_DOCTOR_CHANGED = "identity.doctor.changed";
    public static final String BOOKING_APPOINTMENT_CHANGED = "booking.appointment.changed";
    public static final String BOOKING_WAITLIST_CHANGED = "booking.waitlist.changed";
    public static final String AUDIT_EVENT_RECORDED = "audit.event.recorded";

    public static final List<String> ALL = List.of(
            IDENTITY_USER_CHANGED,
            IDENTITY_DOCTOR_CHANGED,
            BOOKING_APPOINTMENT_CHANGED,
            BOOKING_WAITLIST_CHANGED,
            AUDIT_EVENT_RECORDED
    );

    private TopicNames() {
    }

    public static String deadLetter(String topic) {
        return topic + ".DLT";
    }

    /**
     * Maps a dotted {@link EventTypes} value to its Kafka topic.
     */
    public static String forEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (eventType.startsWith("booking.appointment.")) {
            return BOOKING_APPOINTMENT_CHANGED;
        }
        if (eventType.startsWith("booking.waitlist.")) {
            return BOOKING_WAITLIST_CHANGED;
        }
        if (eventType.startsWith("identity.user.")) {
            return IDENTITY_USER_CHANGED;
        }
        if (eventType.startsWith("identity.doctor.")) {
            return IDENTITY_DOCTOR_CHANGED;
        }
        if (eventType.equals(EventTypes.AUDIT_EVENT_RECORDED)) {
            return AUDIT_EVENT_RECORDED;
        }
        throw new IllegalArgumentException("No Kafka topic mapped for eventType=" + eventType);
    }
}
