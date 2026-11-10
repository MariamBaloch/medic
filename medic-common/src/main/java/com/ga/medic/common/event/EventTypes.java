package com.ga.medic.common.event;

/**
 * Stable event-type strings carried on {@link EventEnvelope#eventType()}.
 */
public final class EventTypes {

    public static final String IDENTITY_USER_CREATED = "identity.user.created";
    public static final String IDENTITY_USER_UPDATED = "identity.user.updated";
    public static final String IDENTITY_USER_DEACTIVATED = "identity.user.deactivated";

    public static final String IDENTITY_DOCTOR_CREATED = "identity.doctor.created";
    public static final String IDENTITY_DOCTOR_UPDATED = "identity.doctor.updated";
    public static final String IDENTITY_DOCTOR_VERIFIED = "identity.doctor.verified";
    public static final String IDENTITY_DOCTOR_DEACTIVATED = "identity.doctor.deactivated";

    public static final String BOOKING_APPOINTMENT_CREATED = "booking.appointment.created";
    public static final String BOOKING_APPOINTMENT_RESCHEDULED = "booking.appointment.rescheduled";
    public static final String BOOKING_APPOINTMENT_CANCELLED = "booking.appointment.cancelled";
    public static final String BOOKING_APPOINTMENT_STATUS_CHANGED = "booking.appointment.status-changed";

    public static final String BOOKING_WAITLIST_JOINED = "booking.waitlist.joined";
    public static final String BOOKING_WAITLIST_OFFERED = "booking.waitlist.offered";
    public static final String BOOKING_WAITLIST_ACCEPTED = "booking.waitlist.accepted";
    public static final String BOOKING_WAITLIST_DECLINED = "booking.waitlist.declined";
    public static final String BOOKING_WAITLIST_EXPIRED = "booking.waitlist.expired";

    public static final String AUDIT_EVENT_RECORDED = "audit.event.recorded";

    private EventTypes() {
    }
}
