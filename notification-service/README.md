# notification-service

In-app notifications, unread counts, server-sent events, and email. Port **8084**. Database role `medic_notification`, schema `notification`.

## What this service owns

Notification is the inbox. It does not decide that an appointment was booked or cancelled. Booking already did that and published an event. This service turns that event into a row the user can list, a count of unread items, a live stream, and an email.

It listens only to `booking.appointment.changed` and `booking.waitlist.changed`. It does not listen to identity, so it never sends verification or password-reset mail. A new booking notifies the doctor only. The patient is not emailed for that event.

The notification row is the record. The SSE push and the email happen after that row commits. A repeat of the same Kafka `eventId` is ignored. Marking items read is local to this database. It does not change the appointment.

Public paths, through the gateway at `/api/notification`:

- `GET /` — list, with `unreadOnly`
- `GET /unread-count`
- `PATCH /read-all` and `PATCH /{notificationId}/read`
- `GET /subscribe` — `text/event-stream`

## How a booking becomes a notification

Booking writes the event to the outbox in the same database transaction as the appointment or waitlist change. The outbox publisher sends it to Kafka on `booking.appointment.changed` or `booking.waitlist.changed`.

This service consumes those topics. It saves the in-app notification, then pushes it on the SSE stream and sends the email. Email uses a pool of 2 core threads, up to 4, with room for 100 waiting tasks. The send happens after the notification row commits. A Kafka delivery with an `eventId` that was already stored is ignored.

A new booking notifies the doctor only. The patient is not emailed for that event. Verification and password-reset mail are sent by identity and are not on these topics.

```bash
./mvnw -pl notification-service -am spring-boot:run
```
