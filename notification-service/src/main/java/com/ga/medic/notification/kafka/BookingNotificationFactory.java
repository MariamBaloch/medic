package com.ga.medic.notification.kafka;

import com.ga.medic.common.event.EventEnvelope;
import com.ga.medic.common.event.EventTypes;
import com.ga.medic.notification.enums.NotificationAction;
import com.ga.medic.notification.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BookingNotificationFactory {

    public List<NotificationDraft> draftsFor(EventEnvelope envelope) {
        Map<String, Object> payload = envelope.payload() == null ? Map.of() : envelope.payload();
        String eventType = envelope.eventType();

        return switch (eventType) {
            case EventTypes.BOOKING_APPOINTMENT_CREATED -> created(payload);
            case EventTypes.BOOKING_APPOINTMENT_RESCHEDULED -> rescheduled(payload);
            case EventTypes.BOOKING_APPOINTMENT_CANCELLED -> cancelled(payload);
            case EventTypes.BOOKING_WAITLIST_OFFERED -> waitlistOffered(payload);
            case EventTypes.BOOKING_WAITLIST_ACCEPTED -> waitlistAccepted(payload);
            default -> List.of();
        };
    }

    private List<NotificationDraft> created(Map<String, Object> payload) {
        if ("waitlist".equals(string(payload, "source"))) {
            return List.of();
        }
        Long relatedId = longValue(payload, "appointmentId");
        return List.of(doctorDraft(
                payload,
                NotificationType.APPOINTMENT,
                NotificationAction.BOOKED,
                "New Appointment Booking",
                "You have a new appointment booking from " + string(payload, "patientName")
                        + " on " + string(payload, "date")
                        + " from " + string(payload, "startTime")
                        + " to " + string(payload, "endTime") + ".",
                relatedId
        ));
    }

    private List<NotificationDraft> rescheduled(Map<String, Object> payload) {
        Long relatedId = longValue(payload, "appointmentId");
        List<NotificationDraft> drafts = new ArrayList<>(2);
        drafts.add(patientDraft(
                payload,
                NotificationType.APPOINTMENT,
                NotificationAction.RESCHEDULED,
                "Appointment Rescheduled",
                "Your appointment with " + string(payload, "doctorName") + " has been rescheduled to "
                        + string(payload, "date") + " at " + string(payload, "startTime") + ".",
                relatedId
        ));
        drafts.add(doctorDraft(
                payload,
                NotificationType.APPOINTMENT,
                NotificationAction.RESCHEDULED,
                "Patient Rescheduled Appointment",
                string(payload, "patientName") + " rescheduled their appointment to "
                        + string(payload, "date") + " at " + string(payload, "startTime") + ".",
                relatedId
        ));
        return drafts;
    }

    private List<NotificationDraft> cancelled(Map<String, Object> payload) {
        Long relatedId = longValue(payload, "appointmentId");
        return List.of(doctorDraft(
                payload,
                NotificationType.APPOINTMENT,
                NotificationAction.CANCELLED,
                "Appointment Cancelled",
                "Appointment cancelled by " + string(payload, "patientName")
                        + " for " + string(payload, "date")
                        + " at " + string(payload, "startTime") + ".",
                relatedId
        ));
    }

    private List<NotificationDraft> waitlistOffered(Map<String, Object> payload) {
        Long relatedId = longValue(payload, "waitlistEntryId");
        String message = "A waitlist appointment with " + string(payload, "doctorName")
                + " is available on " + string(payload, "date")
                + " at " + string(payload, "startTime") + ".";
        String offerExpiresAt = string(payload, "offerExpiresAt");
        if (!offerExpiresAt.isBlank()) {
            message += " Please accept by " + offerExpiresAt + " to book this slot.";
        }
        return List.of(patientDraft(
                payload,
                NotificationType.WAITLIST,
                NotificationAction.OFFERED,
                "Appointment Slot Available",
                message,
                relatedId
        ));
    }

    private List<NotificationDraft> waitlistAccepted(Map<String, Object> payload) {
        Long relatedId = longValue(payload, "appointmentId");
        if (relatedId == null) {
            relatedId = longValue(payload, "waitlistEntryId");
        }
        List<NotificationDraft> drafts = new ArrayList<>(2);
        drafts.add(doctorDraft(
                payload,
                NotificationType.APPOINTMENT,
                NotificationAction.BOOKED,
                "Waitlist Appointment Booked",
                string(payload, "patientName") + " accepted a waitlist offer for "
                        + string(payload, "date") + " at " + string(payload, "startTime") + ".",
                relatedId
        ));
        drafts.add(patientDraft(
                payload,
                NotificationType.APPOINTMENT,
                NotificationAction.BOOKED,
                "Waitlist Offer Accepted",
                "Your appointment with " + string(payload, "doctorName") + " is booked for "
                        + string(payload, "date") + " at " + string(payload, "startTime") + ".",
                relatedId
        ));
        return drafts;
    }

    private NotificationDraft doctorDraft(
            Map<String, Object> payload,
            NotificationType type,
            NotificationAction action,
            String title,
            String message,
            Long relatedEntityId
    ) {
        return new NotificationDraft(
                longValue(payload, "doctorUserId"),
                string(payload, "doctorEmail"),
                string(payload, "doctorName"),
                type,
                action,
                title,
                message,
                relatedEntityId
        );
    }

    private NotificationDraft patientDraft(
            Map<String, Object> payload,
            NotificationType type,
            NotificationAction action,
            String title,
            String message,
            Long relatedEntityId
    ) {
        return new NotificationDraft(
                longValue(payload, "patientUserId"),
                string(payload, "patientEmail"),
                string(payload, "patientName"),
                type,
                action,
                title,
                message,
                relatedEntityId
        );
    }

    private static String string(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static Long longValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
