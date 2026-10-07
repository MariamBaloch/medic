package com.ga.medic.service;

import com.ga.medic.dto.response.NotificationResponse;
import com.ga.medic.enums.NotificationAction;
import com.ga.medic.enums.NotificationType;
import com.ga.medic.mapper.NotificationMapper;
import com.ga.medic.model.Notification;
import com.ga.medic.model.User;
import com.ga.medic.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    // Map to hold userId and all active SseEmitters for that user for multiple connections
    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Opens a server-sent-events connection and tracks it for the user's live notifications.
     *
     * @param userId the ID of the user subscribing to notifications
     * @return the event stream connected to the user
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(error -> removeEmitter(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("Connected"));
        } catch (IOException e) {
            removeEmitter(userId, emitter);
        }
        return emitter;
    }

    /**
     * Sends a notification to every active event-stream connection for the user.
     *
     * @param userId the ID of the notification recipient
     * @param notification the notification to send
     */
    public void sendNotification(Long userId, NotificationResponse notification) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        log.info("Sending notification to userId {}: {}", userId, notification);

        if (userEmitters == null) {
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
                log.info("Notification sent to userId {}: {}", userId, notification);
            } catch (IOException e) {
                removeEmitter(userId, emitter);
            }
        }
    }

    /**
     * Removes a closed event stream and drops the user's entry when no streams remain.
     *
     * @param userId the ID of the stream's user
     * @param emitter the event stream to remove
     */
    private void removeEmitter(Long userId, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emitters.get(userId);

        if (userEmitters != null) {
            userEmitters.remove(emitter);

            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    /**
     * Persists a notification before delivering it to the user's active event-stream connections.
     *
     * @param user the notification recipient
     * @param type the notification category
     * @param action the action associated with the notification
     * @param title the notification title
     * @param message the notification body
     * @param read whether the notification is initially marked as read
     * @param relatedEntityId the ID of the related entity, if any
     */
    public void createAndSend(User user, NotificationType type, NotificationAction action, String title, String message, boolean read, Long relatedEntityId
    ) {
        Notification notification = notificationMapper.toNotification(user, type, action, title, message, read, relatedEntityId);
        notificationRepository.save(notification);
        NotificationResponse response = notificationMapper.toResponse(notification);
        sendNotification(user.getId(), response);
        sendEmailNotification(user, type, action, title, message, relatedEntityId);
    }

    private void sendEmailNotification(User user, NotificationType type, NotificationAction action, String title,
                                       String message, Long relatedEntityId) {
        try {
            emailService.sendTemplateEmail(
                    user.getEmail(),
                    title,
                    "email/notification",
                    Map.of(
                            "fullName", user.getFullName(),
                            "title", title,
                            "message", message,
                            "type", type,
                            "action", action,
                            "relatedEntityId", relatedEntityId == null ? "" : relatedEntityId
                    )
            );
        } catch (RuntimeException e) {
            log.warn("Failed to send email notification to userId {}", user.getId(), e);
        }
    }
}
